package com.example.eac_backend_project.files;

import com.example.eac_backend_project.entity.inventory.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.springframework.http.HttpStatus.*;

/** Guards whole-file escape paths; native rendering has a separately signed channel. */
@Component
class OfficeSheetGuard {
 private final OfficeSheetPolicyRepository policies;private final ObjectMapper json;
 OfficeSheetGuard(OfficeSheetPolicyRepository policies,ObjectMapper json){this.policies=policies;this.json=json;}
 private List<OfficeWorkbookInspection.Rule> rules(OfficeSheetPolicy policy){try{return json.readValue(policy.getRules(),new TypeReference<List<OfficeWorkbookInspection.Rule>>(){});}catch(Exception e){throw new ResponseStatusException(CONFLICT,"Sheet access needs administrator review before exporting this workbook");}}
 private boolean allowed(FileItem file,User user,boolean editing){
  if(FileSuiteService.admin(user)||file.getOwner().getId().equals(user.getId()))return true;
  var policy=policies.findById(file.getId()).orElse(null);if(policy==null)return true;
  if(policy.isPending())return false;
  String id="eac-"+user.getId();return rules(policy).stream().noneMatch(rule->rule.restricted()&&!(editing?"CanEdit".equals(rule.members().get(id)):Set.of("CanView","CanEdit").contains(rule.members().getOrDefault(id,"NotView"))));
 }
 boolean wholeWorkbookAllowed(FileItem file,User user){return allowed(file,user,false);}
 FilePermission.Access savedShareAccess(FileItem file,User user,FilePermission.Access existing){
  var policy=policies.findById(file.getId()).orElse(null);
  if(policy==null)return existing==null?FilePermission.Access.VIEWER:existing;
  if(policy.isPending())throw new ResponseStatusException(CONFLICT,"Apply and save the sheet access changes before sending this file");
  String id="eac-"+user.getId();boolean view=false,edit=false;
  for(var rule:rules(policy)){String access=rule.restricted()?rule.members().getOrDefault(id,"NotView"):"CanEdit";view|=Set.of("CanView","CanEdit").contains(access);edit|="CanEdit".equals(access);}
  if(!view)throw new ResponseStatusException(BAD_REQUEST,"Assign this employee access to at least one sheet before sending with saved file access");
  return edit?FilePermission.Access.EDITOR:FilePermission.Access.VIEWER;
 }
 void requireWorkbookEdit(FileItem file,User user){if(!allowed(file,user,true))throw new ResponseStatusException(FORBIDDEN,"Editing access to every sheet is required to replace or restore the complete workbook. Edit your assigned sheets in EAC instead.");}
 void requireWholeWorkbook(FileItem file,User user){if(!wholeWorkbookAllowed(file,user))throw new ResponseStatusException(FORBIDDEN,"Access to every sheet is required to download, copy or restore the complete workbook, including older versions. Open your assigned sheets in EAC instead.");}
 void requireGuestAllowed(FileItem file){if(policies.findById(file.getId()).map(this::rules).orElse(List.of()).stream().anyMatch(OfficeWorkbookInspection.Rule::restricted))throw new ResponseStatusException(FORBIDDEN,"This workbook has employee-specific sheet access. Sign in with an assigned employee account instead of using a guest link.");}
 void markReplacement(FileItem file,String reason){if(!"ONLYOFFICE checkpoint".equals(reason))policies.findById(file.getId()).ifPresent(policy->{policy.setPending(true);policies.save(policy);});}
}
