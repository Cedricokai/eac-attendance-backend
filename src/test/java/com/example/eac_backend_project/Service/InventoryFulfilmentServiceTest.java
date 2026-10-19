package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.Service.InventoryFulfilmentService.*;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.entity.finance.*;
import com.example.eac_backend_project.entity.directpurchase.*;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) @MockitoSettings(strictness=Strictness.LENIENT)
class InventoryFulfilmentServiceTest {
 @Mock InventoryRequestRepository requests;
 @Mock InventoryPurchaseLineRepository lines;
 @Mock InventoryReceiptRepository receipts;
 @Mock ProductRepository products;
 @Mock UserRepository users;
 @Mock UserService userService;
 @Mock PettyCashService pettyCash;
 @Mock DirectPurchaseService directPurchase;
 @Mock PettyCashRequestRepository pettyRequests;
 @Mock DirectPurchaseRepository directRequests;
 @Mock SettingsService settings;
 @Mock InventoryRequestService inventory;
 @Mock OutgoingRepository outgoing;
 @Mock InventoryProcurementProposalRepository proposals;
 @InjectMocks InventoryFulfilmentService service;
 InventoryRequest request;RequestItem item;Product product;User employee,actor;SystemSettings config;PettyCashRequest cash;List<InventoryPurchaseLine> allocations;
 @BeforeEach void setup(){
  auth("ADMIN");employee=new User();employee.setId(10L);employee.setName("Employee");employee.setEmail("employee@example.test");employee.setUserName("employee");actor=new User();actor.setId(20L);actor.setName("Procurement");
  request=new InventoryRequest();request.setId(1L);request.setRequestNumber("IR-1");request.setRequesterUserId(10L);request.setRequestedBy("employee");request.setStatus(RequestStatus.APPROVED_BY_STORE);
  item=new RequestItem();item.setId(2L);item.setProductId(3L);item.setProductName("Helmet");item.setRequestedQuantity(10);item.setApprovedQuantity(10);item.setIssuedQuantity(0);item.setInventoryRequest(request);request.getItems().add(item);
  product=new Product();product.setId(3L);product.setStock(4);config=new SystemSettings();config.setPettyCashPurchaseLimit(new BigDecimal("100"));allocations=new ArrayList<>();
  when(requests.findForUpdate(1L)).thenReturn(Optional.of(request));when(requests.findById(1L)).thenReturn(Optional.of(request));when(requests.saveAndFlush(any())).thenAnswer(a->a.getArgument(0));when(users.findById(10L)).thenReturn(Optional.of(employee));when(userService.getCurrentUser()).thenReturn(actor);when(settings.getSystemSettings()).thenReturn(config);
  when(products.findById(Long.valueOf(3L))).thenReturn(Optional.of(product));when(products.findForUpdate(3L)).thenReturn(Optional.of(product));when(lines.findByInventoryRequestIdOrderById(1L)).thenReturn(allocations);
  when(lines.reservedStock(3L)).thenAnswer(a->allocations.stream().mapToInt(l->l.getReceivedQuantity()-l.getIssuedQuantity()).sum());
  when(lines.save(any())).thenAnswer(a->{InventoryPurchaseLine l=a.getArgument(0);if(l.getId()==null){l.setId(100L+allocations.size());allocations.add(l);}return l;});when(lines.saveAndFlush(any())).thenAnswer(a->a.getArgument(0));when(lines.findById(anyLong())).thenAnswer(a->allocations.stream().filter(l->l.getId().equals(a.getArgument(0))).findFirst());
  cash=new PettyCashRequest();cash.setId(70L);cash.setReference("PCR-1");cash.setStatus("PENDING_ACCOUNTS");PettyCashItem ci=new PettyCashItem();ci.setId(71L);ci.setQuantity(BigDecimal.valueOf(6));ci.setActualQuantity(BigDecimal.valueOf(6));cash.getItems().add(ci);when(pettyRequests.findById(70L)).thenReturn(Optional.of(cash));when(pettyCash.createForInventory(any(),any(),eq(1L),eq("IR-1"))).thenReturn(cash);
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 void auth(String role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("procurement","",List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
 Funding funding(String route,int qty){return new Funding(route,30L,"Site safety equipment",List.of(new FundingItem(2L,qty,BigDecimal.TEN)));}
 void fund(){service.fund(1L,funding("PETTY_CASH",6));}
 @Test void keepsOriginalEmployeeAndSeparatesCreator(){fund();verify(pettyCash).createForInventory(any(),same(employee),eq(1L),eq("IR-1"));assertEquals("Procurement",allocations.get(0).getCreatedBy());assertEquals(4,product.getStock());assertEquals(6,allocations.get(0).getQuantity());}
 @Test void onlyShortageCanBeFunded(){assertThrows(IllegalArgumentException.class,()->service.fund(1L,funding("PETTY_CASH",7)));verifyNoInteractions(pettyCash);}
 @Test void duplicateFundingAcrossBothRoutesIsRejected(){fund();assertThrows(IllegalArgumentException.class,()->service.fund(1L,funding("DIRECT_PURCHASE",6)));verifyNoInteractions(directPurchase);}
 @Test void mustApproveEmployeeRequestFirst(){request.setStatus(RequestStatus.PROCUREMENT_PENDING);assertThrows(IllegalArgumentException.class,()->service.fund(1L,funding("PETTY_CASH",6)));verifyNoInteractions(pettyCash);}
 @Test void purchaseLimitIsSeparateFromFloat(){config.setPettyCashPurchaseLimit(BigDecimal.valueOf(50));assertThrows(IllegalArgumentException.class,()->service.fund(1L,funding("PETTY_CASH",6)));verifyNoInteractions(pettyCash);}
 @Test void pettyCashRequiresConfiguredLimit(){config.setPettyCashPurchaseLimit(null);assertThrows(IllegalArgumentException.class,()->service.fund(1L,funding("PETTY_CASH",6)));}
 @Test void pendingFinanceDoesNotPermitReceipt(){fund();assertThrows(IllegalArgumentException.class,()->service.receive(1L,100L,new Receipt(6,"Delivery-1")));assertEquals(4,product.getStock());verify(receipts,never()).save(any());}
 @Test void receiptAddsOnlyPurchasedQuantityOnce(){fund();cash.setStatus("VOUCHER_COMPLETED");service.receive(1L,100L,new Receipt(6,"Delivery-1"));assertEquals(10,product.getStock());verify(receipts).save(argThat(r->r.getQuantity()==6&&r.getEvidenceReference().equals("Delivery-1")));assertThrows(IllegalArgumentException.class,()->service.receive(1L,100L,new Receipt(1,"Delivery-1")));assertEquals(10,product.getStock());}
 @Test void actualPurchaseBelowEstimateReleasesUnpurchasedQuantity(){fund();cash.setStatus("VOUCHER_COMPLETED");cash.getItems().get(0).setActualQuantity(BigDecimal.valueOf(4));var summary=service.summary(1L);var rows=(List<Map<String,Object>>)summary.get("items");assertEquals(2,rows.get(0).get("shortfall"));assertThrows(IllegalArgumentException.class,()->service.receive(1L,100L,new Receipt(5,"Delivery-1")));}
 @Test void stockCanBeIssuedInPartsThenShortageFunded(){service.issue(1L,new Issue(2L,4));assertEquals(0,product.getStock());assertEquals(4,item.getIssuedQuantity());assertEquals(RequestStatus.PARTIALLY_ISSUED,request.getStatus());fund();assertEquals(6,allocations.get(0).getQuantity());}
 @Test void completeTenItemFlow(){service.issue(1L,new Issue(2L,4));fund();cash.setStatus("VOUCHER_COMPLETED");service.receive(1L,100L,new Receipt(6,"Delivery-1"));service.issue(1L,new Issue(2L,6));assertEquals(0,product.getStock());assertEquals(10,item.getIssuedQuantity());assertEquals(6,allocations.get(0).getIssuedQuantity());assertEquals(RequestStatus.ISSUED,request.getStatus());verify(outgoing,times(2)).save(any());}
 @Test void rejectedFundingReleasesShortage(){fund();cash.setStatus("REJECTED");var result=service.summary(1L);var rows=(List<Map<String,Object>>)result.get("items");assertEquals(6,rows.get(0).get("shortfall"));}
 @Test void requestOwnerCanTrackButCannotFund(){when(userService.getCurrentUser()).thenReturn(employee);auth("CUSTOMER");assertDoesNotThrow(()->service.summary(1L));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.fund(1L,funding("PETTY_CASH",6)));}
 @Test void anotherEmployeeCannotReadHistory(){auth("CUSTOMER");assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.summary(1L));}
 @Test void manualItemMustBeReceivedBeforeHandover(){item.setProductId(null);assertThrows(IllegalArgumentException.class,()->service.issue(1L,new Issue(2L,1)));assertEquals(0,item.getIssuedQuantity());}
 @Test void directPurchaseKeepsEmployeeAndRequiresPaymentBeforeReceipt(){
  DirectPurchaseResponseDTO response=new DirectPurchaseResponseDTO();response.setId(80L);response.setRequestNumber("DP-1");DirectPurchaseItemDTO di=new DirectPurchaseItemDTO();di.setId(81L);response.setItems(List.of(di));when(directPurchase.createForInventory(any(),same(employee),eq(1L),eq("IR-1"))).thenReturn(response);DirectPurchase purchase=new DirectPurchase();purchase.setStatus(DirectPurchaseStatus.PENDING);when(directRequests.findById(80L)).thenReturn(Optional.of(purchase));service.fund(1L,funding("DIRECT_PURCHASE",6));assertEquals("DIRECT_PURCHASE",allocations.get(0).getRoute());assertThrows(IllegalArgumentException.class,()->service.receive(1L,100L,new Receipt(6,"Delivery-1")));purchase.setStatus(DirectPurchaseStatus.PAID);service.receive(1L,100L,new Receipt(6,"Delivery-1"));assertEquals(10,product.getStock());verify(directPurchase).createForInventory(any(),same(employee),eq(1L),eq("IR-1"));
 }
 @Test void reservedGoodsForOtherEmployeesCannotBeIssued(){when(lines.reservedStock(3L)).thenReturn(4);assertThrows(IllegalArgumentException.class,()->service.issue(1L,new Issue(2L,1)));assertEquals(4,product.getStock());}
 @Test void manualPurchaseReceiptCanBeHandedOverWithoutInventingStock(){item.setProductId(null);item.setRequestedQuantity(6);item.setApprovedQuantity(6);fund();cash.setStatus("VOUCHER_COMPLETED");service.receive(1L,100L,new Receipt(6,"Manual-delivery"));service.issue(1L,new Issue(2L,6));assertEquals(RequestStatus.ISSUED,request.getStatus());assertEquals(6,item.getIssuedQuantity());assertEquals(4,product.getStock());verifyNoInteractions(outgoing);}
 @Test void receiptNeedsEvidence(){fund();cash.setStatus("VOUCHER_COMPLETED");assertThrows(IllegalArgumentException.class,()->service.receive(1L,100L,new Receipt(1," ")));assertEquals(4,product.getStock());}
 ProposalInput proposal(int qty){return new ProposalInput("Safety Supplier","QUOTE-1","Site equipment",List.of(new FundingItem(2L,qty,BigDecimal.TEN)));}
 InventoryProcurementProposal awaiting(){InventoryProcurementProposal p=new InventoryProcurementProposal();p.setId(50L);p.setInventoryRequestId(1L);p.setReference("IPP-1");p.setSupplier("Safety Supplier");p.setQuotationReference("QUOTE-1");p.setTotalAmount(BigDecimal.valueOf(60));p.setSubmittedBy("Procurement");InventoryProposalItem pi=new InventoryProposalItem();pi.setId(51L);pi.setRequestItemId(2L);pi.setQuantity(6);pi.setUnitCost(BigDecimal.TEN);pi.setProductName("Helmet");p.getItems().add(pi);when(proposals.findById(50L)).thenReturn(Optional.of(p));when(proposals.pendingQuantity(1L,2L)).thenAnswer(a->"AWAITING_ACCOUNTS".equals(p.getStatus())?6:0);when(proposals.findByInventoryRequestIdOrderByIdDesc(1L)).thenReturn(List.of(p));return p;}
 @Test void procurementSubmitsPricesWithoutCreatingFinanceOrStock(){auth("PROCUREMENT_OFFICER");service.submitProposal(1L,proposal(6));verify(proposals).saveAndFlush(argThat(p->p.getSupplier().equals("Safety Supplier")&&p.getTotalAmount().compareTo(BigDecimal.valueOf(60))==0&&p.getStatus().equals("AWAITING_ACCOUNTS")));verifyNoInteractions(pettyCash,directPurchase);assertEquals(4,product.getStock());}
 @Test void procurementCannotChooseFunding(){auth("PROCUREMENT_OFFICER");assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.decideProposal(1L,50L,new Decision("FUND","PETTY_CASH",null,null)));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.fund(1L,funding("PETTY_CASH",6)));}
 @Test void employeeCannotSubmitProcurementPrices(){auth("CUSTOMER");assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.submitProposal(1L,proposal(6)));}
 @Test void pendingProposalPreventsDuplicatePricing(){awaiting();auth("PROCUREMENT_OFFICER");assertThrows(IllegalArgumentException.class,()->service.submitProposal(1L,proposal(1)));verifyNoInteractions(pettyCash,directPurchase);}
 @Test void accountsFundsSavedProposalOnlyOnce(){InventoryProcurementProposal p=awaiting();auth("ACCOUNTANT");service.decideProposal(1L,50L,new Decision("FUND","PETTY_CASH",null,"Within limit"));assertEquals("FUNDED",p.getStatus());assertEquals("PETTY_CASH",p.getFundingRoute());assertEquals("PCR-1",p.getFinanceReference());assertEquals(4,product.getStock());assertThrows(IllegalArgumentException.class,()->service.decideProposal(1L,50L,new Decision("FUND","DIRECT_PURCHASE",30L,null)));verify(pettyCash,times(1)).createForInventory(any(),same(employee),eq(1L),eq("IR-1"));}
 @Test void accountsCanReturnProposalAndReleaseItsQuantity(){InventoryProcurementProposal p=awaiting();auth("ACCOUNTANT");service.decideProposal(1L,50L,new Decision("RETURN",null,null,"Please obtain a second quote"));assertEquals("RETURNED",p.getStatus());assertEquals("Procurement",p.getReviewedBy());var rows=(List<Map<String,Object>>)service.summary(1L).get("items");assertEquals(6,rows.get(0).get("shortfall"));verifyNoInteractions(pettyCash,directPurchase);}
 @Test void returnRequiresReason(){awaiting();auth("ACCOUNTANT");assertThrows(IllegalArgumentException.class,()->service.decideProposal(1L,50L,new Decision("RETURN",null,null," ")));}
 @Test void newManualRequestCanGoStraightToProcurementPricing(){item.setProductId(null);request.setStatus(RequestStatus.PROCUREMENT_PENDING);auth("PROCUREMENT_OFFICER");service.submitProposal(1L,proposal(6));assertEquals(RequestStatus.APPROVED_BY_PROCUREMENT,request.getStatus());verifyNoInteractions(pettyCash,directPurchase);}
 @Test void liveStockArrivalPreventsUnnecessaryFunding(){awaiting();auth("ACCOUNTANT");product.setStock(10);assertThrows(IllegalArgumentException.class,()->service.decideProposal(1L,50L,new Decision("FUND","PETTY_CASH",null,null)));verifyNoInteractions(pettyCash,directPurchase);}

 @Test void pendingZeroPlaceholderUsesRequestedQuantityForPricing(){request.setStatus(RequestStatus.PROCUREMENT_PENDING);item.setApprovedQuantity(0);item.setProductId(null);var rows=(List<Map<String,Object>>)service.summary(1L).get("items");assertEquals(10,rows.get(0).get("shortfall"));assertNull(rows.get(0).get("approved"));auth("PROCUREMENT_OFFICER");service.submitProposal(1L,proposal(6));assertEquals(10,item.getApprovedQuantity());assertEquals(RequestStatus.APPROVED_BY_PROCUREMENT,request.getStatus());}
 @Test void explicitZeroOnApprovedRequestIsNotExpanded(){item.setApprovedQuantity(0);assertThrows(IllegalArgumentException.class,()->service.submitProposal(1L,proposal(1)));verifyNoInteractions(pettyCash,directPurchase);}

 @Test void procurementCannotReadAccountsViewOrQueue(){auth("PROCUREMENT_OFFICER");assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.accountsSummary(1L));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.accountsQueue());}
 @Test void operationalResponseOmitsFinanceNotesRoutesAndReferencesEvenForAdmin() throws Exception {
  fund();InventoryProcurementProposal p=awaiting();p.setStatus("FUNDED");p.setReviewNotes("PRIVATE FINANCE NOTE");p.setReviewedBy("PRIVATE REVIEWER");p.setFundingRoute("PETTY_CASH");p.setFinanceRequestId(70L);p.setFinanceReference("PCR-PRIVATE");auth("ADMIN");Map<String,Object> result=service.summary(1L);String json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(result);assertFalse((Boolean)result.get("canFund"));assertFalse(json.contains("PRIVATE"));assertFalse(json.contains("fundingRoute"));assertFalse(json.contains("financeRequestId"));assertFalse(json.contains("PETTY_CASH"));assertFalse(result.containsKey("pettyCashPurchaseLimit"));assertTrue(json.contains("WITH_ACCOUNTS"));
 }
 @Test void accountsViewRetainsPrivateReviewAndFundingDetails() throws Exception {
  fund();InventoryProcurementProposal p=awaiting();p.setStatus("FUNDED");p.setReviewNotes("FINANCE INTERNAL");p.setFundingRoute("PETTY_CASH");p.setFinanceRequestId(70L);p.setFinanceReference("PCR-PRIVATE");auth("ACCOUNTANT");Map<String,Object> result=service.accountsSummary(1L);String json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(result);assertTrue((Boolean)result.get("canFund"));assertTrue(json.contains("FINANCE INTERNAL"));assertTrue(json.contains("PCR-PRIVATE"));assertTrue(json.contains("PETTY_CASH"));
 }
 @Test void procurementSeesOnlyExplicitReturnedCorrectionReason() throws Exception {
  InventoryProcurementProposal p=awaiting();p.setStatus("RETURNED");p.setReviewNotes("Obtain a second quotation");p.setReviewedBy("PRIVATE REVIEWER");auth("PROCUREMENT_OFFICER");String json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(service.summary(1L));assertTrue(json.contains("correctionReason"));assertTrue(json.contains("Obtain a second quotation"));assertFalse(json.contains("reviewNotes"));assertFalse(json.contains("PRIVATE REVIEWER"));
 }
 @Test void paidPurchaseShowsDeliveryReadinessWithoutFinanceStatus() throws Exception {
  fund();cash.setStatus("VOUCHER_COMPLETED");auth("PROCUREMENT_OFFICER");String json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(service.summary(1L));assertTrue(json.contains("READY_FOR_DELIVERY"));assertFalse(json.contains("VOUCHER_COMPLETED"));assertFalse(json.contains("PCR-1"));
 }

 @Test void fundingHistoryTracksRouteReferenceOfficerAndFinanceStatus(){InventoryProcurementProposal p=awaiting();p.setStatus("FUNDED");p.setFundingRoute("PETTY_CASH");p.setFinanceRequestId(70L);p.setFinanceReference("PCR-1");p.setReviewedBy("Accounts officer");p.setReviewedAt(java.time.LocalDateTime.of(2026,10,14,12,0));when(proposals.findAll()).thenReturn(List.of(p));auth("ACCOUNTANT");var rows=service.accountsFundingHistory();assertEquals(1,rows.size());assertEquals("IR-1",rows.get(0).get("requestNumber"));assertEquals("PETTY_CASH",rows.get(0).get("fundingRoute"));assertEquals("PCR-1",rows.get(0).get("financeReference"));assertEquals("Accounts officer",rows.get(0).get("decisionBy"));assertEquals(p.getReviewedAt(),rows.get(0).get("decisionAt"));assertEquals("PENDING_ACCOUNTS",rows.get(0).get("financeStatus"));}
 @Test void fundingHistoryIncludesReturnedAndPendingProposals(){InventoryProcurementProposal p=awaiting();when(proposals.findAll()).thenReturn(List.of(p));auth("ACCOUNTANT");assertEquals("AWAITING_ACCOUNTS",service.accountsFundingHistory().get(0).get("status"));p.setStatus("RETURNED");assertEquals("RETURNED",service.accountsFundingHistory().get(0).get("status"));}
 @Test void procurementCannotReadFundingHistory(){auth("PROCUREMENT_OFFICER");assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.accountsFundingHistory());}

}
