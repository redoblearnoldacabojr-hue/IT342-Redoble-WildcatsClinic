import { useEffect, useState } from 'react';
import LoginPage from './pages/loginpage';
import RegisterPage from './pages/registerpage';
import Dashboard from './pages/dashboard';
import { clearAuth, getStoredUser, persistAuth } from './api/auth';

function App() {
  const [activePage, setActivePage] = useState('login');
  const [user, setUser] = useState(() => getStoredUser());

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    const email = params.get('email');

    if (!token || !email) {
      return;
    }

    const authResponse = {
      token,
      userId: Number(params.get('userId')),
      email,
      firstName: params.get('firstName') || '',
      lastName: params.get('lastName') || '',
      provider: params.get('provider') || 'LOCAL',
      isStaff: params.get('isStaff') === 'true',
    };

    persistAuth(authResponse);
    setUser(getStoredUser());
    window.history.replaceState({}, document.title, window.location.pathname);
  }, []);

  const handleAuthSuccess = () => {
    setUser(getStoredUser());
  };

  const handleLogout = () => {
    clearAuth();
    setUser(null);
    setActivePage('login');
  };

  if (user) {
    return <Dashboard onLogout={handleLogout} />;
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