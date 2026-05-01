/* eslint-disable */
import type { ChatStreamData, ContentBlock } from './typings';

const PROTOCOL_ANTHROPIC = 'ANTHROPIC_MESSAGES';
const PROTOCOL_OPENAI = 'OPENAI_CHAT_COMPLETIONS';

/**
 * 统一的产品级消息发送服务。
 * 发送用户消息到 /api/v1/agents/{agentCode}/messages，
 * 根据响应头 X-Agent-Protocol 动态分发 SSE 解析逻辑。
 */
export async function chatStream(
  agentCode: string,
  content: string,
  sessionCode: string | undefined,
  onMessage: (data: ChatStreamData) => void,
  onError: (error: any) => void,
  onClose: () => void,
  signal?: AbortSignal,
) {
  const headers: Record<string, string> = {
    'Accept': 'text/event-stream',
    'Content-Type': 'application/json',
  };
  if (sessionCode) {
    headers['X-Agent-Session-Code'] = sessionCode;
  }

  try {
    const response = await fetch(`/api/v1/agents/${agentCode}/messages`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ content }),
      signal,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`HTTP error! status: ${response.status}, body: ${errorText}`);
    }

    // 从响应头读取协议类型和 sessionCode
    const protocolType = response.headers.get('X-Agent-Protocol') || '';
    const sessionCodeFromHeader = response.headers.get('X-Agent-Session-Code') || undefined;

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    if (!reader) {
      throw new Error('ReadableStream not supported');
    }

    if (protocolType === PROTOCOL_ANTHROPIC) {
      await parseAnthropicStream(reader, decoder, sessionCodeFromHeader, protocolType, onMessage);
    } else {
      // 默认使用 OpenAI 解析（包括 OPENAI_CHAT_COMPLETIONS 和未知协议）
      await parseOpenAiStream(reader, decoder, sessionCodeFromHeader, protocolType, onMessage);
    }

    onClose();
  } catch (error) {
    onError(error);
  }
}

/**
 * Anthropic Messages SSE 解析。
 * 解析命名事件: message_start, content_block_delta (text_delta / thinking_delta), message_delta, error
 */
async function parseAnthropicStream(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  decoder: TextDecoder,
  sessionCode: string | undefined,
  protocolType: string,
  onMessage: (data: ChatStreamData) => void,
) {
  let buffer = '';
  let accumulatedThought = '';
  let accumulatedContent = '';
  let blocks: ContentBlock[] = [];
  let blockOffset = 0;
  let done = false;

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

      // error 事件需传播到外层
      if (eventType === 'error') {
        try {
          const data = JSON.parse(eventData);
          throw new Error(data.error?.message || 'Anthropic API error');
        } catch (e) {
          throw e;
        }
      }

      try {
        const data = JSON.parse(eventData);

        if (eventType === 'content_block_start') {
          const { index, content_block } = data;
          const realIndex = index + blockOffset;
          const block: ContentBlock = {
            index: realIndex,
            type: content_block.type,
            content: content_block.text || content_block.thinking || '',
            status: 'streaming',
          };
          if (content_block.type === 'tool_use') {
            block.toolName = content_block.name;
            block.toolInput = '';
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
            protocolType,
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
            protocolType,
          });
        } else if (eventType === 'message_stop') {
          // 一个 Message 结束，更新偏移量，为下一个 Message 做准备
          blockOffset = blocks.length;
        } else if (eventType === 'message_delta') {
          // 仅更新状态，不再直接标记 done = true 并退出
          // 这样可以支持一个流中包含多个 Message (Anthropic Multi-Turn)
          onMessage({
            content: accumulatedContent,
            thinking: accumulatedThought || undefined,
            blocks: [...blocks],
            done: false,
            sessionCode,
            protocolType,
          });
        }
      } catch (e) {
        console.error('Parse Anthropic SSE error', e, eventData);
      }
    }

    if (done) break;
  }

  // 最终读取结束，发送 done 信号
  onMessage({
    content: accumulatedContent,
    thinking: accumulatedThought || undefined,
    blocks: [...blocks],
    done: true,
    sessionCode,
    protocolType,
  });
}

/**
 * OpenAI Chat Completions SSE 解析。
 * 解析无事件名的 data: 行，以 data: [DONE] 结尾。
 */
async function parseOpenAiStream(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  decoder: TextDecoder,
  sessionCode: string | undefined,
  protocolType: string,
  onMessage: (data: ChatStreamData) => void,
) {
  let buffer = '';
  let accumulatedContent = '';
  let done = false;

  while (true) {
    const { done: readDone, value } = await reader.read();
    if (readDone) break;

    buffer += decoder.decode(value, { stream: true });

    const eventBlocks = buffer.split('\n\n');
    buffer = eventBlocks.pop() || '';

    for (const block of eventBlocks) {
      // 逐行解析 data: 前缀
      const blockLines = block.split('\n');
      const dataFragments: string[] = [];
      for (const bl of blockLines) {
        if (bl.startsWith('data:')) {
          dataFragments.push(bl.substring(5).trim());
        }
      }
      if (dataFragments.length === 0) continue;

      const dataStr = dataFragments.join('');

      // [DONE] 哨兵
      if (dataStr === '[DONE]') {
        done = true;
        onMessage({
          content: accumulatedContent,
          done: true,
          sessionCode,
          protocolType,
        });
        break;
      }

      try {
        const data = JSON.parse(dataStr);
        const delta = data.choices?.[0]?.delta;
        const finishReason = data.choices?.[0]?.finish_reason;

        if (delta?.content) {
          accumulatedContent += delta.content;
          onMessage({
            content: accumulatedContent,
            done: false,
            sessionCode,
            protocolType,
          });
        }

        if (finishReason) {
          done = true;
          onMessage({
            content: accumulatedContent,
            done: true,
            sessionCode,
            protocolType,
          });
          break;
        }
      } catch (e) {
        console.error('Parse OpenAI SSE error', e, dataStr);
      }
    }

    if (done) break;
  }
}
