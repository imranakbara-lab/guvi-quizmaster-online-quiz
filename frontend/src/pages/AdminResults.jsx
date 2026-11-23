import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { adminApi, errorMessage } from '../api/client';
import { formatDateTime, scoreColor } from '../utils';

export default function AdminResults() {
  const [params, setParams] = useSearchParams();
  const quizId = params.get('quizId') || '';
  const [quizzes, setQuizzes] = useState([]);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => { adminApi.quizzes().then(setQuizzes).catch(() => {}); }, []);
  useEffect(() => {
    setLoading(true);
    adminApi.results(quizId || undefined).then(setResults).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false));
  }, [quizId]);

  return (
    <>
      <div className="d-flex flex-wrap gap-2 justify-content-between align-items-center mb-3">
        <h3 className="mb-0">{quizId ? 'Leaderboard' : 'All Results'}</h3>
        <select className="form-select" style={{ maxWidth: 300 }} value={quizId}
                onChange={(e) => setParams(e.target.value ? { quizId: e.target.value } : {})}>
          <option value="">All quizzes</option>
          {quizzes.map((q) => <option key={q.id} value={q.id}>{q.title}</option>)}
        </select>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="table-responsive card border-0 shadow-sm">
        <table className="table table-hover align-middle mb-0">
          <thead className="table-light"><tr><th>#</th><th>Participant</th><th>Quiz</th><th>Score</th><th>%</th><th>Submitted</th><th></th></tr></thead>
          <tbody>
            {loading && <tr><td colSpan="7" className="text-center py-4">Loading…</td></tr>}
            {!loading && results.length === 0 && <tr><td colSpan="7" className="text-center py-4 text-muted">No results yet.</td></tr>}
            {results.map((a, i) => (
              <tr key={a.id}>
                <td>{i + 1}</td>
                <td>{a.userName}<div className="small text-muted">{a.userEmail}</div></td>
                <td>{a.quizTitle}</td>
                <td>{a.score} / {a.totalQuestions}</td>
                <td><span className={`badge bg-${scoreColor(a.percentage)}`}>{a.percentage}%</span>{a.timedOut && <span className="badge bg-warning text-dark ms-1">timed out</span>}</td>
                <td>{formatDateTime(a.submittedAt)}</td>
                <td className="text-end"><Link className="btn btn-sm btn-outline-primary" to={`/results/${a.id}`}>View</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
