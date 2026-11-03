package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.ItAssetDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.ItAsset;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.ItAssetRepository;
import com.example.eac_backend_project.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItAssetService {

    private final ItAssetRepository itAssetRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public ItAssetService(
            ItAssetRepository itAssetRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository
    ) {
        this.itAssetRepository = itAssetRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    public List<ItAssetDTO> getAllAssets() {
        return itAssetRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ItAssetDTO> getAssetsByEmployee(Long employeeId) {
        return itAssetRepository.findByEmployeeIdOrderByTakenAtDesc(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ItAssetDTO> getPublicAssets() {
        return itAssetRepository.findByIsPublicTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ItAssetDTO> getMyAssets(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return List.of();
        }
        String username = auth.getName();
        Employee employee = null;

        User user = userRepository.findByUserName(username).orElse(null);
        if (user != null) {
            employee = user.getEmployee();
            if (employee == null && user.getEmail() != null) {
                employee = employeeRepository.findByEmail(user.getEmail()).orElse(null);
            }
        }
        if (employee == null) {
            employee = employeeRepository.findByEmail(username).orElse(null);
        }
        if (employee == null) {
            employee = employeeRepository.findByEmployeeId(username).orElse(null);
        }

        if (employee == null) {
            return List.of();
        }

        return getAssetsByEmployee(employee.getId());
    }

    @Transactional
    public ItAssetDTO createAsset(ItAssetDTO dto, String currentUsername) {
        ItAsset asset = new ItAsset();
        applyDtoToEntity(asset, dto, currentUsername);
        ItAsset saved = itAssetRepository.save(asset);
        return convertToDTO(saved);
    }

    @Transactional
    public ItAssetDTO updateAsset(Long id, ItAssetDTO dto, String currentUsername) {
        ItAsset asset = itAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("IT Asset not found with id: " + id));
        applyDtoToEntity(asset, dto, currentUsername);
        ItAsset saved = itAssetRepository.save(asset);
        return convertToDTO(saved);
    }

    @Transactional
    public ItAssetDTO markReturned(Long id, LocalDateTime returnedAt, String conditionNotes) {
        ItAsset asset = itAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("IT Asset not found with id: " + id));
        asset.setReturned(true);
        asset.setReturnedAt(returnedAt != null ? returnedAt : LocalDateTime.now());
        if (conditionNotes != null && !conditionNotes.isBlank()) {
            asset.setConditionNotes(conditionNotes);
        }
        return convertToDTO(itAssetRepository.save(asset));
    }

    @Transactional
    public ItAssetDTO markUnreturned(Long id) {
        ItAsset asset = itAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("IT Asset not found with id: " + id));
        asset.setReturned(false);
        asset.setReturnedAt(null);
        return convertToDTO(itAssetRepository.save(asset));
    }

    @Transactional
    public ItAssetDTO togglePublic(Long id, Boolean isPublic) {
        ItAsset asset = itAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("IT Asset not found with id: " + id));
        boolean targetPublic = isPublic != null ? isPublic : !Boolean.TRUE.equals(asset.getIsPublic());
        asset.setIsPublic(targetPublic);
        if (targetPublic) {
            asset.setEmployee(null);
            asset.setReturned(false);
            asset.setReturnedAt(null);
        }
        return convertToDTO(itAssetRepository.save(asset));
    }

    @Transactional
    public void deleteAsset(Long id) {
        if (!itAssetRepository.existsById(id)) {
            throw new RuntimeException("IT Asset not found with id: " + id);
        }
        itAssetRepository.deleteById(id);
    }

    private void applyDtoToEntity(ItAsset asset, ItAssetDTO dto, String currentUsername) {
        if (dto.getAssetName() == null || dto.getAssetName().trim().isEmpty()) {
            throw new RuntimeException("Asset name is required");
        }
        asset.setAssetName(dto.getAssetName().trim());
        asset.setAssetTag(dto.getAssetTag() != null ? dto.getAssetTag().trim() : null);
        asset.setCategory(dto.getCategory() != null && !dto.getCategory().isBlank() ? dto.getCategory().trim().toUpperCase() : "LAPTOP");
        asset.setSpecifications(dto.getSpecifications() != null ? dto.getSpecifications().trim() : null);
        asset.setLocation(dto.getLocation() != null ? dto.getLocation().trim() : null);
        asset.setConditionNotes(dto.getConditionNotes() != null ? dto.getConditionNotes().trim() : null);

        boolean isPublic = Boolean.TRUE.equals(dto.getIsPublic());
        asset.setIsPublic(isPublic);

        if (isPublic) {
            asset.setEmployee(null);
            asset.setTakenAt(dto.getTakenAt());
            asset.setReturned(false);
            asset.setReturnedAt(null);
        } else {
            if (dto.getEmployeeId() != null) {
                Employee employee = employeeRepository.findById(dto.getEmployeeId())
                        .orElseThrow(() -> new RuntimeException("Employee not found with id: " + dto.getEmployeeId()));
                asset.setEmployee(employee);
            } else {
                asset.setEmployee(null);
            }

            asset.setTakenAt(dto.getTakenAt() != null ? dto.getTakenAt() : (asset.getTakenAt() != null ? asset.getTakenAt() : LocalDateTime.now()));

            boolean isReturned = Boolean.TRUE.equals(dto.getReturned());
            asset.setReturned(isReturned);
            if (isReturned) {
                asset.setReturnedAt(dto.getReturnedAt() != null ? dto.getReturnedAt() : (asset.getReturnedAt() != null ? asset.getReturnedAt() : LocalDateTime.now()));
            } else {
                asset.setReturnedAt(null);
            }
        }

        if (dto.getIssuedBy() != null && !dto.getIssuedBy().isBlank()) {
            asset.setIssuedBy(dto.getIssuedBy().trim());
        } else if (asset.getIssuedBy() == null && currentUsername != null) {
            asset.setIssuedBy(currentUsername);
        }
    }

    public ItAssetDTO convertToDTO(ItAsset asset) {
        ItAssetDTO dto = new ItAssetDTO();
        dto.setId(asset.getId());
        dto.setAssetName(asset.getAssetName());
        dto.setAssetTag(asset.getAssetTag());
        dto.setCategory(asset.getCategory());
        dto.setSpecifications(asset.getSpecifications());
        dto.setIsPublic(Boolean.TRUE.equals(asset.getIsPublic()));
        dto.setLocation(asset.getLocation());

        Employee emp = asset.getEmployee();
        if (emp != null) {
            dto.setEmployeeId(emp.getId());
            dto.setEmployeeCode(emp.getEmployeeId());
            String first = emp.getFirstName() != null ? emp.getFirstName() : "";
            String last = emp.getLastName() != null ? emp.getLastName() : "";
            dto.setEmployeeName((first + " " + last).trim());
            dto.setEmployeeDepartment(
                    emp.getDepartment() != null && !emp.getDepartment().isBlank()
                            ? emp.getDepartment()
                            : (emp.getCategory() != null ? emp.getCategory().getName() : null)
            );
            dto.setEmployeePosition(emp.getJobPosition());
            dto.setEmployeePhone(emp.getPhone());
            dto.setEmployeeEmail(emp.getEmail());
        }

        dto.setTakenAt(asset.getTakenAt());
        dto.setReturned(Boolean.TRUE.equals(asset.getReturned()));
        dto.setReturnedAt(asset.getReturnedAt());

        if (Boolean.TRUE.equals(asset.getIsPublic())) {
            dto.setStatus("PUBLIC");
        } else if (Boolean.TRUE.equals(asset.getReturned())) {
            dto.setStatus("RETURNED");
        } else {
            dto.setStatus("UNRETURNED");
        }

        dto.setConditionNotes(asset.getConditionNotes());
        dto.setIssuedBy(asset.getIssuedBy());
        dto.setCreatedAt(asset.getCreatedAt());
        dto.setUpdatedAt(asset.getUpdatedAt());
        return dto;
    }
}

