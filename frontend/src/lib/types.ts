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

export interface BudgetStatus {
  periodMonth: string;
  budgetAmount: number | null;
  projectedSpend: number;
  remaining: number | null;
  percentageUsed: number | null;
  exceeded: boolean;
}

export interface UsageLogEntry {
  id: number;
  usedAt: string;
}

export interface UsageInsight {
  subscriptionId: number;
  subscriptionName: string;
  monthlyCost: number;
  currency: string;
  lastUsedAt: string | null;
  daysSinceLastUsed: number;
  usageCount: number;
  rarelyUsed: boolean;
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
  budgetStatus: BudgetStatus;
  usageRecommendations: UsageInsight[];
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

export type NotificationType =
  | "RENEWAL_UPCOMING"
  | "TRIAL_ENDING"
  | "PRICE_CHANGE"
  | "BUDGET_EXCEEDED"
  | "SUBSCRIPTION_EXPIRED";

export interface AppNotification {
  id: number;
  type: NotificationType;
  message: string;
  subscriptionId: number | null;
  subscriptionName: string | null;
  isRead: boolean;
  createdAt: string;
}

export interface SavingsItem {
  subscriptionId: number;
  name: string;
  monthlyCost: number;
  yearlyCost: number;
  currency: string;
}

export interface CancellationSavings {
  monthlySavings: number;
  yearlySavings: number;
  subscriptions: SavingsItem[];
}
