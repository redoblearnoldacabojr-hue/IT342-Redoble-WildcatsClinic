/* eslint-disable react/prop-types */
import { useMemo, useState } from 'react';

export default function AppointmentsPage({ user, appointments = [], loading = false, errorMessage = '', onNew }) {
  const [view, setView] = useState('month');

  const myAppointments = useMemo(() => (
    appointments.filter(a => String(a.patientId) === String(user?.userId))
  ), [appointments, user]);

  const filtered = useMemo(() => {
    const now = new Date();
    if (view === 'day') {
      const today = now.toISOString().slice(0,10);
      return myAppointments.filter(a => a.date === today);
    }
    if (view === 'week') {
      const end = new Date(now.getTime());
      end.setDate(end.getDate() + 7);
      return myAppointments.filter(a => {
        const d = new Date(a.date + 'T' + a.time);
        return d >= now && d <= end;
      });
    }
    // month
    const month = now.getMonth();
    const year = now.getFullYear();
    return myAppointments.filter(a => {
      const d = new Date(a.date + 'T' + a.time);
      return d.getMonth() === month && d.getFullYear() === year;
    });
  }, [view, myAppointments]);

  return (
    <div className="section-shell">
      <div className="section-header">
        <div>
          <h1 style={{ margin: 0 }}>My Appointments</h1>
          <p style={{ margin: 0, color: '#6b7280' }}>View only your appointments and switch between calendar scopes.</p>
        </div>
        <div style={{ display: 'flex', gap: '.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', gap: '.25rem' }}>
            <button onClick={() => setView('month')} className={view === 'month' ? 'primary-btn' : 'secondary-btn'}>Month</button>
            <button onClick={() => setView('week')} className={view === 'week' ? 'primary-btn' : 'secondary-btn'}>Week</button>
            <button onClick={() => setView('day')} className={view === 'day' ? 'primary-btn' : 'secondary-btn'}>Day</button>
          </div>
          <button className="primary-btn" onClick={onNew}>+ New Appointment</button>
        </div>
      </div>

      <div className="surface-panel" style={{ marginTop: '1rem' }}>
        {loading && <p>Loading appointments...</p>}
        {!loading && errorMessage && <p style={{ color: '#b91c1c' }}>{errorMessage}</p>}
        {!loading && !errorMessage && filtered.length === 0 && <p>No appointments for the selected view.</p>}
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {filtered.sort((a,b) => new Date(a.date + 'T' + a.time) - new Date(b.date + 'T' + b.time)).map((a) => (
            <li key={a.id} className="list-card">
              <div style={{ fontWeight: 700 }}>{a.reason}</div>
              <div style={{ color: '#6b7280' }}>{new Date(a.date + 'T' + a.time).toLocaleString()}</div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
