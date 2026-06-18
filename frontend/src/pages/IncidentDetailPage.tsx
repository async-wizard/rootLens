import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, AlertTriangle, Wrench, Brain } from 'lucide-react';
import { fetchIncidentDetail, createIncidentStream, updateIncidentStatus } from '@/api/client';
import { SeverityBadge } from '@/components/SeverityBadge';
import { StatusBadge } from '@/components/StatusBadge';
import { Navbar } from '@/components/Navbar';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';

function formatTs(epochMs: number): string {
  return new Date(epochMs).toLocaleString();
}

function relativeTime(epochMs: number): string {
  const secs = Math.floor((Date.now() - epochMs) / 1000);
  if (secs < 60) return `${secs}s ago`;
  if (secs < 3600) return `${Math.floor(secs / 60)}m ago`;
  return `${Math.floor(secs / 3600)}h ago`;
}

export function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [transitioning, setTransitioning] = useState(false);
  const [transitionError, setTransitionError] = useState<string | null>(null);

  const { data, isLoading, dataUpdatedAt } = useQuery({
    queryKey: ['incident', id],
    queryFn: () => fetchIncidentDetail(id!),
    enabled: !!id,
  });

  useEffect(() => {
    const es = createIncidentStream(() => {
      void queryClient.invalidateQueries({ queryKey: ['incident', id] });
    });
    return () => es.close();
  }, [queryClient, id]);

  async function handleTransition(status: string) {
    if (!id) return;
    setTransitioning(true);
    setTransitionError(null);
    try {
      await updateIncidentStatus(id, status);
      await queryClient.invalidateQueries({ queryKey: ['incident', id] });
      await queryClient.invalidateQueries({ queryKey: ['incidents'] });
    } catch (err) {
      setTransitionError(err instanceof Error ? err.message : 'Transition failed');
    } finally {
      setTransitioning(false);
    }
  }

  const lastUpdated = dataUpdatedAt ? new Date(dataUpdatedAt) : null;

  return (
    <div className="min-h-screen bg-background">
      <Navbar lastUpdated={lastUpdated} />

      <main className="max-w-5xl mx-auto px-6 py-8">
        <Button variant="ghost" size="sm" className="mb-6" onClick={() => navigate('/')}>
          <ArrowLeft className="h-4 w-4 mr-1" /> Back
        </Button>

        {isLoading && (
          <div className="space-y-4">
            <Skeleton className="h-8 w-64" />
            <Skeleton className="h-4 w-48" />
            <Skeleton className="h-48 w-full" />
          </div>
        )}

        {data && (
          <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-2xl font-semibold font-mono">{data.incidentId}</h1>
              <SeverityBadge severity={data.severity} />
              <StatusBadge status={data.status} />
              {data.status === 'OPEN' && (
                <Button
                  size="sm"
                  variant="outline"
                  disabled={transitioning}
                  onClick={() => handleTransition('INVESTIGATING')}
                >
                  Start Investigation
                </Button>
              )}
              {data.status === 'INVESTIGATING' && (
                <Button
                  size="sm"
                  variant="outline"
                  disabled={transitioning}
                  onClick={() => handleTransition('RESOLVED')}
                >
                  Mark Resolved
                </Button>
              )}
              {transitionError && (
                <span className="text-xs text-destructive">{transitionError}</span>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4 text-sm text-muted-foreground">
              <span>Created: {formatTs(data.createdAt)}</span>
              <span>Updated: {formatTs(data.updatedAt)}</span>
            </div>

            {/* Services */}
            <div>
              <p className="text-sm font-medium mb-2">Services Impacted</p>
              <div className="flex flex-wrap gap-2">
                {data.servicesImpacted.map((s) => (
                  <Badge key={s} variant="secondary">{s}</Badge>
                ))}
              </div>
            </div>

            {/* Trace IDs */}
            {data.traceIds.length > 0 && (
              <div>
                <p className="text-sm font-medium mb-2">Trace IDs</p>
                <div className="flex flex-wrap gap-2">
                  {data.traceIds.map((t) => (
                    <code key={t} className="text-xs bg-muted px-2 py-1 rounded font-mono">
                      {t.slice(0, 16)}…
                    </code>
                  ))}
                </div>
              </div>
            )}

            <Separator />

            {/* Events timeline */}
            <div>
              <h2 className="text-lg font-semibold mb-4">Events ({data.events.length})</h2>
              <div className="space-y-3">
                {data.events
                  .slice()
                  .sort((a, b) => a.originalTimestamp - b.originalTimestamp)
                  .map((event, i) => (
                    <Card key={i} className="border-l-4 border-l-muted-foreground/30">
                      <CardContent className="py-3 px-4">
                        <div className="flex items-center justify-between mb-1">
                          <div className="flex items-center gap-2">
                            <Badge variant="outline" className="text-xs">{event.service}</Badge>
                            <SeverityBadge severity={event.severity} />
                          </div>
                          <span className="text-xs text-muted-foreground">
                            {relativeTime(event.originalTimestamp)}
                          </span>
                        </div>
                        <p className="text-sm">{event.message}</p>
                        <code className="text-xs text-muted-foreground font-mono">
                          trace: {event.traceId.slice(0, 16)}…
                        </code>
                      </CardContent>
                    </Card>
                  ))}
              </div>
            </div>

            <Separator />

            {/* AI Analysis */}
            <div>
              <h2 className="text-lg font-semibold mb-4">AI Analysis</h2>

              {data.analysis === null && (
                <Card>
                  <CardContent className="py-6">
                    <div className="space-y-3">
                      <Skeleton className="h-4 w-full" />
                      <Skeleton className="h-4 w-3/4" />
                      <p className="text-sm text-muted-foreground text-center pt-2">Analysis pending…</p>
                    </div>
                  </CardContent>
                </Card>
              )}

              {data.analysis && !data.analysis.success && (
                <Card className="border-destructive">
                  <CardContent className="py-6">
                    <p className="text-sm text-destructive">Analysis failed.</p>
                    {data.analysis.rawResponse && (
                      <pre className="mt-3 text-xs bg-muted p-3 rounded overflow-auto max-h-48">
                        {data.analysis.rawResponse}
                      </pre>
                    )}
                  </CardContent>
                </Card>
              )}

              {data.analysis && data.analysis.success && (
                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-sm text-muted-foreground font-normal">
                      Powered by <span className="font-medium text-foreground">{data.analysis.model}</span>
                      {' · '}{formatTs(data.analysis.analysisTimestamp)}
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-5">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <Brain className="h-4 w-4 text-blue-500" />
                        <p className="text-sm font-semibold">Summary</p>
                      </div>
                      <p className="text-sm text-muted-foreground">{data.analysis.summary}</p>
                    </div>
                    <Separator />
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <AlertTriangle className="h-4 w-4 text-orange-500" />
                        <p className="text-sm font-semibold">Probable Cause</p>
                      </div>
                      <p className="text-sm text-muted-foreground">{data.analysis.probableCause}</p>
                    </div>
                    <Separator />
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <Wrench className="h-4 w-4 text-green-500" />
                        <p className="text-sm font-semibold">Remediation</p>
                      </div>
                      <p className="text-sm text-muted-foreground">{data.analysis.remediation}</p>
                    </div>
                  </CardContent>
                </Card>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
