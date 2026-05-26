/* eslint-disable react/prop-types */
import { useMemo, useState } from 'react';
import AppointmentDetailsModal from '../components/AppointmentDetailsModal';

function toDateTimeValue(appointment) {
  return new Date(`${appointment.date}T${appointment.time}`);
}

function formatDateTime(appointment) {
  const parsed = toDateTimeValue(appointment);
  return Number.isNaN(parsed.getTime()) ? `${appointment.date} ${appointment.time}` : parsed.toLocaleString();
}

function formatStatus(status) {
  return String(status || 'PROCESSING').toLowerCase().replaceAll('_', ' ');
}

function sortByDateTime(left, right) {
  return toDateTimeValue(left) - toDateTimeValue(right);
}

function startOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function endOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0);
}

function buildMonthCells(monthDate) {
  const firstDay = startOfMonth(monthDate);
  const lastDay = endOfMonth(monthDate);
  const cells = [];
  const firstWeekday = firstDay.getDay();

  for (let index = 0; index < firstWeekday; index += 1) {
    cells.push(null);
  }

  for (let day = 1; day <= lastDay.getDate(); day += 1) {
    cells.push(new Date(monthDate.getFullYear(), monthDate.getMonth(), day));
  }

  while (cells.length % 7 !== 0) {
    cells.push(null);
  }

  return cells;
}

function sameDay(left, right) {
  return left && right
    && left.getFullYear() === right.getFullYear()
    && left.getMonth() === right.getMonth()
    && left.getDate() === right.getDate();
}

