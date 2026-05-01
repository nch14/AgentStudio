import { request } from '@umijs/max';
import type { AgentDetailResponse, AgentCreateRequest, AgentUpdateRequest } from './typings';

const BASE_URL = '/api/v1/agents';

/** 分页查询 Agent 列表 */
export async function listAgents(
  params: { page?: number; size?: number; status?: string },
  options?: { [key: string]: any },
) {
  return request<{ data: AgentDetailResponse[]; total: number }>(BASE_URL, {
    method: 'GET',
    params,
    ...(options || {}),
  });
}

/** 查看 Agent 详情 */
export async function getAgentDetail(agentCode: string, options?: { [key: string]: any }) {
  return request<{ data: AgentDetailResponse }>(`${BASE_URL}/${agentCode}`, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 新建 Agent */
export async function createAgent(data: AgentCreateRequest, options?: { [key: string]: any }) {
  return request<{ data: AgentDetailResponse }>(BASE_URL, {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

/** 修改 Agent */
export async function updateAgent(
  agentCode: string,
  data: AgentUpdateRequest,
  options?: { [key: string]: any },
) {
  return request<{ data: AgentDetailResponse }>(`${BASE_URL}/${agentCode}`, {
    method: 'PUT',
    data,
    ...(options || {}),
  });
}

/** 启用 Agent */
export async function enableAgent(agentCode: string, options?: { [key: string]: any }) {
  return request<{ data: AgentDetailResponse }>(`${BASE_URL}/${agentCode}/enable`, {
    method: 'POST',
    ...(options || {}),
  });
}

/** 禁用 Agent */
export async function disableAgent(agentCode: string, options?: { [key: string]: any }) {
  return request<{ data: AgentDetailResponse }>(`${BASE_URL}/${agentCode}/disable`, {
    method: 'POST',
    ...(options || {}),
  });
}

/** 删除 Agent */
export async function deleteAgent(agentCode: string, options?: { [key: string]: any }) {
  return request<void>(`${BASE_URL}/${agentCode}`, {
    method: 'DELETE',
    ...(options || {}),
  });
}

