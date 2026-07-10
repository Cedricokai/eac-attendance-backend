package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.RuleSetDTO;
import com.example.eac_backend_project.entity.attendance.RuleSet;
import com.example.eac_backend_project.entity.attendance.RuleSetItem;
import com.example.eac_backend_project.repository.RuleSetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RuleSetService {

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public List<RuleSetDTO> getAllRuleSets() {
        List<RuleSet> ruleSets = ruleSetRepository.findByIsActiveTrue();
        return ruleSets.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public RuleSetDTO getRuleSetById(Long id) {
        RuleSet ruleSet = ruleSetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule set not found"));
        return convertToDTO(ruleSet);
    }

    @Transactional
    public RuleSetDTO createRuleSet(RuleSetDTO dto, String username) {
        RuleSet ruleSet = new RuleSet();
        ruleSet.setName(dto.getName());
        ruleSet.setDescription(dto.getDescription());
        ruleSet.setCreatedBy(username);
        ruleSet.setIsActive(true);

        List<RuleSetItem> items = createRuleSetItems(ruleSet, dto);
        ruleSet.setItems(items);

        RuleSet saved = ruleSetRepository.save(ruleSet);
        return convertToDTO(saved);
    }

    @Transactional
    public RuleSetDTO updateRuleSet(Long id, RuleSetDTO dto) {
        RuleSet ruleSet = ruleSetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule set not found"));

        ruleSet.setName(dto.getName());
        ruleSet.setDescription(dto.getDescription());

        // Clear existing items
        ruleSet.getItems().clear();

        // Add updated items
        List<RuleSetItem> items = createRuleSetItems(ruleSet, dto);
        ruleSet.getItems().addAll(items);

        RuleSet saved = ruleSetRepository.save(ruleSet);
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteRuleSet(Long id) {
        RuleSet ruleSet = ruleSetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule set not found"));
        ruleSet.setIsActive(false);
        ruleSetRepository.save(ruleSet);
    }

    private List<RuleSetItem> createRuleSetItems(RuleSet ruleSet, RuleSetDTO dto) {
        List<RuleSetItem> items = new ArrayList<>();
        for (Map.Entry<String, RuleSetDTO.RuleDTO> entry : dto.getRules().entrySet()) {
            RuleSetItem item = new RuleSetItem();
            item.setRuleSet(ruleSet);
            item.setCode(entry.getKey());
            // Convert Integer to Double
            item.setExpectedHours(entry.getValue().getExpectedHours().doubleValue());
            item.setDescription(entry.getValue().getDescription());
            items.add(item);
        }
        return items;
    }

    private RuleSetDTO convertToDTO(RuleSet ruleSet) {
        RuleSetDTO dto = new RuleSetDTO();
        dto.setId(ruleSet.getId());
        dto.setName(ruleSet.getName());
        dto.setDescription(ruleSet.getDescription());

        Map<String, RuleSetDTO.RuleDTO> rulesMap = new HashMap<>();
        for (RuleSetItem item : ruleSet.getItems()) {
            RuleSetDTO.RuleDTO ruleDTO = new RuleSetDTO.RuleDTO();
            // Convert Double to Integer
            ruleDTO.setExpectedHours(item.getExpectedHours().intValue());
            ruleDTO.setDescription(item.getDescription());
            rulesMap.put(item.getCode(), ruleDTO);
        }

        dto.setRules(rulesMap);
        return dto;
    }
}