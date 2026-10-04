package recipes.bootstrapadmin; // snapshot: adapt package + imports

import app.domain.admin.Admin;
import app.domain.admin.AdminRepository;
import app.domain.admin.Role;
import app.domain.admin.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * There's no admin-management API (out of scope for this build) and voters can
 * never self-register as admins, so the very first Super Admin has to come from
 * somewhere. On an empty admins table, this seeds one from ADMIN_BOOTSTRAP_*
 * env vars — set once, then rotate the password from the app and unset the vars.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUsername;
    private final String bootstrapEmail;
    private final String bootstrapPassword;
    private final String bootstrapFullName;

    public AdminBootstrapRunner(
            AdminRepository adminRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.bootstrap-admin.username}") String bootstrapUsername,
            @Value("${app.auth.bootstrap-admin.email}") String bootstrapEmail,
            @Value("${app.auth.bootstrap-admin.password}") String bootstrapPassword,
            @Value("${app.auth.bootstrap-admin.full-name}") String bootstrapFullName) {
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
        this.bootstrapFullName = bootstrapFullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.count() > 0) {
            return;
        }
        if (!StringUtils.hasText(bootstrapUsername) || !StringUtils.hasText(bootstrapEmail) || !StringUtils.hasText(bootstrapPassword)) {
            log.warn("No admin accounts exist and ADMIN_BOOTSTRAP_USERNAME/EMAIL/PASSWORD are not all set — "
                    + "nobody can sign in. Set them and restart to create the first Super Admin.");
            return;
        }

        Role superAdminRole = roleRepository.findByCode(Role.Code.SUPER_ADMIN.name())
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role missing — check V2__seed_roles.sql ran"));

        Admin admin = new Admin();
        admin.setUsername(bootstrapUsername);
        admin.setEmail(bootstrapEmail);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setFullName(bootstrapFullName);
        admin.setRole(superAdminRole);
        admin.setActive(true);
        adminRepository.save(admin);

        log.warn("Bootstrapped Super Admin '{}' from ADMIN_BOOTSTRAP_* env vars — "
                + "sign in and rotate the password, then unset those vars.", bootstrapUsername);
    }
}
