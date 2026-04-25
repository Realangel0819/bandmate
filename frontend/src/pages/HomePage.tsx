import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getAllBands, getMyBands, createBand, deleteBand } from '../api/bands';
import type { BandResponse } from '../api/bands';
import { useAuthStore } from '../store/authStore';

export default function HomePage() {
  const { isLoggedIn, nickname, userId } = useAuthStore();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [bandName, setBandName] = useState('');
  const [bandDesc, setBandDesc] = useState('');
  const [searchId, setSearchId] = useState('');
  const [error, setError] = useState('');

  const { data: allBands, isLoading: allLoading } = useQuery({
    queryKey: ['allBands'],
    queryFn: () => getAllBands().then((r) => r.data),
  });

  const { data: myBands } = useQuery({
    queryKey: ['myBands'],
    queryFn: () => getMyBands().then((r) => r.data),
    enabled: isLoggedIn(),
  });

  const createMutation = useMutation({
    mutationFn: () => createBand(bandName, bandDesc),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myBands'] });
      queryClient.invalidateQueries({ queryKey: ['allBands'] });
      setShowCreateForm(false);
      setBandName('');
      setBandDesc('');
      setError('');
    },
    onError: (err: Error) => setError(err.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (bandId: number) => deleteBand(bandId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myBands'] });
      queryClient.invalidateQueries({ queryKey: ['allBands'] });
    },
    onError: (err: Error) => alert(err.message),
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const id = parseInt(searchId);
    if (!isNaN(id)) navigate(`/bands/${id}`);
  };

  const isMyBand = (band: BandResponse) =>
    isLoggedIn() && band.leaderId === userId;

  return (
    <div className="max-w-3xl mx-auto px-4 py-8 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">
            {isLoggedIn() ? `안녕하세요, ${nickname}님 👋` : 'BandMate'}
          </h1>
          <p className="text-sm text-gray-500 mt-1">밴드를 만들고 팀원을 모집하세요</p>
        </div>
        {!isLoggedIn() && (
          <Link
            to="/login"
            className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-purple-700"
          >
            로그인
          </Link>
        )}
      </div>

      {/* 밴드 ID 검색 */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
        <h2 className="font-semibold mb-3">밴드 ID로 검색</h2>
        <form onSubmit={handleSearch} className="flex gap-2">
          <input
            type="number"
            value={searchId}
            onChange={(e) => setSearchId(e.target.value)}
            placeholder="밴드 ID 입력"
            className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
          />
          <button
            type="submit"
            className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-purple-700"
          >
            이동
          </button>
        </form>
      </div>

      {/* 내 밴드 (로그인 시) */}
      {isLoggedIn() && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold">내 밴드</h2>
            <button
              onClick={() => setShowCreateForm(!showCreateForm)}
              className="text-sm bg-purple-600 text-white px-3 py-1.5 rounded-lg hover:bg-purple-700"
            >
              + 밴드 만들기
            </button>
          </div>

          {showCreateForm && (
            <div className="bg-gray-50 rounded-xl p-4 mb-4 space-y-3">
              <input
                type="text"
                value={bandName}
                onChange={(e) => setBandName(e.target.value)}
                placeholder="밴드 이름 *"
                maxLength={100}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              <textarea
                value={bandDesc}
                onChange={(e) => setBandDesc(e.target.value)}
                placeholder="밴드 설명 (선택)"
                rows={2}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none"
              />
              {error && <p className="text-red-500 text-sm">{error}</p>}
              <div className="flex gap-2">
                <button
                  onClick={() => createMutation.mutate()}
                  disabled={!bandName || createMutation.isPending}
                  className="flex-1 bg-purple-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-purple-700 disabled:opacity-50"
                >
                  {createMutation.isPending ? '생성 중...' : '만들기'}
                </button>
                <button
                  onClick={() => { setShowCreateForm(false); setError(''); }}
                  className="flex-1 border border-gray-300 text-gray-600 py-2 rounded-lg text-sm hover:bg-gray-50"
                >
                  취소
                </button>
              </div>
            </div>
          )}

          {myBands && myBands.length > 0 ? (
            <ul className="space-y-2">
              {myBands.map((band) => {
                const isBandLeader = band.leaderId === userId;
                return (
                  <li key={band.bandId} className="flex items-center justify-between p-3 rounded-xl hover:bg-gray-50">
                    <Link
                      to={`/bands/${band.bandId}`}
                      className="flex-1 font-medium text-gray-900 hover:text-purple-600"
                    >
                      {band.name}
                      <span className="text-xs text-gray-400 ml-2">#{band.bandId}</span>
                      {isBandLeader ? (
                        <span className="text-xs text-purple-500 ml-1">리더</span>
                      ) : (
                        <span className="text-xs text-green-500 ml-1">멤버</span>
                      )}
                    </Link>
                    {isBandLeader && (
                      <button
                        onClick={() => {
                          if (confirm(`"${band.name}" 밴드를 삭제하시겠습니까?`)) {
                            deleteMutation.mutate(band.bandId);
                          }
                        }}
                        className="text-xs text-red-400 hover:text-red-600 ml-4"
                      >
                        삭제
                      </button>
                    )}
                  </li>
                );
              })}
            </ul>
          ) : (
            <p className="text-sm text-gray-400 text-center py-4">가입한 밴드가 없습니다.</p>
          )}
        </div>
      )}

      {/* 전체 밴드 목록 */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
        <h2 className="font-semibold mb-4">전체 밴드</h2>
        {allLoading ? (
          <p className="text-sm text-gray-400 text-center py-4">불러오는 중...</p>
        ) : allBands && allBands.length > 0 ? (
          <ul className="space-y-2">
            {allBands.map((band) => (
              <li key={band.bandId}>
                <Link
                  to={`/bands/${band.bandId}`}
                  className="flex items-center justify-between p-3 rounded-xl hover:bg-gray-50 group"
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-gray-900 group-hover:text-purple-600 truncate">
                        {band.name}
                      </span>
                      <span className="text-xs text-gray-400 shrink-0">#{band.bandId}</span>
                      {isMyBand(band) && (
                        <span className="text-xs bg-purple-100 text-purple-600 px-1.5 py-0.5 rounded-full shrink-0">
                          내 밴드
                        </span>
                      )}
                    </div>
                    {band.description && (
                      <p className="text-xs text-gray-400 mt-0.5 truncate">{band.description}</p>
                    )}
                  </div>
                  <span className="text-xs text-gray-400 ml-3 shrink-0">
                    멤버 {band.memberCount}명
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-gray-400 text-center py-4">등록된 밴드가 없습니다.</p>
        )}
      </div>
    </div>
  );
}
