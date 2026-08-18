import { useState } from "react";
import {
  Search,
  X,
  RefreshCw,
  Briefcase,
  AlertCircle,
  Trash2,
} from "lucide-react";
import { useJobs } from "./hooks/useJobs";
import { JobStatus } from "./types";
import JobCard from "./components/JobCard";
import ScraperPanel from "./components/ScraperPanel";

const STATUSES: { value: JobStatus | "all"; label: string }[] = [
  { value: "all", label: "All" },
  { value: "saved", label: "Saved" },
  { value: "applied", label: "Applied" },
  { value: "interviewing", label: "Interviewing" },
  { value: "rejected", label: "Rejected" },
];

export default function App() {
  const {
    jobs,
    stats,
    loading,
    error,
    filters,
    setFilters,
    updateStatus,
    updateNotes,
    deleteJob,
    refresh,
  } = useJobs();

  const [searchInput, setSearchInput] = useState("");

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setFilters((f) => ({ ...f, search: searchInput }));
  };

  const clearSearch = () => {
    setSearchInput("");
    setFilters((f) => ({ ...f, search: "" }));
  };

  const [selected, setSelected] = useState<Set<string>>(new Set());

  const toggleSelect = (id: string, checked: boolean) => {
    setSelected((prev) => {
      const next = new Set(prev);
      checked ? next.add(id) : next.delete(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    setSelected((prev) =>
      prev.size === jobs.length ? new Set() : new Set(jobs.map((j) => j.id)),
    );
  };

  const deleteSelected = async () => {
    if (!selected.size) return;
    for (const id of selected) await deleteJob(id);
    setSelected(new Set());
  };

  return (
    <div className="layout">
      {/* ── Header ── */}
      <header className="header">
        <div className="container">
          <div className="header-inner">
            <div className="brand">
              <span className="brand-name">Meridian</span>
              <span className="brand-dot" />
              <span className="brand-tagline">Internship Pipeline</span>
            </div>
            <div className="header-actions">
              <button
                className="btn btn-icon"
                onClick={refresh}
                title="Refresh"
              >
                <RefreshCw size={15} />
              </button>
            </div>
          </div>
        </div>
      </header>

      <main>
        <div className="container">
          {/* ── Stats ── */}
          {stats && (
            <section className="stats-section">
              <div className="stats-grid">
                <div className="stat-cell total">
                  <span className="stat-value">{stats.total}</span>
                  <span className="stat-label">Total</span>
                </div>
                <div className="stat-cell saved">
                  <span className="stat-value">{stats.saved}</span>
                  <span className="stat-label">Saved</span>
                </div>
                <div className="stat-cell applied">
                  <span className="stat-value">{stats.applied}</span>
                  <span className="stat-label">Applied</span>
                </div>
                <div className="stat-cell interviewing">
                  <span className="stat-value">{stats.interviewing}</span>
                  <span className="stat-label">Interviewing</span>
                </div>
                <div className="stat-cell rejected">
                  <span className="stat-value">{stats.rejected}</span>
                  <span className="stat-label">Rejected</span>
                </div>
              </div>
            </section>
          )}

          {/* ── Scraper ── */}
          <ScraperPanel onComplete={refresh} />

          {/* ── Controls ── */}
          <section className="controls-section">
            <div className="controls-row">
              <form className="search-wrap" onSubmit={handleSearch}>
                <Search size={14} className="search-icon" />
                <input
                  className="search-input"
                  type="text"
                  placeholder="Search jobs or companies..."
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                />
                {searchInput && (
                  <button
                    type="button"
                    className="search-clear"
                    onClick={clearSearch}
                  >
                    <X size={13} />
                  </button>
                )}
              </form>

              <div className="filter-tabs">
                {STATUSES.map((s) => (
                  <button
                    key={s.value}
                    className={`filter-tab${filters.status === s.value ? " active" : ""}`}
                    onClick={() =>
                      setFilters((f) => ({ ...f, status: s.value }))
                    }
                  >
                    {s.label}
                  </button>
                ))}
              </div>
            </div>
          </section>

          {/* ── Error ── */}
          {error && (
            <div className="error-bar">
              <AlertCircle size={14} />
              {error}
            </div>
          )}
          {jobs.length > 0 && (
            <div className="bulk-bar">
              <label className="bulk-select-all">
                <input
                  type="checkbox"
                  checked={selected.size === jobs.length && jobs.length > 0}
                  onChange={toggleSelectAll}
                />
                <span>
                  {selected.size > 0
                    ? `${selected.size} selected`
                    : "Select all"}
                </span>
              </label>
              {selected.size > 0 && (
                <button className="btn btn-danger" onClick={deleteSelected}>
                  <Trash2 size={13} />
                  Delete {selected.size} job{selected.size !== 1 ? "s" : ""}
                </button>
              )}
            </div>
          )}
          {/* ── Jobs ── */}
          <section className="jobs-section">
            {!loading && jobs.length > 0 && (
              <p className="results-meta">
                {jobs.length} position{jobs.length !== 1 ? "s" : ""}
                {filters.search && ` matching "${filters.search}"`}
                {filters.status !== "all" && ` · ${filters.status}`}
              </p>
            )}

            {loading ? (
              <div className="state-center">
                <div className="spinner" />
                <p>Loading positions...</p>
              </div>
            ) : jobs.length === 0 ? (
              <div className="state-center">
                <Briefcase
                  size={28}
                  strokeWidth={1.25}
                  style={{ color: "var(--ink-15)" }}
                />
                <p>No positions found</p>
                <p className="state-sub">
                  Run the scraper to discover new opportunities
                </p>
              </div>
            ) : (
              <div className="jobs-list">
                {jobs.map((job) => (
                  <JobCard
                    key={job.id}
                    job={job}
                    selected={selected.has(job.id)}
                    onSelect={toggleSelect}
                    onStatusChange={updateStatus}
                    onNotesChange={updateNotes}
                    onDelete={deleteJob}
                  />
                ))}
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
