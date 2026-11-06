package com.example.eac_backend_project.transport;

import com.example.eac_backend_project.repository.transport.VehicleRepository;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Each vehicle is evaluated atomically; repeat reminders have durable period keys. */
@Component
public class TransportReminderJob {
 private static final Logger log=LoggerFactory.getLogger(TransportReminderJob.class);
 private final VehicleRepository vehicles;
 private final TransportDocuments documents;
 private final TransportComponents components;
 private final TransportPolicyService settings;
 private final TransportAlerts alerts;
 private final TransactionTemplate tx;
 @org.springframework.beans.factory.annotation.Autowired TransportDriverProfiles drivers;
 @org.springframework.beans.factory.annotation.Autowired com.example.eac_backend_project.repository.transport.DriverRepository driverRecords;
 public TransportReminderJob(VehicleRepository vehicles,TransportDocuments documents,TransportComponents components,TransportPolicyService settings,TransportAlerts alerts,PlatformTransactionManager manager){this.vehicles=vehicles;this.documents=documents;this.components=components;this.settings=settings;this.alerts=alerts;this.tx=new TransactionTemplate(manager);}
 @Scheduled(initialDelayString="${transport.reminders.initial-delay-ms:90000}",fixedDelayString="${transport.reminders.delay-ms:3600000}")
 public void evaluate(){var ids=tx.execute(s->vehicles.findAll().stream().filter(v->v.getArchivedAt()==null).map(v->v.getId()).toList());if(ids!=null)for(Long id:ids)try{evaluateVehicle(id);}catch(RuntimeException e){log.warn("Transport reminder evaluation failed for vehicle {} ({})",id,e.getClass().getSimpleName());}var driverIds=tx.execute(s->drivers.findByArchivedAtIsNullOrderByCreatedAtDesc().stream().filter(p->p.active).map(p->p.id).toList());if(driverIds!=null)for(Long id:driverIds)try{tx.executeWithoutResult(s->{var profile=drivers.findById(id).orElse(null);if(profile==null||profile.driver==null)return;driverRecords.lock(profile.driver.getId()).orElseThrow();if(!profile.active||profile.driver.getArchivedAt()!=null||"inactive".equalsIgnoreCase(profile.driver.getStatus())||profile.driver.getLicenseExpiry()==null)return;var policy=settings.policy();long days=ChronoUnit.DAYS.between(TransportValues.today(),profile.driver.getLicenseExpiry());Integer threshold=threshold(days,policy.licenseThresholds);if(threshold!=null)alerts.driverExpiry(profile,days,threshold,Math.floorDiv(TransportValues.today().toEpochDay(),policy.reminderDays));});}catch(RuntimeException e){log.warn("Transport licence reminder failed for profile {} ({})",id,e.getClass().getSimpleName());}}
 void evaluateVehicle(Long id){tx.executeWithoutResult(s->{var found=vehicles.lock(id);if(found.isEmpty()||found.get().getArchivedAt()!=null)return;var vehicle=found.get();var p=settings.policy();LocalDate today=TransportValues.today();long bucket=Math.floorDiv(today.toEpochDay(),p.reminderDays);
  for(var doc:documents.findByVehicle_IdOrderByCreatedAtDesc(id)){if(!doc.active||doc.archivedAt!=null||doc.expiryDate==null)continue;long days=ChronoUnit.DAYS.between(today,doc.expiryDate);String thresholds=switch(doc.documentType){case "DVLA"->p.dvlaThresholds;case "INSURANCE"->p.insuranceThresholds;default->p.roadworthyThresholds;};Integer threshold=threshold(days,thresholds);if(threshold==null)continue;String type=days<0?"DOCUMENT_EXPIRED":"DOCUMENT_EXPIRY";String body=vehicle.getDvla()+" — "+doc.documentType+" "+(days<0?"expired "+(-days)+" days ago":days==0?"expires today (valid through today)":"expires in "+days+" days")+". Expiry: "+doc.expiryDate+".";alerts.event(vehicle,type,"Vehicle document expiry",body,"document:"+doc.id,"/vehicles","document:"+doc.id+":"+doc.expiryDate+":"+threshold+":"+bucket,List.of());}
  for(var c:components.findByVehicle_IdAndArchivedAtIsNull(id)){Long days=c.nextServiceDate==null?null:ChronoUnit.DAYS.between(today,c.nextServiceDate);BigDecimal remaining=vehicle.getCurrentMileage()==null||c.nextServiceMileage==null?null:c.nextServiceMileage.subtract(BigDecimal.valueOf(vehicle.getCurrentMileage()));Integer dateThreshold=days==null?null:threshold(days,p.maintenanceDays);Integer kmThreshold=remaining==null?null:threshold(remaining,p.maintenanceKm);if(dateThreshold==null&&kmThreshold==null)continue;boolean overdue=days!=null&&days<0||remaining!=null&&remaining.signum()<0;String body=vehicle.getDvla()+" — "+c.name+". "+(remaining==null?"Mileage threshold not recorded.":remaining.toPlainString()+" km remaining.")+" "+(days==null?"Date threshold not recorded.":"Service date: "+c.nextServiceDate+" ("+days+" days remaining).");alerts.event(vehicle,overdue?"MAINTENANCE_OVERDUE":"MAINTENANCE_DUE","Maintenance attention required",body,"component:"+c.id,"/maintenance","component:"+c.id+":"+c.nextServiceDate+":"+c.nextServiceMileage+":"+dateThreshold+":"+kmThreshold+":"+bucket,List.of());}
 });}
 static Integer threshold(long remaining,String configured){var values=TransportPolicyService.thresholds(configured);if(remaining<0)return -1;return values.stream().filter(value->remaining<=value).min(Integer::compareTo).orElse(null);}
 static Integer threshold(BigDecimal remaining,String configured){if(remaining.signum()<0)return -1;return TransportPolicyService.thresholds(configured).stream().filter(value->remaining.compareTo(BigDecimal.valueOf(value))<=0).min(Integer::compareTo).orElse(null);}
}
