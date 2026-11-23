import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { quizApi, errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function Home() {
  const { user } = useAuth();
  const [quizzes, setQuizzes] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    quizApi.list().then(setQuizzes).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false));
  }, []);

  const shown = quizzes.filter((q) => `${q.title} ${q.description || ''}`.toLowerCase().includes(search.toLowerCase()));

  return (
    <>
      <section className="hero p-4 p-md-5 mb-4">
        <h1 className="fw-bold">Test your knowledge</h1>
        <p className="lead mb-3">Pick a quiz, beat the timer and get your score instantly.</p>
        {!user && <Link to="/register" className="btn btn-light btn-lg">Get started – it's free</Link>}
      </section>

      <div className="d-flex flex-wrap gap-2 justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Available Quizzes</h4>
        <input className="form-control" style={{ maxWidth: 280 }} placeholder="Search quizzes…" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {loading && <p>Loading quizzes… (the server may take up to a minute to wake up)</p>}
      {!loading && shown.length === 0 && !error && <div className="text-center text-muted py-5">No quizzes available right now.</div>}

      <div className="row g-4">
        {shown.map((q) => (
          <div className="col-sm-6 col-lg-4" key={q.id}>
            <div className="card h-100 border-0 shadow-sm quiz-card">
              <div className="card-body d-flex flex-column">
                <h5 className="card-title">{q.title}</h5>
                <p className="text-muted flex-grow-1">{q.description}</p>
                <div className="d-flex gap-3 small text-muted mb-3">
                  <span>❓ {q.questionCount} questions</span>
                  <span>⏱ {q.timeLimitMinutes} min</span>
                </div>
                <Link to={user ? `/quiz/${q.id}` : '/login'} state={user ? undefined : { from: `/quiz/${q.id}` }}
                      className="btn btn-primary">{user ? 'Start Quiz' : 'Login to start'}</Link>
              </div>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
