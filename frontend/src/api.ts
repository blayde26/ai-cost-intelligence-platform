export type SpendOverview = {
  totalSpend: number;
  totalTokens: number;
  totalRequests: number;
};

export type SpendByStory = {
  storyKey: string | null;
  storyName: string | null;
  storySummary?: string | null;
  epicKey: string | null;
  teamKey: string | null;
  totalCost: number;
  requestCount: number;
  totalTokens: number;
  estimatedCostUsd?: number;
};

export type SpendByEpic = {
  epicKey: string;
  epicName: string | null;
  epicSummary?: string | null;
  teamKey: string | null;
  totalCost: number;
  requestCount: number;
  totalTokens: number;
  storyCount: number;
  estimatedCostUsd?: number;
};

export type SpendByTeam = {
  teamKey: string;
  totalCost: number;
  requestCount: number;
  totalTokens: number;
  epicCount: number;
  storyCount: number;
  estimatedCostUsd?: number;
};

export type AttributionCoverage = {
  totalCost: number;
  attributedCost: number;
  unattributedCost: number;
  coveragePercent: number;
  eventCount: number;
  validEventCount: number;
  invalidEventCount: number;
};

export type PotentialWaste = {
  cancelledStorySpend: number;
  operationalSpend: number;
  unknownAttributionSpend: number;
  failedRequestSpend: number;
};

export type AllocationBucket = {
  category: string;
  totalCost: number;
  percent: number;
};

export type SpendAllocation = {
  totalCost: number;
  buckets: AllocationBucket[];
};

export type UsageEvent = {
  id: string;
  provider: string;
  model: string;
  storyKey: string | null;
  epicKey: string | null;
  teamKey: string;
  userKey: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  estimatedCostUsd: number;
  latencyMs: number;
  requestTimestamp: string;
  environment: string;
  workType: string;
  requestStatus: string;
  attributionStatus: string;
  requestHash: string;
  captureSource: string;
  captureProvider: string;
  captureMethod: string;
  captureConfidence: string;
  attributionSource: string;
  attributionConfidence: string;
  inferredStoryKey: string | null;
  inferenceReason: string | null;
  repository: string | null;
  branch: string | null;
  commitHash: string | null;
  initiativeKey: string | null;
  initiativeName: string | null;
  attributionCorrected: boolean;
  correctedTimestamp: string | null;
  correctedBy: string | null;
};

export type SetupHealthStatus = 'READY' | 'WARNING' | 'NOT_CONFIGURED' | 'ERROR';

export type SetupHealthComponent = {
  key: string;
  label: string;
  status: SetupHealthStatus;
  message: string;
};

export type SetupHealthReport = {
  overallStatus: SetupHealthStatus;
  components: SetupHealthComponent[];
};

export type PilotReadinessCheck = {
  key: string;
  label: string;
  status: SetupHealthStatus;
  message: string;
};

export type PilotReadinessReport = {
  status: SetupHealthStatus;
  score: number;
  summary: string;
  checks: PilotReadinessCheck[];
  recommendedActions: string[];
};

export type JiraConnectionTestResult = {
  status: SetupHealthStatus;
  configured: boolean;
  reachable: boolean;
  issuesReadable: boolean;
  issuesFetched: number;
  sampleIssueKey: string | null;
  message: string;
};

export type SourceControlRepositoryDiagnostic = {
  repository: string;
  owner: string | null;
  teamKey: string | null;
  configured: boolean;
  metricsAvailable: boolean;
  prCount: number | null;
  commitCount: number | null;
  reviewCount: number | null;
  commentCount: number | null;
  averageMergeTimeHours: number | null;
  averageReviewTimeHours: number | null;
};

export type SourceControlMetricsCacheState = {
  enabled: boolean;
  populated: boolean;
  lastLoadedAt: string | null;
  expiresAt: string | null;
  ttlSeconds: number;
};

export type SourceControlDiagnosticsReport = {
  provider: string;
  configured: boolean;
  tokenPresent: boolean;
  configuredRepositoryCount: number;
  metricsAvailableCount: number;
  cache: SourceControlMetricsCacheState;
  repositories: SourceControlRepositoryDiagnostic[];
  message: string;
};

export type UsageImportError = {
  rowNumber: number;
  message: string;
};

export type UsageImportResult = {
  importedCount: number;
  skippedCount: number;
  errors: UsageImportError[];
};

export type TeamAnalyticsSnapshot = {
  teamKey: string;
  aiSpend: number;
  aiRequestCount: number;
  storyCount: number;
  completedStoryCount: number;
  cancelledStoryCount: number;
  storyCompletionRate: number;
  cancelledStoryRate: number;
  capitalizedWorkRate: number;
  operationalWorkRate: number;
  researchWorkRate: number;
  prCount: number | null;
  averageMergeTimeHours: number | null;
  averageReviewTimeHours: number | null;
  outcomeDataStatus: string;
  interpretation: string;
};

