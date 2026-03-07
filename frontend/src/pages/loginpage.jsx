/* eslint-disable react/prop-types */
import React, { useState } from 'react';
import { Eye, EyeOff, LogIn } from 'lucide-react';
import '../css/login.css';
import clinicLogo from '../logo/Logo.png';
import { loginUser, persistAuth, startGoogleLogin } from '../api/auth';

const LoginPage = ({ onNavigateToRegister, onAuthSuccess }) => {
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorMessage('');
    setIsLoading(true);

    const formData = new FormData(event.currentTarget);
    const payload = {
      email: String(formData.get('email') || '').trim(),
      password: String(formData.get('password') || ''),
    };

    try {
      const authResponse = await loginUser(payload);
      persistAuth(authResponse);
      if (onAuthSuccess) {
        onAuthSuccess(authResponse);
      }
    } catch (error) {
      setErrorMessage(error.message || 'Login failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-wrapper">
      {/* Left Side - Branding */}
      <div className="brand-side">
        <div className="logo-container">
          <div className="logo-circle">
            <img src={clinicLogo} alt="Wildcat Clinic logo" className="logo-image" />
          </div>
        </div>
        <h1 className="brand-title">Wildcats Clinic</h1>
      </div>

      {/* Right Side - Login Form */}
      <div className="form-side">
        <div className="login-card">
          <div className="card-header">
            <h2 className="card-title">Welcome Back</h2>
            <p className="card-subtitle">Sign in to your account</p>
          </div>

          <form className="login-form" onSubmit={handleSubmit}>
            {/* Email Input */}
            <div className="input-group">
              <label htmlFor="email" className="input-label">Email Address</label>
              <input
                id="email"
                name="email"
                type="email" 
                placeholder="juan.delacruz@cit.edu"
                className="form-input"
                autoComplete="email"
                required
              />
            </div>

            {/* Password Input */}
            <div className="input-group">
              <div className="label-row">
                <label htmlFor="password" className="input-label">Password</label>
                <a href="/forgot-password" className="forgot-link">Forgot password?</a>
              </div>
              <div className="input-wrapper">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"} 
                  placeholder="Enter your password"
                  className="form-input password-input"
                  autoComplete="current-password"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="toggle-btn"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            {/* Sign In Button */}
            <button className="submit-btn" type="submit" disabled={isLoading}>
              <LogIn size={18} />
              {isLoading ? 'Signing In...' : 'Sign In'}
            </button>

            {errorMessage && (
              <p className="login-error-text" role="alert">{errorMessage}</p>
            )}
          </form>

          {/* Divider */}
          <div className="divider-container">
            <div className="divider-line"></div>
            <span className="divider-text">or</span>
          </div>

          {/* Google OAuth Button */}
          <div className="oauth-container">
            <button className="google-btn" type="button" onClick={startGoogleLogin}>
              <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="Google" className="google-icon" />
              <span>Sign in with Google</span>
            </button>
          </div>

          {/* Register Link */}
          <p className="register-text">
            Don't have an account?{' '}
            <button
              type="button"
              className="register-link"
              onClick={onNavigateToRegister}
            >
              Register here
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;