/* eslint-disable react/prop-types */
import { useEffect, useMemo, useState } from 'react';

const appointmentStatusOptions = ['PROCESSING', 'APPROVED', 'CANCELED', 'COMPLETED'];

function formatLabel(value) {
  return String(value || 'PROCESSING').toLowerCase().replaceAll('_', ' ');
}

function formatDateTime(date, time) {
  if (!date || !time) {
    return 'Unknown';
  }

  const parsed = new Date(`${date}T${time}`);
  return Number.isNaN(parsed.getTime()) ? `${date} ${time}` : parsed.toLocaleString();
}

export default function AppointmentDetailsModal({
  show,
  appointment,
  doctors = [],
  canManage = false,
  onClose,
  onSave,
}) {
  const [status, setStatus] = useState('PROCESSING');
  const [doctorId, setDoctorId] = useState('');
  const [remarks, setRemarks] = useState('');
  const [results, setResults] = useState('');
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const availableDoctors = useMemo(() => doctors.filter((doctor) => doctor.doctorIn ?? doctor.available), [doctors]);
  const selectedDoctor = useMemo(() => doctors.find((doctor) => String(doctor.id) === String(doctorId)), [doctorId, doctors]);

  useEffect(() => {
    if (!show || !appointment) {
      return;
    }

    setStatus(appointment.status || 'PROCESSING');
    setDoctorId(appointment.doctorId ? String(appointment.doctorId) : '');
    setRemarks(appointment.completionRemarks || '');
    setResults(appointment.completionResults || '');
    setError('');
  }, [appointment, show]);

  if (!show || !appointment) {
    return null;
  }

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!canManage) {
      onClose();
      return;
    }

    if (status === 'COMPLETED' && !doctorId) {
      setError('Please assign a doctor before completing this appointment.');
      return;
    }

    try {
      setIsSaving(true);
      await onSave(appointment.id, {
        status,
        doctorId: doctorId ? Number(doctorId) : null,
        remarks,
        results,
      });
      onClose();
    } catch (saveError) {
      setError(saveError?.message || 'Unable to save appointment changes.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <form className="modal-card appointment-modal appointment-details-modal" onSubmit={handleSubmit}>
        <div className="appointment-modal-hero">
          <div className="appointment-modal-hero-copy">
            <div className="appointment-modal-eyebrow">Appointment Details</div>
            <h3 className="appointment-modal-title">{appointment.patientName || appointment.patientEmail || 'Patient Appointment'}</h3>
            <p className="appointment-modal-email">{appointment.patientEmail || 'Unknown email'}</p>
            <p className="appointment-modal-subtitle">
              {appointment.reason || 'Appointment'} · {formatDateTime(appointment.date, appointment.time)}
            </p>
          </div>
          <div className="appointment-modal-hero-meta">
            <span className="appointment-modal-chip">{formatLabel(status)}</span>
            <button type="button" onClick={onClose} className="icon-btn appointment-modal-close" aria-label="Close appointment details">✕</button>
          </div>
        </div>

        <div className="modal-summary-grid">
          <div className="modal-summary-card">
            <span>Status</span>
            <strong>{formatLabel(status)}</strong>
          </div>
          <div className="modal-summary-card">
            <span>Scheduled</span>
            <strong>{formatDateTime(appointment.date, appointment.time)}</strong>
          </div>
          <div className="modal-summary-card">
            <span>Doctor</span>
            <strong>{appointment.doctorName || selectedDoctor?.name || 'Unassigned'}</strong>
          </div>
        </div>

        <div className="appointment-modal-grid">
          <section className="appointment-modal-section">
            <div className="appointment-modal-section-title">Overview</div>
            <div className="modal-detail-grid">
              <div className="modal-detail-card">
                <span>Reason</span>
                <strong>{appointment.reason || 'Appointment'}</strong>
              </div>
              <div className="modal-detail-card">
                <span>Patient</span>
                <strong>{appointment.patientName || 'Patient'}</strong>
              </div>
              <div className="modal-detail-card">
                <span>Completed</span>
                <strong>{appointment.completedAt ? new Date(appointment.completedAt).toLocaleString() : 'Not completed'}</strong>
              </div>
            </div>

            <div className="modal-notes-grid">
              <div className="modal-notes-card">
                <span>Remarks</span>
                <p>{appointment.completionRemarks || remarks || 'No remarks yet.'}</p>
              </div>
              <div className="modal-notes-card">
                <span>Results</span>
                <p>{appointment.completionResults || results || 'No results yet.'}</p>
              </div>
            </div>
          </section>

          <section className="appointment-modal-section appointment-modal-section-accent">
            <div className="appointment-modal-section-title">Management</div>
            {canManage ? (
              <div className="modal-edit-grid">
                <div>
                  <label htmlFor="appointment-status" className="modal-field-label">Status</label>
                  <select
                    id="appointment-status"
                    className="field-input"
                    value={status}
                    onChange={(event) => setStatus(event.target.value)}
                  >
                    {appointmentStatusOptions.map((option) => (
                      <option key={option} value={option}>{formatLabel(option)}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label htmlFor="appointment-doctor" className="modal-field-label">Assign Doctor</label>
                  <select
                    id="appointment-doctor"
                    className="field-input"
                    value={doctorId}
                    onChange={(event) => setDoctorId(event.target.value)}
                  >
                    <option value="">Select doctor</option>
                    {availableDoctors.map((doctor) => (
                      <option key={doctor.id} value={doctor.id}>
                        {doctor.name} (IN)
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label htmlFor="appointment-remarks" className="modal-field-label">Remarks</label>
                  <textarea
                    id="appointment-remarks"
                    className="field-input modal-textarea"
                    value={remarks}
                    onChange={(event) => setRemarks(event.target.value)}
                    placeholder="Add follow-up notes or observations"
                  />
                </div>

                <div>
                  <label htmlFor="appointment-results" className="modal-field-label">Results</label>
                  <textarea
                    id="appointment-results"
                    className="field-input modal-textarea"
                    value={results}
                    onChange={(event) => setResults(event.target.value)}
                    placeholder="Add diagnosis, next steps, or treatment results"
                  />
                </div>
              </div>
            ) : (
              <div className="modal-view-note">
                This appointment is read-only for your current role.
              </div>
            )}

            {error && <div className="modal-error">{error}</div>}
          </section>
        </div>

        <div className="modal-actions-row">
          <button type="button" onClick={onClose} className="secondary-btn">Close</button>
          {canManage && (
            <button type="submit" className="primary-btn" disabled={isSaving}>
              {isSaving ? 'Saving...' : 'Save Changes'}
            </button>
          )}
        </div>
      </form>
    </div>
  );
}
