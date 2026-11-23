import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi, errorMessage } from '../api/client';
import { formatDateTime } from '../utils';

export default function AdminQuizzes() {
  const [quizzes, setQuizzes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    adminApi.quizzes().then(setQuizzes).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false));
  }, []);

  const togglePublish = async (q) => {
    setError('');
    try {
      const updated = await adminApi.updateQuiz(q.id, { title: q.title, description: q.description, timeLimitMinutes: q.timeLimitMinutes, published: !q.published });
      setQuizzes(quizzes.map((x) => (x.id === q.id ? { ...x, published: updated.published } : x)));
    } catch (e) { setError(errorMessage(e)); }
  };

  const remove = async (q) => {
    if (!window.confirm(`Delete "${q.title}" with all its questions and results?`)) return;
    try {
      await adminApi.deleteQuiz(q.id);
      setQuizzes(quizzes.filter((x) => x.id !== q.id));
    } catch (e) { setError(errorMessage(e)); }
  };

  return (
    <>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h3 className="mb-0">Manage Quizzes</h3>
        <Link to="/admin/quizzes/new" className="btn btn-primary">+ New Quiz</Link>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="table-responsive card border-0 shadow-sm">
        <table className="table table-hover align-middle mb-0">
          <thead className="table-light"><tr><th>Title</th><th>Questions</th><th>Time</th><th>Status</th><th>Created</th><th className="text-end">Actions</th></tr></thead>
          <tbody>
            {loading && <tr><td colSpan="6" className="text-center py-4">Loading…</td></tr>}
            {!loading && quizzes.length === 0 && <tr><td colSpan="6" className="text-center py-4 text-muted">No quizzes yet.</td></tr>}
            {quizzes.map((q) => (
              <tr key={q.id}>
                <td><Link to={`/admin/quizzes/${q.id}`}>{q.title}</Link></td>
                <td>{q.questionCount}</td>
                <td>{q.timeLimitMinutes} min</td>
                <td><span className={`badge ${q.published ? 'bg-success' : 'bg-secondary'}`}>{q.published ? 'Published' : 'Draft'}</span></td>
                <td>{formatDateTime(q.createdAt)}</td>
                <td className="text-end text-nowrap">
                  <button className="btn btn-sm btn-outline-secondary me-2" onClick={() => togglePublish(q)}>{q.published ? 'Unpublish' : 'Publish'}</button>
                  <Link className="btn btn-sm btn-outline-primary me-2" to={`/admin/quizzes/${q.id}`}>Edit</Link>
                  <Link className="btn btn-sm btn-outline-info me-2" to={`/admin/results?quizId=${q.id}`}>Results</Link>
                  <button className="btn btn-sm btn-outline-danger" onClick={() => remove(q)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
