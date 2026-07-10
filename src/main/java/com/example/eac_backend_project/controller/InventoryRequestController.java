package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.InventoryRequestService;
import com.example.eac_backend_project.dto.ApprovalDTO;
import com.example.eac_backend_project.dto.InventoryRequestDTO;
import com.example.eac_backend_project.dto.ProcurementApprovalDTO;
import com.example.eac_backend_project.dto.StoreApprovalDTO;
import com.example.eac_backend_project.entity.inventory.InventoryRequest;
import com.example.eac_backend_project.entity.inventory.RequestStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/inventory-requests")
public class InventoryRequestController {

    private final InventoryRequestService requestService;

    public InventoryRequestController(InventoryRequestService requestService) {
        this.requestService = requestService;
    }

    // Create new request
    @PostMapping
    public ResponseEntity<InventoryRequest> createRequest(@RequestBody InventoryRequestDTO requestDTO, Principal principal) {
        String userEmail = principal != null ? principal.getName() : (requestDTO.getRequestedBy() != null ? requestDTO.getRequestedBy() : "anonymous");
        InventoryRequest request = requestService.createRequest(requestDTO, userEmail);
        return ResponseEntity.status(201).body(request);
    }

    // Planner approve/reject
    @PutMapping("/{id}/planner-approve")
    public ResponseEntity<InventoryRequest> approveByPlanner(@PathVariable Long id, @RequestBody ApprovalDTO approvalDTO, Principal principal) {
        String approvedBy = approvalDTO.getApprovedBy() != null ? approvalDTO.getApprovedBy() : (principal != null ? principal.getName() : "planner");
        InventoryRequest request = requestService.approveByPlanner(id, approvedBy, approvalDTO.getNotes());
        return ResponseEntity.ok(request);
    }

    @PutMapping("/{id}/planner-reject")
    public ResponseEntity<InventoryRequest> rejectByPlanner(@PathVariable Long id, @RequestBody ApprovalDTO approvalDTO, Principal principal) {
        String rejectedBy = approvalDTO.getApprovedBy() != null ? approvalDTO.getApprovedBy() : (principal != null ? principal.getName() : "planner");
        InventoryRequest request = requestService.rejectByPlanner(id, rejectedBy, approvalDTO.getNotes());
        return ResponseEntity.ok(request);
    }

    // Procurement approve/reject (PPE requests)
    @PutMapping("/{id}/procurement-approve")
    public ResponseEntity<InventoryRequest> approveByProcurement(@PathVariable Long id, @RequestBody ProcurementApprovalDTO approvalDTO, Principal principal) {
        String approvedBy = approvalDTO.getApprovedBy() != null ? approvalDTO.getApprovedBy() : (principal != null ? principal.getName() : "procurement");
        InventoryRequest request = requestService.approveByProcurement(id, approvedBy, approvalDTO.getNotes(), approvalDTO.getEstimatedCost(), approvalDTO.getDeliveryDate());
        return ResponseEntity.ok(request);
    }

    @PutMapping("/{id}/procurement-reject")
    public ResponseEntity<InventoryRequest> rejectByProcurement(@PathVariable Long id, @RequestBody ApprovalDTO approvalDTO, Principal principal) {
        String rejectedBy = approvalDTO.getApprovedBy() != null ? approvalDTO.getApprovedBy() : (principal != null ? principal.getName() : "procurement");
        InventoryRequest request = requestService.rejectByProcurement(id, rejectedBy, approvalDTO.getNotes());
        return ResponseEntity.ok(request);
    }

    // Store approve/reject
    @PutMapping("/{id}/store-approve")
    public ResponseEntity<InventoryRequest> approveByStore(@PathVariable Long id, @RequestBody StoreApprovalDTO approvalDTO, Principal principal) {
        String approvedBy = approvalDTO.getApprovedBy() != null ? approvalDTO.getApprovedBy() : (principal != null ? principal.getName() : "store");
        InventoryRequest request = requestService.approveByStore(id, approvedBy, approvalDTO.getNotes());
        return ResponseEntity.ok(request);
    }

    @PutMapping("/{id}/store-reject")
    public ResponseEntity<InventoryRequest> rejectByStore(@PathVariable Long id, @RequestBody ApprovalDTO approvalDTO, Principal principal) {
        String rejectedBy = approvalDTO.getApprovedBy() != null ? approvalDTO.getApprovedBy() : (principal != null ? principal.getName() : "store");
        InventoryRequest request = requestService.rejectByStore(id, rejectedBy, approvalDTO.getNotes());
        return ResponseEntity.ok(request);
    }

