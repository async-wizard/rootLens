import { NavLink } from 'react-router-dom';
import { Moon, Sun } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useTheme } from '@/context/ThemeContext';
import { Button } from '@/components/ui/button';

interface Props {
  lastUpdated: Date | null;
}

function timeAgo(date: Date): string {
  const secs = Math.floor((Date.now() - date.getTime()) / 1000);
  if (secs < 60) return `${secs}s ago`;
  return `${Math.floor(secs / 60)}m ago`;
}

export function Navbar({ lastUpdated }: Props) {
  const { theme, toggle } = useTheme();

  return (
    <nav className="border-b border-border bg-card px-6 py-3 flex items-center justify-between">
      <div>
        <span className="text-lg font-bold text-foreground">RootLens</span>
        <span className="ml-2 text-xs text-muted-foreground">Observability Platform</span>
      </div>

      <div className="flex items-center gap-6">
        <NavLink
          to="/"
          end
          className={({ isActive }) =>
            cn('text-sm font-medium transition-colors', isActive ? 'text-foreground' : 'text-muted-foreground hover:text-foreground')
          }
        >
          Incidents
        </NavLink>
        <NavLink
          to="/services"
          className={({ isActive }) =>
            cn('text-sm font-medium transition-colors', isActive ? 'text-foreground' : 'text-muted-foreground hover:text-foreground')
          }
        >
          Services
        </NavLink>
        {lastUpdated && (
          <span className="text-xs text-muted-foreground">
            Updated {timeAgo(lastUpdated)}
          </span>
        )}
        <Button
          variant="ghost"
          size="icon"
          onClick={toggle}
          aria-label="Toggle theme"
          className="h-8 w-8"
        >
          {theme === 'dark' ? (
            <Sun className="h-4 w-4" />
          ) : (
            <Moon className="h-4 w-4" />
          )}
        </Button>
      </div>
    </nav>
  );
}
