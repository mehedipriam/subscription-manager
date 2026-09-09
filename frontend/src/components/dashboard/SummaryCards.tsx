import type { DashboardSummary } from "@/lib/types";
import { formatCurrency } from "@/lib/format";

export function SummaryCards({ summary }: { summary: DashboardSummary }) {
  const cards = [
    { label: "Monthly spend", value: formatCurrency(summary.totalMonthlySpend) },
    { label: "Yearly spend", value: formatCurrency(summary.totalYearlySpend) },
    { label: "Active subscriptions", value: String(summary.activeSubscriptionCount) },
    { label: "Renewing within 7 days", value: String(summary.upcomingRenewalsCount) },
  ];

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {cards.map((card) => (
        <div
          key={card.label}
          className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900"
        >
          <p className="text-sm text-zinc-500 dark:text-zinc-400">{card.label}</p>
          <p className="mt-2 text-2xl font-semibold text-black dark:text-zinc-50">{card.value}</p>
        </div>
      ))}
    </div>
  );
}
