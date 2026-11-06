package com.example.eac_backend_project.transport;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import java.util.*;
import java.io.IOException;

/** Transport actions retain authorization and aggregate locking in domain services. */
@RestController
@RequestMapping("/api/transport")
public class TransportController {
 @Autowired TransportCoreService core;
 @Autowired TransportWorkflows work;
 @Autowired TransportProcurementService procurement;
 @Autowired TransportDriverAccounts drivers;
 @Autowired com.example.eac_backend_project.Service.transport.DriverService legacyDrivers;
 @DeleteMapping("/drivers/{id}") public void archiveDriver(@PathVariable Long id){if(!legacyDrivers.deleteDrivers(id))throw TransportAccess.fail(404,"Driver unavailable");}
 @Autowired TransportPolicyService policy;
 @Autowired TransportAlerts alerts;
 @Autowired TransportAssetService assets;
 @Autowired com.example.eac_backend_project.Service.transport.AttachmentService legacyAttachments;
 @GetMapping("/vehicles/{id}/legacy-attachments") public Object legacyFiles(@PathVariable Long id){return legacyAttachments.getAttachmentsByVehicleId(id);}
 @GetMapping("/legacy-attachments/{id}/download") public ResponseEntity<byte[]> legacyFile(@PathVariable Long id)throws IOException{var metadata=legacyAttachments.getAttachment(id).orElseThrow(()->TransportAccess.fail(404,"Attachment unavailable"));return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox; default-src 'none'").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(metadata.getFileName(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).contentType(MediaType.APPLICATION_OCTET_STREAM).body(legacyAttachments.getFileData(id));}
 @Autowired TransportAccess access;
 @Autowired TransportPermissionService permissionSettings;
 @GetMapping("/permissions") public Object permissions(){return permissionSettings.get();}
 @PutMapping("/permissions/{code}") public Object permissions(@PathVariable String code,@RequestBody JsonNode data){return permissionSettings.save(code,data);}
 @Autowired TransportReportService reports;
 @Autowired TransportDashboardService dashboard;
 @GetMapping("/dashboard") public Object dashboard(){return dashboard.dashboard();}
 @GetMapping("/documents") public Object documents(@RequestParam(defaultValue="false") boolean history){return core.documents(null,history);}
 @Autowired TransportReportExport reportExport;
 @GetMapping("/report-types") public Object reportTypes(){return reports.types();}
 @GetMapping("/reports/{type}") public Object report(@PathVariable String type,@RequestParam Map<String,String> filters){return reports.report(type,filters);}
 @GetMapping("/reports/{type}/export") public ResponseEntity<byte[]> export(@PathVariable String type,@RequestParam(defaultValue="csv") String format,@RequestParam Map<String,String> filters)throws IOException{var query=new LinkedHashMap<>(filters);query.remove("format");var file=reportExport.generate(reports.report(type,query),format);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"transport-"+type.toLowerCase(Locale.ROOT)+"."+file.extension()+"\"").contentType(MediaType.parseMediaType(file.mediaType())).body(file.bytes());}
 @Autowired com.example.eac_backend_project.Service.transport.VehicleService vehicles;
 @Autowired TransportVehicleCommands vehicleCommands;
 @Autowired TransportFuelCommands fuelCommands;
 @Autowired com.example.eac_backend_project.Service.transport.MaintenanceService maintenanceEntries;
 @GetMapping("/maintenance") public Object maintenance(){return maintenanceEntries.getAllMaintenanceEntries().stream().map(core::maintenanceView).toList();}
 @PostMapping("/maintenance") public Object maintenance(@RequestBody com.example.eac_backend_project.entity.transport.Maintenance entry){return core.maintenanceView(maintenanceEntries.createMaintenance(entry));}
 @PutMapping("/maintenance/{id}") public Object maintenance(@PathVariable Long id,@RequestBody com.example.eac_backend_project.entity.transport.Maintenance entry){var result=maintenanceEntries.updateMaintenance(id,entry);if(result==null)throw TransportAccess.fail(404,"Maintenance entry unavailable");return core.maintenanceView(result);}
 @DeleteMapping("/maintenance/{id}") public void archiveMaintenance(@PathVariable Long id){if(!maintenanceEntries.deleteMaintenance(id))throw TransportAccess.fail(404,"Maintenance entry unavailable");}
 @GetMapping("/fuel") public Object fuel(){return fuelCommands.list();}
 @GetMapping("/fuel-driver-options") public Object fuelDrivers(){return fuelCommands.driverOptions();}
 @PostMapping("/fuel") public Object fuel(@RequestBody JsonNode data,@RequestHeader("Idempotency-Key") String key){return fuelCommands.save(null,data,key);}
 @PutMapping("/fuel/{id}") public Object fuel(@PathVariable Long id,@RequestBody JsonNode data){return fuelCommands.save(id,data,null);}
 @DeleteMapping("/fuel/{id}") public void archiveFuel(@PathVariable Long id){fuelCommands.archive(id);}
 @GetMapping("/vehicles/{id}") public Object vehicle(@PathVariable Long id){var result=vehicles.getVehicleById(id);if(result==null)throw TransportAccess.fail(404,"Vehicle unavailable");return result;}
 @PostMapping("/vehicles") public Object vehicle(@RequestBody com.example.eac_backend_project.dto.VehicleDTO data){return vehicles.createVehicle(data);}
 @PatchMapping("/vehicles/{id}") public Object vehicle(@PathVariable Long id,@RequestBody JsonNode data){return vehicleCommands.patch(id,data);}
 @DeleteMapping("/vehicles/{id}") public void archiveVehicle(@PathVariable Long id){if(!vehicles.deleteVehicle(id))throw TransportAccess.fail(404,"Vehicle unavailable");}
 @Autowired com.example.eac_backend_project.Service.transport.TransportUserService directory;
 @GetMapping("/directory") public Object directory(){return directory.getAllUsers();}
 @PostMapping("/directory") public Object directory(@RequestBody com.example.eac_backend_project.entity.transport.Users data){return directory.createUser(data);}
 @PutMapping("/directory/{id}") public Object directory(@PathVariable Long id,@RequestBody com.example.eac_backend_project.entity.transport.Users data){var result=directory.updateUser(id,data);if(result==null)throw TransportAccess.fail(404,"Team member unavailable");return result;}
 @DeleteMapping("/directory/{id}") public void archiveDirectory(@PathVariable Long id){if(!directory.deleteUser(id))throw TransportAccess.fail(404,"Team member unavailable");}
 @ModelAttribute public void authorizeModule(){access.requireEntry();}