    // Confirm receipt (store receives items)
    @PutMapping("/{id}/receive")
    public ResponseEntity<InventoryRequest> confirmReceipt(@PathVariable Long id, @RequestBody StoreApprovalDTO approvalDTO, Principal principal) {
        String receivedBy = approvalDTO.getApprovedBy() != null ? approvalDTO.getApprovedBy() : (principal != null ? principal.getName() : "store");
        InventoryRequest request = requestService.confirmReceipt(id, approvalDTO.getQuantity(), receivedBy);
        return ResponseEntity.ok(request);
    }

    // Issue items from store
    @PutMapping("/{id}/issue")
    public ResponseEntity<InventoryRequest> issueItems(@PathVariable Long id, @RequestBody StoreApprovalDTO approvalDTO, Principal principal) {
        String issuedBy = approvalDTO.getApprovedBy() != null ? approvalDTO.getApprovedBy() : (principal != null ? principal.getName() : "store");
        InventoryRequest request = requestService.issueItems(id, approvalDTO.getQuantity(), issuedBy);
        return ResponseEntity.ok(request);
    }

    // Get all requests
    @GetMapping
    public ResponseEntity<List<InventoryRequest>> getAllRequests() {
        List<InventoryRequest> requests = requestService.getAllRequests();
        return ResponseEntity.ok(requests);
    }

    // Get requests by user
    @GetMapping("/by-user/{username}")
    public ResponseEntity<List<InventoryRequest>> getRequestsByUser(@PathVariable String username) {
        List<InventoryRequest> requests = requestService.getRequestsByUser(username);
        return ResponseEntity.ok(requests);
    }

    // Get requests for planner
    @GetMapping("/for-planner")
    public ResponseEntity<List<InventoryRequest>> getRequestsForPlanner() {
        List<InventoryRequest> requests = requestService.getRequestsForPlanner();
        return ResponseEntity.ok(requests);
    }

    // Get requests for procurement
    @GetMapping("/for-procurement")
    public ResponseEntity<List<InventoryRequest>> getRequestsForProcurement() {
        List<InventoryRequest> requests = requestService.getRequestsForProcurement();
        return ResponseEntity.ok(requests);
    }

    // Get requests for store
    @GetMapping("/for-store")
    public ResponseEntity<List<InventoryRequest>> getRequestsForStore() {
        List<InventoryRequest> requests = requestService.getRequestsForStore();
        return ResponseEntity.ok(requests);
    }

    // Get requests approved by procurement (procured)
    @GetMapping("/procured")
    public ResponseEntity<List<InventoryRequest>> getApprovedByProcurement() {
        List<InventoryRequest> requests = requestService.getApprovedByProcurement();
        return ResponseEntity.ok(requests);
    }

    // Get issued requests
    @GetMapping("/issued")
    public ResponseEntity<List<InventoryRequest>> getIssuedRequests() {
        List<InventoryRequest> requests = requestService.getIssuedRequests();
        System.out.println("DEBUG: Found " + requests.size() + " issued requests");
        requests.forEach(req -> System.out.println("  - Request " + req.getId() + ": " + req.getStatus()));
        return ResponseEntity.ok(requests);
    }

    // Get single request by ID
    @GetMapping("/{id}")
    public ResponseEntity<InventoryRequest> getRequestById(@PathVariable Long id) {
        InventoryRequest request = requestService.getRequestById(id).orElseThrow(() -> new RuntimeException("Request not found"));
        return ResponseEntity.ok(request);
    }

    // Send request to procurement preview (for planner)
    @PostMapping("/{id}/send-to-procurement")
    public ResponseEntity<InventoryRequest> sendToProcurement(@PathVariable Long id, Principal principal) {
        InventoryRequest request = requestService.sendToProcurement(id, principal.getName());
        return ResponseEntity.ok(request);
    }

    // Send request to store officer (for planner)
    @PostMapping("/{id}/send-to-store")
    public ResponseEntity<InventoryRequest> sendToStore(@PathVariable Long id, Principal principal) {
        InventoryRequest request = requestService.sendToStore(id, principal.getName());
        return ResponseEntity.ok(request);
    }
}
