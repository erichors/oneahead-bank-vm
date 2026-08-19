import React, { useEffect, useRef, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';
import './App.css';

const API_BASE_URL = process.env.REACT_APP_API_URL || '';
const api = axios.create({ baseURL: API_BASE_URL });

const fallbackUsers = [
  { username: 'tbrady', password: 'goat', firstName: 'Thomas', lastName: 'Brady', name: 'Thomas Brady', address: '82 Biscayne Terrace', city: 'Miami', state: 'Florida', lastFour: '9832' },
  { username: 'dmorgan', password: 'ahead1', firstName: 'Dave', lastName: 'Morgan', name: 'Dave Morgan', address: '2148 Aurora Avenue', city: 'Naperville', state: 'Illinois', lastFour: '1048' },
  { username: 'mlowe', password: 'ahead1', firstName: 'Matt', lastName: 'Lowe', name: 'Matt Lowe', address: '4406 Clifton Boulevard', city: 'Cleveland', state: 'Ohio', lastFour: '6412' },
  { username: 'dshah', password: 'ahead1', firstName: 'Dipen', lastName: 'Shah', name: 'Dipen Shah', address: '17 Oak Tree Road', city: 'Edison', state: 'New Jersey', lastFour: '7781' },
];

const fallbackAccount = {
  user: fallbackUsers[0],
  account: { type: 'Checking', accountNumber: '99129832', lastFour: '9832', balance: 128704.12 },
  transactions: [
    { id: 1, description: 'Payroll deposit', type: 'DEPOSIT', amount: 4825.00, createdAt: '2026-08-16T09:14:00' },
    { id: 2, description: 'OneAhead card payment', type: 'TRANSFER', amount: 124.42, createdAt: '2026-08-15T16:38:00' },
    { id: 3, description: 'Market Square Grocer', type: 'TRANSFER', amount: 86.19, createdAt: '2026-08-14T11:07:00' },
    { id: 4, description: 'Cash back reward', type: 'DEPOSIT', amount: 72.35, createdAt: '2026-08-13T08:22:00' },
  ],
};

const markets = [
  { label: 'NASDAQ', value: '21,629.77', change: '+0.42%' },
  { label: 'S&P 500', value: '6,449.15', change: '+0.18%' },
  { label: 'DOW', value: '45,127.84', change: '-0.07%' },
];

function currency(value) {
  return Number(value || 0).toLocaleString(undefined, { style: 'currency', currency: 'USD' });
}

function dateLabel(value) {
  if (!value) return 'Today';
  return new Date(value).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

function Login({ onLogin }) {
  const [users, setUsers] = useState(fallbackUsers);
  const [username, setUsername] = useState('tbrady');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    api.get('/api/demo/users')
      .then((response) => setUsers(response.data.length ? response.data : fallbackUsers))
      .catch(() => setUsers(fallbackUsers));
  }, []);

  const fillUser = (user) => {
    setUsername(user.username);
    setPassword(user.password);
    setMessage('');
  };

  const submit = async (event) => {
    event.preventDefault();
    try {
      const response = await api.post('/api/demo/login', { username, password });
      onLogin(response.data);
    } catch (err) {
      const fallback = users.find((user) => user.username === username && user.password === password);
      if (fallback) {
        onLogin({ ...fallbackAccount, user: fallback, account: { ...fallbackAccount.account, lastFour: fallback.lastFour } });
        return;
      }
      setMessage(err.response?.data?.message || 'Demo login failed');
    }
  };

  return (
    <main className="login-shell">
      <section className="login-panel">
        <div className="brand-lockup">
          <div className="brand-mark">1A</div>
          <div>
            <h1>OneAhead Bank</h1>
            <p>Demo banking workspace</p>
          </div>
        </div>
        <form onSubmit={submit}>
          <label>Username<input value={username} onChange={(event) => setUsername(event.target.value)} /></label>
          <label>Password<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          <button className="btn" type="submit">Sign In</button>
        </form>
        {message && <div className="error">{message}</div>}
      </section>
      <section className="user-picker">
        {users.map((user) => (
          <button className="user-card" key={user.username} onClick={() => fillUser(user)}>
            <span>{user.name}</span>
            <small>{user.city}, {user.state}</small>
            <strong>{user.username}</strong>
          </button>
        ))}
      </section>
    </main>
  );
}

