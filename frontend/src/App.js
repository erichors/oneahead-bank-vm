import React, { useEffect, useRef, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import axios from 'axios';
import './App.css';

const API_BASE_URL = process.env.REACT_APP_API_URL || '';
const api = axios.create({ baseURL: API_BASE_URL });

function Dashboard() {
  const [balance, setBalance] = useState(null);
  const [message, setMessage] = useState('');

  const fetchBalance = async () => {
    try {
      const response = await api.get('/api/account/balance');
      setBalance(Number(response.data.balance));
      setMessage('');
    } catch (err) {
      setMessage('Balance request failed: ' + (err.response?.data?.message || err.message));
    }
  };

  useEffect(() => {
    fetchBalance();
  }, []);

  return (
    <section className="dashboard-grid">
      <article className="balance-panel">
        <div className="balance-copy">
          <p className="eyebrow">OneAhead Cash</p>
          <h2>Account 1001</h2>
          <p>Primary checking account</p>
        </div>
        <div className="balance">{balance === null ? '--' : balance.toLocaleString(undefined, { style: 'currency', currency: 'USD' })}</div>
        <button className="btn balance-action" onClick={fetchBalance}>Refresh Balance</button>
      </article>

      <article className="market-panel">
        <p className="eyebrow">Tier Map</p>
        <div className="tier-line">
          <span>Frontend</span>
          <span>Backend + DB</span>
          <span>Credit</span>
        </div>
        <div className="pulse-strip">
          <span />
          <span />
          <span />
        </div>
      </article>

      <article className="insight-panel">
        <p className="eyebrow">Demo Mode</p>
        <h2>Live VM traffic and problem controls are in Admin.</h2>
        <Link className="text-link" to="/admin">Open Admin</Link>
      </article>
      {message && <div className="error">{message}</div>}
    </section>
  );
}

function Deposit() {
  const [amount, setAmount] = useState('100');
  const [metadata, setMetadata] = useState('manual deposit');
  const [message, setMessage] = useState('');

  const submit = async (event) => {
    event.preventDefault();
    try {
      const response = await api.post('/api/account/deposit', { amount: Number(amount), metadata });
      setMessage(response.data.message);
    } catch (err) {
      setMessage('Deposit failed: ' + (err.response?.data?.message || err.message));
    }
  };

  return (
    <section className="panel transaction-panel">
      <h2>Deposit</h2>
      <form onSubmit={submit}>
        <label>Amount<input type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} /></label>
        <label>Metadata<input value={metadata} onChange={(e) => setMetadata(e.target.value)} /></label>
        <button className="btn" type="submit">Deposit</button>
      </form>
      {message && <div className={message.includes('failed') ? 'error' : 'success'}>{message}</div>}
    </section>
  );
}

function Transfer() {
  const [toAccount, setToAccount] = useState('2002');
  const [amount, setAmount] = useState('25');
  const [metadata, setMetadata] = useState('manual transfer');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const submit = async (event) => {
    event.preventDefault();
    try {
      const response = await api.post('/api/account/transfer', { toAccount, amount: Number(amount), metadata });
      setResult(response.data);
      setError('');
    } catch (err) {
      setError('Transfer failed: ' + (err.response?.data?.message || err.message));
      setResult(null);
    }
  };

  return (
    <section className="panel transaction-panel">
      <h2>Transfer</h2>
      <form onSubmit={submit}>
        <label>To Account<input value={toAccount} onChange={(e) => setToAccount(e.target.value)} /></label>
        <label>Amount<input type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} /></label>
        <label>Metadata<input value={metadata} onChange={(e) => setMetadata(e.target.value)} /></label>
        <button className="btn" type="submit">Transfer</button>
      </form>
      {error && <div className="error">{error}</div>}
      {result && <pre>{JSON.stringify(result, null, 2)}</pre>}
    </section>
  );
}

