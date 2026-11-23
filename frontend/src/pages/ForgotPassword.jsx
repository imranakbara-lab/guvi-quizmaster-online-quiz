import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import { authApi, errorMessage } from '../api/client';

export default function ForgotPassword() {
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  return (
    <div className="row justify-content-center">
      <div className="col-md-5">
        <div className="card shadow-sm border-0 p-4">
          <h3 className="mb-2">Forgot Password</h3>
          <p className="text-muted small">Enter your email and we'll send you a link to reset your password.</p>
          {message && <div className="alert alert-success">{message}</div>}
          {error && <div className="alert alert-danger">{error}</div>}
          <Formik initialValues={{ email: '' }}
                  validationSchema={Yup.object({ email: Yup.string().email('Enter a valid email').required('Email is required') })}
                  onSubmit={async ({ email }, { setSubmitting }) => {
                    setError('');
                    try { setMessage((await authApi.forgotPassword(email)).message); }
                    catch (e) { setError(errorMessage(e)); }
                    finally { setSubmitting(false); }
                  }}>
            {({ isSubmitting, errors, touched }) => (
              <Form noValidate>
                <div className="mb-3">
                  <Field name="email" type="email" placeholder="you@example.com" className={`form-control ${touched.email && errors.email ? 'is-invalid' : ''}`} />
                  <ErrorMessage name="email" component="div" className="invalid-feedback" />
                </div>
                <button className="btn btn-primary w-100" type="submit" disabled={isSubmitting}>Send reset link</button>
              </Form>
            )}
          </Formik>
          <p className="mt-3 mb-0 text-center"><Link to="/login">Back to login</Link></p>
        </div>
      </div>
    </div>
  );
}
