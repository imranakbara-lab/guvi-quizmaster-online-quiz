import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../api/client';

const schema = Yup.object({
  name: Yup.string().trim().required('Name is required').max(100),
  email: Yup.string().email('Enter a valid email').required('Email is required'),
  phone: Yup.string().matches(/^[0-9+\- ]{7,15}$/, { message: 'Enter a valid phone number', excludeEmptyString: true }),
  password: Yup.string().min(6, 'At least 6 characters').required('Password is required'),
  confirmPassword: Yup.string().oneOf([Yup.ref('password')], 'Passwords do not match').required('Confirm your password'),
});

const fields = [
  { name: 'name', label: 'Full Name', type: 'text' },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'phone', label: 'Phone (optional)', type: 'tel' },
  { name: 'password', label: 'Password', type: 'password' },
  { name: 'confirmPassword', label: 'Confirm Password', type: 'password' },
];

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');

  const onSubmit = async ({ confirmPassword, ...values }, { setSubmitting }) => {
    setError('');
    try {
      await register(values);
      navigate('/');
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="row justify-content-center">
      <div className="col-md-6">
        <div className="card shadow-sm border-0 p-4">
          <h3 className="mb-3">Create Account</h3>
          {error && <div className="alert alert-danger">{error}</div>}
          <Formik initialValues={{ name: '', email: '', phone: '', password: '', confirmPassword: '' }}
                  validationSchema={schema} onSubmit={onSubmit}>
            {({ isSubmitting, errors, touched }) => (
              <Form noValidate>
                {fields.map((f) => (
                  <div className="mb-3" key={f.name}>
                    <label className="form-label">{f.label}</label>
                    <Field name={f.name} type={f.type}
                           className={`form-control ${touched[f.name] && errors[f.name] ? 'is-invalid' : ''}`} />
                    <ErrorMessage name={f.name} component="div" className="invalid-feedback" />
                  </div>
                ))}
                <button className="btn btn-primary w-100" type="submit" disabled={isSubmitting}>
                  {isSubmitting ? 'Creating…' : 'Register'}
                </button>
              </Form>
            )}
          </Formik>
          <p className="mt-3 mb-0 text-center">Already have an account? <Link to="/login">Login</Link></p>
        </div>
      </div>
    </div>
  );
}
