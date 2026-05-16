/** Conversation 模块类型定义 */

export interface AgentSessionResponse {
  code: string;
  title: string;
  agentCode: string;
  scene?: string;
  messageCount: number;
  lastMessageTime: string;
  archived: boolean;
  createTime: string;
  updateTime: string;
}

export interface RenameAgentSessionRequest {
  title: string;
}

// ===== 历史消息 — turn 聚合模型 =====

/** 一个 turn 的响应 DTO（对应服务端 AgentSessionTurnDTO） */
export interface AgentSessionTurnDTO {
  turnCode: string;
  blocks: AgentSessionBlockDTO[];
}

/** turn 内的单个 block（对应服务端 AgentSessionBlockDTO） */
export interface AgentSessionBlockDTO {
  code: string;
  role: 'USER' | 'ASSISTANT' | 'TOOL' | 'SYSTEM';
  type: 'TEXT' | 'THINKING' | 'TOOL_USE' | 'TOOL_RESULT' | 'IMAGE' | 'DOCUMENT' | 'TURN_START' | 'TURN_STOP';
  messageIndex: number;
  payload: TextPayload | ThinkingPayload | ToolUsePayload | ToolResultPayload | ImagePayload | DocumentPayload | null;
  errorPayload: any;
  externalMessageId: string | null;
}

// ===== Block payload 类型 =====

export interface TextPayload {
  text: string;
}

export interface ThinkingPayload {
  thinking: string;
  signature?: string;
}

export interface ToolUsePayload {
  toolUseId: string;
  name: string;
  input: any;
}

export interface ToolResultPayload {
  toolUseId: string;
  content: string;
  isError: boolean;
}

export interface ImagePayload {
  ossKey: string;
  mime: string;
  filename: string;
  size: number;
  url: string;
}

export interface DocumentPayload {
  ossKey: string;
  mime: string;
  filename: string;
  size: number;
  url: string;
}

// ===== Cursor Page Response =====

/** 游标分页响应（对应服务端 CursorPageResponse） */
export interface CursorPageResponse<T> {
  data: T[];
  nextCursor: number | null;
  hasMore: boolean;
}

// ===== 流式 SSE — 统一 block 模型 =====

/** 前端运行时维护的 content block（流式/历史通用） */
export interface ContentBlock {
  index: number;
  type: 'text' | 'thinking' | 'tool_use' | 'tool_result' | 'image' | 'document' | 'turn_start' | 'turn_stop';
  content: string;
  toolUseId?: string;
  toolName?: string;
  toolInput?: string;
  toolResultContent?: string;
  toolResultIsError?: boolean;
  signature?: string;
  status: 'streaming' | 'completed';
  // image/document fields
  ossKey?: string;
  mime?: string;
  filename?: string;
  size?: number;
  url?: string;
}

/** 统一的 SSE 流式回调数据 */
export interface ChatStreamData {
  content: string;
  thinking?: string;
  blocks?: ContentBlock[];
  done: boolean;
  sessionCode?: string;
}
