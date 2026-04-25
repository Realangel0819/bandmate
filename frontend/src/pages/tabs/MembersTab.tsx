import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getBandRecruits,
  getBandApplications,
  getBandMembers,
  approveApplication,
  rejectApplication,
  createRecruit,
  applyBand,
} from '../../api/bands';
import type { RecruitResponse } from '../../api/bands';
import { useAuthStore } from '../../store/authStore';

const POSITIONS = ['VOCAL', 'GUITAR', 'BASS', 'DRUM', 'KEYBOARD', 'ETC'];

const positionLabel: Record<string, string> = {
  VOCAL: '보컬', GUITAR: '기타', BASS: '베이스',
  DRUM: '드럼', KEYBOARD: '키보드', ETC: '기타(ETC)',
};

const statusLabel: Record<string, string> = {
  PENDING: '대기중', APPROVED: '승인됨', REJECTED: '거절됨',
};
const statusColor: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  APPROVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-600',
};

interface Props {
  bandId: number;
  isLeader: boolean;
}

export default function MembersTab({ bandId, isLeader }: Props) {
  const { isLoggedIn } = useAuthStore();
  const queryClient = useQueryClient();

  const [showRecruitForm, setShowRecruitForm] = useState(false);
  const [recruitPos, setRecruitPos] = useState('GUITAR');
  const [recruitCount, setRecruitCount] = useState(1);
  const [recruitError, setRecruitError] = useState('');

  const [applyingTo, setApplyingTo] = useState<RecruitResponse | null>(null);
  const [introduction, setIntroduction] = useState('');
  const [applyError, setApplyError] = useState('');

  const { data: members, isLoading: membersLoading } = useQuery({
    queryKey: ['bandMembers', bandId],
    queryFn: () => getBandMembers(bandId).then((r) => r.data),
  });

  const { data: recruits, isLoading: recruitsLoading } = useQuery({
    queryKey: ['recruits', bandId],
    queryFn: () => getBandRecruits(bandId).then((r) => r.data),
  });

  const { data: applications, isLoading: appsLoading } = useQuery({
    queryKey: ['applications', bandId],
    queryFn: () => getBandApplications(bandId).then((r) => r.data),
    enabled: isLeader,
  });

  const recruitMutation = useMutation({
    mutationFn: () => createRecruit(bandId, recruitPos, recruitCount),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recruits', bandId] });
      setShowRecruitForm(false);
      setRecruitError('');
    },
    onError: (e: Error) => setRecruitError(e.message),
  });

  const applyMutation = useMutation({
    mutationFn: () => applyBand(bandId, applyingTo!.recruitId, applyingTo!.position, introduction),
    onSuccess: () => {
      setApplyingTo(null);
      setIntroduction('');
      setApplyError('');
    },
    onError: (e: Error) => setApplyError(e.message),
  });

  const approveMutation = useMutation({
    mutationFn: (appId: number) => approveApplication(bandId, appId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['applications', bandId] }),
    onError: (e: Error) => alert(e.message),
  });

  const rejectMutation = useMutation({
    mutationFn: (appId: number) => rejectApplication(bandId, appId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['applications', bandId] }),
    onError: (e: Error) => alert(e.message),
  });

  return (
    <div className="space-y-4">
      {/* 현재 멤버 */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
        <h3 className="font-semibold mb-3">현재 멤버 {members ? `(${members.length}명)` : ''}</h3>
        {membersLoading ? (
          <p className="text-sm text-gray-400 text-center py-4">불러오는 중...</p>
        ) : members && members.length > 0 ? (
          <ul className="space-y-2">
            {members.map((m) => (
              <li key={m.memberId} className="flex items-center justify-between border border-gray-100 rounded-xl p-3">
                <div>
                  <span className="font-medium text-sm">{m.nickname}</span>
                  <span className="text-xs text-gray-400 ml-2">{positionLabel[m.position] ?? m.position}</span>
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-gray-400 text-center py-4">멤버가 없습니다.</p>
        )}
      </div>

      {/* 모집 공고 */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
        <div className="flex items-center justify-between mb-3">
          <h3 className="font-semibold">모집 공고</h3>
          {isLeader && (
            <button
              onClick={() => setShowRecruitForm(!showRecruitForm)}
              className="text-sm bg-purple-600 text-white px-3 py-1.5 rounded-lg hover:bg-purple-700"
            >
              + 공고 등록
            </button>
          )}
        </div>

        {showRecruitForm && (
          <div className="bg-gray-50 rounded-xl p-4 space-y-3 mb-4">
            <div className="flex gap-3">
              <select
                value={recruitPos}
                onChange={(e) => setRecruitPos(e.target.value)}
                className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm"
              >
                {POSITIONS.map((p) => (
                  <option key={p} value={p}>{positionLabel[p]}</option>
                ))}
              </select>
              <div className="flex items-center gap-2">
                <label className="text-sm text-gray-500 whitespace-nowrap">모집 인원</label>
                <input
                  type="number"
                  value={recruitCount}
                  onChange={(e) => setRecruitCount(Number(e.target.value))}
                  min={1}
                  className="w-16 border border-gray-300 rounded-lg px-2 py-2 text-sm text-center"
                />
              </div>
            </div>
            {recruitError && <p className="text-red-500 text-sm">{recruitError}</p>}
            <div className="flex gap-2">
              <button
                onClick={() => recruitMutation.mutate()}
                disabled={recruitMutation.isPending}
                className="flex-1 bg-purple-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-purple-700 disabled:opacity-50"
              >
                등록
              </button>
              <button
                onClick={() => { setShowRecruitForm(false); setRecruitError(''); }}
                className="flex-1 border border-gray-300 text-gray-600 py-2 rounded-lg text-sm"
              >
                취소
              </button>
            </div>
          </div>
        )}

        {recruitsLoading ? (
          <p className="text-sm text-gray-400 text-center py-4">불러오는 중...</p>
        ) : recruits && recruits.length > 0 ? (
          <ul className="space-y-2">
            {recruits.map((r) => {
              const full = r.currentCount >= r.requiredCount;
              return (
                <li
                  key={r.recruitId}
                  className="flex items-center justify-between border border-gray-100 rounded-xl p-3"
                >
                  <div>
                    <span className="font-medium text-sm">{positionLabel[r.position] ?? r.position}</span>
                    <span className={`text-xs ml-2 ${full ? 'text-red-500' : 'text-gray-400'}`}>
                      {r.currentCount}/{r.requiredCount}명
                      {full && ' (마감)'}
                    </span>
                  </div>
                  {isLoggedIn() && !isLeader && !full && (
                    <button
                      onClick={() => { setApplyingTo(r); setApplyError(''); setIntroduction(''); }}
                      className="text-xs bg-green-600 text-white px-3 py-1 rounded-lg hover:bg-green-700"
                    >
                      지원하기
                    </button>
                  )}
                </li>
              );
            })}
          </ul>
        ) : (
          <p className="text-sm text-gray-400 text-center py-4">등록된 모집 공고가 없습니다.</p>
        )}
      </div>

      {/* 지원 모달 */}
      {applyingTo && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl p-6 w-full max-w-sm mx-4 shadow-xl">
            <h3 className="font-bold text-lg mb-1">지원하기</h3>
            <p className="text-sm text-gray-500 mb-4">
              {positionLabel[applyingTo.position] ?? applyingTo.position} 포지션에 지원합니다.
            </p>
            <textarea
              value={introduction}
              onChange={(e) => setIntroduction(e.target.value)}
              placeholder="자기소개 / 경력 / 연락처 등 (선택)"
              rows={4}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500 resize-none mb-3"
            />
            {applyError && <p className="text-red-500 text-sm mb-3">{applyError}</p>}
            <div className="flex gap-2">
              <button
                onClick={() => applyMutation.mutate()}
                disabled={applyMutation.isPending}
                className="flex-1 bg-green-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-green-700 disabled:opacity-50"
              >
                {applyMutation.isPending ? '지원 중...' : '지원 완료'}
              </button>
              <button
                onClick={() => { setApplyingTo(null); setApplyError(''); setIntroduction(''); }}
                className="flex-1 border border-gray-300 text-gray-600 py-2 rounded-lg text-sm"
              >
                취소
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 지원자 목록 (리더만) */}
      {isLeader && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
          <h3 className="font-semibold mb-3">지원 현황</h3>
          {appsLoading ? (
            <p className="text-sm text-gray-400 text-center py-4">불러오는 중...</p>
          ) : applications && applications.length > 0 ? (
            <ul className="space-y-3">
              {applications.map((app) => (
                <li key={app.applicationId} className="border border-gray-100 rounded-xl p-3">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-medium text-sm">{app.nickname}</span>
                        <span className="text-xs text-gray-400">
                          {positionLabel[app.position] ?? app.position}
                        </span>
                        <span className={`text-xs px-2 py-0.5 rounded-full ${statusColor[app.status]}`}>
                          {statusLabel[app.status]}
                        </span>
                      </div>
                      {app.introduction && (
                        <p className="text-xs text-gray-500 mt-1 bg-gray-50 rounded-lg px-2 py-1.5 whitespace-pre-wrap">
                          {app.introduction}
                        </p>
                      )}
                    </div>
                    {app.status === 'PENDING' && (
                      <div className="flex gap-1 shrink-0">
                        <button
                          onClick={() => approveMutation.mutate(app.applicationId)}
                          className="text-xs bg-green-600 text-white px-2 py-1 rounded-lg hover:bg-green-700"
                        >
                          승인
                        </button>
                        <button
                          onClick={() => rejectMutation.mutate(app.applicationId)}
                          className="text-xs bg-red-500 text-white px-2 py-1 rounded-lg hover:bg-red-600"
                        >
                          거절
                        </button>
                      </div>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-gray-400 text-center py-4">아직 지원자가 없습니다.</p>
          )}
        </div>
      )}

      {!isLoggedIn() && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 text-center">
          <p className="text-sm text-gray-500">로그인 후 지원할 수 있습니다.</p>
        </div>
      )}
    </div>
  );
}
