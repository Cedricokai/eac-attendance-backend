package com.example.eac_backend_project.transport;

import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.transport.Vehicles;
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

@Service @Transactional(readOnly=true)
public class TransportAccess {
 public static final Set<String> CODES=Set.of("TRANSPORT_VIEW","TRANSPORT_MANAGE_VEHICLES","TRANSPORT_MANAGE_DRIVERS","TRANSPORT_MANAGE_FUEL","TRANSPORT_MANAGE_MAINTENANCE","TRANSPORT_REVIEW_FAULTS","TRANSPORT_PROCUREMENT","TRANSPORT_APPROVE_PROCUREMENT","TRANSPORT_SETTINGS","TRANSPORT_REPORTS","TRANSPORT_RELEASE_VEHICLE");
 private final UserRepository users;private final EndpointPermissionRepository permissions;private final PageService pages;private final TransportDriverProfiles drivers;private final TransportVehicleProfiles profiles;
 public TransportAccess(UserRepository users,EndpointPermissionRepository permissions,PageService pages,TransportDriverProfiles drivers,TransportVehicleProfiles profiles){this.users=users;this.permissions=permissions;this.pages=pages;this.drivers=drivers;this.profiles=profiles;}
 public User current(){var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null||!auth.isAuthenticated())throw fail(401,"Sign in required");User user=users.findByUserName(auth.getName()).orElseThrow(()->fail(401,"Account unavailable"));if(!Boolean.TRUE.equals(user.getEnabled()) || user.getEmployee()!=null&&Boolean.FALSE.equals(user.getEmployee().getActive()))throw fail(403,"Account is inactive");return user;}
 public boolean admin(User user){return user.getRole()!=null&&Set.of("ROLE_ADMIN","ROLE_TRANSPORT_ADMIN").contains(user.getRole().getName());}
 public boolean allows(User user,String code){if(!CODES.contains(code))return false;if(admin(user))return true;return permissions.findAllActiveWithRolesAndUsers().stream().filter(p->code.equals(p.getDescription())).anyMatch(p->p.getUsers().stream().anyMatch(u->u.getId().equals(user.getId()))||p.getRoles().stream().anyMatch(r->user.getRole()!=null&&r.getId().equals(user.getRole().getId())));}
 public void require(String code){if(!allows(current(),code))throw fail(403,"Transport permission required: "+code);}
 public void requireEntry(){User user=current();if(admin(user)||procurement(user)||driver(user).isPresent()||CODES.stream().anyMatch(code->allows(user,code)))return;if(user.getRole()==null||"ROLE_DRIVER".equals(user.getRole().getName()))throw fail(403,"Transport access is not assigned to this account");var auth=new UsernamePasswordAuthenticationToken(user.getUserName(),"",List.of(new SimpleGrantedAuthority(user.getRole().getName())));if(pages.getAccessiblePages(auth).stream().noneMatch(p->Set.of("/transport/driver","/vehicles","/drivers","/fuel","/maintenance","/transportReports","/adminDashboard").stream().anyMatch(path->path.equalsIgnoreCase(p.getPath()))))throw fail(403,"Transport access is not assigned to this account");}
 public boolean procurement(User user){return allows(user,"TRANSPORT_PROCUREMENT")||user.getRole()!=null&&Set.of("ROLE_PROCUREMENT_OFFICER","ROLE_PROCUREMENT_MANAGER").contains(user.getRole().getName());}
 public Optional<TransportDriverProfile> driver(User user){return drivers.findByAccount_Id(user.getId()).filter(d->d.active&&d.archivedAt==null&&d.driver!=null&&!"inactive".equalsIgnoreCase(d.driver.getStatus()));}
 public boolean assigned(User user,Vehicles vehicle){var d=driver(user);if(d.isEmpty())return false;var profile=profiles.findByVehicle_Id(vehicle.getId());return profile.map(p->p.driver!=null&&p.driver.getId().equals(d.get().driver.getId())).orElse(false);}
 public boolean mayView(User user,Vehicles vehicle){if(admin(user)||grantsFor(user).stream().anyMatch(Set.of("TRANSPORT_VIEW","TRANSPORT_MANAGE_VEHICLES","TRANSPORT_MANAGE_MAINTENANCE","TRANSPORT_MANAGE_FUEL","TRANSPORT_REVIEW_FAULTS","TRANSPORT_REPORTS")::contains))return true;if(driver(user).isPresent())return assigned(user,vehicle);if(user.getRole()!=null&&"ROLE_DRIVER".equals(user.getRole().getName()))return false;var auth=new UsernamePasswordAuthenticationToken(user.getUserName(),"",List.of(new SimpleGrantedAuthority(user.getRole().getName())));return pages.getAccessiblePages(auth).stream().anyMatch(p->"/vehicles".equals(p.getPath())||"/adminDashboard".equalsIgnoreCase(p.getPath()));}
 public void vehicle(Vehicles vehicle){if(!mayView(current(),vehicle))throw fail(403,"This vehicle is not assigned or accessible to your account");}
 public List<Vehicles> visible(List<Vehicles> records){User user=current();return records.stream().filter(v->mayView(user,v)).toList();}
 public boolean manager(String code){return allows(current(),code);}
 public boolean maySeeDriver(com.example.eac_backend_project.entity.transport.Drivers record){User user=current();return record.getArchivedAt()==null&&(admin(user)||allows(user,"TRANSPORT_MANAGE_DRIVERS")||allows(user,"TRANSPORT_MANAGE_VEHICLES")||driver(user).map(p->p.driver.getId().equals(record.getId())).orElse(false));}
 public void driverRecord(com.example.eac_backend_project.entity.transport.Drivers record){if(!maySeeDriver(record))throw fail(403,"Driver record is outside your access");}
 public Set<String> grants(){return grantsFor(current());} public Set<String> grantsFor(User user){if(admin(user))return new HashSet<>(CODES);Set<String> result=new HashSet<>();for(var p:permissions.findAllActiveWithRolesAndUsers())if(CODES.contains(p.getDescription())&&(p.getUsers().stream().anyMatch(u->u.getId().equals(user.getId()))||p.getRoles().stream().anyMatch(r->user.getRole()!=null&&r.getId().equals(user.getRole().getId()))))result.add(p.getDescription());return result;}
 public static ResponseStatusException fail(int code,String message){return new ResponseStatusException(HttpStatus.valueOf(code),message);}
}
