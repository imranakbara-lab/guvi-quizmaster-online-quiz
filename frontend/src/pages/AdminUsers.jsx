import { useEffect, useState } from 'react';
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import { adminApi, errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { formatDate } from '../utils';

const schema = Yup.object({
  name: Yup.string().trim().required('Name is required'),
  email: Yup.string().email('Enter a valid email').required('Email is required'),
  phone: Yup.string().nullable(),
  role: Yup.string().oneOf(['PARTICIPANT', 'ADMIN']).required(),
});

export default function AdminUsers() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState([]);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminApi.users().then(setUsers).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false));
  }, []);

  const save = async (values, { setSubmitting }) => {
    setError('');
    try {
      const updated = await adminApi.updateUser(editing.id, values);
      setUsers(users.map((u) => (u.id === updated.id ? updated : u)));
      setEditing(null);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  };

  const remove = async (u) => {
    if (!window.confirm(`Delete ${u.name}? All of their quiz results will also be deleted.`)) return;
    try {
      await adminApi.deleteUser(u.id);
      setUsers(users.filter((x) => x.id !== u.id));
    } catch (e) { setError(errorMessage(e)); }
  };

  return (
    <>
      <h3 className="mb-3">User Management</h3>
      {error && <div className="alert alert-danger">{error}</div>}

      {editing && (
        <div className="card border-0 shadow-sm p-4 mb-4">
          <h5>Edit User</h5>
          <Formik initialValues={{ name: editing.name, email: editing.email, phone: editing.phone || '', role: editing.role }}
                  validationSchema={schema} onSubmit={save} enableReinitialize>
            {({ isSubmitting, errors, touched }) => (
              <Form className="row g-3" noValidate>
                {['name', 'email', 'phone'].map((f) => (
                  <div className="col-md-3" key={f}>
                    <label className="form-label text-capitalize">{f}</label>
                    <Field name={f} className={`form-control ${touched[f] && errors[f] ? 'is-invalid' : ''}`} />
                    <ErrorMessage name={f} component="div" className="invalid-feedback" />
                  </div>
                ))}
                <div className="col-md-3">
                  <label className="form-label">Role</label>
                  <Field as="select" name="role" className="form-select">
                    <option value="PARTICIPANT">PARTICIPANT</option>
                    <option value="ADMIN">ADMIN</option>
                  </Field>
                </div>
                <div className="col-12 d-flex gap-2">
                  <button className="btn btn-primary" type="submit" disabled={isSubmitting}>Save</button>
                  <button className="btn btn-outline-secondary" type="button" onClick={() => setEditing(null)}>Cancel</button>
                </div>
              </Form>
            )}
          </Formik>
        </div>
      )}

      <div className="table-responsive card border-0 shadow-sm">
        <table className="table table-hover align-middle mb-0">
          <thead className="table-light">
            <tr><th>#</th><th>Name</th><th>Email</th><th>Phone</th><th>Role</th><th>Joined</th><th className="text-end">Actions</th></tr>
          </thead>
          <tbody>
            {loading && <tr><td colSpan="7" className="text-center py-4">Loading…</td></tr>}
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.id}</td>
                <td>{u.name}</td>
                <td>{u.email}</td>
                <td>{u.phone || '-'}</td>
                <td><span className={`badge ${u.role === 'ADMIN' ? 'bg-dark' : 'bg-secondary'}`}>{u.role}</span></td>
                <td>{formatDate(u.createdAt)}</td>
                <td className="text-end text-nowrap">
                  <button className="btn btn-outline-primary btn-sm me-2" onClick={() => setEditing(u)}>Edit</button>
                  {u.id !== me.id && <button className="btn btn-outline-danger btn-sm" onClick={() => remove(u)}>Delete</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
