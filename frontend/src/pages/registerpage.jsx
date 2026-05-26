/* eslint-disable react/prop-types */
import React, { useState } from 'react';
import { Eye, EyeOff, UserPlus } from 'lucide-react';
import '../css/register.css';
import clinicLogo from '../logo/Logo.png';
import { registerUser, startGoogleLogin } from '../api/auth';

const RegisterPage = ({ onNavigateToLogin }) => {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordMismatchError, setPasswordMismatchError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitError('');
    setSuccessMessage('');

    if (password !== confirmPassword) {
      setPasswordMismatchError('Passwords do not match.');
      return;
    }

    setPasswordMismatchError('');

    const formData = new FormData(event.currentTarget);
    const payload = {
      firstName: formData.get('firstName'),
      lastName: formData.get('lastName'),
      email: formData.get('registerEmail'),
      password: confirmPassword,
    };

    try {
      setIsLoading(true);
      await registerUser(payload);
      setSuccessMessage('Registration successful. Redirecting to login...');
      window.setTimeout(() => {
        onNavigateToLogin();
      }, 1200);
    } catch (error) {
      setSubmitError(error.message || 'Registration failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="register-wrapper">
      {successMessage && (
        <div className="register-success-toast" role="status" aria-live="polite">
          {successMessage}
        </div>
      )}

      <div className="register-brand-side">
        <div className="register-logo-container">
            <div className="register-logo-circle">
            <img src={clinicLogo} alt="WildcatsClinic logo" className="register-logo-image" />
          </div>
        </div>
        <h1 className="register-brand-title">WildcatsClinic</h1>
      </div>

      <div className="register-form-side">
        <div className="register-card">
          <div className="register-card-header">
            <h2 className="register-card-title">Create Account</h2>
            <p className="register-card-subtitle">Register to get started</p>
          </div>

          <form className="register-form" onSubmit={handleSubmit}>
            <div className="register-input-group">
              <label htmlFor="firstName" className="register-input-label">First Name</label>
              <input
                id="firstName"
                name="firstName"
                type="text"
                placeholder="Juan"
                className="register-form-input"
                autoComplete="given-name"
                required
              />
            </div>

            <div className="register-input-group">
              <label htmlFor="lastName" className="register-input-label">Last Name</label>
              <input
                id="lastName"
                name="lastName"
                type="text"
                placeholder="Dela Cruz"
                className="register-form-input"
                autoComplete="family-name"
                required
              />
            </div>

            <div className="register-input-group">
              <label htmlFor="registerEmail" className="register-input-label">Email Address</label>
              <input
                id="registerEmail"
                name="registerEmail"
                type="email"
                placeholder="juan.delacruz@cit.edu"
                className="register-form-input"
                autoComplete="email"
                required
              />
            </div>

            <div className="register-input-group">
              <label htmlFor="registerPassword" className="register-input-label">Password</label>
              <div className="register-input-wrapper">
                <input
                  id="registerPassword"
                  name="registerPassword"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Create a password"
                  className="register-form-input register-password-input"
                  autoComplete="new-password"
                  value={password}
                  onChange={(event) => {
                    setPassword(event.target.value);
                    if (passwordMismatchError) {
                      setPasswordMismatchError('');
                    }
                  }}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="register-toggle-btn"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <div className="register-input-group">
              <label htmlFor="confirmPassword" className="register-input-label">Confirm Password</label>
              <div className="register-input-wrapper">
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type={showConfirmPassword ? 'text' : 'password'}
                  placeholder="Re-enter your password"
                  className={`register-form-input register-password-input ${passwordMismatchError ? 'register-input-error' : ''}`}
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(event) => {
                    setConfirmPassword(event.target.value);
                    if (passwordMismatchError) {
                      setPasswordMismatchError('');
                    }
                  }}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="register-toggle-btn"
                  aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                >
                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            {passwordMismatchError && (
              <p className="register-error-text" role="alert">{passwordMismatchError}</p>
            )}

            <button className="register-submit-btn" type="submit" disabled={isLoading}>
              <UserPlus size={18} />
              {isLoading ? 'Creating Account...' : 'Create Account'}
            </button>

            {submitError && (
              <p className="register-error-text" role="alert">{submitError}</p>
            )}
          </form>

          <div className="register-divider-container">
            <div className="register-divider-line"></div>
            <span className="register-divider-text">or</span>
          </div>

          <div className="register-oauth-container">
            <button className="register-google-btn" type="button" onClick={startGoogleLogin}>
              <img
                src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg"
                alt="Google"
                className="register-google-icon"
              />
              <span>Sign up with Google</span>
            </button>
          </div>

          <p className="register-footer-text">
            Already have an account?{' '}
            <button
              type="button"
              className="register-footer-link"
              onClick={onNavigateToLogin}
            >
              Sign in
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
