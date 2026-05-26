import { useEffect, useState } from 'react';
import LoginPage from './pages/loginpage';
import RegisterPage from './pages/registerpage';
import Dashboard from './pages/dashboard';
import { clearAuth, getCurrentUser, getStoredToken, persistAuth } from './api/auth';

function App() {
  const [activePage, setActivePage] = useState('login');
  const [user, setUser] = useState(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);

  useEffect(() => {
    const bootstrapAuth = async () => {
      const params = new URLSearchParams(window.location.search);
      const token = params.get('token');
      const email = params.get('email');

      if (token && email) {
        const authResponse = {
          token,
          userId: Number(params.get('userId')),
          email,
          firstName: params.get('firstName') || '',
          lastName: params.get('lastName') || '',
          provider: params.get('provider') || 'LOCAL',
          role: Number(params.get('role') || '1'),
        };

        persistAuth(authResponse);
        window.history.replaceState({}, document.title, window.location.pathname);
      }

      const storedToken = getStoredToken();
      if (storedToken) {
        try {
          const currentUser = await getCurrentUser();
          setUser(currentUser);
          return;
        } catch {
          clearAuth();
        }
      }

      setUser(null);
      setActivePage('login');
      setIsBootstrapping(false);
    };

    bootstrapAuth().finally(() => setIsBootstrapping(false));
  }, []);

  const handleAuthSuccess = async () => {
    try {
      const currentUser = await getCurrentUser();
      setUser(currentUser);
    } catch {
      setUser(null);
    }
  };

  const handleLogout = () => {
    clearAuth();
    setUser(null);
    setActivePage('login');
  };

  if (user) {
    return <Dashboard onLogout={handleLogout} />;
  }

  if (isBootstrapping) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: '1rem' }}>
        <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '12px', padding: '1.5rem', maxWidth: '460px', width: '100%' }}>
          <h2 style={{ marginTop: 0 }}>Loading session...</h2>
        </div>
      </div>
    );
  }

  return (
    activePage === 'login' ? (
      <LoginPage
        onNavigateToRegister={() => setActivePage('register')}
        onAuthSuccess={handleAuthSuccess}
      />
    ) : (
      <RegisterPage
        onNavigateToLogin={() => setActivePage('login')}
      />
    )
  );
}

export default App;