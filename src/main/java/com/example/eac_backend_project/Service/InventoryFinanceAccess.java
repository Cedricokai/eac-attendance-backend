package com.example.eac_backend_project.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.Set;
/** Inventory finance documents belong to Accounts; operational users track fulfilment separately. */
final class InventoryFinanceAccess {
 private InventoryFinanceAccess() {}
 static boolean canRead(){var auth=SecurityContextHolder.getContext().getAuthentication();return auth!=null&&auth.isAuthenticated()&&auth.getAuthorities().stream().anyMatch(a->Set.of("ADMIN","ACCOUNTANT","ACCOUNTS","FINANCE","REVIEWER").contains(a.getAuthority().replaceFirst("^ROLE_","")));}
 static boolean visible(Long inventoryId){return inventoryId==null||canRead();}
 static void require(Long inventoryId){if(!visible(inventoryId))throw new AccessDeniedException("Linked inventory finance decisions are restricted to Accounts");}
}
