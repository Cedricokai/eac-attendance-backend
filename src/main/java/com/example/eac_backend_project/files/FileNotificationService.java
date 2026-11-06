package com.example.eac_backend_project.files;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.util.HtmlUtils;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
@Service @Transactional(readOnly=true)
public class FileNotificationService {
 private final EmailService mail;private final FileSettingsRepository settings;private final UserRepository users;private final FileEventRepository events;private final String frontend;
 private Consumer<Runnable> dispatch;private ExecutorService workers;
 @Autowired public FileNotificationService(EmailService mail,FileSettingsRepository settings,UserRepository users,FileEventRepository events,@Value("${app.frontend.base-url:http://localhost:5173}")String frontend){this(mail,settings,users,events,frontend,null);var pool=new ThreadPoolExecutor(1,2,60,TimeUnit.SECONDS,new ArrayBlockingQueue<>(200),task->{Thread thread=new Thread(task,"eac-file-mail");thread.setDaemon(true);return thread;},new ThreadPoolExecutor.AbortPolicy());workers=pool;dispatch=pool::execute;}
 FileNotificationService(EmailService mail,FileSettingsRepository settings,UserRepository users,FileEventRepository events,String frontend,Consumer<Runnable> dispatch){this.mail=mail;this.settings=settings;this.users=users;this.events=events;this.frontend=frontend.replaceAll("/+$","");this.dispatch=dispatch;}
 public boolean enabled(){return settings.findById(1L).map(FileSettings::isEmailNotifications).orElse(false);}
 private String escape(String value){return HtmlUtils.htmlEscape(Objects.toString(value,""));}
 private boolean queue(User target,String subject,String message,String path,boolean explicit){
  if((!explicit&&!enabled())||target==null||target.getId()==null||!Boolean.TRUE.equals(target.getEnabled())||target.getEmail()==null||!target.getEmail().trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))return false;
  Long userId=target.getId();String address=target.getEmail().trim();String link=frontend+path;
  String html="<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:auto;color:#25324a\"><div style=\"background:#1d4ed8;color:white;padding:22px;font-size:22px;font-weight:bold\">EAC Files</div><div style=\"padding:24px\"><h2>"+escape(subject)+"</h2><p>"+escape(message).replace("\n","<br>")+"</p><p><a style=\"color:#1d4ed8\" href=\""+escape(link)+"\">Open EAC Files</a></p><p style=\"font-size:12px;color:#65758b\">Sign in with your existing EAC account. Current file and page permissions apply.</p></div></div>";
  Runnable submit=()->{try{dispatch.accept(()->{try{var current=users.findById(userId);if(current.isEmpty()||!Boolean.TRUE.equals(current.get().getEnabled())||!address.equalsIgnoreCase(Objects.toString(current.get().getEmail(),""))||(!explicit&&!enabled()))return;mail.sendCustomEmail(address,"EAC Files: "+subject,html);}catch(Exception ex){failed(userId);}});}catch(RuntimeException ex){failed(userId);}};
  if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){submit.run();}});else submit.run();return true;
 }
 public boolean explicitNotice(User target,String subject,String message,Long fileId){return queue(target,subject,message,"/#/files?file="+fileId,true);}
 public boolean notice(User target,String subject,String message,Long fileId){return queue(target,subject,message,"/#/files?file="+fileId,false);}
 public boolean storage(User target,String message){return queue(target,"Storage nearly full",message,"/#/files?view=storage",false);}
 public void organizationStorage(String message){if(!enabled())return;for(var user:users.findByEnabledTrue())if(FileSuiteService.admin(user))queue(user,"Organization storage nearly full",message,"/#/files?view=storage",false);}
 public void backupFailed(){if(!enabled())return;for(var user:users.findByEnabledTrue())if(FileSuiteService.admin(user))queue(user,"Backup failed","A scheduled file backup failed. Review backup history and server storage before the next run.","/#/files?view=backups",false);}
 private void failed(Long owner){org.slf4j.LoggerFactory.getLogger(getClass()).warn("A file email notice could not be delivered; check existing SMTP settings");try{var event=new FileEvent();event.setOwnerId(owner);event.setUsername("file-notifications");event.setFilename("Automatic email notice");event.setAction("EMAIL_NOTICE");event.setStatus("FAILED");events.save(event);}catch(RuntimeException ignored){}}
 @jakarta.annotation.PreDestroy public void close(){if(workers!=null){workers.shutdown();try{if(!workers.awaitTermination(10,TimeUnit.SECONDS))workers.shutdownNow();}catch(InterruptedException ex){workers.shutdownNow();Thread.currentThread().interrupt();}}}
}
