/** Conversation 模块类型定义 */

export interface AgentSessionResponse {
  code: string;
  title: string;
  agentCode: string;
  messageCount: number;
  lastMessageTime: string;
  archived: boolean;
  createTime: string;
  updateTime: string;
}

export interface AgentMessageResponse {
  code: string;
  role: string;
  protocolType: string;
  status: string;
  payloadJson: string;
  errorPayloadJson: string;
  externalMessageId: string;
  messageIndex: number;
}

export interface RenameAgentSessionRequest {
  title: string;
}

/** 单个 content block 的结构 */
export interface ContentBlock {
  index: number;
  type: 'thinking' | 'text' | 'tool_use' | string;
  content: string;        // thinking text / response text / tool name
  toolName?: string;      // tool_use 时的工具名
  toolInput?: string;     // tool_use 时的输入 JSON
  status: 'streaming' | 'completed';
}

/** 统一的 SSE 流式回调数据 */
export interface ChatStreamData {
  content: string;
  thinking?: string;
  done: boolean;
  sessionCode?: string;
  protocolType?: string;
  blocks?: ContentBlock[];
}
