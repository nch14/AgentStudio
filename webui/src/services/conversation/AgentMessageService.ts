/* eslint-disable */
import type { ChatStreamData, ContentBlock } from './typings';
import { getToken } from '@/utils/auth';

/**
 * 统一的产品级消息发送服务。
 * 发送用户消息到 /api/v1/agents/{agentCode}/messages，
 * 服务端统一返回 Anthropic Messages 风格 SSE 事件流。
 */
export interface AttachmentMeta {
  ossKey: string;
  mime: string;
  filename: string;
  size: number;
}

export async function chatStream(
  agentCode: string,
  content: string,
  sessionCode: string | undefined,
  onMessage: (data: ChatStreamData) => void,
  onError: (error: any) => void,
  onClose: () => void,
  signal?: AbortSignal,
  attachments?: AttachmentMeta[],
) {
  const headers: Record<string, string> = {
    'Accept': 'text/event-stream',
    'Content-Type': 'application/json',
  };
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (sessionCode) {
    headers['X-Agent-Session-Code'] = sessionCode;
  }

  try {
    const body: any = { content };
    if (attachments && attachments.length > 0) {
      body.attachments = attachments;
    }
    const response = await fetch(`/api/v1/agents/${agentCode}/messages`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      signal,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`HTTP error! status: ${response.status}, body: ${errorText}`);
    }

    // 从响应头读取 sessionCode（服务端不再返回 X-Agent-Protocol）
    const sessionCodeFromHeader = response.headers.get('X-Agent-Session-Code') || undefined;

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    if (!reader) {
      throw new Error('ReadableStream not supported');
    }

    // 统一走 Anthropic Messages 风格解析
    await parseMessagesStream(reader, decoder, sessionCodeFromHeader, onMessage);

    onClose();
  } catch (error) {
    onError(error);
  }
}

/**
 * 流式恢复接口。
 *
 * 从服务端 streamResume 接口获取 SSE 流，包含：
 * - historical_block 事件：已持久化的历史消息
 * - 实时流事件：content_block_start/delta/stop、message_start/delta/stop、turn_stop
 *
 * 适用于页面刷新/断线后恢复正在进行的对话。
 */
export async function streamResume(
  agentCode: string,
  sessionCode: string,
  fromIndex: number | undefined,
  onHistoricalBlock: (block: ContentBlock, role: 'user' | 'assistant') => void,
  onStreamMessage: (data: ChatStreamData) => void,
  onError: (error: any) => void,
  onClose: () => void,
  signal?: AbortSignal,
) {
  const headers: Record<string, string> = {
    'Accept': 'text/event-stream',
  };
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const query = fromIndex !== undefined ? `?fromIndex=${fromIndex}` : '';
  try {
    const response = await fetch(`/api/v1/agents/${agentCode}/sessions/${sessionCode}/messages/stream${query}`, {
      method: 'GET',
      headers,
      signal,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`HTTP error! status: ${response.status}, body: ${errorText}`);
    }

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    if (!reader) {
      throw new Error('ReadableStream not supported');
    }

    // 先处理 historical_block，再走实时流
    let accumulatedThought = '';
    let accumulatedContent = '';
    let blocks: ContentBlock[] = [];
    let blockOffset = 0;
    let liveOffsetAligned = false;

    await parseStreamResumeStream(reader, decoder, (eventType, data) => {
      if (eventType === 'historical_block') {
        const { block, role } = parseHistoricalBlock(data);
        onHistoricalBlock(block, role);
      } else {
        // 跳过汇总快照事件（无顶层 "type" 字段）
        if (!data.type) return;

        // streamResume 续传中段消息时，首个 live content_block_start 的 index
        // 是该 message 在已持久化部分之后的延续位置（可能 > 0）。以它对齐 blockOffset，
        // 让本地 blocks 从下标 0 开始连续写入，避免产生稀疏数组导致下游 .filter 崩溃。
        if (!liveOffsetAligned && eventType === 'content_block_start' && typeof data?.index === 'number') {
          blockOffset = -data.index;
          liveOffsetAligned = true;
        }
        applyStreamEvent(eventType, data, blocks, blockOffset,
          (off) => { blockOffset = off; liveOffsetAligned = true; },
          () => accumulatedThought,
          (v) => { accumulatedThought = v; },
          () => accumulatedContent,
          (v) => { accumulatedContent = v; },
          (d) => onStreamMessage(d),
        );
      }
    });

    onClose();
  } catch (error) {
    onError(error);
  }
}

