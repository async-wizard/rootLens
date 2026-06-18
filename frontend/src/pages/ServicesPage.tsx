import { useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchServices, createIncidentStream } from '@/api/client';
import { Navbar } from '@/components/Navbar';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { SeverityBadge } from '@/components/SeverityBadge';
import { Skeleton } from '@/components/ui/skeleton';
import { CheckCircle2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { ServiceHealth } from '@/types';

function relativeTime(epochMs: number): string {
  const secs = Math.floor((Date.now() - epochMs) / 1000);
  if (secs < 60) return `${secs}s ago`;
  if (secs < 3600) return `${Math.floor(secs / 60)}m ago`;
  return `${Math.floor(secs / 3600)}h ago`;
}

function borderColor(severity: string): string {
  if (severity === 'HIGH') return 'border-t-red-500';
  if (severity === 'MEDIUM') return 'border-t-orange-400';
  if (severity === 'LOW') return 'border-t-slate-400';
  return 'border-t-green-500';
}

function ServiceCard({ service }: { service: ServiceHealth }) {
  const healthy = service.activeIncidents === 0;
  return (
    <Card className={cn('border-t-4', borderColor(service.highestSeverity))}>
      <CardHeader className="pb-2">
        <CardTitle className="text-base flex items-center justify-between">
          <span>{service.name}</span>
          {healthy && <CheckCircle2 className="h-4 w-4 text-green-500" />}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-2 text-sm">
        <div className="flex items-center justify-between">
          <span className="text-muted-foreground">Active incidents</span>
          <span className={cn('font-semibold', service.activeIncidents > 0 && 'text-destructive')}>
            {service.activeIncidents}
          </span>
        </div>
        {!healthy && (
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">Highest severity</span>
            <SeverityBadge severity={service.highestSeverity} />
          </div>
        )}
        {service.lastEventAt > 0 && (
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">Last event</span>
            <span className="text-muted-foreground">{relativeTime(service.lastEventAt)}</span>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export function ServicesPage() {
  const queryClient = useQueryClient();

  const { data, isLoading, dataUpdatedAt } = useQuery({
    queryKey: ['services'],
    queryFn: fetchServices,
  });

  useEffect(() => {
    const es = createIncidentStream(() => {
      void queryClient.invalidateQueries({ queryKey: ['services'] });
    });
    return () => es.close();
  }, [queryClient]);

  const lastUpdated = dataUpdatedAt ? new Date(dataUpdatedAt) : null;
  const allHealthy = data?.services.every((s) => s.activeIncidents === 0);

  return (
    <div className="min-h-screen bg-background">
      <Navbar lastUpdated={lastUpdated} />

      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-semibold">Services</h1>
          {allHealthy && (
            <span className="text-sm text-green-600 flex items-center gap-1">
              <CheckCircle2 className="h-4 w-4" /> All services healthy
            </span>
          )}
        </div>

        {isLoading && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-36 w-full rounded-lg" />
            ))}
          </div>
        )}

        {data && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {data.services.map((service) => (
              <ServiceCard key={service.name} service={service} />
            ))}
          </div>
        )}

        {!isLoading && data?.services.length === 0 && (
          <div className="text-center text-muted-foreground py-16">No services found</div>
        )}
      </main>
    </div>
  );
}
