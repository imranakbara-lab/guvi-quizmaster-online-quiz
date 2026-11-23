import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function NavBar() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <nav className="navbar navbar-expand-lg navbar-dark" style={{ background: '#3d2a73' }}>
      <div className="container">
        <Link className="navbar-brand fw-bold" to="/">🧠 QuizMaster</Link>
        <button className="navbar-toggler" type="button" onClick={() => document.getElementById('nav').classList.toggle('show')}>
          <span className="navbar-toggler-icon" />
        </button>
        <div className="collapse navbar-collapse" id="nav">
          <ul className="navbar-nav me-auto">
            <li className="nav-item"><NavLink className="nav-link" to="/" end>Quizzes</NavLink></li>
            {user && <li className="nav-item"><NavLink className="nav-link" to="/results">My Results</NavLink></li>}
            {isAdmin && <li className="nav-item"><NavLink className="nav-link" to="/admin/quizzes">Manage Quizzes</NavLink></li>}
            {isAdmin && <li className="nav-item"><NavLink className="nav-link" to="/admin/results">All Results</NavLink></li>}
            {isAdmin && <li className="nav-item"><NavLink className="nav-link" to="/admin/users">Users</NavLink></li>}
          </ul>
          <ul className="navbar-nav">
            {user ? (
              <>
                <li className="nav-item"><span className="navbar-text me-3">Hi, {user.name}{isAdmin && ' (Admin)'}</span></li>
                <li className="nav-item">
                  <button className="btn btn-outline-light btn-sm mt-1" onClick={() => { logout(); navigate('/'); }}>Logout</button>
                </li>
              </>
            ) : (
              <>
                <li className="nav-item"><NavLink className="nav-link" to="/login">Login</NavLink></li>
                <li className="nav-item"><NavLink className="btn btn-light btn-sm mt-1 ms-lg-2" to="/register">Register</NavLink></li>
              </>
            )}
          </ul>
        </div>
      </div>
    </nav>
  );
}
