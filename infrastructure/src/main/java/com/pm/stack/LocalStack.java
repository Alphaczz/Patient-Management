package com.pm.stack;

import com.amazonaws.services.dynamodbv2.xspec.S;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;
import software.amazon.awscdk.services.servicediscovery.DnsRecordType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalStack extends Stack {
    private final Vpc vpc;
 private final Cluster ecsCluster;
    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope,id,props);
        this.vpc = createVpc();
        DatabaseInstance authServiceDb =
                createDatabase("AuthServiceDB","auth-service-db");
        DatabaseInstance patientServiceDb =
                createDatabase("PatientServiceDB","patient-service-db");
        CfnHealthCheck authDbHeathCheck =createDbHealthCheck(authServiceDb,"AuthServiceDbHealthCheck");
        CfnHealthCheck patientDbHeathCheck =createDbHealthCheck(patientServiceDb,"PatientServiceDbHealthCheck");
        CfnCluster  mskCluster =createMskCkuster();
        this.ecsCluster = createEcsCluster();

        FargateService authService =
                createFargateService("AuthService",
                        "auth-service",
                        List.of(4005),
                        authServiceDb,
                        Map.of("JWT_SECRET", "Y2hhVEc3aHJnb0hYTzMyZ2ZqVkpiZ1RkZG93YWxrUkM="));
        authService.getNode().addDependency(authDbHeathCheck);
        authService.getNode().addDependency(authServiceDb);

        FargateService billingService =
                createFargateService("BillingService",
                        "billing-service",
                        List.of(4001,9001),
                        null,
                        null);

        FargateService analyticsService =
                createFargateService("AnalyticsService",
                        "analytics-service",
                        List.of(4002),
                        null,
                        null);

        analyticsService.getNode().addDependency(mskCluster);
        FargateService patientService = createFargateService("PatientService",
                "patient-service",
                List.of(4000),
                patientServiceDb,
                Map.of(
                        "BILLING_SERVICE_ADDRESS", "billing-service.patient-management.local",
                        "BILLING_SERVICE_GRPC_PORT", "9001"
                ));
        patientService.getNode().addDependency(patientServiceDb);
        patientService.getNode().addDependency(patientDbHeathCheck);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);
//        patientService.getNode().addDependency(elastiCacheCluster);
        ApplicationLoadBalancedFargateService apiGateway =
                createApiGatewayService();
   //     apiGateway.getNode().addDependency(elastiCacheCluster);

    }

    private Cluster createEcsCluster() {
        return Cluster.Builder.create(
                this,"PatientMabagementCluster"
        ).vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("patient-management.local")
                        .build())

                .build();
    }

    private Vpc createVpc() {
        return Vpc.Builder.create(this, "PatientManagementVpc")
                .vpcName("PatientManagementVpc")
                .maxAzs(2)
                .build();


    }
    private FargateService createFargateService(String id, String imageName,
                                                List<Integer>  ports,
                                                DatabaseInstance db,
                                                Map<String,String> additionalEnvVars) {

        FargateTaskDefinition taskDefinition =FargateTaskDefinition.Builder.create(this,id+"Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build()
                ;
        ContainerDefinitionOptions.Builder containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry(imageName))
                        .portMappings(ports.stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                        .logGroupName("/ecs/" + imageName)
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix(imageName)
                                .build()));

        Map<String, String > envVaras =new HashMap<>();
        envVaras.put("SPRING_KAFKA_BOOTSTRAP_SERVERS",
                "localhost.localstack.cloud:4510,localhost.localstack.cloud:4511,localhost.localstack.cloud:4512");
  if (additionalEnvVars!=null) {
      envVaras.putAll(additionalEnvVars);
  }
  if (db!=null) {
       envVaras.put("SPRING_DATASOURCE_URL","jdbc:postgresql://%s:%s/%s-db".formatted(
               db.getDbInstanceEndpointAddress(),
               db.getDbInstanceEndpointPort(),
               imageName
       ));
      envVaras.put("SPRING_DATASOURCE_USERNAME", "admin_user");
      envVaras.put("SPRING_DATASOURCE_PASSWORD",
              db.getSecret().secretValueFromJson("password").toString());
      envVaras.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
      envVaras.put("SPRING_SQL_INIT_MODE", "always");
      envVaras.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
  }
        containerOptions.environment(envVaras);
        taskDefinition.addContainer(imageName + "Container", containerOptions.build());


        return FargateService.Builder.create(this, id)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .cloudMapOptions(CloudMapOptions.builder()
                        .name(imageName)
                        .dnsRecordType(DnsRecordType.A)
                        .build())
                .serviceName(imageName)
                .build();
    }
    private ApplicationLoadBalancedFargateService createApiGatewayService() {
        FargateTaskDefinition taskDefinition =
                FargateTaskDefinition.Builder.create(this, "APIGatewayTaskDefinition")
                        .cpu(256)
                        .memoryLimitMiB(512)
                        .build();

        ContainerDefinitionOptions containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry("api-gateway"))
                        .environment(Map.of(
                                "SPRING_PROFILES_ACTIVE", "prod",
                                "AUTH_SERVICE_URL", "http://host.docker.internal:4005"

                        ))
                        .portMappings(List.of(4004).stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                                        .logGroupName("/ecs/api-gateway")
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix("api-gateway")
                                .build()))
                        .build();


        taskDefinition.addContainer("APIGatewayContainer", containerOptions);

        ApplicationLoadBalancedFargateService apiGateway =
                ApplicationLoadBalancedFargateService.Builder.create(this, "APIGatewayService")
                        .cluster(ecsCluster)
                        .serviceName("api-gateway")
                        .taskDefinition(taskDefinition)
                        .desiredCount(1)
                        .healthCheckGracePeriod(Duration.seconds(60))
                        .publicLoadBalancer(true)
                        .cloudMapOptions(CloudMapOptions.builder()
                                .name("api-gateway")
                                .dnsRecordType(DnsRecordType.A)
                                .build())
                        .build();

        return apiGateway;
    }
    private DatabaseInstance createDatabase(String id,String dbname){
           return  DatabaseInstance.Builder
                   .create(this,id)
                   .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                           .version(PostgresEngineVersion.VER_17_2).build()))
                   .vpc(vpc)
                   .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                   .allocatedStorage(20)
                   .credentials(Credentials.fromGeneratedSecret("admin_user"))
                   .databaseName(dbname)
                   .removalPolicy(RemovalPolicy.DESTROY)/// not in prod
                   .build();

    }

    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db,String id){
        return CfnHealthCheck.Builder.create(
                this,id
        ).healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                .type("TCP")
                .port(Token.asNumber(
                        db.getDbInstanceEndpointPort()
                ))
                .ipAddress(db.getDbInstanceEndpointAddress())
                .requestInterval(30)
                .failureThreshold(3)
                .build()).build();
    }
     private CfnCluster createMskCkuster(){
        return CfnCluster.Builder.create(this,"MskCluster")
                .clusterName("kafka-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.xlarge")
                        .clientSubnets(vpc.getPrivateSubnets().stream().map(
                                ISubnet::getSubnetId
                        ).collect(Collectors.toList()))
                        .brokerAzDistribution("DEFAULT").build())
                        .build();

     }
    public static void main(String[] args) {
        App app = new App(AppProps.builder().outdir("./cdk.out").build());
        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();

        new LocalStack(app, "localstack", props);
        app.synth();
        System.out.println("App Synrhesizing in Process");

    }
}
