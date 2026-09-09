"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { user, status, logout } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/login");
    }
  }, [status, router]);

  if (status !== "authenticated") {
    return (
      <div className="flex flex-1 items-center justify-center bg-zinc-50 dark:bg-black">
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Loading…</p>
      </div>
    );
  }

  async function handleLogout() {
    await logout();
    router.replace("/login");
  }

  return (
    <div className="flex min-h-full flex-1 flex-col bg-zinc-50 font-sans dark:bg-black">
      <header className="flex items-center justify-between border-b border-black/[.08] bg-white px-6 py-3 dark:border-white/[.145] dark:bg-zinc-900">
        <span className="text-sm font-semibold text-black dark:text-zinc-50">
          Subscription Manager
        </span>
        <div className="flex items-center gap-4">
          <span className="text-sm text-zinc-500 dark:text-zinc-400">{user?.email}</span>
          <button
            onClick={handleLogout}
            className="rounded-md border border-black/[.12] px-3 py-1.5 text-sm font-medium text-black transition-colors hover:bg-black/[.04] dark:border-white/[.16] dark:text-zinc-50 dark:hover:bg-white/[.06]"
          >
            Log out
          </button>
        </div>
      </header>
      <main className="flex flex-1 flex-col">{children}</main>
    </div>
  );
}
