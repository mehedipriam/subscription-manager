"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { apiFetch } from "@/lib/apiClient";
import { clearTokens, getRefreshToken, setAccessToken, setRefreshToken } from "@/lib/tokenStore";
import type { AuthResponse, User } from "@/lib/types";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: User | null;
  status: AuthStatus;
  login: (email: string, password: string) => Promise<void>;
  register: (fullName: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  useEffect(() => {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      setStatus("unauthenticated");
      return;
    }

    apiFetch<User>("/auth/me")
      .then((me) => {
        setUser(me);
        setStatus("authenticated");
      })
      .catch(() => {
        clearTokens();
        setUser(null);
        setStatus("unauthenticated");
      });
  }, []);

  const applyAuthResponse = useCallback((data: AuthResponse) => {
    setAccessToken(data.accessToken);
    setRefreshToken(data.refreshToken);
    setUser(data.user);
    setStatus("authenticated");
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const data = await apiFetch<AuthResponse>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      applyAuthResponse(data);
    },
    [applyAuthResponse]
  );

  const register = useCallback(
    async (fullName: string, email: string, password: string) => {
      const data = await apiFetch<AuthResponse>("/auth/register", {
        method: "POST",
        body: JSON.stringify({ fullName, email, password }),
      });
      applyAuthResponse(data);
    },
    [applyAuthResponse]
  );

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      try {
        await apiFetch("/auth/logout", {
          method: "POST",
          body: JSON.stringify({ refreshToken }),
        });
      } catch {
        // best-effort: clear local state even if the server call fails
      }
    }
    clearTokens();
    setUser(null);
    setStatus("unauthenticated");
  }, []);

  return (
    <AuthContext.Provider value={{ user, status, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
