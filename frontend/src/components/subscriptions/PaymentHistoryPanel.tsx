"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/apiClient";
import type { Payment, PaymentStatus, Subscription } from "@/lib/types";
import { formatCurrency, formatDate } from "@/lib/format";

const STATUSES: PaymentStatus[] = ["SUCCESS", "FAILED", "PENDING", "REFUNDED"];

const fieldClass =
  "rounded-md border border-black/[.12] bg-transparent px-3 py-2 text-sm text-black outline-none focus:border-zinc-950 dark:border-white/[.16] dark:text-zinc-50 dark:focus:border-zinc-50";

const STATUS_STYLES: Record<PaymentStatus, string> = {
  SUCCESS: "bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400",
  FAILED: "bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-400",
  PENDING: "bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-400",
  REFUNDED: "bg-zinc-100 text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400",
};

type LoadState =
  | { phase: "loading" }
  | { phase: "error"; message: string }
  | { phase: "ready"; payments: Payment[] };

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export function PaymentHistoryPanel({ subscription }: { subscription: Subscription }) {
  const [state, setState] = useState<LoadState>({ phase: "loading" });
  const [showForm, setShowForm] = useState(false);
  const [amount, setAmount] = useState(subscription.price.toString());
  const [paymentDate, setPaymentDate] = useState(todayIso());
  const [status, setStatus] = useState<PaymentStatus>("SUCCESS");
  const [reference, setReference] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;

    apiFetch<Payment[]>(`/subscriptions/${subscription.id}/payments`)
      .then((payments) => {
        if (!cancelled) setState({ phase: "ready", payments });
      })
      .catch((err) => {
        if (!cancelled) {
          setState({
            phase: "error",
            message: err instanceof ApiError ? err.message : "Failed to load payments.",
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [subscription.id]);

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    const parsedAmount = Number(amount);
    if (!Number.isFinite(parsedAmount) || parsedAmount < 0) {
      setError("Enter a valid amount.");
      return;
    }

    setError(null);
    setSaving(true);
    try {
      const payment = await apiFetch<Payment>(`/subscriptions/${subscription.id}/payments`, {
        method: "POST",
        body: JSON.stringify({
          amount: parsedAmount,
          currency: subscription.currency,
          paymentDate,
          status,
          transactionReference: reference.trim() || null,
        }),
      });
      setState((prev) =>
        prev.phase === "ready" ? { ...prev, payments: [payment, ...prev.payments] } : prev
      );
      setShowForm(false);
      setReference("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to record payment.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(paymentId: number) {
    if (!confirm("Delete this payment record?")) return;
    setDeletingId(paymentId);
    try {
      await apiFetch(`/subscriptions/${subscription.id}/payments/${paymentId}`, {
        method: "DELETE",
      });
      setState((prev) =>
        prev.phase === "ready"
          ? { ...prev, payments: prev.payments.filter((p) => p.id !== paymentId) }
          : prev
      );
    } catch {
      alert("Failed to delete payment.");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="border-t border-black/[.06] px-4 py-3 dark:border-white/[.08]">
      {state.phase === "loading" && (
        <p className="text-xs text-zinc-500 dark:text-zinc-400">Loading payments…</p>
      )}
      {state.phase === "error" && (
        <p className="text-xs text-red-600 dark:text-red-400">{state.message}</p>
      )}
      {state.phase === "ready" && (
        <>
          {state.payments.length === 0 ? (
            <p className="text-xs text-zinc-500 dark:text-zinc-400">No payments recorded yet.</p>
          ) : (
            <ul className="flex flex-col divide-y divide-black/[.06] dark:divide-white/[.08]">
              {state.payments.map((payment) => (
                <li key={payment.id} className="flex items-center justify-between gap-3 py-2 text-sm">
                  <div className="flex items-center gap-2">
                    <span className="text-black dark:text-zinc-50">
                      {formatCurrency(payment.amount, payment.currency)}
                    </span>
                    <span className="text-xs text-zinc-500 dark:text-zinc-400">
                      {formatDate(payment.paymentDate)}
                    </span>
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[payment.status]}`}
                    >
                      {payment.status.charAt(0) + payment.status.slice(1).toLowerCase()}
                    </span>
                    {payment.transactionReference && (
                      <span className="text-xs text-zinc-400">{payment.transactionReference}</span>
                    )}
                  </div>
                  <button
                    onClick={() => handleDelete(payment.id)}
                    disabled={deletingId === payment.id}
                    className="text-xs text-red-600 hover:text-red-700 disabled:opacity-50 dark:text-red-400 dark:hover:text-red-300"
                  >
                    {deletingId === payment.id ? "Deleting…" : "Delete"}
                  </button>
                </li>
              ))}
            </ul>
          )}

          {showForm ? (
            <form onSubmit={handleAdd} className="mt-3 flex flex-col gap-2">
              <div className="flex flex-wrap gap-2">
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  required
                  autoFocus
                  placeholder="Amount"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className={`w-28 ${fieldClass}`}
                />
                <input
                  type="date"
                  required
                  value={paymentDate}
                  onChange={(e) => setPaymentDate(e.target.value)}
                  className={fieldClass}
                />
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as PaymentStatus)}
                  className={fieldClass}
                >
                  {STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {s.charAt(0) + s.slice(1).toLowerCase()}
                    </option>
                  ))}
                </select>
                <input
                  placeholder="Reference (optional)"
                  value={reference}
                  onChange={(e) => setReference(e.target.value)}
                  className={`min-w-0 flex-1 ${fieldClass}`}
                />
              </div>
              {error && <p className="text-xs text-red-600 dark:text-red-400">{error}</p>}
              <div className="flex items-center gap-3">
                <button
                  type="submit"
                  disabled={saving}
                  className="rounded-md bg-black px-3 py-1.5 text-xs font-medium text-white disabled:opacity-50 dark:bg-white dark:text-black"
                >
                  {saving ? "Saving…" : "Record payment"}
                </button>
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="text-xs text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
                >
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <button
              onClick={() => setShowForm(true)}
              className="mt-3 text-xs font-medium text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
            >
              + Record a payment
            </button>
          )}
        </>
      )}
    </div>
  );
}
