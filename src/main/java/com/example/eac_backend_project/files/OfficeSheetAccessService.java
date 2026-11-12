package com.example.eac_backend_project.files;

import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import static org.springframework.http.HttpStatus.*;

@Service @Transactional(rollbackFor=Exception.class)
public class OfficeSheetAccessService {
 @org.springframework.beans.factory.annotation.Autowired private OfficePasswordVault passwords;
 @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.repository.EmployeeRepository employeeDirectory;
 private final FileSuiteService files;private final FileOperationService operations;private final UserRepository users;private final OfficeSheetPolicyRepository policies;private final OfficeSheetPolicyAuditRepository audits;private final ObjectMapper json;
 public OfficeSheetAccessService(FileSuiteService files,FileOperationService operations,UserRepository users,OfficeSheetPolicyRepository policies,OfficeSheetPolicyAuditRepository audits,ObjectMapper json){this.files=files;this.operations=operations;this.users=users;this.policies=policies;this.audits=audits;this.json=json;}
 public record Grant(Long userId,String access){}
 public record Update(String sheet,boolean restricted,List<Grant> grants,long policyVersion,boolean shareWorkbook){}
 public record BatchUpdate(List<String> sheets,boolean restricted,List<Grant> grants,long policyVersion,boolean shareWorkbook){}
 public State updateBatch(Long id,BatchUpdate request)throws IOException{
  if(request.sheets()==null||request.sheets().isEmpty()||request.sheets().size()>1000||request.sheets().stream().anyMatch(Objects::isNull)||new HashSet<>(request.sheets()).size()!=request.sheets().size())throw new ResponseStatusException(BAD_REQUEST,"Select distinct sheets to update");
  // The enclosing transaction makes all sheet assignments and workbook shares atomic.
  State result=null;long version=request.policyVersion();
  for(String sheet:request.sheets()){result=update(id,new Update(sheet,request.restricted(),request.grants(),version,request.shareWorkbook()));version=result.policyVersion();}
  return result;
 }
 public record Person(Long userId,String id,String name,String employeeId,String workbookAccess,boolean canEdit,boolean admin){}
 public record Audit(long policyVersion,String employee,Instant savedAt){}
 public record State(long policyVersion,boolean pending,boolean canManage,boolean adminOverride,String ownerId,List<OfficeWorkbookInspection.Rule> sheets,List<Person> employees,List<Audit> audit){}
 private static String uid(User u){return "eac-"+u.getId();}
 private boolean manager(FileItem f,User u){return Set.of("OWNER","MANAGER").contains(Objects.toString(files.access(f,u,false),""))&&operations.allows(u,"FILE_MANAGE_ACCESS");}
 private FileItem workbook(Long id){FileItem f=files.officeLock(id);files.officeView(f);if(f.isFolder()||!f.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx"))throw new ResponseStatusException(BAD_REQUEST,"Sheet access is available for XLSX workbooks");return f;}
 private List<OfficeWorkbookInspection.Rule> canonical(FileItem f)throws IOException{return OfficeWorkbookInspection.read(passwords.decode(f.getId(),java.nio.file.Files.readAllBytes(files.officeSource(f,f.getCurrentVersion()).path()),"xlsx"));}
 private List<OfficeWorkbookInspection.Rule> rules(OfficeSheetPolicy policy)throws IOException{return json.readValue(policy.getRules(),new TypeReference<List<OfficeWorkbookInspection.Rule>>(){});}
 public List<User> administrators(){return users.findByEnabledTrue().stream().filter(FileSuiteService::admin).toList();}
 private List<OfficeWorkbookInspection.Rule> effective(FileItem f,List<OfficeWorkbookInspection.Rule> source){var mandatory=new ArrayList<>(administrators());mandatory.add(f.getOwner());return source.stream().map(rule->{Map<String,String> members=new LinkedHashMap<>(rule.restricted()?rule.members():Map.of());if(rule.restricted())mandatory.forEach(user->members.put(uid(user),"CanEdit"));return new OfficeWorkbookInspection.Rule(rule.name(),rule.restricted(),members);}).toList();}
 private List<Person> directory(FileItem file){
  var enabled=users.findByEnabledTrue();
  var byEmployee=new HashMap<Long,List<User>>();
  for(var account:enabled)if(account.getEmployee()!=null)byEmployee.computeIfAbsent(account.getEmployee().getId(),ignored->new ArrayList<>()).add(account);
  Map<String,Person> result=new LinkedHashMap<>();
  for(var employee:employeeDirectory.findAll()){
   String name=(Objects.toString(employee.getFirstName(),"")+" "+Objects.toString(employee.getLastName(),"")).trim();
   var accounts=Boolean.FALSE.equals(employee.getActive())?List.<User>of():byEmployee.getOrDefault(employee.getId(),List.of());
   if(accounts.isEmpty()){String id="employee-"+employee.getId();result.put(id,new Person(null,id,name,employee.getEmployeeId(),null,false,false));}
   else for(var account:accounts)result.put(uid(account),new Person(account.getId(),uid(account),name.isBlank()?account.getName():name,employee.getEmployeeId(),files.access(file,account,false),operations.allows(account,"FILE_EDIT"),FileSuiteService.admin(account)));
  }
  for(var account:enabled)if(FileSuiteService.admin(account)||account.getId().equals(file.getOwner().getId()))result.putIfAbsent(uid(account),new Person(account.getId(),uid(account),account.getName(),"",files.access(file,account,false),true,FileSuiteService.admin(account)));
  return result.values().stream().sorted(Comparator.comparing(p->Objects.toString(p.name(),""),String.CASE_INSENSITIVE_ORDER)).toList();
 }
 public State state(Long id)throws IOException{
  User user=files.currentUser();FileItem f=workbook(id);boolean manage=manager(f,user);var stored=policies.findById(id).orElse(null);var nativeRules=canonical(f);var rules=effective(f,stored==null?nativeRules:rules(stored));
  List<Person> people=manage?directory(f):List.of();
  List<Audit> history=manage?audits.findTop50ByFileIdOrderByIdDesc(id).stream().map(a->new Audit(a.getPolicyVersion(),a.getActorName(),a.getCreatedAt())).toList():List.of();
  // Preserve every actual sheet tab; renamed/new sheets require an owner to reconcile policy.
  Map<String,OfficeWorkbookInspection.Rule> byName=new LinkedHashMap<>();rules.forEach(rule->byName.put(rule.name(),rule));
  for(var actual:nativeRules)if(!byName.containsKey(actual.name()))byName.put(actual.name(),actual);
  return new State(stored==null?0:stored.getPolicyVersion(),stored!=null&&stored.isPending(),manage,FileSuiteService.admin(user),uid(f.getOwner()),List.copyOf(byName.values()),people,history);
 }
 public State update(Long id,Update request)throws IOException{
  User actor=files.currentUser();FileItem f=workbook(id);if(!manager(f,actor))throw new ResponseStatusException(FORBIDDEN,"Only the owner, an authorized manager or ADMIN can assign sheet access");
  var stored=policies.findById(id).orElse(null);long version=stored==null?0:stored.getPolicyVersion();if(request.policyVersion()!=version)throw new ResponseStatusException(CONFLICT,"Sheet access changed. Reload before saving.");
  var actual=canonical(f);if(actual.stream().noneMatch(rule->rule.name().equals(request.sheet())))throw new ResponseStatusException(CONFLICT,"Save the workbook first, then refresh its sheet list");
  if(request.grants()==null||request.grants().size()>2000)throw new ResponseStatusException(BAD_REQUEST,"Invalid employee assignments");
  Map<String,String> members=new LinkedHashMap<>();Set<Long> seen=new HashSet<>();
  for(var grant:request.grants()){
   if(grant.userId()==null||!seen.add(grant.userId())||!Set.of("CanEdit","CanView").contains(Objects.toString(grant.access(),"")))throw new ResponseStatusException(BAD_REQUEST,"Choose one valid permission per employee");
   User person=users.findById(grant.userId()).filter(u->Boolean.TRUE.equals(u.getEnabled())&&(FileSuiteService.admin(u)||(u.getEmployee()!=null&&!Boolean.FALSE.equals(u.getEmployee().getActive())))).orElseThrow(()->new ResponseStatusException(BAD_REQUEST,"Choose an active employee account"));
   if(grant.access().equals("CanEdit")&&!operations.allows(person,"FILE_EDIT"))throw new ResponseStatusException(BAD_REQUEST,"This employee does not have file editing permission");
   String access=files.access(f,person,false);boolean needsShare=access==null||(grant.access().equals("CanEdit")&&!Set.of("OWNER","MANAGER","EDITOR").contains(access));
   if(needsShare){if(!request.shareWorkbook())throw new ResponseStatusException(BAD_REQUEST,"Share the workbook with the selected employee first, or enable workbook sharing");operations.require(actor,"FILE_SHARE");files.share(id,person.getId(),grant.access().equals("CanEdit")?FilePermission.Access.EDITOR:FilePermission.Access.VIEWER,null,false);}
   members.put(uid(person),grant.access());
  }
  var previous=stored==null?actual:rules(stored);Map<String,OfficeWorkbookInspection.Rule> next=new LinkedHashMap<>();for(var rule:actual){var old=previous.stream().filter(p->p.name().equals(rule.name())).findFirst().orElse(rule);next.put(rule.name(),old);}next.put(request.sheet(),new OfficeWorkbookInspection.Rule(request.sheet(),request.restricted(),members));
  var normalized=effective(f,new ArrayList<>(next.values()));
  // Sharing an unchanged upload or reapplying identical permissions requires no content edit/checkpoint.
  if(normalized.equals(effective(f,previous)))return state(id);
  if(stored==null){stored=new OfficeSheetPolicy();stored.setFile(f);}
  stored.setRules(json.writeValueAsString(normalized));stored.setPending(true);stored.setPolicyVersion(version+1);stored.setChangedAt(Instant.now());stored.setChangedBy(Objects.toString(actor.getName(),actor.getUserName()));policies.saveAndFlush(stored);
  var audit=new OfficeSheetPolicyAudit();audit.setFileId(id);audit.setPolicyVersion(version+1);audit.setActorId(actor.getId());audit.setActorName(stored.getChangedBy());audit.setRules(stored.getRules());audits.save(audit);files.officeEvent(f,"SHEET_ACCESS_ASSIGNED");return state(id);
 }
 public void assertOpen(FileItem f,User user){if(policies.findById(f.getId()).filter(OfficeSheetPolicy::isPending).isPresent()&&!manager(f,user))throw new ResponseStatusException(CONFLICT,"The owner is updating sheet access. Reopen after they apply and save the workbook.");}
 public void verifyCheckpoint(FileItem file,byte[] bytes)throws IOException{
  var stored=policies.findById(file.getId()).orElse(null);if(stored==null)return;
  var actual=OfficeWorkbookInspection.read(bytes);var expected=effective(file,rules(stored));
  for(var rule:expected){var found=actual.stream().filter(a->a.name().equals(rule.name())).findFirst().orElseThrow(()->new ResponseStatusException(CONFLICT,"A managed sheet was renamed or removed. Reconcile sheet access with the owner before saving."));
   if(rule.restricted()!=found.restricted()||(rule.restricted()&&!rule.members().equals(found.members())))throw new ResponseStatusException(CONFLICT,"Sheet access was not applied correctly in the editor. Owner or ADMIN must apply the EAC policy and save again; previous versions are preserved.");
  }
  stored.setPending(false);policies.save(stored);
 }
}
