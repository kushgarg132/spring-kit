export interface StatBarItem {
  id: string;
  label: string;
  total: number;
  voted: number;
  votingPercentage: number;
}

/**
 * Turnout-by-group as a sorted meter list rather than a categorical chart —
 * with dozens of groups (wards, castes), per-group hue would blow past any
 * safe categorical count. One sequential fill (brand) against a neutral
 * track reads correctly at any number of rows.
 */
export function StatBarList({ items }: { items: StatBarItem[] }) {
  const sorted = [...items].sort((a, b) => b.votingPercentage - a.votingPercentage);

  return (
    <ul className="flex flex-col gap-3">
      {sorted.map((item) => (
        <li key={item.id} className="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:gap-3">
          <span className="truncate text-sm text-slate-700 dark:text-slate-300 sm:w-40 sm:shrink-0" title={item.label}>
            {item.label}
          </span>
          <div className="flex flex-1 items-center gap-3">
            <div className="h-2 flex-1 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
              <div
                className="h-full rounded-full bg-brand-600 dark:bg-brand-500"
                style={{ width: `${Math.min(100, item.votingPercentage)}%` }}
              />
            </div>
            <span className="shrink-0 text-right font-mono text-xs tabular-nums text-slate-500 dark:text-slate-400 sm:w-32">
              {item.voted}/{item.total} · {item.votingPercentage.toFixed(1)}%
            </span>
          </div>
        </li>
      ))}
    </ul>
  );
}
