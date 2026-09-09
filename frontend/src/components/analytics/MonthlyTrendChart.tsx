"use client";

import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { MonthlySpendPoint } from "@/lib/types";
import { formatCurrency, formatMonthLabel } from "@/lib/format";

const BAR_COLOR = "#4285F4";

export function MonthlyTrendChart({ trend }: { trend: MonthlySpendPoint[] }) {
  const data = trend.map((point) => ({
    month: formatMonthLabel(point.month),
    amount: point.totalSpend,
  }));
  const hasSpend = trend.some((point) => point.totalSpend > 0);

  return (
    <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
      <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Monthly spend trend</h2>
      <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">
        Actual payments recorded, last 12 months
      </p>

      {!hasSpend ? (
        <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">
          No payments recorded yet in this window.
        </p>
      ) : (
        <div className="mt-4 h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} margin={{ top: 4, right: 8, bottom: 0, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="currentColor" opacity={0.1} />
              <XAxis
                dataKey="month"
                tick={{ fontSize: 12 }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                tick={{ fontSize: 12 }}
                axisLine={false}
                tickLine={false}
                width={48}
                tickFormatter={(value: number) => formatCurrency(value)}
              />
              <Tooltip
                formatter={(value) => formatCurrency(Number(value))}
                contentStyle={{ fontSize: 12, borderRadius: 8 }}
              />
              <Bar dataKey="amount" fill={BAR_COLOR} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}
