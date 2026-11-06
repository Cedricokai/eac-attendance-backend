package com.example.eac_backend_project.communication;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** AES-256-GCM envelopes. Context authenticates purpose and record identity. Never falls back to plaintext. */
@Component public class CommunicationCrypto {
 private final byte[] key;
 private final SecureRandom random=new SecureRandom();
 public CommunicationCrypto(@Value("${app.communication.key:}")String encoded){
  byte[] candidate=null;try{candidate=Base64.getDecoder().decode(encoded);}catch(IllegalArgumentException ignored){}
  key=candidate!=null&&candidate.length==32?candidate:null;
 }
 public boolean available(){return key!=null;}
 public byte[] seal(byte[] value,String context){return crypt(value,context,true);}
 public byte[] open(byte[] value,String context){return crypt(value,context,false);}
 private byte[] crypt(byte[] value,String context,boolean encrypt){
  if(key==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Communication encryption key is not configured");
  try{byte[] nonce=new byte[12];if(encrypt)random.nextBytes(nonce);else{if(value.length<29||value[0]!=1)throw new IllegalArgumentException();System.arraycopy(value,1,nonce,0,12);}
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(encrypt?Cipher.ENCRYPT_MODE:Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
   if(!encrypt)return cipher.doFinal(value,13,value.length-13);
   byte[] result=cipher.doFinal(value),envelope=new byte[result.length+13];envelope[0]=1;System.arraycopy(nonce,0,envelope,1,12);System.arraycopy(result,0,envelope,13,result.length);return envelope;
  }catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Encrypted communication content could not be verified");}
 }
}
