import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { quizApi, errorMessage } from '../api/client';
import { formatSeconds } from '../utils';

export default function TakeQuiz() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [attempt, setAttempt] = useState(null);
  const [answers, setAnswers] = useState({});      // questionId -> [optionIds]
  const [current, setCurrent] = useState(0);
  const [remaining, setRemaining] = useState(0);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const submittedRef = useRef(false);
  const answersRef = useRef(answers);
  answersRef.current = answers;

  useEffect(() => {
    quizApi.start(id)
      .then((a) => {
        setAttempt(a);
        setRemaining(a.remainingSeconds);
        try {
          const saved = JSON.parse(sessionStorage.getItem(`attempt-${a.attemptId}`) || '{}');
          setAnswers(saved);
        } catch { /* ignore */ }
      })
      .catch((e) => setError(errorMessage(e)));
  }, [id]);

  const submit = useCallback(async (auto = false) => {
    if (!attempt || submittedRef.current) return;
    if (!auto) {
      const unanswered = attempt.questions.filter((q) => !(answersRef.current[q.id] || []).length).length;
      const msg = unanswered ? `You have ${unanswered} unanswered question(s). Submit anyway?` : 'Submit your answers?';
      if (!window.confirm(msg)) return;
    }
    submittedRef.current = true;
    setSubmitting(true);
    try {
      const payload = attempt.questions.map((q) => ({ questionId: q.id, selectedOptionIds: answersRef.current[q.id] || [] }));
      const result = await quizApi.submit(attempt.attemptId, payload);
      sessionStorage.removeItem(`attempt-${attempt.attemptId}`);
      navigate(`/results/${result.summary.id}`, { replace: true, state: { auto } });
    } catch (e) {
      submittedRef.current = false;
      setSubmitting(false);
      setError(errorMessage(e));
    }
  }, [attempt, navigate]);

  // Countdown timer - auto submit when it reaches zero
  useEffect(() => {
    if (!attempt) return undefined;
    const endAt = Date.now() + attempt.remainingSeconds * 1000;
    const timer = setInterval(() => {
      const left = Math.max(0, Math.round((endAt - Date.now()) / 1000));
      setRemaining(left);
      if (left === 0) {
        clearInterval(timer);
        submit(true);
      }
    }, 500);
    return () => clearInterval(timer);
  }, [attempt, submit]);

  const choose = (question, optionId) => {
    const prev = answers[question.id] || [];
    const next = question.multipleCorrect
      ? (prev.includes(optionId) ? prev.filter((x) => x !== optionId) : [...prev, optionId])
      : [optionId];
    const updated = { ...answers, [question.id]: next };
    setAnswers(updated);
    sessionStorage.setItem(`attempt-${attempt.attemptId}`, JSON.stringify(updated));
  };

  if (error && !attempt) return <div className="alert alert-danger">{error}</div>;
  if (!attempt) return <div className="text-center py-5">Starting quiz…</div>;

  const q = attempt.questions[current];
  const picked = answers[q.id] || [];
  const answeredCount = attempt.questions.filter((x) => (answers[x.id] || []).length).length;

  return (
    <div className="row g-4">
      <div className="col-lg-8">
        <div className="card border-0 shadow-sm">
          <div className="card-header bg-white d-flex justify-content-between align-items-center">
            <div>
              <h5 className="mb-0">{attempt.quizTitle}</h5>
              <small className="text-muted">Question {current + 1} of {attempt.questions.length}</small>
            </div>
            <div className={`timer fw-bold ${remaining <= 30 ? 'warn' : ''}`} title="Time left">⏱ {formatSeconds(remaining)}</div>
          </div>
          <div className="progress rounded-0" style={{ height: 4 }}>
            <div className="progress-bar" style={{ width: `${(remaining / (attempt.timeLimitMinutes * 60)) * 100}%` }} />
          </div>
          <div className="card-body p-4">
            {error && <div className="alert alert-danger">{error}</div>}
            <h5 className="mb-1" style={{ whiteSpace: 'pre-line' }}>{q.text}</h5>
            <p className="text-muted small mb-3">{q.multipleCorrect ? 'Select all correct answers' : 'Select one answer'}</p>
            <div className="d-grid gap-2">
              {q.options.map((o, i) => (
                <button key={o.id} type="button"
                        className={`btn btn-outline-secondary option-btn ${picked.includes(o.id) ? 'selected' : ''}`}
                        onClick={() => choose(q, o.id)}>
                  <input className="form-check-input me-2" type={q.multipleCorrect ? 'checkbox' : 'radio'} readOnly
                         checked={picked.includes(o.id)} tabIndex={-1} />
                  <strong className="me-2">{String.fromCharCode(65 + i)}.</strong>{o.text}
                </button>
              ))}
            </div>
          </div>
          <div className="card-footer bg-white d-flex justify-content-between">
            <button className="btn btn-outline-secondary" disabled={current === 0} onClick={() => setCurrent(current - 1)}>← Previous</button>
            {current < attempt.questions.length - 1
              ? <button className="btn btn-primary" onClick={() => setCurrent(current + 1)}>Next →</button>
              : <button className="btn btn-success" disabled={submitting} onClick={() => submit(false)}>{submitting ? 'Submitting…' : 'Submit Quiz'}</button>}
          </div>
        </div>
      </div>

      <div className="col-lg-4">
        <div className="card border-0 shadow-sm p-3">
          <h6>Questions</h6>
          <p className="small text-muted mb-2">{answeredCount} of {attempt.questions.length} answered</p>
          <div className="d-flex flex-wrap gap-2 palette mb-3">
            {attempt.questions.map((x, i) => (
              <button key={x.id} type="button"
                      className={`btn btn-sm ${i === current ? 'btn-primary' : (answers[x.id] || []).length ? 'btn-success' : 'btn-outline-secondary'}`}
                      onClick={() => setCurrent(i)}>{i + 1}</button>
            ))}
          </div>
          <button className="btn btn-success w-100" disabled={submitting} onClick={() => submit(false)}>
            {submitting ? 'Submitting…' : 'Submit Quiz'}
          </button>
          <p className="small text-muted mt-2 mb-0">The quiz is submitted automatically when the timer reaches 00:00.</p>
        </div>
      </div>
    </div>
  );
}
