import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getRehearsals,
  createRehearsal,
  joinRehearsal,
  cancelAttendance,
  getAttendances,
} from '../../api/rehearsals';
import type { AttendanceResponse } from '../../api/rehearsals';
import { useAuthStore } from '../../store/authStore';

interface Props {
  bandId: number;
  isLeader: boolean;
}

function AttendanceSection({
  bandId,
  rehearsalId,
  currentUserId,
}: {
  bandId: number;
  rehearsalId: number;
  currentUserId: number | null;
}) {
  const { data } = useQuery({
    queryKey: ['attendances', rehearsalId],
    queryFn: () => getAttendances(bandId, rehearsalId).then((r) => r.data),
  });

  if (!data || data.length === 0) {
    return <p className="text-xs text-gray-400 mt-2">참석자 없음</p>;
  }

  return (
    <div className="mt-2">
      <p className="text-xs text-gray-500 mb-1">참석자 {data.length}명</p>
      <div className="flex flex-wrap gap-1">
        {data.map((a: AttendanceResponse) => (
          <span
            key={a.attendanceId}
            className={`text-xs px-2 py-0.5 rounded-full ${
              a.userId === currentUserId
                ? 'bg-purple-100 text-purple-600'
                : 'bg-gray-100 text-gray-600'
            }`}
          >
            {a.userId === currentUserId ? `나 (${a.nickname})` : a.nickname}
          </span>
        ))}
      </div>
    </div>
  );
}

