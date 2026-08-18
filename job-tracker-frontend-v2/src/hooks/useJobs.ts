import { useState, useEffect, useCallback } from 'react';
import { Job, JobStats, JobStatus, JobFilters } from '../types';
import * as api from '../api/jobsApi';

export function useJobs() {
  const [jobs, setJobs]       = useState<Job[]>([]);
  const [stats, setStats]     = useState<JobStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);
  const [filters, setFilters] = useState<JobFilters>({ status: 'all', search: '' });

  const loadJobs = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await api.fetchJobs(
        filters.status !== 'all' ? filters.status : undefined,
        filters.search || undefined
      );
      setJobs(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load jobs');
    } finally {
      setLoading(false);
    }
  }, [filters]);

  const loadStats = useCallback(async () => {
    try { setStats(await api.fetchStats()); }
    catch (err) { console.error('Stats error:', err); }
  }, []);

  useEffect(() => { loadJobs(); }, [loadJobs]);
  useEffect(() => { loadStats(); }, [loadStats]);

  const updateStatus = async (id: string, status: JobStatus) => {
    setJobs(prev => prev.map(j => j.id === id ? { ...j, status } : j));
    try { await api.updateJobStatus(id, status); loadStats(); }
    catch { loadJobs(); setError('Failed to update status'); }
  };

  const updateNotes = async (id: string, notes: string) => {
    setJobs(prev => prev.map(j => j.id === id ? { ...j, notes } : j));
    try { await api.updateJobNotes(id, notes); }
    catch { loadJobs(); setError('Failed to save notes'); }
  };

  const deleteJob = async (id: string) => {
    setJobs(prev => prev.filter(j => j.id !== id));
    try { await api.deleteJob(id); loadStats(); }
    catch { loadJobs(); setError('Failed to delete job'); }
  };

  return { jobs, stats, loading, error, filters, setFilters, updateStatus, updateNotes, deleteJob, refresh: loadJobs };
}
