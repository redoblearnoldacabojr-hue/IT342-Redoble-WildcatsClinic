/* eslint-disable react/prop-types */
import { useEffect, useMemo, useState } from 'react';
import { clearAuth, getCurrentUser, getStoredToken, logoutUser } from '../api/auth';
import { apiFetch } from '../api/http';
import AppointmentModal from '../components/AppointmentModal';
import AppointmentDetailsModal from '../components/AppointmentDetailsModal';
import AppointmentsPage from './appointments';
import RecordsPage from './records';
import StaffManagementPage from './staffManagement';
import ReportsPage from './reports';
import clinicLogo from '../logo/Logo.png';

function getDisplayName(profile) {
  const fullName = [profile?.firstName, profile?.lastName].filter(Boolean).join(' ').trim();
  if (fullName && !/^\d+$/.test(fullName)) {
    return fullName;
  }

  if (profile?.email) {
    return profile.email;
  }

  return 'Your account';
}

function getShortLabel(profile) {
  if (profile?.email) {
    return profile.email.split('@')[0];
  }

  return getDisplayName(profile);
}

function getRoleLabel(role) {
  const roleNumber = Number(role || 1);
  if (roleNumber >= 3) {
    return 'Admin';
  }

  if (roleNumber >= 2) {
    return 'Staff';
  }

  return 'User';
}

function formatDateTime(appointment) {
  const parsed = new Date(`${appointment.date}T${appointment.time}`);
  return Number.isNaN(parsed.getTime()) ? `${appointment.date} ${appointment.time}` : parsed.toLocaleString();
}

function getAppointmentTitle(appointment) {
  return appointment.reason || 'Appointment';
}

function getAppointmentStatus(appointment) {
  return String(appointment.status || 'PROCESSING').toUpperCase();
}

function sortAppointmentsBySchedule(left, right) {
  return new Date(`${left.date}T${left.time}`) - new Date(`${right.date}T${right.time}`);
}