function Shell({ session, setSession }) {
  const location = useLocation();
  const navigate = useNavigate();
  const intervalRef = useRef(null);
  const [showProfile, setShowProfile] = useState(false);
  const [loadLevel, setLoadLevelState] = useState(() => Number(localStorage.getItem('oneahead-load-level') || 3));
  const [stats, setStats] = useState({ sent: 0, ok: 0, failed: 0 });
  const [lastError, setLastError] = useState('');
  const links = [
    ['/', 'Overview'],
    ['/deposit', 'Deposit'],
    ['/transfer', 'Transfer'],
    ['/credit', 'Credit'],
  ];

  useEffect(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    if (loadLevel <= 0) return undefined;
    intervalRef.current = setInterval(() => runLoadBatch(loadLevel), 1000);
    return () => clearInterval(intervalRef.current);
  }, [loadLevel]);

  const setLoadLevel = (nextLevel) => {
    localStorage.setItem('oneahead-load-level', String(nextLevel));
    setLoadLevelState(nextLevel);
  };

  const runLoadBatch = async (count) => {
    const calls = Array.from({ length: count }, () => api.post('/api/demo/transactions/random'));
    const results = await Promise.allSettled(calls);
    const ok = results.filter((result) => result.status === 'fulfilled').length;
    const failed = results.length - ok;
    setStats((current) => ({ sent: current.sent + results.length, ok: current.ok + ok, failed: current.failed + failed }));
    const rejected = results.find((result) => result.status === 'rejected');
    if (rejected) setLastError(rejected.reason?.message || 'Load request failed');
  };

  return (
    <div className="App">
      <header className="topbar">
        <Link className="brand-mini" to="/">
          <span className="brand-mark small">1A</span>
          <span>OneAhead Bank</span>
        </Link>
        <nav className="nav">
          {links.map(([to, label]) => <Link key={to} to={to} className={location.pathname === to ? 'active' : ''}>{label}</Link>)}
        </nav>
        <div className="right-actions">
          <button className="customer-chip" onClick={() => setShowProfile(true)}>Hi, {session.user.firstName}</button>
          <button className="icon-button" aria-label="Settings" title="Settings" onClick={() => navigate('/admin')}>⚙</button>
          <button className="btn secondary compact" onClick={() => setSession(null)}>Sign Out</button>
        </div>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<Dashboard session={session} setSession={setSession} />} />
          <Route path="/deposit" element={<Deposit />} />
          <Route path="/transfer" element={<Transfer />} />
          <Route path="/credit" element={<CreditCheck />} />
          <Route path="/admin" element={<Controls loadLevel={loadLevel} setLoadLevel={setLoadLevel} stats={stats} setStats={setStats} lastError={lastError} setLastError={setLastError} />} />
        </Routes>
      </main>
      {showProfile && <AccountModal session={session} onClose={() => setShowProfile(false)} />}
    </div>
  );
}

function Dashboard({ session, setSession }) {
  const [showDetails, setShowDetails] = useState(false);
  const [showProfile, setShowProfile] = useState(false);

  const refresh = async () => {
    try {
      const response = await api.get(`/api/demo/account/${session.user.username}`);
      setSession(response.data);
    } catch {
      setSession(session);
    }
  };

  return (
    <section className="dashboard-layout">
      <div className="left-stack">
        <article className="account-hero">
          <p className="eyebrow">{session.account.type}</p>
          <h2>{currency(session.account.balance)}</h2>
          <p>Account ending {session.account.lastFour}</p>
          <div className="hero-actions">
            <button className="btn" onClick={() => setShowDetails((current) => !current)}>Account Details</button>
            <button className="btn secondary" onClick={refresh}>Refresh</button>
          </div>
          {showDetails && (
            <div className="details-grid">
              <span>Owner <strong>{session.user.name}</strong></span>
              <span>Location <strong>{session.user.city}, {session.user.state}</strong></span>
              <span>Account <strong>{session.account.accountNumber}</strong></span>
            </div>
          )}
        </article>
        <Transactions transactions={session.transactions} />
      </div>
      <aside className="right-stack">
        <MarketPanel />
        <ApplyCard onOpen={() => setShowProfile(true)} />
      </aside>
      {showProfile && <AccountModal session={session} onClose={() => setShowProfile(false)} />}
    </section>
  );
}

