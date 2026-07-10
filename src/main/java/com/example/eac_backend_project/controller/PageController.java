package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.PageService;
import com.example.eac_backend_project.dto.PageDTO;
import com.example.eac_backend_project.dto.UserDTO;
import com.example.eac_backend_project.entity.inventory.Page;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.PageRepository;
import com.example.eac_backend_project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pages")
public class PageController {

    @Autowired
    private PageService pageService;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/my-pages")
    public ResponseEntity<List<PageDTO>> getMyPages(Authentication auth) {
        return ResponseEntity.ok(pageService.getAccessiblePages(auth));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<PageDTO>> getAllPages() {
        return ResponseEntity.ok(pageService.getAllPages());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PageDTO> createPage(@RequestBody PageDTO pageDTO) {
        return ResponseEntity.ok(pageService.createPage(pageDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PageDTO> updatePage(@PathVariable Long id, @RequestBody PageDTO pageDTO) {
        return ResponseEntity.ok(pageService.updatePage(id, pageDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deletePage(@PathVariable Long id) {
        pageService.deletePage(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{pageId}/assign-role/{roleName}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PageDTO> assignRoleToPage(@PathVariable Long pageId, @PathVariable String roleName) {
        return ResponseEntity.ok(pageService.assignRoleToPage(pageId, roleName));
    }

    @PutMapping("/{pageId}/remove-role/{roleName}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PageDTO> removeRoleFromPage(@PathVariable Long pageId, @PathVariable String roleName) {
        return ResponseEntity.ok(pageService.removeRoleFromPage(pageId, roleName));
    }

    @PutMapping("/{pageId}/set-public")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PageDTO> setPagePublic(@PathVariable Long pageId, @RequestBody Boolean isPublic) {
        return ResponseEntity.ok(pageService.setPagePublic(pageId, isPublic));
    }

    // NEW: User assignment endpoints
    @GetMapping("/{pageId}/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserDTO>> getPageUsers(@PathVariable Long pageId) {
        return ResponseEntity.ok(pageService.getPageUsers(pageId));
    }

    @PutMapping("/{pageId}/assign-user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PageDTO> assignUserToPage(@PathVariable Long pageId, @PathVariable Long userId) {
        return ResponseEntity.ok(pageService.assignUserToPage(pageId, userId));
    }

    @PutMapping("/{pageId}/remove-user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PageDTO> removeUserFromPage(@PathVariable Long pageId, @PathVariable Long userId) {
        return ResponseEntity.ok(pageService.removeUserFromPage(pageId, userId));
    }

    @PutMapping("/{pageId}/set-active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PageDTO> setPageActive(@PathVariable Long pageId, @RequestBody Boolean isActive) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Page not found"));

        page.setIsActive(isActive);
        Page saved = pageRepository.save(page);
        return ResponseEntity.ok(convertToDTO(saved));
    }

    @GetMapping("/modules")
    public ResponseEntity<Map<String, String>> getModules() {
        Map<String, String> modules = new HashMap<>();
        for (Page.Module module : Page.Module.values()) {
            modules.put(module.name(), module.getDisplayName());
        }
        return ResponseEntity.ok(modules);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPageStats() {
        long totalPages = pageRepository.count();
        long publicPages = pageRepository.countByIsPublic(true);
        long activePages = pageRepository.countByIsActive(true);
        long pagesWithRoles = pageRepository.countPagesWithRoles();
        long pagesWithUsers = pageRepository.countPagesWithUsers();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPages", totalPages);
        stats.put("publicPages", publicPages);
        stats.put("privatePages", totalPages - publicPages);
        stats.put("activePages", activePages);
        stats.put("inactivePages", totalPages - activePages);
        stats.put("pagesWithRoles", pagesWithRoles);
        stats.put("pagesWithUsers", pagesWithUsers);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/check-access")
    public ResponseEntity<Map<String, Object>> checkAccess(
            @RequestParam String path,
            Authentication auth) {

        Map<String, Object> result = new HashMap<>();
        result.put("allowed", false); // default

        if (auth == null) {
            result.put("message", "Not authenticated");
            return ResponseEntity.status(401).body(result);
        }

        String username = auth.getName();
        List<String> userRoles = auth.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .toList();

        Optional<Page> pageOpt = pageRepository.findByPath(path);
        if (pageOpt.isEmpty()) {
            result.put("allowed", true); // unknown pages allowed
            return ResponseEntity.ok(result);
        }

        Page page = pageOpt.get();

        if (Boolean.TRUE.equals(page.getIsPublic())) {
            result.put("allowed", true);
            return ResponseEntity.ok(result);
        }

        boolean hasRoleAccess = page.getRoles()
                .stream()
                .anyMatch(role -> userRoles.contains(role.getName()));

        if (hasRoleAccess) {
            result.put("allowed", true);
            return ResponseEntity.ok(result);
        }

        Optional<User> userOpt = userRepository.findByUserName(username);
        if (userOpt.isPresent()) {
            boolean hasUserAccess = userOpt.get().getAccessiblePages()
                    .stream()
                    .anyMatch(p -> p.getId().equals(page.getId()));

            if (hasUserAccess) {
                result.put("allowed", true);
                return ResponseEntity.ok(result);
            }
        }

        result.put("message", "Access denied");
        return ResponseEntity.status(403).body(result); // still JSON
    }



    // Helper method to convert Page to PageDTO
    private PageDTO convertToDTO(Page page) {
        java.util.Set<String> roleNames = page.getRoles().stream()
                .map(role -> role.getName())
                .collect(java.util.stream.Collectors.toSet());

        return new PageDTO(
                page.getId(),
                page.getName(),
                page.getPath(),
                page.getDescription(),
                page.getIsPublic(),
                page.getIconName(),
                page.getDisplayOrder(),
                page.getModule() != null ? page.getModule().name() : "GENERAL",
                page.getIsActive(),
                page.getRequiresAuth(),
                roleNames
        );
    }
}