/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  theme: {
    extend: {
      fontFamily: {
        sans: ["InterVariable", "Inter", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["ui-monospace", "SFMono-Regular", "Menlo", "Consolas", "monospace"]
      },
      colors: {
        // "Indelible ink" violet — the app's single accent, echoing the
        // Election Commission finger-ink mark. Used sparingly: primary actions,
        // active nav, focus rings, links.
        brand: {
          50: "#f6f2fb",
          100: "#ece2f6",
          200: "#d7c2ec",
          300: "#bd9adf",
          400: "#a172d1",
          500: "#8752c0",
          600: "#6f3ba3",
          700: "#5a2f80",
          800: "#472664",
          900: "#3a2050"
        },
        // Fixed status palette (never themed) — validated for contrast + distinctness
        // from `brand`. Reserved for state (voted/pending/warning), never reused as
        // "another series color".
        status: {
          good: "#0ca30c",
          warning: "#fab219",
          serious: "#ec835a",
          critical: "#d03b3b"
        }
      },
      boxShadow: {
        panel: "0 1px 2px 0 rgb(0 0 0 / 0.04), 0 1px 6px -1px rgb(0 0 0 / 0.06)"
      }
    }
  },
  plugins: []
};
