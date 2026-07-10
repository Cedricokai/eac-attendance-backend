package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.ComparisonService;
import com.example.eac_backend_project.config.JwtProvider;
import com.example.eac_backend_project.dto.ComparisonSummaryDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/excel-comparator/comparisons")
@CrossOrigin(origins = "*")
public class ComparisonController {

    @Autowired
    private ComparisonService comparisonService;

    @Autowired
    private JwtProvider jwtProvider;

    @PostMapping("/compare")
    public ResponseEntity<ComparisonSummaryDTO> compareFiles(
            @RequestParam("fileA") MultipartFile fileA,
            @RequestParam("fileB") MultipartFile fileB,
            @RequestParam("sheetA") String sheetA,
            @RequestParam("sheetB") String sheetB,
            @RequestParam("ruleSetId") Long ruleSetId,
            HttpServletRequest request) {

        String username = extractUsernameFromToken(request);
        ComparisonSummaryDTO result = comparisonService.compareFiles(
                fileA, fileB, sheetA, sheetB, ruleSetId, username);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ComparisonSummaryDTO>> getComparisonHistory(HttpServletRequest request) {
        String username = extractUsernameFromToken(request);
        return ResponseEntity.ok(comparisonService.getComparisonHistory(username));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComparisonSummaryDTO> getComparison(@PathVariable Long id) {
        return ResponseEntity.ok(comparisonService.getComparisonById(id));
    }

    private String extractUsernameFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtProvider.validateToken(token)) {
                    return jwtProvider.extractUsername(token);
                }
            } catch (Exception e) {
                // Token validation failed, return anonymous
            }
        }
        return "anonymous";
    }
}