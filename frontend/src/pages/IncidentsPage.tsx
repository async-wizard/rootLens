import { useState, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, Minus, X } from 'lucide-react';
import { fetchIncidents, createIncidentStream, type IncidentFilters } from '@/api/client';
import { SeverityBadge } from '@/components/SeverityBadge';
import { StatusBadge } from '@/components/StatusBadge';
import { Navbar } from '@/components/Navbar';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

function relativeTime(epochMs: number): string {
  const secs = Math.floor((Date.now() - epochMs) / 1000);
  if (secs < 60) return `${secs}s ago`;
  if (secs < 3600) return `${Math.floor(secs / 60)}m ago`;
  if (secs < 86400) return `${Math.floor(secs / 3600)}h ago`;
  return `${Math.floor(secs / 86400)}d ago`;
}

const EMPTY_FILTERS: IncidentFilters = { severity: '', status: '', service: '' };

export function IncidentsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<IncidentFilters>(EMPTY_FILTERS);

  // Reset to page 0 whenever filters change
  useEffect(() => { setPage(0); }, [filters]);

  const { data, isLoading, dataUpdatedAt } = useQuery({
    queryKey: ['incidents', page, filters],
    queryFn: () => fetchIncidents(page, 20, filters),
  });

  useEffect(() => {
    const es = createIncidentStream(() => {
      void queryClient.invalidateQueries({ queryKey: ['incidents'] });
    });
    return () => es.close();
  }, [queryClient]);

  const lastUpdated = dataUpdatedAt ? new Date(dataUpdatedAt) : null;
  const hasActiveFilters = Object.values(filters).some(Boolean);

  const selectClass =
    'h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground ' +
    'focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1';

  return (
    <div className="min-h-screen bg-background">
      <Navbar lastUpdated={lastUpdated} />

      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-semibold">Incidents</h1>
          {data && (
            <span className="text-sm text-muted-foreground">
              {data.totalElements} total
            </span>
          )}
        </div>

        {/* Filter bar */}
        <div className="flex flex-wrap items-center gap-3 mb-4">
          <select
            className={selectClass}
            value={filters.severity ?? ''}
            onChange={(e) => setFilters((f) => ({ ...f, severity: e.target.value }))}
            aria-label="Filter by severity"
          >
            <option value="">All Severities</option>
            <option value="HIGH">HIGH</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="LOW">LOW</option>
          </select>

          <select
            className={selectClass}
            value={filters.status ?? ''}
            onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value }))}
            aria-label="Filter by status"
          >
            <option value="">All Statuses</option>
            <option value="OPEN">OPEN</option>
            <option value="INVESTIGATING">INVESTIGATING</option>
            <option value="RESOLVED">RESOLVED</option>
          </select>

          <input
            type="text"
            className={selectClass + ' w-44'}
            placeholder="Filter by service..."
            value={filters.service ?? ''}
            onChange={(e) => setFilters((f) => ({ ...f, service: e.target.value }))}
            aria-label="Filter by service name"
          />

          {hasActiveFilters && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setFilters(EMPTY_FILTERS)}
              className="gap-1 text-muted-foreground hover:text-foreground"
            >
              <X className="h-3.5 w-3.5" />
              Clear
            </Button>
          )}
        </div>

        <div className="rounded-lg border border-border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Severity</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Services Impacted</TableHead>
                <TableHead>Created</TableHead>
                <TableHead className="text-center">AI</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading &&
                Array.from({ length: 8 }).map((_, i) => (
                  <TableRow key={i}>
                    {Array.from({ length: 6 }).map((__, j) => (
                      <TableCell key={j}>
                        <Skeleton className="h-4 w-full" />
                      </TableCell>
                    ))}
                  </TableRow>
                ))}

              {data?.content.map((incident) => (
                <TableRow
                  key={incident.incidentId}
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => navigate(`/incidents/${incident.incidentId}`)}
                >
                  <TableCell className="font-mono text-sm">{incident.incidentId}</TableCell>
                  <TableCell>
                    <SeverityBadge severity={incident.severity} />
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={incident.status} />
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {incident.servicesImpacted.join(', ')}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {relativeTime(incident.createdAt)}
                  </TableCell>
                  <TableCell className="text-center">
                    {incident.hasAnalysis ? (
                      <CheckCircle className="h-4 w-4 text-green-500 inline" />
                    ) : (
                      <Minus className="h-4 w-4 text-muted-foreground inline" />
                    )}
                  </TableCell>
                </TableRow>
              ))}

              {!isLoading && data?.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground py-12">
                    {hasActiveFilters ? 'No incidents match the current filters.' : 'No incidents found.'}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>

        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between mt-4">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Previous
            </Button>
            <span className="text-sm text-muted-foreground">
              Page {page + 1} of {data.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </main>
    </div>
  );
}
