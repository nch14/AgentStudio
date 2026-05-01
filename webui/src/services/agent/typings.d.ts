/** Agent 模块类型定义 */

export interface AgentDetailResponse {
  code: string;
  name: string;
  responsibility: string;
  provider: string;
  status: string;
  providerConfig: Record<string, string>;
  createTime: string;
  updateTime: string;
}

export interface AgentCreateRequest {
  name: string;
  responsibility?: string;
  provider: string;
  providerConfig?: Record<string, string>;
}

export interface AgentUpdateRequest {
  name?: string;
  responsibility?: string;
  providerConfig?: Record<string, string>;
}

/** LocalFile 模块类型定义（本地文件系统） */

export interface LocalFileDetailDTO {
  path: string;
  name: string;
  fileSize?: number;
  lastModifiedTime?: string;
}

export interface LocalFileCreateRequest {
  path: string;
  name: string;
  content: string;
}

export interface LocalFileWriteContentRequest {
  path: string;
  content: string;
}

/** CloudFile 模块类型定义（云端备份） */

export interface CloudFileDetailDTO {
  code: string;
  agentCode: string;
  path: string;
  name: string;
  ossKey?: string;
  fileSize?: number;
  lastSyncTime?: string;
}
