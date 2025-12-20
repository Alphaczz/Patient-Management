package com.pm.patient_service.service;

import com.pm.patient_service.dto.PatientRequestDTO;
import com.pm.patient_service.dto.PatientResponseDTO;
import com.pm.patient_service.exception.EmailAlreadyExistsException;
import com.pm.patient_service.exception.PatientNotFoundException;
import com.pm.patient_service.grpc.BillingServiceGrpcClient;
import com.pm.patient_service.mapper.PatientMapper;
import com.pm.patient_service.model.Patient;
import com.pm.patient_service.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    public PatientService(PatientRepository patientRepository,BillingServiceGrpcClient  billingServiceGrpcClient){
        this.patientRepository=patientRepository;
        this.billingServiceGrpcClient=billingServiceGrpcClient;
    }

    public List<PatientResponseDTO> getPatient(){
         List<Patient> patients= patientRepository.findAll();

        return patients.stream().map(patient->PatientMapper.toDTO(patient)).toList();
    }
    public PatientResponseDTO createPatient(PatientRequestDTO patient){
         if (patientRepository.existsByEmail(patient.getEmail())){
             throw new EmailAlreadyExistsException("A patient with this email already exist : "+patient.getEmail());
         }
         Patient new_patient;
         new_patient=patientRepository.save(PatientMapper.toModel(patient));
        billingServiceGrpcClient.createBillingAccount(new_patient.getId().toString(),
                new_patient.getName(), new_patient.getEmail());
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
