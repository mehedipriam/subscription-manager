"use client";

import { useEffect, useState } from "react";
import { API_V1_URL } from "@/lib/api";

type HealthResponse = {
  status: string;
  service: string;
  timestamp: string;
};

type CheckState =
  | { phase: "loading" }
  | { phase: "success"; data: HealthResponse }
  | { phase: "error"; message: string };

export default function Home() {
  const [check, setCheck] = useState<CheckState>({ phase: "loading" });

  useEffect(() => {
    let cancelled = false;

    fetch(`${API_V1_URL}/health`)
      .then((res) => {
        if (!res.ok) throw new Error(`Backend responded with ${res.status}`);
        return res.json() as Promise<HealthResponse>;
      })
      .then((data) => {
        if (!cancelled) setCheck({ phase: "success", data });
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setCheck({
            phase: "error",
            message: err instanceof Error ? err.message : "Unknown error",
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="flex flex-1 items-center justify-center bg-zinc-50 font-sans dark:bg-black">
      <main className="flex w-full max-w-md flex-col items-center gap-6 rounded-xl border border-black/[.08] bg-white p-10 text-center dark:border-white/[.145] dark:bg-zinc-900">
        <h1 className="text-2xl font-semibold tracking-tight text-black dark:text-zinc-50">
          Subscription Manager
        </h1>
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Phase 1 &mdash; frontend / backend / database wiring check
        </p>

        <div className="w-full rounded-lg border border-black/[.08] p-4 text-left text-sm dark:border-white/[.145]">
          {check.phase === "loading" && (
            <p className="text-zinc-500 dark:text-zinc-400">
              Checking backend connection&hellip;
            </p>
          )}
          {check.phase === "success" && (
            <div className="flex flex-col gap-1">
              <p className="font-medium text-green-600 dark:text-green-400">
                ✓ Backend is reachable
              </p>
              <p className="text-zinc-500 dark:text-zinc-400">
                {check.data.service} &middot; {check.data.status}
              </p>
              <p className="text-xs text-zinc-400 dark:text-zinc-500">
                {check.data.timestamp}
              </p>
            </div>
          )}
          {check.phase === "error" && (
            <div className="flex flex-col gap-1">
              <p className="font-medium text-red-600 dark:text-red-400">
                ✗ Could not reach backend
              </p>
              <p className="text-zinc-500 dark:text-zinc-400">
                {check.message}
              </p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
