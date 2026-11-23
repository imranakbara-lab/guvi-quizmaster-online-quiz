import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../api/client';

const schema = Yup.object({
  email: Yup.string().email('Enter a valid email').required('Email is required'),
  password: Yup.string().required('Password is required'),
});

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState('');

  const onSubmit = async (values, { setSubmitting }) => {
    setError('');
    try {
      const user = await login(values);
      navigate(location.state?.from || (user.role === 'ADMIN' ? '/admin/quizzes' : '/'));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="row justify-content-center">
      <div className="col-md-5">
        <div className="card shadow-sm border-0 p-4">
          <h3 className="mb-3">Login</h3>
          {error && <div className="alert alert-danger">{error}</div>}
          <Formik initialValues={{ email: '', password: '' }} validationSchema={schema} onSubmit={onSubmit}>
            {({ isSubmitting, errors, touched }) => (
              <Form noValidate>
                <div className="mb-3">
                  <label className="form-label">Email</label>
                  <Field name="email" type="email" className={`form-control ${touched.email && errors.email ? 'is-invalid' : ''}`} />
                  <ErrorMessage name="email" component="div" className="invalid-feedback" />
                </div>
                <div className="mb-3">
                  <label className="form-label">Password</label>
                  <Field name="password" type="password" className={`form-control ${touched.password && errors.password ? 'is-invalid' : ''}`} />
                  <ErrorMessage name="password" component="div" className="invalid-feedback" />
                </div>
                <button className="btn btn-primary w-100" type="submit" disabled={isSubmitting}>
                  {isSubmitting ? 'Logging in…' : 'Login'}
                </button>
              </Form>
            )}
          </Formik>
          <p className="mt-3 mb-1 text-center"><Link to="/forgot-password">Forgot password?</Link></p>
          <p className="mb-0 text-center">New here? <Link to="/register">Create an account</Link></p>
        </div>
      </div>
    </div>
  );
}
