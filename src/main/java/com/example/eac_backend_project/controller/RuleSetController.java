package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.RuleSetService;
import com.example.eac_backend_project.config.JwtProvider;
import com.example.eac_backend_project.dto.RuleSetDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/excel-comparator/rule-sets")
@CrossOrigin(origins = "*")
public class RuleSetController {

    @Autowired
    private RuleSetService ruleSetService;

    @Autowired
    private JwtProvider jwtProvider;

    @GetMapping
    public ResponseEntity<List<RuleSetDTO>> getAllRuleSets() {
        return ResponseEntity.ok(ruleSetService.getAllRuleSets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RuleSetDTO> getRuleSet(@PathVariable Long id) {
        return ResponseEntity.ok(ruleSetService.getRuleSetById(id));
    }

    @PostMapping
    public ResponseEntity<RuleSetDTO> createRuleSet(@RequestBody RuleSetDTO dto, HttpServletRequest request) {
        String username = extractUsernameFromToken(request);
        return ResponseEntity.ok(ruleSetService.createRuleSet(dto, username));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RuleSetDTO> updateRuleSet(@PathVariable Long id, @RequestBody RuleSetDTO dto) {
        return ResponseEntity.ok(ruleSetService.updateRuleSet(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRuleSet(@PathVariable Long id) {
        ruleSetService.deleteRuleSet(id);
        return ResponseEntity.ok().build();
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