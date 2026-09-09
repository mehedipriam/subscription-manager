import { API_V1_URL } from "./api";
import { getAccessToken, getRefreshToken, setAccessToken, setRefreshToken, clearTokens } from "./tokenStore";

export class ApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;

  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

interface RefreshResponse {
  accessToken: string;
  refreshToken: string;
}

let refreshInFlight: Promise<boolean> | null = null;

async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;

  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const res = await fetch(`${API_V1_URL}/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });
        if (!res.ok) {
          clearTokens();
          return false;
        }
        const data: RefreshResponse = await res.json();
        setAccessToken(data.accessToken);
        setRefreshToken(data.refreshToken);
        return true;
      } catch {
        clearTokens();
        return false;
      } finally {
        refreshInFlight = null;
      }
    })();
  }
  return refreshInFlight;
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  allowRetry = true
): Promise<T> {
  const headers = new Headers(options.headers);
  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }
  const token = getAccessToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const res = await fetch(`${API_V1_URL}${path}`, { ...options, headers });

  if (res.status === 401 && allowRetry) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return apiFetch<T>(path, options, false);
    }
  }

  if (!res.ok) {
    let body: { message?: string; fieldErrors?: Record<string, string> } | null = null;
    try {
      body = await res.json();
    } catch {
      // no JSON body
    }
    throw new ApiError(
      res.status,
      body?.message ?? `Request failed with status ${res.status}`,
      body?.fieldErrors
    );
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return res.json();
}
