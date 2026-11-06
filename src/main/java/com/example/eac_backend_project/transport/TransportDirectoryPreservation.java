package com.example.eac_backend_project.transport;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Local ddl-auto setups receive the same non-destructive copy as SQL migration V11. */
@Component
public class TransportDirectoryPreservation implements ApplicationRunner {
 private final JdbcTemplate jdbc;
 public TransportDirectoryPreservation(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Override @Transactional public void run(ApplicationArguments args){
  Set<String> columns=jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Set<String>>)connection->{Set<String> found=new HashSet<>();try(var rows=connection.getMetaData().getColumns(connection.getCatalog(),null,"%","%")){while(rows.next())if("users".equalsIgnoreCase(rows.getString("TABLE_NAME")))found.add(rows.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));}return found;});
  if(columns!=null&&columns.containsAll(Set.of("id","full_name","email","role","phone")))jdbc.update("INSERT INTO transport_directory (id,full_name,email,role,phone) SELECT u.id,u.full_name,u.email,u.role,u.phone FROM users u WHERE u.full_name IS NOT NULL AND TRIM(u.full_name)<>'' AND NOT EXISTS (SELECT 1 FROM transport_directory d WHERE d.id=u.id)");
 }
}
