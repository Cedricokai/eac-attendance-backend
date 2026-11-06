package com.example.eac_backend_project.communication;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.Service.PageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@Service @Transactional(readOnly=true) public class CommunicationAccess {
 public static final Set<String> CODES=Set.of("COMMUNICATION_ADMIN","COMMUNICATION_VIEW_ALL","COMMUNICATION_POLICY_MANAGE","COMMUNICATION_RECOVERY_MANAGE","COMMUNICATION_EXPORT","COMMUNICATION_BACKUP_MANAGE","COMMUNICATION_GROUP_MANAGE");
 private final UserRepository users;private final EndpointPermissionRepository permissions;private final PageService pages;
 public CommunicationAccess(UserRepository users,EndpointPermissionRepository permissions,PageService pages){this.users=users;this.permissions=permissions;this.pages=pages;}
 public User account(){var a=SecurityContextHolder.getContext().getAuthentication();if(a==null||!a.isAuthenticated())throw fail(401,"Sign in required");return identity(a.getName());}
 public User identity(String name){User u=users.findByUserName(name).orElseThrow(()->fail(401,"Account not found"));if(!Boolean.TRUE.equals(u.getEnabled()))throw fail(403,"Account disabled");return u;}
 public boolean eligible(User u){if(!Boolean.TRUE.equals(u.getEnabled())||u.getEmployee()==null||Boolean.FALSE.equals(u.getEmployee().getActive()))return false;
  if(admin(u))return true;var auth=new UsernamePasswordAuthenticationToken(u.getUserName(),"",List.of(new SimpleGrantedAuthority(u.getRole().getName())));return pages.getAccessiblePages(auth).stream().anyMatch(p->"/communication".equals(p.getPath()));}
 public Long socketUser(String name){User u=identity(name);if(!eligible(u))throw fail(403,"Communication access required");return u.getId();}
 public User employee(){User u=account();if(!eligible(u))throw fail(403,"An active employee account with Communication page access is required");return u;}
 public static boolean admin(User u){return u.getRole()!=null&&"ROLE_ADMIN".equals(u.getRole().getName());}
 public static final Set<String> MANAGEMENT_CODES=Set.of("COMMUNICATION_POLICY_MANAGE","COMMUNICATION_RECOVERY_MANAGE","COMMUNICATION_BACKUP_MANAGE","COMMUNICATION_GROUP_MANAGE","COMMUNICATION_ADMIN");
 private boolean assigned(User u,String code){return permissions.findAllActiveWithRolesAndUsers().stream().filter(p->code.equals(p.getDescription())).anyMatch(p->p.getUsers().stream().anyMatch(x->x.getId().equals(u.getId()))||p.getRoles().stream().anyMatch(x->u.getRole()!=null&&x.getId().equals(u.getRole().getId())));}
 public boolean allows(User u,String code){if(!CODES.contains(code)||!Boolean.TRUE.equals(u.getEnabled()))return false;if(code.equals("COMMUNICATION_VIEW_ALL")||code.equals("COMMUNICATION_EXPORT"))return admin(u);if(MANAGEMENT_CODES.contains(code))return admin(u)||assigned(u,code);return false;}
 public void require(User u,String code){if(!allows(u,code))throw fail(403,"Administrator or assigned communication permission required: "+code);}
 public Set<String> grants(User u){Set<String>s=new HashSet<>();for(String c:CODES)if(allows(u,c))s.add(c);return s;}
 public static ResponseStatusException fail(int status,String text){return new ResponseStatusException(HttpStatus.valueOf(status),text);}
}
