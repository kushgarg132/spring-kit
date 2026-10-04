import * as CheckboxPrimitive from "@radix-ui/react-checkbox";
import { Check } from "lucide-react";

export function Checkbox({
  checked,
  onCheckedChange,
  "aria-label": ariaLabel
}: {
  checked: boolean | "indeterminate";
  onCheckedChange: (checked: boolean) => void;
  "aria-label"?: string;
}) {
  return (
    <CheckboxPrimitive.Root
      checked={checked}
      onCheckedChange={(value) => onCheckedChange(value === true)}
      aria-label={ariaLabel}
      className="flex h-4 w-4 items-center justify-center rounded border border-slate-300 bg-white data-[state=checked]:border-brand-700 data-[state=checked]:bg-brand-700 data-[state=indeterminate]:border-brand-700 data-[state=indeterminate]:bg-brand-700 dark:border-slate-600 dark:bg-slate-900"
    >
      <CheckboxPrimitive.Indicator className="text-white">
        <Check className="h-3 w-3" />
      </CheckboxPrimitive.Indicator>
    </CheckboxPrimitive.Root>
  );
}
