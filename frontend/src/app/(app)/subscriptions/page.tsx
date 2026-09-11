"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/apiClient";
import type { Category, Subscription } from "@/lib/types";
import { formatCurrency, formatDate } from "@/lib/format";
import { SubscriptionFormPanel } from "@/components/subscriptions/SubscriptionFormPanel";
import { CategoryManager } from "@/components/subscriptions/CategoryManager";
import { PaymentHistoryPanel } from "@/components/subscriptions/PaymentHistoryPanel";

type LoadState =
  | { phase: "loading" }
  | { phase: "error"; message: string }
  | { phase: "ready"; subscriptions: Subscription[]; categories: Category[] };

const STATUS_STYLES: Record<string, string> = {
  ACTIVE: "bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400",
  PAUSED: "bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-400",
  CANCELLED: "bg-zinc-100 text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400",
  EXPIRED: "bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-400",
};

export default function SubscriptionsPage() {
  const [state, setState] = useState<LoadState>({ phase: "loading" });
  const [panel, setPanel] = useState<"none" | "add" | number>("none");
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [showCategoryManager, setShowCategoryManager] = useState(false);
  const [expandedPaymentsId, setExpandedPaymentsId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;

    Promise.all([
      apiFetch<Subscription[]>("/subscriptions"),
      apiFetch<Category[]>("/categories"),
    ])
      .then(([subscriptions, categories]) => {
        if (!cancelled) setState({ phase: "ready", subscriptions, categories });
      })
      .catch((err) => {
        if (!cancelled) {
          setState({
            phase: "error",
            message: err instanceof ApiError ? err.message : "Failed to load subscriptions.",
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  function handleSaved(subscription: Subscription) {
    setState((prev) => {
      if (prev.phase !== "ready") return prev;
      const exists = prev.subscriptions.some((s) => s.id === subscription.id);
      const subscriptions = exists
        ? prev.subscriptions.map((s) => (s.id === subscription.id ? subscription : s))
        : [subscription, ...prev.subscriptions];
      return { ...prev, subscriptions };
    });
    setPanel("none");
  }

  function handleCategoryCreated(category: Category) {
    setState((prev) =>
      prev.phase === "ready" ? { ...prev, categories: [...prev.categories, category] } : prev
    );
  }

  function handleCategoryDeleted(categoryId: number) {
    setState((prev) =>
      prev.phase === "ready"
        ? { ...prev, categories: prev.categories.filter((c) => c.id !== categoryId) }
        : prev
    );
  }

  async function handleDelete(id: number) {
    if (!confirm("Delete this subscription? This can't be undone.")) return;
    setDeletingId(id);
    try {
      await apiFetch(`/subscriptions/${id}`, { method: "DELETE" });
      setState((prev) =>
        prev.phase === "ready"
          ? { ...prev, subscriptions: prev.subscriptions.filter((s) => s.id !== id) }
          : prev
      );
      if (panel === id) setPanel("none");
    } catch {
      alert("Failed to delete subscription.");
    } finally {
      setDeletingId(null);
    }
  }

  if (state.phase === "loading") {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Loading subscriptions…</p>
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

  const { subscriptions, categories } = state;

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-4 py-8 sm:px-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">Subscriptions</h1>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            Everything you&apos;re billed for, in one place.
          </p>
        </div>
        {panel === "none" && (
          <div className="flex shrink-0 items-center gap-3">
            <button
              onClick={() => setShowCategoryManager((v) => !v)}
              className="rounded-md border border-black/[.12] px-4 py-2 text-sm font-medium text-black transition-colors hover:bg-black/[.04] dark:border-white/[.16] dark:text-zinc-50 dark:hover:bg-white/[.06]"
            >
              Manage categories
            </button>
            <button
              onClick={() => setPanel("add")}
              className="rounded-md bg-black px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-black dark:hover:bg-zinc-200"
            >
              Add subscription
            </button>
          </div>
        )}
      </div>

      {showCategoryManager && (
        <CategoryManager
          categories={categories}
          onDeleted={handleCategoryDeleted}
          onClose={() => setShowCategoryManager(false)}
        />
      )}

      {panel === "add" && (
        <SubscriptionFormPanel
          categories={categories}
          onSaved={handleSaved}
          onCancel={() => setPanel("none")}
          onCategoryCreated={handleCategoryCreated}
        />
      )}

      {subscriptions.length === 0 && panel !== "add" ? (
        <div className="flex flex-1 flex-col items-center justify-center rounded-xl border border-dashed border-black/[.12] bg-white px-6 py-16 text-center dark:border-white/[.16] dark:bg-zinc-900">
          <span className="text-4xl">📭</span>
          <h2 className="mt-4 text-lg font-semibold text-black dark:text-zinc-50">
            No subscriptions yet
          </h2>
          <p className="mt-1.5 max-w-sm text-sm text-zinc-500 dark:text-zinc-400">
            Add your first subscription — like a Claude Code or Gemini Plus plan — to start
            tracking it.
          </p>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          {subscriptions.map((subscription) =>
            panel === subscription.id ? (
              <SubscriptionFormPanel
                key={subscription.id}
                categories={categories}
                initial={subscription}
                onSaved={handleSaved}
                onCancel={() => setPanel("none")}
                onCategoryCreated={handleCategoryCreated}
              />
            ) : (
              <div
                key={subscription.id}
                className="rounded-xl border border-black/[.08] bg-white dark:border-white/[.145] dark:bg-zinc-900"
              >
                <div className="flex items-center justify-between gap-4 p-4">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="truncate font-medium text-black dark:text-zinc-50">
                        {subscription.name}
                      </span>
                      <span
                        className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${
                          STATUS_STYLES[subscription.status] ?? STATUS_STYLES.ACTIVE
                        }`}
                      >
                        {subscription.status.charAt(0) + subscription.status.slice(1).toLowerCase()}
                      </span>
                      {subscription.isTrial && (
                        <span className="shrink-0 rounded-full bg-sky-50 px-2 py-0.5 text-xs font-medium text-sky-700 dark:bg-sky-950/40 dark:text-sky-400">
                          Trial
                        </span>
                      )}
                    </div>
                    <p className="mt-0.5 truncate text-xs text-zinc-500 dark:text-zinc-400">
                      {subscription.category ? `${subscription.category.name} · ` : ""}
                      {subscription.billingCycle.toLowerCase()}
                      {subscription.nextBillingDate
                        ? ` · renews ${formatDate(subscription.nextBillingDate)}`
                        : ""}
                      {subscription.paymentCardLastFour
                        ? ` · •••• ${subscription.paymentCardLastFour}`
                        : ""}
                    </p>
                  </div>

                  <div className="flex shrink-0 items-center gap-4">
                    <span className="text-sm font-medium text-black dark:text-zinc-50">
                      {formatCurrency(subscription.price, subscription.currency)}
                    </span>
                    <button
                      onClick={() =>
                        setExpandedPaymentsId((prev) => (prev === subscription.id ? null : subscription.id))
                      }
                      className="text-sm text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
                    >
                      {expandedPaymentsId === subscription.id ? "Hide payments" : "Payments"}
                    </button>
                    <button
                      onClick={() => setPanel(subscription.id)}
                      className="text-sm text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleDelete(subscription.id)}
                      disabled={deletingId === subscription.id}
                      className="text-sm text-red-600 hover:text-red-700 disabled:opacity-50 dark:text-red-400 dark:hover:text-red-300"
                    >
                      {deletingId === subscription.id ? "Deleting…" : "Delete"}
                    </button>
                  </div>
                </div>

                {expandedPaymentsId === subscription.id && (
                  <PaymentHistoryPanel subscription={subscription} />
                )}
              </div>
            )
          )}
        </div>
      )}
    </div>
  );
}
