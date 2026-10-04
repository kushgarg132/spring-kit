import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';

const STATUS_QUERY_KEY = ['push-subscription-status'];

// The backend hands back the VAPID public key as a base64url string (no padding); the
// PushManager.subscribe() browser API requires it as a raw Uint8Array instead.
function urlBase64ToUint8Array(base64Url: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64Url.length % 4)) % 4);
  const base64 = (base64Url + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = atob(base64);
  const bytes = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; i++) {
    bytes[i] = rawData.charCodeAt(i);
  }
  return bytes;
}

export function isPushSupported(): boolean {
  return 'serviceWorker' in navigator && 'PushManager' in window;
}

export function usePushSubscriptionStatus() {
  return useQuery({
    queryKey: STATUS_QUERY_KEY,
    queryFn: async () => {
      if (!isPushSupported()) return false;
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      return subscription !== null;
    },
  });
}

export function useEnablePush() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') {
        throw new Error('Notification permission was not granted');
      }
      const { data } = await apiClient.get<{ publicKey: string }>('/api/v1/push/vapid-public-key');
      const registration = await navigator.serviceWorker.ready;
      // Chrome returns a cached subscription from subscribe() when one already exists,
      // silently ignoring the applicationServerKey argument — so a stale subscription
      // (e.g. from before a VAPID key rotation) never actually gets renewed and every
      // send since 403s server-side with no client-visible error. Force a clean re-subscribe.
      const existing = await registration.pushManager.getSubscription();
      if (existing) {
        await existing.unsubscribe();
      }
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(data.publicKey),
      });
      const json = subscription.toJSON();
      await apiClient.post('/api/v1/push/subscriptions', { endpoint: json.endpoint, keys: json.keys });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: STATUS_QUERY_KEY }),
  });
}

export function useDisablePush() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      if (subscription) {
        const endpoint = subscription.endpoint;
        await subscription.unsubscribe();
        await apiClient.delete('/api/v1/push/subscriptions', { data: { endpoint } });
      }
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: STATUS_QUERY_KEY }),
  });
}
