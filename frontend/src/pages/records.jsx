/* eslint-disable react/prop-types */
import { useEffect, useState } from 'react';
import { getStoredToken } from '../api/auth';

export default function RecordsPage({ user }) {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setErrorMessage('');
        const token = getStoredToken();
        const res = await fetch('/api/records', {
          method: 'GET',
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        });
        if (res.ok) {
          const list = await res.json();
          setRecords(list || []);
        } else {
          setErrorMessage('Unable to load records right now.');
          setRecords([]);
        }
      } catch {
        setErrorMessage('Unable to load records right now.');
        setRecords([]);
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [user]);

  return (
    <div className="section-shell">
      <div className="section-header">
        <div>
          <h1 style={{ margin: 0 }}>My Records</h1>
          <p style={{ margin: 0, color: '#6b7280' }}>View the patient's records and history here.</p>
        </div>
      </div>

      <div className="surface-panel" style={{ marginTop: '1rem' }}>
        {loading && <p>Loading records...</p>}
        {!loading && errorMessage && <p style={{ color: '#b91c1c' }}>{errorMessage}</p>}
        {!loading && !errorMessage && records.length === 0 && <p>No records available.</p>}
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {records.map((r) => (
          <li key={r.id} className="list-card">
            <div style={{ fontWeight: 700 }}>{r.title}</div>
            <div style={{ color: '#6b7280' }}>{r.date}</div>
            <div style={{ marginTop: '.25rem' }}>{r.summary}</div>
          </li>
        ))}
        </ul>
      </div>
    </div>
  );
}
