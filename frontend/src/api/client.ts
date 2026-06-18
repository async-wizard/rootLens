import type { IncidentDetail, IncidentSummary, PagedResponse, ServiceHealth } from '@/types';

const BASE_URL = 'http://localhost:8086';

export interface IncidentFilters {
  severity?: string;
  status?: string;
  service?: string;
}

export async function fetchIncidents(
  page = 0,
  size = 20,
  filters: IncidentFilters = {}
): Promise<PagedResponse<IncidentSummary>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (filters.severity) params.set('severity', filters.severity);
  if (filters.status) params.set('status', filters.status);
  if (filters.service) params.set('service', filters.service);
  const res = await fetch(`${BASE_URL}/incidents?${params.toString()}`);
  if (!res.ok) throw new Error('Failed to fetch incidents');
  return res.json() as Promise<PagedResponse<IncidentSummary>>;
}

export async function fetchIncidentDetail(id: string): Promise<IncidentDetail> {
  const res = await fetch(`${BASE_URL}/incidents/${id}`);
  if (!res.ok) throw new Error(`Incident ${id} not found`);
  return res.json() as Promise<IncidentDetail>;
}

export async function fetchServices(): Promise<{ services: ServiceHealth[] }> {
  const res = await fetch(`${BASE_URL}/services`);
  if (!res.ok) throw new Error('Failed to fetch services');
  return res.json() as Promise<{ services: ServiceHealth[] }>;
}

export async function updateIncidentStatus(id: string, status: string): Promise<IncidentSummary> {
  const res = await fetch(`${BASE_URL}/incidents/${id}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(body.message ?? `Failed to update status to ${status}`);
  }
  return res.json() as Promise<IncidentSummary>;
}

export function createIncidentStream(onUpdate: () => void): EventSource {
  const es = new EventSource(`${BASE_URL}/incidents/stream`);
  es.addEventListener('incident-update', () => onUpdate());
  es.onerror = () => es.close();
  return es;
}
