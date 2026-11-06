package com.example.eac_backend_project.transport;

import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.transport.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Additive setup only: never invent accounts, dated readings or ambiguous assignments. */
@Component
public class TransportBootstrap implements ApplicationRunner {
 private final RoleRepository roles;
 private final EndpointPermissionRepository permissions;
 private final VehicleRepository vehicles;
 private final DriverRepository drivers;
 private final TransportVehicleProfiles profiles;
 private final TransportDriverProfiles driverProfiles;
 private final TransportPolicyService policy;
 @org.springframework.beans.factory.annotation.Autowired TransportDocuments documents;
 @org.springframework.beans.factory.annotation.Autowired FuelRepository fuel;
 @org.springframework.beans.factory.annotation.Autowired MaintenanaceRepository maintenance;
 @org.springframework.beans.factory.annotation.Autowired TransportFuelDetails fuelDetails;
 @org.springframework.beans.factory.annotation.Autowired TransportMaintenanceDetails maintenanceDetails;
 public TransportBootstrap(RoleRepository roles,EndpointPermissionRepository permissions,VehicleRepository vehicles,DriverRepository drivers,TransportVehicleProfiles profiles,TransportDriverProfiles driverProfiles,TransportPolicyService policy){this.roles=roles;this.permissions=permissions;this.vehicles=vehicles;this.drivers=drivers;this.profiles=profiles;this.driverProfiles=driverProfiles;this.policy=policy;}
 @Override @Transactional public void run(ApplicationArguments args){
  for(String name:List.of("ROLE_TRANSPORT_ADMIN","ROLE_DRIVER","ROLE_PROCUREMENT_OFFICER","ROLE_PROCUREMENT_MANAGER"))if(roles.findByName(name).isEmpty()){Role role=new Role();role.setName(name);roles.save(role);}
  var existing=permissions.findAll();
  for(String code:TransportAccess.CODES)if(existing.stream().noneMatch(p->code.equals(p.getDescription()))){EndpointPermission permission=new EndpointPermission();permission.setDescription(code);permission.setHttpMethod("POST");permission.setEndpointPattern("/api/transport/permissions/"+code);permission.setEnabled(true);permissions.save(permission);}
  policy.policy();
  for(var d:drivers.findAll())if(driverProfiles.findByDriver_Id(d.getId()).isEmpty()){var p=new TransportDriverProfile();p.driver=d;p.active=d.getArchivedAt()==null&&!"inactive".equalsIgnoreCase(d.getStatus());driverProfiles.save(p);}
  for(var v:vehicles.findAll())if(profiles.findByVehicle_Id(v.getId()).isEmpty()){var p=new TransportVehicleProfile();p.vehicle=v;String assigned=v.getAssignedDriver();if(assigned!=null&&!assigned.isBlank()){var matches=drivers.findAll().stream().filter(d->d.getArchivedAt()==null&&assigned.trim().equalsIgnoreCase(d.getDriverName())).toList();if(matches.size()==1)p.driver=matches.get(0);}p.operationalStatus="MAINTENANCE".equalsIgnoreCase(v.getStatus())?"UNDER_MAINTENANCE":"INACTIVE".equalsIgnoreCase(v.getStatus())?"INACTIVE":p.driver==null?"AVAILABLE":"ASSIGNED";profiles.save(p);}
  for(var v:vehicles.findAll()){var history=documents.findByVehicle_IdOrderByCreatedAtDesc(v.getId());for(String type:List.of("DVLA","INSURANCE")){var expiry="DVLA".equals(type)?v.getVehicleLicenseExpiry():v.getInsuranceExpiry();if(expiry!=null&&history.stream().noneMatch(d->type.equals(d.documentType))){var document=new TransportDocument();document.vehicle=v;document.documentType=type;document.expiryDate=expiry;document.referenceNumber="DVLA".equals(type)?v.getVehicleLicenseNumber():null;document.notes="Preserved from the existing vehicle record during Transport upgrade.";documents.save(document);}}}
  var fleet=vehicles.findAll();for(var entry:fuel.findAll())if(fuelDetails.findByFuel_Id(entry.getId()).isEmpty()){var owner=legacyVehicle(fleet,entry.getVehicle());if(owner!=null){var detail=new TransportFuelDetail();detail.fuel=entry;detail.vehicle=owner;if(entry.getDriver()!=null){var matches=drivers.findByDriverName(entry.getDriver());if(matches.size()==1)detail.driver=matches.get(0);}if(entry.getOdometer()!=null)try{var reading=new java.math.BigDecimal(entry.getOdometer());if(reading.signum()>=0)detail.odometer=reading;}catch(NumberFormatException ignored){}fuelDetails.save(detail);}}
  for(var entry:maintenance.findAll())if(maintenanceDetails.findByMaintenance_Id(entry.getId()).isEmpty()){var owner=legacyVehicle(fleet,entry.getVehicles());if(owner!=null){var detail=new TransportMaintenanceDetail();detail.maintenance=entry;detail.vehicle=owner;maintenanceDetails.save(detail);}}
 }
 private com.example.eac_backend_project.entity.transport.Vehicles legacyVehicle(List<com.example.eac_backend_project.entity.transport.Vehicles> fleet,String reference){if(reference==null||reference.isBlank())return null;String ref=reference.trim();var registrations=fleet.stream().filter(v->ref.equalsIgnoreCase(v.getDvla())).toList();if(registrations.size()==1)return registrations.get(0);var names=fleet.stream().filter(v->ref.equalsIgnoreCase(v.getName())).toList();return names.size()==1?names.get(0):null;}
}
