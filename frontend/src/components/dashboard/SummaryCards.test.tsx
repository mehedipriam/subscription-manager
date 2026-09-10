import { describe, expect, test } from "vitest";
import { render, screen } from "@testing-library/react";
import { SummaryCards } from "./SummaryCards";
import type { DashboardSummary } from "@/lib/types";

const summary: DashboardSummary = {
  totalMonthlySpend: 42.5,
  totalYearlySpend: 510,
  activeSubscriptionCount: 3,
  upcomingRenewalsCount: 1,
  upcomingPayments: [],
  categoryBreakdown: [],
  recentActivity: [],
  recentPriceChanges: [],
  budgetStatus: {
    periodMonth: "2026-09-01",
    budgetAmount: null,
    projectedSpend: 42.5,
    remaining: null,
    percentageUsed: null,
    exceeded: false,
  },
  usageRecommendations: [],
};

describe("SummaryCards", () => {
  test("renders a formatted card for each summary figure", () => {
    render(<SummaryCards summary={summary} />);

    expect(screen.getByText("Monthly spend")).toBeInTheDocument();
    expect(screen.getByText("$42.50")).toBeInTheDocument();
    expect(screen.getByText("Yearly spend")).toBeInTheDocument();
    expect(screen.getByText("$510.00")).toBeInTheDocument();
    expect(screen.getByText("Active subscriptions")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getByText("Renewing within 7 days")).toBeInTheDocument();
    expect(screen.getByText("1")).toBeInTheDocument();
  });
});
