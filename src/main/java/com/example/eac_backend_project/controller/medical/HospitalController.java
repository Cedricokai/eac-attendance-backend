package com.example.eac_backend_project.controller.medical;
import com.example.eac_backend_project.Service.medical.MedicalVisitService;
import com.example.eac_backend_project.dto.medical.HospitalRequest;
import com.example.eac_backend_project.entity.medical.Hospital;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/hospitals") @RequiredArgsConstructor
public class HospitalController {
 private final MedicalVisitService service;
 @GetMapping public List<Hospital> list(){return service.hospitals();}
 @PostMapping public Hospital create(@Valid @RequestBody HospitalRequest r){return service.createHospital(r);}
 @PostMapping("/{id}/access-link") public Map<String,String> link(@PathVariable Long id){return service.generateLink(id);}
 @PostMapping("/{id}/revoke-link") public Hospital revoke(@PathVariable Long id){return service.revokeLink(id);}
}
