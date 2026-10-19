package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.inventory.Page;
import com.example.eac_backend_project.repository.PageRepository;
import com.example.eac_backend_project.repository.RoleRepository;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IssuedItemsPageRegistration {
    private final PageRepository pages;
    private final RoleRepository roles;
    public IssuedItemsPageRegistration(PageRepository pages, RoleRepository roles) { this.pages = pages; this.roles = roles; }
    @EventListener(ApplicationReadyEvent.class) @Transactional
    public void register() {
        var page = pages.findByPath("/employee-issued-items").orElseGet(() -> pages.findByName("Employee Issued Items").orElseGet(Page::new));
        page.setName("Employee Issued Items"); page.setPath("/employee-issued-items"); page.setModule(Page.Module.HR);
        page.setDescription("Employee equipment issue records and PPE replacement dates"); page.setIconName("Package");
        // Retain existing user/role grants when connecting the registered sidebar entry to this page.
        roles.findByName("ROLE_ADMIN").ifPresent(role -> page.getRoles().add(role));
        pages.save(page);
    }
}
