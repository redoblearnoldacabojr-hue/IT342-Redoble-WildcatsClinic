/* eslint-disable react/prop-types */
import { useEffect, useState } from 'react';
import { clearAuth, getCurrentUser, getStoredToken } from '../api/auth';
import AppointmentModal from '../components/AppointmentModal';
import AppointmentsPage from './appointments';
import RecordsPage from './records';

function Dashboard({ onLogout }) {
  const [profile, setProfile] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [page, setPage] = useState('dashboard');
  const [showModal, setShowModal] = useState(false);
  const [appointments, setAppointments] = useState([]);
  const [appointmentsLoading, setAppointmentsLoading] = useState(true);
  const [appointmentsError, setAppointmentsError] = useState('');

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const me = await getCurrentUser();
        setProfile(me);
        // load appointments from backend
        try {
          setAppointmentsLoading(true);
          setAppointmentsError('');
          const token = getStoredToken();
          const res = await fetch('/api/appointments', {
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
        } catch {
          setAppointmentsError('Unable to load appointments right now.');
          setAppointments([]);
        } finally {
          setAppointmentsLoading(false);
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

  const handleCreateAppointment = (appt) => {
    setAppointments((current) => [...current, appt]);
    setShowModal(false);
    setPage('appointments');
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
          <button type="button" className="submit-btn" onClick={onLogout}>Logout</button>
        </div>
      </div>
    );
  }

  const upcoming = appointments
    .map(a => ({ ...a, dt: new Date(a.date + 'T' + a.time) }))
    .filter(a => a.dt >= new Date())
    .sort((x, y) => x.dt - y.dt);

  const nextAppointment = upcoming[0];

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <aside style={{ width: 260, background: '#6b0b17', color: '#fff', padding: '1.25rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ margin: 0, marginBottom: '1.25rem' }}>Wildcat Clinic</h2>
          <nav style={{ display: 'flex', flexDirection: 'column', gap: '.5rem' }}>
            <button className={`nav-btn ${page === 'dashboard' ? 'active' : ''}`} onClick={() => setPage('dashboard')}>Dashboard</button>
            <button className={`nav-btn ${page === 'appointments' ? 'active' : ''}`} onClick={() => setPage('appointments')}>Appointments</button>
            <button className={`nav-btn ${page === 'records' ? 'active' : ''}`} onClick={() => setPage('records')}>My Records</button>
          </nav>
        </div>

        <div style={{ marginTop: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '.75rem' }}>
            <div style={{ width: 40, height: 40, borderRadius: '50%', background: '#f6d3d6', color: '#6b0b17', display: 'grid', placeItems: 'center' }}>
              {profile?.firstName?.[0] || 'U'}
            </div>
            <div>
              <div style={{ fontWeight: 700 }}>{profile?.firstName} {profile?.lastName}</div>
              <div style={{ fontSize: '.85rem', opacity: 0.9 }}>{profile?.email}</div>
            </div>
          </div>

          <button type="button" className="submit-btn" style={{ marginTop: '1rem' }} onClick={onLogout}>Sign Out</button>
        </div>
      </aside>

      <main style={{ flex: 1, padding: '1.5rem' }}>
        {page === 'dashboard' && (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <h1 style={{ margin: 0 }}>Hello, {profile?.firstName}!</h1>
                <p style={{ margin: 0, color: '#6b7280' }}>{profile?.email}</p>
              </div>
              <div>
                <button className="primary-btn" onClick={() => setShowModal(true)}>Book Appointment</button>
              </div>
            </div>

            <section style={{ marginTop: '1.25rem', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
              <div style={{ background: '#fff', borderRadius: 8, padding: '1rem', minWidth: 200 }}>
                <div style={{ color: '#6b7280', fontSize: '.9rem' }}>Next Appointment</div>
                <div style={{ marginTop: '.5rem', fontWeight: 700 }}>{nextAppointment ? `${new Date(nextAppointment.dt).toLocaleDateString()} ${nextAppointment.time}` : 'No upcoming'}</div>
              </div>
              <div style={{ background: '#fff', borderRadius: 8, padding: '1rem', minWidth: 200 }}>
                <div style={{ color: '#6b7280', fontSize: '.9rem' }}>Number of Visits</div>
                <div style={{ marginTop: '.5rem', fontWeight: 700 }}>{appointments.length}</div>
              </div>
            </section>

            <section style={{ marginTop: '1.25rem' }}>
              <h2>Upcoming Appointments</h2>
              {appointmentsLoading && <p>Loading appointments...</p>}
              {appointmentsError && <p style={{ color: '#b91c1c' }}>{appointmentsError}</p>}
              {!appointmentsLoading && upcoming.length === 0 && <p>No upcoming appointments.</p>}
              <ul>
                {upcoming.slice(0,5).map((a, idx) => (
                  <li key={idx} style={{ background: '#fff', padding: '.75rem', borderRadius: 8, marginBottom: '.5rem' }}>
                    <div style={{ fontWeight: 700 }}>{a.reason || 'Appointment'}</div>
                    <div style={{ color: '#6b7280' }}>{new Date(a.dt).toLocaleString()}</div>
                  </li>
                ))}
              </ul>
            </section>
          </div>
        )}

        {page === 'appointments' && (
          <AppointmentsPage
            user={profile}
            appointments={appointments}
            loading={appointmentsLoading}
            errorMessage={appointmentsError}
            onNew={() => setShowModal(true)}
          />
        )}

        {page === 'records' && (
          <RecordsPage user={profile} />
        )}
      </main>

      <AppointmentModal
        show={showModal}
        onClose={() => setShowModal(false)}
        user={profile}
        existingAppointments={appointments}
        onCreate={handleCreateAppointment}
      />
    </div>
  );
}

export default Dashboard;
