/* eslint-disable */
import { request } from '@umijs/max';
import { getToken } from '@/utils/auth';
import type {
  AgentSessionResponse,
  AgentSessionTurnDTO,
  RenameAgentSessionRequest,
  CursorPageResponse,
} from './typings';

const BASE_URL = '/api/v1/agents';

/** 分页查询会话列表 */
export async function listSessions(
  agentCode: string,
  params: { page?: number; size?: number; archived?: boolean },
  options?: { [key: string]: any },
) {
  return request<{ data: AgentSessionResponse[]; total: number }>(
    `${BASE_URL}/${agentCode}/sessions`,
    {
      method: 'GET',
      params,
      ...(options || {}),
    },
  );
}

/** 查看会话详情 */
export async function getSessionDetail(
  agentCode: string,
  sessionCode: string,
  options?: { [key: string]: any },
) {
  return request<{ data: AgentSessionResponse }>(
    `${BASE_URL}/${agentCode}/sessions/${sessionCode}`,
    {
      method: 'GET',
      ...(options || {}),
    },
  );
}

/** 分页查询会话消息列表（按 turn 聚合返回） */
export async function listMessages(
  agentCode: string,
  sessionCode: string,
  params: { page?: number; size?: number },
  options?: { [key: string]: any },
) {
  return request<{ data: AgentSessionTurnDTO[]; total: number }>(
    `${BASE_URL}/${agentCode}/sessions/${sessionCode}/messages`,
    {
      method: 'GET',
      params,
      ...(options || {}),
    },
  );
}

/** 游标分页查询会话消息列表（按 turn 聚合返回） */
export async function listMessagesByCursor(
  agentCode: string,
  sessionCode: string,
  params: { cursor?: number; size?: number },
  options?: { [key: string]: any },
) {
  return request<CursorPageResponse<AgentSessionTurnDTO>>(
    `${BASE_URL}/${agentCode}/sessions/${sessionCode}/messages/cursor`,
    {
      method: 'GET',
      params,
      ...(options || {}),
    },
  );
}

/** SSE 流式恢复接口 */
export async function streamResume(
  agentCode: string,
  sessionCode: string,
  params?: { fromIndex?: number },
  options?: { [key: string]: any },
) {
  const token = getToken();
  const headers: Record<string, string> = {
    'Accept': 'text/event-stream',
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const query = params?.fromIndex !== undefined ? `?fromIndex=${params.fromIndex}` : '';
  return fetch(`${BASE_URL}/${agentCode}/sessions/${sessionCode}/messages/stream${query}`, {
    method: 'GET',
    headers,
    signal: options?.signal,
  });
}

/** 重命名会话 */
export async function renameSession(
  agentCode: string,
  sessionCode: string,
  data: RenameAgentSessionRequest,
  options?: { [key: string]: any },
) {
  return request<{ data: AgentSessionResponse }>(
    `${BASE_URL}/${agentCode}/sessions/${sessionCode}/rename`,
    {
      method: 'POST',
      data,
      ...(options || {}),
    },
  );
}

/** 归档会话 */
export async function archiveSession(
  agentCode: string,
  sessionCode: string,
  options?: { [key: string]: any },
) {
  return request<void>(`${BASE_URL}/${agentCode}/sessions/${sessionCode}/archive`, {
    method: 'POST',
    ...(options || {}),
  });
}

/** 取消归档会话 */
export async function unarchiveSession(
  agentCode: string,
  sessionCode: string,
  options?: { [key: string]: any },
) {
  return request<void>(`${BASE_URL}/${agentCode}/sessions/${sessionCode}/unarchive`, {
    method: 'POST',
    ...(options || {}),
  });
}

/** 中断会话的活跃流式对话 */
export async function interruptStream(
  agentCode: string,
  sessionCode: string,
  options?: { [key: string]: any },
) {
  return request<{ data: { interrupted: boolean } }>(
    `${BASE_URL}/${agentCode}/sessions/${sessionCode}/stream`,
    {
      method: 'DELETE',
      ...(options || {}),
    },
  );
}

/** 删除会话 */
export async function deleteSession(
  agentCode: string,
  sessionCode: string,
  options?: { [key: string]: any },
) {
  return request<void>(`${BASE_URL}/${agentCode}/sessions/${sessionCode}/delete`, {
    method: 'POST',
    ...(options || {}),
  });
}
