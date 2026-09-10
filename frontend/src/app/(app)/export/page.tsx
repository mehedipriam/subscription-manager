"use client";

import { useState } from "react";
import { apiDownload, ApiError } from "@/lib/apiClient";

type ExportKind = "subscriptions-csv" | "subscriptions-pdf" | "payments-csv" | "payments-pdf";

const EXPORTS: {
  kind: ExportKind;
  path: string;
  filename: string;
  label: string;
}[] = [
  { kind: "subscriptions-csv", path: "/export/subscriptions/csv", filename: "subscriptions.csv", label: "CSV" },
  { kind: "subscriptions-pdf", path: "/export/subscriptions/pdf", filename: "subscriptions.pdf", label: "PDF" },
  { kind: "payments-csv", path: "/export/payments/csv", filename: "payments.csv", label: "CSV" },
  { kind: "payments-pdf", path: "/export/payments/pdf", filename: "payments.pdf", label: "PDF" },
];

export default function ExportPage() {
  const [pending, setPending] = useState<ExportKind | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleDownload(item: (typeof EXPORTS)[number]) {
    setError(null);
    setPending(item.kind);
    try {
      await apiDownload(item.path, item.filename);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Download failed. Please try again.");
    } finally {
      setPending(null);
    }
  }

  const subscriptionExports = EXPORTS.filter((e) => e.kind.startsWith("subscriptions"));
  const paymentExports = EXPORTS.filter((e) => e.kind.startsWith("payments"));

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-4 py-8 sm:px-6">
      <div>
        <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">Export</h1>
        <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
          Download your subscriptions and payment history as a CSV or PDF file.
        </p>
      </div>

      {error && (
        <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-600 dark:bg-red-950/40 dark:text-red-400">
          {error}
        </p>
      )}

      <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
        <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Subscriptions</h2>
        <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">
          All active and cancelled subscriptions on your account.
        </p>
        <div className="mt-4 flex flex-wrap gap-2">
          {subscriptionExports.map((item) => (
            <button
              key={item.kind}
              onClick={() => handleDownload(item)}
              disabled={pending !== null}
              className="rounded-md border border-black/[.12] px-4 py-2 text-sm font-medium text-black transition-colors hover:bg-black/[.04] disabled:opacity-50 dark:border-white/[.16] dark:text-zinc-50 dark:hover:bg-white/[.06]"
            >
              {pending === item.kind ? "Preparing…" : `Download ${item.label}`}
            </button>
          ))}
        </div>
      </div>

      <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
        <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Payment history</h2>
        <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">
          Every recorded payment across all of your subscriptions.
        </p>
        <div className="mt-4 flex flex-wrap gap-2">
          {paymentExports.map((item) => (
            <button
              key={item.kind}
              onClick={() => handleDownload(item)}
              disabled={pending !== null}
              className="rounded-md border border-black/[.12] px-4 py-2 text-sm font-medium text-black transition-colors hover:bg-black/[.04] disabled:opacity-50 dark:border-white/[.16] dark:text-zinc-50 dark:hover:bg-white/[.06]"
            >
              {pending === item.kind ? "Preparing…" : `Download ${item.label}`}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
