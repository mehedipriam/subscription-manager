export interface User {
  id: number;
  email: string;
  fullName: string;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}

export type BillingCycle = "WEEKLY" | "MONTHLY" | "QUARTERLY" | "YEARLY";
export type SubscriptionStatus = "ACTIVE" | "PAUSED" | "CANCELLED" | "EXPIRED";

export interface Category {
  id: number;
  name: string;
  icon: string | null;
  color: string | null;
  isDefault: boolean;
}

export interface Subscription {
  id: number;
  name: string;
  description: string | null;
  category: Category | null;
  price: number;
  currency: string;
  billingCycle: BillingCycle;
  startDate: string;
  nextBillingDate: string | null;
  status: SubscriptionStatus;
  isTrial: boolean;
  trialEndDate: string | null;
  cancelUrl: string | null;
  cancellationInstructions: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpcomingPayment {
  subscriptionId: number;
  name: string;
  nextBillingDate: string;
  price: number;
  currency: string;
  category: Category | null;
}

export interface CategorySpend {
  categoryId: number | null;
  categoryName: string;
  icon: string | null;
  color: string | null;
  monthlyAmount: number;
  percentage: number;
}

export interface ActivityItem {
  type: "SUBSCRIPTION_ADDED" | "PAYMENT_RECORDED";
  timestamp: string;
  subscriptionId: number;
  subscriptionName: string;
  amount: number | null;
  currency: string | null;
}

export interface PriceChange {
  subscriptionId: number;
  subscriptionName: string;
  oldPrice: number;
  newPrice: number;
  percentageChange: number;
  currency: string;
  changedAt: string;
}

export interface DashboardSummary {
  totalMonthlySpend: number;
  totalYearlySpend: number;
  activeSubscriptionCount: number;
  upcomingRenewalsCount: number;
  upcomingPayments: UpcomingPayment[];
  categoryBreakdown: CategorySpend[];
  recentActivity: ActivityItem[];
  recentPriceChanges: PriceChange[];
}

export interface MonthlySpendPoint {
  month: string;
  totalSpend: number;
}

export interface TopSubscription {
  subscriptionId: number;
  name: string;
  category: Category | null;
  normalizedMonthlyCost: number;
  currency: string;
}

export interface AnalyticsSummary {
  monthlyTrend: MonthlySpendPoint[];
  yearlyTotal: number;
  categoryBreakdown: CategorySpend[];
  mostExpensive: TopSubscription[];
}
