package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.OutgoingService;
import com.example.eac_backend_project.entity.inventory.Outgoing;
import com.example.eac_backend_project.entity.inventory.OutgoingRecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/outgoing")
public class OutgoingController {
    private static final Logger logger = LoggerFactory.getLogger(OutgoingController.class);

    @Autowired
    private OutgoingService outgoingService;

    @GetMapping
    public List<Outgoing> getAllOutgoingProducts() {
        return outgoingService.getAllOutGoingProducts();
    }

    @GetMapping("/type/{type}")
    public List<Outgoing> getOutgoingByType(@PathVariable OutgoingRecordType type) {
        return outgoingService.getOutgoingByType(type);
    }

    @GetMapping("/request/{requestId}")
    public List<Outgoing> getOutgoingByRequestId(@PathVariable Long requestId) {
        return outgoingService.getOutgoingByRequestId(requestId);
    }

    @PostMapping
    public ResponseEntity<?> saveOutgoingRecords(@RequestBody List<Outgoing> outgoingItems) {
        try {
            logger.info("Received {} outgoing items", outgoingItems.size());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUser = authentication != null ? authentication.getName() : "system";

            for (Outgoing outgoingItem : outgoingItems) {
                logger.info("Processing outgoing: {} | Type: {} | Stock: {} | QuantityMoved: {} | ProductID: {}",
                        outgoingItem.getName(),
                        outgoingItem.getRecordType(),
                        outgoingItem.getStock(),
                        outgoingItem.getQuantityMoved(),
                        outgoingItem.getProductId());

                if (outgoingItem.getProductId() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Product ID must not be null for item: " + outgoingItem.getName());
                }

                Integer quantityToMove = outgoingItem.getQuantityMoved();
                if (quantityToMove == null || quantityToMove <= 0) {
                    quantityToMove = outgoingItem.getStock();
                }

                if (quantityToMove == null || quantityToMove <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Quantity moved must be greater than 0 for item: " + outgoingItem.getName());
                }

                outgoingItem.setQuantityMoved(quantityToMove);
                outgoingItem.setUserName(currentUser);

                // Set returnable fields if applicable
                if (outgoingItem.getReturnableAfterUse() == null) {
                    // You might want to fetch this from product table
                    outgoingItem.setReturnableAfterUse(false);
                }

                // Set expected return date for returnable items
                if (outgoingItem.getReturnableAfterUse() != null && outgoingItem.getReturnableAfterUse()) {
                    if (outgoingItem.getExpectedReturnDate() == null) {
                        outgoingItem.setExpectedReturnDate(LocalDateTime.now().plusDays(30));
                    }
                    outgoingItem.setReturnStatus("PENDING");
                }

                if (outgoingItem.getMovementDate() == null) {
                    outgoingItem.setMovementDate(LocalDateTime.now());
                }

                if (outgoingItem.getRecordType() == null) {
                    outgoingItem.setRecordType(OutgoingRecordType.TRANSFER);
                }

                if (outgoingItem.getStatus() == null) {
                    if (outgoingItem.getRecordType() == OutgoingRecordType.REQUEST) {
                        outgoingItem.setStatus("ISSUED");
                    } else {
                        outgoingItem.setStatus("TRANSFERRED");
                    }
                }
            }

            List<Outgoing> savedItems = outgoingService.saveAllOutgoing(outgoingItems);
            logger.info("Successfully saved {} outgoing records", savedItems.size());
            return ResponseEntity.ok(savedItems);

        } catch (Exception e) {
            logger.error("Error saving outgoing records: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save outgoing records: " + e.getMessage());
        }
    }

    @GetMapping("/store-type/{storeType}")
    public List<Outgoing> getOutgoingByStoreType(@PathVariable String storeType) {
        return outgoingService.getOutgoingByStoreType(storeType);
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<?> markAsReturned(@PathVariable Long id, @RequestBody ReturnRequest returnRequest) {
        try {
            Outgoing outgoing = outgoingService.findById(id);
            if (outgoing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Outgoing record not found");
            }

            outgoing.setReturnStatus("RETURNED");
            outgoing.setReturnedDate(LocalDateTime.now());
            outgoing.setReturnNotes(returnRequest.getNotes());

            Outgoing updated = outgoingService.saveOutgoing(outgoing);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update return status: " + e.getMessage());
        }
    }


    @PostMapping("/issue")
    public ResponseEntity<?> issueItems(@RequestBody List<Outgoing> outgoingItems) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUser = authentication != null ? authentication.getName() : "system";

            for (Outgoing outgoingItem : outgoingItems) {
                outgoingItem.setRecordType(OutgoingRecordType.ISSUED);
                outgoingItem.setUserName(currentUser);
                outgoingItem.setStatus("ISSUED");

                if (outgoingItem.getMovementDate() == null) {
                    outgoingItem.setMovementDate(LocalDateTime.now());
                }

                if (outgoingItem.getQuantityMoved() == null && outgoingItem.getStock() != null) {
                    outgoingItem.setQuantityMoved(outgoingItem.getStock());
                }
            }

            List<Outgoing> savedItems = outgoingService.saveAllOutgoing(outgoingItems);
            return ResponseEntity.ok(savedItems);

        } catch (Exception e) {
            logger.error("Error issuing items: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to issue items: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    public long getTotalOutgoingProducts() {
        return outgoingService.getTotalOutgoingProducts();
    }

    @GetMapping("/product/{productId}")
    public List<Outgoing> getOutgoingByProductId(@PathVariable Long productId) {
        return outgoingService.getOutgoingByProductId(productId);
    }

    @GetMapping("/user/{userName}")
    public List<Outgoing> getOutgoingByUser(@PathVariable String userName) {
        return outgoingService.getOutgoingByUserName(userName);
    }


}

class ReturnRequest {
    private String notes;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}