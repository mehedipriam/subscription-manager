import type { UsageInsight } from "@/lib/types";
import { formatCurrency } from "@/lib/format";

export function UsageInsightsWidget({ insights }: { insights: UsageInsight[] }) {
  return (
    <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
      <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Rarely used</h2>
      <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">
        Subscriptions you might not be getting your money&apos;s worth from
      </p>

      {insights.length === 0 ? (
        <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">
          Nothing flagged — everything looks in use.
        </p>
      ) : (
        <ul className="mt-4 flex flex-col divide-y divide-black/[.06] dark:divide-white/[.08]">
          {insights.map((insight) => (
            <li key={insight.subscriptionId} className="flex items-center justify-between py-2.5">
              <div>
                <p className="text-sm font-medium text-black dark:text-zinc-50">{insight.subscriptionName}</p>
                <p className="text-xs text-amber-600 dark:text-amber-400">
                  {insight.usageCount === 0
                    ? `Never logged as used (added ${insight.daysSinceLastUsed}d ago)`
                    : `Not used in ${insight.daysSinceLastUsed}d`}
                </p>
              </div>
              <span className="text-sm font-medium text-black dark:text-zinc-50">
                {formatCurrency(insight.monthlyCost, insight.currency)}/mo
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