function Transactions({ transactions }) {
  return (
    <article className="panel">
      <div className="section-head">
        <div>
          <p className="eyebrow">Activity</p>
          <h2>Recent Transactions</h2>
        </div>
      </div>
      <div className="transaction-list">
        {(transactions || []).slice(0, 6).map((transaction) => {
          const isDeposit = transaction.type === 'DEPOSIT';
          return (
            <div className="transaction-row" key={transaction.id || transaction.description}>
              <div>
                <strong>{transaction.description}</strong>
                <span>{dateLabel(transaction.createdAt)}</span>
              </div>
              <b className={isDeposit ? 'positive' : ''}>{isDeposit ? '+' : '-'}{currency(transaction.amount)}</b>
            </div>
          );
        })}
      </div>
    </article>
  );
}

function MarketPanel() {
  return (
    <article className="panel market-data">
      <p className="eyebrow">Markets</p>
      {markets.map((market) => (
        <div className="market-row" key={market.label}>
          <span>{market.label}</span>
          <strong>{market.value}</strong>
          <b className={market.change.startsWith('-') ? 'negative' : 'positive'}>{market.change}</b>
        </div>
      ))}
    </article>
  );
}

function ApplyCard({ onOpen }) {
  return (
    <article className="panel apply-card">
      <p className="eyebrow">Credit Card</p>
      <h2>Apply for a OneAhead card</h2>
      <p>Review account details before starting a demo application.</p>
      <button className="btn" onClick={onOpen}>Apply</button>
    </article>
  );
}

function Deposit() {
  const [amount, setAmount] = useState('100');
  const [metadata, setMetadata] = useState('mobile deposit');
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

  return <ActionPanel title="Deposit" submit={submit} message={message}>
    <label>Amount<input type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} /></label>
    <label>Memo<input value={metadata} onChange={(event) => setMetadata(event.target.value)} /></label>
  </ActionPanel>;
}

function Transfer() {
  const [toAccount, setToAccount] = useState('2002');
  const [amount, setAmount] = useState('25');
  const [metadata, setMetadata] = useState('checking transfer');
  const [message, setMessage] = useState('');

  const submit = async (event) => {
    event.preventDefault();
    try {
      const response = await api.post('/api/account/transfer', { toAccount, amount: Number(amount), metadata });
      setMessage(`Transfer ${response.data.transactionId} complete`);
    } catch (err) {
      setMessage('Transfer failed: ' + (err.response?.data?.message || err.message));
    }
  };

  return <ActionPanel title="Transfer" submit={submit} message={message}>
    <label>To Account<input value={toAccount} onChange={(event) => setToAccount(event.target.value)} /></label>
    <label>Amount<input type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} /></label>
    <label>Memo<input value={metadata} onChange={(event) => setMetadata(event.target.value)} /></label>
  </ActionPanel>;
}

function ActionPanel({ title, submit, message, children }) {
  return (
    <section className="panel action-panel">
      <h2>{title}</h2>
      <form onSubmit={submit}>
        {children}
        <button className="btn" type="submit">{title}</button>
      </form>
      {message && <div className={message.includes('failed') ? 'error' : 'success'}>{message}</div>}
    </section>
  );
}

function CreditCheck() {
  const [ssn, setSsn] = useState('123-45-6789');
  const [metadata, setMetadata] = useState('credit review');
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

  const history = result?.history || [];

  return (
    <section className="credit-layout">
      <article className="panel action-panel">
        <h2>Credit Check</h2>
        <form onSubmit={submit}>
          <label>SSN<input value={ssn} onChange={(event) => setSsn(event.target.value)} /></label>
          <label>Memo<input value={metadata} onChange={(event) => setMetadata(event.target.value)} /></label>
          <button className="btn" type="submit">Check Credit</button>
        </form>
        {error && <div className="error">{error}</div>}
      </article>
      <article className="panel credit-card">
        <p className="eyebrow">Score</p>
        <h2>{result?.creditScore || '--'}</h2>
        <span>{result?.status || 'Ready'}</span>
      </article>
      <article className="panel wide-panel">
        <div className="section-head">
          <div>
            <p className="eyebrow">History</p>
            <h2>Credit History</h2>
          </div>
        </div>
        <div className="history-grid">
          {history.slice(-12).map((item) => (
            <div className="history-item" key={item.date}>
              <strong>{item.score}</strong>
              <span>{item.date}</span>
              <small>{item.description}</small>
            </div>
          ))}
        </div>
      </article>
    </section>
  );
}

