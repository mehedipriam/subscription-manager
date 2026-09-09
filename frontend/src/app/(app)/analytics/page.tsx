"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/apiClient";
import type { AnalyticsSummary } from "@/lib/types";
import { formatCurrency } from "@/lib/format";
import { MonthlyTrendChart } from "@/components/analytics/MonthlyTrendChart";
import { MostExpensiveList } from "@/components/analytics/MostExpensiveList";
import { CategoryBreakdownChart } from "@/components/dashboard/CategoryBreakdownChart";

type LoadState =
  | { phase: "loading" }
  | { phase: "error"; message: string }
  | { phase: "ready"; analytics: AnalyticsSummary };

export default function AnalyticsPage() {
  const [state, setState] = useState<LoadState>({ phase: "loading" });

  useEffect(() => {
    let cancelled = false;

    apiFetch<AnalyticsSummary>("/analytics/summary")
      .then((analytics) => {
        if (!cancelled) setState({ phase: "ready", analytics });
      })
      .catch((err) => {
        if (!cancelled) {
          setState({
            phase: "error",
            message: err instanceof ApiError ? err.message : "Failed to load analytics.",
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
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Loading analytics…</p>
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

  const { analytics } = state;

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-6 px-6 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">Analytics</h1>
        <div className="rounded-xl border border-black/[.08] bg-white px-5 py-3 text-right dark:border-white/[.145] dark:bg-zinc-900">
          <p className="text-xs text-zinc-500 dark:text-zinc-400">Projected yearly total</p>
          <p className="text-xl font-semibold text-black dark:text-zinc-50">
            {formatCurrency(analytics.yearlyTotal)}
          </p>
        </div>
      </div>

      <MonthlyTrendChart trend={analytics.monthlyTrend} />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <CategoryBreakdownChart breakdown={analytics.categoryBreakdown} />
        <MostExpensiveList items={analytics.mostExpensive} />
      </div>
    </div>
  );
}
