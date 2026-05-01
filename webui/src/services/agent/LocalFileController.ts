/* eslint-disable */
import { request } from '@umijs/max';
import type {
  LocalFileDetailDTO,
  LocalFileCreateRequest,
  LocalFileWriteContentRequest,
} from './typings';

const AGENT_LOCAL_FILES_BASE = '/api/v1/agents';

/** 查询 Agent 本地文件列表（支持 path 和 exclude 过滤） */
export async function listLocalFiles(
  agentCode: string,
  params?: { path?: string; exclude?: string },
  options?: { [key: string]: any },
) {
  return request<{ data: LocalFileDetailDTO[] }>(`${AGENT_LOCAL_FILES_BASE}/${agentCode}/local-files`, {
    method: 'GET',
    params,
    ...(options || {}),
  });
}

/** 读取本地文件内容（响应直接是字符串） */
export async function readLocalFileContent(
  agentCode: string,
  path: string,
  options?: { [key: string]: any },
) {
  return request<{ data: string }>(
    `${AGENT_LOCAL_FILES_BASE}/${agentCode}/local-files/content`,
    {
      method: 'GET',
      params: { path },
      ...(options || {}),
    },
  );
}

/** 创建本地文件 */
export async function createLocalFile(
  agentCode: string,
  data: LocalFileCreateRequest,
  options?: { [key: string]: any },
) {
  return request(`${AGENT_LOCAL_FILES_BASE}/${agentCode}/local-files`, {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

/** 覆盖写入本地文件内容 */
export async function writeLocalFileContent(
  agentCode: string,
  data: LocalFileWriteContentRequest,
  options?: { [key: string]: any },
) {
  return request(`${AGENT_LOCAL_FILES_BASE}/${agentCode}/local-files/content`, {
    method: 'PUT',
    data,
    ...(options || {}),
  });
}

/** 删除本地文件 */
export async function deleteLocalFile(
  agentCode: string,
  path: string,
  options?: { [key: string]: any },
) {
  return request(`${AGENT_LOCAL_FILES_BASE}/${agentCode}/local-files`, {
    method: 'DELETE',
    params: { path },
    ...(options || {}),
  });
}
