import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { quizApi, errorMessage } from '../api/client';
import { formatDateTime, scoreColor } from '../utils';

export default function MyResults() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    quizApi.myResults().then(setItems).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false));
  }, []);

  const avg = items.length ? (items.reduce((s, a) => s + a.percentage, 0) / items.length).toFixed(1) : 0;
  const best = items.length ? Math.max(...items.map((a) => a.percentage)) : 0;

  return (
    <>
      <h3 className="mb-3">My Results</h3>
      <div className="row g-3 mb-4">
        {[['Quizzes taken', items.length, 'primary'], ['Average score', `${avg}%`, 'info'], ['Best score', `${best}%`, 'success']].map(([label, v, c]) => (
          <div className="col-4" key={label}>
            <div className={`card border-0 shadow-sm border-start border-4 border-${c}`}>
              <div className="card-body"><div className="text-muted small">{label}</div><div className="fs-3 fw-bold">{v}</div></div>
            </div>
          </div>
        ))}
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="table-responsive card border-0 shadow-sm">
        <table className="table table-hover align-middle mb-0">
          <thead className="table-light"><tr><th>Quiz</th><th>Score</th><th>%</th><th>Submitted</th><th></th></tr></thead>
          <tbody>
            {loading && <tr><td colSpan="5" className="text-center py-4">Loading…</td></tr>}
            {!loading && items.length === 0 && <tr><td colSpan="5" className="text-center py-4 text-muted">No attempts yet. <Link to="/">Take a quiz</Link></td></tr>}
            {items.map((a) => (
              <tr key={a.id}>
                <td>{a.quizTitle}{a.timedOut && <span className="badge bg-warning text-dark ms-2">timed out</span>}</td>
                <td>{a.score} / {a.totalQuestions}</td>
                <td><span className={`badge bg-${scoreColor(a.percentage)}`}>{a.percentage}%</span></td>
                <td>{formatDateTime(a.submittedAt)}</td>
                <td className="text-end"><Link to={`/results/${a.id}`} className="btn btn-sm btn-outline-primary">Review</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
