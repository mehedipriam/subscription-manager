import "@testing-library/jest-dom/vitest";
import { afterEach, vi } from "vitest";
import { cleanup } from "@testing-library/react";

// `globals` isn't enabled in vitest.config.mts, so Testing Library can't
// auto-detect `afterEach` to register its DOM cleanup — do it explicitly,
// otherwise each test file's later tests render on top of earlier ones.
afterEach(cleanup);

// jsdom doesn't implement matchMedia; ThemeContext (and anything else probing
// prefers-color-scheme) needs at least a no-op stub to avoid crashing.
Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});
