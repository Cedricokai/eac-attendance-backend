package com.example.eac_backend_project.files;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.transaction.annotation.Transactional;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.entity.inventory.EndpointPermission;
@Component public class FileOperationSeeder {
 private final EndpointPermissionRepository permissions;private final RoleRepository roles;
 public FileOperationSeeder(EndpointPermissionRepository permissions,RoleRepository roles){this.permissions=permissions;this.roles=roles;}
 @EventListener(ApplicationReadyEvent.class) @Transactional public void seed(){
  String[][] templates={{"FILE_VIEW","GET","/api/files/**"},{"FILE_UPLOAD","POST","/api/files/upload"},{"FILE_CREATE","POST","/api/files/folders"},{"FILE_EDIT","POST","/api/files/*/content"},{"FILE_DELETE","DELETE","/api/files/*"},{"FILE_DOWNLOAD","GET","/api/files/*/download"},{"FILE_SHARE","POST","/api/files/*/share"},{"FILE_MANAGE_ACCESS","DELETE","/api/files/*/shares/*"},{"FILE_RESTORE","POST","/api/files/*/restore"},{"FILE_VIEW_AUDIT","GET","/api/files/admin/audit/**"},{"FILE_MANAGE_SETTINGS","PUT","/api/files/settings"},{"FILE_MANAGE_BACKUPS","POST","/api/files/backups/**"},{"FILE_ADMIN","GET","/api/files/admin/**"}};
  var existing=permissions.findAll();for(var template:templates){if(existing.stream().anyMatch(p->template[0].equals(p.getDescription())))continue;var permission=new EndpointPermission();permission.setDescription(template[0]);permission.setHttpMethod(template[1]);permission.setEndpointPattern(template[2]);permission.setEnabled(false);roles.findByName("ROLE_ADMIN").ifPresent(role->permission.getRoles().add(role));permissions.save(permission);}
 }
}
