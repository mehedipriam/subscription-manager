"use client";

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import type { CategorySpend } from "@/lib/types";
import { formatCurrency } from "@/lib/format";

const FALLBACK_COLOR = "#9E9E9E";

export function CategoryBreakdownChart({ breakdown }: { breakdown: CategorySpend[] }) {
  const data = breakdown.map((entry) => ({
    name: entry.categoryName,
    value: entry.monthlyAmount,
    color: entry.color ?? FALLBACK_COLOR,
    icon: entry.icon,
    percentage: entry.percentage,
  }));

  return (
    <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
      <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Spending by category</h2>
      <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">Monthly, normalized</p>

      {data.length === 0 ? (
        <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">No active subscriptions yet.</p>
      ) : (
        <div className="mt-2 flex flex-col items-center gap-4 sm:flex-row">
          <div className="h-48 w-48 shrink-0">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={data}
                  dataKey="value"
                  nameKey="name"
                  innerRadius="60%"
                  outerRadius="90%"
                  paddingAngle={2}
                  strokeWidth={0}
                >
                  {data.map((entry) => (
                    <Cell key={entry.name} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(value) => formatCurrency(Number(value))}
                  contentStyle={{ fontSize: 12, borderRadius: 8 }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <ul className="flex w-full flex-col gap-2">
            {data.map((entry) => (
              <li key={entry.name} className="flex items-center justify-between text-sm">
                <span className="flex items-center gap-2 text-zinc-700 dark:text-zinc-300">
                  <span
                    className="inline-block h-2.5 w-2.5 rounded-full"
                    style={{ backgroundColor: entry.color }}
                  />
                  {entry.icon} {entry.name}
                </span>
                <span className="text-zinc-500 dark:text-zinc-400">
                  {formatCurrency(entry.value)} &middot; {entry.percentage}%
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
