/* eslint-disable react/prop-types */
import { useEffect, useMemo, useState } from 'react';
import { getStoredToken } from '../api/auth';

function formatDateForInput(d) {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

function formatTimeForInput(d) {
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${hh}:${mm}`;
}

export default function AppointmentModal({ show, onClose, user, existingAppointments = [], onCreate }) {
  const [date, setDate] = useState('');
  const [time, setTime] = useState('09:00');
  const [reason, setReason] = useState('General Checkup');
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (show) {
      setError('');
      const now = new Date();
      setDate(formatDateForInput(now));
      // default to next available hour
      const nextHour = new Date(now.getTime());
      nextHour.setMinutes(0);
      nextHour.setHours(nextHour.getHours() + 1);
      setTime(formatTimeForInput(nextHour));
      setReason('General Checkup');
    }
  }, [show]);

  const minDate = useMemo(() => formatDateForInput(new Date()), []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!date || !time) {
      setError('Please select date and time.');
      return;
    }

    const chosen = new Date(date + 'T' + time);
    const now = new Date();
    if (chosen < now) {
      setError('Cannot book a time in the past.');
      return;
    }

    const conflict = existingAppointments.find((appointment) => (
      String(appointment.patientId) === String(user?.userId)
      && appointment.date === date
      && appointment.time === time
    ));
    if (conflict) {
      setError('You already have an appointment at that date and time.');
      return;
    }

    try {
      setIsSaving(true);
      const token = getStoredToken();
      const resp = await fetch('/api/appointments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ date, time, reason }),
      });

      const data = await resp.json().catch(() => null);
      if (!resp.ok) {
        setError(data?.message || 'Failed to book appointment');
        return;
      }

      onCreate(data);
    } catch (err) {
      setError(err.message || 'Failed to book appointment');
    } finally {
      setIsSaving(false);
    }
  };

  if (!show) return null;

  return (
    <div className="modal-backdrop">
      <form onSubmit={handleSubmit} className="modal-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0 }}>New Appointment</h3>
          <button type="button" onClick={onClose} className="icon-btn">✕</button>
        </div>

        <div style={{ marginTop: '.75rem' }}>
          <label style={{ display: 'block', marginBottom: '.25rem' }}>Patient</label>
          <input value={`${user?.firstName || ''} ${user?.lastName || ''}`.trim()} readOnly className="field-input" />
        </div>

        <div style={{ display: 'flex', gap: '.5rem', marginTop: '.75rem' }}>
          <div style={{ flex: 1 }}>
            <label style={{ display: 'block', marginBottom: '.25rem' }}>Pick a date</label>
            <input type="date" value={date} onChange={(e) => setDate(e.target.value)} min={minDate} className="field-input" />
          </div>
          <div style={{ width: 160 }}>
            <label style={{ display: 'block', marginBottom: '.25rem' }}>Time</label>
            <input type="time" value={time} onChange={(e) => setTime(e.target.value)} className="field-input" />
          </div>
        </div>

        <div style={{ marginTop: '.75rem' }}>
          <label style={{ display: 'block', marginBottom: '.25rem' }}>Reason</label>
          <input value={reason} onChange={(e) => setReason(e.target.value)} className="field-input" />
        </div>

        {error && <div style={{ color: '#b91c1c', marginTop: '.5rem' }}>{error}</div>}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '.5rem', marginTop: '1rem' }}>
          <button type="button" onClick={onClose} className="secondary-btn">Cancel</button>
          <button type="submit" className="primary-btn" disabled={isSaving}>{isSaving ? 'Booking...' : 'Book'}</button>
        </div>
      </form>
    </div>
  );
}
