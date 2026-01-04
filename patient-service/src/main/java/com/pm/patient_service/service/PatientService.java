package com.pm.patient_service.service;

import com.pm.patient_service.dto.PagedPatientResponseDTO;
import com.pm.patient_service.dto.PatientRequestDTO;
import com.pm.patient_service.dto.PatientResponseDTO;
import com.pm.patient_service.exception.EmailAlreadyExistsException;
import com.pm.patient_service.exception.PatientNotFoundException;
import com.pm.patient_service.grpc.BillingServiceGrpcClient;
import com.pm.patient_service.kafka.KafkaProducer;
import com.pm.patient_service.mapper.PatientMapper;
import com.pm.patient_service.model.Patient;
import com.pm.patient_service.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;
    public PatientService(PatientRepository patientRepository,
                          BillingServiceGrpcClient  billingServiceGrpcClient,
                          KafkaProducer kafkaProducer){
        this.patientRepository=patientRepository;
        this.kafkaProducer=kafkaProducer;
        this.billingServiceGrpcClient=billingServiceGrpcClient;
    }

    @Cacheable(
            value = "patients",
            key = "#page+ '-' +#size +'-' +#sort + '-' + #sortField",
            condition = "#searchValue == null || #searchValue.isEmpty()"

    )
    public PagedPatientResponseDTO getPatient(int page ,
                                              int size,
                                              String sort,
                                              String sortField,
                                              String searchValue){
        log.info("[REDIS]:cache miss -fetching from db");
//        try{
//            Thread.sleep(2000);
//        }catch (InterruptedException e){
//            log.error(" error :",e.getMessage());
//        }
        Pageable pageable = PageRequest.of(page-1, size,
                sort.equalsIgnoreCase("desc") ? Sort.by(sortField).descending()
                :Sort.by(sortField).ascending() );

        Page<Patient> patientPage;
        if (searchValue == null|| searchValue.isBlank()) {
             patientPage = patientRepository.findAll(pageable);
        }else {
            patientPage=patientRepository.findByNameContainingIgnoreCase(searchValue,pageable);
        }
        List<PatientResponseDTO> patientResponseFDtos= patientPage.getContent().stream()
                .map(PatientMapper::toDTO).toList();

        return new PagedPatientResponseDTO(
                         patientResponseFDtos,
                         patientPage.getNumber()+1,
                         patientPage.getSize(),
                         patientPage.getTotalPages(),
                         (int)patientPage.getTotalElements()
                 );

    }
    public PatientResponseDTO createPatient(PatientRequestDTO patient){
         if (patientRepository.existsByEmail(patient.getEmail())){
             throw new EmailAlreadyExistsException("A patient with this email already exist : "+patient.getEmail());
         }
         Patient new_patient;
         new_patient=patientRepository.save(PatientMapper.toModel(patient));
        billingServiceGrpcClient.createBillingAccount(new_patient.getId().toString(),
                new_patient.getName(), new_patient.getEmail());

        kafkaProducer.sendEvent(new_patient);
         return  PatientMapper.toDTO(new_patient);
    }
    public void delete(UUID id){
//        if (!patientRepository.existsByEmail(patient.getEmail())){
//            throw new EmailDoestNotExistException("A patient with this email doesnt exist : "+patient.getEmail());
//        }
        Patient patient_to_delete=patientRepository.findById(id).orElseThrow(
                ()->    new PatientNotFoundException("Patient not found with ID : " + id));

        patientRepository.delete(patient_to_delete);


    }

    public PatientResponseDTO updatePatient(UUID id , PatientRequestDTO patientRequestDTO){
        Patient patient =patientRepository.findById(id).orElseThrow(
                ()->
             new PatientNotFoundException("Patient not found with ID : " + id));
        if ( patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(),id)){
            throw new EmailAlreadyExistsException("A patient with this email already exist : "+patientRequestDTO.getEmail());
        }
        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));
        patient.setEmail(patientRequestDTO.getEmail());
        Patient updatedPatient = patientRepository.save(patient);
        return PatientMapper.toDTO(updatedPatient);
    }

}
