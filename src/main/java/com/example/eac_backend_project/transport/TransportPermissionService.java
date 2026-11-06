package com.example.eac_backend_project.transport;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.entity.inventory.EndpointPermission;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import static com.example.eac_backend_project.transport.TransportValues.*;
import static com.example.eac_backend_project.transport.TransportAccess.fail;

@Service @Transactional
public class TransportPermissionService {
 @Autowired TransportAccess access;
 @Autowired EndpointPermissionRepository permissions;
 @Autowired UserRepository users;
 @Autowired RoleRepository roles;
 @Autowired TransportCoreService core;
 private void administrator(){if(!access.admin(access.current()))throw fail(403,"Transport permission assignment requires an administrator account");}
 private String version(EndpointPermission p){return hash(p.getDescription()+":"+p.isEnabled()+":"+p.getUsers().stream().map(u->u.getId().toString()).sorted().toList()+":"+p.getRoles().stream().map(r->r.getId().toString()).sorted().toList());}
 private Map<String,Object> describe(EndpointPermission p){return map("code",p.getDescription(),"version",version(p),"enabled",p.isEnabled(),"userIds",p.getUsers().stream().map(u->u.getId()).toList(),"roleIds",p.getRoles().stream().map(r->r.getId()).toList());}
 public Map<String,Object> get(){administrator();return map("permissions",permissions.findAll().stream().filter(p->TransportAccess.CODES.contains(p.getDescription())).sorted(Comparator.comparing(EndpointPermission::getDescription)).map(this::describe).toList(),"users",users.findAll().stream().map(u->map("id",u.getId(),"name",u.getName(),"username",u.getUserName(),"enabled",u.getEnabled(),"roleId",u.getRole()==null?null:u.getRole().getId())).toList(),"roles",roles.findAll().stream().map(r->map("id",r.getId(),"name",r.getName())).toList());}
 public Map<String,Object> save(String code,JsonNode data){administrator();if(!TransportAccess.CODES.contains(code))throw fail(400,"Choose a Transport action permission");var candidate=permissions.findAll().stream().filter(p->code.equals(p.getDescription())).findFirst().orElseThrow(()->fail(404,"Permission definition unavailable"));var permission=permissions.lock(candidate.getId()).orElseThrow();String expected=text(data,"version",64,true);if(!expected.equals(version(permission)))throw fail(409,"Permission assignments changed; reload before saving");String reason=text(data,"reason",2000,true);if(reason.length()<10)throw fail(400,"Record an assignment reason of at least ten characters");Set<Long> userIds=ids(data,"userIds"),roleIds=ids(data,"roleIds");var selectedUsers=users.findAllById(userIds);var selectedRoles=roles.findAllById(roleIds);if(selectedUsers.size()!=userIds.size()||selectedRoles.size()!=roleIds.size())throw fail(400,"One or more selected accounts or roles are unavailable");var previous=describe(permission);permission.setUsers(new HashSet<>(selectedUsers));permission.setRoles(new HashSet<>(selectedRoles));permission.setEnabled(true);permissions.saveAndFlush(permission);core.auditDetails(null,"TRANSPORT_PERMISSION_CHANGED",reason,code,map("previous",previous,"current",describe(permission)));return get();}
 private Set<Long> ids(JsonNode data,String key){JsonNode values=data.path(key);if(!values.isArray()||values.size()>10000)throw fail(400,"Select valid accounts and roles");Set<Long> ids=new HashSet<>();for(var value:values){if(!value.isIntegralNumber()||!value.canConvertToLong()||value.longValue()<1)throw fail(400,"Select valid accounts and roles");ids.add(value.longValue());}return ids;}
}
