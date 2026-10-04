package recipes.webpush; // snapshot: adapt package + imports

import app.domain.push.PushSubscription;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.security.Security;

@Component
public class WebPushSender implements PushSender {

    private final PushService pushService;

    public WebPushSender(@Value("${app.push.vapid-public-key}") String publicKey,
                          @Value("${app.push.vapid-private-key}") String privateKey,
                          @Value("${app.push.vapid-subject}") String subject) throws GeneralSecurityException {
        // web-push's key loading (Utils.loadPublicKey/loadPrivateKey) requires the "BC" provider
        // to be registered — verified directly (see Task 3 commit message / spec research).
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        this.pushService = new PushService(publicKey, privateKey, subject);
    }

    @Override
    public int send(PushSubscription subscription, String payload) throws Exception {
        Subscription sub = new Subscription(subscription.getEndpoint(),
                new Subscription.Keys(subscription.getP256dh(), subscription.getAuth()));
        Notification notification = new Notification(sub, payload);
        // PushService.send(notification) with no encoding arg defaults to the deprecated AESGCM
        // encoding, which packs a legacy "dh=...;p256ecdsa=..." value into the Crypto-Key header —
        // FCM now rejects that as an invalid format (403). AES128GCM is the modern, correct encoding.
        HttpResponse response = pushService.send(notification, Encoding.AES128GCM);
        return response.getStatusLine().getStatusCode();
    }
}
