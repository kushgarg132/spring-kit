import type { LucideIcon } from "lucide-react";
import { cn } from "../lib/cn";

export function StatTile({
  label,
  value,
  icon: Icon,
  tone = "default"
}: {
  label: string;
  value: string;
  icon?: LucideIcon;
  tone?: "default" | "good" | "brand";
}) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
          {label}
        </span>
        {Icon && (
          <Icon
            className={cn(
              "h-4 w-4",
              tone === "good" && "text-status-good",
              tone === "brand" && "text-brand-600 dark:text-brand-400",
              tone === "default" && "text-slate-400"
            )}
            aria-hidden
          />
        )}
      </div>
      <div
        className={cn(
          "mt-2 font-mono text-2xl font-semibold tabular-nums text-slate-900 dark:text-slate-50",
          tone === "good" && "text-status-good",
          tone === "brand" && "text-brand-700 dark:text-brand-400"
        )}
      >
        {value}
      </div>
    </div>
  );
}
