export type JobStatus = 'saved' | 'applied' | 'interviewing' | 'rejected';

export interface Job {
  id: string;
  jobUrl: string;
  jobTitle: string;
  companyName: string;
  companyWebsite?: string;
  location?: string;
  postedAt?: string;
  salary?: string;
  description?: string;
  numApplications?: string;
  jobPostLink: string;
  applyUrl?: string;
  source?: string;
  atsKeywords?: string;
  resumeFilename?: string;
  resumePdf?: string;
  resumePdfUrl?: string;
  dateScraped?: string;
  status: JobStatus;
  notes?: string;
}

export interface JobStats {
  total: number;
  saved: number;
  applied: number;
  interviewing: number;
  rejected: number;
}

export interface JobFilters {
  status: JobStatus | 'all';
  search: string;
}
