"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/apiClient";
import type { Subscription, UsageInsight, UsageLogEntry } from "@/lib/types";
import { formatCurrency, formatRelativeTime } from "@/lib/format";

type LoadState =
  | { phase: "loading" }
  | { phase: "error"; message: string }
  | { phase: "ready"; subscriptions: Subscription[]; insights: Map<number, UsageInsight> };

export default function UsagePage() {
  const [state, setState] = useState<LoadState>({ phase: "loading" });
  const [loggingId, setLoggingId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;

    Promise.all([
      apiFetch<Subscription[]>("/subscriptions"),
      apiFetch<UsageInsight[]>("/usage-insights"),
    ])
      .then(([subscriptions, insights]) => {
        if (!cancelled) {
          setState({
            phase: "ready",
            subscriptions: subscriptions.filter((s) => s.status === "ACTIVE"),
            insights: new Map(insights.map((insight) => [insight.subscriptionId, insight])),
          });
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setState({
            phase: "error",
            message: err instanceof ApiError ? err.message : "Failed to load usage data.",
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  async function logUsage(subscriptionId: number) {
    setLoggingId(subscriptionId);
    try {
      const logged = await apiFetch<UsageLogEntry>(`/subscriptions/${subscriptionId}/usage`, {
        method: "POST",
      });
      setState((prev) => {
        if (prev.phase !== "ready") return prev;
        const existing = prev.insights.get(subscriptionId);
        if (!existing) return prev;

        const next = new Map(prev.insights);
        next.set(subscriptionId, {
          ...existing,
          lastUsedAt: logged.usedAt,
          daysSinceLastUsed: 0,
          usageCount: existing.usageCount + 1,
          rarelyUsed: false,
        });
        return { ...prev, insights: next };
      });
    } catch {
      // best-effort: leave state as-is on failure
    } finally {
      setLoggingId(null);
    }
  }

  if (state.phase === "loading") {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Loading usage data…</p>
      </div>
    );
  }

  if (state.phase === "error") {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-red-600 dark:text-red-400">{state.message}</p>
      </div>
    );
  }

  const { subscriptions, insights } = state;

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-6 py-8">
      <div>
        <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">Usage</h1>
        <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
          Log when you actually use a subscription — rarely-used ones get flagged on your dashboard.
        </p>
      </div>

      {subscriptions.length === 0 ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">No active subscriptions to track.</p>
      ) : (
        <div className="rounded-xl border border-black/[.08] bg-white dark:border-white/[.145] dark:bg-zinc-900">
          <ul className="flex flex-col divide-y divide-black/[.06] dark:divide-white/[.08]">
            {subscriptions.map((subscription) => {
              const insight = insights.get(subscription.id);
              return (
                <li key={subscription.id} className="flex items-center justify-between gap-3 px-5 py-3">
                  <div>
                    <p className="text-sm font-medium text-black dark:text-zinc-50">{subscription.name}</p>
                    <p
                      className={`text-xs ${
                        insight?.rarelyUsed
                          ? "text-amber-600 dark:text-amber-400"
                          : "text-zinc-500 dark:text-zinc-400"
                      }`}
                    >
                      {insight
                        ? insight.lastUsedAt
                          ? `Last used ${formatRelativeTime(insight.lastUsedAt)} · ${insight.usageCount} log${insight.usageCount === 1 ? "" : "s"}`
                          : "Never logged as used"
                        : "—"}
                      {" · "}
                      {formatCurrency(subscription.price, subscription.currency)}/{subscription.billingCycle.toLowerCase()}
                    </p>
                  </div>
                  <button
                    onClick={() => logUsage(subscription.id)}
                    disabled={loggingId === subscription.id}
                    className="shrink-0 rounded-md border border-black/[.12] px-3 py-1.5 text-sm font-medium text-black transition-colors hover:bg-black/[.04] disabled:opacity-50 dark:border-white/[.16] dark:text-zinc-50 dark:hover:bg-white/[.06]"
                  >
                    {loggingId === subscription.id ? "Logging…" : "Log usage"}
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
}
