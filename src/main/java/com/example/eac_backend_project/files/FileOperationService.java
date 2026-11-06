package com.example.eac_backend_project.files;
import org.springframework.stereotype.Service;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.EndpointPermissionRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service public class FileOperationService {
 public static final java.util.Set<String> CODES=java.util.Set.of("FILE_VIEW","FILE_UPLOAD","FILE_CREATE","FILE_EDIT","FILE_DELETE","FILE_DOWNLOAD","FILE_SHARE","FILE_MANAGE_ACCESS","FILE_RESTORE","FILE_VIEW_AUDIT","FILE_MANAGE_SETTINGS","FILE_MANAGE_BACKUPS","FILE_ADMIN");
 private final EndpointPermissionRepository permissions;
 public FileOperationService(EndpointPermissionRepository permissions){this.permissions=permissions;}
 public void require(User user,String code){if(!allows(user,code))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Your account does not have "+code+" permission");}
 public boolean allows(User user,String code){
  if(FileSuiteService.admin(user))return true;
  var policies=permissions.findAllActiveWithRolesAndUsers().stream().filter(p->code.equals(p.getDescription())).toList();
  if(policies.isEmpty())return true; // Existing page and per-file ACLs remain the default.
  boolean granted=policies.stream().anyMatch(p->p.getUsers().stream().anyMatch(u->u.getId().equals(user.getId()))||p.getRoles().stream().anyMatch(r->user.getRole()!=null&&r.getId().equals(user.getRole().getId())));
  return granted;
 }
}
