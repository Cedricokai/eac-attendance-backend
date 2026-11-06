package com.example.eac_backend_project.communication;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.transaction.annotation.Transactional;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.entity.inventory.*;
@Component public class CommunicationBootstrap {
 private final CommunicationPolicyRepository policy;private final EndpointPermissionRepository permissions;private final PageRepository pages;private final RoleRepository roles;
 public CommunicationBootstrap(CommunicationPolicyRepository policy,EndpointPermissionRepository permissions,PageRepository pages,RoleRepository roles){this.policy=policy;this.permissions=permissions;this.pages=pages;this.roles=roles;}
 @EventListener(ApplicationReadyEvent.class) @Transactional public void initialize(){
  if(!policy.existsById(1L))policy.save(new CommunicationPolicy());
  for(String code:CommunicationAccess.CODES)if(permissions.findAll().stream().noneMatch(p->code.equals(p.getDescription()))){EndpointPermission p=new EndpointPermission();p.setDescription(code);p.setHttpMethod("GET");p.setEndpointPattern("/api/communication/privileges/"+code);p.setEnabled(false);permissions.save(p);}
  String[][] routes={{"Communication","/communication"},{"Group Communication","/communication/groups"},{"Communication Oversight","/admin/communications"},{"Communication Settings","/admin/communication-settings"},{"Communication Recovery","/admin/communications/recovery"},{"Communication Backups","/admin/communications/backups"}};
  for(var route:routes)if(pages.findByPath(route[1]).isEmpty()){Page p=new Page();p.setName(route[0]);p.setPath(route[1]);p.setIconName("MessageSquare");p.setModule(Page.Module.GENERAL);roles.findByName("ROLE_ADMIN").ifPresent(r->p.getRoles().add(r));pages.save(p);}
 }
}
