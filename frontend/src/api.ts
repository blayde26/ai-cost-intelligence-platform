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

export type AttributionCorrectionRequest = {
  storyKey?: string | null;
  epicKey?: string | null;
  teamKey?: string | null;
  workType?: string | null;
  correctedBy: string;
  note?: string | null;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`);
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

async function patchJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
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

export const api = {
  overview: () => getJson<SpendOverview>('/api/v1/reports/overview'),
  coverage: () => getJson<AttributionCoverage>('/api/v1/reports/attribution-coverage'),
  allocation: () => getJson<SpendAllocation>('/api/v1/reports/allocation'),
  potentialWaste: () => getJson<PotentialWaste>('/api/v1/reports/potential-waste'),
  spendByStory: () => getJson<SpendByStory[]>('/api/v1/reports/spend/by-story'),
  spendByEpic: () => getJson<SpendByEpic[]>('/api/v1/reports/spend/by-epic'),
  spendByTeam: () => getJson<SpendByTeam[]>('/api/v1/reports/spend/by-team'),
  usageEvents: () => getJson<UsageEvent[]>('/api/v1/usage/events?limit=100'),
  usageEvent: (id: string) => getJson<UsageEvent>(`/api/v1/usage/events/${id}`),
  correctAttribution: (id: string, request: AttributionCorrectionRequest) =>
    patchJson<UsageEvent>(`/api/v1/usage/events/${id}/attribution`, request)
};
