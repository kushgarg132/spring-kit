import { cva, type VariantProps } from "class-variance-authority";
import { CheckCircle2, Circle } from "lucide-react";
import type { HTMLAttributes } from "react";
import { cn } from "../lib/cn";

const badgeVariants = cva(
  "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium",
  {
    variants: {
      variant: {
        good: "bg-status-good/10 text-status-good dark:bg-status-good/15",
        pending: "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300",
        warning: "bg-status-warning/15 text-amber-800 dark:text-status-warning",
        critical: "bg-status-critical/10 text-status-critical",
        brand: "bg-brand-50 text-brand-800 dark:bg-brand-900/40 dark:text-brand-300"
      }
    },
    defaultVariants: { variant: "pending" }
  }
);

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement>, VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}

/** Status is never color-alone: icon + label carry it, color is reinforcement. */
export function VoteStatusBadge({ hasVoted }: { hasVoted: boolean }) {
  return (
    <Badge variant={hasVoted ? "good" : "pending"}>
      {hasVoted ? <CheckCircle2 className="h-3 w-3" aria-hidden /> : <Circle className="h-3 w-3" aria-hidden />}
      {hasVoted ? "Voted" : "Not voted"}
    </Badge>
  );
}
