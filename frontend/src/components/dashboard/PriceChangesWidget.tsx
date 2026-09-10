import type { PriceChange } from "@/lib/types";
import { formatCurrency, formatRelativeTime } from "@/lib/format";

function formatPercentage(value: number): string {
  const sign = value > 0 ? "+" : "";
  return `${sign}${value}%`;
}

export function PriceChangesWidget({ changes }: { changes: PriceChange[] }) {
  return (
    <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
      <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Recent price changes</h2>

      {changes.length === 0 ? (
        <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">No price changes recorded yet.</p>
      ) : (
        <ul className="mt-4 flex flex-col divide-y divide-black/[.06] dark:divide-white/[.08]">
          {changes.map((change, idx) => {
            const increased = change.percentageChange > 0;
            const unchanged = change.percentageChange === 0;
            return (
              <li key={`${change.subscriptionId}-${change.changedAt}-${idx}`} className="py-2.5">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-medium text-black dark:text-zinc-50">{change.subscriptionName}</p>
                  <span
                    className={
                      unchanged
                        ? "text-sm font-medium text-zinc-500 dark:text-zinc-400"
                        : increased
                          ? "text-sm font-medium text-rose-600 dark:text-rose-400"
                          : "text-sm font-medium text-emerald-600 dark:text-emerald-400"
                    }
                  >
                    {formatPercentage(change.percentageChange)}
                  </span>
                </div>
                <div className="mt-0.5 flex items-center justify-between">
                  <p className="text-xs text-zinc-400 dark:text-zinc-500">
                    {formatRelativeTime(change.changedAt)}
                  </p>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    {formatCurrency(change.oldPrice, change.currency)} &rarr;{" "}
                    {formatCurrency(change.newPrice, change.currency)}
                  </p>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
