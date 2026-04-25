import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

export default function Navbar() {
  const { nickname, isLoggedIn, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
      <Link to="/" className="text-xl font-bold text-purple-600">
        🎸 BandMate
      </Link>
      <div className="flex items-center gap-4">
        {isLoggedIn() ? (
          <>
            <span className="text-sm text-gray-600">{nickname}</span>
            <button
              onClick={handleLogout}
              className="text-sm text-gray-500 hover:text-gray-900"
            >
              로그아웃
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="text-sm text-gray-600 hover:text-gray-900">
              로그인
            </Link>
            <Link
              to="/signup"
              className="text-sm bg-purple-600 text-white px-3 py-1.5 rounded-lg hover:bg-purple-700"
            >
              회원가입
            </Link>
          </>
        )}
      </div>
    </nav>
  );
}