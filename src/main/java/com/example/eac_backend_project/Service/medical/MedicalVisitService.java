package com.example.eac_backend_project.Service.medical;

import com.example.eac_backend_project.Service.FileStorageService;
import com.example.eac_backend_project.dto.medical.*;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.medical.*;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.medical.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class MedicalVisitService {
 private final HospitalRepository hospitals; private final MedicalVisitRepository visits;
 private final EmployeeRepository employees; private final UserRepository users; private final FileStorageService files;

 public List<Hospital> hospitals(){ return hospitals.findAll(); }
 public Hospital createHospital(HospitalRequest r){
  String code=r.code().trim().toUpperCase(); if(hospitals.existsByCodeIgnoreCase(code)) throw new IllegalArgumentException("Facility code already exists");
  Hospital h=new Hospital(); h.setName(r.name().trim()); h.setCode(code); h.setAddress(r.address()); h.setContactName(r.contactName().trim()); h.setPhone(r.phone().trim()); h.setEmail(blankToNull(r.email())); return hospitals.save(h);
 }
 public Map<String,String> generateLink(Long id){ Hospital h=hospital(id); h.setAccessToken(UUID.randomUUID().toString().replace("-","")); h.setTokenCreatedAt(LocalDateTime.now()); hospitals.save(h); return Map.of("token",h.getAccessToken()); }
 public Hospital revokeLink(Long id){ Hospital h=hospital(id); h.setAccessToken(null); h.setTokenCreatedAt(null); return hospitals.save(h); }
 public Map<String,Object> lookup(String token,String identifier){
  activeHospital(token);
  String value=identifier.trim();
  Employee e=employees.findByEmployeeIdIgnoreCase(value)
   .or(()->employees.findByTagNumberIgnoreCase(value))
   .filter(x->!Boolean.FALSE.equals(x.getActive()))
   .orElseThrow(()->new NoSuchElementException("Employee not found for the supplied employee ID or tag number"));
  return employeeMap(e);
 }
 @Transactional public MedicalVisitResponse submit(String token, MedicalVisitRequest r){
  Hospital h=activeHospital(token); Employee e=employees.findById(r.employeeId()).orElseThrow(()->new NoSuchElementException("Employee not found")); validate(r);
  MedicalVisit v=new MedicalVisit(); v.setReferenceNumber("MV-"+UUID.randomUUID().toString().substring(0,8).toUpperCase()); v.setHospital(h); v.setEmployee(e); v.setVisitDate(r.visitDate()); v.setArrivalTime(r.arrivalTime()); v.setDischargeTime(r.dischargeTime()); v.setVisitType(r.visitType()); v.setRequiresExcuseDuty(r.requiresExcuseDuty()); v.setExcuseStartDate(r.requiresExcuseDuty()?r.excuseStartDate():null); v.setExcuseEndDate(r.requiresExcuseDuty()?r.excuseEndDate():null); v.setFitToReturn(r.fitToReturn()); v.setFollowUpDate(r.followUpDate()); v.setHospitalReference(blankToNull(r.hospitalReference())); v.setRemarks(blankToNull(r.remarks())); v.setSubmittedByName(r.submittedByName().trim()); v.setSubmittedByContact(r.submittedByContact().trim()); return MedicalVisitResponse.from(visits.save(v));
 }
 @Transactional public MedicalVisitResponse attach(String token,Long id,MultipartFile file) throws IOException {
  Hospital h=activeHospital(token); MedicalVisit v=visit(id); if(!v.getHospital().getId().equals(h.getId())) throw new SecurityException("Visit does not belong to this hospital");
  String type=file.getContentType(); if(type==null||!(type.equals("application/pdf")||type.startsWith("image/"))) throw new IllegalArgumentException("Only PDF and image attachments are allowed");
  if(file.getSize()>10*1024*1024) throw new IllegalArgumentException("Attachment must not exceed 10 MB");
  v.setAttachmentPath(files.storeFile(file,"medical-visits/"+id)); v.setAttachmentName(file.getOriginalFilename()); v.setAttachmentContentType(type); return MedicalVisitResponse.from(visits.save(v));
 }
 @Transactional(readOnly=true) public List<MedicalVisitResponse> list(String status,String search){ MedicalVisit.VisitStatus s=blankToNull(status)==null?null:MedicalVisit.VisitStatus.valueOf(status.toUpperCase()); return visits.search(s,blankToNull(search)).stream().map(MedicalVisitResponse::from).toList(); }
 @Transactional(readOnly=true) public MedicalVisitResponse get(Long id){ return MedicalVisitResponse.from(visit(id)); }
 @Transactional public MedicalVisitResponse decide(Long id,boolean verified,MedicalVisitDecisionRequest r,Authentication auth){
  MedicalVisit v=visit(id); if(v.getStatus()!=MedicalVisit.VisitStatus.PENDING_VERIFICATION) throw new IllegalStateException("This visit has already been reviewed");
  if(!verified && (r==null||blankToNull(r.hrComment())==null)) throw new IllegalArgumentException("A rejection reason is required");
  v.setStatus(verified?MedicalVisit.VisitStatus.VERIFIED:MedicalVisit.VisitStatus.REJECTED); v.setHrComment(r==null?null:blankToNull(r.hrComment())); v.setPaidExcuseDuty(verified&&r!=null?r.paidExcuseDuty():null); v.setReviewedBy(auth.getName()); v.setReviewedAt(LocalDateTime.now()); return MedicalVisitResponse.from(visits.save(v));
 }
 @Transactional(readOnly=true) public List<MedicalVisitResponse> mine(Authentication auth){ User u=users.findByUserName(auth.getName()).or(()->users.findByEmail(auth.getName())).orElseThrow(()->new NoSuchElementException("User not found")); if(u.getEmployee()==null) throw new IllegalStateException("Your account is not linked to an employee"); return visits.findByEmployeeIdOrderByVisitDateDescCreatedAtDesc(u.getEmployee().getId()).stream().map(MedicalVisitResponse::from).toList(); }
 public Resource attachment(Long id){ MedicalVisit v=visit(id); if(v.getAttachmentPath()==null) throw new NoSuchElementException("Attachment not found"); return files.loadFileAsResource(v.getAttachmentPath()); }
 public MedicalVisit attachmentInfo(Long id){ return visit(id); }
 private Hospital hospital(Long id){ return hospitals.findById(id).orElseThrow(()->new NoSuchElementException("Hospital not found")); }
 private Hospital activeHospital(String token){ return hospitals.findByAccessTokenAndStatus(token,Hospital.HospitalStatus.ACTIVE).orElseThrow(()->new NoSuchElementException("Invalid or revoked hospital link")); }
 private MedicalVisit visit(Long id){ return visits.findById(id).orElseThrow(()->new NoSuchElementException("Medical visit not found")); }
 private void validate(MedicalVisitRequest r){ if(r.requiresExcuseDuty()&&(r.excuseStartDate()==null||r.excuseEndDate()==null)) throw new IllegalArgumentException("Both excuse-duty dates are required"); if(r.requiresExcuseDuty()&&r.excuseEndDate().isBefore(r.excuseStartDate())) throw new IllegalArgumentException("Excuse-duty end date cannot be before start date"); if(r.dischargeTime()!=null&&r.arrivalTime()!=null&&r.dischargeTime().isBefore(r.arrivalTime())) throw new IllegalArgumentException("Discharge time cannot be before arrival time"); }
 private Map<String,Object> employeeMap(Employee e){ Map<String,Object> m=new LinkedHashMap<>(); m.put("id",e.getId());m.put("firstName",e.getFirstName());m.put("lastName",e.getLastName());m.put("displayName",((e.getFirstName()==null?"":e.getFirstName())+" "+(e.getLastName()==null?"":e.getLastName())).trim());m.put("tagNumber",e.getTagNumber());m.put("department",e.getDepartment());return m; }
 private String blankToNull(String s){ return s==null||s.isBlank()?null:s.trim(); }
}