export default function RehearsalsTab({ bandId, isLeader }: Props) {
  const { isLoggedIn, userId } = useAuthStore();
  const queryClient = useQueryClient();

  const [showForm, setShowForm] = useState(false);
  const [formTitle, setFormTitle] = useState('');
  const [formDate, setFormDate] = useState('');
  const [formLocation, setFormLocation] = useState('');
  const [formCapacity, setFormCapacity] = useState(10);
  const [formError, setFormError] = useState('');
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const { data: rehearsals, isLoading } = useQuery({
    queryKey: ['rehearsals', bandId],
    queryFn: () => getRehearsals(bandId).then((r) => r.data),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createRehearsal(bandId, formTitle, formDate, formLocation, formCapacity),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rehearsals', bandId] });
      setShowForm(false);
      setFormTitle('');
      setFormDate('');
      setFormLocation('');
      setFormCapacity(10);
      setFormError('');
    },
    onError: (e: Error) => setFormError(e.message),
  });

  const joinMutation = useMutation({
    mutationFn: (rehearsalId: number) => joinRehearsal(bandId, rehearsalId),
    onSuccess: (_, rehearsalId) => {
      queryClient.invalidateQueries({ queryKey: ['rehearsals', bandId] });
      queryClient.invalidateQueries({ queryKey: ['attendances', rehearsalId] });
    },
    onError: (e: Error) => alert(e.message),
  });

  const cancelMutation = useMutation({
    mutationFn: (rehearsalId: number) => cancelAttendance(bandId, rehearsalId),
    onSuccess: (_, rehearsalId) => {
      queryClient.invalidateQueries({ queryKey: ['rehearsals', bandId] });
      queryClient.invalidateQueries({ queryKey: ['attendances', rehearsalId] });
    },
    onError: (e: Error) => alert(e.message),
  });

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString('ko-KR', {
      year: 'numeric', month: 'long', day: 'numeric', weekday: 'short', hour: '2-digit', minute: '2-digit',
    });

  const isPast = (dateStr: string) => new Date(dateStr) < new Date();

  return (
    <div className="space-y-4">
      {isLeader && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold">합주 일정 관리</h3>
            <button
              onClick={() => setShowForm(!showForm)}
              className="text-sm bg-purple-600 text-white px-3 py-1.5 rounded-lg hover:bg-purple-700"
            >
              + 일정 추가
            </button>
          </div>
          {showForm && (
            <div className="bg-gray-50 rounded-xl p-4 space-y-3">
              <input
                type="text"
                value={formTitle}
                onChange={(e) => setFormTitle(e.target.value)}
                placeholder="제목 *"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              <input
                type="datetime-local"
                value={formDate}
                onChange={(e) => setFormDate(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              <input
                type="text"
                value={formLocation}
                onChange={(e) => setFormLocation(e.target.value)}
                placeholder="장소 (선택)"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              <div className="flex items-center gap-2">
                <label className="text-sm text-gray-600 whitespace-nowrap">최대 인원</label>
                <input
                  type="number"
                  value={formCapacity}
                  onChange={(e) => setFormCapacity(Number(e.target.value))}
                  min={1}
                  className="w-20 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
              {formError && <p className="text-red-500 text-sm">{formError}</p>}
              <div className="flex gap-2">
                <button
                  onClick={() => createMutation.mutate()}
                  disabled={!formTitle || !formDate || createMutation.isPending}
                  className="flex-1 bg-purple-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-purple-700 disabled:opacity-50"
                >
                  {createMutation.isPending ? '추가 중...' : '추가'}
                </button>
                <button
                  onClick={() => { setShowForm(false); setFormError(''); }}
                  className="flex-1 border border-gray-300 text-gray-600 py-2 rounded-lg text-sm"
                >
                  취소
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
        {!isLeader && <h3 className="font-semibold mb-3">합주 일정</h3>}
        {isLoading ? (
          <p className="text-sm text-gray-400 text-center py-4">불러오는 중...</p>
        ) : rehearsals && rehearsals.length > 0 ? (
          <ul className="space-y-3">
            {rehearsals
              .sort(
                (a, b) =>
                  new Date(a.rehearsalDate).getTime() -
                  new Date(b.rehearsalDate).getTime()
              )
              .map((r) => {
                const past = isPast(r.rehearsalDate);
                const expanded = expandedId === r.rehearsalId;
                const full = r.currentAttendees >= r.maxCapacity;

                return (
                  <li
                    key={r.rehearsalId}
                    className={`border rounded-xl p-4 ${past ? 'border-gray-100 opacity-60' : 'border-gray-200'}`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-sm">{r.title}</p>
                        <p className="text-xs text-gray-500 mt-0.5">{formatDate(r.rehearsalDate)}</p>
                        {r.location && (
                          <p className="text-xs text-gray-400">📍 {r.location}</p>
                        )}
                        <p className="text-xs text-gray-400 mt-1">
                          참석 {r.currentAttendees}/{r.maxCapacity}명
                          {full && !past && (
                            <span className="ml-1 text-red-500">마감</span>
                          )}
                        </p>
                      </div>
                      <div className="flex items-center gap-1 shrink-0">
                        {!past && isLoggedIn() && (
                          <>
                            <button
                              onClick={() => joinMutation.mutate(r.rehearsalId)}
                              disabled={joinMutation.isPending || full}
                              className="text-xs bg-green-600 text-white px-2 py-1 rounded-lg hover:bg-green-700 disabled:opacity-50"
                            >
                              참석
                            </button>
                            <button
                              onClick={() => cancelMutation.mutate(r.rehearsalId)}
                              disabled={cancelMutation.isPending}
                              className="text-xs border border-gray-300 text-gray-600 px-2 py-1 rounded-lg hover:bg-gray-50 disabled:opacity-50"
                            >
                              취소
                            </button>
                          </>
                        )}
                        {isLoggedIn() && (
                          <button
                            onClick={() => setExpandedId(expanded ? null : r.rehearsalId)}
                            className="text-xs text-gray-400 hover:text-gray-600 px-1"
                          >
                            {expanded ? '▲' : '▼'}
                          </button>
                        )}
                      </div>
                    </div>
                    {expanded && (
                      <AttendanceSection
                        bandId={bandId}
                        rehearsalId={r.rehearsalId}
                        currentUserId={userId}
                      />
                    )}
                  </li>
                );
              })}
          </ul>
        ) : (
          <p className="text-sm text-gray-400 text-center py-4">
            {isLeader ? '+ 일정 추가 버튼으로 합주 일정을 등록하세요.' : '등록된 합주 일정이 없습니다.'}
          </p>
        )}
      </div>
    </div>
  );
}