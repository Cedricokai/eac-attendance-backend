package com.example.eac_backend_project.controller;
import com.example.eac_backend_project.Service.InventoryFulfilmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/inventory-fulfilment")
public class InventoryFulfilmentListController {
 private final InventoryFulfilmentService service;
 @GetMapping("/accounts-funding-review") public List<Map<String,Object>> fundingReview(){return service.accountsFundingHistory();}
 @GetMapping("/accounts-queue") public List<Map<String,Object>> queue(){return service.accountsQueue();}
 @GetMapping("/requests") public List<Map<String,Object>> requests(){return service.requestList();}
}
