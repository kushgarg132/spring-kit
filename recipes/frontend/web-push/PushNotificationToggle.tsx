import { Bell, BellOff } from 'lucide-react';
import { isPushSupported, useDisablePush, useEnablePush, usePushSubscriptionStatus } from '../api/push';
import { Button } from './ui/Button';

export function PushNotificationToggle() {
  const { data: isSubscribed, isLoading } = usePushSubscriptionStatus();
  const enable = useEnablePush();
  const disable = useDisablePush();

  if (!isPushSupported() || isLoading) {
    return null;
  }

  const blocked = typeof Notification !== 'undefined' && Notification.permission === 'denied';

  if (isSubscribed) {
    return (
      <Button variant="secondary" onClick={() => disable.mutate()} disabled={disable.isPending}>
        <Bell className="h-4 w-4" /> Notifications on · सूचनाएं चालू
      </Button>
    );
  }

  return (
    <div className="flex flex-col items-end gap-1">
      <Button variant="secondary" onClick={() => enable.mutate()} disabled={blocked || enable.isPending}>
        <BellOff className="h-4 w-4" /> Enable notifications · सूचनाएं सक्षम करें
      </Button>
      {blocked && (
        <p className="text-xs text-ink-500">Blocked in browser settings · ब्राउज़र सेटिंग्स में अवरोधित</p>
      )}
      {enable.isError && (
        <p className="text-xs text-red-600">{(enable.error as Error).message}</p>
      )}
    </div>
  );
}
