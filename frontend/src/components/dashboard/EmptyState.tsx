import Link from "next/link";

export function EmptyState() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center rounded-xl border border-dashed border-black/[.12] bg-white px-6 py-16 text-center dark:border-white/[.16] dark:bg-zinc-900">
      <span className="text-4xl">📭</span>
      <h2 className="mt-4 text-lg font-semibold text-black dark:text-zinc-50">
        No subscriptions yet
      </h2>
      <p className="mt-1.5 max-w-sm text-sm text-zinc-500 dark:text-zinc-400">
        Once you add your first subscription, your spending summary, upcoming payments, and
        category breakdown will show up here.
      </p>
      <Link
        href="/subscriptions"
        className="mt-5 rounded-md bg-black px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-black dark:hover:bg-zinc-200"
      >
        Add subscription
      </Link>
    </div>
  );
}
