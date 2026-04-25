import client from './client';

export interface BandResponse {
  bandId: number;
  name: string;
  description: string;
  leaderId: number;
  memberCount: number;
  createdAt: string;
  maxVotesPerPerson: number;
}

export interface RecruitResponse {
  recruitId: number;
  bandId: number;
  position: string;
  requiredCount: number;
  currentCount: number;
  createdAt: string;
}

export interface ApplicationResponse {
  applicationId: number;
  bandId: number;
  userId: number;
  nickname: string;
  position: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  introduction?: string;
  createdAt: string;
}

export interface BandMemberResponse {
  memberId: number;
  userId: number;
  nickname: string;
  position: string;
  joinedAt: string;
}

export const getAllBands = () =>
  client.get<BandResponse[]>('/bands');

export const createBand = (name: string, description?: string) =>
  client.post<BandResponse>('/bands', { name, description });

export const getBand = (bandId: number) =>
  client.get<BandResponse>(`/bands/${bandId}`);

export const getMyBands = () =>
  client.get<BandResponse[]>('/bands/my-bands');

export const getBandMembers = (bandId: number) =>
  client.get<BandMemberResponse[]>(`/bands/${bandId}/members`);

export const deleteBand = (bandId: number) =>
  client.delete(`/bands/${bandId}`);

export const updateVoteSettings = (bandId: number, maxVotesPerPerson: number) =>
  client.put<BandResponse>(`/bands/${bandId}/vote-settings`, { maxVotesPerPerson });

export const getBandRecruits = (bandId: number) =>
  client.get<RecruitResponse[]>(`/bands/${bandId}/recruits`);

export const createRecruit = (bandId: number, position: string, requiredCount: number) =>
  client.post<RecruitResponse>(`/bands/${bandId}/recruits`, { bandId, position, requiredCount });

export const applyBand = (bandId: number, recruitId: number, position: string, introduction?: string) =>
  client.post<ApplicationResponse>(`/bands/${bandId}/apply`, { recruitId, position, introduction });

export const getBandApplications = (bandId: number) =>
  client.get<ApplicationResponse[]>(`/bands/${bandId}/applications`);

export const approveApplication = (bandId: number, applicationId: number) =>
  client.put<ApplicationResponse>(`/bands/${bandId}/applications/${applicationId}/approve`);

export const rejectApplication = (bandId: number, applicationId: number) =>
  client.put<ApplicationResponse>(`/bands/${bandId}/applications/${applicationId}/reject`);
