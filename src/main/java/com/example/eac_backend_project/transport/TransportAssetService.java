package com.example.eac_backend_project.transport;
import com.example.eac_backend_project.entity.transport.*;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.files.*;
import com.example.eac_backend_project.repository.transport.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.time.*;
import java.io.*;
import java.security.MessageDigest;
import static com.example.eac_backend_project.transport.TransportValues.*;
import static com.example.eac_backend_project.transport.TransportAccess.fail;

@Service @Transactional
public class TransportAssetService {
 @Autowired TransportAssets assets;
 @Autowired TransportManagedFiles files;
 @Autowired TransportCoreService core;
 @Autowired TransportAccess access;
 @Autowired TransportVehicleProfiles profiles;
 @Autowired TransportDriverProfiles drivers;
 @Autowired TransportFaults faults;
 @Autowired TransportWorkOrders orders;
 @Autowired TransportInspections inspections;
 @Autowired TransportProcurementService procurement;
 @Autowired FuelRepository fuel;
 @Autowired VehicleRepository vehicles;
 @Autowired TransportFuelDetails fuelDetails;
 static final Set<String> PURPOSES=Set.of("VEHICLE_IMAGE","DOCUMENT","FUEL_RECEIPT","FAULT","INSPECTION","WORK_ORDER","PROCUREMENT_INVOICE");
 private Vehicles authorize(Long vehicleId,String purpose,Long reference,boolean write){if(!PURPOSES.contains(purpose))throw fail(400,"Unsupported attachment purpose");User user=access.current();Vehicles vehicle=vehicles.findById(vehicleId).orElseThrow(()->fail(404,"Vehicle unavailable"));if(write&&vehicle.getArchivedAt()!=null)throw fail(409,"Restore the vehicle before changing attachments");
  if("PROCUREMENT_INVOICE".equals(purpose)){if(reference==null)throw fail(400,"Choose the procurement request");var r=procurement.request(reference);if(!r.vehicle.getId().equals(vehicleId)||write&&!procurement.purchaser(r,user))throw fail(403,"Invoice is outside your procurement assignment");if(write&&!Set.of("APPROVED","SENT_TO_PROCUREMENT","PURCHASING","PARTIALLY_RECEIVED","RECEIVED").contains(r.status))throw fail(409,"An invoice must belong to an approved purchase");return vehicle;}
  access.vehicle(vehicle);if(!write)return vehicle;
  switch(purpose){case "VEHICLE_IMAGE","DOCUMENT"->access.require("TRANSPORT_MANAGE_VEHICLES");case "FUEL_RECEIPT"->{if(reference==null)throw fail(400,"Save the fuel entry before attaching its receipt");Fuel entry=fuel.findById(reference).filter(f->f.getArchivedAt()==null).orElseThrow(()->fail(404,"Fuel entry unavailable"));core.fuelPermission(entry,true);if(!core.resolveVehicle(entry.getVehicle()).getId().equals(vehicleId))throw fail(400,"Fuel entry belongs to another vehicle");}case "FAULT"->{if(reference==null)throw fail(400,"Choose the fault report");TransportFault f=faults.findById(reference).orElseThrow(()->fail(404,"Fault unavailable"));if(!f.vehicle.getId().equals(vehicleId)||!access.allows(user,"TRANSPORT_REVIEW_FAULTS")&&!f.reporter.getId().equals(user.getId()))throw fail(403,"You may only attach to your own fault report");}case "WORK_ORDER"->{access.require("TRANSPORT_MANAGE_MAINTENANCE");if(reference==null||orders.findById(reference).filter(o->o.vehicle.getId().equals(vehicleId)).isEmpty())throw fail(400,"Choose this vehicle's work order");}case "INSPECTION"->{if(reference==null)throw fail(400,"Choose an inspection");var i=inspections.findById(reference).orElseThrow(()->fail(404,"Inspection unavailable"));if(!i.vehicle.getId().equals(vehicleId)||!access.allows(user,"TRANSPORT_MANAGE_MAINTENANCE")&&!i.inspector.getId().equals(user.getId()))throw fail(403,"You may only attach to your own inspection");}default->throw fail(400,"Unsupported attachment purpose");}return vehicle;
 }
 private Long reference(TransportAsset a){return a.fuel!=null?a.fuel.getId():a.fault!=null?a.fault.id:a.workOrder!=null?a.workOrder.id:a.inspection!=null?a.inspection.id:a.procurement!=null?a.procurement.id:null;}
 private void link(TransportAsset a,Long reference){if(reference==null)return;switch(a.purpose){case "FUEL_RECEIPT"->{a.fuel=fuel.findById(reference).orElseThrow();}case "FAULT"->{a.fault=faults.findById(reference).orElseThrow();}case "WORK_ORDER"->{a.workOrder=orders.findById(reference).orElseThrow();}case "INSPECTION"->{a.inspection=inspections.findById(reference).orElseThrow();}case "PROCUREMENT_INVOICE"->{a.procurement=procurement.request(reference);}default->{}}}
 Map<String,Object> describe(TransportAsset a){boolean readable=!a.removed&&a.file!=null&&files.readable(a.file.getId());Map<String,Object> result=row(a);boolean own=FileSuiteService.admin(access.current());result.putAll(map("vehicleId",a.vehicle.getId(),"purpose",a.purpose,"referenceId",reference(a),"fileId",readable?a.file.getId():null,"name",readable||own?a.name:"Unavailable attachment","mediaType",readable||own?a.mediaType:null,"sizeBytes",readable||own?a.sizeBytes:null,"readable",readable,"removed",a.removed,"contentUnavailable",a.file==null));return result;}
 public List<Map<String,Object>> list(Long vehicleId,String purpose,Long reference){if(purpose==null)core.vehicle(vehicleId);else authorize(vehicleId,purpose,reference,false);return assets.findByVehicle_IdAndRemovedFalse(vehicleId).stream().filter(a->(purpose==null||purpose.equals(a.purpose))&&(reference==null||reference.equals(reference(a)))).map(this::describe).toList();}
 public Map<String,Object> upload(Long vehicleId,String purpose,Long reference,MultipartFile part,String requestKey,boolean shareWithAssignedDriver)throws IOException{
  Vehicles vehicle=authorize(vehicleId,purpose,reference,true);vehicles.lock(vehicleId).orElseThrow();if(requestKey==null||requestKey.length()<10||requestKey.length()>120||!requestKey.matches("[A-Za-z0-9._:-]+"))throw fail(400,"A stable upload Idempotency-Key is required");TransportFileValidation.Validated checked=TransportFileValidation.validate(part,"VEHICLE_IMAGE".equals(purpose),false);String contentHash;try{contentHash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(checked.bytes()));}catch(Exception ex){throw new IllegalStateException(ex);}String requestHash=hash(vehicleId+":"+purpose+":"+reference+":"+checked.name()+":"+contentHash);var previous=assets.findByRequestKey(requestKey);if(previous.isPresent()){var old=previous.get();if(old.createdBy==null||!old.createdBy.getId().equals(access.current().getId())||!Objects.equals(old.requestHash,requestHash))throw fail(409,"Upload key was already used for a different file");return describe(old);}
  FileItem file=files.upload(TransportFileValidation.part(checked));TransportAsset a=new TransportAsset();a.vehicle=vehicle;a.file=file;a.originalFileId=file.getId();a.purpose=purpose;a.name=checked.name();a.mediaType=checked.mediaType();a.sizeBytes=checked.bytes().length;a.createdBy=access.current();a.requestKey=requestKey;a.requestHash=requestHash;link(a,reference);assets.saveAndFlush(a);
  if("VEHICLE_IMAGE".equals(purpose)){var p=core.profile(vehicle);if(p.primaryImage==null)p.primaryImage=file;}
  if("FUEL_RECEIPT".equals(purpose)){var detail=fuelDetails.findByFuel_Id(reference).orElseThrow();if(detail.receipt!=null)detail.receipt.removed=true;detail.receipt=a;}
  boolean shared=false;if(shareWithAssignedDriver){var recipient=profiles.findByVehicle_Id(vehicleId).filter(p->p.driver!=null).flatMap(p->drivers.findByDriver_Id(p.driver.getId())).filter(p->p.active&&p.account!=null).map(p->p.account);if(recipient.isPresent()&&!recipient.get().getId().equals(access.current().getId())){shared=files.shareNew(file.getId(),recipient.get());}}
  core.audit(vehicle,"ATTACHMENT_UPLOADED","Original file added to "+purpose,"asset:"+a.id);Map<String,Object> result=describe(a);result.put("sharedWithAssignedDriver",shared);return result;
 }
 public Map<String,Object> attachExisting(Long vehicleId,String purpose,Long reference,JsonNode data){Vehicles vehicle=authorize(vehicleId,purpose,reference,true);vehicles.lock(vehicleId).orElseThrow();Long id=id(data,"fileId",true);var view=files.metadata(id);if(view.folder())throw fail(400,"Attach a file, not a folder");if("VEHICLE_IMAGE".equals(purpose)&&!Set.of("image/jpeg","image/png","image/webp").contains(view.mediaType()))throw fail(400,"Choose a JPG, PNG or WebP image");var previous=assets.findByVehicle_IdAndRemovedFalse(vehicleId).stream().filter(a->a.file!=null&&a.file.getId().equals(id)&&purpose.equals(a.purpose)&&Objects.equals(reference,reference(a))).findFirst();if(previous.isPresent())return describe(previous.get());TransportAsset a=new TransportAsset();a.vehicle=vehicle;a.file=new FileItem();a.file.setId(id);a.originalFileId=id;a.name=view.name();a.mediaType=view.mediaType();a.sizeBytes=view.sizeBytes();a.purpose=purpose;a.createdBy=access.current();link(a,reference);assets.saveAndFlush(a);if("FUEL_RECEIPT".equals(purpose)){var detail=fuelDetails.findByFuel_Id(reference).orElseThrow();if(detail.receipt!=null)detail.receipt.removed=true;detail.receipt=a;}core.audit(vehicle,"MANAGED_FILE_LINKED","File linked from File Management","asset:"+a.id);return describe(a);}
 public FileSuiteService.Download download(Long id){TransportAsset a=assets.findById(id).filter(item->!item.removed&&item.file!=null).orElseThrow(()->fail(404,"Attachment unavailable"));authorize(a.vehicle.getId(),a.purpose,reference(a),false);return files.download(a.file.getId());}
 public void remove(Long id){TransportAsset a=assets.findById(id).orElseThrow(()->fail(404,"Attachment unavailable"));authorize(a.vehicle.getId(),a.purpose,reference(a),true);vehicles.lock(a.vehicle.getId()).orElseThrow();a=assets.lock(id).orElseThrow();a.removed=true;var profile=profiles.findByVehicle_Id(a.vehicle.getId());if(profile.isPresent()&&profile.get().primaryImage!=null&&a.file!=null&&profile.get().primaryImage.getId().equals(a.file.getId()))profile.get().primaryImage=null;if(a.fuel!=null){var detail=fuelDetails.findByFuel_Id(a.fuel.getId());if(detail.isPresent()&&detail.get().receipt!=null&&detail.get().receipt.id.equals(a.id))detail.get().receipt=null;}core.audit(a.vehicle,"ATTACHMENT_UNLINKED","File link removed from Transport; the original remains in File Management","asset:"+a.id);}
 public void primary(Long vehicleId,Long assetId){access.require("TRANSPORT_MANAGE_VEHICLES");Vehicles vehicle=core.lockedVehicle(vehicleId);TransportAsset a=assets.findById(assetId).filter(item->!item.removed&&item.file!=null&&item.vehicle.getId().equals(vehicleId)&&"VEHICLE_IMAGE".equals(item.purpose)).orElseThrow(()->fail(404,"Vehicle image unavailable"));files.metadata(a.file.getId());core.profile(vehicle).primaryImage=a.file;core.audit(vehicle,"PRIMARY_IMAGE_CHANGED","Vehicle primary image updated","asset:"+assetId);}
}