function CreditCheck() {
  const [ssn, setSsn] = useState('123-45-6789');
  const [metadata, setMetadata] = useState('manual credit check');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const submit = async (event) => {
    event.preventDefault();
    try {
      const response = await api.post('/api/credit/check', { ssn, metadata });
      setResult(response.data);
      setError('');
    } catch (err) {
      setError('Credit check failed: ' + (err.response?.data?.message || err.message));
      setResult(null);
    }
  };

  return (
    <section className="panel wide-panel transaction-panel">
      <h2>Credit Check</h2>
      <form className="inline-form" onSubmit={submit}>
        <label>SSN<input value={ssn} onChange={(e) => setSsn(e.target.value)} /></label>
        <label>Metadata<input value={metadata} onChange={(e) => setMetadata(e.target.value)} /></label>
        <button className="btn" type="submit">Check</button>
      </form>
      {error && <div className="error">{error}</div>}
      {result && <pre>{JSON.stringify(result, null, 2)}</pre>}
    </section>
  );
}

function Admin() {
  const [configs, setConfigs] = useState({});
  const [loadLevel, setLoadLevel] = useState(0);
  const [stats, setStats] = useState({ sent: 0, ok: 0, failed: 0 });
  const [lastError, setLastError] = useState('');
  const intervalRef = useRef(null);

  const loadConfigs = async () => {
    const response = await api.get('/api/admin/configs');
    const next = {};
    response.data.forEach((item) => { next[item.configKey] = item.configValue; });
    setConfigs(next);
  };

  useEffect(() => {
    loadConfigs().catch((err) => setLastError(err.message));
  }, []);

  useEffect(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    if (loadLevel <= 0) return undefined;

    intervalRef.current = setInterval(() => runLoadBatch(loadLevel), 1000);
    return () => clearInterval(intervalRef.current);
  }, [loadLevel]);

  const updateConfig = async (key, value, description) => {
    await api.post('/api/admin/configs', { key, value: String(value), description });
    await loadConfigs();
  };

  const toggle = (key) => configs[key] === 'true';

  const runLoadBatch = async (count) => {
    const calls = Array.from({ length: count }, (_, index) => {
      const amount = Math.floor(Math.random() * 200) + 10;
      const account = String(Math.floor(Math.random() * 9000) + 1000);
      const ssn = `${Math.floor(Math.random() * 900) + 100}-45-6789`;
      const routes = [
        () => api.get('/api/account/balance'),
        () => api.post('/api/account/deposit', { amount, metadata: `admin load deposit ${index}` }),
        () => api.post('/api/account/transfer', { toAccount: account, amount: 1, metadata: `admin load transfer ${index}` }),
        () => api.post('/api/credit/check', { ssn, metadata: `admin load credit ${index}` }),
      ];
      return routes[index % routes.length]();
    });

    const results = await Promise.allSettled(calls);
    const ok = results.filter((result) => result.status === 'fulfilled').length;
    const failed = results.length - ok;
    setStats((current) => ({ sent: current.sent + results.length, ok: current.ok + ok, failed: current.failed + failed }));
    const rejected = results.find((result) => result.status === 'rejected');
    if (rejected) setLastError(rejected.reason?.message || 'Load request failed');
  };

  return (
    <section className="admin-grid">
      <article className="panel wide-panel load-panel">
        <div className="section-head">
          <div>
            <p className="eyebrow">Control Room</p>
            <h2>Load Driver</h2>
          </div>
          <button className="btn secondary" onClick={() => setStats({ sent: 0, ok: 0, failed: 0 })}>Reset Stats</button>
        </div>
        <label className="range-label">Request groups per second: {loadLevel}
          <input type="range" min="0" max="40" value={loadLevel} onChange={(e) => setLoadLevel(Number(e.target.value))} />
        </label>
        <div className="stat-row">
          <span>Sent <strong>{stats.sent}</strong></span>
          <span>OK <strong>{stats.ok}</strong></span>
          <span>Failed <strong>{stats.failed}</strong></span>
        </div>
        {lastError && <div className="error">{lastError}</div>}
      </article>

      <ProblemToggle
        title="Backend CPU Burn"
        detail={`${configs['problem.cpu.millis'] || 250}ms per backend request`}
        enabled={toggle('problem.cpu.enabled')}
        onToggle={(enabled) => updateConfig('problem.cpu.enabled', enabled, 'Built-in CPU burn problem')}
      >
        <label>CPU burn millis
          <input type="number" min="25" max="5000" value={configs['problem.cpu.millis'] || 250} onChange={(e) => updateConfig('problem.cpu.millis', e.target.value, 'CPU burn duration per request')} />
        </label>
      </ProblemToggle>

      <ProblemToggle
        title="Slow SQL"
        detail={`${configs['sql.slow.delay'] || 0}s delay on deposit and transfer`}
        enabled={toggle('sql.slow.enabled')}
        onToggle={(enabled) => updateConfig('sql.slow.enabled', enabled, 'Slow SQL simulation')}
      >
        <label>Delay seconds
          <input type="number" min="0" max="60" value={configs['sql.slow.delay'] || 2} onChange={(e) => updateConfig('sql.slow.delay', e.target.value, 'Slow SQL delay in seconds')} />
        </label>
      </ProblemToggle>

      <ProblemToggle
        title="Slow Credit"
        detail={`${configs['credit.slow.delay'] || 0}s delay before backend calls credit tier`}
        enabled={toggle('credit.slow.enabled')}
        onToggle={(enabled) => updateConfig('credit.slow.enabled', enabled, 'Slow credit check simulation')}
      >
        <label>Delay seconds
          <input type="number" min="0" max="60" value={configs['credit.slow.delay'] || 2} onChange={(e) => updateConfig('credit.slow.delay', e.target.value, 'Slow credit check delay in seconds')} />
        </label>
      </ProblemToggle>

      <ProblemToggle
        title="404 Errors"
        detail="Return 404 from banking and credit proxy endpoints"
        enabled={toggle('error.404.enabled')}
        onToggle={(enabled) => updateConfig('error.404.enabled', enabled, '404 error simulation')}
      />
    </section>
  );
}

