package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.Service.settings.WorkflowService;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class InventoryRequestRoutingTest {
 InventoryRequestRepository requests=mock(InventoryRequestRepository.class);ProductRepository products=mock(ProductRepository.class);WorkflowService workflows=mock(WorkflowService.class);OutgoingRepository outgoing=mock(OutgoingRepository.class);
 InventoryRequestService service=new InventoryRequestService(requests,products,mock(ProductService.class),outgoing,mock(CostCenterRepository.class),mock(JobRepository.class),workflows,mock(EmailService.class));
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void stockedRequestStartsPendingStore(){InventoryRequest r=new InventoryRequest();ReflectionTestUtils.invokeMethod(service,"determineInitialStatus",r);assertEquals(RequestStatus.PENDING_STORE,r.getStatus());verifyNoInteractions(workflows);}
 @Test void missingItemStartsPendingProcurement(){InventoryRequest r=new InventoryRequest();r.setHasManualProducts(true);ReflectionTestUtils.invokeMethod(service,"determineInitialStatus",r);assertEquals(RequestStatus.PROCUREMENT_PENDING,r.getStatus());}
 @Test void ppeStartsPendingProcurement(){InventoryRequest r=new InventoryRequest();r.setPpeRequest(true);ReflectionTestUtils.invokeMethod(service,"determineInitialStatus",r);assertEquals(RequestStatus.PROCUREMENT_PENDING,r.getStatus());}
 @Test void savedLabelWithSpaceIsNormalized(){assertEquals(RequestStatus.PENDING_PLANNER,ReflectionTestUtils.invokeMethod(service,"parseRequestStatus","PENDING PLANNER"));}
 @Test void storeApprovalDoesNotAutomaticallyIssueStock(){InventoryRequest r=new InventoryRequest();r.setId(1L);r.setFinanceFulfilment(true);r.setStatus(RequestStatus.PENDING_STORE);when(requests.findById(1L)).thenReturn(Optional.of(r));when(requests.save(r)).thenReturn(r);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("store","",List.of(new SimpleGrantedAuthority("ROLE_INVENTORY"))));service.approveByStore(1L,"Store","Checked availability");assertEquals(RequestStatus.APPROVED_BY_STORE,r.getStatus());verifyNoInteractions(products,outgoing);}
 @Test void procurementApprovalKeepsRequestInPricingStage(){InventoryRequest r=new InventoryRequest();r.setId(1L);r.setFinanceFulfilment(true);r.setStatus(RequestStatus.PROCUREMENT_PENDING);when(requests.findById(1L)).thenReturn(Optional.of(r));when(requests.save(r)).thenReturn(r);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("procurement","",List.of(new SimpleGrantedAuthority("ROLE_PROCUREMENT_OFFICER"))));service.approveByProcurement(1L,"Procurement","Sourcing",60.0,null);assertEquals(RequestStatus.APPROVED_BY_PROCUREMENT,r.getStatus());verifyNoInteractions(workflows,products,outgoing);}
 @Test void pendingStoreRequestCanBeRejectedWithoutLegacyWorkflow(){InventoryRequest r=new InventoryRequest();r.setId(1L);r.setFinanceFulfilment(true);r.setStatus(RequestStatus.PENDING_STORE);when(requests.findById(1L)).thenReturn(Optional.of(r));when(requests.save(r)).thenReturn(r);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("store","",List.of(new SimpleGrantedAuthority("ROLE_INVENTORY"))));service.rejectByStore(1L,"Store","Not required");assertEquals(RequestStatus.REJECTED_BY_STORE,r.getStatus());verifyNoInteractions(workflows,products,outgoing);}
 @Test void storeCannotApproveProcurementStage(){InventoryRequest r=new InventoryRequest();r.setId(1L);r.setFinanceFulfilment(true);r.setStatus(RequestStatus.PROCUREMENT_PENDING);when(requests.findById(1L)).thenReturn(Optional.of(r));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("store","",List.of(new SimpleGrantedAuthority("ROLE_STORE_OFFICER"))));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.approveByProcurement(1L,"Store",null,60.0,null));assertEquals(RequestStatus.PROCUREMENT_PENDING,r.getStatus());}

 @Test void pricedManualRequestStaysVisibleInProcurementHistory(){InventoryRequest r=new InventoryRequest();r.setFinanceFulfilment(true);r.setHasManualProducts(true);r.setStatus(RequestStatus.APPROVED_BY_PROCUREMENT);assertEquals(true,ReflectionTestUtils.invokeMethod(service,"shouldDisplayForProcurement",r));r.setStatus(RequestStatus.ISSUED);assertEquals(true,ReflectionTestUtils.invokeMethod(service,"shouldDisplayForProcurement",r));}

}
