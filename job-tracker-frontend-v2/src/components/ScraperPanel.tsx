import { useState, useRef, useEffect } from 'react';
import { Play, Square, Terminal, ChevronDown, ChevronUp } from 'lucide-react';
import { auth } from '../firebase';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';
// The scraper runs as a subprocess on whatever machine the backend is on — only
// possible when backend and scraper share a filesystem, i.e. local dev.
const SCRAPER_AVAILABLE = API_BASE.includes('localhost') || API_BASE.includes('127.0.0.1');

interface LogLine { type: 'log' | 'done' | 'error' | 'sys'; text: string; }
interface Props { onComplete: () => void; }

export default function ScraperPanel({ onComplete }: Props) {
  const [running, setRunning]   = useState(false);
  const [logs, setLogs]         = useState<LogLine[]>([]);
  const [open, setOpen]         = useState(false);
  const [maxJobs, setMaxJobs]   = useState(25);
  const logEndRef               = useRef<HTMLDivElement>(null);

  useEffect(() => { logEndRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [logs]);

  const addLog = (type: LogLine['type'], text: string) =>
    setLogs(prev => [...prev, { type, text }]);

  const runScraper = async () => {
    if (running) return;
    setRunning(true);
    setLogs([]);
    setOpen(true);
    addLog('sys', 'Connecting to scraper process...');

    try {
      const token = await auth.currentUser?.getIdToken();
      const response = await fetch(`${API_BASE}/scraper/run?maxJobs=${maxJobs}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}`, 'Accept': 'text/event-stream' },
      });

      if (!response.ok) {
        addLog('error', `Failed to start: HTTP ${response.status}`);
        setRunning(false);
        return;
      }

      const reader  = response.body?.getReader();
      const decoder = new TextDecoder();
      if (!reader) { addLog('error', 'No response stream'); setRunning(false); return; }

      let buffer = '';
      let eventName = 'log';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() ?? '';

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim();
          } else if (line.startsWith('data:')) {
            const text = line.slice(5).trim();
            if (eventName === 'done') {
              addLog('done', text);
              onComplete();
            } else if (eventName === 'error') {
              addLog('error', text);
            } else {
              addLog('log', text);
            }
            eventName = 'log';
          }
        }
      }
    } catch (err) {
      addLog('error', `Connection error: ${err}`);
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="scraper-section">
      <div className="scraper-bar">
        <div className="scraper-bar-info">
          <span className="scraper-bar-title">Job Scraper</span>
          <span className="scraper-bar-sub">
            {running ? 'Running pipeline — scraping, filtering, tailoring...' : 'Idle — ready to run'}
          </span>
        </div>

        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {logs.length > 0 && (
            <button
              className="btn btn-ghost"
              onClick={() => setOpen(o => !o)}
              style={{ fontSize: '0.75rem' }}
            >
              <Terminal size={13} />
              Logs
              {open ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
            </button>
          )}
          {SCRAPER_AVAILABLE ? (
            <>
              <label style={{ display: 'flex', gap: '0.4rem', alignItems: 'center', fontSize: '0.75rem' }}>
                Limit
                <input
                  type="range"
                  min={0}
                  max={50}
                  value={maxJobs}
                  disabled={running}
                  onChange={(e) => setMaxJobs(Number(e.target.value))}
                />
                <span style={{ minWidth: '1.5em', textAlign: 'right' }}>{maxJobs}</span>
              </label>
              <button
                className={`btn-run${running ? ' running' : ''}`}
                onClick={runScraper}
                disabled={running}
              >
                {running ? <Square size={13} /> : <Play size={13} />}
                {running ? 'Running...' : 'Run Scraper'}
              </button>
            </>
          ) : (
            <span className="scraper-bar-sub" style={{ fontSize: '0.75rem' }}>
              Run locally — <code>python main.py</code> in scraper/
            </span>
          )}
        </div>
      </div>

      {open && logs.length > 0 && (
        <div className="console-wrap">
          <div className="console-header">
            <span className="console-title">
              {running && <span className="live-dot" />}
              Output
            </span>
            <button
              className="btn btn-icon"
              onClick={() => setOpen(false)}
              style={{ color: 'rgba(255,255,255,0.3)', padding: '0.2rem' }}
            >
              <Square size={11} />
            </button>
          </div>
          <div className="console-body">
            {logs.map((line, i) => (
              <div key={i} className={`log-line ${line.type}`}>
                {line.text}
              </div>
            ))}
            <div ref={logEndRef} />
          </div>
        </div>
      )}
    </div>
  );
}
