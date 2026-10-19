package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AccountsFundingPageRegistrationTest {
 @Test void registersOnlyExistingAccountsRoles(){PageRepository pages=mock(PageRepository.class);RoleRepository roles=mock(RoleRepository.class);when(pages.findByPath("/accountsFundingReview")).thenReturn(Optional.empty());when(roles.findAll()).thenReturn(List.of(new Role("ROLE_ADMIN",true),new Role("ROLE_ACCOUNTANT",true),new Role("ROLE_PROCUREMENT_OFFICER",true)));new AccountsFundingPageRegistration(pages,roles).run(null);verify(pages).save(argThat(p->p.getPath().equals("/accountsFundingReview")&&!p.getIsPublic()&&p.getRoles().size()==2&&p.getRoles().stream().noneMatch(r->r.getName().contains("PROCUREMENT"))));}
 @Test void preservesExistingPagePermissionConfiguration(){PageRepository pages=mock(PageRepository.class);RoleRepository roles=mock(RoleRepository.class);when(pages.findByPath("/accountsFundingReview")).thenReturn(Optional.of(new Page()));new AccountsFundingPageRegistration(pages,roles).run(null);verify(pages,never()).save(any());verifyNoInteractions(roles);}
}
