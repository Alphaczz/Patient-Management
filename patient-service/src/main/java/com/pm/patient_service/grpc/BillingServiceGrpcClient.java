package com.pm.patient_service.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class BillingServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;
    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String serverAdress,
            @Value("${billing.service.grpc.port:9001}") int serverPort
    ){
      log.info("Server Address:{}:{}",serverAdress,serverPort);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAdress,serverPort)
                .usePlaintext().build();

        blockingStub =BillingServiceGrpc.newBlockingStub(channel);

    }
    public BillingResponse createBillingAccount(String patientId,String name,String email)
    {
        BillingRequest request= BillingRequest.newBuilder().setPatientId(patientId).setEmail(email)
                .setName(name).build();
        BillingResponse response =blockingStub.createBillingAccount(request);
        log.info("Recieve reponse from billing service via GRPC:{}",response.getAccountId());
        return  response;
    }


}
