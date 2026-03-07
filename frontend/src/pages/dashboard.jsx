/* eslint-disable react/prop-types */
import { useEffect, useState } from 'react';
import { clearAuth, getCurrentUser } from '../api/auth';

function Dashboard({ onLogout }) {
  const [profile, setProfile] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const me = await getCurrentUser();
        setProfile(me);
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

  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: '1rem' }}>
      <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '12px', padding: '1.5rem', maxWidth: '460px', width: '100%' }}>
        <h2 style={{ marginTop: 0 }}>Dashboard</h2>
        <p style={{ marginBottom: '0.5rem' }}><strong>Name:</strong> {profile?.firstName} {profile?.lastName}</p>
        <p style={{ marginBottom: '0.5rem' }}><strong>Email:</strong> {profile?.email}</p>
        <p style={{ marginBottom: '0.5rem' }}><strong>Provider:</strong> {profile?.provider}</p>
        <p style={{ marginBottom: '1rem' }}><strong>Staff:</strong> {profile?.isStaff ? 'Yes' : 'No'}</p>
        <button type="button" className="submit-btn" onClick={onLogout}>Logout</button>
      </div>
    </div>
  );
}

export default Dashboard;
