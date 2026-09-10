"use client";

import { useState } from "react";
import { apiFetch, ApiError } from "@/lib/apiClient";
import type { BudgetStatus } from "@/lib/types";
import { formatCurrency, formatMonthLabel } from "@/lib/format";

export function BudgetWidget({
  status,
  onUpdated,
}: {
  status: BudgetStatus;
  onUpdated: (status: BudgetStatus) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [amount, setAmount] = useState(status.budgetAmount?.toString() ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    const parsed = Number(amount);
    if (!Number.isFinite(parsed) || parsed < 0) {
      setError("Enter a valid amount.");
      return;
    }

    setError(null);
    setSaving(true);
    try {
      await apiFetch("/budget", {
        method: "PUT",
        body: JSON.stringify({ amount: parsed, periodMonth: status.periodMonth }),
      });
      const refreshed = await apiFetch<BudgetStatus>("/budget/current");
      onUpdated(refreshed);
      setEditing(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save budget.");
    } finally {
      setSaving(false);
    }
  }

  const percentageUsed = status.percentageUsed ?? 0;
  const barColor = status.exceeded
    ? "bg-rose-600"
    : percentageUsed >= 90
      ? "bg-amber-500"
      : "bg-emerald-600";

  return (
    <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Budget</h2>
          <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">
            {formatMonthLabel(status.periodMonth)}
          </p>
        </div>
        {!editing && (
          <button
            onClick={() => setEditing(true)}
            className="text-xs font-medium text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
          >
            {status.budgetAmount === null ? "Set budget" : "Edit"}
          </button>
        )}
      </div>

      {editing ? (
        <form onSubmit={handleSave} className="mt-4 flex flex-col gap-2">
          <div className="flex items-center gap-2">
            <input
              type="number"
              min="0"
              step="0.01"
              autoFocus
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="Monthly budget"
              className="w-full rounded-md border border-black/[.12] bg-transparent px-2.5 py-1.5 text-sm text-black outline-none focus:border-zinc-950 dark:border-white/[.16] dark:text-zinc-50 dark:focus:border-zinc-50"
            />
            <button
              type="submit"
              disabled={saving}
              className="shrink-0 rounded-md bg-black px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-white dark:text-black"
            >
              Save
            </button>
            <button
              type="button"
              onClick={() => setEditing(false)}
              className="shrink-0 text-sm text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
            >
              Cancel
            </button>
          </div>
          {error && <p className="text-xs text-red-600 dark:text-red-400">{error}</p>}
        </form>
      ) : status.budgetAmount === null ? (
        <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">
          No budget set. Projected spend this month: {formatCurrency(status.projectedSpend)}.
        </p>
      ) : (
        <div className="mt-4">
          <div className="flex items-baseline justify-between text-sm">
            <span className="font-medium text-black dark:text-zinc-50">
              {formatCurrency(status.projectedSpend)}
            </span>
            <span className="text-zinc-500 dark:text-zinc-400">of {formatCurrency(status.budgetAmount)}</span>
          </div>
          <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-black/[.06] dark:bg-white/[.08]">
            <div
              className={`h-full rounded-full ${barColor}`}
              style={{ width: `${Math.min(100, percentageUsed)}%` }}
            />
          </div>
          <p
            className={`mt-2 text-xs ${
              status.exceeded ? "text-rose-600 dark:text-rose-400" : "text-zinc-500 dark:text-zinc-400"
            }`}
          >
            {status.exceeded
              ? `Over budget by ${formatCurrency(Math.abs(status.remaining ?? 0))}`
              : `${formatCurrency(status.remaining ?? 0)} remaining`}
          </p>
        </div>
      )}
    </div>
  );
}
