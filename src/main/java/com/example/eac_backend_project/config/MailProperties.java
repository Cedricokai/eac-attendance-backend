package com.example.eac_backend_project.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {
    private String host;
    private int port;
    private String username;
    private String password;
    private String protocol = "smtp";
    private boolean auth = true;
    private boolean starttlsEnable = true;
    private boolean starttlsRequired = true;
    private String debug = "false";
    private String smtpAuth = "true";
    private String smtpStarttlsEnable = "true";
    private String smtpStarttlsRequired = "true";
    private String connectiontimeout = "5000";
    private String timeout = "5000";
    private String writetimeout = "5000";

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public boolean isAuth() { return auth; }
    public void setAuth(boolean auth) { this.auth = auth; }
    public boolean isStarttlsEnable() { return starttlsEnable; }
    public void setStarttlsEnable(boolean starttlsEnable) { this.starttlsEnable = starttlsEnable; }
    public boolean isStarttlsRequired() { return starttlsRequired; }
    public void setStarttlsRequired(boolean starttlsRequired) { this.starttlsRequired = starttlsRequired; }
    public String getDebug() { return debug; }
    public void setDebug(String debug) { this.debug = debug; }
    public String getSmtpAuth() { return smtpAuth; }
    public void setSmtpAuth(String smtpAuth) { this.smtpAuth = smtpAuth; }
    public String getSmtpStarttlsEnable() { return smtpStarttlsEnable; }
    public void setSmtpStarttlsEnable(String smtpStarttlsEnable) { this.smtpStarttlsEnable = smtpStarttlsEnable; }
    public String getSmtpStarttlsRequired() { return smtpStarttlsRequired; }
    public void setSmtpStarttlsRequired(String smtpStarttlsRequired) { this.smtpStarttlsRequired = smtpStarttlsRequired; }
    public String getConnectiontimeout() { return connectiontimeout; }
    public void setConnectiontimeout(String connectiontimeout) { this.connectiontimeout = connectiontimeout; }
    public String getTimeout() { return timeout; }
    public void setTimeout(String timeout) { this.timeout = timeout; }
    public String getWritetimeout() { return writetimeout; }
    public void setWritetimeout(String writetimeout) { this.writetimeout = writetimeout; }
}