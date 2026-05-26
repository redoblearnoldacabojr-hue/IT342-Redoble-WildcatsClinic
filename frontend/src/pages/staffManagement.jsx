/* eslint-disable react/prop-types */
import { useMemo, useState } from 'react';

const roleLabels = {
  1: 'Role - 1 User',
  2: 'Role - 2 Staff',
  3: 'Role - 3 Admin',
};

export default function StaffManagementPage({ users = [], doctors = [], canChangeRoles = false, onChangeRole, onSaveDoctor, onRemoveDoctor }) {
  const [doctorForm, setDoctorForm] = useState({ name: '', specialization: '', doctorIn: true });
  const [showAddStaffForm, setShowAddStaffForm] = useState(false);
  const [staffToAddId, setStaffToAddId] = useState('');

  const sortedUsers = useMemo(() => [...users].sort((left, right) => String(left.email || '').localeCompare(String(right.email || ''))), [users]);
  const staffUsers = useMemo(() => sortedUsers.filter((user) => Number(user.role || 1) === 2), [sortedUsers]);
  const usersForStaff = useMemo(() => sortedUsers.filter((user) => Number(user.role || 1) === 1), [sortedUsers]);
  const sortedDoctors = useMemo(() => [...doctors].sort((left, right) => String(left.name || '').localeCompare(String(right.name || ''))), [doctors]);

  const handleAddStaff = async (event) => {
    event.preventDefault();
    if (!onChangeRole || !staffToAddId) {
      return;
    }

    await onChangeRole(staffToAddId, 2);
    setStaffToAddId('');
    setShowAddStaffForm(false);
  };

  const handleRemoveStaff = async (userId) => {
    if (!onChangeRole) {
      return;
    }

    await onChangeRole(userId, 1);
  };

  const handleAddDoctor = async (event) => {
    event.preventDefault();
    if (!onSaveDoctor || !doctorForm.name.trim() || !doctorForm.specialization.trim()) {
      return;
    }

    await onSaveDoctor(null, {
      ...doctorForm,
      available: doctorForm.doctorIn,
    });
    setDoctorForm({ name: '', specialization: '', doctorIn: true });
  };

  const handleToggleDoctorIn = async (doctor) => {
    if (!onSaveDoctor) {
      return;
    }

    await onSaveDoctor(doctor.id, {
      name: doctor.name,
      specialization: doctor.specialization,
      doctorIn: !(doctor.doctorIn ?? doctor.available),
      available: !(doctor.doctorIn ?? doctor.available),
    });
  };

  const handleRemoveDoctor = async (doctor) => {
    if (!onRemoveDoctor) {
      return;
    }

    await onRemoveDoctor(doctor.id);
  };

  return (
    <div className="staff-management-layout">
      <section className="surface-panel staff-panel">
        <div className="section-header compact">
          <div>
            <div className="calendar-eyebrow">Accounts</div>
            <h2 style={{ margin: 0 }}>Staff Users</h2>
            <p style={{ margin: '.35rem 0 0', color: '#6b7280' }}>Only staff accounts are shown here. Admins can add or remove staff access.</p>
          </div>
          {canChangeRoles && (
            <button type="button" className="primary-btn" onClick={() => setShowAddStaffForm((current) => !current)}>
              {showAddStaffForm ? 'Close Add Staff' : 'Add Staff'}
            </button>
          )}
        </div>

        {canChangeRoles && showAddStaffForm && (
          <form className="doctor-form staff-add-form" onSubmit={handleAddStaff}>
            <label className="field-label">
              <span>Select user to promote</span>
              <select
                className="field-input"
                value={staffToAddId}
                onChange={(event) => setStaffToAddId(event.target.value)}
              >
                <option value="">Choose a user</option>
                {usersForStaff.map((user) => (
                  <option key={user.id} value={user.id}>
                    {[user.firstName, user.lastName].filter(Boolean).join(' ') || user.email}
                  </option>
                ))}
              </select>
            </label>
            <button type="submit" className="primary-btn" disabled={!staffToAddId}>Add Staff</button>
          </form>
        )}

        <div className="table-shell">
          <table className="admin-table">
            <thead>
              <tr>
                <th>User</th>
                <th>Email</th>
                <th>Role</th>
                {canChangeRoles && <th>Action</th>}
              </tr>
            </thead>
            <tbody>
              {staffUsers.length === 0 && (
                <tr>
                  <td colSpan={canChangeRoles ? 4 : 3} style={{ color: '#6b7280' }}>No staff users yet.</td>
                </tr>
              )}
              {staffUsers.map((user) => (
                <tr key={user.id}>
                  <td>{[user.firstName, user.lastName].filter(Boolean).join(' ') || 'Unknown'}</td>
                  <td>{user.email}</td>
                  <td><span className="role-pill role-2">Role - 2 Staff</span></td>
                  {canChangeRoles && (
                    <td>
                      <button type="button" className="secondary-btn danger-btn" onClick={() => handleRemoveStaff(user.id)}>
                        Remove Staff
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <aside className="staff-sidebar">
        <section className="surface-panel staff-panel">
          <div className="section-header compact">
            <div>
              <div className="calendar-eyebrow">Doctors</div>
              <h2 style={{ margin: 0 }}>Doctor Roster</h2>
            </div>
          </div>

          <form className="doctor-form" onSubmit={handleAddDoctor}>
            <input
              className="form-input"
              placeholder="Doctor name"
              value={doctorForm.name}
              onChange={(event) => setDoctorForm((current) => ({ ...current, name: event.target.value }))}
            />
            <input
              className="form-input"
              placeholder="Specialization"
              value={doctorForm.specialization}
              onChange={(event) => setDoctorForm((current) => ({ ...current, specialization: event.target.value }))}
            />
            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={doctorForm.doctorIn}
                onChange={(event) => setDoctorForm((current) => ({ ...current, doctorIn: event.target.checked }))}
              />
              <span>Doctor in for assignment</span>
            </label>
            <button type="submit" className="primary-btn">Add Doctor</button>
          </form>

          <div className="table-shell compact-table">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Specialization</th>
                  <th>Doctor In</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {sortedDoctors.map((doctor) => (
                  <tr key={doctor.id}>
                    <td>{doctor.name}</td>
                    <td>{doctor.specialization}</td>
                    <td><span className={`role-pill ${(doctor.doctorIn ?? doctor.available) ? 'role-2' : 'role-1'}`}>{(doctor.doctorIn ?? doctor.available) ? 'IN' : 'OUT'}</span></td>
                    <td>
                      <div className="inline-actions">
                        <button type="button" className="secondary-btn" onClick={() => handleToggleDoctorIn(doctor)}>
                          {(doctor.doctorIn ?? doctor.available) ? 'Set OUT' : 'Set IN'}
                        </button>
                        <button type="button" className="secondary-btn danger-btn" onClick={() => handleRemoveDoctor(doctor)}>
                          Remove
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </aside>
    </div>
  );
}