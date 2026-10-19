package com.example.eac_backend_project.controller;
import com.example.eac_backend_project.Service.PettyCashService;
import com.example.eac_backend_project.Service.PageService;
import com.example.eac_backend_project.dto.PettyCashRequestDTO;
import com.example.eac_backend_project.dto.PettyCashVoucherDTO;
import com.example.eac_backend_project.entity.finance.PettyCashRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
@RestController @RequestMapping("/api/petty-cash")
public class PettyCashController{
 private static final String VIEW_SUMMARY="/pettyCash/features/view-summary", REVIEW_REQUESTS="/pettyCash/actions/review-requests", VIEW_VOUCHERS="/pettyCash/features/view-vouchers", COMPLETE_VOUCHERS="/pettyCash/actions/complete-vouchers", REIMBURSE_FLOAT="/pettyCash/actions/reimburse-float";
 private final PettyCashService service; private final PageService pageService; public PettyCashController(PettyCashService service,PageService pageService){this.service=service;this.pageService=pageService;}
 @PostMapping public ResponseEntity<PettyCashRequest> create(@Valid @RequestBody PettyCashRequestDTO dto){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));}
 @GetMapping public List<PettyCashRequest> list(Authentication auth){return service.list(has(auth,REVIEW_REQUESTS)||has(auth,VIEW_VOUCHERS));}
 @GetMapping("/vouchers") public List<PettyCashRequest> vouchers(Authentication auth){require(auth,VIEW_VOUCHERS);return service.vouchers();}
 @GetMapping("/summary") public Map<String,Object> summary(Authentication auth){if(!has(auth,VIEW_SUMMARY)&&!has(auth,REIMBURSE_FLOAT))throw new AccessDeniedException("Petty cash summary permission is required");return service.summary();}
 @GetMapping("/cycles") public List<Map<String,Object>> cycles(Authentication auth){require(auth,VIEW_SUMMARY);return service.cycles();}
 @PutMapping("/{id}/approve") public PettyCashRequest approve(@PathVariable Long id,@RequestBody Map<String,Object> body,Authentication auth){require(auth,REVIEW_REQUESTS);BigDecimal amount=body.get("approvedAmount")==null?null:new BigDecimal(String.valueOf(body.get("approvedAmount")));return service.approve(id,amount,Objects.toString(body.get("notes"),null));}
 @PutMapping("/{id}/reject") public PettyCashRequest reject(@PathVariable Long id,@RequestBody Map<String,Object> body,Authentication auth){require(auth,REVIEW_REQUESTS);return service.reject(id,Objects.toString(body.get("notes"),null));}
 @PostMapping(value="/{id}/receipt",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Map<String,Object> uploadReceipt(@PathVariable Long id,@RequestPart("file") MultipartFile file,Authentication auth) throws IOException{require(auth,COMPLETE_VOUCHERS);return service.uploadReceipt(id,file);}
 @GetMapping("/{id}/receipt") public ResponseEntity<Resource> receipt(@PathVariable Long id,Authentication auth){require(auth,VIEW_VOUCHERS);PettyCashRequest info=service.receiptInfo(id);String name=Optional.ofNullable(info.getReceiptFileName()).orElse("receipt").replaceAll("[\\r\\n\\\"]","");String type=Optional.ofNullable(info.getReceiptContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+name+"\"").body(service.receipt(id));}
 @PutMapping("/{id}/voucher") public PettyCashRequest completeVoucher(@PathVariable Long id,@Valid @RequestBody PettyCashVoucherDTO dto,Authentication auth){require(auth,COMPLETE_VOUCHERS);return service.completeVoucher(id,dto);}
 @PutMapping("/reimburse") public Map<String,Object> reimburse(@RequestBody Map<String,Object> body,Authentication auth){require(auth,REIMBURSE_FLOAT);BigDecimal amount=new BigDecimal(String.valueOf(body.get("amount")));return service.reimburse(amount,Objects.toString(body.get("reference"),null),Objects.toString(body.get("notes"),null));}
 @PutMapping("/settings/threshold") @PreAuthorize("hasAuthority('ROLE_ADMIN')") public Map<String,Object> threshold(@RequestBody Map<String,Object> body){BigDecimal amount=new BigDecimal(String.valueOf(body.get("threshold")));return Map.of("message","Petty cash threshold updated","threshold",service.setThreshold(amount).getPettyCashThreshold());}
 @PutMapping("/settings/purchase-limit") @PreAuthorize("hasAuthority('ROLE_ADMIN')") public Map<String,Object> purchaseLimit(@RequestBody Map<String,Object> body){return Map.of("purchaseLimit",service.setPurchaseLimit(new BigDecimal(String.valueOf(body.get("purchaseLimit")))).getPettyCashPurchaseLimit());}
 private boolean has(Authentication auth,String path){return auth!=null&&auth.isAuthenticated()&&pageService.getAccessiblePages(auth).stream().anyMatch(page->path.equalsIgnoreCase(page.getPath()));}
 private void require(Authentication auth,String path){if(!has(auth,path))throw new AccessDeniedException("You are not assigned permission: "+path);}
}
