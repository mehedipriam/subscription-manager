import type { ActivityItem } from "@/lib/types";
import { formatCurrency, formatRelativeTime } from "@/lib/format";

function describe(item: ActivityItem): string {
  if (item.type === "PAYMENT_RECORDED") {
    return `Payment of ${formatCurrency(item.amount ?? 0, item.currency ?? "USD")} recorded for ${item.subscriptionName}`;
  }
  return `Added subscription ${item.subscriptionName}`;
}

export function RecentActivityFeed({ activity }: { activity: ActivityItem[] }) {
  return (
    <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
      <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Recent activity</h2>

      {activity.length === 0 ? (
        <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">Nothing here yet.</p>
      ) : (
        <ul className="mt-4 flex flex-col divide-y divide-black/[.06] dark:divide-white/[.08]">
          {activity.map((item, idx) => (
            <li key={`${item.type}-${item.subscriptionId}-${item.timestamp}-${idx}`} className="py-2.5">
              <p className="text-sm text-zinc-700 dark:text-zinc-300">{describe(item)}</p>
              <p className="text-xs text-zinc-400 dark:text-zinc-500">
                {formatRelativeTime(item.timestamp)}
              </p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
