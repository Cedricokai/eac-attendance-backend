package com.example.eac_backend_project.files;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component @ConfigurationProperties(prefix="app.onlyoffice") @Getter @Setter
public class OfficeSettings {
 private boolean enabled=false;
 private String browserUrl="http://localhost:8082";
 private String serverUrl="http://localhost:8082";
 private String connectorUrl="http://host.docker.internal:8080";
 private String jwtSecret="";
 private int tokenMinutes=30;
 private long maxSaveBytes=100*1024*1024;
 private boolean protectedRangesVerified=false;
 private Long templateFolderId;
}
