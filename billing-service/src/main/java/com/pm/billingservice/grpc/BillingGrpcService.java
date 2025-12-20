package com.pm.billingservice.grpc;

import billing.BillingServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class BillingGrpcService
        extends BillingServiceGrpc.BillingServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    @Override
    public void createBillingAccount(billing.BillingRequest billingRequest,
                                     StreamObserver<billing.BillingResponse> responseStreamObserver){
        log.info("CreateBillingAccount request received {}",billingRequest.toString());
        //business logic -eg save to db and perform calculate etc
        billing.BillingResponse response = billing.BillingResponse.newBuilder()
                .setAccountId("12345")
                .setStatus("ACTIVE")
                .build();
        responseStreamObserver.onNext(response);
        responseStreamObserver.onCompleted();

    }
}
