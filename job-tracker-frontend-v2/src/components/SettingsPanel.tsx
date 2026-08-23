import { useEffect, useState } from 'react';
import { Copy, X } from 'lucide-react';
import { fetchProfile, regenerateApiKey, updateSearchConfig, Profile, JobLevel } from '../api/jobsApi';

interface Props { onClose: () => void; }

export default function SettingsPanel({ onClose }: Props) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [newKey, setNewKey] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [queriesInput, setQueriesInput] = useState('');
  const [locationsInput, setLocationsInput] = useState('');
  const [jobLevel, setJobLevel] = useState<JobLevel>('internship');
  const [savingSearch, setSavingSearch] = useState(false);
  const [searchSaved, setSearchSaved] = useState(false);

  useEffect(() => {
    fetchProfile().then((p) => {
      setProfile(p);
      setQueriesInput(p.searchQueries.join(', '));
      setLocationsInput(p.locations.join(', '));
      setJobLevel(p.jobLevel);
    }).catch(() => setError('Failed to load profile'));
  }, []);

  const handleRegenerate = async () => {
    setBusy(true);
    setError(null);
    try {
      const result = await regenerateApiKey();
      setNewKey(result.apiKey);
      setProfile((p) => (p ? { ...p, hasApiKey: true, apiKeyLast4: result.apiKey.slice(-4) } : p));
    } catch {
      setError('Failed to regenerate key');
    } finally {
      setBusy(false);
    }
  };

  const handleSaveSearch = async () => {
    setSavingSearch(true);
    setSearchSaved(false);
    setError(null);
    try {
      const queries = queriesInput.split(',').map((s) => s.trim()).filter(Boolean);
      const locations = locationsInput.split(',').map((s) => s.trim()).filter(Boolean);
      const updated = await updateSearchConfig(queries, locations, jobLevel);
      setProfile(updated);
      setSearchSaved(true);
    } catch {
      setError('Failed to save search preferences');
    } finally {
      setSavingSearch(false);
    }
  };

  return (
    <div className="scraper-section">
      <div className="scraper-bar">
        <div className="scraper-bar-info">
          <span className="scraper-bar-title">Scraper Setup</span>
          <span className="scraper-bar-sub">
            Copy these into your local <code>scraper/.env</code> to connect your own scraper to this account.
          </span>
        </div>
        <button className="btn btn-icon" onClick={onClose}><X size={16} /></button>
      </div>

      <div style={{ padding: '1rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {error && <div className="error-bar">{error}</div>}

        {profile && (
          <div>
            <div className="scraper-bar-sub" style={{ marginBottom: '0.25rem' }}>FIREBASE_UID</div>
            <code className="search-input" style={{ display: 'block', padding: '0.6rem 0.875rem' }}>
              {profile.uid}
            </code>
          </div>
        )}

        {newKey ? (
          <div>
            <div className="scraper-bar-sub" style={{ marginBottom: '0.25rem' }}>
              SCRAPER_API_KEY — copy this now, it won't be shown again
            </div>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <code className="search-input" style={{ display: 'block', padding: '0.6rem 0.875rem', flex: 1, wordBreak: 'break-all' }}>
                {newKey}
              </code>
              <button
                className="btn btn-icon"
                onClick={() => navigator.clipboard.writeText(newKey)}
                title="Copy"
              >
                <Copy size={15} />
              </button>
            </div>
          </div>
        ) : profile?.hasApiKey && (
          <div className="scraper-bar-sub">
            Personal API key ending in <strong>{profile.apiKeyLast4}</strong> is active.
          </div>
        )}

        <button className="btn btn-primary" onClick={handleRegenerate} disabled={busy} style={{ alignSelf: 'flex-start' }}>
          {busy ? 'Generating...' : profile?.hasApiKey ? 'Regenerate API Key' : 'Generate API Key'}
        </button>

        <hr style={{ border: 'none', borderTop: '1px solid var(--ink-15)', margin: '0.5rem 0' }} />

        <div className="scraper-bar-title" style={{ fontSize: '0.9rem' }}>Job Search Preferences</div>
        <span className="scraper-bar-sub">
          Leave blank to use the scraper's built-in software-engineering-intern defaults.
        </span>

        <div>
          <div className="scraper-bar-sub" style={{ marginBottom: '0.25rem' }}>Search Keywords (comma-separated)</div>
          <input
            className="search-input"
            style={{ paddingLeft: '0.875rem' }}
            type="text"
            placeholder="e.g. cybersecurity analyst, SOC analyst, retail associate"
            value={queriesInput}
            onChange={(e) => setQueriesInput(e.target.value)}
          />
        </div>

        <div>
          <div className="scraper-bar-sub" style={{ marginBottom: '0.25rem' }}>Locations (comma-separated)</div>
          <input
            className="search-input"
            style={{ paddingLeft: '0.875rem' }}
            type="text"
            placeholder="e.g. Toronto, Ontario, Canada"
            value={locationsInput}
            onChange={(e) => setLocationsInput(e.target.value)}
          />
        </div>

        <div>
          <div className="scraper-bar-sub" style={{ marginBottom: '0.25rem' }}>Job Level</div>
          <div style={{ display: 'flex', gap: '1rem', fontSize: '0.85rem' }}>
            <label style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
              <input
                type="radio"
                checked={jobLevel === 'internship'}
                onChange={() => setJobLevel('internship')}
              />
              Internship / Co-op
            </label>
            <label style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
              <input
                type="radio"
                checked={jobLevel === 'entry_level'}
                onChange={() => setJobLevel('entry_level')}
              />
              Entry-level / Full-time
            </label>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <button className="btn btn-primary" onClick={handleSaveSearch} disabled={savingSearch} style={{ alignSelf: 'flex-start' }}>
            {savingSearch ? 'Saving...' : 'Save Search Preferences'}
          </button>
          {searchSaved && <span className="scraper-bar-sub">Saved.</span>}
        </div>
      </div>
    </div>
  );
}