/**
 * 解析 streamResume 返回的 SSE 流。
 */
async function parseStreamResumeStream(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  decoder: TextDecoder,
  onEvent: (eventType: string, data: any) => void,
) {
  let buffer = '';

  while (true) {
    const { done: readDone, value } = await reader.read();
    if (readDone) break;

    buffer += decoder.decode(value, { stream: true });

    const events = buffer.split('\n\n');
    buffer = events.pop() || '';

    for (const eventBlock of events) {
      const lines = eventBlock.split('\n');
      let eventType = '';
      let eventData = '';

      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.substring(6).trim();
        } else if (line.startsWith('data:')) {
          eventData = line.substring(5).trim();
        }
      }

      if (!eventType || !eventData) continue;
      if (eventType === 'ping') continue;
      if (eventType === 'error') {
        try {
          const data = JSON.parse(eventData);
          throw new Error(data.error?.message || data.message || 'Stream error');
        } catch (e) {
          throw e;
        }
      }

      try {
        const data = JSON.parse(eventData);
        onEvent(eventType, data);
      } catch (e) {
        console.error('Parse streamResume event error', e, eventData);
      }
    }
  }
}

/**
 * 将 tool_result 的 content 扁平化为纯字符串。
 * 服务端按 Anthropic 标准格式存储为 ContentBlock[]，
 * 前端渲染时期望的是字符串，需要在此做适配。
 */
export function normalizeToolResultContent(content: any): string {
  if (typeof content === 'string') return content;
  if (Array.isArray(content)) {
    return content
      .filter((item: any) => item?.type === 'text' && typeof item.text === 'string')
      .map((item: any) => item.text)
      .join('\n');
  }
  if (typeof content === 'object' && content !== null) {
    return content.text || JSON.stringify(content);
  }
  return content == null ? '' : String(content);
}

/**
 * 将 historical_block 数据转换为前端 ContentBlock，同时返回服务端 role。
 */
function parseHistoricalBlock(data: any): { block: ContentBlock; role: 'user' | 'assistant' } {
  const serverRole = (data.role as string) || 'ASSISTANT';
  const role: 'user' | 'assistant' = serverRole === 'USER' ? 'user' : 'assistant';

  const block: ContentBlock = {
    index: data.messageIndex ?? 0,
    type: (data.type || 'TEXT').toLowerCase() as ContentBlock['type'],
    content: '',
    status: 'completed',
  };

  const payload = data.payload;
  if (payload) {
    if (data.type === 'TEXT') {
      block.content = payload.text || '';
    } else if (data.type === 'THINKING') {
      block.content = payload.thinking || '';
      block.signature = payload.signature;
    } else if (data.type === 'TOOL_USE') {
      block.toolUseId = payload.toolUseId;
      block.toolName = payload.name;
      block.toolInput = typeof payload.input === 'object'
        ? JSON.stringify(payload.input, null, 2)
        : (payload.input || '');
    } else if (data.type === 'TOOL_RESULT') {
      block.toolUseId = payload.toolUseId;
      block.toolResultContent = normalizeToolResultContent(payload.content);
      block.toolResultIsError = payload.isError || false;
    } else if (data.type === 'IMAGE') {
      block.url = payload.url || '';
      block.ossKey = payload.ossKey || '';
      block.mime = payload.mime || '';
      block.filename = payload.filename || '';
      block.size = payload.size || 0;
      block.content = payload.url || '';
    } else if (data.type === 'DOCUMENT') {
      block.url = payload.url || '';
      block.ossKey = payload.ossKey || '';
      block.mime = payload.mime || '';
      block.filename = payload.filename || '';
      block.size = payload.size || 0;
      block.content = payload.url || '';
    }
  }

  return { block, role };
}

/**
 * 共享的流式事件处理逻辑，供 chatStream 和 streamResume 复用。
 */
