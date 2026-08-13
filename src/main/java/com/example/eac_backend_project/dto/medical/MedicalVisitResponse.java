package com.example.eac_backend_project.dto.medical;
import com.example.eac_backend_project.entity.medical.MedicalVisit;
import java.time.*;
public record MedicalVisitResponse(Long id, String referenceNumber, EmployeeSummary employee, HospitalSummary hospital,
 LocalDate visitDate, LocalTime arrivalTime, LocalTime dischargeTime, MedicalVisit.VisitType visitType,
 boolean requiresExcuseDuty, LocalDate excuseStartDate, LocalDate excuseEndDate, boolean fitToReturn,
 LocalDate followUpDate, String hospitalReference, String remarks, String submittedByName, String submittedByContact,
 MedicalVisit.VisitStatus status, String hrComment, Boolean paidExcuseDuty, String reviewedBy,
 LocalDateTime reviewedAt, String attachmentUrl, LocalDateTime createdAt) {
 public record EmployeeSummary(Long id, String displayName, String tagNumber, String department) {}
 public record HospitalSummary(Long id, String name, String code) {}
 public static MedicalVisitResponse from(MedicalVisit v) {
  var e=v.getEmployee(); var h=v.getHospital();
  return new MedicalVisitResponse(v.getId(),v.getReferenceNumber(),new EmployeeSummary(e.getId(),
   ((e.getFirstName()==null?"":e.getFirstName())+" "+(e.getLastName()==null?"":e.getLastName())).trim(),e.getTagNumber(),e.getDepartment()),
   new HospitalSummary(h.getId(),h.getName(),h.getCode()),v.getVisitDate(),v.getArrivalTime(),v.getDischargeTime(),v.getVisitType(),
   v.isRequiresExcuseDuty(),v.getExcuseStartDate(),v.getExcuseEndDate(),v.isFitToReturn(),v.getFollowUpDate(),v.getHospitalReference(),
   v.getRemarks(),v.getSubmittedByName(),v.getSubmittedByContact(),v.getStatus(),v.getHrComment(),v.getPaidExcuseDuty(),v.getReviewedBy(),
   v.getReviewedAt(),v.getAttachmentPath()==null?null:"/api/medical-visits/"+v.getId()+"/attachment",v.getCreatedAt());
 }
}