function Dashboard({ onLogout }) {
  const [profile, setProfile] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [page, setPage] = useState('dashboard');
  const [showModal, setShowModal] = useState(false);
  const [appointments, setAppointments] = useState([]);
  const [appointmentsLoading, setAppointmentsLoading] = useState(true);
  const [appointmentsError, setAppointmentsError] = useState('');
  const [notifications, setNotifications] = useState([]);
  const [notificationsLoading, setNotificationsLoading] = useState(true);
  const [notificationsError, setNotificationsError] = useState('');
  const [showNotificationsModal, setShowNotificationsModal] = useState(false);
  const [staffUsers, setStaffUsers] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [reportSummary, setReportSummary] = useState(null);
  const [managementLoading, setManagementLoading] = useState(true);
  const [managementError, setManagementError] = useState('');

  useEffect(() => {
    const loadManagementData = async (token) => {
      const [usersRes, doctorsRes, reportsRes] = await Promise.all([
        apiFetch('/api/users', {
          method: 'GET',
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        }),
        apiFetch('/api/doctors', {
          method: 'GET',
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        }),
        apiFetch('/api/reports/summary', {
          method: 'GET',
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        }),
      ]);

      if (usersRes.ok) {
        setStaffUsers(await usersRes.json());
      } else {
        setStaffUsers([]);
      }

      if (doctorsRes.ok) {
        setDoctors(await doctorsRes.json());
      } else {
        setDoctors([]);
      }

      if (reportsRes.ok) {
        setReportSummary(await reportsRes.json());
      } else {
        setReportSummary(null);
      }
    };

    const loadProfile = async () => {
      try {
        const me = await getCurrentUser();
        setProfile(me);
        const token = getStoredToken();
        const isPrivileged = Number(me?.role || 1) >= 2;

        // load appointments from backend
        try {
          setAppointmentsLoading(true);
          setAppointmentsError('');
          const res = await apiFetch('/api/appointments', {
            method: 'GET',
            headers: {
              ...(token ? { Authorization: `Bearer ${token}` } : {}),
            },
          });
          if (res.ok) {
            const list = await res.json();
            setAppointments(list || []);
          } else {
            setAppointmentsError('Unable to load appointments right now.');
            setAppointments([]);
          }

          const notificationsRes = await apiFetch('/api/notifications', {
            method: 'GET',
            headers: {
              ...(token ? { Authorization: `Bearer ${token}` } : {}),
            },
          });
          if (notificationsRes.ok) {
            const notificationList = await notificationsRes.json();
            setNotifications(notificationList || []);
          } else {
            setNotificationsError('Unable to load notifications right now.');
            setNotifications([]);
          }
        } catch {
          setAppointmentsError('Unable to load appointments right now.');
          setAppointments([]);
          setNotificationsError('Unable to load notifications right now.');
          setNotifications([]);
        } finally {
          setAppointmentsLoading(false);
          setNotificationsLoading(false);
        }

        if (isPrivileged) {
          try {
            setManagementLoading(true);
            setManagementError('');
            await loadManagementData(token);
          } catch {
            setManagementError('Unable to load staff dashboard data right now.');
            setStaffUsers([]);
            setDoctors([]);
            setReportSummary(null);
          } finally {
            setManagementLoading(false);
          }
        } else {
          setStaffUsers([]);
          setDoctors([]);
          setReportSummary(null);
          setManagementLoading(false);
        }
      } catch (error) {
        if (error.status === 401 || error.status === 403) {
          clearAuth();
          onLogout();
          return;
        }

        setErrorMessage(error.message || 'Failed to load account details.');
      } finally {
        setIsLoading(false);
      }
    };

    loadProfile();
  }, [onLogout]);

  const refreshManagementData = async () => {
    if (Number(profile?.role || 1) < 2) {
      return;
    }

    const token = getStoredToken();
    setManagementLoading(true);
    try {
      const [usersRes, doctorsRes, reportsRes] = await Promise.all([
        apiFetch('/api/users', {
          method: 'GET',
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        }),
        apiFetch('/api/doctors', {
          method: 'GET',
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        }),
        apiFetch('/api/reports/summary', {
          method: 'GET',
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        }),
      ]);

      setStaffUsers(usersRes.ok ? await usersRes.json() : []);
      setDoctors(doctorsRes.ok ? await doctorsRes.json() : []);
      setReportSummary(reportsRes.ok ? await reportsRes.json() : null);
      setManagementError('');
    } catch {
      setManagementError('Unable to refresh staff dashboard data right now.');
    } finally {
      setManagementLoading(false);
    }
  };

  const handleCreateAppointment = (appt) => {
    setAppointments((current) => [...current, appt]);
    setShowModal(false);
    setPage('appointments');
  };

  const markNotificationsRead = async () => {
    const unreadCount = notifications.filter((notification) => !notification.isRead).length;
    if (unreadCount === 0) {
      return;
    }

    const token = getStoredToken();
    const response = await apiFetch('/api/notifications/read-all', {
      method: 'PATCH',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });

    if (response.ok) {
      setNotifications((current) => current.map((notification) => ({ ...notification, isRead: true })));
    }
  };

  const handleOpenNotifications = async () => {
    setShowNotificationsModal(true);
    try {
      await markNotificationsRead();
    } catch {
      // Best effort: modal still opens even if read-state update fails.
    }
  };

  const handleUpdateAppointmentStatus = async (appointmentId, payload) => {
    const token = getStoredToken();
    const response = await apiFetch(`/api/appointments/${appointmentId}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(payload),
    });

    const data = await response.json().catch(() => null);
    if (!response.ok) {
      const message = data?.message || 'Unable to update appointment status.';
      throw new Error(message);
    }

    setAppointments((current) => current.map((appointment) => (appointment.id === data.id ? data : appointment)));
    await refreshManagementData();
    return data;
  };

  const handleSaveDoctor = async (doctorId, payload) => {
    const token = getStoredToken();
    const method = doctorId ? 'PATCH' : 'POST';
    const endpoint = doctorId ? `/api/doctors/${doctorId}` : '/api/doctors';

    const response = await apiFetch(endpoint, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(payload),
    });

    const data = await response.json().catch(() => null);
    if (!response.ok) {
      const message = data?.message || 'Unable to save doctor.';
      throw new Error(message);
    }

    await refreshManagementData();
    return data;
  };

  const handleRemoveDoctor = async (doctorId) => {
    const token = getStoredToken();
    const response = await apiFetch(`/api/doctors/${doctorId}`, {
      method: 'DELETE',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });

    if (!response.ok) {
      const message = await response.text().catch(() => 'Unable to remove doctor.');
      throw new Error(message || 'Unable to remove doctor.');
    }

    await refreshManagementData();
  };

  const handleChangeUserRole = async (userId, role) => {
    const token = getStoredToken();
    const response = await apiFetch(`/api/users/${userId}/role`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ role }),
    });

    const data = await response.text();
    if (!response.ok) {
      throw new Error(data || 'Unable to update user role.');
    }

    await refreshManagementData();
    return data;
  };

  const handleSignOut = async () => {
    await logoutUser();
    clearAuth();
    onLogout();
  };

  if (isLoading) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: '1rem' }}>
        <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '12px', padding: '1.5rem', maxWidth: '460px', width: '100%' }}>
          <h2 style={{ marginTop: 0 }}>Loading your dashboard...</h2>
        </div>
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: '1rem' }}>
        <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '12px', padding: '1.5rem', maxWidth: '460px', width: '100%' }}>
          <h2 style={{ marginTop: 0 }}>Dashboard</h2>
          <p style={{ color: '#b91c1c' }}>{errorMessage}</p>
          <button type="button" className="submit-btn" onClick={handleSignOut}>Logout</button>
        </div>
      </div>
    );
  }

  const upcoming = appointments
    .map(a => ({ ...a, dt: new Date(a.date + 'T' + a.time) }))
    .filter(a => a.dt >= new Date())
    .sort((x, y) => x.dt - y.dt);

  const nextAppointment = upcoming[0];
  const displayName = getDisplayName(profile);
  const shortLabel = getShortLabel(profile);
  const hasStaffAccess = Number(profile?.role || 1) >= 2;
  const isAdmin = Number(profile?.role || 1) >= 3;
  const unreadNotifications = notifications.filter((notification) => !notification.isRead).length;
  const notificationButtonLabel = unreadNotifications > 0
    ? `Open notifications, ${unreadNotifications} unread`
    : 'Open notifications';
  const activePage = hasStaffAccess || ['dashboard', 'appointments', 'records'].includes(page) ? page : 'dashboard';

  return (
    <PortalShell
      profile={profile}
      page={page}
      setPage={setPage}
      appointments={appointments}
      appointmentsLoading={appointmentsLoading}
      appointmentsError={appointmentsError}
      notifications={notifications}
      notificationsLoading={notificationsLoading}
      notificationsError={notificationsError}
      showModal={showModal}
      setShowModal={setShowModal}
      showNotificationsModal={showNotificationsModal}
      setShowNotificationsModal={setShowNotificationsModal}
      staffUsers={staffUsers}
      doctors={doctors}
      reportSummary={reportSummary}
      hasStaffAccess={hasStaffAccess}
      isAdmin={isAdmin}
      displayName={displayName}
      shortLabel={shortLabel}
      unreadNotifications={unreadNotifications}
      notificationButtonLabel={notificationButtonLabel}
      nextAppointment={nextAppointment}
      activePage={activePage}
      handleSignOut={handleSignOut}
      handleOpenNotifications={handleOpenNotifications}
      handleCreateAppointment={handleCreateAppointment}
      handleUpdateAppointmentStatus={handleUpdateAppointmentStatus}
      handleSaveDoctor={handleSaveDoctor}
      handleRemoveDoctor={handleRemoveDoctor}
      handleChangeUserRole={handleChangeUserRole}
      managementLoading={managementLoading}
      managementError={managementError}
    />
  );
}

function PortalShell({
  profile,
  page,
  setPage,
  appointments,
  appointmentsLoading,
  appointmentsError,
  notifications,
  notificationsLoading,
  notificationsError,
  showModal,
  setShowModal,
  showNotificationsModal,
  setShowNotificationsModal,
  staffUsers,
  doctors,
  reportSummary,
  hasStaffAccess,
  isAdmin,
  displayName,
  shortLabel,
  unreadNotifications,
  notificationButtonLabel,
  nextAppointment,
  activePage,
  handleSignOut,
  handleOpenNotifications,
  handleCreateAppointment,
  handleUpdateAppointmentStatus,
  handleSaveDoctor,
  handleRemoveDoctor,
  handleChangeUserRole,
  managementLoading,
  managementError,
}) {
  const [selectedAppointment, setSelectedAppointment] = useState(null);

  const openAppointmentDetails = (appointment) => {
    setSelectedAppointment(appointment);
  };

  return (
    <div className="dashboard-shell">
      <aside className="dashboard-sidebar">
        <div>
          <div className="dashboard-brand">
            <div className="dashboard-brand-mark dashboard-brand-logo-wrap">
              <img src={clinicLogo} alt="Wildcat Clinic logo" className="dashboard-brand-logo" />
            </div>
            <div>
              <div style={{ fontSize: '.8rem', letterSpacing: '.12em', textTransform: 'uppercase', opacity: 0.75 }}>Wildcat Clinic</div>
              <h2 style={{ margin: 0 }}>{hasStaffAccess ? 'Staff Portal' : 'Patient Portal'}</h2>
            </div>
          </div>
          <nav style={{ display: 'flex', flexDirection: 'column', gap: '.5rem', marginTop: '1.25rem' }}>
            <button className={`nav-btn ${page === 'dashboard' ? 'active' : ''}`} onClick={() => setPage('dashboard')}>Dashboard</button>
            <button className={`nav-btn ${page === 'appointments' ? 'active' : ''}`} onClick={() => setPage('appointments')}>Appointments</button>
            {hasStaffAccess && <button className={`nav-btn ${page === 'staff' ? 'active' : ''}`} onClick={() => setPage('staff')}>Staff Management</button>}
            {hasStaffAccess && <button className={`nav-btn ${page === 'reports' ? 'active' : ''}`} onClick={() => setPage('reports')}>Reports</button>}
            <button className={`nav-btn ${page === 'records' ? 'active' : ''}`} onClick={() => setPage('records')}>{hasStaffAccess ? 'Record Management' : 'My Records'}</button>
          </nav>
        </div>

        <div className="dashboard-user-card">
          <div style={{ display: 'flex', alignItems: 'center', gap: '.75rem' }}>
            <div className="dashboard-avatar">
              {shortLabel?.[0]?.toUpperCase() || 'U'}
            </div>
            <div>
              <div style={{ fontWeight: 700, wordBreak: 'break-word' }}>{displayName}</div>
              <div style={{ fontSize: '.85rem', opacity: 0.9, wordBreak: 'break-word' }}>{profile?.email}</div>
              <div style={{ fontSize: '.75rem', opacity: 0.75, textTransform: 'uppercase', letterSpacing: '.1em' }}>{getRoleLabel(profile?.role)}</div>
            </div>
          </div>

          <button type="button" className="submit-btn" style={{ marginTop: '1rem' }} onClick={handleSignOut}>Sign Out</button>
        </div>
      </aside>

      <PortalMainContent
        profile={profile}
        page={page}
        setPage={setPage}
        appointments={appointments}
        appointmentsLoading={appointmentsLoading}
        appointmentsError={appointmentsError}
        doctors={doctors}
        staffUsers={staffUsers}
        reportSummary={reportSummary}
        hasStaffAccess={hasStaffAccess}
        isAdmin={isAdmin}
        displayName={displayName}
        unreadNotifications={unreadNotifications}
        notificationButtonLabel={notificationButtonLabel}
        nextAppointment={nextAppointment}
        activePage={activePage}
        managementLoading={managementLoading}
        managementError={managementError}
        handleOpenNotifications={handleOpenNotifications}
        handleUpdateAppointmentStatus={handleUpdateAppointmentStatus}
        handleSaveDoctor={handleSaveDoctor}
        handleRemoveDoctor={handleRemoveDoctor}
        handleChangeUserRole={handleChangeUserRole}
        openAppointmentDetails={openAppointmentDetails}
        setShowModal={setShowModal}
      />

      <AppointmentModal
        show={showModal}
        onClose={() => setShowModal(false)}
        user={profile}
        existingAppointments={appointments}
        onCreate={handleCreateAppointment}
      />

      <AppointmentDetailsModal
        show={Boolean(selectedAppointment)}
        appointment={selectedAppointment}
        doctors={doctors}
        canManage={hasStaffAccess}
        onClose={() => setSelectedAppointment(null)}
        onSave={handleUpdateAppointmentStatus}
      />

      {showNotificationsModal && (
        <div className="modal-layer">
          <button type="button" className="notifications-backdrop" aria-label="Close notifications" onClick={() => setShowNotificationsModal(false)} />
          <div className="modal-card notification-modal">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem' }}>
              <div>
                <h3 style={{ margin: 0 }}>Notifications</h3>
                <p style={{ margin: 0, color: '#6b7280' }}>Appointment updates and status changes.</p>
              </div>
              <button type="button" onClick={() => setShowNotificationsModal(false)} className="icon-btn">✕</button>
            </div>

            <div style={{ marginTop: '1rem' }}>
              {notificationsLoading && <p>Loading notifications...</p>}
              {notificationsError && <p style={{ color: '#b91c1c' }}>{notificationsError}</p>}
              {!notificationsLoading && !notificationsError && notifications.length === 0 && <p>No notifications yet.</p>}
              <ul className="notification-list">
                {notifications.map((notification) => (
                  <li key={notification.id} className={`notification-card ${notification.isRead ? 'notification-card-read' : 'notification-card-unread'}`}>
                    <div className="notification-row">
                      <div style={{ fontWeight: 700 }}>{notification.message}</div>
                      {!notification.isRead && <span className="notification-unread-dot" aria-hidden="true" />}
                    </div>
                    <div style={{ color: '#6b7280' }}>{notification.createdAt ? new Date(notification.createdAt).toLocaleString() : ''}</div>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function StaffAppointmentBoard({ label, title, appointments: appointmentList, emptyMessage, onSelect, showCompletedAt = false }) {
  return (
    <section className="appointment-column">
      <div className="section-header compact">
        <div>
          <div className="calendar-eyebrow">{label}</div>
          <h3 style={{ margin: 0 }}>{title}</h3>
        </div>
      </div>
      {appointmentList.length === 0 ? (
        <p style={{ margin: 0, color: '#6b7280' }}>{emptyMessage}</p>
      ) : (
        <div className="appointment-card-list">
          {appointmentList.slice(0, 5).map((appointment) => {
            const status = getAppointmentStatus(appointment);
            return (
              <button key={appointment.id} type="button" className="appointment-card dashboard-appointment-card" onClick={() => onSelect(appointment)}>
                <div className="appointment-card-head">
                  <div>
                    <h3>{getAppointmentTitle(appointment)}</h3>
                    <p>{formatDateTime(appointment)}</p>
                  </div>
                  <span className={`appointment-status appointment-status-${status.toLowerCase()}`}>{status.toLowerCase()}</span>
                </div>
                <div className="appointment-card-meta">
                  <div><span>Patient</span><strong>{appointment.patientName || appointment.patientEmail || 'Patient'}</strong></div>
                  <div><span>Doctor</span><strong>{appointment.doctorName || 'Unassigned'}</strong></div>
                  <div>
                    <span>{showCompletedAt ? 'Completed' : 'Reason'}</span>
                    <strong>{(() => {
                      if (showCompletedAt) {
                        return appointment.completedAt ? new Date(appointment.completedAt).toLocaleString() : 'Not completed';
                      }

                      return appointment.reason || 'Appointment';
                    })()}</strong>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
}

function PortalMainContent({
  profile,
  page,
  setPage,
  appointments,
  appointmentsLoading,
  appointmentsError,
  doctors,
  staffUsers,
  reportSummary,
  hasStaffAccess,
  isAdmin,
  displayName,
  unreadNotifications,
  notificationButtonLabel,
  nextAppointment,
  activePage,
  managementLoading,
  managementError,
  handleOpenNotifications,
  handleUpdateAppointmentStatus,
  handleSaveDoctor,
  handleRemoveDoctor,
  handleChangeUserRole,
  openAppointmentDetails,
  setShowModal,
}) {
  const pendingAppointments = useMemo(() => appointments
    .filter((appointment) => {
      const status = getAppointmentStatus(appointment);
      return status === 'PROCESSING' || status === 'APPROVED';
    })
    .sort(sortAppointmentsBySchedule), [appointments]);
  const completedAppointments = useMemo(() => appointments
    .filter((appointment) => getAppointmentStatus(appointment) === 'COMPLETED')
    .sort(sortAppointmentsBySchedule), [appointments]);
  const doctorsInCount = useMemo(() => doctors.filter((doctor) => doctor.doctorIn ?? doctor.available).length, [doctors]);

  return (
    <main className="dashboard-main">
      {activePage === 'dashboard' && (
        <div className="dashboard-content">
          <div className="dashboard-hero">
            <div>
              <div className="dashboard-hero-kicker">Welcome back</div>
              <h1 style={{ margin: 0, fontSize: '2rem' }}>{displayName}</h1>
              <p style={{ margin: '.35rem 0 0', color: '#6b7280' }}>{profile?.email}</p>
            </div>
            <div style={{ display: 'flex', gap: '.5rem', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
              <button type="button" className="notification-btn" onClick={handleOpenNotifications} aria-label={notificationButtonLabel}>
                <svg className="notification-bell" aria-hidden="true" viewBox="0 0 24 24" focusable="false">
                  <path d="M12 3a5 5 0 0 0-5 5v2.1c0 .8-.3 1.6-.8 2.2L4.6 14.2c-.4.6 0 1.4.7 1.4h13.4c.7 0 1.1-.8.7-1.4l-1.6-1.9c-.5-.6-.8-1.4-.8-2.2V8a5 5 0 0 0-5-5Z" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
                  <path d="M9.5 18a2.5 2.5 0 0 0 5 0" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                  <circle cx="12" cy="5.1" r="1" fill="currentColor" />
                </svg>
                {unreadNotifications > 0 && <span className="notification-dot" aria-hidden="true" />}
              </button>
              <button className="primary-btn" onClick={() => setShowModal(true)}>Book Appointment</button>
            </div>
          </div>

          {hasStaffAccess ? (
            <section className="dashboard-stats">
              <div className="stat-card"><div className="stat-label">Pending Appointments</div><div className="stat-value">{pendingAppointments.length}</div></div>
              <div className="stat-card"><div className="stat-label">Completed Appointments</div><div className="stat-value">{completedAppointments.length}</div></div>
              <div className="stat-card"><div className="stat-label">Doctors In</div><div className="stat-value">{doctorsInCount}</div></div>
            </section>
          ) : (
            <section className="dashboard-stats dashboard-stats-two">
              <div className="stat-card">
                <div className="stat-label">Next Appointment</div>
                <div className="stat-value">{nextAppointment ? `${new Date(nextAppointment.dt).toLocaleDateString()} ${nextAppointment.time}` : 'No upcoming'}</div>
              </div>
              <div className="stat-card">
                <div className="stat-label">Number of Visits</div>
                <div className="stat-value">{appointments.length}</div>
              </div>
            </section>
          )}

          <section className="dashboard-panel">
            <div className="section-header" style={{ marginBottom: '.75rem' }}>
              <div>
                <h2 style={{ margin: 0 }}>{hasStaffAccess ? 'Appointments' : 'Upcoming Appointments'}</h2>
                <p style={{ margin: 0, color: '#6b7280' }}>{hasStaffAccess ? 'Pending and completed appointments with quick access to details.' : 'A quick view of your booked visits.'}</p>
              </div>
            </div>
            {hasStaffAccess && managementLoading && <p>Loading clinic overview...</p>}
            {hasStaffAccess && managementError && <p style={{ color: '#b91c1c' }}>{managementError}</p>}
            {appointmentsLoading && <p>Loading appointments...</p>}
            {appointmentsError && <p style={{ color: '#b91c1c' }}>{appointmentsError}</p>}
            {!appointmentsLoading && !hasStaffAccess && upcomingList(appointments).length === 0 && <p>No upcoming appointments.</p>}
            {hasStaffAccess ? (
              <div className="appointments-board dashboard-appointments-board">
                <StaffAppointmentBoard
                  label="Current"
                  title="Pending Appointments"
                  appointments={pendingAppointments}
                  emptyMessage="No pending appointments right now."
                  onSelect={openAppointmentDetails}
                  showCompletedAt={false}
                />

                <StaffAppointmentBoard
                  label="Archive"
                  title="Completed Appointments"
                  appointments={completedAppointments}
                  emptyMessage="No completed appointments yet."
                  onSelect={openAppointmentDetails}
                  showCompletedAt
                />
              </div>
            ) : (
              <ul className="appointment-list">
                {upcomingList(appointments).slice(0, 5).map((a) => (
                  <li key={a.id || `${a.date}-${a.time}-${a.reason}`} className="appointment-row">
                    <div style={{ fontWeight: 700 }}>{a.reason || 'Appointment'}</div>
                    <div style={{ color: '#6b7280' }}>{new Date(a.dt).toLocaleString()}</div>
                    <div className={`appointment-status appointment-status-${String(a.status || 'PROCESSING').toLowerCase()}`}>{String(a.status || 'PROCESSING').toLowerCase()}</div>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {hasStaffAccess && reportSummary && (
            <p style={{ margin: 0, color: '#6b7280' }}>Total appointments: {reportSummary.totalAppointments || 0}</p>
          )}
        </div>
      )}

      {activePage === 'appointments' && (
        <AppointmentsPage
          user={profile}
          appointments={appointments}
          doctors={doctors}
          loading={appointmentsLoading}
          errorMessage={appointmentsError}
          onNew={() => setShowModal(true)}
          onSaveAppointment={handleUpdateAppointmentStatus}
        />
      )}

      {activePage === 'staff' && hasStaffAccess && (
        <StaffManagementPage
          users={staffUsers}
          doctors={doctors}
          canChangeRoles={isAdmin}
          onChangeRole={handleChangeUserRole}
          onSaveDoctor={handleSaveDoctor}
          onRemoveDoctor={handleRemoveDoctor}
        />
      )}

      {activePage === 'reports' && hasStaffAccess && (
        <ReportsPage summary={reportSummary || {}} />
      )}

      {activePage === 'records' && (
        <RecordsPage user={profile} canManage={hasStaffAccess} canDelete={isAdmin} />
      )}
    </main>
  );
}

function upcomingList(appointments) {
  return appointments
    .map((appointment) => ({ ...appointment, dt: new Date(`${appointment.date}T${appointment.time}`) }))
    .filter((appointment) => appointment.dt >= new Date())
    .sort((left, right) => left.dt - right.dt);
}

export default Dashboard;
