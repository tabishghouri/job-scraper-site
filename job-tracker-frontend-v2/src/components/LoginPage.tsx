import { useState } from "react";
import { useAuth } from "../hooks/useAuth";

export default function LoginPage() {
  const { signIn, signUp } = useAuth();
  const [mode, setMode] = useState<"signin" | "signup">("signin");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      if (mode === "signup") {
        await signUp(email, password);
      } else {
        await signIn(email, password);
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message.replace(/^Firebase: /, "")
          : "Something went wrong",
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="layout">
      <main>
        <div
          className="container"
          style={{ maxWidth: "380px", paddingTop: "4rem" }}
        >
          <div
            className="brand"
            style={{ justifyContent: "center", marginBottom: "2rem" }}
          >
            <span className="brand-name">Job Tracker</span>
            <span className="brand-dot" />
            <span className="brand-tagline">Internship Pipeline</span>
          </div>

          <form
            onSubmit={handleSubmit}
            style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}
          >
            <input
              className="search-input"
              style={{ paddingLeft: "0.875rem" }}
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <input
              className="search-input"
              style={{ paddingLeft: "0.875rem" }}
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={6}
              required
            />

            {error && <div className="error-bar">{error}</div>}

            <button className="btn btn-primary" type="submit" disabled={busy}>
              {busy
                ? "Please wait..."
                : mode === "signup"
                  ? "Create account"
                  : "Sign in"}
            </button>

            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => {
                setMode(mode === "signup" ? "signin" : "signup");
                setError(null);
              }}
            >
              {mode === "signup"
                ? "Already have an account? Sign in"
                : "Don't have an account? Sign up"}
            </button>
          </form>
        </div>
      </main>
    </div>
  );
}
