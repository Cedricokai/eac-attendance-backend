package com.example.eac_backend_project.files;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.entity.inventory.*;
import jakarta.persistence.*;
import java.util.*;
@RestController @RequestMapping("/api/files/admin/operations") @Transactional
public class FilePolicyController {
 private final FileSuiteService files;private final EndpointPermissionRepository permissions;private final RoleRepository roles;private final UserRepository users;private final FileEventRepository events;
 @PersistenceContext private EntityManager entities;
 public FilePolicyController(FileSuiteService files,EndpointPermissionRepository permissions,RoleRepository roles,UserRepository users,FileEventRepository events){this.files=files;this.permissions=permissions;this.roles=roles;this.users=users;this.events=events;}
 public record Update(boolean enabled,List<Long> roleIds,List<Long> userIds,String revision){}
 private void admin(){if(!FileSuiteService.admin(files.currentUser()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Administrator required");}
 private String stamp(EndpointPermission permission){String value=permission.isEnabled()+":"+permission.getRoles().stream().map(Role::getId).sorted().toList()+":"+permission.getUsers().stream().map(User::getId).sorted().toList();try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
 private Map<String,Object> view(EndpointPermission permission){return Map.of("id",permission.getId(),"code",permission.getDescription(),"enabled",permission.isEnabled(),"revision",stamp(permission),"roles",permission.getRoles().stream().map(r->Map.of("id",r.getId(),"name",r.getName())).toList(),"users",permission.getUsers().stream().map(u->Map.of("id",u.getId(),"name",u.getName()+" ("+u.getUserName()+")")).toList());}
 @GetMapping public Object list(){admin();return permissions.findAll().stream().filter(p->FileOperationService.CODES.contains(Objects.toString(p.getDescription(),""))).sorted(Comparator.comparing(EndpointPermission::getDescription)).map(this::view).toList();}
 @GetMapping("/roles")public Object roles(){admin();return roles.findAll().stream().map(r->Map.of("id",r.getId(),"name",r.getName())).toList();}
 @PutMapping("/{id}")public Object update(@PathVariable Long id,@RequestBody Update request){
  admin();EndpointPermission permission=entities.find(EndpointPermission.class,id,LockModeType.PESSIMISTIC_WRITE);if(permission==null||!FileOperationService.CODES.contains(Objects.toString(permission.getDescription(),"")))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"File operation not found");if(!stamp(permission).equals(request.revision()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This policy changed. Refresh before saving");if(request.roleIds()==null||request.userIds()==null||request.roleIds().size()>200||request.userIds().size()>500)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose valid roles and accounts");
  var assignedRoles=new HashSet<Role>();for(Long role:request.roleIds())assignedRoles.add(roles.findById(role).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Role not found")));roles.findByName("ROLE_ADMIN").ifPresent(assignedRoles::add);var assignedUsers=new HashSet<User>();for(Long user:request.userIds())assignedUsers.add(users.findById(user).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Account not found")));
  permission.setEnabled(request.enabled());permission.setRoles(assignedRoles);permission.setUsers(assignedUsers);permissions.saveAndFlush(permission);var actor=files.currentUser();var event=new FileEvent();event.setOwnerId(actor.getId());event.setUsername(actor.getUserName());event.setFilename(permission.getDescription());event.setAction("OPERATION_POLICY_UPDATE");events.save(event);return view(permission);
 }
}
