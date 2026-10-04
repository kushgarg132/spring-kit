import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "./Button";
import { Select } from "./Select";

const DEFAULT_PAGE_SIZE_OPTIONS = [10, 20, 25, 50, 100];

export function Pagination({
  page,
  totalPages,
  totalElements,
  onPageChange,
  pageSize,
  onPageSizeChange,
  pageSizeOptions = DEFAULT_PAGE_SIZE_OPTIONS
}: {
  page: number;
  totalPages: number;
  totalElements?: number;
  onPageChange: (page: number) => void;
  pageSize?: number;
  onPageSizeChange?: (size: number) => void;
  pageSizeOptions?: number[];
}) {
  if (totalPages <= 1 && !pageSize) {
    return null;
  }

  const rangeStart = pageSize ? page * pageSize + 1 : undefined;
  const rangeEnd = pageSize && totalElements !== undefined ? Math.min((page + 1) * pageSize, totalElements) : undefined;

  return (
    <div className="flex flex-col gap-3 border-t border-slate-100 px-5 py-3 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-center gap-4">
        <span className="text-xs text-slate-500 dark:text-slate-400">
          {rangeStart !== undefined && rangeEnd !== undefined && totalElements !== undefined
            ? `Showing ${rangeStart}–${rangeEnd} of ${totalElements.toLocaleString()}`
            : `Page ${page + 1} of ${totalPages}`}
        </span>
        {pageSize !== undefined && onPageSizeChange && (
          <label className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
            Rows per page
            <Select
              value={String(pageSize)}
              onValueChange={(v) => onPageSizeChange(Number(v))}
              options={pageSizeOptions.map((n) => ({ value: String(n), label: String(n) }))}
              className="h-8 w-20"
            />
          </label>
        )}
      </div>
      {totalPages > 1 && (
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" onClick={() => onPageChange(page - 1)} disabled={page <= 0}>
            <ChevronLeft className="h-3.5 w-3.5" /> Previous
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => onPageChange(page + 1)}
            disabled={page >= totalPages - 1}
          >
            Next <ChevronRight className="h-3.5 w-3.5" />
          </Button>
        </div>
      )}
    </div>
  );
}
