import { useEffect, useState } from 'react';
import { Copy, X } from 'lucide-react';
import { fetchProfile, regenerateApiKey, Profile } from '../api/jobsApi';

interface Props { onClose: () => void; }

export default function SettingsPanel({ onClose }: Props) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [newKey, setNewKey] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    fetchProfile().then(setProfile).catch(() => setError('Failed to load profile'));
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
      </div>
    </div>
  );
}