function Controls({ loadLevel, setLoadLevel, stats, setStats, lastError, setLastError }) {
  const [configs, setConfigs] = useState({});

  const loadConfigs = async () => {
    const response = await api.get('/api/admin/configs');
    const next = {};
    response.data.forEach((item) => { next[item.configKey] = item.configValue; });
    setConfigs(next);
  };

  useEffect(() => {
    loadConfigs().catch((err) => setLastError(err.message));
  }, []);

  const updateConfig = async (key, value, description) => {
    await api.post('/api/admin/configs', { key, value: String(value), description });
    await loadConfigs();
  };

  const toggle = (key) => configs[key] === 'true';

  return (
    <section className="controls-grid">
      <article className="panel wide-panel">
        <div className="section-head">
          <div>
            <p className="eyebrow">Controls</p>
            <h2>Load Driver</h2>
          </div>
          <button className="btn secondary compact" onClick={() => setStats({ sent: 0, ok: 0, failed: 0 })}>Reset</button>
        </div>
        <label>Request groups per second: {loadLevel}
          <input type="range" min="0" max="40" value={loadLevel} onChange={(event) => setLoadLevel(Number(event.target.value))} />
        </label>
        <div className="stat-row">
          <span>Sent <strong>{stats.sent}</strong></span>
          <span>OK <strong>{stats.ok}</strong></span>
          <span>Failed <strong>{stats.failed}</strong></span>
        </div>
        {lastError && <div className="error">{lastError}</div>}
      </article>
      <ProblemToggle title="CPU Burn" detail={`${configs['problem.cpu.millis'] || 250}ms per request`} enabled={toggle('problem.cpu.enabled')} onToggle={(enabled) => updateConfig('problem.cpu.enabled', enabled, 'Built-in CPU burn problem')}>
        <label>Millis<input type="number" min="25" max="5000" value={configs['problem.cpu.millis'] || 250} onChange={(event) => updateConfig('problem.cpu.millis', event.target.value, 'CPU burn duration per request')} /></label>
      </ProblemToggle>
      <ProblemToggle title="Slow SQL" detail={`${configs['sql.slow.delay'] || 0}s on deposit and transfer`} enabled={toggle('sql.slow.enabled')} onToggle={(enabled) => updateConfig('sql.slow.enabled', enabled, 'Slow SQL simulation')}>
        <label>Seconds<input type="number" min="0" max="60" value={configs['sql.slow.delay'] || 2} onChange={(event) => updateConfig('sql.slow.delay', event.target.value, 'Slow SQL delay in seconds')} /></label>
      </ProblemToggle>
      <ProblemToggle title="Slow Credit" detail={`${configs['credit.slow.delay'] || 0}s before credit call`} enabled={toggle('credit.slow.enabled')} onToggle={(enabled) => updateConfig('credit.slow.enabled', enabled, 'Slow credit check simulation')}>
        <label>Seconds<input type="number" min="0" max="60" value={configs['credit.slow.delay'] || 2} onChange={(event) => updateConfig('credit.slow.delay', event.target.value, 'Slow credit check delay in seconds')} /></label>
      </ProblemToggle>
      <ProblemToggle title="404 Errors" detail="Return 404 from banking and credit proxy endpoints" enabled={toggle('error.404.enabled')} onToggle={(enabled) => updateConfig('error.404.enabled', enabled, '404 error simulation')} />
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
          <input type="checkbox" checked={enabled} onChange={(event) => onToggle(event.target.checked)} />
          <span />
        </label>
      </div>
      {children && <div className="problem-controls">{children}</div>}
    </article>
  );
}

function AccountModal({ session, onClose }) {
  const { user, account } = session;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <section className="account-modal" onClick={(event) => event.stopPropagation()}>
        <div className="section-head">
          <div>
            <p className="eyebrow">Account</p>
            <h2>{user.name}</h2>
          </div>
          <button className="icon-button light" aria-label="Close" onClick={onClose}>x</button>
        </div>
        <div className="modal-grid">
          <span>Username <strong>{user.username}</strong></span>
          <span>Address <strong>{user.address}</strong></span>
          <span>City <strong>{user.city}, {user.state}</strong></span>
          <span>Account <strong>{account.type} ending {account.lastFour}</strong></span>
          <span>Balance <strong>{currency(account.balance)}</strong></span>
        </div>
      </section>
    </div>
  );
}

function App() {
  const [session, setSession] = useState(null);

  return (
    <Router>
      {session ? <Shell session={session} setSession={setSession} /> : <Login onLogin={setSession} />}
    </Router>
  );
}

export default App;
