"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { apiFetch } from "@/lib/apiClient";
import type { AppNotification } from "@/lib/types";
import { formatRelativeTime } from "@/lib/format";

const POLL_INTERVAL_MS = 60_000;

export function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState<AppNotification[] | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const refreshUnreadCount = useCallback(() => {
    apiFetch<{ count: number }>("/notifications/unread-count")
      .then((res) => setUnreadCount(res.count))
      .catch(() => {
        // best-effort: leave the last known count in place
      });
  }, []);

  useEffect(() => {
    refreshUnreadCount();
    const interval = setInterval(refreshUnreadCount, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [refreshUnreadCount]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  function toggleOpen() {
    const next = !open;
    setOpen(next);
    if (next) {
      apiFetch<AppNotification[]>("/notifications").then(setNotifications).catch(() => setNotifications([]));
    }
  }

  async function markRead(id: number) {
    setNotifications((prev) =>
      prev ? prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)) : prev
    );
    setUnreadCount((prev) => Math.max(0, prev - 1));
    try {
      await apiFetch(`/notifications/${id}/read`, { method: "PATCH" });
    } catch {
      refreshUnreadCount();
    }
  }

  async function markAllRead() {
    setNotifications((prev) => (prev ? prev.map((n) => ({ ...n, isRead: true })) : prev));
    setUnreadCount(0);
    try {
      await apiFetch("/notifications/read-all", { method: "POST" });
    } catch {
      refreshUnreadCount();
    }
  }

  return (
    <div ref={containerRef} className="relative">
      <button
        onClick={toggleOpen}
        aria-label="Notifications"
        className="relative rounded-md p-1.5 text-zinc-500 transition-colors hover:bg-black/[.04] hover:text-black dark:text-zinc-400 dark:hover:bg-white/[.06] dark:hover:text-zinc-50"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth={1.75}
          className="h-5 w-5"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0"
          />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-600 px-1 text-[10px] font-semibold text-white">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-20 mt-2 w-80 rounded-xl border border-black/[.08] bg-white p-3 shadow-lg dark:border-white/[.145] dark:bg-zinc-900">
          <div className="flex items-center justify-between px-1 pb-2">
            <h3 className="text-sm font-semibold text-black dark:text-zinc-50">Notifications</h3>
            {unreadCount > 0 && (
              <button
                onClick={markAllRead}
                className="text-xs font-medium text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
              >
                Mark all read
              </button>
            )}
          </div>

          {notifications === null ? (
            <p className="px-1 py-3 text-sm text-zinc-500 dark:text-zinc-400">Loading…</p>
          ) : notifications.length === 0 ? (
            <p className="px-1 py-3 text-sm text-zinc-500 dark:text-zinc-400">You&apos;re all caught up.</p>
          ) : (
            <ul className="flex max-h-96 flex-col divide-y divide-black/[.06] overflow-y-auto dark:divide-white/[.08]">
              {notifications.map((notification) => (
                <li key={notification.id}>
                  <button
                    onClick={() => !notification.isRead && markRead(notification.id)}
                    className={`flex w-full flex-col gap-0.5 px-1 py-2.5 text-left ${
                      notification.isRead ? "" : "bg-black/[.02] dark:bg-white/[.03]"
                    }`}
                  >
                    <div className="flex items-start gap-2">
                      {!notification.isRead && (
                        <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-600" />
                      )}
                      <p className="text-sm text-zinc-700 dark:text-zinc-300">{notification.message}</p>
                    </div>
                    <p className="pl-3.5 text-xs text-zinc-400 dark:text-zinc-500">
                      {formatRelativeTime(notification.createdAt)}
                    </p>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
