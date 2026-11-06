package com.example.eac_backend_project.transport;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import com.example.eac_backend_project.repository.AttachmentRepository;
import org.springframework.web.util.UriUtils;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

/** Managed Transport files are served exclusively by authenticated, ACL-checked API routes. */
@Component @Order(Ordered.LOWEST_PRECEDENCE-20)
public class TransportPrivateFilesFilter extends OncePerRequestFilter {
 private final TransportAssets assets;private final AttachmentRepository legacy;
 public TransportPrivateFilesFilter(TransportAssets assets,AttachmentRepository legacy){this.assets=assets;this.legacy=legacy;}
 @Override protected boolean shouldNotFilter(HttpServletRequest request){String path=request.getRequestURI();return path.startsWith(request.getContextPath()+"/api/")||path.startsWith(request.getContextPath()+"/auth/")||!"GET".equals(request.getMethod())&&!"HEAD".equals(request.getMethod());}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String path=UriUtils.decode(request.getRequestURI().substring(request.getContextPath().length()),StandardCharsets.UTF_8);String last=path.substring(path.lastIndexOf('/')+1);boolean blocked=last.matches("[a-f0-9-]{36}")&&assets.storedKey(last);
  if(!blocked){Path root=Path.of("uploads").toAbsolutePath().normalize();Path candidate=root.resolve(path.replaceFirst("^/+","")).normalize();blocked=candidate.startsWith(root)&&legacy.existsByFilePath(candidate.toString());}
  if(blocked){response.sendError(404);return;}chain.doFilter(request,response);
 }
}