export default function AppointmentsPage({ user, appointments = [], doctors = [], loading = false, errorMessage = '', onNew, onSaveAppointment }) {
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [currentMonth, setCurrentMonth] = useState(() => startOfMonth(new Date()));
  const [selectedDay, setSelectedDay] = useState(() => new Date());

  const currentUserId = user?.userId ?? user?.id ?? null;
  const canManageAppointments = Number(user?.role ?? 1) >= 2;

  const visibleAppointments = useMemo(() => {
    if (canManageAppointments || !currentUserId) {
      return appointments;
    }

    return appointments.filter((appointment) => String(appointment.patientId) === String(currentUserId));
  }, [appointments, canManageAppointments, currentUserId]);

  const sortedAppointments = useMemo(() => [...visibleAppointments].sort(sortByDateTime), [visibleAppointments]);

  const pendingAppointments = useMemo(() => (
    sortedAppointments.filter((appointment) => {
      const status = String(appointment.status || 'PROCESSING').toUpperCase();
      return status === 'PROCESSING' || status === 'APPROVED';
    })
  ), [sortedAppointments]);

  const completedAppointments = useMemo(() => (
    sortedAppointments.filter((appointment) => String(appointment.status || 'PROCESSING').toUpperCase() === 'COMPLETED')
  ), [sortedAppointments]);

  const canceledAppointments = useMemo(() => (
    sortedAppointments.filter((appointment) => String(appointment.status || 'PROCESSING').toUpperCase() === 'CANCELED')
  ), [sortedAppointments]);

  const calendarAppointments = useMemo(() => (
    visibleAppointments
      .map((appointment) => ({ ...appointment, dt: toDateTimeValue(appointment) }))
      .sort((left, right) => left.dt - right.dt)
  ), [visibleAppointments]);

  const monthCells = useMemo(() => buildMonthCells(currentMonth), [currentMonth]);
  const selectedDayAppointments = useMemo(() => (
    calendarAppointments.filter((appointment) => sameDay(appointment.dt, selectedDay))
  ), [calendarAppointments, selectedDay]);

  const monthLabel = currentMonth.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });

  const moveMonth = (direction) => {
    setCurrentMonth((current) => {
      const next = new Date(current);
      next.setMonth(next.getMonth() + direction);
      return startOfMonth(next);
    });
  };

  const openAppointment = (appointment) => {
    setSelectedAppointment(appointment);
  };

  const renderAppointmentCard = (appointment) => {
    const status = String(appointment.status || 'PROCESSING').toUpperCase();

    return (
      <button
        key={appointment.id}
        type="button"
        className="appointment-card"
        onClick={() => openAppointment(appointment)}
      >
        <div className="appointment-card-head">
          <div>
            <h3>{appointment.patientName || appointment.patientEmail || 'Patient'}</h3>
            <p>{formatDateTime(appointment)}</p>
          </div>
          <span className={`appointment-status appointment-status-${status.toLowerCase()}`}>{formatStatus(status)}</span>
        </div>

        <div className="appointment-card-meta">
          <div>
            <span>Doctor</span>
            <strong>{appointment.doctorName || 'Unassigned'}</strong>
          </div>
          <div>
            <span>Reason</span>
            <strong>{appointment.reason || 'Appointment'}</strong>
          </div>
          <div>
            <span>Completed</span>
            <strong>{appointment.completedAt ? new Date(appointment.completedAt).toLocaleString() : 'Not completed'}</strong>
          </div>
        </div>

        <div className="appointment-card-notes">
          <div>
            <span>Remarks</span>
            <p>{appointment.completionRemarks || 'No remarks yet.'}</p>
          </div>
          <div>
            <span>Results</span>
            <p>{appointment.completionResults || 'No results yet.'}</p>
          </div>
        </div>
      </button>
    );
  };

  const renderCalendarCell = (date) => {
    if (!date) {
      return <div className="calendar-cell calendar-cell-muted" aria-hidden="true" />;
    }

    const dayAppointments = calendarAppointments.filter((appointment) => sameDay(appointment.dt, date));
    const isSelected = sameDay(date, selectedDay);
    const isToday = sameDay(date, new Date());

    return (
      <button
        type="button"
        className={`calendar-cell calendar-cell-button ${isSelected ? 'calendar-cell-today' : ''} ${isToday ? 'calendar-cell-today' : ''}`}
        onClick={() => setSelectedDay(date)}
      >
        <div className="calendar-cell-header">
          <strong>{date.getDate()}</strong>
          <span className="calendar-pill">{dayAppointments.length}</span>
        </div>
        <div className="calendar-cell-events">
          {dayAppointments.slice(0, 2).map((appointment) => (
            <div key={appointment.id} className="calendar-event calendar-event-mini">
              <strong>{formatStatus(String(appointment.status || 'PROCESSING').toUpperCase())}</strong>
              <span>{appointment.reason || 'Appointment'}</span>
            </div>
          ))}
          {dayAppointments.length > 2 && <div className="calendar-more">+{dayAppointments.length - 2} more</div>}
        </div>
      </button>
    );
  };

  if (!canManageAppointments) {
    return (
      <div className="section-shell">
        <div className="section-header appointments-header">
          <div>
            <h1 style={{ margin: 0 }}>Appointments</h1>
            <p style={{ margin: 0, color: '#6b7280' }}>Calendar view of your booked appointments.</p>
          </div>
          <div style={{ display: 'flex', gap: '.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <button className="primary-btn" onClick={onNew}>+ New Appointment</button>
          </div>
        </div>

        <section className="surface-panel calendar-agenda-panel">
          <div className="calendar-toolbar">
            <div>
              <div className="calendar-eyebrow">Calendar</div>
              <h2 style={{ margin: 0 }}>{monthLabel}</h2>
              <p style={{ margin: '.2rem 0 0', color: '#6b7280' }}>Select a day to inspect appointment details.</p>
            </div>
            <div className="calendar-nav">
              <button type="button" className="secondary-btn" onClick={() => moveMonth(-1)}>Previous</button>
              <button type="button" className="secondary-btn" onClick={() => { setCurrentMonth(startOfMonth(new Date())); setSelectedDay(new Date()); }}>Today</button>
              <button type="button" className="secondary-btn" onClick={() => moveMonth(1)}>Next</button>
            </div>
          </div>

          {loading && <p>Loading appointments...</p>}
          {!loading && errorMessage && <p style={{ color: '#b91c1c' }}>{errorMessage}</p>}

          {!loading && !errorMessage && (
            <div className="calendar-month-grid">
              {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day) => (
                <div key={day} className="calendar-weekday">{day}</div>
              ))}
              {monthCells.map((date, index) => (
                <div key={date ? date.toISOString() : `blank-${index}`}>
                  {renderCalendarCell(date)}
                </div>
              ))}
            </div>
          )}

          <div className="calendar-day-details">
            <div className="section-header compact">
              <div>
                <div className="calendar-eyebrow">Selected Day</div>
                <h3 style={{ margin: 0 }}>{selectedDay.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })}</h3>
              </div>
            </div>
            {selectedDayAppointments.length === 0 ? (
              <p style={{ margin: 0, color: '#6b7280' }}>No appointments on this day.</p>
            ) : (
              <div className="calendar-day-list">
                {selectedDayAppointments.map((appointment) => {
                  const status = String(appointment.status || 'PROCESSING').toUpperCase();
                  return (
                    <button key={appointment.id} type="button" className="calendar-day-item calendar-cell-button" onClick={() => openAppointment(appointment)}>
                      <div className="calendar-day-item-main">
                        <strong>{appointment.reason || 'Appointment'}</strong>
                        <span>{appointment.doctorName || 'Unassigned'} · {appointment.time}</span>
                      </div>
                      <div className="calendar-day-item-controls">
                        <span className={`appointment-status appointment-status-${status.toLowerCase()}`}>{formatStatus(status)}</span>
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </section>

        <AppointmentDetailsModal
          show={Boolean(selectedAppointment)}
          appointment={selectedAppointment}
          doctors={doctors}
          canManage={false}
          onClose={() => setSelectedAppointment(null)}
          onSave={onSaveAppointment}
        />
      </div>
    );
  }

  return (
    <div className="section-shell">
      <div className="section-header appointments-header">
        <div>
          <h1 style={{ margin: 0 }}>Appointments</h1>
          <p style={{ margin: 0, color: '#6b7280' }}>A detailed view for current pending and completed appointments.</p>
        </div>
        <div style={{ display: 'flex', gap: '.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <button className="primary-btn" onClick={onNew}>+ New Appointment</button>
        </div>
      </div>

      <section className="surface-panel appointments-summary-panel">
        <div className="appointments-summary-grid">
          <div className="overview-card">
            <div className="overview-label">Pending</div>
            <div className="overview-value">{pendingAppointments.length}</div>
          </div>
          <div className="overview-card">
            <div className="overview-label">Completed</div>
            <div className="overview-value">{completedAppointments.length}</div>
          </div>
          <div className="overview-card">
            <div className="overview-label">Canceled</div>
            <div className="overview-value">{canceledAppointments.length}</div>
          </div>
          <div className="overview-card">
            <div className="overview-label">Total Visible</div>
            <div className="overview-value">{sortedAppointments.length}</div>
          </div>
        </div>
      </section>

      <div className="appointments-board">
        <section className="surface-panel appointment-column">
          <div className="section-header compact">
            <div>
              <div className="calendar-eyebrow">Current</div>
              <h2 style={{ margin: 0 }}>Pending Appointments</h2>
            </div>
          </div>

          {loading && <p>Loading appointments...</p>}
          {!loading && errorMessage && <p style={{ color: '#b91c1c' }}>{errorMessage}</p>}
          {!loading && !errorMessage && pendingAppointments.length === 0 && <p style={{ margin: 0, color: '#6b7280' }}>No pending appointments right now.</p>}
          {!loading && !errorMessage && pendingAppointments.length > 0 && (
            <div className="appointment-card-list">
              {pendingAppointments.map(renderAppointmentCard)}
            </div>
          )}
        </section>

        <section className="surface-panel appointment-column">
          <div className="section-header compact">
            <div>
              <div className="calendar-eyebrow">Archive</div>
              <h2 style={{ margin: 0 }}>Completed Appointments</h2>
            </div>
          </div>

          {loading && <p>Loading appointments...</p>}
          {!loading && errorMessage && <p style={{ color: '#b91c1c' }}>{errorMessage}</p>}
          {!loading && !errorMessage && completedAppointments.length === 0 && <p style={{ margin: 0, color: '#6b7280' }}>No completed appointments yet.</p>}
          {!loading && !errorMessage && completedAppointments.length > 0 && (
            <div className="appointment-card-list">
              {completedAppointments.map(renderAppointmentCard)}
            </div>
          )}
        </section>
      </div>

      {!loading && !errorMessage && canceledAppointments.length > 0 && (
        <section className="surface-panel appointment-canceled-panel">
          <div className="section-header compact">
            <div>
              <div className="calendar-eyebrow">Other</div>
              <h2 style={{ margin: 0 }}>Canceled Appointments</h2>
            </div>
          </div>
          <div className="appointment-card-list compact-list">
            {canceledAppointments.map(renderAppointmentCard)}
          </div>
        </section>
      )}

      <AppointmentDetailsModal
        show={Boolean(selectedAppointment)}
        appointment={selectedAppointment}
        doctors={doctors}
        canManage={canManageAppointments}
        onClose={() => setSelectedAppointment(null)}
        onSave={onSaveAppointment}
      />
    </div>
  );
}
