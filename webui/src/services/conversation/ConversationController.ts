/* eslint-disable */
import { request } from '@umijs/max';
import type {
  AgentSessionResponse,
  AgentMessageResponse,
  RenameAgentSessionRequest,
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

/** 分页查询会话消息列表 */
export async function listMessages(
  agentCode: string,
  sessionCode: string,
  params: { page?: number; size?: number },
  options?: { [key: string]: any },
) {
  return request<{ data: AgentMessageResponse[]; total: number }>(
    `${BASE_URL}/${agentCode}/sessions/${sessionCode}/messages`,
    {
      method: 'GET',
      params,
      ...(options || {}),
    },
  );
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
