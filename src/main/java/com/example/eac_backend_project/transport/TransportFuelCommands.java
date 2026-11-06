package com.example.eac_backend_project.transport;
import com.example.eac_backend_project.entity.transport.*;
import com.example.eac_backend_project.Service.transport.FuelService;
import com.example.eac_backend_project.repository.transport.DriverRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import static com.example.eac_backend_project.transport.TransportValues.*;
import static com.example.eac_backend_project.transport.TransportAccess.fail;

@Service @Transactional
public class TransportFuelCommands {
 @Transactional(rollbackFor=java.io.IOException.class)
 public Map<String,Object> saveWithReceipt(Long id,JsonNode data,String requestKey,org.springframework.web.multipart.MultipartFile receipt)throws java.io.IOException{
  var checked=TransportFileValidation.validate(receipt,false,false);
  String key=workflows.key(requestKey);
  var saved=save(id,data,key);Long fuelId=((Number)saved.get("id")).longValue();Long vehicleId=((Number)saved.get("vehicleId")).longValue();
  assets.upload(vehicleId,"FUEL_RECEIPT",fuelId,TransportFileValidation.part(checked),"fuel-receipt:"+hash(key),false);
  return describe(fuel.getFuelById(fuelId));
 }
 @Autowired FuelService fuel;
 @Autowired TransportCoreService core;
 @Autowired TransportAccess access;
 @Autowired TransportFuelDetails details;
 @Autowired TransportWorkflows workflows;
 @Autowired TransportAssetService assets;
 @Autowired DriverRepository drivers;
 private Map<String,Object> describe(Fuel entry){var detail=details.findByFuel_Id(entry.getId());Map<String,Object> result=map("id",entry.getId(),"vehicle",entry.getVehicle(),"driver",entry.getDriver(),"date",entry.getDate(),"litres",entry.getLitres(),"cost",entry.getCost(),"odometer",entry.getOdometer());detail.ifPresent(d->{result.putAll(map("revision",d.revision,"vehicleArchived",d.vehicle.getArchivedAt()!=null,"vehicleId",d.vehicle.getId(),"driverId",d.driver==null?null:d.driver.getId(),"pricePerLitre",d.pricePerLitre,"fuelType",d.fuelType,"station",d.station,"paymentMethod",d.paymentMethod,"transactionReference",d.transactionReference,"remarks",d.remarks,"createdById",d.createdBy==null?null:d.createdBy.getId(),"receipt",d.receipt==null?null:assets.describe(d.receipt)));});return result;}
 public List<Map<String,Object>> list(){return fuel.getAllFuelEntries().stream().map(this::describe).toList();}
 public Object driverOptions(){access.require("TRANSPORT_MANAGE_FUEL");return drivers.findAll().stream().filter(d->d.getArchivedAt()==null&&!"inactive".equalsIgnoreCase(d.getStatus())).map(d->map("id",d.getId(),"driverName",d.getDriverName())).toList();}
 public Map<String,Object> save(Long id,JsonNode data,String requestKey){var user=access.current();boolean manager=access.allows(user,"TRANSPORT_MANAGE_FUEL");Fuel existing=id==null?null:fuel.getFuelById(id);if(id!=null&&existing==null)throw fail(404,"Fuel entry unavailable");if(existing!=null)core.fuelPermission(existing,true);Long vehicleId=id(data,"vehicleId",true);var vehicle=core.lockedVehicle(vehicleId);if(existing!=null)details.findByFuel_Id(existing.getId()).filter(d->d.receipt!=null&&!d.vehicle.getId().equals(vehicleId)).ifPresent(d->{throw fail(409,"A purchase with a receipt cannot move to another vehicle; archive it and record the corrected purchase separately");});String key=id==null?workflows.key(requestKey):null;
  if(key!=null){var old=details.findByRequestKey(key);if(old.isPresent()){var d=old.get();if(d.createdBy==null||!d.createdBy.getId().equals(user.getId())||!Objects.equals(d.requestHash,workflows.fingerprint(data)))throw fail(409,"Submission key was used for another purchase");return describe(d.fuel);}}
  if(id!=null)details.findByFuel_Id(id).ifPresent(d->revision(d,data));
  var entry=new Fuel();entry.setVehicle(vehicle.getDvla());entry.setDate(date(data,"date",true));entry.setLitres(number(data,"litres",true,true).doubleValue());entry.setCost(number(data,"cost",true,true).doubleValue());var mileage=number(data,"odometer",false,false);entry.setOdometer(mileage==null?null:mileage.toPlainString());entry.setOdometerCorrectionReason(text(data,"odometerCorrectionReason",2000,false));Long driverId=id(data,"driverId",false);Drivers driver=null;if(manager&&driverId!=null)driver=drivers.findById(driverId).filter(d->d.getArchivedAt()==null).orElseThrow(()->fail(404,"Driver unavailable"));if(!manager)driver=access.driver(user).map(d->d.driver).orElseThrow(()->fail(403,"An assigned driver account is required"));entry.setDriver(driver==null?null:driver.getDriverName());if(manager&&existing!=null&&!data.has("driverId"))entry.setDriver(existing.getDriver());entry=id==null?fuel.createFuel(entry):fuel.updateFuel(id,entry);TransportFuelDetail d=details.findByFuel_Id(entry.getId()).orElseThrow();if(data.has("pricePerLitre"))d.pricePerLitre=number(data,"pricePerLitre",false,false);if(data.has("fuelType"))d.fuelType=text(data,"fuelType",40,false);if(data.has("station"))d.station=text(data,"station",180,false);if(data.has("paymentMethod"))d.paymentMethod=text(data,"paymentMethod",60,false);if(data.has("transactionReference"))d.transactionReference=text(data,"transactionReference",180,false);if(data.has("remarks"))d.remarks=text(data,"remarks",2000,false);if(key!=null){d.requestKey=key;d.requestHash=workflows.fingerprint(data);}details.saveAndFlush(d);return describe(entry);
 }
 public void archive(Long id){access.require("TRANSPORT_MANAGE_FUEL");var entry=fuel.getFuelById(id);if(entry==null)throw fail(404,"Fuel entry unavailable");var v=core.fuelVehicle(entry);core.lockedVehicle(v.getId());if(!fuel.deleteFuel(id))throw fail(404,"Fuel entry unavailable");core.audit(v,"FUEL_ARCHIVED","Fuel purchase archived; original receipt and audit history retained","fuel:"+id);}
}
