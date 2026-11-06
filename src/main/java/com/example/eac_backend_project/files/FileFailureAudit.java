package com.example.eac_backend_project.files;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.eac_backend_project.repository.UserRepository;
import org.springframework.web.context.request.*;
@Service public class FileFailureAudit {
 private final FileEventRepository events;private final UserRepository users;
 public FileFailureAudit(FileEventRepository events,UserRepository users){this.events=events;this.users=users;}
 @Transactional(propagation=Propagation.REQUIRES_NEW)public void record(int status){
  var auth=SecurityContextHolder.getContext().getAuthentication();boolean signed=auth!=null&&auth.isAuthenticated()&&!"anonymousUser".equals(auth.getName());String name=signed?auth.getName():"guest";var event=new FileEvent();event.setUsername(name.substring(0,Math.min(250,name.length())));if(signed)users.findByUserName(name).ifPresent(user->event.setOwnerId(user.getId()));event.setAction("REQUEST_REJECTED");event.setStatus(status==401||status==403?"DENIED":status==409?"CONFLICT":status>=500?"FAILED":"REJECTED");
  var attributes=RequestContextHolder.getRequestAttributes();if(attributes instanceof ServletRequestAttributes request){String path=request.getRequest().getRequestURI();event.setFilename(path.substring(0,Math.min(250,path.length())));event.setIpAddress(request.getRequest().getRemoteAddr());var match=java.util.regex.Pattern.compile("^/api/files/(\\d+)(/.*)?$").matcher(path);if(match.matches())try{event.setItemId(Long.valueOf(match.group(1)));}catch(NumberFormatException ignored){}}
  // No headers, tokens, OTPs, query strings, request bodies, or private filenames.
  events.saveAndFlush(event);
 }
}
