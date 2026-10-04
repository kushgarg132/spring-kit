package recipes.webpush; // snapshot: adapt package + imports

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.domain.access.PermissionLevel;
import app.domain.access.WardAccessGrantRepository;
import app.domain.admin.Admin;
import app.domain.admin.AdminRepository;
import app.domain.push.PushSubscription;
import app.domain.push.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushSubscriptionRepository subscriptionRepository;
    private final AdminRepository adminRepository;
    private final WardAccessGrantRepository grantRepository;
    private final PushSender pushSender;
    private final ObjectMapper objectMapper;
    private final String vapidPublicKey;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository,
                                    AdminRepository adminRepository,
                                    WardAccessGrantRepository grantRepository,
                                    PushSender pushSender,
                                    ObjectMapper objectMapper,
                                    @Value("${app.push.vapid-public-key}") String vapidPublicKey) {
        this.subscriptionRepository = subscriptionRepository;
        this.adminRepository = adminRepository;
        this.grantRepository = grantRepository;
        this.pushSender = pushSender;
        this.objectMapper = objectMapper;
        this.vapidPublicKey = vapidPublicKey;
    }

    public String vapidPublicKey() {
        return vapidPublicKey;
    }

    public void subscribe(UUID userId, String endpoint, String p256dh, String auth) {
        PushSubscription sub = subscriptionRepository.findByEndpoint(endpoint).orElseGet(PushSubscription::new);
        sub.setUser(adminRepository.getReferenceById(userId));
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        subscriptionRepository.save(sub);
    }

    public void unsubscribe(String endpoint) {
        subscriptionRepository.deleteByEndpoint(endpoint);
    }

    /** Same approver definition AccessService.listPending() gates visibility on: super admins + anyone holding GRANT tier anywhere. */
    public void notifyNewAccessRequest(String requesterName) {
        sendToUsers(approverUserIds(), "New access request", requesterName + " requested ward access.");
    }

    public void notifyAccessResolved(UUID userId, boolean approved) {
        String title = approved ? "Access request approved" : "Access request denied";
        String body = approved ? "Your ward access request was approved." : "Your ward access request was denied.";
        sendToUsers(List.of(userId), title, body);
    }

    private List<UUID> approverUserIds() {
        Set<UUID> ids = new HashSet<>();
        for (Admin admin : adminRepository.findBySuperAdminTrue()) {
            ids.add(admin.getId());
        }
        grantRepository.findByRevokedAtIsNull().stream()
                .filter(g -> g.getPermissionLevel() == PermissionLevel.GRANT)
                .forEach(g -> ids.add(g.getUser().getId()));
        return List.copyOf(ids);
    }

    private void sendToUsers(List<UUID> userIds, String title, String body) {
        if (userIds.isEmpty()) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of("title", title, "body", body, "url", "/access"));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize push payload, skipping send", e);
            return;
        }
        for (PushSubscription sub : subscriptionRepository.findByUserIdIn(userIds)) {
            send(sub, payload);
        }
    }

    /** Never throws — a dead subscription or network error must not fail the caller's transaction. */
    private void send(PushSubscription sub, String payload) {
        try {
            int status = pushSender.send(sub, payload);
            if (status == 404 || status == 410 || status == 403) {
                // 403 here means a VAPID key mismatch (stale subscription bound to an old
                // applicationServerKey) — it will never succeed on retry, same as a dead endpoint.
                log.warn("Deleting subscription {} (endpoint {}) after status {}",
                        sub.getId(), sub.getEndpoint(), status);
                subscriptionRepository.deleteByEndpoint(sub.getEndpoint());
            } else if (status < 200 || status >= 300) {
                log.warn("Push notification to subscription {} got unexpected status {}", sub.getId(), status);
            }
        } catch (Exception e) {
            log.warn("Push notification failed for subscription {}", sub.getId(), e);
        }
    }
}
