"use client";

import { useState } from "react";
import { apiFetch, ApiError } from "@/lib/apiClient";
import type { BillingCycle, Category, Subscription, SubscriptionStatus } from "@/lib/types";

const BILLING_CYCLES: BillingCycle[] = ["WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"];
const STATUSES: SubscriptionStatus[] = ["ACTIVE", "PAUSED", "CANCELLED", "EXPIRED"];

const fieldClass =
  "rounded-md border border-black/[.12] bg-transparent px-3 py-2 text-sm text-black outline-none focus:border-zinc-950 dark:border-white/[.16] dark:text-zinc-50 dark:focus:border-zinc-50";
const inputClass = `w-full ${fieldClass}`;
const labelClass = "text-sm font-medium text-zinc-700 dark:text-zinc-300";

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

const NEW_CATEGORY_VALUE = "__new__";

export function SubscriptionFormPanel({
  categories,
  initial,
  onSaved,
  onCancel,
  onCategoryCreated,
}: {
  categories: Category[];
  initial?: Subscription;
  onSaved: (subscription: Subscription) => void;
  onCancel: () => void;
  onCategoryCreated: (category: Category) => void;
}) {
  const [name, setName] = useState(initial?.name ?? "");
  const [categoryId, setCategoryId] = useState(initial?.category?.id.toString() ?? "");
  const [addingCategory, setAddingCategory] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState("");
  const [newCategoryIcon, setNewCategoryIcon] = useState("");
  const [categoryError, setCategoryError] = useState<string | null>(null);
  const [creatingCategory, setCreatingCategory] = useState(false);
  const [price, setPrice] = useState(initial?.price.toString() ?? "");
  const [currency, setCurrency] = useState(initial?.currency ?? "USD");
  const [billingCycle, setBillingCycle] = useState<BillingCycle>(initial?.billingCycle ?? "MONTHLY");
  const [startDate, setStartDate] = useState(initial?.startDate ?? todayIso());
  const [status, setStatus] = useState<SubscriptionStatus>(initial?.status ?? "ACTIVE");
  const [isTrial, setIsTrial] = useState(initial?.isTrial ?? false);
  const [trialEndDate, setTrialEndDate] = useState(initial?.trialEndDate ?? "");
  const [cancelUrl, setCancelUrl] = useState(initial?.cancelUrl ?? "");
  const [cardLastFour, setCardLastFour] = useState(initial?.paymentCardLastFour ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleCreateCategory() {
    if (!newCategoryName.trim()) {
      setCategoryError("Category name is required.");
      return;
    }
    setCategoryError(null);
    setCreatingCategory(true);
    try {
      const category = await apiFetch<Category>("/categories", {
        method: "POST",
        body: JSON.stringify({
          name: newCategoryName.trim(),
          icon: newCategoryIcon.trim() || null,
          color: null,
        }),
      });
      onCategoryCreated(category);
      setCategoryId(category.id.toString());
      setAddingCategory(false);
      setNewCategoryName("");
      setNewCategoryIcon("");
    } catch (err) {
      setCategoryError(err instanceof ApiError ? err.message : "Failed to create category.");
    } finally {
      setCreatingCategory(false);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const parsedPrice = Number(price);
    if (!name.trim()) {
      setError("Name is required.");
      return;
    }
    if (!Number.isFinite(parsedPrice) || parsedPrice < 0) {
      setError("Enter a valid price.");
      return;
    }
    if (cardLastFour && !/^\d{4}$/.test(cardLastFour)) {
      setError("Card last 4 digits must be exactly 4 digits.");
      return;
    }

    setError(null);
    setSaving(true);
    try {
      const body = {
        name: name.trim(),
        description: null,
        categoryId: categoryId ? Number(categoryId) : null,
        price: parsedPrice,
        currency: currency.trim().toUpperCase() || "USD",
        billingCycle,
        startDate,
        nextBillingDate: null,
        status,
        isTrial,
        trialEndDate: isTrial && trialEndDate ? trialEndDate : null,
        cancelUrl: cancelUrl.trim() || null,
        cancellationInstructions: null,
        paymentCardLastFour: cardLastFour || null,
      };

      const saved = initial
        ? await apiFetch<Subscription>(`/subscriptions/${initial.id}`, {
            method: "PUT",
            body: JSON.stringify(body),
          })
        : await apiFetch<Subscription>("/subscriptions", {
            method: "POST",
            body: JSON.stringify(body),
          });

      onSaved(saved);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save subscription.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900"
    >
      <h2 className="text-sm font-semibold text-black dark:text-zinc-50">
        {initial ? "Edit subscription" : "Add subscription"}
      </h2>

      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="flex flex-col gap-1.5 sm:col-span-2">
          <label htmlFor="sub-name" className={labelClass}>
            Name
          </label>
          <input
            id="sub-name"
            autoFocus
            required
            placeholder="e.g. Claude Code, Gemini Plus"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputClass}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="sub-category" className={labelClass}>
            Category
          </label>
          <select
            id="sub-category"
            value={categoryId}
            onChange={(e) => {
              if (e.target.value === NEW_CATEGORY_VALUE) {
                setAddingCategory(true);
                return;
              }
              setCategoryId(e.target.value);
            }}
            className={inputClass}
          >
            <option value="">No category</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.icon ? `${category.icon} ` : ""}
                {category.name}
              </option>
            ))}
            <option value={NEW_CATEGORY_VALUE}>+ Add new category…</option>
          </select>

          {addingCategory && (
            <div className="mt-1 flex flex-col gap-2 rounded-md border border-black/[.12] p-3 dark:border-white/[.16]">
              <div className="flex gap-2">
                <input
                  autoFocus
                  placeholder="Category name"
                  value={newCategoryName}
                  onChange={(e) => setNewCategoryName(e.target.value)}
                  className={`min-w-0 flex-1 ${fieldClass}`}
                />
                <input
                  placeholder="🏷️"
                  maxLength={4}
                  value={newCategoryIcon}
                  onChange={(e) => setNewCategoryIcon(e.target.value)}
                  className={`w-16 shrink-0 text-center ${fieldClass}`}
                />
              </div>
              {categoryError && (
                <p className="text-xs text-red-600 dark:text-red-400">{categoryError}</p>
              )}
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={handleCreateCategory}
                  disabled={creatingCategory}
                  className="rounded-md bg-black px-3 py-1.5 text-xs font-medium text-white disabled:opacity-50 dark:bg-white dark:text-black"
                >
                  {creatingCategory ? "Adding…" : "Add category"}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setAddingCategory(false);
                    setCategoryError(null);
                  }}
                  className="text-xs text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="sub-status" className={labelClass}>
            Status
          </label>
          <select
            id="sub-status"
            value={status}
            onChange={(e) => setStatus(e.target.value as SubscriptionStatus)}
            className={inputClass}
          >
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s.charAt(0) + s.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="sub-price" className={labelClass}>
            Price
          </label>
          <input
            id="sub-price"
            type="number"
            min="0"
            step="0.01"
            required
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            className={inputClass}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="sub-currency" className={labelClass}>
            Currency
          </label>
          <input
            id="sub-currency"
            maxLength={3}
            placeholder="USD"
            value={currency}
            onChange={(e) => setCurrency(e.target.value.toUpperCase())}
            className={inputClass}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="sub-billing-cycle" className={labelClass}>
            Billing cycle
          </label>
          <select
            id="sub-billing-cycle"
            value={billingCycle}
            onChange={(e) => setBillingCycle(e.target.value as BillingCycle)}
            className={inputClass}
          >
            {BILLING_CYCLES.map((cycle) => (
              <option key={cycle} value={cycle}>
                {cycle.charAt(0) + cycle.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="sub-start-date" className={labelClass}>
            Start date
          </label>
          <input
            id="sub-start-date"
            type="date"
            required
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className={inputClass}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="sub-cancel-url" className={labelClass}>
            Cancel URL <span className="text-zinc-400">(optional)</span>
          </label>
          <input
            id="sub-cancel-url"
            type="url"
            placeholder="https://…"
            value={cancelUrl}
            onChange={(e) => setCancelUrl(e.target.value)}
            className={inputClass}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="sub-card-last-four" className={labelClass}>
            Card last 4 digits <span className="text-zinc-400">(optional)</span>
          </label>
          <input
            id="sub-card-last-four"
            inputMode="numeric"
            placeholder="1234"
            maxLength={4}
            value={cardLastFour}
            onChange={(e) => setCardLastFour(e.target.value.replace(/\D/g, "").slice(0, 4))}
            className={inputClass}
          />
          <p className="text-xs text-zinc-400">
            So you can tell which card or account paid for this.
          </p>
        </div>

        <div className="flex flex-col gap-2 sm:col-span-2">
          <label className="flex items-center gap-2 text-sm text-zinc-700 dark:text-zinc-300">
            <input
              type="checkbox"
              checked={isTrial}
              onChange={(e) => setIsTrial(e.target.checked)}
              className="h-4 w-4 rounded border-black/[.24] dark:border-white/[.24]"
            />
            This is a free trial
          </label>
          {isTrial && (
            <div className="flex flex-col gap-1.5 sm:max-w-xs">
              <label htmlFor="sub-trial-end" className={labelClass}>
                Trial ends
              </label>
              <input
                id="sub-trial-end"
                type="date"
                value={trialEndDate}
                onChange={(e) => setTrialEndDate(e.target.value)}
                className={inputClass}
              />
            </div>
          )}
        </div>
      </div>

      {error && (
        <p className="mt-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-600 dark:bg-red-950/40 dark:text-red-400">
          {error}
        </p>
      )}

      <div className="mt-5 flex items-center gap-3">
        <button
          type="submit"
          disabled={saving}
          className="rounded-md bg-black px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-zinc-800 disabled:opacity-50 dark:bg-white dark:text-black dark:hover:bg-zinc-200"
        >
          {saving ? "Saving…" : initial ? "Save changes" : "Add subscription"}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="text-sm text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
