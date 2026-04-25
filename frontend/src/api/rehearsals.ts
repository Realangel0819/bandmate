import client from './client';

export interface RehearsalResponse {
  rehearsalId: number;
  bandId: number;
  title: string;
  rehearsalDate: string;
  location: string;
  maxCapacity: number;
  currentAttendees: number;
  createdAt: string;
}

export interface AttendanceResponse {
  attendanceId: number;
  rehearsalId: number;
  userId: number;
  nickname: string;
  createdAt: string;
}

export const getRehearsals = (bandId: number) =>
  client.get<RehearsalResponse[]>(`/bands/${bandId}/rehearsals`);

export const getRehearsal = (bandId: number, rehearsalId: number) =>
  client.get<RehearsalResponse>(`/bands/${bandId}/rehearsals/${rehearsalId}`);

export const createRehearsal = (
  bandId: number,
  title: string,
  rehearsalDate: string,
  location?: string,
  maxCapacity?: number
) =>
  client.post<RehearsalResponse>(`/bands/${bandId}/rehearsals`, {
    title,
    rehearsalDate,
    location,
    maxCapacity,
  });

export const joinRehearsal = (bandId: number, rehearsalId: number) =>
  client.post<AttendanceResponse>(
    `/bands/${bandId}/rehearsals/${rehearsalId}/join`
  );

export const cancelAttendance = (bandId: number, rehearsalId: number) =>
  client.delete(`/bands/${bandId}/rehearsals/${rehearsalId}/join`);

export const getAttendances = (bandId: number, rehearsalId: number) =>
  client.get<AttendanceResponse[]>(
    `/bands/${bandId}/rehearsals/${rehearsalId}/attendances`
  );