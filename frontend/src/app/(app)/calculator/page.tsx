"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/apiClient";
import type { CancellationSavings, Subscription } from "@/lib/types";
import { formatCurrency } from "@/lib/format";

type LoadState =
  | { phase: "loading" }
  | { phase: "error"; message: string }
  | { phase: "ready"; subscriptions: Subscription[] };

type CalcState =
  | { phase: "idle" }
  | { phase: "calculating" }
  | { phase: "ready"; savings: CancellationSavings };

export default function CalculatorPage() {
  const [state, setState] = useState<LoadState>({ phase: "loading" });
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [calc, setCalc] = useState<CalcState>({ phase: "idle" });

  useEffect(() => {
    let cancelled = false;

    apiFetch<Subscription[]>("/subscriptions")
      .then((subscriptions) => {
        if (!cancelled) {
          setState({ phase: "ready", subscriptions: subscriptions.filter((s) => s.status === "ACTIVE") });
        }
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

  useEffect(() => {
    if (selected.size === 0) {
      return;
    }

    let cancelled = false;

    apiFetch<CancellationSavings>("/savings-calculator", {
      method: "POST",
      body: JSON.stringify({ subscriptionIds: Array.from(selected) }),
    })
      .then((savings) => {
        if (!cancelled) setCalc({ phase: "ready", savings });
      })
      .catch(() => {
        if (!cancelled) setCalc({ phase: "idle" });
      });

    return () => {
      cancelled = true;
    };
  }, [selected]);

  function toggle(id: number) {
    const next = new Set(selected);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    setSelected(next);
    setCalc(next.size === 0 ? { phase: "idle" } : { phase: "calculating" });
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

  const { subscriptions } = state;

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-6 py-8">
      <div>
        <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">What if I cancel?</h1>
        <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
          Select subscriptions below to see how much you&apos;d save by cancelling them.
        </p>
      </div>

      {subscriptions.length === 0 ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">No active subscriptions to evaluate.</p>
      ) : (
        <div className="rounded-xl border border-black/[.08] bg-white dark:border-white/[.145] dark:bg-zinc-900">
          <ul className="flex flex-col divide-y divide-black/[.06] dark:divide-white/[.08]">
            {subscriptions.map((subscription) => (
              <li key={subscription.id}>
                <label className="flex cursor-pointer items-center justify-between gap-3 px-5 py-3">
                  <span className="flex items-center gap-3">
                    <input
                      type="checkbox"
                      checked={selected.has(subscription.id)}
                      onChange={() => toggle(subscription.id)}
                      className="h-4 w-4 rounded border-black/[.24] dark:border-white/[.24]"
                    />
                    <span>
                      <span className="block text-sm font-medium text-black dark:text-zinc-50">
                        {subscription.name}
                      </span>
                      <span className="block text-xs text-zinc-500 dark:text-zinc-400">
                        {subscription.billingCycle.toLowerCase()}
                      </span>
                    </span>
                  </span>
                  <span className="text-sm text-zinc-500 dark:text-zinc-400">
                    {formatCurrency(subscription.price, subscription.currency)}
                  </span>
                </label>
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
        <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Potential savings</h2>
        {calc.phase === "idle" ? (
          <p className="mt-2 text-sm text-zinc-500 dark:text-zinc-400">
            Select subscriptions above to calculate.
          </p>
        ) : calc.phase === "calculating" ? (
          <p className="mt-2 text-sm text-zinc-500 dark:text-zinc-400">Calculating…</p>
        ) : (
          <div className="mt-3 grid grid-cols-2 gap-4">
            <div>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">Per month</p>
              <p className="text-xl font-semibold text-emerald-600 dark:text-emerald-400">
                {formatCurrency(calc.savings.monthlySavings)}
              </p>
            </div>
            <div>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">Per year</p>
              <p className="text-xl font-semibold text-emerald-600 dark:text-emerald-400">
                {formatCurrency(calc.savings.yearlySavings)}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