function applyStreamEvent(
  eventType: string,
  data: any,
  blocks: ContentBlock[],
  blockOffset: number,
  setBlockOffset: (offset: number) => void,
  getAccumulatedThought: () => string,
  setAccumulatedThought: (v: string) => void,
  getAccumulatedContent: () => string,
  setAccumulatedContent: (v: string) => void,
  onMessage: (data: ChatStreamData) => void,
) {
  if (eventType === 'content_block_start') {
    const { index, content_block } = data;
    const realIndex = index + blockOffset;
    const blockType = content_block.type as ContentBlock['type'];
    const block: ContentBlock = {
      index: realIndex,
      type: blockType,
      content: '',
      status: 'streaming',
    };

    if (blockType === 'text') block.content = content_block.text || '';
    else if (blockType === 'thinking') block.content = content_block.thinking || '';
    else if (blockType === 'tool_use') {
      block.toolUseId = content_block.id;
      block.toolName = content_block.name;
      block.toolInput = '';
    } else if (blockType === 'tool_result') {
      block.toolUseId = content_block.tool_use_id;
      block.toolResultContent = normalizeToolResultContent(content_block.content);
      block.toolResultIsError = content_block.is_error || false;
    } else if (blockType === 'image') {
      block.url = content_block.url || '';
      block.ossKey = content_block.oss_key || '';
      block.mime = content_block.mime_type || '';
      block.filename = content_block.filename || '';
      block.size = content_block.size || 0;
      block.content = content_block.url || '';
    } else if (blockType === 'document') {
      block.url = content_block.url || '';
      block.ossKey = content_block.oss_key || '';
      block.mime = content_block.mime_type || '';
      block.filename = content_block.filename || '';
      block.size = content_block.size || 0;
      block.content = content_block.url || '';
    }

    blocks[realIndex] = block;

  } else if (eventType === 'content_block_delta') {
    const { index, delta } = data;
    const realIndex = index + blockOffset;
    const block = blocks[realIndex];
    if (block) {
      if (delta.type === 'text_delta') {
        block.content += delta.text || '';
        setAccumulatedContent(getAccumulatedContent() + (delta.text || ''));
      } else if (delta.type === 'thinking_delta') {
        block.content += delta.thinking || '';
        setAccumulatedThought(getAccumulatedThought() + (delta.thinking || ''));
      } else if (delta.type === 'signature_delta') {
        block.signature = (block.signature || '') + (delta.signature || '');
      } else if (delta.type === 'input_json_delta') {
        block.toolInput = (block.toolInput || '') + (delta.partial_json || '');
      }
    }
    onMessage({
      content: getAccumulatedContent(),
      thinking: getAccumulatedThought() || undefined,
      blocks: [...blocks],
      done: false,
    });

  } else if (eventType === 'content_block_stop') {
    const { index } = data;
    const realIndex = index + blockOffset;
    if (blocks[realIndex]) blocks[realIndex].status = 'completed';
    onMessage({
      content: getAccumulatedContent(),
      thinking: getAccumulatedThought() || undefined,
      blocks: [...blocks],
      done: false,
    });

  } else if (eventType === 'message_delta') {
    onMessage({
      content: getAccumulatedContent(),
      thinking: getAccumulatedThought() || undefined,
      blocks: [...blocks],
      done: false,
    });

  } else if (eventType === 'message_stop') {
    setBlockOffset(blocks.length);

  } else if (eventType === 'turn_stop') {
    onMessage({
      content: getAccumulatedContent(),
      thinking: getAccumulatedThought() || undefined,
      blocks: [...blocks],
      done: true,
    });
  }
}

/**
 * 统一 SSE 事件流解析。
 *
 * 解析命名事件:
 * - message_start: 一轮 assistant 回复开始
 * - content_block_start: 一个 block 开始（text / thinking / tool_use / tool_result）
 * - content_block_delta: block 增量（text_delta / thinking_delta / signature_delta / input_json_delta）
 * - content_block_stop: 一个 block 结束
 * - message_delta: 消息级状态更新（stop_reason 等）
 * - message_stop: 一轮回复结束
 * - error: 流式错误
 * - ping: 保活（忽略）
 * - turn_start: turn 级别开始信号（忽略）
 * - turn_stop: turn 级别完成信号
 */
