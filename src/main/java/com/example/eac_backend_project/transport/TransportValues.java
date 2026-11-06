package com.example.eac_backend_project.transport;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.math.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import static com.example.eac_backend_project.transport.TransportAccess.fail;

final class TransportValues {
 private TransportValues(){}
 static LocalDate today(){return LocalDate.now(ZoneId.of("Africa/Accra"));}
 static Map<String,Object> map(Object... pairs){Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);return result;}
 static Map<String,Object> row(TransportRow row){return map("id",row.id,"revision",row.revision,"createdAt",row.createdAt,"updatedAt",row.updatedAt,"archivedAt",row.archivedAt);}
 static String text(JsonNode data,String key,int max,boolean required){var value=data.get(key);if(value!=null&&!value.isNull()&&!value.isTextual())throw fail(400,key+" must be text");String text=value==null||value.isNull()?null:value.isTextual()?value.asText().trim():null;if(required&&(text==null||text.isBlank()))throw fail(400,key+" is required");if(text!=null&&(text.length()>max||text.chars().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t')))throw fail(400,key+" contains invalid text or exceeds "+max+" characters");return text==null||text.isBlank()?null:text;}
 static Long id(JsonNode data,String key,boolean required){JsonNode value=data.get(key);if(value==null||value.isNull()||value.isTextual()&&value.asText().isBlank()){if(required)throw fail(400,key+" is required");return null;}try{long id=Long.parseLong(value.asText());if(id<=0)throw new NumberFormatException();return id;}catch(Exception e){throw fail(400,key+" must be a positive identifier");}}
 static BigDecimal number(JsonNode data,String key,boolean required,boolean positive){JsonNode value=data.get(key);if(value==null||value.isNull()||value.isTextual()&&value.asText().isBlank()){if(required)throw fail(400,key+" is required");return null;}try{BigDecimal n=new BigDecimal(value.asText());if(n.signum()<0||positive&&n.signum()==0||n.precision()>19||Math.max(0,n.scale())>4)throw new NumberFormatException();return n;}catch(Exception e){throw fail(400,key+" must be a valid "+(positive?"positive":"non-negative")+" number");}}
 static LocalDate date(JsonNode data,String key,boolean required){String text=text(data,key,10,required);if(text==null)return null;try{return LocalDate.parse(text);}catch(Exception e){throw fail(400,key+" must be a valid YYYY-MM-DD date");}}
 static boolean bool(JsonNode data,String key,boolean fallback){JsonNode node=data.get(key);if(node==null||node.isNull())return fallback;if(!node.isBoolean())throw fail(400,key+" must be true or false");return node.asBoolean();}
 static Integer integer(JsonNode data,String key,boolean required,int min,int max){BigDecimal value=number(data,key,required,false);if(value==null)return null;try{int n=value.intValueExact();if(n<min||n>max)throw new ArithmeticException();return n;}catch(Exception e){throw fail(400,key+" must be a whole number between "+min+" and "+max);}}
 static String choice(JsonNode data,String key,Set<String> options,String fallback){String value=text(data,key,60,false);if(value==null)value=fallback;if(value==null||!options.contains(value))throw fail(400,"Invalid "+key);return value;}
 static void revision(TransportRow row,JsonNode data){JsonNode value=data.get("revision");if(value==null||!value.canConvertToLong()||value.asLong()!=row.revision)throw fail(409,"This record changed. Refresh it before saving.");}
 static String email(String value){if(value==null||value.isBlank())return null;String v=value.trim();if(v.length()>254||!v.matches("^[^\\s@,;<>]+@[^\\s@,;<>]+\\.[^\\s@,;<>]+$"))throw fail(400,"Enter a valid email address");return v;}
 static String emails(String values){if(values==null||values.isBlank())return null;LinkedHashSet<String> emails=new LinkedHashSet<>();for(String value:values.split("[,;\\n]"))if(!value.isBlank())emails.add(email(value));if(emails.size()>30)throw fail(400,"Choose at most 30 notification recipients");return String.join(",",emails);}
 static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 static BigDecimal decimal(Double value){return value==null?null:BigDecimal.valueOf(value);}
 static BigDecimal money(BigDecimal value){return value==null?BigDecimal.ZERO:value.setScale(2,RoundingMode.HALF_UP);}
 static String html(String text){return Objects.toString(text,"").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
}
