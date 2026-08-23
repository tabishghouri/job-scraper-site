import { Job, JobStats, JobStatus } from '../types';
import { auth } from '../firebase';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

async function apiFetch<T>(url: string, options?: RequestInit): Promise<T> {
  const token = await auth.currentUser?.getIdToken();
  const response = await fetch(`${API_BASE}${url}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
    ...options,
  });
  if (!response.ok) throw new Error(`API error: ${response.status} ${response.statusText}`);
  if (response.status === 204) return undefined as T;
  return response.json();
}

export const fetchJobs = (status?: string, search?: string): Promise<Job[]> => {
  const params = new URLSearchParams();
  if (status && status !== 'all') params.append('status', status);
  if (search) params.append('search', search);
  const query = params.toString() ? `?${params.toString()}` : '';
  return apiFetch<Job[]>(`/jobs${query}`);
};

export const fetchStats = (): Promise<JobStats> => apiFetch<JobStats>('/jobs/stats');

export const updateJobStatus = (id: string, status: JobStatus): Promise<Job> =>
  apiFetch<Job>(`/jobs/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) });

export const updateJobNotes = (id: string, notes: string): Promise<Job> =>
  apiFetch<Job>(`/jobs/${id}/notes`, { method: 'PATCH', body: JSON.stringify({ notes }) });

export const deleteJob = (id: string): Promise<void> =>
  apiFetch<void>(`/jobs/${id}`, { method: 'DELETE' });

export type JobLevel = 'internship' | 'entry_level';

export interface Profile {
  uid: string;
  hasApiKey: boolean;
  apiKeyLast4: string | null;
  searchQueries: string[];
  locations: string[];
  jobLevel: JobLevel;
}
export interface RegeneratedKey { uid: string; apiKey: string; }

export const fetchProfile = (): Promise<Profile> => apiFetch<Profile>('/me');

export const regenerateApiKey = (): Promise<RegeneratedKey> =>
  apiFetch<RegeneratedKey>('/me/regenerate-key', { method: 'POST' });

export const updateSearchConfig = (searchQueries: string[], locations: string[], jobLevel: JobLevel): Promise<Profile> =>
  apiFetch<Profile>('/me/search-config', {
    method: 'PATCH',
    body: JSON.stringify({ searchQueries, locations, jobLevel }),
  });
