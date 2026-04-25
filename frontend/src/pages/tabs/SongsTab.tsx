import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getBandSongs,
  voteSong,
  selectSong,
  deselectSong,
  createAndAddSong,
  resetCandidates,
  resetVotes,
} from '../../api/songs';
import type { BandSongResponse } from '../../api/songs';
import { updateVoteSettings } from '../../api/bands';
import { useAuthStore } from '../../store/authStore';

interface Props {
  bandId: number;
  isLeader: boolean;
  maxVotesPerPerson: number;
}

export default function SongsTab({ bandId, isLeader, maxVotesPerPerson }: Props) {
  const { isLoggedIn } = useAuthStore();
  const queryClient = useQueryClient();

  const [showAddForm, setShowAddForm] = useState(false);
  const [title, setTitle] = useState('');
  const [artist, setArtist] = useState('');
  const [youtubeUrl, setYoutubeUrl] = useState('');
  const [addError, setAddError] = useState('');

  const [showVoteSettings, setShowVoteSettings] = useState(false);
  const [votesInput, setVotesInput] = useState(maxVotesPerPerson);

  const { data: songs, isLoading } = useQuery({
    queryKey: ['songs', bandId],
    queryFn: () => getBandSongs(bandId).then((r) => r.data),
  });

  const addMutation = useMutation({
    mutationFn: () => createAndAddSong(bandId, title, artist, youtubeUrl || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['songs', bandId] });
      setShowAddForm(false);
      setTitle('');
      setArtist('');
      setYoutubeUrl('');
      setAddError('');
    },
    onError: (e: Error) => setAddError(e.message),
  });

  const voteMutation = useMutation({
    mutationFn: (bandSongId: number) => voteSong(bandId, bandSongId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['songs', bandId] }),
    onError: (e: Error) => alert(e.message),
  });

  const selectMutation = useMutation({
    mutationFn: (bandSongId: number) => selectSong(bandId, bandSongId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['songs', bandId] }),
    onError: (e: Error) => alert(e.message),
  });

  const deselectMutation = useMutation({
    mutationFn: (bandSongId: number) => deselectSong(bandId, bandSongId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['songs', bandId] }),
    onError: (e: Error) => alert(e.message),
  });

  const resetCandidatesMutation = useMutation({
    mutationFn: () => resetCandidates(bandId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['songs', bandId] }),
    onError: (e: Error) => alert(e.message),
  });

  const resetVotesMutation = useMutation({
    mutationFn: () => resetVotes(bandId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['songs', bandId] }),
    onError: (e: Error) => alert(e.message),
  });

  const voteSettingsMutation = useMutation({
    mutationFn: () => updateVoteSettings(bandId, votesInput),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['band', bandId] });
      setShowVoteSettings(false);
    },
    onError: (e: Error) => alert(e.message),
  });

  const selectedSongs = songs?.filter((s) => s.isSelected) ?? [];
  const candidates = songs?.filter((s) => !s.isSelected) ?? [];

  const formatYoutubeUrl = (url: string) => {
    try {
      const u = new URL(url);
      const v = u.searchParams.get('v') ?? u.pathname.split('/').pop();
      return `https://www.youtube.com/watch?v=${v}`;
    } catch {
      return url;
    }
  };

  return (
    <div className="space-y-4">
      {/* 선정된 곡들 */}
      {selectedSongs.length > 0 && (
        <div className="bg-purple-50 border border-purple-200 rounded-2xl p-5">
          <p className="text-xs font-semibold text-purple-500 mb-3">
            🎵 선정된 곡 ({selectedSongs.length}곡)
          </p>
          <div className="space-y-2">
            {selectedSongs.map((s) => (
              <div key={s.bandSongId} className="flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="font-bold text-base truncate">{s.title}</p>
                  <p className="text-sm text-gray-600">{s.artist}</p>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="text-xs text-gray-400">👍 {s.voteCount}표</span>
                    {s.youtubeUrl && (
                      <a
                        href={formatYoutubeUrl(s.youtubeUrl)}
                        target="_blank"
                        rel="noreferrer"
                        className="text-xs text-red-500 hover:underline"
                      >
                        ▶ YouTube
                      </a>
                    )}
                  </div>
                </div>
                {isLeader && (
                  <button
                    onClick={() => deselectMutation.mutate(s.bandSongId)}
                    disabled={deselectMutation.isPending}
                    className="text-xs border border-gray-300 text-gray-500 px-2 py-1 rounded-lg hover:bg-gray-100 shrink-0"
                  >
                    선정 취소
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 후보곡 + 리더 도구 */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
        <div className="flex items-center justify-between mb-3">
          <h3 className="font-semibold">후보 곡 ({candidates.length})</h3>
          {isLeader && (
            <button
              onClick={() => setShowAddForm(!showAddForm)}
              className="text-sm bg-purple-600 text-white px-3 py-1.5 rounded-lg hover:bg-purple-700"
            >
              + 곡 추가
            </button>
          )}
        </div>

        {showAddForm && (
          <div className="bg-gray-50 rounded-xl p-4 space-y-3 mb-4">
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="곡 제목 *"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <input
              type="text"
              value={artist}
              onChange={(e) => setArtist(e.target.value)}
              placeholder="아티스트 *"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <input
              type="url"
              value={youtubeUrl}
              onChange={(e) => setYoutubeUrl(e.target.value)}
              placeholder="유튜브 링크 (선택)"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            {addError && <p className="text-red-500 text-sm">{addError}</p>}
            <div className="flex gap-2">
              <button
                onClick={() => addMutation.mutate()}
                disabled={!title || !artist || addMutation.isPending}
                className="flex-1 bg-purple-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-purple-700 disabled:opacity-50"
              >
                {addMutation.isPending ? '추가 중...' : '추가'}
              </button>
              <button
                onClick={() => { setShowAddForm(false); setAddError(''); }}
                className="flex-1 border border-gray-300 text-gray-600 py-2 rounded-lg text-sm"
              >
                취소
              </button>
            </div>
          </div>
        )}

        {!isLoggedIn() && (
          <p className="text-xs text-gray-400 mb-3 text-center">로그인 후 투표할 수 있습니다.</p>
        )}

        {isLoading ? (
          <p className="text-sm text-gray-400 text-center py-4">불러오는 중...</p>
        ) : candidates.length > 0 ? (
          <div className="space-y-2">
            {candidates
              .sort((a, b) => b.voteCount - a.voteCount)
              .map((s: BandSongResponse) => (
                <div
                  key={s.bandSongId}
                  className="border border-gray-100 rounded-xl p-3 flex items-center justify-between gap-2"
                >
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-sm truncate">{s.title}</p>
                    <p className="text-xs text-gray-500">{s.artist}</p>
                    <div className="flex items-center gap-2 mt-0.5 flex-wrap">
                      <span className="text-xs text-gray-400">
                        👍 {s.voteCount}표
                        {s.isVotingActive && <span className="ml-1 text-green-600">투표 중</span>}
                        {!s.isVotingActive && <span className="ml-1 text-gray-400">투표 종료</span>}
                      </span>
                      {s.youtubeUrl && (
                        <a
                          href={formatYoutubeUrl(s.youtubeUrl)}
                          target="_blank"
                          rel="noreferrer"
                          className="text-xs text-red-500 hover:underline"
                        >
                          ▶ YouTube
                        </a>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    {s.isVotingActive && isLoggedIn() && (
                      <button
                        onClick={() => voteMutation.mutate(s.bandSongId)}
                        disabled={voteMutation.isPending}
                        className="text-xs bg-blue-500 text-white px-2 py-1 rounded-lg hover:bg-blue-600 disabled:opacity-50"
                      >
                        투표
                      </button>
                    )}
                    {isLeader && (
                      <button
                        onClick={() => {
                          if (confirm(`"${s.title}"을 선정 곡으로 추가하시겠습니까?`)) {
                            selectMutation.mutate(s.bandSongId);
                          }
                        }}
                        disabled={selectMutation.isPending}
                        className="text-xs bg-purple-600 text-white px-2 py-1 rounded-lg hover:bg-purple-700 disabled:opacity-50"
                      >
                        선정
                      </button>
                    )}
                  </div>
                </div>
              ))}
          </div>
        ) : (
          <p className="text-sm text-gray-400 text-center py-4">
            {isLeader ? '+ 곡 추가 버튼으로 후보 곡을 등록하세요.' : '등록된 후보 곡이 없습니다.'}
          </p>
        )}
      </div>

      {/* 리더 전용 설정 */}
      {isLeader && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 space-y-3">
          <h3 className="font-semibold text-sm text-gray-700">리더 설정</h3>

          {/* 투표 설정 */}
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium">인당 투표 수</p>
              <p className="text-xs text-gray-400">현재: {maxVotesPerPerson}표</p>
            </div>
            <button
              onClick={() => { setVotesInput(maxVotesPerPerson); setShowVoteSettings(!showVoteSettings); }}
              className="text-xs border border-gray-300 text-gray-600 px-3 py-1.5 rounded-lg hover:bg-gray-50"
            >
              변경
            </button>
          </div>
          {showVoteSettings && (
            <div className="flex items-center gap-2 bg-gray-50 rounded-xl p-3">
              <input
                type="number"
                value={votesInput}
                onChange={(e) => setVotesInput(Number(e.target.value))}
                min={1}
                max={20}
                className="w-20 border border-gray-300 rounded-lg px-2 py-1.5 text-sm text-center focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              <span className="text-sm text-gray-500">표</span>
              <button
                onClick={() => voteSettingsMutation.mutate()}
                disabled={votesInput < 1 || voteSettingsMutation.isPending}
                className="ml-auto bg-purple-600 text-white px-3 py-1.5 rounded-lg text-xs hover:bg-purple-700 disabled:opacity-50"
              >
                저장
              </button>
              <button
                onClick={() => setShowVoteSettings(false)}
                className="text-xs text-gray-400 hover:text-gray-600"
              >
                취소
              </button>
            </div>
          )}

          {/* 초기화 버튼 */}
          <div className="flex gap-2 pt-1">
            <button
              onClick={() => {
                if (confirm('모든 투표를 초기화하시겠습니까? 후보곡은 유지됩니다.')) {
                  resetVotesMutation.mutate();
                }
              }}
              disabled={resetVotesMutation.isPending}
              className="flex-1 text-xs border border-orange-300 text-orange-600 py-2 rounded-lg hover:bg-orange-50 disabled:opacity-50"
            >
              투표 초기화
            </button>
            <button
              onClick={() => {
                if (confirm('후보곡과 모든 투표를 초기화하시겠습니까? 되돌릴 수 없습니다.')) {
                  resetCandidatesMutation.mutate();
                }
              }}
              disabled={resetCandidatesMutation.isPending}
              className="flex-1 text-xs border border-red-300 text-red-600 py-2 rounded-lg hover:bg-red-50 disabled:opacity-50"
            >
              후보곡 전체 초기화
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
