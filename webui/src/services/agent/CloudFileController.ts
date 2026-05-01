/* eslint-disable */
import { request } from '@umijs/max';
import type { CloudFileDetailDTO } from './typings';

const AGENT_CLOUD_FILES_BASE = '/api/v1/agents';

/** 列举所有已备份的云端文件 */
export async function listCloudFiles(
  agentCode: string,
  options?: { [key: string]: any },
) {
  return request<{ data: CloudFileDetailDTO[] }>(`${AGENT_CLOUD_FILES_BASE}/${agentCode}/cloud-files`, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 手动触发备份（本地 → 云端） */
export async function backupAgent(
  agentCode: string,
  options?: { [key: string]: any },
) {
  return request<{ data: { backedUpCount: number; skippedCount: number; failedCount: number } }>(
    `${AGENT_CLOUD_FILES_BASE}/${agentCode}/cloud-files/backup`,
    {
      method: 'POST',
      ...(options || {}),
    },
  );
}

/** 从备份恢复（云端 → 本地覆盖） */
export async function restoreAgent(
  agentCode: string,
  options?: { [key: string]: any },
) {
  return request<{ data: { restoredCount: number } }>(
    `${AGENT_CLOUD_FILES_BASE}/${agentCode}/cloud-files/restore`,
    {
      method: 'POST',
      ...(options || {}),
    },
  );
}