async function parseMessagesStream(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  decoder: TextDecoder,
  sessionCode: string | undefined,
  onMessage: (data: ChatStreamData) => void,
) {
  let buffer = '';
  let accumulatedThought = '';
  let accumulatedContent = '';
  let blocks: ContentBlock[] = [];
  let blockOffset = 0;
  let lastStopReason = '';

  while (true) {
    const { done: readDone, value } = await reader.read();
    if (readDone) break;

    buffer += decoder.decode(value, { stream: true });

    const events = buffer.split('\n\n');
    buffer = events.pop() || '';

    for (const eventBlock of events) {
      const lines = eventBlock.split('\n');
      let eventType = '';
      let eventData = '';

      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.substring(6).trim();
        } else if (line.startsWith('data:')) {
          eventData = line.substring(5).trim();
        }
      }

      if (!eventType || !eventData) continue;

      // ping 事件直接忽略
      if (eventType === 'ping') continue;

      // error 事件需传播到外层
      if (eventType === 'error') {
        try {
          const data = JSON.parse(eventData);
          throw new Error(data.error?.message || data.message || 'Stream error');
        } catch (e) {
          throw e;
        }
      }

      try {
        const data = JSON.parse(eventData);

        // 跳过服务端发送的汇总快照事件：标准事件顶层必有 "type" 字段，汇总快照没有
        if (!data.type) continue;

        if (eventType === 'content_block_start') {
          const { index, content_block } = data;
          const realIndex = index + blockOffset;
          const blockType = content_block.type as ContentBlock['type'];
          const block: ContentBlock = {
            index: realIndex,
            type: blockType,
            content: '',
            status: 'streaming',
          };

          if (blockType === 'text') {
            block.content = content_block.text || '';
          } else if (blockType === 'thinking') {
            block.content = content_block.thinking || '';
          } else if (blockType === 'tool_use') {
            block.toolUseId = content_block.id;
            block.toolName = content_block.name;
            block.toolInput = '';
          } else if (blockType === 'tool_result') {
            block.toolUseId = content_block.tool_use_id;
            block.toolResultContent = normalizeToolResultContent(content_block.content);
            block.toolResultIsError = content_block.is_error || false;
          } else if (blockType === 'image') {
            block.url = content_block.url || '';
            block.ossKey = content_block.oss_key || '';
            block.mime = content_block.mime_type || '';
            block.filename = content_block.filename || '';
            block.size = content_block.size || 0;
            block.content = content_block.url || '';
          } else if (blockType === 'document') {
            block.url = content_block.url || '';
            block.ossKey = content_block.oss_key || '';
            block.mime = content_block.mime_type || '';
            block.filename = content_block.filename || '';
            block.size = content_block.size || 0;
            block.content = content_block.url || '';
          }

          blocks[realIndex] = block;

        } else if (eventType === 'content_block_delta') {
          const { index, delta } = data;
          const realIndex = index + blockOffset;
          const block = blocks[realIndex];
          if (block) {
            if (delta.type === 'text_delta') {
              block.content += delta.text || '';
              accumulatedContent += delta.text || '';
            } else if (delta.type === 'thinking_delta') {
              block.content += delta.thinking || '';
              accumulatedThought += delta.thinking || '';
            } else if (delta.type === 'signature_delta') {
              block.signature = (block.signature || '') + (delta.signature || '');
            } else if (delta.type === 'input_json_delta') {
              block.toolInput = (block.toolInput || '') + (delta.partial_json || '');
            }
          }
          onMessage({
            content: accumulatedContent,
            thinking: accumulatedThought || undefined,
            blocks: [...blocks],
            done: false,
            sessionCode,
          });

        } else if (eventType === 'content_block_stop') {
          const { index } = data;
          const realIndex = index + blockOffset;
          if (blocks[realIndex]) {
            blocks[realIndex].status = 'completed';
          }
          onMessage({
            content: accumulatedContent,
            thinking: accumulatedThought || undefined,
            blocks: [...blocks],
            done: false,
            sessionCode,
          });

        } else if (eventType === 'message_delta') {
          // 提取 stop_reason 用于判断是否为最终 message
          if (data.delta?.stop_reason) {
            lastStopReason = data.delta.stop_reason;
          }
          onMessage({
            content: accumulatedContent,
            thinking: accumulatedThought || undefined,
            blocks: [...blocks],
            done: false,
            sessionCode,
          });

        } else if (eventType === 'message_stop') {
          // 一个 Message 结束，仅更新偏移量，等待 turn_stop
          blockOffset = blocks.length;
          lastStopReason = '';

        } else if (eventType === 'turn_start') {
          // turn 开始信号，忽略

        } else if (eventType === 'turn_stop') {
          // turn 级别完成信号，结束流
          onMessage({
            content: accumulatedContent,
            thinking: accumulatedThought || undefined,
            blocks: [...blocks],
            done: true,
            sessionCode,
          });
          return;
        }
        // message_start 不需要特殊处理，UI 不依赖它
      } catch (e) {
        console.error('Parse SSE event error', e, eventData);
      }
    }
  }

  // 兜底：reader 关闭但未通过 message_stop 结束（异常情况）
  onMessage({
    content: accumulatedContent,
    thinking: accumulatedThought || undefined,
    blocks: [...blocks],
    done: true,
    sessionCode,
  });
}
