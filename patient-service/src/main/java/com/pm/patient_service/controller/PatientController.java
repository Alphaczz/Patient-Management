package com.pm.patient_service.controller;

import com.pm.patient_service.dto.PagedPatientResponseDTO;
import com.pm.patient_service.dto.PatientRequestDTO;
import com.pm.patient_service.dto.PatientResponseDTO;
import com.pm.patient_service.dto.vaildators.CreatePatientValidationGroup;
import com.pm.patient_service.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient",description = "APis to manage patients")
public class PatientController {
   private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(summary = "Get Patients")
    public ResponseEntity<PagedPatientResponseDTO> getAllPatient(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "asc") String sort,
            @RequestParam(defaultValue = "name") String sortField  ,
            @RequestParam(defaultValue = "") String searchValue
    ){
            PagedPatientResponseDTO response=patientService.getPatient(page,size,sort,sortField,searchValue);
            return ResponseEntity.ok().body(response);

    }
    @PostMapping
    @Operation(summary = "Create a new Patients")
    public  ResponseEntity<PatientResponseDTO> createPatient(@Validated({Default.class, CreatePatientValidationGroup.class}) @RequestBody PatientRequestDTO req){
        PatientResponseDTO response = patientService.createPatient(req);
        return  ResponseEntity.ok().body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Patients Details")
    public  ResponseEntity<PatientResponseDTO> updatePatient(@PathVariable UUID id,
                                                             @Validated({Default.class}) @RequestBody PatientRequestDTO req){
        PatientResponseDTO response = patientService.updatePatient(id,req);
        return  ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Patients")
    public  ResponseEntity<Void> deletePatient(@PathVariable UUID id){
        patientService.delete(id);
        return  ResponseEntity.noContent().build();
    }



}
