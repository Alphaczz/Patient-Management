package com.pm.patient_service.kafka;

import billing.events.BillingAccountEvent;
import com.pm.patient_service.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String,byte[]> kafkaTemplate;



    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient){
        PatientEvent event =PatientEvent.newBuilder().setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType("PATIENT_CREATED")
                .build();
        try{
            //
            // byte array to keep size small
            log.info(" sending Patient Created event:{}",event);
            kafkaTemplate.send("patient", patient.getId().toString(), event.toByteArray());

        }catch (Exception ex){
            log.error("Error sending Patient Created event:{}",event);
        }


    }

    public void sendBillingAcountEvent(String patientId, String name, String email) {
        BillingAccountEvent event = BillingAccountEvent.newBuilder()
                .setPatientId(patientId).setName(name).setEmail(email).setEventType("BILLING_ACCOUNT_CREATED_REQUESTED")
                .build();
        try{
            kafkaTemplate.send("billing-account", event.toByteArray());
            log.info(" sending Billing Account Created event:{}",event);
        }catch (Exception ex){
            log.error("Error sending Billing Account Created event:{}",ex.getMessage());
        }


    }
}
