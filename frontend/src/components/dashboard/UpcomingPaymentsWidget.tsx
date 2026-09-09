import type { UpcomingPayment } from "@/lib/types";
import { formatCurrency, formatDate } from "@/lib/format";

export function UpcomingPaymentsWidget({ payments }: { payments: UpcomingPayment[] }) {
  return (
    <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
      <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Upcoming payments</h2>
      <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">Next 30 days</p>

      {payments.length === 0 ? (
        <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">Nothing renewing soon.</p>
      ) : (
        <ul className="mt-4 flex flex-col divide-y divide-black/[.06] dark:divide-white/[.08]">
          {payments.map((payment) => (
            <li key={payment.subscriptionId} className="flex items-center justify-between py-2.5">
              <div className="flex items-center gap-2.5">
                <span className="text-lg">{payment.category?.icon ?? "📦"}</span>
                <div>
                  <p className="text-sm font-medium text-black dark:text-zinc-50">{payment.name}</p>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    {formatDate(payment.nextBillingDate)}
                  </p>
                </div>
              </div>
              <span className="text-sm font-medium text-black dark:text-zinc-50">
                {formatCurrency(payment.price, payment.currency)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
