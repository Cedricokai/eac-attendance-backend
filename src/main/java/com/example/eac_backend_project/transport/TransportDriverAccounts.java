package com.example.eac_backend_project.transport;
import com.example.eac_backend_project.entity.transport.Drivers;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.transport.DriverRepository;
import com.example.eac_backend_project.Service.transport.DriverService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.security.SecureRandom;
import static com.example.eac_backend_project.transport.TransportValues.*;
import static com.example.eac_backend_project.transport.TransportAccess.fail;

@Service @Transactional
public class TransportDriverAccounts {
 @org.springframework.beans.factory.annotation.Autowired com.example.eac_backend_project.files.TransportManagedFiles managedFiles;
 @Transactional(rollbackFor=java.io.IOException.class)
 public Map<String,Object> saveWithPhoto(Long id,JsonNode data,org.springframework.web.multipart.MultipartFile photo)throws java.io.IOException{
  access.require("TRANSPORT_MANAGE_DRIVERS");
  var checked=TransportFileValidation.validate(photo,true,false);
  var saved=save(id,data);Long driverId=((Number)saved.get("id")).longValue();
  var profile=profiles.findByDriver_Id(driverId).orElseThrow();profile.photo=managedFiles.upload(TransportFileValidation.part(checked));profiles.saveAndFlush(profile);
  core.audit(null,"DRIVER_PHOTO_UPDATED","Driver profile photo saved in File Management; earlier originals retained","driver:"+driverId);
  return describe(profile.driver);
 }
 public com.example.eac_backend_project.files.FileSuiteService.Download photo(Long id){
  var driver=drivers.findById(id).filter(access::maySeeDriver).orElseThrow(()->fail(404,"Driver unavailable"));
  var profile=profiles.findByDriver_Id(driver.getId()).filter(p->p.photo!=null).orElseThrow(()->fail(404,"No driver photo recorded"));
  return managedFiles.download(profile.photo.getId());
 }
 @org.springframework.beans.factory.annotation.Autowired EndpointPermissionRepository filePermissions;
 private final DriverRepository drivers;private final DriverService legacy;private final TransportDriverProfiles profiles;private final UserRepository users;private final EmployeeRepository employees;private final RoleRepository roles;private final PageRepository pages;private final PasswordEncoder passwords;private final TransportAccess access;private final TransportCoreService core;private final TransportAlerts alerts;
 public TransportDriverAccounts(DriverRepository drivers,DriverService legacy,TransportDriverProfiles profiles,UserRepository users,EmployeeRepository employees,RoleRepository roles,PageRepository pages,PasswordEncoder passwords,TransportAccess access,TransportCoreService core,TransportAlerts alerts){this.drivers=drivers;this.legacy=legacy;this.profiles=profiles;this.users=users;this.employees=employees;this.roles=roles;this.pages=pages;this.passwords=passwords;this.access=access;this.core=core;this.alerts=alerts;}
 Map<String,Object> describe(Drivers driver){var detail=profiles.findByDriver_Id(driver.getId());Map<String,Object> result=map("id",driver.getId(),"driverName",driver.getDriverName(),"licenseNumber",driver.getLicenseNumber(),"licenseExpiry",driver.getLicenseExpiry(),"contact",driver.getContact(),"status",driver.getStatus(),"archivedAt",driver.getArchivedAt());detail.ifPresent(p->result.putAll(map("profileId",p.id,"revision",p.revision,"email",p.email,"employeeRecordId",p.employee==null?null:p.employee.getId(),"employeeId",p.employee==null?null:p.employee.getEmployeeId(),"licenseClass",p.licenseClass,"department",p.department,"project",p.project,"active",p.active,"notificationsEnabled",p.notificationsEnabled,"accountId",p.account==null?null:p.account.getId(),"username",p.account==null?null:p.account.getUserName(),"accountEnabled",p.account==null?null:p.account.getEnabled(),"photoFileId",p.photo!=null&&managedFiles.readable(p.photo.getId())?p.photo.getId():null,"managedAccount",p.managedAccount)));return result;}
 public List<Map<String,Object>> list(){return list(false);} public List<Map<String,Object>> list(boolean includeArchived){if(includeArchived){access.require("TRANSPORT_MANAGE_DRIVERS");return drivers.findAll().stream().map(this::describe).toList();}return drivers.findAll().stream().filter(access::maySeeDriver).map(this::describe).toList();} public Map<String,Object> restore(Long id){access.require("TRANSPORT_MANAGE_DRIVERS");var driver=drivers.lock(id).orElseThrow(()->fail(404,"Driver unavailable"));if(driver.getArchivedAt()==null)throw fail(409,"Driver is already in the register");driver.setArchivedAt(null);driver.setStatus("inactive");profiles.findByDriver_Id(id).ifPresent(p->p.active=false);drivers.saveAndFlush(driver);core.audit(null,"DRIVER_RESTORED","Driver restored as inactive; account activation requires a separate explicit edit","driver:"+id);return describe(driver);}
 public Map<String,Object> save(Long id,JsonNode data){access.require("TRANSPORT_MANAGE_DRIVERS");if(bool(data,"createAccount",false))throw fail(400,"Use the employee's existing EAC account. Transport does not create a separate login");Drivers d;if(id==null){d=new Drivers();}else d=drivers.lock(id).filter(x->x.getArchivedAt()==null).orElseThrow(()->fail(404,"Driver unavailable"));
  Long employeeId=id(data,"employeeRecordId",false);var employee=employeeId==null?null:employees.findById(employeeId).filter(e->!Boolean.FALSE.equals(e.getActive())).orElseThrow(()->fail(404,"Active employee unavailable"));
  d.setDriverName(text(data,"driverName",180,true));d.setLicenseNumber(text(data,"licenseNumber",120,true));d.setLicenseExpiry(date(data,"licenseExpiry",false));d.setContact(text(data,"contact",50,false));d.setStatus(choice(data,"status",Set.of("active","inactive","on_leave"),"active"));drivers.saveAndFlush(d);
  TransportDriverProfile p=profiles.findByDriver_Id(d.getId()).orElseGet(TransportDriverProfile::new);if(p.id!=null)revision(p,data);p.driver=d;if(employee!=null){if(p.account!=null&&(p.account.getEmployee()==null||!p.account.getEmployee().getId().equals(employee.getId())))throw fail(409,"The linked EAC account belongs to a different employee");p.employee=employee;}p.email=email(text(data,"email",254,false));p.licenseClass=text(data,"licenseClass",60,false);p.department=text(data,"department",120,false);p.project=text(data,"project",180,false);p.active=bool(data,"active",!"inactive".equalsIgnoreCase(d.getStatus()));p.notificationsEnabled=bool(data,"notificationsEnabled",true);p.createdBy=p.createdBy==null?access.current():p.createdBy;
  Long accountId=id(data,"accountId",false);if(accountId==null&&p.account==null&&p.employee!=null){var available=users.findByEmployee_Id(p.employee.getId()).stream().filter(u->Boolean.TRUE.equals(u.getEnabled())).toList();if(available.size()==1)accountId=available.get(0).getId();else throw fail(400,"Choose an active existing EAC account for this employee");}if(id==null&&accountId==null)throw fail(400,"Select an employee with an existing EAC account");final Long selectedAccountId=accountId;if(accountId!=null){User account=users.findById(accountId).filter(u->Boolean.TRUE.equals(u.getEnabled())||p.managedAccount&&p.account!=null&&p.account.getId().equals(u.getId())).orElseThrow(()->fail(404,"Active account unavailable"));if(p.account!=null&&!p.account.getId().equals(selectedAccountId))throw fail(409,"An existing driver account cannot be replaced silently");if(p.employee==null||account.getEmployee()==null||!p.employee.getId().equals(account.getEmployee().getId()))throw fail(400,"The account must belong to the selected employee");profiles.findByAccount_Id(accountId).filter(other->!other.driver.getId().equals(d.getId())).ifPresent(other->{throw fail(409,"This account is already linked to another driver");});p.account=account;}
  profiles.saveAndFlush(p);
  if(p.account!=null&&p.active){Page workspace=pages.findByPath("/transport/driver").orElseGet(()->{Page page=new Page();page.setName("Driver workspace");page.setPath("/transport/driver");page.setModule(Page.Module.TRANSPORT);page.setIsActive(true);page.setIsPublic(false);page.setRequiresAuth(true);return pages.save(page);});workspace.getUsers().add(p.account);p.account.getAccessiblePages().add(workspace);pages.save(workspace);}
  // Driver workspace membership never changes the employee login, role or password.
  core.audit(null,id==null?"DRIVER_CREATED":"DRIVER_UPDATED","Driver record and notification preferences updated: "+d.getDriverName(),"driver:"+d.getId());profiles.saveAndFlush(p);return describe(d);}
 public void invite(Long id){access.require("TRANSPORT_MANAGE_DRIVERS");throw fail(409,"Use the employee's normal EAC login and password recovery; no separate transport setup is needed");}
 public List<Map<String,Object>> availableAccounts(Long employeeId){access.require("TRANSPORT_MANAGE_DRIVERS");return users.findByEmployee_Id(employeeId).stream().map(u->map("id",u.getId(),"name",u.getName(),"username",u.getUserName(),"email",u.getEmail(),"enabled",u.getEnabled())).toList();}
}
