"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { NotificationBell } from "@/components/notifications/NotificationBell";
import { ThemeToggle } from "@/components/theme/ThemeToggle";

const NAV_LINKS = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/subscriptions", label: "Subscriptions" },
  { href: "/analytics", label: "Analytics" },
  { href: "/usage", label: "Usage" },
  { href: "/calculator", label: "What if I cancel?" },
  { href: "/export", label: "Export" },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { user, status, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  const [menuPathname, setMenuPathname] = useState(pathname);

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/login");
    }
  }, [status, router]);

  // Close the mobile menu on navigation without an Effect: adjust state
  // during render when `pathname` changes (see react.dev "adjusting state
  // when a prop changes" — avoids an extra render from a set-in-effect).
  if (pathname !== menuPathname) {
    setMenuPathname(pathname);
    setMenuOpen(false);
  }

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
      <header className="border-b border-black/[.08] bg-white dark:border-white/[.145] dark:bg-zinc-900">
        <div className="flex items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <div className="flex items-center gap-6">
            <span className="text-sm font-semibold text-black dark:text-zinc-50">
              Subscription Manager
            </span>
            <nav className="hidden items-center gap-1 md:flex">
              {NAV_LINKS.map((link) => {
                const active = pathname.startsWith(link.href);
                return (
                  <Link
                    key={link.href}
                    href={link.href}
                    className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                      active
                        ? "bg-black/[.06] text-black dark:bg-white/[.10] dark:text-zinc-50"
                        : "text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
                    }`}
                  >
                    {link.label}
                  </Link>
                );
              })}
            </nav>
          </div>

          <div className="hidden items-center gap-4 md:flex">
            <ThemeToggle />
            <NotificationBell />
            <span className="text-sm text-zinc-500 dark:text-zinc-400">{user?.email}</span>
            <button
              onClick={handleLogout}
              className="rounded-md border border-black/[.12] px-3 py-1.5 text-sm font-medium text-black transition-colors hover:bg-black/[.04] dark:border-white/[.16] dark:text-zinc-50 dark:hover:bg-white/[.06]"
            >
              Log out
            </button>
          </div>

          <div className="flex items-center gap-2 md:hidden">
            <NotificationBell />
            <button
              onClick={() => setMenuOpen((prev) => !prev)}
              aria-label="Toggle menu"
              aria-expanded={menuOpen}
              className="rounded-md p-1.5 text-zinc-500 transition-colors hover:bg-black/[.04] hover:text-black dark:text-zinc-400 dark:hover:bg-white/[.06] dark:hover:text-zinc-50"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5">
                {menuOpen ? <path d="M18 6 6 18M6 6l12 12" /> : <path d="M3 6h18M3 12h18M3 18h18" />}
              </svg>
            </button>
          </div>
        </div>

        {menuOpen && (
          <div className="border-t border-black/[.08] px-4 py-3 dark:border-white/[.145] md:hidden">
            <nav className="flex flex-col gap-1">
              {NAV_LINKS.map((link) => {
                const active = pathname.startsWith(link.href);
                return (
                  <Link
                    key={link.href}
                    href={link.href}
                    className={`rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                      active
                        ? "bg-black/[.06] text-black dark:bg-white/[.10] dark:text-zinc-50"
                        : "text-zinc-500 hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
                    }`}
                  >
                    {link.label}
                  </Link>
                );
              })}
            </nav>
            <div className="mt-3 flex items-center justify-between gap-3 border-t border-black/[.08] pt-3 dark:border-white/[.145]">
              <span className="truncate text-sm text-zinc-500 dark:text-zinc-400">{user?.email}</span>
              <ThemeToggle />
            </div>
            <button
              onClick={handleLogout}
              className="mt-3 w-full rounded-md border border-black/[.12] px-3 py-2 text-sm font-medium text-black transition-colors hover:bg-black/[.04] dark:border-white/[.16] dark:text-zinc-50 dark:hover:bg-white/[.06]"
            >
              Log out
            </button>
          </div>
        )}
      </header>
      <main className="flex flex-1 flex-col">{children}</main>
    </div>
  );
}
