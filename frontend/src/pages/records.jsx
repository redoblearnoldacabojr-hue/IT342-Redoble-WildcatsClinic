/* eslint-disable react/prop-types */
import { useEffect, useMemo, useState } from 'react';
import { getStoredToken } from '../api/auth';

function emptyDraft(record = null) {
  return {
    title: record?.title || '',
    summary: record?.summary || '',
    date: record?.date || '',
    remarks: record?.remarks || '',
    results: record?.results || '',
  };
}

export default function RecordsPage({ user, canManage = false, canDelete = false }) {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [selectedRecord, setSelectedRecord] = useState(null);
  const [draft, setDraft] = useState(emptyDraft());
  const [isSaving, setIsSaving] = useState(false);
  const [actionError, setActionError] = useState('');

  const pageTitle = canManage ? 'Record Management' : 'My Records';
  const pageDescription = canManage
    ? 'Review, update, and manage patient records. Only admin users can delete records.'
    : 'View the patient records and history here.';

  const selectedRecordLabel = useMemo(() => {
    if (!selectedRecord) {
      return '';
    }

    return `${selectedRecord.title || 'Record'} · ${selectedRecord.date || 'Unknown date'}`;
  }, [selectedRecord]);

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

  const openEdit = (record) => {
    setSelectedRecord(record);
    setDraft(emptyDraft(record));
    setActionError('');
  };

  const closeEdit = () => {
    setSelectedRecord(null);
    setDraft(emptyDraft());
    setActionError('');
  };

  const handleSave = async (event) => {
    event.preventDefault();
    if (!selectedRecord || !canManage) {
      return;
    }

    try {
      setIsSaving(true);
      setActionError('');
      const token = getStoredToken();
      const res = await fetch(`/api/records/${selectedRecord.id}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(draft),
      });

      const data = await res.json().catch(() => null);
      if (!res.ok) {
        throw new Error(data?.message || 'Unable to save record changes.');
      }

      setRecords((current) => current.map((record) => (record.id === data.id ? data : record)));
      closeEdit();
    } catch (saveError) {
      setActionError(saveError.message || 'Unable to save record changes.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (record) => {
    if (!canDelete) {
      return;
    }

    const confirmed = globalThis.confirm(`Delete ${record.title || 'this record'}?`);
    if (!confirmed) {
      return;
    }

    try {
      setActionError('');
      const token = getStoredToken();
      const res = await fetch(`/api/records/${record.id}`, {
        method: 'DELETE',
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });

      if (!res.ok) {
        const message = await res.text().catch(() => 'Unable to delete record.');
        throw new Error(message || 'Unable to delete record.');
      }

      setRecords((current) => current.filter((item) => item.id !== record.id));
      if (selectedRecord?.id === record.id) {
        closeEdit();
      }
    } catch (deleteError) {
      setActionError(deleteError.message || 'Unable to delete record.');
    }
  };

  return (
    <div className="section-shell">
      <div className="section-header">
        <div>
          <h1 style={{ margin: 0 }}>{pageTitle}</h1>
          <p style={{ margin: 0, color: '#6b7280' }}>{pageDescription}</p>
        </div>
      </div>

      <div className="surface-panel" style={{ marginTop: '1rem' }}>
        {loading && <p>Loading records...</p>}
        {!loading && errorMessage && <p style={{ color: '#b91c1c' }}>{errorMessage}</p>}
        {!loading && !errorMessage && records.length === 0 && <p>No records available.</p>}
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {records.map((r) => (
            <li key={r.id} className="list-card record-card">
              <div className="record-card-head">
                <div>
                  <div style={{ fontWeight: 700 }}>{r.title}</div>
                  <div style={{ color: '#6b7280' }}>{r.date}</div>
                </div>
                <div className="record-card-actions">
                  {r.doctorName && <span className="role-pill role-2">{r.doctorName}</span>}
                  {canManage && <button type="button" className="secondary-btn" onClick={() => openEdit(r)}>Edit</button>}
                  {canDelete && <button type="button" className="secondary-btn danger-btn" onClick={() => handleDelete(r)}>Delete</button>}
                </div>
              </div>
              <div style={{ marginTop: '.5rem' }}>{r.summary}</div>
              {(r.remarks || r.results || r.completedAt) && (
                <div className="record-meta-grid">
                  {r.completedAt && <div><span>Completed</span><strong>{new Date(r.completedAt).toLocaleString()}</strong></div>}
                  {r.remarks && <div><span>Remarks</span><strong>{r.remarks}</strong></div>}
                  {r.results && <div><span>Results</span><strong>{r.results}</strong></div>}
                </div>
              )}
            </li>
          ))}
        </ul>
      </div>

      {selectedRecord && canManage && (
        <div className="modal-backdrop">
          <form className="modal-card appointment-modal records-modal" onSubmit={handleSave}>
            <div className="appointment-modal-hero">
              <div className="appointment-modal-hero-copy">
                <div className="appointment-modal-eyebrow">Record Management</div>
                <h3 className="appointment-modal-title">Edit Record</h3>
                <p className="appointment-modal-subtitle">{selectedRecordLabel}</p>
              </div>
              <button type="button" onClick={closeEdit} className="icon-btn appointment-modal-close" aria-label="Close record editor">✕</button>
            </div>

            <div className="modal-summary-grid">
              <div className="modal-summary-card"><span>Doctor</span><strong>{selectedRecord.doctorName || 'Unassigned'}</strong></div>
              <div className="modal-summary-card"><span>Completed</span><strong>{selectedRecord.completedAt ? new Date(selectedRecord.completedAt).toLocaleString() : 'Not completed'}</strong></div>
              <div className="modal-summary-card"><span>Appointment</span><strong>{selectedRecord.appointmentId || 'N/A'}</strong></div>
            </div>

            <div className="appointment-modal-grid booking-modal-grid">
              <section className="appointment-modal-section">
                <div className="appointment-modal-section-title">Details</div>
                <div className="booking-form-grid">
                  <div>
                    <label className="modal-field-label" htmlFor="record-title">Title</label>
                    <input id="record-title" className="field-input" value={draft.title} onChange={(event) => setDraft((current) => ({ ...current, title: event.target.value }))} />
                  </div>
                  <div>
                    <label className="modal-field-label" htmlFor="record-date">Date</label>
                    <input id="record-date" className="field-input" value={draft.date} onChange={(event) => setDraft((current) => ({ ...current, date: event.target.value }))} />
                  </div>
                  <div>
                    <label className="modal-field-label" htmlFor="record-summary">Summary</label>
                    <textarea id="record-summary" className="field-input modal-textarea" value={draft.summary} onChange={(event) => setDraft((current) => ({ ...current, summary: event.target.value }))} />
                  </div>
                </div>
              </section>

              <section className="appointment-modal-section appointment-modal-section-accent">
                <div className="appointment-modal-section-title">Clinical Notes</div>
                <div className="booking-form-grid">
                  <div>
                    <label className="modal-field-label" htmlFor="record-remarks">Remarks</label>
                    <textarea id="record-remarks" className="field-input modal-textarea" value={draft.remarks} onChange={(event) => setDraft((current) => ({ ...current, remarks: event.target.value }))} />
                  </div>
                  <div>
                    <label className="modal-field-label" htmlFor="record-results">Results</label>
                    <textarea id="record-results" className="field-input modal-textarea" value={draft.results} onChange={(event) => setDraft((current) => ({ ...current, results: event.target.value }))} />
                  </div>
                </div>
                {actionError && <div className="modal-error">{actionError}</div>}
              </section>
            </div>

            <div className="modal-actions-row">
              <button type="button" className="secondary-btn" onClick={closeEdit}>Cancel</button>
              <button type="submit" className="primary-btn" disabled={isSaving}>{isSaving ? 'Saving...' : 'Save Changes'}</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
