import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

interface Props {
  status: string;
}

export function StatusBadge({ status }: Props) {
  const classes = cn(
    status === 'OPEN' && 'bg-yellow-500 text-white hover:bg-yellow-600',
    status === 'INVESTIGATING' && 'bg-blue-500 text-white hover:bg-blue-600',
    status === 'RESOLVED' && 'bg-green-600 text-white hover:bg-green-700',
  );
  return <Badge className={classes}>{status}</Badge>;
}
