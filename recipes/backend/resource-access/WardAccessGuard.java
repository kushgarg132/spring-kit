package recipes.resourceaccess; // snapshot: adapt package + imports

import app.domain.access.PermissionLevel;
import app.domain.access.WardAccessGrantRepository;
import app.domain.admin.Admin;
import app.domain.admin.AdminRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("wardGuard")
public class WardAccessGuard {

    private final AdminRepository adminRepository;
    private final WardAccessGrantRepository grantRepository;

    public WardAccessGuard(AdminRepository adminRepository, WardAccessGrantRepository grantRepository) {
        this.adminRepository = adminRepository;
        this.grantRepository = grantRepository;
    }

    /** Always re-read from the DB — never trust the JWT for this. */
    public boolean isSuperAdmin() {
        return adminRepository.findById(CurrentPrincipal.id())
                .map(Admin::isSuperAdmin)
                .orElse(false);
    }

    public boolean canView(UUID wardId) {
        return isSuperAdmin() || hasLevel(wardId, PermissionLevel.VIEW);
    }

    public boolean canAct(UUID wardId) {
        return isSuperAdmin() || hasLevel(wardId, PermissionLevel.ACTION);
    }

    public boolean canGrant(UUID wardId) {
        return isSuperAdmin() || hasLevel(wardId, PermissionLevel.GRANT);
    }

    /** True if the caller can grant on at least one ward — super-admin, or holds GRANT anywhere. */
    public boolean canGrantAnywhere() {
        return isSuperAdmin()
                || grantRepository.findByUserIdAndRevokedAtIsNull(CurrentPrincipal.id()).stream()
                        .anyMatch(g -> g.getPermissionLevel() == PermissionLevel.GRANT);
    }

    private boolean hasLevel(UUID wardId, PermissionLevel minimum) {
        return grantRepository.findByUserIdAndWardIdAndRevokedAtIsNull(CurrentPrincipal.id(), wardId)
                .map(grant -> grant.getPermissionLevel().atLeast(minimum))
                .orElse(false);
    }
}
