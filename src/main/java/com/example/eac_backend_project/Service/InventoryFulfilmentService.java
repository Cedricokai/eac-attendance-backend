package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.dto.*;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.entity.finance.*;
import com.example.eac_backend_project.entity.directpurchase.*;
import com.example.eac_backend_project.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Employee request -> shortage funding -> paid purchase receipt -> employee issue. */
@Service @RequiredArgsConstructor
public class InventoryFulfilmentService {
 private final InventoryRequestRepository requests;
 private final InventoryPurchaseLineRepository lines;
 private final InventoryReceiptRepository receipts;
 private final ProductRepository products;
 private final UserRepository users;
 private final UserService userService;
 private final PettyCashService pettyCash;
 private final DirectPurchaseService directPurchase;
 private final PettyCashRequestRepository pettyRequests;
 private final DirectPurchaseRepository directRequests;
 private final SettingsService settings;
 private final InventoryRequestService inventory;
 private final OutgoingRepository outgoing;
 private final InventoryProcurementProposalRepository proposals;

 public record FundingItem(@NotNull Long itemId, @Min(1) int quantity, @NotNull @DecimalMin("0.00") BigDecimal unitCost) {}
 public record Funding(@NotBlank String route, Long accountantUserId, @Size(max=800) String notes, @NotEmpty List<@Valid FundingItem> items) {}
 public record Issue(@NotNull Long itemId, @Min(1) int quantity) {}
 public record Receipt(@Min(1) int quantity, @NotBlank @Size(max=500) String evidenceReference) {}

 private boolean role(String... values){var auth=SecurityContextHolder.getContext().getAuthentication();return auth!=null&&auth.getAuthorities().stream().anyMatch(a->Arrays.asList(values).contains(a.getAuthority().replaceFirst("^ROLE_","")));}
 private boolean canPropose(){return role("ADMIN","PROCUREMENT_OFFICER","PROCUREMENT");}
 private boolean canFund(){return role("ADMIN","ACCOUNTANT","ACCOUNTS","FINANCE");}
 private boolean canStore(){return role("ADMIN","INVENTORY","STORE","STORE_OFFICER","STOREKEEPER");}
 private User origin(InventoryRequest r){
  if(r.getRequesterUserId()!=null)return users.findById(r.getRequesterUserId()).orElseThrow(()->new IllegalArgumentException("Original employee account no longer exists"));
  if(r.getRequestedBy()==null)throw new IllegalArgumentException("Original requester is missing");
  return users.findByUserName(r.getRequestedBy()).or(()->users.findByEmail(r.getRequestedBy())).orElseThrow(()->new IllegalArgumentException("Original requester must be linked to a user account before funding"));
 }
 private void view(InventoryRequest r){if(canPropose()||canFund()||canStore()||role("ACCOUNTANT","ACCOUNTS","FINANCE","HR","PLANNER"))return;if(!Objects.equals(origin(r).getId(),userService.getCurrentUser().getId()))throw new AccessDeniedException("This inventory request belongs to another employee");}
 private InventoryRequest lock(Long id){return requests.findForUpdate(id).orElseThrow(()->new IllegalArgumentException("Inventory request not found"));}
 private RequestItem item(InventoryRequest r,Long id){return r.getItems().stream().filter(i->Objects.equals(i.getId(),id)).findFirst().orElseThrow(()->new IllegalArgumentException("Item does not belong to this request"));}
 private int n(Integer i){return i==null?0:i;}
 private boolean pending(RequestItem i){return i.getInventoryRequest()!=null && Set.of(RequestStatus.PENDING,RequestStatus.PENDING_PLANNER,RequestStatus.PENDING_STORE,RequestStatus.PROCUREMENT_PENDING).contains(i.getInventoryRequest().getStatus());}
 private int approved(RequestItem i){Integer quantity=i.getApprovedQuantity();if(quantity==null||(quantity==0&&pending(i)))quantity=i.getRequestedQuantity();return Math.max(0,n(quantity));}
 private void approvedRequest(InventoryRequest r){
  if(!Set.of(RequestStatus.APPROVED_BY_STORE,RequestStatus.APPROVED_BY_PROCUREMENT,RequestStatus.APPROVED_BY_PLANNER,RequestStatus.PROCURED,RequestStatus.STORE_REVIEW,RequestStatus.RECEIVED_IN_STORE,RequestStatus.PARTIALLY_ISSUED).contains(r.getStatus()))throw new IllegalArgumentException("Approve the employee inventory request before funding or issuing items");
 }
 private List<InventoryPurchaseLine> itemLines(List<InventoryPurchaseLine> all,RequestItem i){return all.stream().filter(l->Objects.equals(l.getRequestItemId(),i.getId())).toList();}
 private String status(InventoryPurchaseLine l){
  if("PETTY_CASH".equals(l.getRoute()))return pettyRequests.findById(l.getFinanceRequestId()).map(PettyCashRequest::getStatus).orElse("MISSING");
  return directRequests.findById(l.getFinanceRequestId()).map(p->p.getStatus().name()).orElse("MISSING");
 }
 private int purchased(InventoryPurchaseLine l){
  String status=status(l);if(Set.of("REJECTED","MISSING").contains(status))return l.getReceivedQuantity();
  if("PETTY_CASH".equals(l.getRoute())&&"VOUCHER_COMPLETED".equals(status))return pettyRequests.findById(l.getFinanceRequestId()).orElseThrow().getItems().stream().filter(i->Objects.equals(i.getId(),l.getFinanceItemId())).findFirst().map(i->i.getActualQuantity().intValueExact()).orElse(0);
  return l.getQuantity();
 }
 private int reserved(List<InventoryPurchaseLine> all,RequestItem item){return itemLines(all,item).stream().mapToInt(l->Math.max(0,purchased(l)-l.getIssuedQuantity())).sum();}
 private int freeStock(RequestItem i){if(i.getProductId()==null)return 0;return products.findById(i.getProductId()).map(p->Math.max(0,p.getStock()-lines.reservedStock(p.getId()))).orElse(0);}
 private int shortfall(RequestItem i,List<InventoryPurchaseLine> all){return Math.max(0,approved(i)-n(i.getIssuedQuantity())-reserved(all,i)-freeStock(i)-proposals.pendingQuantity(i.getInventoryRequest().getId(),i.getId()));}
 private void lockProducts(InventoryRequest r){r.getItems().stream().map(RequestItem::getProductId).filter(Objects::nonNull).distinct().sorted().forEach(id->products.findForUpdate(id).orElseThrow(()->new IllegalArgumentException("Product not found")));}

