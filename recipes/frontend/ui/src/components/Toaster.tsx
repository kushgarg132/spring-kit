import { Toaster as SonnerToaster } from "sonner";
import { useTheme } from "../theme/ThemeProvider";

export { toast } from "sonner";

export function Toaster() {
  const { resolvedTheme } = useTheme();
  return <SonnerToaster theme={resolvedTheme} position="top-right" richColors closeButton />;
}
