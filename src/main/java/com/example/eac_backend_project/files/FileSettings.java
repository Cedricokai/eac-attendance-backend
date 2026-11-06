package com.example.eac_backend_project.files;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="file_settings") @Getter @Setter
public class FileSettings {
    @Id private Long id=1L;
    private long maxUploadBytes=52428800L;
    private long userQuotaBytes=0L; // Zero means unlimited.
    private long organizationQuotaBytes=0L;
    private int maxFilenameLength=180;
    @Column(length=1000) private String blockedExtensions="exe,msi,bat,cmd,com,scr,ps1,sh,jar,html,htm,svg,js,vbs";
    private boolean internalSharing=true;
    private boolean externalSharing=false;
    private boolean collaborationEnabled=true;
    @Column(length=160) private String disabledEditors="";
    // Zero keeps all content. Administrators explicitly opt in to deletion.
    private int recycleRetentionDays=0;
    private int versionRetentionDays=0;
    private int auditRetentionDays=0;
    private boolean emailNotifications=false;
    private java.time.Instant lastOrganizationStorageNotice;
    @Version private long revision;
}
