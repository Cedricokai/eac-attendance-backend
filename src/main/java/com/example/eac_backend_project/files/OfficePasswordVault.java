package com.example.eac_backend_project.files;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.crypt.*;
/** Verified passwords persist as authenticated ciphertext. Plaintext exists only during authorized verification/use. */
@Component
public class OfficePasswordVault {
 private record Key(char[] password,Instant expires){}
 private final OfficePasswordStore store;
 public OfficePasswordVault(OfficePasswordStore store){this.store=store;}
 private final Map<Long,Key> keys=new ConcurrentHashMap<>();
 private final Map<Long,byte[]> pending=new ConcurrentHashMap<>();
 static boolean encrypted(byte[] data){return data.length>=8&&data[0]==(byte)0xd0&&data[1]==(byte)0xcf&&data[2]==0x11&&data[3]==(byte)0xe0;}
 @org.springframework.scheduling.annotation.Scheduled(fixedDelay=60000) public synchronized void clearExpired(){for(Long id:new ArrayList<>(keys.keySet())){Key k=keys.get(id);if(k!=null&&!k.expires().isAfter(Instant.now())){keys.remove(id);Arrays.fill(k.password(),'\0');}}}
 private synchronized Key key(Long id){clearExpired();Key k=keys.get(id);if(k==null){char[] secret=store.load(id);if(secret!=null){k=new Key(secret,Instant.now().plusSeconds(1800));keys.put(id,k);}}return k;}
 public synchronized boolean needsPassword(Long id){return pending.containsKey(id);}
 private synchronized void remember(Long id,byte[] bytes){if(bytes.length>64*1024*1024||(!pending.containsKey(id)&&pending.size()>=8)||pending.entrySet().stream().filter(e->!e.getKey().equals(id)).mapToLong(e->e.getValue().length).sum()+bytes.length>128*1024*1024L)throw new ResponseStatusException(PAYLOAD_TOO_LARGE,"Encrypted verification capacity reached; original retained");pending.put(id,bytes.clone());}
 private byte[] decrypt(byte[] bytes,char[] password)throws IOException{if(bytes.length>64*1024*1024)throw new ResponseStatusException(PAYLOAD_TOO_LARGE,"Encrypted verification supports files up to 64 MB");try(var fs=new POIFSFileSystem(new ByteArrayInputStream(bytes))){if(!fs.getRoot().hasEntry("EncryptionInfo")||!fs.getRoot().hasEntry("EncryptedPackage"))throw new IOException("This is not a password-encrypted Office package");var info=new EncryptionInfo(fs);var decryptor=Decryptor.getInstance(info);if(!decryptor.verifyPassword(new String(password)))throw new ResponseStatusException(PRECONDITION_REQUIRED,"The file opening password is incorrect or has changed. Verify it again in EAC.");if(info.getEncryptionMode()==EncryptionMode.agile)return decryptAgile(fs,info,decryptor);try(var in=decryptor.getDataStream(fs)){byte[] data=in.readNBytes(64*1024*1024+1);if(data.length>64*1024*1024)throw new IOException("Decrypted file exceeds verification limits");return data;}}catch(java.security.GeneralSecurityException e){throw new IOException("Could not verify encrypted Office document",e);}}
 // POI 5.2.3 truncates a partial final Agile cipher block. Read complete AES blocks,
 // then trim each 4096-byte segment to the package length, preserving every plaintext byte.
 private byte[] decryptAgile(POIFSFileSystem fs,EncryptionInfo info,Decryptor decryptor)throws IOException,java.security.GeneralSecurityException {
  if(info.getHeader().getBlockSize()!=16||info.getHeader().getChainingMode()!=ChainingMode.cbc)throw new IOException("Unsupported Office Agile encryption mode");
  try(var stream=fs.createDocumentInputStream("EncryptedPackage");var plain=new ByteArrayOutputStream()){
   long remaining=stream.readLong();if(remaining<0||remaining>64*1024*1024)throw new IOException("Decrypted file exceeds verification limits");int block=0;
   while(remaining>0){int count=(int)Math.min(4096,remaining);int padded=((count+15)/16)*16;byte[] input=new byte[padded];stream.readFully(input);byte[] index=java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(block++).array();byte[] iv=CryptoFunctions.generateIv(info.getHeader().getHashAlgorithm(),info.getHeader().getKeySalt(),index,16);var cipher=javax.crypto.Cipher.getInstance("AES/CBC/NoPadding");cipher.init(javax.crypto.Cipher.DECRYPT_MODE,decryptor.getSecretKey(),new javax.crypto.spec.IvParameterSpec(iv));plain.write(cipher.doFinal(input),0,count);remaining-=count;}
   return plain.toByteArray();
  }
 }
 public synchronized byte[] decode(Long id,byte[] source,String extension)throws IOException{
  if(!encrypted(source))return source;Key k=key(id);if(k==null){remember(id,source);throw new ResponseStatusException(PRECONDITION_REQUIRED,"Verify the file opening password in EAC before saving this encrypted document. Existing versions are preserved.");}
  try{byte[] decoded=decrypt(source,k.password());OfficeDownloadClient.validatePackage(decoded,extension);pending.remove(id);return decoded;}catch(ResponseStatusException e){remember(id,source);throw e;}
 }
 public void verify(Long id,String password,byte[] stored,String extension)throws IOException{
  if(password==null||password.isEmpty()||password.length()>128)throw new ResponseStatusException(BAD_REQUEST,"Enter the file opening password (up to 128 characters)");
  byte[] candidate; synchronized(this){candidate=pending.getOrDefault(id,stored);}if(!encrypted(candidate))throw new ResponseStatusException(CONFLICT,"No encrypted file is waiting for verification. Save the document first if you just added an opening password.");
  // Never hold the vault monitor while waiting for a database foreign-key lock:
  // a save callback may own that file row and need this monitor to finish.
  char[] secret=password.toCharArray();try{byte[] decoded=decrypt(candidate,secret);OfficeDownloadClient.validatePackage(decoded,extension);store.save(id,secret);synchronized(this){Key old=keys.put(id,new Key(secret,Instant.now().plusSeconds(1800)));if(old!=null)Arrays.fill(old.password(),'\0');pending.remove(id,candidate);}}catch(IOException|RuntimeException e){Arrays.fill(secret,'\0');throw e;}
 }
 public synchronized byte[] encode(Long id,byte[] plain)throws IOException{Key k=key(id);if(k==null)throw new ResponseStatusException(PRECONDITION_REQUIRED,"Verify the file opening password again before applying protection");try(var fs=new POIFSFileSystem();var out=new ByteArrayOutputStream()){var encryptor=new EncryptionInfo(EncryptionMode.agile).getEncryptor();encryptor.confirmPassword(new String(k.password()));try(var data=encryptor.getDataStream(fs)){data.write(plain.clone());}fs.writeFilesystem(out);return out.toByteArray();}catch(java.security.GeneralSecurityException e){throw new IOException("Could not preserve file encryption",e);}}
}
