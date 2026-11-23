import { Routes, Route, Navigate } from 'react-router-dom';
import NavBar from './components/NavBar';
import ProtectedRoute from './components/ProtectedRoute';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import TakeQuiz from './pages/TakeQuiz';
import Result from './pages/Result';
import MyResults from './pages/MyResults';
import AdminQuizzes from './pages/AdminQuizzes';
import QuizEditor from './pages/QuizEditor';
import AdminResults from './pages/AdminResults';
import AdminUsers from './pages/AdminUsers';

export default function App() {
  return (
    <>
      <NavBar />
      <main className="container py-4">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/quiz/:id" element={<ProtectedRoute><TakeQuiz /></ProtectedRoute>} />
          <Route path="/results" element={<ProtectedRoute><MyResults /></ProtectedRoute>} />
          <Route path="/results/:attemptId" element={<ProtectedRoute><Result /></ProtectedRoute>} />
          <Route path="/admin/quizzes" element={<ProtectedRoute adminOnly><AdminQuizzes /></ProtectedRoute>} />
          <Route path="/admin/quizzes/new" element={<ProtectedRoute adminOnly><QuizEditor /></ProtectedRoute>} />
          <Route path="/admin/quizzes/:id" element={<ProtectedRoute adminOnly><QuizEditor /></ProtectedRoute>} />
          <Route path="/admin/results" element={<ProtectedRoute adminOnly><AdminResults /></ProtectedRoute>} />
          <Route path="/admin/users" element={<ProtectedRoute adminOnly><AdminUsers /></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <footer className="text-center text-muted small py-4">© {new Date().getFullYear()} QuizMaster · Online Quiz Application</footer>
    </>
  );
}
