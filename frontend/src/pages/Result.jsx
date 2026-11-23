import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { quizApi, errorMessage } from '../api/client';
import { formatDateTime, scoreColor } from '../utils';

export default function Result() {
  const { attemptId } = useParams();
  const location = useLocation();
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    quizApi.result(attemptId).then(setResult).catch((e) => setError(errorMessage(e)));
  }, [attemptId]);

  if (error) return <div className="alert alert-danger">{error}</div>;
  if (!result) return <div className="text-center py-5">Loading result…</div>;

  const s = result.summary;
  const color = scoreColor(s.percentage);
  const minutes = Math.max(0, Math.round((new Date(s.submittedAt) - new Date(s.startedAt)) / 1000));

  return (
    <>
      <div className="card border-0 shadow-sm p-4 mb-4 text-center">
        {(location.state?.auto || s.timedOut) && <div className="alert alert-warning">⏱ Time ran out – your answers were submitted automatically.</div>}
        <h3 className="mb-3">{s.quizTitle}</h3>
        <div className={`score-ring border border-5 border-${color} text-${color}`}>{s.percentage}%</div>
        <p className="fs-5 mt-3 mb-1">You scored <strong>{s.score}</strong> out of <strong>{s.totalQuestions}</strong></p>
        <p className="text-muted small">Submitted {formatDateTime(s.submittedAt)} · Time taken {Math.floor(minutes / 60)}m {minutes % 60}s</p>
        <div className="d-flex gap-2 justify-content-center">
          <Link to={`/quiz/${s.quizId}`} className="btn btn-outline-primary">Retake Quiz</Link>
          <Link to="/" className="btn btn-primary">More Quizzes</Link>
        </div>
      </div>

      <h4 className="mb-3">Answer Review</h4>
      {result.review.map((r, i) => (
        <div key={r.questionId} className={`card border-0 shadow-sm mb-3 ${r.correct ? 'review-correct' : 'review-wrong'}`}>
          <div className="card-body">
            <div className="d-flex justify-content-between">
              <h6 style={{ whiteSpace: 'pre-line' }}>{i + 1}. {r.text}</h6>
              <span className={`badge align-self-start ${r.correct ? 'bg-success' : 'bg-danger'}`}>{r.correct ? 'Correct' : r.selectedOptionIds.length ? 'Wrong' : 'Not answered'}</span>
            </div>
            <ul className="list-unstyled mb-0">
              {r.options.map((o) => {
                const chosen = r.selectedOptionIds.includes(o.id);
                return (
                  <li key={o.id} className={`py-1 ${o.correct ? 'text-success fw-semibold' : chosen ? 'text-danger' : ''}`}>
                    {o.correct ? '✔' : chosen ? '✘' : '○'} {o.text}
                    {chosen && <span className="badge bg-light text-dark ms-2">your answer</span>}
                  </li>
                );
              })}
            </ul>
          </div>
        </div>
      ))}
    </>
  );
}
