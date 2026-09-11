"use client";

import { useState } from "react";
import { apiFetch, ApiError } from "@/lib/apiClient";
import type { Category } from "@/lib/types";

export function CategoryManager({
  categories,
  onDeleted,
  onClose,
}: {
  categories: Category[];
  onDeleted: (categoryId: number) => void;
  onClose: () => void;
}) {
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleDelete(category: Category) {
    if (!confirm(`Delete the "${category.name}" category?`)) return;
    setError(null);
    setDeletingId(category.id);
    try {
      await apiFetch(`/categories/${category.id}`, { method: "DELETE" });
      onDeleted(category.id);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete category.");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="rounded-xl border border-black/[.08] bg-white p-5 dark:border-white/[.145] dark:bg-zinc-900">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold text-black dark:text-zinc-50">Manage categories</h2>
        <button
          onClick={onClose}
          className="text-sm text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
        >
          Close
        </button>
      </div>

      {error && (
        <p className="mt-3 rounded-md bg-red-50 px-3 py-2 text-sm text-red-600 dark:bg-red-950/40 dark:text-red-400">
          {error}
        </p>
      )}

      <ul className="mt-3 flex flex-col divide-y divide-black/[.06] dark:divide-white/[.08]">
        {categories.map((category) => (
          <li key={category.id} className="flex items-center justify-between gap-3 py-2">
            <span className="flex items-center gap-2 text-sm text-black dark:text-zinc-50">
              {category.icon ? `${category.icon} ` : ""}
              {category.name}
              {category.isDefault && (
                <span className="rounded-full bg-black/[.06] px-2 py-0.5 text-xs font-medium text-zinc-500 dark:bg-white/[.08] dark:text-zinc-400">
                  Default
                </span>
              )}
            </span>
            {!category.isDefault && (
              <button
                onClick={() => handleDelete(category)}
                disabled={deletingId === category.id}
                className="text-sm text-red-600 hover:text-red-700 disabled:opacity-50 dark:text-red-400 dark:hover:text-red-300"
              >
                {deletingId === category.id ? "Deleting…" : "Delete"}
              </button>
            )}
          </li>
        ))}
      </ul>
      <p className="mt-3 text-xs text-zinc-500 dark:text-zinc-400">
        Default categories can&apos;t be deleted. A category still used by a subscription must be
        reassigned first.
      </p>
    </div>
  );
}
