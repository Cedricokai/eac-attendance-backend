package com.example.eac_backend_project.Service;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class InventoryFinanceVisibilityTest {
 void auth(String role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("staff","",List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void procurementCannotReadLinkedFinance(){auth("PROCUREMENT_OFFICER");assertFalse(InventoryFinanceAccess.visible(1L));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->InventoryFinanceAccess.require(1L));}
 @Test void employeeCannotReadLinkedFinanceEvenIfOriginalRequester(){auth("CUSTOMER");assertFalse(InventoryFinanceAccess.visible(1L));}
 @Test void accountsAndFinanceApproversCanReadLinkedFinance(){for(String role:List.of("ADMIN","ACCOUNTANT","ACCOUNTS","FINANCE","REVIEWER")){auth(role);assertTrue(InventoryFinanceAccess.visible(1L));}}
 @Test void standaloneDocumentsKeepExistingAccessRules(){auth("PROCUREMENT_OFFICER");assertTrue(InventoryFinanceAccess.visible(null));}
 @Test void noAuthenticationCannotReadLinkedFinance(){SecurityContextHolder.clearContext();assertFalse(InventoryFinanceAccess.visible(1L));}
 @Test void directPurchaseServiceFiltersLinkedRecordsAndBlocksTheirDetail(){
  auth("PROCUREMENT_OFFICER");var repo=org.mockito.Mockito.mock(com.example.eac_backend_project.repository.DirectPurchaseRepository.class);var linked=new com.example.eac_backend_project.entity.directpurchase.DirectPurchase();linked.setId(1L);linked.setInventoryRequestId(10L);var standalone=new com.example.eac_backend_project.entity.directpurchase.DirectPurchase();standalone.setId(2L);org.mockito.Mockito.when(repo.findAll()).thenReturn(List.of(linked,standalone));org.mockito.Mockito.when(repo.findById(1L)).thenReturn(Optional.of(linked));var service=new DirectPurchaseService(repo,org.mockito.Mockito.mock(com.example.eac_backend_project.Service.settings.WorkflowService.class),org.mockito.Mockito.mock(com.example.eac_backend_project.Service.email.EmailService.class),org.mockito.Mockito.mock(com.example.eac_backend_project.repository.UserRepository.class),org.mockito.Mockito.mock(com.example.eac_backend_project.Service.settings.SettingsService.class));assertEquals(1,service.getAllRequests().size());assertEquals(2L,service.getAllRequests().get(0).getId());assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.getRequestById(1L));
 }
 @Test void pettyCashServiceFiltersLinkedRecordsAndBlocksReceiptDetail(){
  auth("PROCUREMENT_OFFICER");var repo=org.mockito.Mockito.mock(com.example.eac_backend_project.repository.PettyCashRequestRepository.class);var us=org.mockito.Mockito.mock(UserService.class);var actor=new com.example.eac_backend_project.entity.inventory.User();actor.setId(8L);org.mockito.Mockito.when(us.getCurrentUser()).thenReturn(actor);var linked=new com.example.eac_backend_project.entity.finance.PettyCashRequest();linked.setId(1L);linked.setInventoryRequestId(10L);var standalone=new com.example.eac_backend_project.entity.finance.PettyCashRequest();standalone.setId(2L);org.mockito.Mockito.when(repo.findByRequesterIdOrderByCreatedAtDesc(8L)).thenReturn(List.of(linked,standalone));org.mockito.Mockito.when(repo.findById(1L)).thenReturn(Optional.of(linked));var service=new PettyCashService(repo,null,null,null,us,null,null,null,null,null);assertEquals(1,service.list(false).size());assertEquals(2L,service.list(false).get(0).getId());assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.receiptInfo(1L));
 }

}
