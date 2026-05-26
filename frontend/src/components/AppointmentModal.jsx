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
  const [reason, setReason] = useState('General Consultation');
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const reasonOptions = ['General Consultation', 'Medical Services', 'Dental Service'];

  useEffect(() => {
    if (show) {
      setError('');
      const now = new Date();
      setDate(formatDateForInput(now));
      // default to next available hour
      const nextHour = new Date(now);
      nextHour.setMinutes(0);
      nextHour.setHours(nextHour.getHours() + 1);
      setTime(formatTimeForInput(nextHour));
      setReason('General Consultation');
    }
  }, [show]);

  const minDate = useMemo(() => formatDateForInput(new Date()), []);
  const currentUserId = user?.userId ?? user?.id ?? null;

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
      currentUserId != null && String(appointment.patientId) === String(currentUserId)
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
      <form onSubmit={handleSubmit} className="modal-card appointment-modal booking-modal">
        <div className="appointment-modal-hero">
          <div className="appointment-modal-hero-copy">
            <div className="appointment-modal-eyebrow">New Appointment</div>
            <h3 className="appointment-modal-title">Schedule a Visit</h3>
            <p className="appointment-modal-subtitle">Choose a reason, date, and time for the patient visit.</p>
          </div>
          <button type="button" onClick={onClose} className="icon-btn appointment-modal-close" aria-label="Close new appointment form">✕</button>
        </div>

        <div className="modal-summary-grid booking-summary-grid">
          <div className="modal-summary-card">
            <span>Patient</span>
            <strong>{user?.email || `${user?.firstName || ''} ${user?.lastName || ''}`.trim()}</strong>
          </div>
          <div className="modal-summary-card">
            <span>Default Reason</span>
            <strong>General Consultation</strong>
          </div>
          <div className="modal-summary-card">
            <span>Time Window</span>
            <strong>Next available hour</strong>
          </div>
        </div>

        <div className="appointment-modal-grid booking-modal-grid">
          <section className="appointment-modal-section">
            <div className="appointment-modal-section-title">Visit Details</div>
            <div className="booking-form-grid">
              <div>
                <label htmlFor="appointment-patient" className="modal-field-label">Patient</label>
                <input id="appointment-patient" value={user?.email || `${user?.firstName || ''} ${user?.lastName || ''}`.trim()} readOnly className="field-input" />
              </div>

              <div className="booking-date-grid">
                <div>
                  <label htmlFor="appointment-date" className="modal-field-label">Pick a date</label>
                  <input id="appointment-date" type="date" value={date} onChange={(e) => setDate(e.target.value)} min={minDate} className="field-input" />
                </div>
                <div>
                  <label htmlFor="appointment-time" className="modal-field-label">Time</label>
                  <input id="appointment-time" type="time" value={time} onChange={(e) => setTime(e.target.value)} className="field-input" />
                </div>
              </div>

              <div>
                <label htmlFor="appointment-reason" className="modal-field-label">Reason</label>
                <select id="appointment-reason" value={reason} onChange={(e) => setReason(e.target.value)} className="field-input">
                  {reasonOptions.map((option) => (
                    <option key={option} value={option}>{option}</option>
                  ))}
                </select>
              </div>
            </div>
          </section>

          <section className="appointment-modal-section appointment-modal-section-accent">
            <div className="appointment-modal-section-title">Preview</div>
            <div className="booking-preview-card">
              <span>Selected Reason</span>
              <strong>{reason}</strong>
              <p>{date && time ? `${date} at ${time}` : 'Pick a schedule to continue.'}</p>
            </div>
            {error && <div className="modal-error">{error}</div>}
          </section>
        </div>

        <div className="modal-actions-row">
          <button type="button" onClick={onClose} className="secondary-btn">Cancel</button>
          <button type="submit" className="primary-btn" disabled={isSaving}>{isSaving ? 'Booking...' : 'Book'}</button>
        </div>
      </form>
    </div>
  );
}