function ProblemToggle({ title, detail, enabled, onToggle, children }) {
  return (
    <article className="panel problem-panel">
      <div className="section-head">
        <div>
          <h2>{title}</h2>
          <p>{detail}</p>
        </div>
        <label className="switch">
          <input type="checkbox" checked={enabled} onChange={(e) => onToggle(e.target.checked)} />
          <span />
        </label>
      </div>
      {children && <div className="problem-controls">{children}</div>}
    </article>
  );
}

function Navigation() {
  const location = useLocation();
  const links = [
    ['/', 'Dashboard'],
    ['/deposit', 'Deposit'],
    ['/transfer', 'Transfer'],
    ['/credit', 'Credit'],
    ['/admin', 'Admin'],
  ];

  return (
    <nav className="nav">
      {links.map(([to, label]) => <Link key={to} to={to} className={location.pathname === to ? 'active' : ''}>{label}</Link>)}
    </nav>
  );
}

function App() {
  return (
    <Router>
      <div className="App">
        <header className="app-header">
          <div className="header-content">
            <div className="brand-lockup">
              <div className="brand-mark">1A</div>
              <div>
                <h1>OneAhead Bank</h1>
                <p>React frontend -> Java backend + DB -> Java credit service</p>
              </div>
            </div>
            <div className="header-actions">
              <span className="status-pill">VM Edition</span>
              <span className="tier-badge">3 VMs</span>
            </div>
          </div>
        </header>
        <Navigation />
        <main className="container">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/deposit" element={<Deposit />} />
            <Route path="/transfer" element={<Transfer />} />
            <Route path="/credit" element={<CreditCheck />} />
            <Route path="/admin" element={<Admin />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
