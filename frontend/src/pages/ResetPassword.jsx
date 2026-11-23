import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import { authApi, errorMessage } from '../api/client';

const schema = Yup.object({
  password: Yup.string().min(6, 'At least 6 characters').required('Password is required'),
  confirm: Yup.string().oneOf([Yup.ref('password')], 'Passwords do not match').required('Confirm your password'),
});

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  return (
    <div className="row justify-content-center">
      <div className="col-md-5">
        <div className="card shadow-sm border-0 p-4">
          <h3 className="mb-3">Set a New Password</h3>
          {!token && <div className="alert alert-warning">This page needs the link from your email.</div>}
          {message ? (
            <div className="alert alert-success">{message} <Link to="/login">Login</Link></div>
          ) : (
            <>
              {error && <div className="alert alert-danger">{error}</div>}
              <Formik initialValues={{ password: '', confirm: '' }} validationSchema={schema}
                      onSubmit={async ({ password }, { setSubmitting }) => {
                        setError('');
                        try { setMessage((await authApi.resetPassword(token, password)).message); }
                        catch (e) { setError(errorMessage(e)); }
                        finally { setSubmitting(false); }
                      }}>
                {({ isSubmitting, errors, touched }) => (
                  <Form noValidate>
                    {[['password', 'New password'], ['confirm', 'Confirm password']].map(([name, label]) => (
                      <div className="mb-3" key={name}>
                        <label className="form-label">{label}</label>
                        <Field name={name} type="password" className={`form-control ${touched[name] && errors[name] ? 'is-invalid' : ''}`} />
                        <ErrorMessage name={name} component="div" className="invalid-feedback" />
                      </div>
                    ))}
                    <button className="btn btn-primary w-100" type="submit" disabled={isSubmitting || !token}>Update password</button>
                  </Form>
                )}
              </Formik>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
