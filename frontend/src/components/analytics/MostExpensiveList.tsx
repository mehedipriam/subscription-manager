import type { TopSubscription } from "@/lib/types";
import { formatCurrency } from "@/lib/format";

export function MostExpensiveList({ items }: { items: TopSubscription[] }) {
  return (
    <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
      <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Most expensive</h2>
      <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">Ranked by normalized monthly cost</p>

      {items.length === 0 ? (
        <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">No active subscriptions yet.</p>
      ) : (
        <ol className="mt-4 flex flex-col divide-y divide-black/[.06] dark:divide-white/[.08]">
          {items.map((item, idx) => (
            <li key={item.subscriptionId} className="flex items-center justify-between py-2.5">
              <div className="flex items-center gap-3">
                <span className="w-4 text-sm font-medium text-zinc-400 dark:text-zinc-500">
                  {idx + 1}
                </span>
                <span className="text-lg">{item.category?.icon ?? "📦"}</span>
                <div>
                  <p className="text-sm font-medium text-black dark:text-zinc-50">{item.name}</p>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    {item.category?.name ?? "Uncategorized"}
                  </p>
                </div>
              </div>
              <span className="text-sm font-medium text-black dark:text-zinc-50">
                {formatCurrency(item.normalizedMonthlyCost, item.currency)}/mo
              </span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
