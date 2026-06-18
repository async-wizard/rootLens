export interface IncidentSummary {
  incidentId: string;
  status: string;
  severity: string;
  servicesImpacted: string[];
  createdAt: number;
  updatedAt: number;
  hasAnalysis: boolean;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface IncidentEvent {
  service: string;
  severity: string;
  traceId: string;
  message: string;
  originalTimestamp: number;
  receivedAt: number;
}

export interface Analysis {
  summary: string;
  probableCause: string;
  remediation: string;
  model: string;
  analysisTimestamp: number;
  success: boolean;
  rawResponse?: string;
}

export interface IncidentDetail {
  incidentId: string;
  status: string;
  severity: string;
  servicesImpacted: string[];
  traceIds: string[];
  createdAt: number;
  updatedAt: number;
  events: IncidentEvent[];
  analysis: Analysis | null;
}

export interface ServiceHealth {
  name: string;
  activeIncidents: number;
  highestSeverity: string;
  lastEventAt: number;
}
