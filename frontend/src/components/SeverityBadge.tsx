import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

interface Props {
  severity: string;
}

export function SeverityBadge({ severity }: Props) {
  const classes = cn(
    severity === 'HIGH' && 'bg-red-600 text-white hover:bg-red-700',
    severity === 'MEDIUM' && 'bg-orange-500 text-white hover:bg-orange-600',
    severity === 'LOW' && 'bg-slate-400 text-white hover:bg-slate-500',
  );
  return <Badge className={classes}>{severity}</Badge>;
}
