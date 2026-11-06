package com.example.eac_backend_project.files;

import com.example.eac_backend_project.Service.email.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.HtmlUtils;
import java.time.Instant;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

@Service @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
public class FileGuestService {
    private final FileGuestRepository guests;
    private final FileSuiteService files;
    private final EmailService mail;
    private final String frontend;
    private final SecureRandom random=new SecureRandom();
    public FileGuestService(FileGuestRepository guests,FileSuiteService files,EmailService mail,@Value("${app.frontend.base-url:http://localhost:5173}")String frontend){this.guests=guests;this.files=files;this.mail=mail;this.frontend=frontend;}
    public record InvitationView(Long id,String name,String email,String access,Instant expiresAt,Instant revokedAt,Instant createdAt){}
    public record Created(InvitationView invitation,String link,String emailDelivery){}
    public record Verification(boolean verified,String session,String message){}
    private InvitationView view(FileGuestInvitation i){return new InvitationView(i.getId(),i.getName(),i.getEmail(),i.getAccess(),i.getExpiresAt(),i.getRevokedAt(),i.getCreatedAt());}
    private static ResponseStatusException fail(int code,String message){return new ResponseStatusException(HttpStatus.valueOf(code),message);}
    static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private String secret(){byte[] bytes=new byte[32];random.nextBytes(bytes);return HexFormat.of().formatHex(bytes);}
    private FileGuestInvitation invitation(String token){if(token==null||!token.matches("[a-f0-9]{64}"))throw fail(404,"Invitation unavailable");FileGuestInvitation i=guests.lockByToken(hash(token)).orElseThrow(()->fail(404,"Invitation unavailable"));if(i.getRevokedAt()!=null||i.getExpiresAt()==null||!i.getExpiresAt().isAfter(Instant.now()))throw fail(403,"Invitation expired or revoked");files.guestAvailable(i);return i;}
    private FileGuestInvitation session(String token,String session){FileGuestInvitation i=invitation(token);if(session==null||!session.matches("[a-f0-9]{64}")||i.getSessionHash()==null||i.getSessionExpiresAt()==null||!i.getSessionExpiresAt().isAfter(Instant.now())||!MessageDigest.isEqual(hash(session).getBytes(StandardCharsets.UTF_8),i.getSessionHash().getBytes(StandardCharsets.UTF_8)))throw fail(401,"Verify your email to access this file");return i;}
    public Created create(Long fileId,String name,String email,String access,Instant expiry,String message,boolean notify){
        var file=files.managedItem(fileId);files.requireDownloadAllowed(file,files.currentUser());if(!files.externalSharing())throw fail(403,"External sharing is disabled in File settings");if(file.isFolder())throw fail(400,"External invitations are limited to individual files");
        if(name==null||name.isBlank()||name.length()>160||email==null||email.length()>254||!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw fail(400,"Enter a valid name and email address");
        if(!List.of("VIEWER","EDITOR").contains(access))throw fail(400,"Guest permission must be VIEWER or EDITOR");
        if(expiry==null||!expiry.isAfter(Instant.now())||expiry.isAfter(Instant.now().plusSeconds(90L*86400)))throw fail(400,"Choose an expiry within the next 90 days");
        if(message!=null&&message.length()>2000)throw fail(400,"Invitation message is too long");
        String token=secret();FileGuestInvitation i=new FileGuestInvitation();i.setItem(file);i.setGrantedBy(files.currentUser());i.setName(name.trim());i.setEmail(email.trim().toLowerCase(Locale.ROOT));i.setAccess(access);i.setExpiresAt(expiry);i.setTokenHash(hash(token));guests.saveAndFlush(i);
        String link=frontend.replaceAll("/+$","")+"/#/file-guest?token="+token;String delivery="not_requested";
        if(notify){try{mail.sendCustomEmail(i.getEmail(),"EAC file invitation",html("A file was shared with you",HtmlUtils.htmlEscape(i.getGrantedBy().getName())+" invited you to <strong>"+HtmlUtils.htmlEscape(file.getName())+"</strong> with "+access+" access.<br>"+HtmlUtils.htmlEscape(Objects.requireNonNullElse(message,""))+"<br><a href=\""+HtmlUtils.htmlEscape(link)+"\">Open shared file</a><br>Verify your invited email address to continue. This invitation expires "+expiry+"."));delivery="requested";}catch(Exception ex){delivery="failed";}}
        files.invitationEvent(i,"EXTERNAL_INVITATION_CREATED");return new Created(view(i),link,delivery);
    }
    static String html(String title,String body){return "<div style=\"font-family:Arial,sans-serif;color:#243047;max-width:600px;padding:30px\"><h2 style=\"color:#2459d3\">EAC Files</h2><h3>"+title+"</h3><p style=\"line-height:1.7\">"+body+"</p><p style=\"font-size:12px;color:#789\">EAC Management System · Secure file workspace</p></div>";}
    public List<InvitationView> list(Long file){files.managedItem(file);return guests.findByItem_Id(file).stream().map(this::view).toList();}
    public void revoke(Long file,Long id){files.accessManagedItem(file);FileGuestInvitation i=guests.lockById(id).orElseThrow(()->fail(404,"Invitation not found"));if(!i.getItem().getId().equals(file))throw fail(404,"Invitation not found");i.setRevokedAt(Instant.now());i.setSessionHash(null);guests.save(i);files.invitationEvent(i,"EXTERNAL_INVITATION_REVOKED");}
    public Map<String,Object> info(String token){FileGuestInvitation i=invitation(token);return Map.of("name",i.isUploadOnly()?i.getRequestTitle():i.getItem().getName(),"recipient",i.getEmail().replaceAll("(^.).*(@.*$)","$1***$2"),"access",i.getAccess(),"expiresAt",i.getExpiresAt(),"uploadOnly",i.isUploadOnly());}
    public void requestCode(String token){
        FileGuestInvitation i=invitation(token);Instant now=Instant.now();
        if(i.getCodeSentAt()!=null&&i.getCodeSentAt().plusSeconds(60).isAfter(now))throw fail(429,"Wait one minute before requesting another code");
        if(i.getCodeWindowAt()==null||i.getCodeWindowAt().plusSeconds(3600).isBefore(now)){i.setCodeWindowAt(now);i.setCodeRequests(0);}
        if(i.getCodeRequests()>=5)throw fail(429,"Too many code requests. Try again in an hour");
        String code=String.format(Locale.ROOT,"%06d",random.nextInt(1000000));i.setCodeHash(hash(i.getTokenHash()+":"+code));i.setCodeExpiresAt(now.plusSeconds(600));i.setCodeSentAt(now);i.setCodeRequests(i.getCodeRequests()+1);i.setFailedAttempts(0);guests.save(i);
        try{mail.sendCustomEmail(i.getEmail(),"Your EAC file verification code",html("Verify your email", "Your verification code is <strong>"+code+"</strong>. It expires in 10 minutes. Do not share this code."));}catch(Exception ex){throw fail(503,"Verification email could not be submitted. Contact the sender");}
    }
    public Verification verify(String token,String code){
        FileGuestInvitation i=invitation(token);if(i.getCodeHash()==null||i.getCodeExpiresAt()==null||!i.getCodeExpiresAt().isAfter(Instant.now())||i.getFailedAttempts()>=5)return new Verification(false,null,"Code expired or too many attempts. Request a new code");
        if(code==null||!code.matches("\\d{6}")||!MessageDigest.isEqual(hash(i.getTokenHash()+":"+code).getBytes(StandardCharsets.UTF_8),i.getCodeHash().getBytes(StandardCharsets.UTF_8))){i.setFailedAttempts(i.getFailedAttempts()+1);guests.save(i);return new Verification(false,null,"Incorrect verification code");}
        String session=secret();i.setSessionHash(hash(session));i.setSessionExpiresAt(Instant.now().plusSeconds(3600).isBefore(i.getExpiresAt())?Instant.now().plusSeconds(3600):i.getExpiresAt());i.setCodeHash(null);i.setCodeExpiresAt(null);guests.save(i);files.guestEvent(i,"GUEST_EMAIL_VERIFIED");return new Verification(true,session,"Email verified");
    }
    public FileSuiteService.ItemView state(String token,String session){FileGuestInvitation i=session(token,session);if(i.isUploadOnly())throw fail(403,"Upload-only requests cannot browse files");return files.guestView(i);}
    public Object stateResponse(String token,String session){FileGuestInvitation i=session(token,session);return i.isUploadOnly()?Map.of("uploadOnly",true,"title",i.getRequestTitle(),"description",Objects.requireNonNullElse(i.getRequestDescription(),""),"maxUploadBytes",i.getMaxUploadBytes(),"remainingUploads",Math.max(0,i.getMaxUploads()-i.getUploadCount())):files.guestView(i);}
    public FileSuiteService.Download download(String token,String session){FileGuestInvitation i=session(token,session);if(i.isUploadOnly())throw fail(403,"Upload-only requests cannot download files");return files.guestDownload(i);}
    public FileSuiteService.ItemView save(String token,String session,MultipartFile file,Long revision)throws IOException{FileGuestInvitation i=session(token,session);if(i.isUploadOnly())throw fail(403,"Upload-only requests cannot edit files");return files.guestSave(i,file,revision);}
    public Map<String,Object> upload(String token,String session,MultipartFile upload)throws IOException{FileGuestInvitation i=session(token,session);if(!i.isUploadOnly()||i.getUploadCount()>=i.getMaxUploads())throw fail(403,"This upload request is closed or full");files.guestUpload(i,upload);i.setUploadCount(i.getUploadCount()+1);guests.save(i);return Map.of("received",true,"name",upload.getOriginalFilename(),"remainingUploads",i.getMaxUploads()-i.getUploadCount());}
    public record RequestView(Long id,Long folderId,String folder,String title,String description,String recipient,String access,Instant expiresAt,Instant revokedAt,int uploadCount,long maxUploadBytes){}
    public List<RequestView> requests(){var user=files.currentUser();var list=FileSuiteService.admin(user)?guests.findByUploadOnlyTrue():guests.findByGrantedBy_IdAndUploadOnlyTrue(user.getId());return list.stream().filter(i->{try{files.managedItem(i.getItem().getId());return true;}catch(ResponseStatusException e){return false;}}).map(i->new RequestView(i.getId(),i.getItem().getId(),i.getItem().getName(),i.getRequestTitle(),i.getRequestDescription(),i.getEmail(),i.getAccess(),i.getExpiresAt(),i.getRevokedAt(),i.getUploadCount(),i.getMaxUploadBytes())).toList();}
    public Created createRequest(Long folderId,String title,String description,String name,String email,Instant expiresAt,long maxBytes,boolean notify){
        var folder=files.managedItem(folderId);if(!folder.isFolder())throw fail(400,"Choose a destination folder");if(!files.externalSharing())throw fail(403,"Enable external sharing in File settings before creating requests");
        if(title==null||title.isBlank()||title.length()>180||description!=null&&description.length()>2000||name==null||name.isBlank()||name.length()>160||email==null||email.length()>254||!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw fail(400,"Enter a valid title, recipient and description");
        if(expiresAt==null||!expiresAt.isAfter(Instant.now())||expiresAt.isAfter(Instant.now().plusSeconds(90L*86400))||maxBytes<1||maxBytes>524288000L)throw fail(400,"Choose an expiry within 90 days and a valid upload limit");
        String token=secret();FileGuestInvitation i=new FileGuestInvitation();i.setItem(folder);i.setGrantedBy(files.currentUser());i.setName(name.trim());i.setEmail(email.trim().toLowerCase(Locale.ROOT));i.setAccess("UPLOAD_ONLY");i.setUploadOnly(true);i.setRequestTitle(title.trim());i.setRequestDescription(description);i.setExpiresAt(expiresAt);i.setMaxUploadBytes(maxBytes);i.setTokenHash(hash(token));guests.saveAndFlush(i);String link=frontend.replaceAll("/+$","")+"/#/file-guest?token="+token;String delivery="not_requested";
        if(notify){try{mail.sendCustomEmail(i.getEmail(),"EAC file request: "+title,html("Files requested",HtmlUtils.htmlEscape(title)+"<br>"+HtmlUtils.htmlEscape(Objects.requireNonNullElse(description,""))+"<br><a href=\""+HtmlUtils.htmlEscape(link)+"\">Submit requested files</a><br>Your invited email must be verified before uploading."));delivery="requested";}catch(Exception e){delivery="failed";}}
        files.invitationEvent(i,"FILE_REQUEST_CREATED");return new Created(view(i),link,delivery);
    }
    public void closeRequest(Long id){FileGuestInvitation i=guests.lockById(id).orElseThrow(()->fail(404,"Request not found"));if(!i.isUploadOnly())throw fail(404,"Request not found");revoke(i.getItem().getId(),id);}
}
