package com.pm.patient_service.mapper;

import com.pm.patient_service.dto.PatientRequestDTO;
import com.pm.patient_service.dto.PatientResponseDTO;
import com.pm.patient_service.model.Patient;

import java.time.LocalDate;

public class PatientMapper {
    public  static PatientResponseDTO toDTO(Patient patient){
        PatientResponseDTO patientDto= new PatientResponseDTO();
        patientDto.setId(patient.getId().toString());
        patientDto.setName(patient.getName());
        patientDto.setAddress(patient.getAddress());
        patientDto.setEmail(patient.getEmail());
        patientDto.setDateOfBirth(patient.getDateOfBirth().toString());
        return  patientDto;
    }
    public  static Patient toModel(PatientRequestDTO patientreq){
        Patient patient= new Patient();
        patient.setName(patientreq.getName());
        patient.setAddress(patientreq.getAddress());
        patient.setEmail(patientreq.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientreq.getDateOfBirth()));
        patient.setRegisteredDate(LocalDate.parse(patientreq.getRegistredDate()));

        return  patient;
    }
}
