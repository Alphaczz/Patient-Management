package com.pm.patient_service.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import com.pm.patient_service.kafka.KafkaProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;
    private  final KafkaProducer kafkaProducer;
    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String serverAddress,
            @Value("${billing.service.grpc.port:9001}") int serverPort,
            KafkaProducer kafkaProducer
    ){
      log.info("Server Address:{}:{}",serverAddress,serverPort);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress,serverPort)
                .usePlaintext().build();

        blockingStub =BillingServiceGrpc.newBlockingStub(channel);
        this.kafkaProducer = kafkaProducer;

    }


    @CircuitBreaker(name="billingservice",fallbackMethod="billingFallback")
    @Retry(name="billingRetry")
    public BillingResponse createBillingAccount(String patientId,String name,String email)
    {
        BillingRequest request= BillingRequest.newBuilder().setPatientId(patientId).setEmail(email)
                .setName(name).build();
        BillingResponse response =blockingStub.createBillingAccount(request);
        log.info("Recieve reponse from billing service via GRPC:{}",response.getAccountId());
        return  response;
    }

    public  BillingResponse billingFallback(String patientId,String name,String email,Throwable t ){
        log.warn("[CIRCUIT Breaker]: Billing Service is unavailable.Triggered :: "+t.getMessage(),t);
        kafkaProducer.sendBillingAcountEvent(patientId,name,email);
        return BillingResponse.newBuilder()
                .setAccountId("")
                .setStatus("PENDING")
                .build();

    }


}
