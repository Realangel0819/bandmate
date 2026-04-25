import client from './client';

export interface BandSongResponse {
  bandSongId: number;
  bandId: number;
  songId: number;
  title: string;
  artist: string;
  youtubeUrl?: string;
  voteStartDate: string;
  voteEndDate: string;
  voteCount: number;
  isSelected: boolean;
  isVotingActive: boolean;
  createdAt: string;
}

const fmtDate = (d: Date) => d.toISOString().slice(0, 19);

export const getBandSongs = (bandId: number) =>
  client.get<BandSongResponse[]>(`/bands/${bandId}/songs`);

export const createAndAddSong = async (
  bandId: number,
  title: string,
  artist: string,
  youtubeUrl?: string
) => {
  const now = new Date();
  const end = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
  const songRes = await client.post<{ id: number }>(`/bands/${bandId}/songs`, {
    title,
    artist,
    youtubeUrl: youtubeUrl || undefined,
  });
  const songId = songRes.data.id;
  return client.post<BandSongResponse>(`/bands/${bandId}/songs/candidates`, {
    songId,
    voteStartDate: fmtDate(now),
    voteEndDate: fmtDate(end),
  });
};

export const voteSong = (bandId: number, bandSongId: number) =>
  client.post(`/bands/${bandId}/songs/vote`, { bandSongId });

export const selectSong = (bandId: number, bandSongId: number) =>
  client.put<BandSongResponse>(`/bands/${bandId}/songs/${bandSongId}/select`);

export const deselectSong = (bandId: number, bandSongId: number) =>
  client.delete(`/bands/${bandId}/songs/${bandSongId}/select`);

export const resetCandidates = (bandId: number) =>
  client.delete(`/bands/${bandId}/songs/candidates`);

export const resetVotes = (bandId: number) =>
  client.delete(`/bands/${bandId}/songs/votes`);
