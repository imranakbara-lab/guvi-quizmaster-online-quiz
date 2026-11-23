import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import { adminApi, errorMessage } from '../api/client';

const quizSchema = Yup.object({
  title: Yup.string().trim().required('Title is required').max(150),
  description: Yup.string().max(2000),
  timeLimitMinutes: Yup.number().typeError('Enter minutes').integer().min(1, 'At least 1 minute').max(180, 'Max 180 minutes').required('Required'),
});

const questionSchema = Yup.object({
  text: Yup.string().trim().required('Question text is required'),
  options: Yup.array().of(Yup.object({ text: Yup.string().trim().required('Option text is required'), correct: Yup.boolean() }))
    .min(2, 'At least 2 options').max(8, 'At most 8 options')
    .test('one-correct', 'Mark at least one correct option', (opts) => (opts || []).some((o) => o.correct)),
});

const emptyQuestion = { text: '', options: [{ text: '', correct: true }, { text: '', correct: false }, { text: '', correct: false }, { text: '', correct: false }] };

function QuestionForm({ initial, onSave, onCancel }) {
  return (
    <Formik initialValues={initial} validationSchema={questionSchema} onSubmit={onSave} enableReinitialize>
      {({ values, errors, touched, setFieldValue, isSubmitting }) => (
        <Form noValidate className="border rounded p-3 bg-light mb-3">
          <label className="form-label fw-semibold">Question</label>
          <Field as="textarea" rows="2" name="text" className={`form-control mb-2 ${touched.text && errors.text ? 'is-invalid' : ''}`} />
          <ErrorMessage name="text" component="div" className="invalid-feedback mb-2" />
          <label className="form-label fw-semibold">Options <small className="text-muted fw-normal">(tick the correct answer – tick more than one for multi-select)</small></label>
          {values.options.map((o, i) => (
            <div className="input-group mb-2" key={i}>
              <span className="input-group-text">
                <input type="checkbox" className="form-check-input mt-0" checked={o.correct}
                       onChange={(e) => setFieldValue(`options.${i}.correct`, e.target.checked)} title="Correct answer" />
              </span>
              <Field name={`options.${i}.text`} className="form-control" placeholder={`Option ${String.fromCharCode(65 + i)}`} />
              <button type="button" className="btn btn-outline-danger" disabled={values.options.length <= 2}
                      onClick={() => setFieldValue('options', values.options.filter((_, j) => j !== i))}>✕</button>
            </div>
          ))}
          {typeof errors.options === 'string' && <div className="text-danger small mb-2">{errors.options}</div>}
          {Array.isArray(errors.options) && touched.options && <div className="text-danger small mb-2">Every option needs text</div>}
          <div className="d-flex gap-2">
            <button type="button" className="btn btn-sm btn-outline-secondary" disabled={values.options.length >= 8}
                    onClick={() => setFieldValue('options', [...values.options, { text: '', correct: false }])}>+ Option</button>
            <span className="flex-grow-1" />
            {onCancel && <button type="button" className="btn btn-sm btn-outline-secondary" onClick={onCancel}>Cancel</button>}
            <button type="submit" className="btn btn-sm btn-primary" disabled={isSubmitting}>Save Question</button>
          </div>
        </Form>
      )}
    </Formik>
  );
}

