package com.example.eac_backend_project.communication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import java.util.*;
import com.example.eac_backend_project.entity.inventory.User;
@Service public class CommunicationAuditService {
 private final CommunicationAuditRepository audit;
 public CommunicationAuditService(CommunicationAuditRepository audit){this.audit=audit;}
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void write(User user,String action,Long conversation,String reason,String details){CommunicationAudit a=new CommunicationAudit();a.actorId=user.getId();a.actor=user.getUserName();a.action=action;a.conversationId=conversation;a.reason=reason==null?null:reason.substring(0,Math.min(500,reason.length()));a.details=details;audit.saveAndFlush(a);}
 @Transactional(readOnly=true) public org.springframework.data.domain.Page<CommunicationAudit> list(int page){return audit.findAll(PageRequest.of(Math.max(0,page),50,Sort.by(Sort.Direction.DESC,"id")));}
}