 @GetMapping("/context") public Object context(){return core.context();}
 @GetMapping("/vehicles") public Object vehicles(@RequestParam(defaultValue="false") boolean includeArchived){return vehicles.getAllVehicles(includeArchived);}
 @PostMapping("/vehicles/{id}/restore") public Object restoreVehicle(@PathVariable Long id){core.restoreVehicle(id);return vehicles.getVehicleById(id);}
 @GetMapping("/employees") public Object employees(){return core.employeeDirectory();}
 @GetMapping("/drivers") public Object drivers(@RequestParam(defaultValue="false") boolean includeArchived){return drivers.list(includeArchived);}
 @PostMapping("/drivers/{id}/restore") public Object restoreDriver(@PathVariable Long id){return drivers.restore(id);}
 @PostMapping("/drivers") public Object driver(@RequestBody JsonNode d){return drivers.save(null,d);}
 @PostMapping(value="/drivers/with-photo",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object driverPhoto(@RequestPart("driver") JsonNode data,@RequestPart("photo") MultipartFile photo)throws IOException{return drivers.saveWithPhoto(null,data,photo);}
 @PutMapping(value="/drivers/{id}/with-photo",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object driverPhoto(@PathVariable Long id,@RequestPart("driver") JsonNode data,@RequestPart("photo") MultipartFile photo)throws IOException{return drivers.saveWithPhoto(id,data,photo);}
 @GetMapping("/drivers/{id}/photo") public ResponseEntity<FileSystemResource> driverPhoto(@PathVariable Long id){var photo=drivers.photo(id);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").contentType(MediaType.parseMediaType(photo.mediaType())).body(new FileSystemResource(photo.path()));}
 @PostMapping(value="/fuel/with-receipt",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object fuelReceipt(@RequestPart("purchase") JsonNode data,@RequestPart("receipt") MultipartFile receipt,@RequestHeader("Idempotency-Key") String key)throws IOException{return fuelCommands.saveWithReceipt(null,data,key,receipt);}
 @PutMapping(value="/fuel/{id}/with-receipt",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object fuelReceipt(@PathVariable Long id,@RequestPart("purchase") JsonNode data,@RequestPart("receipt") MultipartFile receipt,@RequestHeader("Idempotency-Key") String key)throws IOException{return fuelCommands.saveWithReceipt(id,data,key,receipt);}
 @PutMapping("/drivers/{id}") public Object driver(@PathVariable Long id,@RequestBody JsonNode d){return drivers.save(id,d);}
 @PostMapping("/drivers/{id}/invitation") public void invitation(@PathVariable Long id){drivers.invite(id);}
 @GetMapping("/driver-accounts") public Object accounts(@RequestParam Long employeeId){return drivers.availableAccounts(employeeId);}
 @GetMapping("/vehicles/{id}/odometer") public Object mileage(@PathVariable Long id){return core.odometer(id);}
 @PostMapping("/vehicles/{id}/odometer") public Object mileage(@PathVariable Long id,@RequestBody JsonNode d){return core.recordOdometer(id,d);}
 @GetMapping("/vehicles/{id}/timeline") public Object timeline(@PathVariable Long id){return core.timeline(id);}
 @GetMapping("/vehicles/{id}/documents") public Object documents(@PathVariable Long id,@RequestParam(defaultValue="false") boolean history){return core.documents(id,history);}
 @PostMapping("/documents") public Object document(@RequestBody JsonNode d){return core.saveDocument(d);}
 @GetMapping("/documents/{id}/download") public ResponseEntity<FileSystemResource> documentFile(@PathVariable Long id){var file=core.downloadDocument(id);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox; default-src 'none'").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.name(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).contentType(MediaType.APPLICATION_OCTET_STREAM).body(new FileSystemResource(file.path()));}
 @PostMapping("/vehicles/{id}/release") public Object release(@PathVariable Long id,@RequestBody JsonNode d){return work.release(id,d);}
 @GetMapping("/components") public Object components(@RequestParam(required=false) Long vehicleId){return work.components(vehicleId);}
 @PostMapping("/components") public Object component(@RequestBody JsonNode d){return work.saveComponent(null,d);}
 @PostMapping("/components/bulk") public Object componentPlans(@RequestBody JsonNode d){return work.bulkComponents(d);}
 @PutMapping("/components/{id}") public Object component(@PathVariable Long id,@RequestBody JsonNode d){return work.saveComponent(id,d);}
 @GetMapping("/faults") public Object faults(@RequestParam(required=false) Long vehicleId){return work.faults(vehicleId);}
 @PostMapping("/faults") public Object fault(@RequestBody JsonNode d,@RequestHeader("Idempotency-Key") String key){return work.reportFault(d,key);}
 @PostMapping("/faults/{id}/review") public Object review(@PathVariable Long id,@RequestBody JsonNode d){return work.reviewFault(id,d);}
 @GetMapping("/work-orders") public Object orders(@RequestParam(required=false) Long vehicleId){return work.workOrders(vehicleId);}
 @PostMapping("/work-orders") public Object order(@RequestBody JsonNode d){return work.saveOrder(null,d);}
 @PutMapping("/work-orders/{id}") public Object order(@PathVariable Long id,@RequestBody JsonNode d){return work.saveOrder(id,d);}
 @PostMapping("/work-orders/{id}/transition") public Object orderAction(@PathVariable Long id,@RequestBody JsonNode d){return work.transitionOrder(id,d);}
 @GetMapping("/inspection-templates") public Object templates(){return work.templates();}
 @PostMapping("/inspection-templates") public Object template(@RequestBody JsonNode d){return work.saveTemplate(null,d);}
 @PutMapping("/inspection-templates/{id}") public Object template(@PathVariable Long id,@RequestBody JsonNode d){return work.saveTemplate(id,d);}
 @GetMapping("/inspection-assignments") public Object assignments(@RequestParam(required=false) Long vehicleId){return work.assignments(vehicleId);}
 @PostMapping("/inspection-assignments") public Object assignment(@RequestBody JsonNode d){return work.assignTemplate(d);}
 @GetMapping("/inspections") public Object inspections(@RequestParam(required=false) Long vehicleId){return work.inspections(vehicleId);}
 @PostMapping("/inspections") public Object inspection(@RequestBody JsonNode d,@RequestHeader("Idempotency-Key") String key){return work.submitInspection(d,key);}
 @GetMapping("/procurement") public Object purchases(@RequestParam(required=false) Long vehicleId){return procurement.list(vehicleId);}
 @GetMapping("/procurement/{id}") public Object purchase(@PathVariable Long id){return procurement.get(id);}
 @PostMapping("/procurement") public Object purchase(@RequestBody JsonNode d,@RequestHeader("Idempotency-Key") String key){return procurement.save(null,d,key);}
 @PutMapping("/procurement/{id}") public Object purchase(@PathVariable Long id,@RequestBody JsonNode d,@RequestHeader("Idempotency-Key") String key){return procurement.save(id,d,key);}
 @PostMapping("/procurement/{id}/transition") public Object purchaseAction(@PathVariable Long id,@RequestBody JsonNode d){return procurement.transition(id,d);}
 @PostMapping("/procurement/{id}/receipts") public Object receive(@PathVariable Long id,@RequestBody JsonNode d,@RequestHeader("Idempotency-Key") String key){return procurement.receive(id,d,key);}
 @GetMapping("/procurement-officers") public Object officers(){return procurement.officers();}
 @GetMapping("/settings") public Object settings(){return policy.get();}
 @PutMapping("/settings") public Object settings(@RequestBody JsonNode d){return policy.save(d);}
 @GetMapping("/notifications") public Object notifications(@RequestParam(defaultValue="false") boolean deliveryLog){return alerts.list(deliveryLog);}
 @PostMapping("/notifications/{id}/read") public void read(@PathVariable Long id){alerts.read(id);}
 @PostMapping("/notifications/read-all") public void readAll(){alerts.readAll();}
 @GetMapping("/notification-history") public Object history(@RequestParam(defaultValue="false") boolean deliveryLog,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size,@RequestParam(required=false) String type,@RequestParam(defaultValue="false") boolean unreadOnly){return alerts.history(deliveryLog,page,size,type,unreadOnly);}
 @PostMapping("/notifications/{id}/retry") public void retry(@PathVariable Long id){alerts.retry(id);}
 @PostMapping("/settings/test-email") public Object test(@RequestBody JsonNode d){return alerts.test(d);}
 @GetMapping("/vehicles/{id}/assets") public Object assets(@PathVariable Long id,@RequestParam(required=false) String purpose,@RequestParam(required=false) Long referenceId){return assets.list(id,purpose,referenceId);}
 @PostMapping(value="/vehicles/{id}/assets",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Object upload(@PathVariable Long id,@RequestParam String purpose,@RequestParam(required=false) Long referenceId,@RequestPart MultipartFile file,@RequestHeader("Idempotency-Key") String key,@RequestParam(defaultValue="false") boolean shareWithAssignedDriver)throws IOException{return assets.upload(id,purpose,referenceId,file,key,shareWithAssignedDriver);}
 @PostMapping("/vehicles/{id}/assets/link") public Object link(@PathVariable Long id,@RequestParam String purpose,@RequestParam(required=false) Long referenceId,@RequestBody JsonNode d){return assets.attachExisting(id,purpose,referenceId,d);}
 @DeleteMapping("/assets/{id}") public void unlink(@PathVariable Long id){assets.remove(id);}
 @PostMapping("/vehicles/{id}/primary-image/{assetId}") public void primary(@PathVariable Long id,@PathVariable Long assetId){assets.primary(id,assetId);}
 @GetMapping("/assets/{id}/download") public ResponseEntity<FileSystemResource> download(@PathVariable Long id){var file=assets.download(id);return ResponseEntity.ok().header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox; default-src 'none'").cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.name(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).contentType(MediaType.APPLICATION_OCTET_STREAM).body(new FileSystemResource(file.path()));}
}
