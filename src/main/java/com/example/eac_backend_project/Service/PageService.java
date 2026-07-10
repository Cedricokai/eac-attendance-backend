package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.PageDTO;
import com.example.eac_backend_project.dto.UserDTO;
import com.example.eac_backend_project.entity.inventory.Page;
import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.PageRepository;
import com.example.eac_backend_project.repository.RoleRepository;
import com.example.eac_backend_project.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PageService {

    private final PageRepository pageRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public PageService(PageRepository pageRepository, RoleRepository roleRepository, UserRepository userRepository) {
        this.pageRepository = pageRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public List<PageDTO> getAccessiblePages(Authentication auth) {
        String username = auth.getName();
        List<String> userRoles = auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Get role-based accessible pages
        List<Page> roleBasedPages = pageRepository.findActiveAccessiblePages(userRoles);

        // Get user-specific accessible pages
        Optional<User> userOpt = userRepository.findByUserName(username);
        Set<Page> userBasedPages = userOpt.map(User::getAccessiblePages).orElse(Set.of());

        // Combine and deduplicate
        Set<Page> allAccessiblePages = roleBasedPages.stream()
                .collect(Collectors.toSet());
        allAccessiblePages.addAll(userBasedPages);

        // Filter active pages only
        return allAccessiblePages.stream()
                .filter(Page::getIsActive)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PageDTO> getAllPages() {
        return pageRepository.findAllWithRoles()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // NEW: Get users assigned to a page
    public List<UserDTO> getPageUsers(Long pageId) {
        Page page = pageRepository.findByIdWithUsers(pageId)
                .orElseThrow(() -> new RuntimeException("Page not found"));

        return page.getUsers().stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    // NEW: Assign user to page
    @Transactional
    public PageDTO assignUserToPage(Long pageId, Long userId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Page not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        page.getUsers().add(user);
        user.getAccessiblePages().add(page);

        Page saved = pageRepository.save(page);
        userRepository.save(user);

        return convertToDTO(saved);
    }

    // NEW: Remove user from page
    @Transactional
    public PageDTO removeUserFromPage(Long pageId, Long userId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Page not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        page.getUsers().remove(user);
        user.getAccessiblePages().remove(page);

        Page saved = pageRepository.save(page);
        userRepository.save(user);

        return convertToDTO(saved);
    }

    @Transactional
    public PageDTO createPage(PageDTO pageDTO) {
        Page page = new Page();
        page.setName(pageDTO.getName());
        page.setPath(pageDTO.getPath());
        page.setDescription(pageDTO.getDescription());
        page.setIsPublic(pageDTO.getIsPublic() != null ? pageDTO.getIsPublic() : false);
        page.setIconName(pageDTO.getIconName());
        page.setDisplayOrder(pageDTO.getDisplayOrder() != null ? pageDTO.getDisplayOrder() : 0);
        page.setModule(pageDTO.getModule() != null ? Page.Module.valueOf(pageDTO.getModule()) : Page.Module.GENERAL);
        page.setIsActive(pageDTO.getIsActive() != null ? pageDTO.getIsActive() : true);
        page.setRequiresAuth(pageDTO.getRequiresAuth() != null ? pageDTO.getRequiresAuth() : true);

        if (pageDTO.getRoleNames() != null) {
            Set<Role> roles = pageDTO.getRoleNames().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                    .collect(Collectors.toSet());
            page.setRoles(roles);
        }

        Page saved = pageRepository.save(page);
        return convertToDTO(saved);
    }

    @Transactional
    public PageDTO updatePage(Long id, PageDTO pageDTO) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Page not found"));

        page.setName(pageDTO.getName());
        page.setPath(pageDTO.getPath());
        page.setDescription(pageDTO.getDescription());
        page.setIsPublic(pageDTO.getIsPublic());
        page.setIconName(pageDTO.getIconName());
        page.setDisplayOrder(pageDTO.getDisplayOrder());
        page.setModule(pageDTO.getModule() != null ? Page.Module.valueOf(pageDTO.getModule()) : Page.Module.GENERAL);
        page.setIsActive(pageDTO.getIsActive());
        page.setRequiresAuth(pageDTO.getRequiresAuth());

        // Update roles
        Set<Role> roles = pageDTO.getRoleNames().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());
        page.setRoles(roles);

        Page updated = pageRepository.save(page);
        return convertToDTO(updated);
    }

    @Transactional
    public void deletePage(Long id) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Page not found"));

        // Remove page from all users' accessible pages
        for (User user : page.getUsers()) {
            user.getAccessiblePages().remove(page);
            userRepository.save(user);
        }

        pageRepository.delete(page);
    }

    @Transactional
    public PageDTO assignRoleToPage(Long pageId, String roleName) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Page not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        page.getRoles().add(role);
        Page saved = pageRepository.save(page);
        return convertToDTO(saved);
    }

    @Transactional
    public PageDTO removeRoleFromPage(Long pageId, String roleName) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Page not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        page.getRoles().remove(role);
        Page saved = pageRepository.save(page);
        return convertToDTO(saved);
    }

    @Transactional
    public PageDTO setPagePublic(Long pageId, Boolean isPublic) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Page not found"));

        page.setIsPublic(isPublic);
        Page saved = pageRepository.save(page);
        return convertToDTO(saved);
    }

    @Transactional
    public PageDTO setPageActive(Long pageId, Boolean isActive) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Page not found"));

        page.setIsActive(isActive);
        Page saved = pageRepository.save(page);
        return convertToDTO(saved);
    }

    public PageDTO convertToDTO(Page page) {
        Set<String> roleNames = page.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

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

    private UserDTO convertToUserDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getUserName(),
                user.getEmail(),
                user.getMobile(),
                user.getRole() != null ? user.getRole().getName() : null,
                user.getEnabled()
        );
    }
}