 @Transactional(readOnly=true) public List<Map<String,Object>> requestList(){
  if(!canPropose()&&!canFund()&&!canStore())throw new AccessDeniedException("Store or procurement permission is required");
  return requests.findAll().stream().map(r->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getId());m.put("requestNumber",r.getRequestNumber());m.put("requesterName",r.getRequesterName());m.put("requestedBy",r.getRequestedBy());m.put("status",r.getStatus());return m;}).toList();
 }
 @Transactional(readOnly=true) public Map<String,Object> summary(Long id){return buildSummary(id,false);}
 @Transactional(readOnly=true) public Map<String,Object> accountsSummary(Long id){if(!canFund())throw new AccessDeniedException("Accounts permission is required to see funding decisions");return buildSummary(id,true);}
 private Map<String,Object> buildSummary(Long id,boolean accountsView){
  InventoryRequest r=requests.findById(id).orElseThrow();view(r);List<InventoryPurchaseLine> all=lines.findByInventoryRequestIdOrderById(id);
  List<Map<String,Object>> rows=new ArrayList<>();
  for(RequestItem i:r.getItems()){
   Map<String,Object> row=new LinkedHashMap<>();row.put("id",i.getId());row.put("name",i.getProductName());row.put("productId",i.getProductId());row.put("requested",n(i.getRequestedQuantity()));row.put("approved",pending(i)?null:approved(i));row.put("issued",n(i.getIssuedQuantity()));row.put("outstanding",Math.max(0,approved(i)-n(i.getIssuedQuantity())));row.put("fundedOutstanding",reserved(all,i));row.put("awaitingAccounts",proposals.pendingQuantity(id,i.getId()));row.put("availableStock",freeStock(i));row.put("shortfall",shortfall(i,all));row.put("estimatedUnitCost",i.getUnitCost());
   int own=itemLines(all,i).stream().mapToInt(l->l.getReceivedQuantity()-l.getIssuedQuantity()).sum();row.put("readyToIssue",Math.min(Math.max(0,approved(i)-n(i.getIssuedQuantity())),freeStock(i)+own));rows.add(row);
  }
  List<Map<String,Object>> history=new ArrayList<>();for(var l:all){Map<String,Object> h=new LinkedHashMap<>();h.put("allocation",accountsView?l:operationalAllocation(l));h.put("status",accountsView?status(l):deliveryProgress(l));h.put("readyToReceive",Set.of("VOUCHER_COMPLETED","PAID").contains(status(l)));h.put("purchasedQuantity",Set.of("VOUCHER_COMPLETED","PAID").contains(status(l))?purchased(l):0);h.put("receipts",receipts.findByPurchaseLineIdOrderById(l.getId()));history.add(h);}
  Map<String,Object> result=new LinkedHashMap<>();result.put("requestId",id);result.put("requestNumber",r.getRequestNumber());result.put("requester",r.getRequesterName()!=null?r.getRequesterName():r.getRequestedBy());result.put("project",r.getProjectName());result.put("costCenter",r.getCostCenterName());result.put("status",r.getStatus());result.put("items",rows);result.put("history",history);result.put("canFund",accountsView&&canFund());result.put("accountsView",accountsView);result.put("canPropose",canPropose());result.put("proposals",proposals.findByInventoryRequestIdOrderByIdDesc(id).stream().map(p->proposalView(p,all,accountsView)).toList());result.put("canStore",canStore());result.put("canConfigure",accountsView&&role("ADMIN"));if(accountsView)result.put("pettyCashPurchaseLimit",settings.getSystemSettings().getPettyCashPurchaseLimit());return result;
 }


 private String deliveryProgress(InventoryPurchaseLine l){
  int actual=purchased(l);if(actual>0&&l.getIssuedQuantity()>=actual)return "HANDED_OVER";
  if(actual>0&&l.getReceivedQuantity()>=actual)return "RECEIVED";
  if(Set.of("VOUCHER_COMPLETED","PAID").contains(status(l)))return "READY_FOR_DELIVERY";
  return "WITH_ACCOUNTS";
 }
 private Map<String,Object> operationalAllocation(InventoryPurchaseLine l){
  Map<String,Object> m=new LinkedHashMap<>();m.put("id",l.getId());m.put("requestItemId",l.getRequestItemId());m.put("quantity",l.getQuantity());m.put("receivedQuantity",l.getReceivedQuantity());m.put("issuedQuantity",l.getIssuedQuantity());return m;
 }
 private Map<String,Object> proposalView(InventoryProcurementProposal p,List<InventoryPurchaseLine> all,boolean accountsView){
  Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.getId());m.put("reference",p.getReference());m.put("supplier",p.getSupplier());m.put("quotationReference",p.getQuotationReference());m.put("totalAmount",p.getTotalAmount());m.put("notes",p.getNotes());m.put("submittedBy",p.getSubmittedBy());m.put("submittedAt",p.getSubmittedAt());
  m.put("items",p.getItems().stream().map(i->{Map<String,Object> v=new LinkedHashMap<>();v.put("id",i.getId());v.put("requestItemId",i.getRequestItemId());v.put("productName",i.getProductName());v.put("quantity",i.getQuantity());v.put("unitCost",i.getUnitCost());return v;}).toList());
  String progress=p.getStatus();if("FUNDED".equals(progress))progress=all.stream().filter(l->Objects.equals(l.getFinanceRequestId(),p.getFinanceRequestId())&&Objects.equals(l.getRoute(),p.getFundingRoute())).map(this::deliveryProgress).findFirst().orElse("WITH_ACCOUNTS");
  m.put("status",accountsView?p.getStatus():progress);
  if("RETURNED".equals(p.getStatus()))m.put("correctionReason",p.getReviewNotes());
  if(accountsView){m.put("reviewedBy",p.getReviewedBy());m.put("reviewedAt",p.getReviewedAt());m.put("reviewNotes",p.getReviewNotes());m.put("fundingRoute",p.getFundingRoute());m.put("financeRequestId",p.getFinanceRequestId());m.put("financeReference",p.getFinanceReference());}
  return m;
 }

 @Transactional Map<String,Object> fund(Long id,Funding input){
  if(!canFund())throw new AccessDeniedException("Accounts permission is required to choose funding");
  InventoryRequest r=lock(id);approvedRequest(r);lockProducts(r);User employee=origin(r);User actor=userService.getCurrentUser();
  if(input.route()==null||!Set.of("PETTY_CASH","DIRECT_PURCHASE").contains(input.route()))throw new IllegalArgumentException("Choose petty cash or direct purchase");
  List<InventoryPurchaseLine> existing=lines.findByInventoryRequestIdOrderById(id);Set<Long> seen=new HashSet<>();BigDecimal total=BigDecimal.ZERO;
  // Stock is counted once even if a request contains the same product on multiple lines.
  if(r.getItems().stream().map(RequestItem::getProductId).filter(Objects::nonNull).distinct().count()!=r.getItems().stream().filter(i->i.getProductId()!=null).count())throw new IllegalArgumentException("Consolidate duplicate product lines before funding this request");
  for(FundingItem f:input.items()){if(!seen.add(f.itemId()))throw new IllegalArgumentException("Duplicate funding item");RequestItem i=item(r,f.itemId());if(f.quantity()<=0||f.quantity()>shortfall(i,existing))throw new IllegalArgumentException("Funding exceeds the unfunded stock shortage for "+i.getProductName());if(f.unitCost()==null||f.unitCost().signum()<0||f.unitCost().scale()>2)throw new IllegalArgumentException("Unit cost must be a non-negative amount with at most two decimals");total=total.add(f.unitCost().multiply(BigDecimal.valueOf(f.quantity())));}
  if(total.signum()<=0)throw new IllegalArgumentException("Purchase amount must be greater than zero");
  Long financeId;String reference;List<Long> financeItemIds;
  String purpose="Inventory "+r.getRequestNumber()+" — "+Objects.toString(input.notes(),"Purchase shortage");
  if("PETTY_CASH".equals(input.route())){
   BigDecimal limit=settings.getSystemSettings().getPettyCashPurchaseLimit();
   BigDecimal prior=existing.stream().filter(l->"PETTY_CASH".equals(l.getRoute())).map(l->l.getUnitCost().multiply(BigDecimal.valueOf(purchased(l)))).reduce(BigDecimal.ZERO,BigDecimal::add);
   if(limit==null||limit.signum()<=0)throw new IllegalArgumentException("Admin must configure a petty-cash purchase limit first");
   if(prior.add(total).compareTo(limit)>0)throw new IllegalArgumentException("This request exceeds the petty-cash purchase limit; use direct purchase");
   List<PettyCashItemDTO> items=input.items().stream().map(f->new PettyCashItemDTO(item(r,f.itemId()).getProductName(),BigDecimal.valueOf(f.quantity()),f.unitCost())).toList();
   PettyCashRequest saved=pettyCash.createForInventory(new PettyCashRequestDTO("Inventory purchase",purpose,r.getCostCenterName(),items),employee,id,r.getRequestNumber());financeId=saved.getId();reference=saved.getReference();financeItemIds=saved.getItems().stream().map(PettyCashItem::getId).toList();
  }else{
   DirectPurchaseRequestDTO dto=new DirectPurchaseRequestDTO();dto.setAccountantUserId(input.accountantUserId());dto.setNotes(purpose);dto.setItems(input.items().stream().map(f->{DirectPurchaseItemDTO d=new DirectPurchaseItemDTO();d.setDescription(item(r,f.itemId()).getProductName());d.setQuantity(f.quantity());d.setUnitPrice(f.unitCost().doubleValue());return d;}).toList());
   DirectPurchaseResponseDTO saved=directPurchase.createForInventory(dto,employee,id,r.getRequestNumber());financeId=saved.getId();reference=saved.getRequestNumber();financeItemIds=saved.getItems().stream().map(DirectPurchaseItemDTO::getId).toList();
  }
  for(int index=0;index<input.items().size();index++){FundingItem f=input.items().get(index);InventoryPurchaseLine l=new InventoryPurchaseLine();l.setInventoryRequestId(id);l.setRequestItemId(f.itemId());l.setProductId(item(r,f.itemId()).getProductId());l.setRoute(input.route());l.setFinanceRequestId(financeId);l.setFinanceItemId(financeItemIds.get(index));l.setFinanceReference(reference);l.setQuantity(f.quantity());l.setUnitCost(f.unitCost());l.setCreatedBy(actor.getName());lines.save(l);}
  r.setFinanceFulfilment(true);requests.saveAndFlush(r);return summary(id);
 }


 public record ProposalInput(@NotBlank @Size(max=255) String supplier,@NotBlank @Size(max=500) String quotationReference,@Size(max=800) String notes,@NotEmpty List<@Valid FundingItem> items) {}
 public record Decision(@NotBlank String action,String route,Long accountantUserId,@Size(max=800) String notes) {}

 @Transactional(readOnly=true) public List<Map<String,Object>> accountsFundingHistory(){
  if(!canFund())throw new AccessDeniedException("Accounts permission is required");
  return proposals.findAll().stream().sorted(Comparator.comparing(InventoryProcurementProposal::getSubmittedAt,Comparator.nullsLast(Comparator.reverseOrder()))).map(p->{
   InventoryRequest r=requests.findById(p.getInventoryRequestId()).orElseThrow();Map<String,Object> m=new LinkedHashMap<>();
   m.put("id",p.getId());m.put("requestId",r.getId());m.put("requestNumber",r.getRequestNumber());m.put("requesterName",r.getRequesterName()!=null?r.getRequesterName():r.getRequestedBy());m.put("project",r.getProjectName());m.put("requestStatus",r.getStatus());m.put("proposalReference",p.getReference());m.put("supplier",p.getSupplier());m.put("totalAmount",p.getTotalAmount());m.put("status",p.getStatus());m.put("submittedAt",p.getSubmittedAt());m.put("fundingRoute",p.getFundingRoute());m.put("financeRequestId",p.getFinanceRequestId());m.put("financeReference",p.getFinanceReference());m.put("decisionBy",p.getReviewedBy());m.put("decisionAt",p.getReviewedAt());
   String financeStatus=null;if(p.getFinanceRequestId()!=null){if("PETTY_CASH".equals(p.getFundingRoute()))financeStatus=pettyRequests.findById(p.getFinanceRequestId()).map(PettyCashRequest::getStatus).orElse("MISSING");else if("DIRECT_PURCHASE".equals(p.getFundingRoute()))financeStatus=directRequests.findById(p.getFinanceRequestId()).map(v->v.getStatus().name()).orElse("MISSING");}m.put("financeStatus",financeStatus);return m;
  }).toList();
 }

 @Transactional(readOnly=true) public List<Map<String,Object>> accountsQueue(){
  if(!canFund())throw new AccessDeniedException("Accounts permission is required");
  return proposals.findByStatusOrderBySubmittedAtAsc("AWAITING_ACCOUNTS").stream().map(p->{
   InventoryRequest r=requests.findById(p.getInventoryRequestId()).orElseThrow();Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getId());m.put("requestNumber",r.getRequestNumber());m.put("requesterName",r.getRequesterName()!=null?r.getRequesterName():r.getRequestedBy());m.put("status","AWAITING_ACCOUNTS");return m;
  }).distinct().toList();
 }

 @Transactional public Map<String,Object> submitProposal(Long id,ProposalInput input){
  if(!canPropose())throw new AccessDeniedException("Procurement permission is required to submit prices");
  InventoryRequest r=lock(id);if(r.getStatus()!=RequestStatus.PROCUREMENT_PENDING)approvedRequest(r);lockProducts(r);origin(r);
  List<InventoryPurchaseLine> existing=lines.findByInventoryRequestIdOrderById(id);Set<Long> seen=new HashSet<>();BigDecimal total=BigDecimal.ZERO;
  if(r.getItems().stream().map(RequestItem::getProductId).filter(Objects::nonNull).distinct().count()!=r.getItems().stream().filter(i->i.getProductId()!=null).count())throw new IllegalArgumentException("Consolidate duplicate product lines before proposing a purchase");
  InventoryProcurementProposal p=new InventoryProcurementProposal();p.setInventoryRequestId(id);p.setReference("IPP-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(Locale.ROOT));p.setSupplier(input.supplier().trim());p.setQuotationReference(input.quotationReference().trim());p.setNotes(input.notes());
  for(FundingItem f:input.items()){
   RequestItem i=item(r,f.itemId());if(!seen.add(f.itemId()))throw new IllegalArgumentException("Duplicate proposal item");
   if(f.quantity()<=0||f.quantity()>shortfall(i,existing))throw new IllegalArgumentException("Proposal exceeds the uncommitted shortage for "+i.getProductName());
   if(f.unitCost()==null||f.unitCost().signum()<0||f.unitCost().scale()>2)throw new IllegalArgumentException("Unit cost must have at most two decimal places and cannot be negative");
   InventoryProposalItem pi=new InventoryProposalItem();pi.setProposal(p);pi.setRequestItemId(i.getId());pi.setProductName(i.getProductName());pi.setQuantity(f.quantity());pi.setUnitCost(f.unitCost());p.getItems().add(pi);total=total.add(f.unitCost().multiply(BigDecimal.valueOf(f.quantity())));
  }
  if(total.signum()<=0)throw new IllegalArgumentException("Proposal total must be greater than zero");p.setTotalAmount(total);User actor=userService.getCurrentUser();p.setSubmittedByUserId(actor.getId());p.setSubmittedBy(actor.getName());proposals.saveAndFlush(p);
  r.setFinanceFulfilment(true);if(r.getStatus()==RequestStatus.PROCUREMENT_PENDING){for(RequestItem i:r.getItems())if(i.getApprovedQuantity()==null||i.getApprovedQuantity()==0)i.setApprovedQuantity(i.getRequestedQuantity());r.setStatus(RequestStatus.APPROVED_BY_PROCUREMENT);r.setProcurementApprovedBy(actor.getName());r.setProcurementApprovalDate(LocalDateTime.now());}requests.saveAndFlush(r);return summary(id);
 }

 @Transactional public Map<String,Object> decideProposal(Long id,Long proposalId,Decision input){
  if(!canFund())throw new AccessDeniedException("Accounts chooses the funding route");InventoryRequest r=lock(id);
  InventoryProcurementProposal p=proposals.findById(proposalId).filter(v->Objects.equals(v.getInventoryRequestId(),id)).orElseThrow(()->new IllegalArgumentException("Proposal does not belong to this request"));
  if(!"AWAITING_ACCOUNTS".equals(p.getStatus()))throw new IllegalArgumentException("This proposal has already been reviewed");
  if(!Set.of("FUND","RETURN").contains(input.action()))throw new IllegalArgumentException("Choose FUND or RETURN");
  User actor=userService.getCurrentUser();p.setReviewedByUserId(actor.getId());p.setReviewedBy(actor.getName());p.setReviewedAt(LocalDateTime.now());p.setReviewNotes(input.notes());
  if("RETURN".equals(input.action())){
   if(input.notes()==null||input.notes().isBlank())throw new IllegalArgumentException("Explain why the proposal is being returned");p.setStatus("RETURNED");proposals.saveAndFlush(p);return summary(id);
  }
  approvedRequest(r);
  // Release this proposal's reservation within the same locked transaction, then recheck live stock and other commitments.
  p.setStatus("FUNDING");proposals.saveAndFlush(p);
  Funding funding=new Funding(input.route(),input.accountantUserId(),"Supplier: "+p.getSupplier()+"; Quote: "+p.getQuotationReference()+"; Proposal: "+p.getReference(),p.getItems().stream().map(i->new FundingItem(i.getRequestItemId(),i.getQuantity(),i.getUnitCost())).toList());
  fund(id,funding);
  InventoryPurchaseLine last=lines.findByInventoryRequestIdOrderById(id).stream().max(Comparator.comparing(InventoryPurchaseLine::getId)).orElseThrow();p.setFundingRoute(input.route());p.setFinanceRequestId(last.getFinanceRequestId());p.setFinanceReference(last.getFinanceReference());p.setStatus("FUNDED");proposals.saveAndFlush(p);return summary(id);
 }

 @Transactional public Map<String,Object> receive(Long id,Long lineId,Receipt input){
  if(!canStore())throw new AccessDeniedException("Store permission is required");InventoryRequest r=lock(id);approvedRequest(r);lockProducts(r);
  InventoryPurchaseLine l=lines.findById(lineId).filter(v->Objects.equals(v.getInventoryRequestId(),id)).orElseThrow(()->new IllegalArgumentException("Purchase line does not belong to this request"));
  String status=status(l);if(!("PETTY_CASH".equals(l.getRoute())?"VOUCHER_COMPLETED".equals(status):"PAID".equals(status)))throw new IllegalArgumentException("Complete the purchase payment and receipt evidence in Finance first");
  if(input.quantity()<=0||input.quantity()>purchased(l)-l.getReceivedQuantity())throw new IllegalArgumentException("Receipt exceeds unreceived purchased quantity");
  if(input.evidenceReference()==null||input.evidenceReference().isBlank())throw new IllegalArgumentException("Delivery or receipt evidence is required");
  if(l.getProductId()!=null){Product p=products.findForUpdate(l.getProductId()).orElseThrow();p.setStock(Math.addExact(p.getStock(),input.quantity()));products.save(p);}
  l.setReceivedQuantity(l.getReceivedQuantity()+input.quantity());lines.saveAndFlush(l);InventoryReceipt receipt=new InventoryReceipt();receipt.setPurchaseLineId(lineId);receipt.setQuantity(input.quantity());receipt.setEvidenceReference(input.evidenceReference().trim());receipt.setReceivedBy(userService.getCurrentUser().getName());receipts.save(receipt);
  r.setReceivedQuantity(n(r.getReceivedQuantity())+input.quantity());r.setReceivedDate(LocalDateTime.now());requests.saveAndFlush(r);return summary(id);
 }

 @Transactional public Map<String,Object> issue(Long id,Issue input){
  if(!canStore())throw new AccessDeniedException("Store permission is required");InventoryRequest r=lock(id);approvedRequest(r);lockProducts(r);RequestItem i=item(r,input.itemId());
  if(input.quantity()<=0||input.quantity()>approved(i)-n(i.getIssuedQuantity()))throw new IllegalArgumentException("Issue exceeds outstanding approved quantity");
  List<InventoryPurchaseLine> all=lines.findByInventoryRequestIdOrderById(id);List<InventoryPurchaseLine> own=itemLines(all,i);int held=own.stream().mapToInt(l->l.getReceivedQuantity()-l.getIssuedQuantity()).sum();Product p=i.getProductId()==null?null:products.findForUpdate(i.getProductId()).orElseThrow();
  int available=p==null?held:Math.max(0,p.getStock()-lines.reservedStock(p.getId())+held);if(input.quantity()>available)throw new IllegalArgumentException("Not enough received or available stock for this request");
  String actor=userService.getCurrentUser().getName();int consume=input.quantity();for(var l:own){int take=Math.min(consume,l.getReceivedQuantity()-l.getIssuedQuantity());l.setIssuedQuantity(l.getIssuedQuantity()+take);lines.save(l);consume-=take;}
  if(p!=null){p.setStock(p.getStock()-input.quantity());products.save(p);outgoing.save(inventory.createOutgoingRecord(r,i,p,input.quantity(),actor));}
  i.setIssuedQuantity(n(i.getIssuedQuantity())+input.quantity());i.setStatus(i.getIssuedQuantity()==approved(i)?"ISSUED":"PARTIALLY_ISSUED");r.setFinanceFulfilment(true);r.setIssuedQuantity(r.getItems().stream().mapToInt(v->n(v.getIssuedQuantity())).sum());r.setIssueDate(LocalDateTime.now());r.setIssuedBy(actor);r.setStatus(r.getItems().stream().allMatch(v->n(v.getIssuedQuantity())>=approved(v))?RequestStatus.ISSUED:RequestStatus.PARTIALLY_ISSUED);requests.saveAndFlush(r);return summary(id);
 }
}
