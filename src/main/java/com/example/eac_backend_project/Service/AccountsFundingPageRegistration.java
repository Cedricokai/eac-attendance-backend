package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.inventory.Page;
import com.example.eac_backend_project.repository.PageRepository;
import com.example.eac_backend_project.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
/** Register the new Accounts workspace for the roles already allowed to choose inventory funding. */
@Component @RequiredArgsConstructor
public class AccountsFundingPageRegistration implements ApplicationRunner {
 private final PageRepository pages;
 private final RoleRepository roles;
 @Override @Transactional public void run(ApplicationArguments args){
  if(pages.findByPath("/accountsFundingReview").isPresent())return;
  Page p=new Page();p.setName("Accounts Funding Review");p.setPath("/accountsFundingReview");p.setDescription("Review procurement quotations, choose funding routes and track linked finance requests");p.setIconName("ClipboardList");p.setDisplayOrder(45);p.setModule(Page.Module.GENERAL);p.setRequiresAuth(true);p.setIsPublic(false);p.setIsActive(true);
  p.setRoles(roles.findAll().stream().filter(r->Set.of("ADMIN","ACCOUNTANT","ACCOUNTS","FINANCE").contains(r.getName().replaceFirst("^ROLE_","").toUpperCase(Locale.ROOT))).collect(Collectors.toSet()));pages.save(p);
 }
}
