package recipes.webpush; // snapshot: adapt package + imports

import app.domain.push.PushSubscription;

/**
 * Thin seam over the real network call so PushNotificationServiceTest (and
 * AccessControllerTest) can mock delivery without hitting an actual push service.
 */
public interface PushSender {
    /** Returns the push service's HTTP status code (200/201 success, 404/410 = gone). */
    int send(PushSubscription subscription, String payload) throws Exception;
}
