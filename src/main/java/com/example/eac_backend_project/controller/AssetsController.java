package com.example.eac_backend_project.controller;


import com.example.eac_backend_project.Service.AssetsService;
import com.example.eac_backend_project.config.Assets;
import com.example.eac_backend_project.entity.inventory.MoveRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "http://localhost:3000/")
public class AssetsController {

    private final AssetsService assetsService;

    public AssetsController(AssetsService assetsService) {
        this.assetsService = assetsService;
    }

    // Get all assets
    @GetMapping
    public List<Assets> getAssets() {
        return assetsService.getAllAssets();
    }

    // Add a new asset
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Assets> addAsset(@RequestBody Assets asset, Principal principal) {
        String userEmail = principal.getName();
        Assets savedAsset = assetsService.addAssets(asset, userEmail);
        return new ResponseEntity<>(savedAsset, HttpStatus.CREATED);
    }

    // Update an existing asset (Admin only)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Assets> updateAsset(@PathVariable Long id, @RequestBody Assets updatedAsset, Principal principal) {
        try {
            String userEmail = principal.getName();
            Assets savedAsset = assetsService.updateAssets(id, updatedAsset, userEmail);
            return ResponseEntity.ok(savedAsset);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete an asset
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id, Principal principal) {
        String userEmail = principal.getName();
        assetsService.deleteAssets(id, userEmail);
        return ResponseEntity.noContent().build();
    }

    // Search for assets
    @GetMapping("/search")
    public ResponseEntity<List<Assets>> searchAssets(@RequestParam String query) {
        List<Assets> assets = assetsService.searchAssets(query);
        if (assets.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Return 204 No Content if no assets match
        }
        return new ResponseEntity<>(assets, HttpStatus.OK);
    }

}
