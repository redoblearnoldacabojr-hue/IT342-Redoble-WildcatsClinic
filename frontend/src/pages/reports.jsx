/* eslint-disable react/prop-types */

function formatCount(value) {
  return Number(value || 0).toLocaleString();
}

export default function ReportsPage({ summary = {} }) {
  const statusCards = [
    { label: 'Processing', value: summary.processingAppointments || 0 },
    { label: 'Approved', value: summary.approvedAppointments || 0 },
    { label: 'Completed', value: summary.completedAppointments || 0 },
    { label: 'Canceled', value: summary.canceledAppointments || 0 },
  ];

  const doctorUtilization = Array.isArray(summary.doctorUtilization) ? summary.doctorUtilization : [];
  const maxCount = doctorUtilization.reduce((max, item) => Math.max(max, Number(item.assignedAppointments || 0)), 0) || 1;

  return (
    <div className="reports-layout">
      <section className="reports-grid">
        <div className="report-card"><span>Appointments</span><strong>{formatCount(summary.totalAppointments)}</strong></div>
        <div className="report-card"><span>Records</span><strong>{formatCount(summary.totalRecords)}</strong></div>
        <div className="report-card"><span>Doctors</span><strong>{formatCount(summary.totalDoctors)}</strong></div>
        <div className="report-card"><span>Doctors In</span><strong>{formatCount(summary.doctorsIn ?? summary.availableDoctors)}</strong></div>
      </section>

      <section className="surface-panel report-panel">
        <div className="section-header compact">
          <div>
            <div className="calendar-eyebrow">Appointments</div>
            <h2 style={{ margin: 0 }}>Appointment Status Mix</h2>
          </div>
        </div>

        <div className="status-stack">
          {statusCards.map((card) => (
            <div key={card.label} className="status-row">
              <span>{card.label}</span>
              <strong>{formatCount(card.value)}</strong>
            </div>
          ))}
        </div>
      </section>

      <section className="surface-panel report-panel">
        <div className="section-header compact">
          <div>
            <div className="calendar-eyebrow">Doctors</div>
            <h2 style={{ margin: 0 }}>Doctor Utilization</h2>
          </div>
        </div>

        <div className="utilization-list">
          {doctorUtilization.length === 0 ? (
            <p style={{ margin: 0, color: '#6b7280' }}>No doctors have been assigned yet.</p>
          ) : doctorUtilization.map((doctor) => {
            const count = Number(doctor.assignedAppointments || 0);
            const width = Math.max(8, Math.round((count / maxCount) * 100));

            return (
              <div key={doctor.id} className="utilization-item">
                <div className="utilization-head">
                  <strong>{doctor.name}</strong>
                  <span>{doctor.specialization}</span>
                </div>
                <div className="utilization-bar">
                  <div className="utilization-fill" style={{ width: `${width}%` }} />
                </div>
                <small>{count} assigned appointment{count === 1 ? '' : 's'}</small>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}