export type RepositoryAnalyticsSnapshot = {
  repository: string;
  owner: string | null;
  teamKey: string | null;
  aiSpend: number;
  aiRequestCount: number;
  totalTokens: number;
  attributedEventCount: number;
  unattributedEventCount: number;
  attributionCoveragePercent: number;
  prCount: number | null;
  commitCount: number | null;
  reviewCount: number | null;
  commentCount: number | null;
  averageMergeTimeHours: number | null;
  averageReviewTimeHours: number | null;
  outcomeDataStatus: string;
  interpretation: string;
};

export type OutcomeCorrelationSignal = {
  subjectType: string;
  subjectKey: string;
  aiSpend: number;
  outcomeMetric: string;
  outcomeValue: number;
  signal: string;
  interpretation: string;
};

export type OutcomeCorrelationReport = {
  totalAiSpend: number;
  teamCount: number;
  aiActiveTeamCount: number;
  repositoryCount: number;
  aiActiveRepositoryCount: number;
  repositoriesWithOutcomeMetrics: number;
  averageStoryCompletionRateForAiActiveTeams: number;
  averageMergeTimeHoursForAiActiveRepositories: number | null;
  signals: OutcomeCorrelationSignal[];
  interpretation: string;
};

export type ProviderUtilizationSnapshot = {
  provider: string;
  totalCost: number;
  totalTokens: number;
  requestCount: number;
  modelCount: number;
  costPercent: number;
};

export type ModelUtilizationSnapshot = {
  provider: string;
  model: string;
  totalCost: number;
  totalTokens: number;
  requestCount: number;
  teamCount: number;
  workTypeCount: number;
  costPercent: number;
};

export type AttributionCorrectionRequest = {
  storyKey?: string | null;
  epicKey?: string | null;
  teamKey?: string | null;
  workType?: string | null;
  correctedBy: string;
  note?: string | null;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

export function apiUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(apiUrl(path));
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

async function patchJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(apiUrl(path), {
    method: 'PATCH',
    headers: {
      'content-type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

async function postText<T>(path: string, body: string, contentType: string): Promise<T> {
  const response = await fetch(apiUrl(path), {
    method: 'POST',
    headers: {
      'content-type': contentType
    },
    body
  });
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

export const api = {
  overview: () => getJson<SpendOverview>('/api/v1/reports/overview'),
  coverage: () => getJson<AttributionCoverage>('/api/v1/reports/attribution-coverage'),
  allocation: () => getJson<SpendAllocation>('/api/v1/reports/allocation'),
  potentialWaste: () => getJson<PotentialWaste>('/api/v1/reports/potential-waste'),
  spendByStory: () => getJson<SpendByStory[]>('/api/v1/reports/spend/by-story'),
  spendByEpic: () => getJson<SpendByEpic[]>('/api/v1/reports/spend/by-epic'),
  spendByTeam: () => getJson<SpendByTeam[]>('/api/v1/reports/spend/by-team'),
  setupHealth: () => getJson<SetupHealthReport>('/api/v1/setup/health'),
  pilotReadiness: () => getJson<PilotReadinessReport>('/api/v1/setup/pilot-readiness'),
  jiraConnectionTest: () => getJson<JiraConnectionTestResult>('/api/v1/jira/connection-test'),
  sourceControlDiagnostics: () => getJson<SourceControlDiagnosticsReport>('/api/v1/source-control/diagnostics'),
  importUsageCsv: (csv: string) => postText<UsageImportResult>('/api/v1/usage/imports/csv', csv, 'text/csv'),
  previewUsageCsv: (csv: string) => postText<UsageImportResult>('/api/v1/usage/imports/csv/preview', csv, 'text/csv'),
  teamEffectiveness: () => getJson<TeamAnalyticsSnapshot[]>('/api/v1/analytics/team-effectiveness'),
  repositoryAnalytics: () => getJson<RepositoryAnalyticsSnapshot[]>('/api/v1/analytics/repositories'),
  outcomeCorrelations: () => getJson<OutcomeCorrelationReport>('/api/v1/analytics/correlations'),
  providerUtilization: () => getJson<ProviderUtilizationSnapshot[]>('/api/v1/analytics/model-utilization/providers'),
  modelUtilization: () => getJson<ModelUtilizationSnapshot[]>('/api/v1/analytics/model-utilization/models'),
  usageEvents: () => getJson<UsageEvent[]>('/api/v1/usage/events?limit=100'),
  usageEvent: (id: string) => getJson<UsageEvent>(`/api/v1/usage/events/${id}`),
  correctAttribution: (id: string, request: AttributionCorrectionRequest) =>
    patchJson<UsageEvent>(`/api/v1/usage/events/${id}/attribution`, request)
};
