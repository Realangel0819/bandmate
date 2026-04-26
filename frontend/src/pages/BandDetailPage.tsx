import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getBand, getBandMembers, removeMember } from '../api/bands';
import { useAuthStore } from '../store/authStore';
import MembersTab from './tabs/MembersTab';
import SongsTab from './tabs/SongsTab';
import RehearsalsTab from './tabs/RehearsalsTab';

const positionLabel: Record<string, string> = {
  VOCAL: '보컬', GUITAR: '기타', BASS: '베이스',
  DRUM: '드럼', KEYBOARD: '키보드', ETC: '기타',
};

const TABS = ['멤버 관리', '곡 목록', '합주 일정'] as const;
type Tab = (typeof TABS)[number];

export default function BandDetailPage() {
  const { bandId } = useParams<{ bandId: string }>();
  const navigate = useNavigate();
  const { userId } = useAuthStore();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<Tab>('멤버 관리');

  const id = Number(bandId);

  const { data: band, isLoading, error } = useQuery({
    queryKey: ['band', id],
    queryFn: () => getBand(id).then((r) => r.data),
    enabled: !isNaN(id),
  });

  const { data: members, isLoading: membersLoading } = useQuery({
    queryKey: ['bandMembers', id],
    queryFn: () => getBandMembers(id).then((r) => r.data),
    enabled: !isNaN(id),
    retry: 1,
  });

  const removeMemberMutation = useMutation({
    mutationFn: (memberId: number) => removeMember(id, memberId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['bandMembers', id] }),
    onError: (e: Error) => alert(e.message),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64 text-gray-400">
        불러오는 중...
      </div>
    );
  }

  if (error || !band) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-8 text-center">
        <p className="text-red-500 mb-4">밴드를 찾을 수 없습니다.</p>
        <button onClick={() => navigate('/')} className="text-purple-600 hover:underline text-sm">
          홈으로 돌아가기
        </button>
      </div>
    );
  }

  const isLeader = userId === band.leaderId;

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      {/* 헤더 */}
      <div className="mb-6">
        <button
          onClick={() => navigate('/')}
          className="text-sm text-gray-400 hover:text-gray-600 mb-3 block"
        >
          ← 홈으로
        </button>
        <h1 className="text-2xl font-bold">{band.name}</h1>
        {band.description && (
          <p className="text-sm text-gray-500 mt-1">{band.description}</p>
        )}
        <div className="flex gap-3 mt-2 text-xs text-gray-400">
          <span>밴드 ID: {band.bandId}</span>
          {isLeader && (
            <span className="bg-purple-100 text-purple-600 px-2 py-0.5 rounded-full font-medium">
              리더
            </span>
          )}
        </div>
      </div>

      {/* 현재 멤버 로스터 */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-4 mb-6">
        <h2 className="text-sm font-semibold text-gray-500 mb-3">
          멤버 {members ? `(${members.length}명)` : ''}
        </h2>
        {membersLoading ? (
          <p className="text-xs text-gray-400">불러오는 중...</p>
        ) : members && members.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {members.map((m) => {
              const isThisLeader = m.userId === band?.leaderId;
              const canKick = isLeader && !isThisLeader;
              const canLeave = !isLeader && m.userId === userId;
              return (
                <div
                  key={m.memberId}
                  className="flex items-center gap-1.5 bg-gray-50 border border-gray-100 rounded-xl px-3 py-2"
                >
                  <span className="text-sm font-medium text-gray-800">{m.nickname}</span>
                  <span className="text-xs text-gray-400">{positionLabel[m.position] ?? m.position}</span>
                  {isThisLeader && (
                    <span className="text-xs bg-purple-100 text-purple-600 px-1.5 py-0.5 rounded-full leading-none">
                      리더
                    </span>
                  )}
                  {(canKick || canLeave) && (
                    <button
                      onClick={() => {
                        const msg = canKick ? `"${m.nickname}"을(를) 강퇴하시겠습니까?` : '밴드에서 탈퇴하시겠습니까?';
                        if (confirm(msg)) removeMemberMutation.mutate(m.memberId);
                      }}
                      disabled={removeMemberMutation.isPending}
                      className="text-xs text-red-400 hover:text-red-600 ml-1 disabled:opacity-50"
                    >
                      {canKick ? '강퇴' : '탈퇴'}
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        ) : (
          <p className="text-xs text-gray-400">멤버가 없습니다.</p>
        )}
      </div>

      {/* 탭 */}
      <div className="flex gap-1 bg-gray-100 p-1 rounded-xl mb-6">
        {TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`flex-1 py-2 text-sm font-medium rounded-lg transition-colors ${
              activeTab === tab
                ? 'bg-white text-purple-600 shadow-sm'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* 탭 컨텐츠 */}
      {activeTab === '멤버 관리' && (
        <MembersTab bandId={id} isLeader={isLeader} />
      )}
      {activeTab === '곡 목록' && (
        <SongsTab bandId={id} isLeader={isLeader} maxVotesPerPerson={band.maxVotesPerPerson ?? 1} />
      )}
      {activeTab === '합주 일정' && (
        <RehearsalsTab bandId={id} isLeader={isLeader} />
      )}
    </div>
  );
}