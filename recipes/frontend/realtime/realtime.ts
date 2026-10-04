import { useEffect } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import { getStoredToken } from './client';

export interface WardEvent {
  type: 'VOTER_UPDATED' | 'IMPORT_COMPLETED' | 'ACCESS_CHANGED' | 'ACCESS_REQUESTS_CHANGED';
  payload: unknown;
}

function brokerUrl(): string {
  const origin = new URL(import.meta.env.VITE_API_BASE_URL).origin;
  return origin.replace(/^http/, 'ws') + '/ws';
}

let client: Client | null = null;
// Every active topic's (re)subscribe callback. AppShell's access-change socket is mounted
// on every authenticated page alongside whatever page-specific socket (ward/dashboard) is
// also active, so more than one concurrent subscriber is the normal case — each connect
// (including a reconnect after a dropped connection) must re-run all of them, not just
// whichever hook last overwrote a single onConnect handler.
let pendingSubscribers: Array<() => void> = [];

function getClient(): Client {
  if (client) return client;
  client = new Client({
    brokerURL: brokerUrl(),
    connectHeaders: { Authorization: `Bearer ${getStoredToken() ?? ''}` },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });
  client.onConnect = () => pendingSubscribers.forEach((subscribe) => subscribe());
  client.activate();
  return client;
}

function useTopic(destination: string | null, onEvent: (event: WardEvent) => void) {
  useEffect(() => {
    if (!destination) return;
    const c = getClient();
    // connectHeaders is read once at connect — refresh it in case the token changed
    // (e.g. a login after the singleton client was first created) before this
    // subscription's connect/reconnect happens.
    c.connectHeaders = { Authorization: `Bearer ${getStoredToken() ?? ''}` };

    let subscription: { unsubscribe: () => void } | null = null;
    const subscribe = () => {
      subscription = c.subscribe(destination, (message: IMessage) => {
        onEvent(JSON.parse(message.body) as WardEvent);
      });
    };

    pendingSubscribers.push(subscribe);
    if (c.connected) subscribe();

    return () => {
      subscription?.unsubscribe();
      pendingSubscribers = pendingSubscribers.filter((fn) => fn !== subscribe);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [destination]);
}

export function useWardSocket(wardId: string, onEvent: (event: WardEvent) => void) {
  useTopic(`/topic/wards/${wardId}`, onEvent);
}

export function useDashboardSocket(onEvent: (event: WardEvent) => void) {
  useTopic('/topic/dashboard', onEvent);
}

export function useMyAccessSocket(userId: string | null, onEvent: (event: WardEvent) => void) {
  useTopic(userId ? `/topic/users/${userId}/access` : null, onEvent);
}

export function useAccessRequestsSocket(onEvent: (event: WardEvent) => void) {
  useTopic('/topic/access-requests', onEvent);
}
