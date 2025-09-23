package com.example.eac_backend_project.Controller;



import com.example.eac_backend_project.Service.OutgoingService;
import com.example.eac_backend_project.entity.inventory.Outgoing;
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
@CrossOrigin(origins = "http://localhost:5173/")
public class OutgoingController {

    private static final Logger logger = LoggerFactory.getLogger(OutgoingController.class);

    @Autowired
    private OutgoingService outgoingService;
    // Endpoint to get all outgoing movements
    @GetMapping
    public List<Outgoing> getAllOutgoingProducts() {
        return outgoingService.getAllOutGoingProducts();
    }

    @PostMapping
    public ResponseEntity<?> saveOutgoingItems(@RequestBody List<Outgoing> outgoingItems) {
        try {
            // Retrieve the current user's authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUser = authentication.getName();

            for (Outgoing outgoingItem : outgoingItems) {
                outgoingItem.setUserName(currentUser);

                // Fetch and set the actual asset ID

                outgoingItem.setMovementDate(LocalDateTime.now()); // Set the movement date

                outgoingService.saveOutgoing(outgoingItem);
            }
            return ResponseEntity.ok("Items moved to outgoing successfully");
        } catch (Exception e) {
            logger.error("Error saving outgoing items: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to move items to outgoing: " + e.getMessage());
        }
    }

}
