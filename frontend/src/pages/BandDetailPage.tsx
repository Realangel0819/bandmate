import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getBand } from '../api/bands';
import { useAuthStore } from '../store/authStore';
import MembersTab from './tabs/MembersTab';
import SongsTab from './tabs/SongsTab';
import RehearsalsTab from './tabs/RehearsalsTab';

const TABS = ['멤버 관리', '곡 목록', '합주 일정'] as const;
type Tab = (typeof TABS)[number];

export default function BandDetailPage() {
  const { bandId } = useParams<{ bandId: string }>();
  const navigate = useNavigate();
  const { userId } = useAuthStore();
  const [activeTab, setActiveTab] = useState<Tab>('멤버 관리');

  const id = Number(bandId);

  const { data: band, isLoading, error } = useQuery({
    queryKey: ['band', id],
    queryFn: () => getBand(id).then((r) => r.data),
    enabled: !isNaN(id),
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
        <MembersTab bandId={id} isLeader={isLeader} leaderId={band.leaderId} />
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