"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/apiClient";
import type { DashboardSummary } from "@/lib/types";
import { SummaryCards } from "@/components/dashboard/SummaryCards";
import { UpcomingPaymentsWidget } from "@/components/dashboard/UpcomingPaymentsWidget";
import { CategoryBreakdownChart } from "@/components/dashboard/CategoryBreakdownChart";
import { RecentActivityFeed } from "@/components/dashboard/RecentActivityFeed";
import { PriceChangesWidget } from "@/components/dashboard/PriceChangesWidget";
import { EmptyState } from "@/components/dashboard/EmptyState";

type LoadState =
  | { phase: "loading" }
  | { phase: "error"; message: string }
  | { phase: "ready"; summary: DashboardSummary };

export default function DashboardPage() {
  const [state, setState] = useState<LoadState>({ phase: "loading" });

  useEffect(() => {
    let cancelled = false;

    apiFetch<DashboardSummary>("/dashboard/summary")
      .then((summary) => {
        if (!cancelled) setState({ phase: "ready", summary });
      })
      .catch((err) => {
        if (!cancelled) {
          setState({
            phase: "error",
            message: err instanceof ApiError ? err.message : "Failed to load dashboard.",
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  if (state.phase === "loading") {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Loading dashboard…</p>
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

  const { summary } = state;
  const isEmpty = summary.activeSubscriptionCount === 0 && summary.recentActivity.length === 0;

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-6 px-6 py-8">
      <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">Dashboard</h1>

      {isEmpty ? (
        <EmptyState />
      ) : (
        <>
          <SummaryCards summary={summary} />
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <UpcomingPaymentsWidget payments={summary.upcomingPayments} />
            <CategoryBreakdownChart breakdown={summary.categoryBreakdown} />
          </div>
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <RecentActivityFeed activity={summary.recentActivity} />
            <PriceChangesWidget changes={summary.recentPriceChanges} />
          </div>
        </>
      )}
    </div>
  );
}