export default function QuizEditor() {
  const { id } = useParams();
  const isNew = !id;
  const navigate = useNavigate();
  const [quiz, setQuiz] = useState(isNew ? { title: '', description: '', timeLimitMinutes: 10, published: false, questions: [] } : null);
  const [editingId, setEditingId] = useState(null);   // question id being edited, or 'new'
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (!isNew) adminApi.quiz(id).then(setQuiz).catch((e) => setError(errorMessage(e)));
  }, [id, isNew]);

  const saveQuiz = async (values, { setSubmitting }) => {
    setError(''); setNotice('');
    try {
      const body = { ...values, timeLimitMinutes: Number(values.timeLimitMinutes) };
      if (isNew) {
        const created = await adminApi.createQuiz(body);
        navigate(`/admin/quizzes/${created.id}`, { replace: true });
      } else {
        setQuiz(await adminApi.updateQuiz(id, body));
        setNotice('Quiz saved');
      }
    } catch (e) { setError(errorMessage(e)); }
    finally { setSubmitting(false); }
  };

  const saveQuestion = async (values, { setSubmitting }) => {
    setError(''); setNotice('');
    try {
      if (editingId === 'new') await adminApi.addQuestion(id, values);
      else await adminApi.updateQuestion(editingId, values);
      setQuiz(await adminApi.quiz(id));
      setEditingId(null);
    } catch (e) { setError(errorMessage(e)); }
    finally { setSubmitting(false); }
  };

  const deleteQuestion = async (q) => {
    if (!window.confirm('Delete this question?')) return;
    try {
      await adminApi.deleteQuestion(q.id);
      setQuiz(await adminApi.quiz(id));
    } catch (e) { setError(errorMessage(e)); }
  };

  if (!quiz) return error ? <div className="alert alert-danger">{error}</div> : <div className="text-center py-5">Loading…</div>;

  return (
    <>
      <Link to="/admin/quizzes" className="small">← All quizzes</Link>
      <h3 className="mt-2 mb-3">{isNew ? 'New Quiz' : `Edit: ${quiz.title}`}</h3>
      {error && <div className="alert alert-danger">{error}</div>}
      {notice && <div className="alert alert-success">{notice}</div>}

      <div className="card border-0 shadow-sm p-4 mb-4">
        <Formik initialValues={{ title: quiz.title, description: quiz.description || '', timeLimitMinutes: quiz.timeLimitMinutes, published: quiz.published }}
                validationSchema={quizSchema} onSubmit={saveQuiz} enableReinitialize>
          {({ errors, touched, isSubmitting }) => (
            <Form noValidate className="row g-3">
              <div className="col-md-8">
                <label className="form-label">Title</label>
                <Field name="title" className={`form-control ${touched.title && errors.title ? 'is-invalid' : ''}`} />
                <ErrorMessage name="title" component="div" className="invalid-feedback" />
              </div>
              <div className="col-md-4">
                <label className="form-label">Time limit (minutes)</label>
                <Field name="timeLimitMinutes" type="number" min="1" max="180" className={`form-control ${touched.timeLimitMinutes && errors.timeLimitMinutes ? 'is-invalid' : ''}`} />
                <ErrorMessage name="timeLimitMinutes" component="div" className="invalid-feedback" />
              </div>
              <div className="col-12">
                <label className="form-label">Description</label>
                <Field as="textarea" rows="2" name="description" className="form-control" />
              </div>
              <div className="col-12 d-flex align-items-center gap-3">
                {!isNew && (
                  <div className="form-check form-switch">
                    <Field type="checkbox" name="published" className="form-check-input" id="pub" />
                    <label className="form-check-label" htmlFor="pub">Published (visible to participants)</label>
                  </div>
                )}
                <span className="flex-grow-1" />
                <button className="btn btn-primary" type="submit" disabled={isSubmitting}>{isNew ? 'Create & add questions' : 'Save Quiz'}</button>
              </div>
            </Form>
          )}
        </Formik>
      </div>

      {!isNew && (
        <>
          <div className="d-flex justify-content-between align-items-center mb-2">
            <h4 className="mb-0">Questions ({quiz.questions.length})</h4>
            {editingId !== 'new' && <button className="btn btn-success" onClick={() => setEditingId('new')}>+ Add Question</button>}
          </div>
          {editingId === 'new' && <QuestionForm initial={emptyQuestion} onSave={saveQuestion} onCancel={() => setEditingId(null)} />}
          {quiz.questions.length === 0 && editingId !== 'new' && <p className="text-muted">No questions yet. Add at least one, then publish the quiz.</p>}
          {quiz.questions.map((q, i) => (
            editingId === q.id
              ? <QuestionForm key={q.id} initial={{ text: q.text, options: q.options.map((o) => ({ text: o.text, correct: o.correct })) }}
                              onSave={saveQuestion} onCancel={() => setEditingId(null)} />
              : (
                <div key={q.id} className="card border-0 shadow-sm mb-2">
                  <div className="card-body">
                    <div className="d-flex justify-content-between">
                      <h6 style={{ whiteSpace: 'pre-line' }}>{i + 1}. {q.text}</h6>
                      <div className="text-nowrap">
                        <button className="btn btn-sm btn-outline-primary me-2" onClick={() => setEditingId(q.id)}>Edit</button>
                        <button className="btn btn-sm btn-outline-danger" onClick={() => deleteQuestion(q)}>Delete</button>
                      </div>
                    </div>
                    <ul className="list-unstyled mb-0 small">
                      {q.options.map((o) => <li key={o.id} className={o.correct ? 'text-success fw-semibold' : ''}>{o.correct ? '✔' : '○'} {o.text}</li>)}
                    </ul>
                  </div>
                </div>
              )
          ))}
        </>
      )}
    </>
  );
}
