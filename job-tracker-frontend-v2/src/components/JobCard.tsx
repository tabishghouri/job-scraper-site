import { useState } from "react";
import {
  ChevronDown,
  ChevronUp,
  MapPin,
  Calendar,
  Users,
  FileText,
  ExternalLink,
  Trash2,
  FileCheck,
  Tag,
  ArrowUpRight,
  StickyNote,
} from "lucide-react";
import { Job, JobStatus } from "../types";

const STATUS_LABELS: Record<string, string> = {
  saved: "Saved",
  applied: "Applied",
  interviewing: "Interviewing",
  rejected: "Rejected",
};

const STATUSES: JobStatus[] = ["saved", "applied", "interviewing", "rejected"];

interface Props {
  job: Job;
  onStatusChange: (id: string, s: JobStatus) => void;
  onNotesChange: (id: string, n: string) => void;
  onDelete: (id: string) => void;
  selected?: boolean;
  onSelect?: (id: string, checked: boolean) => void;
}

export default function JobCard({
  job,
  onStatusChange,
  onNotesChange,
  onDelete,
  selected,
  onSelect,
}: Props) {
  const [expanded, setExpanded] = useState(false);
  const [notes, setNotes] = useState(job.notes ?? "");
  const [editingNotes, setEditingNotes] = useState(false);

  const keywords = job.atsKeywords
    ? job.atsKeywords
        .split(",")
        .map((k) => k.trim())
        .filter(Boolean)
        .slice(0, 12)
    : [];

  const hasPdf = job.resumePdfUrl && job.resumePdfUrl.startsWith("http");

  return (
    <div className={`job-card${expanded ? " expanded" : ""}`}>
      {/* ── Header ── */}
      <div className="job-card-header" onClick={() => setExpanded((e) => !e)}>
        <input
          type="checkbox"
          className="job-select"
          checked={selected ?? false}
          onChange={(e) => {
            e.stopPropagation();
            onSelect?.(job.id, e.target.checked);
          }}
          onClick={(e) => e.stopPropagation()}
        />
        <div className="job-card-left">
          <div className="job-title-row">
            <h3 className="job-title">{job.jobTitle}</h3>
            <span className={`status-pill ${job.status}`}>
              {STATUS_LABELS[job.status] ?? job.status}
            </span>
          </div>

          <div className="job-company-row">
            <span className="company-name">{job.companyName}</span>
            {job.location && (
              <>
                <span className="sep">·</span>
                <MapPin size={11} style={{ flexShrink: 0 }} />
                <span>{job.location}</span>
              </>
            )}
            {job.salary && job.salary !== "N/A" && (
              <>
                <span className="sep">·</span>
                <span>{job.salary}</span>
              </>
            )}
          </div>

          <div className="job-chips">
            {job.postedAt && (
              <span className="chip">
                <Calendar size={9} />
                {job.postedAt}
              </span>
            )}
            {job.source && <span className="chip">{job.source}</span>}
            {job.numApplications && job.numApplications !== "N/A" && (
              <span className="chip">
                <Users size={9} />
                {job.numApplications}
              </span>
            )}
          </div>
        </div>

        <div className="job-card-chevron">
          {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </div>
      </div>

      {/* ── Body ── */}
      {expanded && (
        <div className="job-card-body">
          {/* Status */}
          <div>
            <span className="section-label">Application Status</span>
            <div className="status-selector">
              {STATUSES.map((s) => (
                <button
                  key={s}
                  className={`status-btn${job.status === s ? ` active-${s}` : ""}`}
                  onClick={() => onStatusChange(job.id, s)}
                >
                  {STATUS_LABELS[s]}
                </button>
              ))}
            </div>
          </div>

          {/* Keywords */}
          {keywords.length > 0 && (
            <div>
              <span className="section-label">
                <Tag
                  size={9}
                  style={{ display: "inline", marginRight: "0.3rem" }}
                />
                ATS Keywords
              </span>
              <div className="keywords-wrap">
                {keywords.map((kw) => (
                  <span key={kw} className="kw-tag">
                    {kw}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Description */}
          {job.description && (
            <div>
              <span className="section-label">Description</span>
              <p className="description-text">
                {job.description.slice(0, 600)}
                {job.description.length > 600 && "…"}
              </p>
            </div>
          )}

          {/* Notes */}
          <div>
            <span className="section-label">
              <StickyNote
                size={9}
                style={{ display: "inline", marginRight: "0.3rem" }}
              />
              Notes
            </span>
            {editingNotes ? (
              <>
                <textarea
                  className="notes-textarea"
                  value={notes}
                  rows={3}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Add your thoughts about this role..."
                  autoFocus
                />
                <div className="notes-actions">
                  <button
                    className="btn btn-primary"
                    style={{ fontSize: "0.75rem", padding: "0.375rem 0.75rem" }}
                    onClick={() => {
                      onNotesChange(job.id, notes);
                      setEditingNotes(false);
                    }}
                  >
                    Save
                  </button>
                  <button
                    className="btn btn-ghost"
                    style={{ fontSize: "0.75rem", padding: "0.375rem 0.75rem" }}
                    onClick={() => setEditingNotes(false)}
                  >
                    Cancel
                  </button>
                </div>
              </>
            ) : (
              <div
                className={`notes-display${!notes ? " placeholder-text" : ""}`}
                onClick={() => setEditingNotes(true)}
              >
                {notes || "Click to add notes..."}
              </div>
            )}
          </div>

          {/* Resume */}
          {(job.resumeFilename || hasPdf) && (
            <div>
              <span className="section-label">
                <FileCheck
                  size={9}
                  style={{ display: "inline", marginRight: "0.3rem" }}
                />
                Tailored Resume
              </span>
              <div className="resume-row">
                {job.resumeFilename && (
                  <span className="file-badge">
                    <FileText size={10} />
                    {job.resumeFilename}
                  </span>
                )}
                {hasPdf && (
                  <a
                    href={job.resumePdfUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn btn-link"
                  >
                    <FileText size={12} />
                    View PDF
                    <ArrowUpRight size={11} />
                  </a>
                )}
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="card-actions">
            {job.applyUrl && (
              <a
                href={job.applyUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-apply"
              >
                Apply Now
                <ArrowUpRight size={13} />
              </a>
            )}
            {job.jobPostLink && (
              <a
                href={job.jobPostLink}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-ghost"
                style={{ fontSize: "0.8125rem" }}
              >
                View Posting
                <ExternalLink size={12} />
              </a>
            )}
            <div className="card-actions-right">
              <button
                className="btn btn-danger"
                onClick={() => onDelete(job.id)}
              >
                <Trash2 size={12} />
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
