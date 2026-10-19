package com.example.eac_backend_project.controller;
import com.example.eac_backend_project.Service.InventoryFulfilmentService;
import com.example.eac_backend_project.Service.InventoryFulfilmentService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequiredArgsConstructor @RequestMapping("/api/inventory-requests/{requestId}/fulfilment")
public class InventoryFulfilmentController {
 private final InventoryFulfilmentService service;
 @GetMapping public Map<String,Object> summary(@PathVariable Long requestId,@RequestParam(defaultValue="operational") String view){if("accounts".equals(view))return service.accountsSummary(requestId);if(!"operational".equals(view))throw new IllegalArgumentException("Unknown review view");return service.summary(requestId);}
 @PostMapping("/proposals") public Map<String,Object> propose(@PathVariable Long requestId,@Valid @RequestBody ProposalInput input){return service.submitProposal(requestId,input);}
 @PostMapping("/proposals/{proposalId}/decision") public Map<String,Object> decide(@PathVariable Long requestId,@PathVariable Long proposalId,@Valid @RequestBody Decision input){return service.decideProposal(requestId,proposalId,input);}
 @PostMapping("/issue") public Map<String,Object> issue(@PathVariable Long requestId,@Valid @RequestBody Issue input){return service.issue(requestId,input);}
 @PostMapping("/receive/{lineId}") public Map<String,Object> receive(@PathVariable Long requestId,@PathVariable Long lineId,@Valid @RequestBody Receipt input){return service.receive(requestId,lineId,input);}
}
