import { useState, useEffect, useRef, useCallback } from 'react';
import { useSearchParams } from 'umi';

import { message, Avatar, Typography, Button, Spin, ConfigProvider, Tooltip, Modal, Input } from 'antd';
import {
  RobotOutlined, UserOutlined, PlusOutlined,
  MessageOutlined, DeleteOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
  CheckCircleOutlined, ToolOutlined, CoffeeOutlined,
  FolderOutlined, InboxOutlined, EditOutlined, PaperClipOutlined
} from '@ant-design/icons';
import { Bubble, Sender, Welcome, Conversations, Think, ThoughtChain } from '@ant-design/x';
import XMarkdown from '@ant-design/x-markdown';
import './style.less';
import AgentSelect from './components/AgentSelect';
import { listChatAgents } from '@/services/agent/AgentController';
import type { AgentDetailResponse } from '@/services/agent/typings';
import { listSessions, listMessagesByCursor, streamResume, deleteSession, archiveSession, unarchiveSession, renameSession, interruptStream } from '@/services/conversation/ConversationController';
import { chatStream, streamResume as chatStreamResume, normalizeToolResultContent } from '@/services/conversation/AgentMessageService';
import type { AgentSessionResponse, AgentSessionBlockDTO, ChatStreamData, ContentBlock } from '@/services/conversation/typings';
import AgentFileWorkspace from '@/components/AgentFileWorkspace';
import { getUploadUrl, uploadToOss } from '@/services/oss/OssFileService';

const { Text, Title } = Typography;

export default function ChatPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const [agents, setAgents] = useState<AgentDetailResponse[]>([]);
  const [selectedAgent, setSelectedAgent] = useState<string>(() => searchParams.get('agent') || '');


  // History states
  const [sessions, setSessions] = useState<AgentSessionResponse[]>([]);
  const [archivedSessions, setArchivedSessions] = useState<AgentSessionResponse[]>([]);
  const [selectedSessionCode, setSelectedSessionCode] = useState<string>(() => searchParams.get('session') || '');

  const [loadingHistory, setLoadingHistory] = useState(false);
  const [sidebarVisible, setSidebarVisible] = useState(true);
  const [renamingSession, setRenamingSession] = useState<AgentSessionResponse | null>(null);
  const [renameTitle, setRenameTitle] = useState('');
  const [fileSidebarVisible, setFileSidebarVisible] = useState(false);
  
  // Messaging states — renderItems: each element is a group of consecutive same-role blocks
  const [renderItems, setRenderItems] = useState<{ id: string; role: 'user' | 'assistant'; blocks: ContentBlock[] }[]>([]);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [streaming, setStreaming] = useState(false);
  const [currentContent, setCurrentContent] = useState('');
  const [currentBlocks, setCurrentBlocks] = useState<ContentBlock[]>([]);

  // Cursor pagination states
  const messageCursorRef = useRef<{ nextCursor: number | null; hasMore: boolean; loadingMore: boolean }>({
    nextCursor: null,
    hasMore: false,
    loadingMore: false,
  });
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const currentStreamSessionCodeRef = useRef<string>('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Attachment states
  const ALLOWED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];
  const ALLOWED_DOC_TYPES = ['application/pdf', 'text/plain', 'text/markdown'];
  const MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
  const MAX_DOC_SIZE = 32 * 1024 * 1024; // 32MB
  const MAX_ATTACHMENTS = 10;
  const MAX_IMAGES = 8;
  const MAX_DOCUMENTS = 2;

  interface PendingAttachment {
    uid: string;
    file: File;
    previewUrl?: string;
    ossKey?: string;
    publicUrl?: string;
    uploading: boolean;
    error?: string;
  }
  const [attachments, setAttachments] = useState<PendingAttachment[]>([]);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const senderRef = useRef<any>(null);
  const skipNextFetchRef = useRef(false);

  const fetchAgents = async () => {
    try {
      const res = await listChatAgents();
      setAgents(res.data || []);
      if (res.data?.length > 0) {
        // If current selectedAgent is not in the list, fall back
        if (!selectedAgent || !res.data.some(a => a.code === selectedAgent)) {
          setSelectedAgent(res.data[0].code);
        }
      }
    } catch (e) {
      // ignored
    }
  };



  useEffect(() => {
    fetchAgents();
  }, []);

  const fetchConversations = async (agentCode: string, keepSelected = false) => {
    try {
      setLoadingHistory(true);
      const res = await listSessions(agentCode, { page: 0, size: 50, archived: false });
      setSessions(res.data || []);

      if (!keepSelected) {
        // Validate if selectedSessionCode (could be from URL) is in the current session list
        if (selectedSessionCode && !res.data?.some(s => s.code === selectedSessionCode)) {
          setSelectedSessionCode('');
          setRenderItems([]);
        }
      }



    } catch (e) {
      // ignored
    } finally {
      setLoadingHistory(false);
    }
  };

  const fetchArchivedConversations = async (agentCode: string) => {
    try {
      const res = await listSessions(agentCode, { page: 0, size: 50, archived: true });
      setArchivedSessions(res.data || []);
    } catch (e) {
      // ignored
    }
  };

  useEffect(() => {
    if (selectedAgent) {
      fetchConversations(selectedAgent);
      fetchArchivedConversations(selectedAgent);
    } else {
      setSessions([]);
      setArchivedSessions([]);
    }
  }, [selectedAgent]);

  const fetchSessionMessages = async (agentCode: string, sessionCode: string, cursor?: number, append = false) => {
    try {
      if (!append) {
        setLoadingMessages(true);
      } else {
        messageCursorRef.current.loadingMore = true;
      }
      const res = await listMessagesByCursor(agentCode, sessionCode, { cursor, size: 20 });

      // Update cursor state
      messageCursorRef.current.nextCursor = res.nextCursor;
      messageCursorRef.current.hasMore = res.hasMore;

      // Flatten turns into role-grouped render items.
      const items: { id: string; role: 'user' | 'assistant'; blocks: ContentBlock[] }[] = [];

      for (const turn of res.data || []) {
        let blockIdx = 0;
        let currentGroup: { role: 'user' | 'assistant'; blocks: ContentBlock[] } | null = null;

        for (const b of turn.blocks) {
          const role = b.role === 'USER' ? 'user' as const : 'assistant' as const;
          const block: ContentBlock = {
            index: b.messageIndex ?? blockIdx,
            type: (b.type || 'TEXT').toLowerCase() as ContentBlock['type'],
            content: '',
            status: 'completed',
          };
          const payload = b.payload as any;
          if (payload) {
            if (b.role === 'USER' && b.type === 'TEXT') {
              block.content = payload.text || '';
            } else if (b.type === 'TEXT') {
              block.content = payload.text || '';
            } else if (b.type === 'THINKING') {
              block.content = payload.thinking || '';
              block.signature = payload.signature;
            } else if (b.type === 'TOOL_USE') {
              block.toolUseId = payload.toolUseId;
              block.toolName = payload.name;
              block.toolInput = typeof payload.input === 'object'
                ? JSON.stringify(payload.input, null, 2)
                : (payload.input || '');
            } else if (b.type === 'TOOL_RESULT') {
              block.toolUseId = payload.toolUseId;
              block.toolResultContent = normalizeToolResultContent(payload.content);
              block.toolResultIsError = payload.isError || false;
            } else if (b.type === 'IMAGE') {
              block.url = payload.url || '';
              block.ossKey = payload.ossKey || '';
              block.mime = payload.mime || '';
              block.filename = payload.filename || '';
              block.size = payload.size || 0;
              block.content = payload.url || '';
            } else if (b.type === 'DOCUMENT') {
              block.url = payload.url || '';
              block.ossKey = payload.ossKey || '';
              block.mime = payload.mime || '';
              block.filename = payload.filename || '';
              block.size = payload.size || 0;
              block.content = payload.url || '';
            } else if (b.type === 'TURN_START' || b.type === 'TURN_STOP') {
              // turn sentinels, not rendered
            }
          }

          // Start new group when role changes
          if (!currentGroup || currentGroup.role !== role) {
            currentGroup = { role, blocks: [] };
            items.push({ id: `${turn.turnCode}-${role}-${blockIdx}`, ...currentGroup });
          }
          currentGroup.blocks.push(block);
          blockIdx++;
        }
      }

      if (append) {
        // Prepend older messages to the beginning
        setRenderItems(prev => [...items, ...prev]);
        messageCursorRef.current.loadingMore = false;
      } else {
        setRenderItems(items);
      }
    } catch (e) {
      if (!append) {
        message.error('无法加载历史消息');
      }
    } finally {
      setLoadingMessages(false);
      messageCursorRef.current.loadingMore = false;
    }
  };

  useEffect(() => {
    if (selectedAgent && selectedSessionCode) {
      if (skipNextFetchRef.current) {
        skipNextFetchRef.current = false;
        return;
      }
      // Abort any previous streamResume before starting new one
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
      fetchSessionMessages(selectedAgent, selectedSessionCode);
      // 消息加载后尝试恢复活跃流
      setTimeout(() => tryStreamResume(selectedAgent, selectedSessionCode), 500);
    } else {
      setRenderItems([]);
    }
  }, [selectedSessionCode, selectedAgent]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'auto' });
  };

  // Scroll-to-load-more: when scrolled to top and hasMore, fetch older messages
  const handleScroll = useCallback(() => {
    const el = scrollContainerRef.current;
    if (!el || messageCursorRef.current.loadingMore || !messageCursorRef.current.hasMore) return;
    if (el.scrollTop <= 20) {
      fetchSessionMessages(selectedAgent!, selectedSessionCode!, messageCursorRef.current.nextCursor ?? undefined, true);
    }
  }, [selectedAgent, selectedSessionCode]);

  // Try to resume active stream after loading messages
  const tryStreamResume = useCallback((agentCode: string, sessionCode: string) => {
    const controller = new AbortController();
    abortControllerRef.current = controller;

    // Calculate fromIndex from current renderItems: get last messageIndex + 1
    const allBlocks = renderItems.flatMap(item => item.blocks);
    const lastIndex = allBlocks.length > 0 ? Math.max(...allBlocks.map(b => b.index)) : -1;
    const fromIndex = lastIndex >= 0 ? lastIndex + 1 : undefined;

    let accumulatedThought = '';
    let accumulatedContent = '';
    let resolvedBlocks: ContentBlock[] = [];
    let hasActiveStream = false;

    const onHistoricalBlock = (block: ContentBlock, role: 'user' | 'assistant') => {
      setRenderItems(prev => {
        const exists = prev.some(item => item.blocks.some(b => b.index === block.index));
        if (exists) return prev;
        return [...prev, { id: `resumed-block-${block.index}`, role, blocks: [block] }];
      });
    };

    const onStreamMessage = (data: ChatStreamData) => {
      hasActiveStream = true;
      setStreaming(true);

      if (data.blocks) {
        resolvedBlocks = data.blocks;
        setCurrentBlocks(data.blocks);
      }
      if (data.thinking !== undefined) accumulatedThought = data.thinking;
      accumulatedContent = data.content;

      const display = accumulatedThought
        ? `<think>${accumulatedThought}</think>${accumulatedContent}`
        : accumulatedContent;
      setCurrentContent(display);
      scrollToBottom();

      if (data.done) {
        const assistantItem = {
          id: `resumed-ai-item-${Date.now()}`,
          role: 'assistant' as const,
          blocks: resolvedBlocks.map(b => ({ ...b, status: 'completed' as const })),
        };
        setRenderItems(prev => [...prev, assistantItem]);
        setCurrentContent('');
        setCurrentBlocks([]);
        setStreaming(false);
        messageCursorRef.current = { nextCursor: null, hasMore: false, loadingMore: false };
      }
    };

    const onStreamError = (err: any) => {
      const errorMsg = err instanceof Error ? err.message : String(err);
      if (!errorMsg.includes('aborted') && errorMsg !== 'AbortError') {
        console.log('streamResume error (may be no active stream):', errorMsg);
      }
      if (hasActiveStream && (accumulatedContent || accumulatedThought)) {
        const assistantItem = {
          id: `resumed-ai-item-${Date.now()}`,
          role: 'assistant' as const,
          blocks: resolvedBlocks.map(b => ({ ...b, status: 'completed' as const })),
        };
        setRenderItems(prev => [...prev, assistantItem]);
      }
      setCurrentContent('');
      setCurrentBlocks([]);
      setStreaming(false);
    };

    const onStreamClose = () => {
      setStreaming(false);
    };

    chatStreamResume(agentCode, sessionCode, fromIndex, onHistoricalBlock, onStreamMessage, onStreamError, onStreamClose, controller.signal);
  }, [renderItems]);

  useEffect(() => {
    scrollToBottom();
  }, [renderItems, currentContent]);

  // Sync state to URL
  useEffect(() => {
    // Avoid running if searchParams are exactly what they should be
    if (searchParams.get('agent') === (selectedAgent || null) && 
        searchParams.get('session') === (selectedSessionCode || null)) {
      return;
    }

    const params = new URLSearchParams(searchParams);
    
    if (selectedAgent) {
      params.set('agent', selectedAgent);
    } else {
      params.delete('agent');
    }

    if (selectedSessionCode) {
      params.set('session', selectedSessionCode);
    } else {
      params.delete('session');
    }

    setSearchParams(params, { replace: true });
  }, [selectedAgent, selectedSessionCode]);

  // --- File attachment handling ---

  const uploadSingleFile = async (uid: string, file: File, agentCode: string) => {
    try {
      setAttachments(prev => prev.map(a => a.uid === uid ? { ...a, uploading: true, error: undefined } : a));
      
      const res = await getUploadUrl(file.name, file.type, agentCode);
      await uploadToOss(res.uploadUrl, file, res.contentType);
      
      setAttachments(prev => prev.map(a => a.uid === uid ? { 
        ...a, 
        uploading: false, 
        ossKey: res.objectKey, 
        publicUrl: res.publicUrl // assuming backend returns this or we can construct it
      } : a));
    } catch (err: any) {
      const errorMsg = err instanceof Error ? err.message : '上传失败';
      message.error(`${file.name}: ${errorMsg}`);
      setAttachments(prev => prev.map(a => a.uid === uid ? { ...a, uploading: false, error: errorMsg } : a));
    }
  };

  const validateAndAddFiles = (files: FileList | File[]) => {
    if (!selectedAgent) {
      message.warning('请先选择一个 Agent');
      return;
    }
    const fileArray = Array.from(files);
    if (attachments.length + fileArray.length > MAX_ATTACHMENTS) {
      message.warning(`最多只能添加 ${MAX_ATTACHMENTS} 个附件`);
      return;
    }

    const newAttachments: PendingAttachment[] = [];
    let imageCount = attachments.filter(a => ALLOWED_IMAGE_TYPES.includes(a.file.type)).length;
    let docCount = attachments.filter(a => ALLOWED_DOC_TYPES.includes(a.file.type)).length;

    for (const file of fileArray) {
      const isImage = ALLOWED_IMAGE_TYPES.includes(file.type);
      const isDoc = ALLOWED_DOC_TYPES.includes(file.type);

      if (!isImage && !isDoc) {
        message.warning(`${file.name}: 不支持的文件类型`);
        continue;
      }
      if (isImage && file.size > MAX_IMAGE_SIZE) {
        message.warning(`${file.name}: 图片大小不能超过 10MB`);
        continue;
      }
      if (isDoc && file.size > MAX_DOC_SIZE) {
        message.warning(`${file.name}: 文件大小不能超过 32MB`);
        continue;
      }
      if (isImage && imageCount >= MAX_IMAGES) {
        message.warning('图片数量已达上限（8张）');
        continue;
      }
      if (isDoc && docCount >= MAX_DOCUMENTS) {
        message.warning('文档数量已达上限（2个）');
        continue;
      }

      if (isImage) imageCount++;
      if (isDoc) docCount++;
      
      const uid = `rc-upload-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
      const previewUrl = isImage ? URL.createObjectURL(file) : undefined;
      // Initialize uploading:true so spinner shows immediately
      const att = { uid, file, previewUrl, uploading: true };
      newAttachments.push(att);
    }

    if (newAttachments.length > 0) {
      setAttachments(prev => [...prev, ...newAttachments]);
      // Trigger uploads after state is committed
      for (const att of newAttachments) {
        uploadSingleFile(att.uid, att.file, selectedAgent);
      }
    }
  };

  const removeAttachment = (uid: string) => {
    setAttachments(prev => prev.filter((a) => a.uid !== uid));
  };

  const handleFileSelect = () => {
    fileInputRef.current?.click();
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      validateAndAddFiles(e.target.files);
      e.target.value = '';
    }
  };

  // uploadAttachments is now deprecated in favor of instant upload

  // --- End attachment handling ---

  const handleCreateNewChat = () => {
    setSelectedSessionCode('');
    setRenderItems([]);
    messageCursorRef.current = { nextCursor: null, hasMore: false, loadingMore: false };
  };

  const handleDeleteSession = async (session: AgentSessionResponse) => {
    if (!selectedAgent) return;
    try {
      await deleteSession(selectedAgent, session.code);
      message.success('已删除对话');
      if (selectedSessionCode === session.code) {
        setSelectedSessionCode('');
        setRenderItems([]);
      }
      fetchConversations(selectedAgent, true);
    } catch (e) {
      // error handled by interceptor
    }
  };

  const handleArchiveSession = async (session: AgentSessionResponse) => {
    if (!selectedAgent) return;
    try {
      await archiveSession(selectedAgent, session.code);
      message.success('已归档对话');
      if (selectedSessionCode === session.code) {
        setSelectedSessionCode('');
        setRenderItems([]);
      }
      setSessions(prev => prev.filter(s => s.code !== session.code));
      setArchivedSessions(prev => [...prev, { ...session, archived: true }]);
    } catch (e) {
      // error handled by interceptor
    }
  };

  const handleRenameSession = async () => {
    if (!selectedAgent || !renamingSession) return;
    const trimmed = renameTitle.trim();
    if (!trimmed) return;
    try {
      await renameSession(selectedAgent, renamingSession.code, { title: trimmed });
      message.success('已重命名');
      setSessions(prev => prev.map(s => s.code === renamingSession.code ? { ...s, title: trimmed } : s));
      setRenamingSession(null);
    } catch (e) {
      // error handled by interceptor
    }
  };

  const handleUnarchiveSession = async (session: AgentSessionResponse) => {
    if (!selectedAgent) return;
    try {
      await unarchiveSession(selectedAgent, session.code);
      message.success('已取消归档');
      setArchivedSessions(prev => prev.filter(s => s.code !== session.code));
      setSessions(prev => [...prev, { ...session, archived: false }]);
    } catch (e) {
      // error handled by interceptor
    }
  };

  const handleSend = useCallback(async (value: string) => {
    if (!selectedAgent) return;
    const hasContent = value.trim();
    const hasFiles = attachments.length > 0;
    if (!hasContent && !hasFiles) return;

    // Check if all files are uploaded
    const uploading = attachments.some(a => a.uploading);
    if (uploading) {
      message.warning('请等待文件上传完成');
      return;
    }
    const anyError = attachments.some(a => a.error);
    if (anyError) {
      message.error('部分文件上传失败，请移除或重试');
      return;
    }

    const attachmentMetas = attachments
      .filter(a => a.ossKey)
      .map(a => ({
        ossKey: a.ossKey!,
        mime: a.file.type,
        filename: a.file.name,
        size: a.file.size
      }));

    // Optimistic UI：构造用户消息 block
    const userBlocks: ContentBlock[] = [];

    if (hasFiles) {
      for (const att of attachments) {
        if (!att.ossKey) continue;
        const isImage = ALLOWED_IMAGE_TYPES.includes(att.file.type);
        userBlocks.push({
          index: userBlocks.length,
          type: isImage ? 'image' : 'document',
          content: att.ossKey,
          ossKey: att.ossKey,
          mime: att.file.type,
          filename: att.file.name,
          size: att.file.size,
          url: att.previewUrl || att.publicUrl, // Use local preview URL for instant rendering
          status: 'completed',
        });
      }
    }

    if (hasContent) {
      userBlocks.push({
        index: userBlocks.length,
        type: 'text',
        content: value,
        status: 'completed',
      });
    }

    const tempUserItem = { id: `temp-turn-${Date.now()}`, role: 'user' as const, blocks: userBlocks };

    setRenderItems((prev) => [...prev, tempUserItem]);
    setStreaming(true);
    setCurrentContent('');
    senderRef.current?.clear?.();
    setAttachments([]);

    let accumulatedThought = '';
    let accumulatedContent = '';
    let currentSessionCode = selectedSessionCode;
    currentStreamSessionCodeRef.current = selectedSessionCode;
    let resolvedBlocks: ContentBlock[] = [];

    const updateDisplay = () => {
      const display = accumulatedThought
        ? `<think>${accumulatedThought}</think>${accumulatedContent}`
        : accumulatedContent;
      setCurrentContent(display);
      scrollToBottom();
    };

    const onStreamMessage = (data: ChatStreamData) => {
      if (data.sessionCode) {
        currentSessionCode = data.sessionCode;
        currentStreamSessionCodeRef.current = data.sessionCode;
      }
      if (data.blocks) {
        resolvedBlocks = data.blocks;
        setCurrentBlocks(data.blocks);
      }

      if (data.thinking !== undefined) {
        accumulatedThought = data.thinking;
      }
      accumulatedContent = data.content;
      updateDisplay();

      if (data.done) {
        // 流式结束，把助理 blocks 作为一个 render item 追加
        const assistantItem = {
          id: `temp-ai-item-${Date.now()}`,
          role: 'assistant' as const,
          blocks: resolvedBlocks.map(b => ({ ...b, status: 'completed' as const })),
        };
        setRenderItems((prev) => [...prev, assistantItem]);
        setCurrentContent('');
        setCurrentBlocks([]);
        setStreaming(false);

        // 流式结束后，重置游标以便下次加载从最新开始
        messageCursorRef.current = { nextCursor: null, hasMore: false, loadingMore: false };

        if (currentSessionCode) {
          if (currentSessionCode !== selectedSessionCode) {
            skipNextFetchRef.current = true;
            setSelectedSessionCode(currentSessionCode);
            fetchConversations(selectedAgent, true);
          }
        }
      }
    };

    const onStreamError = (err: any) => {
      const errorMsg = err instanceof Error ? err.message : String(err);
      if (errorMsg.includes('aborted') || errorMsg === 'AbortError' || errorMsg.includes('User aborted')) {
        // 用户主动中断 — 保留已积累的输出内容
        if (accumulatedContent || accumulatedThought || resolvedBlocks.length > 0) {
          const partialItem = {
            id: `temp-ai-item-${Date.now()}`,
            role: 'assistant' as const,
            blocks: resolvedBlocks.length > 0
              ? resolvedBlocks.map(b => ({ ...b, status: 'completed' as const }))
              : [{ index: 0, type: 'text' as const, content: accumulatedContent, status: 'completed' as const }],
          };
          setRenderItems((prev) => [...prev, partialItem]);

          if (currentSessionCode && currentSessionCode !== selectedSessionCode) {
            skipNextFetchRef.current = true;
            setSelectedSessionCode(currentSessionCode);
            fetchConversations(selectedAgent, true);
          }
        }
      } else {
        if (accumulatedContent || accumulatedThought) {
          const errorItem = {
            id: `temp-ai-item-${Date.now()}`,
            role: 'assistant' as const,
            blocks: resolvedBlocks.map(b => ({ ...b, status: 'completed' as const })),
          };
          setRenderItems((prev) => [...prev, errorItem]);

          if (currentSessionCode && currentSessionCode !== selectedSessionCode) {
            setSelectedSessionCode(currentSessionCode);
            fetchConversations(selectedAgent, true);
          }
        } else {
          message.error(`对话请求失败: ${errorMsg}`);
        }
      }
      setCurrentContent('');
      setCurrentBlocks([]);
      setStreaming(false);
    };

    const onStreamClose = () => {
      setStreaming(false);
    };

    const controller = new AbortController();
    abortControllerRef.current = controller;

    await chatStream(
      selectedAgent,
      value,
      selectedSessionCode || undefined,
      onStreamMessage,
      onStreamError,
      onStreamClose,
      controller.signal,
      attachmentMetas.length > 0 ? attachmentMetas : undefined,
    );
  }, [selectedAgent, selectedSessionCode, attachments]);

  const conversationItems = sessions.map((s) => ({
    key: s.code,
    label: s.title || '新对话',
    icon: <MessageOutlined />,
    description: new Date(s.updateTime || s.createTime).toLocaleString(),
  }));

  const archivedItems = archivedSessions.map((s) => ({
    key: s.code,
    label: s.title || '新对话',
    icon: <MessageOutlined style={{ color: '#8c8c8c' }} />,
    description: new Date(s.updateTime || s.createTime).toLocaleString(),
  }));

  const renderMessages = () => {
    // renderItems is already flattened into role-grouped items
    const allItems: { role: 'user' | 'assistant'; blocks: ContentBlock[]; id: string }[] = [...renderItems];

    if (streaming && currentBlocks.length > 0) {
      allItems.push({
        role: 'assistant',
        blocks: currentBlocks,
        id: 'streaming',
      });
    }

    const blocksToItems = (blocks: ContentBlock[]) => {
      const toolResultMap = new Map<string, ContentBlock>();
      blocks.filter(b => b.type === 'tool_result' && b.toolUseId).forEach(b => {
        toolResultMap.set(b.toolUseId!, b);
      });

      return blocks
        .filter(b => b.type !== 'tool_result' && b.type !== 'turn_start' && b.type !== 'turn_stop')
        .map((block) => {
        const isTool = block.type === 'tool_use';
        const isThinking = block.type === 'thinking';
        const isStreaming = block.status === 'streaming';

        let iconCls = 'tc-icon-wrapper';
        let iconEl: React.ReactNode = <CheckCircleOutlined />;
        let title = '回复';
        let content: React.ReactNode = <XMarkdown>{block.content}</XMarkdown>;

        if (isThinking) {
          iconCls += ' tc-icon-thinking';
          if (isStreaming) iconCls += ' tc-icon-streaming';
          iconEl = isStreaming ? <CoffeeOutlined spin /> : <CoffeeOutlined />;
          title = '深度思考';
        } else if (isTool) {
          iconCls += ' tc-icon-tool';
          const matchedResult = block.toolUseId ? toolResultMap.get(block.toolUseId) : undefined;
          if (isStreaming || (block.toolUseId && !matchedResult)) iconCls += ' tc-icon-streaming';
          iconEl = <ToolOutlined />;
          title = `调用工具: ${block.toolName || '未知'}`;
          content = (
            <div>
              <div style={{
                backgroundColor: '#fafafa', padding: '12px 16px', borderRadius: '12px',
                fontSize: '13px', fontFamily: "'Cascadia Code', 'Fira Code', monospace",
                border: '1px solid #f0f0f0', lineHeight: '1.6', whiteSpace: 'pre-wrap',
                wordBreak: 'break-all', color: '#434343'
              }}>
                <div style={{ color: '#8c8c8c', marginBottom: 4, fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Input Parameters</div>
                {block.toolInput}
              </div>
              {matchedResult && (
                <div style={{
                  marginTop: 8,
                  backgroundColor: matchedResult.toolResultIsError ? '#fff2f0' : '#f6ffed',
                  padding: '12px 16px', borderRadius: '12px', fontSize: '13px',
                  fontFamily: "'Cascadia Code', 'Fira Code', monospace",
                  border: `1px solid ${matchedResult.toolResultIsError ? '#ffccc7' : '#d9f7be'}`,
                  lineHeight: '1.6', whiteSpace: 'pre-wrap', wordBreak: 'break-all', color: '#434343',
                  maxHeight: 200, overflow: 'auto',
                }}>
                  <div style={{ color: '#8c8c8c', marginBottom: 4, fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                    {matchedResult.toolResultIsError ? 'Error Output' : 'Output'}
                  </div>
                  {matchedResult.toolResultContent}
                </div>
              )}
              {!matchedResult && !isStreaming && block.toolUseId && (
                <div style={{ marginTop: 8, color: '#8c8c8c', fontSize: '12px' }}>调用中…</div>
              )}
            </div>
          );
        } else {
          iconCls += ' tc-icon-success';
        }

        return {
          key: block.index.toString(),
          icon: <div className={iconCls}>{iconEl}</div>,
          title: <span className="ant-thought-chain-node-title">{title}</span>,
          status: (isStreaming ? 'loading' : 'success') as any,
          content,
          collapsible: isThinking || isTool,
          defaultCollapsed: block.status === 'completed' && (isThinking || isTool),
        };
      });
    };

    return allItems.map((item) => {
      if (item.role === 'user') {
        const textContent = item.blocks.filter(b => b.type === 'text').map(b => b.content).join('\n');
        const imageBlocks = item.blocks.filter(b => b.type === 'image');
        const documentBlocks = item.blocks.filter(b => b.type === 'document');
        const hasAttachments = imageBlocks.length > 0 || documentBlocks.length > 0;

        const userContent = () => {
          if (!hasAttachments) {
            return textContent;
          }
          return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, alignItems: 'flex-end' }}>
              {textContent && <div style={{ whiteSpace: 'pre-wrap' }}>{textContent}</div>}
              {imageBlocks.map(b => (
                <img
                  key={b.index}
                  src={b.url}
                  alt={b.filename || 'image'}
                  style={{ maxWidth: 280, maxHeight: 200, borderRadius: 12, objectFit: 'cover', cursor: 'pointer' }}
                  onClick={() => window.open(b.url, '_blank')}
                />
              ))}
              {documentBlocks.map(b => (
                <a
                  key={b.index}
                  href={b.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{
                    display: 'flex', alignItems: 'center', gap: 8,
                    padding: '8px 14px', borderRadius: 10,
                    backgroundColor: '#fff', border: '1px solid #e0e0e0',
                    fontSize: '13px', color: '#333', textDecoration: 'none', maxWidth: 280,
                  }}
                >
                  <span style={{
                    flexShrink: 0, width: 24, height: 24, borderRadius: 6,
                    backgroundColor: '#f0f0f0', display: 'flex', alignItems: 'center',
                    justifyContent: 'center', fontSize: '14px'
                  }}></span>
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {b.filename || 'document'}
                  </span>
                  {b.size != null && (
                    <span style={{ color: '#999', fontSize: '11px', flexShrink: 0 }}>
                      {b.size > 1024 * 1024 ? `${(b.size / 1024 / 1024).toFixed(1)}M` : `${(b.size / 1024).toFixed(0)}K`}
                    </span>
                  )}
                </a>
              ))}
            </div>
          );
        };

        return (
          <Bubble
            key={item.id}
            content={textContent}
            contentRender={hasAttachments ? userContent : undefined}
            role="user"
            placement="end"
            style={{ maxWidth: '85%', marginBottom: 32 }}
            styles={{ content: { backgroundColor: '#f5f5f5', color: '#000', borderRadius: 16, padding: '12px 18px' } }}
          />
        );
      }

      // Assistant turn — 统一走 block 渲染
      const items = blocksToItems(item.blocks);
      if (items.length > 0) {
        return (
          <div key={item.id} style={{ marginBottom: 32 }}>
            <div className="thought-chain-container">
              <ThoughtChain items={items} style={{ width: '100%' }} />
            </div>
          </div>
        );
      }

      const fallbackContent = item.blocks.map(b => b.content).join('\n');
      return (
        <Bubble
          key={item.id}
          content={fallbackContent}
          contentRender={(c) => <XMarkdown>{c}</XMarkdown>}
          role="ai"
          placement="start"
          style={{ maxWidth: '85%', marginBottom: 32 }}
          styles={{ content: { backgroundColor: 'transparent', color: '#000', padding: '4px 0px', fontSize: '15px', lineHeight: '1.6' } }}
        />
      );
    });
  };



  if (agents.length === 0 && !selectedAgent) {
    return (
      <div style={{ height: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <Welcome variant="borderless" icon="🤖" title="暂无可用 Agent" description="请先配置 Agent" />
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 60px)', width: '100%', position: 'relative', overflow: 'hidden' }}>

      {/* Sidebar for Conversations */}
      <div style={{
        width: sidebarVisible ? 280 : 0,
        opacity: sidebarVisible ? 1 : 0,
        height: '100%',
        borderRight: '1px solid #f0f0f0',
        backgroundColor: '#fafafa',
        display: 'flex',
        flexDirection: 'column',
        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        overflow: 'hidden',
        flexShrink: 0
      }}>
        <div style={{ padding: '16px', display: 'flex', alignItems: 'center', gap: 8 }}>
          <Button
            type="text"
            icon={<PlusOutlined />}
            style={{ flex: 1, height: 40, borderRadius: 8, backgroundColor: '#f1f1f1', textAlign: 'left', fontWeight: 500, display: 'flex', justifyContent: 'flex-start', alignItems: 'center' }}
            onClick={handleCreateNewChat}
          >
            新聊天
          </Button>
          <Tooltip title="关闭侧边栏">
            <Button type="text" icon={<MenuFoldOutlined />} onClick={() => setSidebarVisible(false)} style={{ fontSize: 18, color: '#666', width: 40, height: 40, borderRadius: 8 }} />
          </Tooltip>
        </div>

        <div style={{ flex: 1, overflow: 'auto', padding: '0 8px' }}>
          <Spin spinning={loadingHistory}>
            <ConfigProvider theme={{ components: { Menu: { itemSelectedBg: 'rgba(0,0,0,0.04)', itemSelectedColor: '#000' } } }}>
              <div className="conversations-wrapper" style={{ __html: `<style>.conversations-wrapper .ant-menu-item-selected { background-color: rgba(0,0,0,0.04) !important; color: #000 !important; }</style>` } as any}>
                {/* 对话分组 */}
                <div style={{ marginBottom: 8 }}>
                  <div style={{ padding: '8px 12px 4px', fontSize: 12, color: '#8c8c8c', fontWeight: 500 }}>对话</div>
                  <Conversations
                    items={conversationItems}
                    activeKey={selectedSessionCode}
                    onActiveChange={(key) => setSelectedSessionCode(key)}
                    menu={(conversation) => {
                      const s = sessions.find(item => item.code === conversation.key);
                      if (!s) return undefined;
                      return {
                        items: [
                          { key: 'rename', label: '重命名', icon: <EditOutlined /> },
                          { key: 'archive', label: '归档', icon: <FolderOutlined /> },
                        ],
                        onClick: ({ key }: { key: string }) => {
                          if (key === 'rename') { setRenamingSession(s); setRenameTitle(s.title || ''); }
                          if (key === 'archive') handleArchiveSession(s);
                        },
                      };
                    }}
                  />
                </div>
                {/* 已归档分组 */}
                {archivedSessions.length > 0 && (
                  <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 8 }}>
                    <div style={{ padding: '8px 12px 4px', fontSize: 12, color: '#8c8c8c', fontWeight: 500 }}>已归档</div>
                    <Conversations
                      items={archivedItems}
                      activeKey={selectedSessionCode}
                      onActiveChange={(key) => setSelectedSessionCode(key)}
                      menu={(conversation) => {
                        const s = archivedSessions.find(item => item.code === conversation.key);
                        if (!s) return undefined;
                        return {
                          items: [
                            { key: 'unarchive', label: '取消归档', icon: <InboxOutlined /> },
                            { key: 'delete', label: '删除', danger: true, icon: <DeleteOutlined /> },
                          ],
                          onClick: ({ key }: { key: string }) => {
                            if (key === 'unarchive') handleUnarchiveSession(s);
                            if (key === 'delete') handleDeleteSession(s);
                          },
                        };
                      }}
                    />
                  </div>
                )}
              </div>
            </ConfigProvider>
          </Spin>
        </div>
      </div>

      {/* Main Chat Area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', backgroundColor: '#fff', height: '100%', position: 'relative' }}>

        {/* Top Header Area */}
        <div style={{ position: 'absolute', top: 16, left: 0, right: 0, zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: `0 ${sidebarVisible ? 16 : 16}px` }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {!sidebarVisible && (
              <Tooltip title="打开侧边栏">
                <Button
                  type="text"
                  icon={<MenuUnfoldOutlined />}
                  onClick={() => setSidebarVisible(true)}
                  style={{ fontSize: 18, color: '#666' }}
                />
              </Tooltip>
            )}

            <AgentSelect
              agents={agents}
              value={selectedAgent}
              onChange={(val) => {
                setSelectedAgent(val);
                setSelectedSessionCode('');
              }}
            />
          </div>

          {!fileSidebarVisible && selectedAgent && (
            <Tooltip title="打开文件面板">
              <Button
                type="text"
                icon={<MenuUnfoldOutlined />}
                onClick={() => setFileSidebarVisible(true)}
                style={{ fontSize: 18, color: '#666' }}
              />
            </Tooltip>
          )}
        </div>

        <div ref={scrollContainerRef} style={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column', position: 'relative' }} onScroll={handleScroll}>
          {loadingMessages && (
            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, zIndex: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(255,255,255,0.7)' }}>
              <Spin size="large" />
            </div>
          )}
          {messageCursorRef.current.loadingMore && (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '8px 0', color: '#8c8c8c', fontSize: '12px' }}>
              <Spin size="small" /> 加载更多消息...
            </div>
          )}
          <div style={{ padding: '60px 10% 80px', display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
            {renderItems.length === 0 && !streaming ? (
              <div style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                <Welcome
                  variant="borderless"
                  title={<Title level={3} style={{ marginTop: 0, fontWeight: 500 }}>Hello, I'm {agents.find((a) => a.code === selectedAgent)?.name || 'Agent'}</Title>}
                  description={<Text type="secondary" style={{ fontSize: 16 }}>How can I help you today?</Text>}
                />
              </div>
            ) : (
              renderMessages()
            )}
            <div ref={messagesEndRef} style={{ height: 1 }} />
          </div>
        </div>

        {/* Input Area */}
        <div style={{ padding: '16px 10% 24px' }}>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept="image/png,image/jpeg,image/gif,image/webp,application/pdf,text/plain,text/markdown"
            style={{ display: 'none' }}
            onChange={handleFileInputChange}
          />
          <Sender
            ref={senderRef}
            loading={streaming}
            onSubmit={handleSend}
            onCancel={() => {
              const sessionCode = currentStreamSessionCodeRef.current;
              if (selectedAgent && sessionCode) {
                interruptStream(selectedAgent, sessionCode).catch(() => {});
              }
              if (abortControllerRef.current) {
                abortControllerRef.current.abort();
                abortControllerRef.current = null;
              }
            }}
            placeholder="Message..."
            header={attachments.length > 0 && (
              <div style={{
                display: 'flex', gap: 12, marginBottom: 4,
                overflowX: 'auto', padding: '12px 12px 0',
                scrollbarWidth: 'none',
              }}>
                {attachments.map((att) => {
                  const isImage = ALLOWED_IMAGE_TYPES.includes(att.file.type);
                  const hasError = !!att.error;
                  return (
                    <Tooltip title={att.error || att.file.name} key={att.uid}>
                      <div
                        style={{
                          position: 'relative', flexShrink: 0, width: 80, height: 80,
                          borderRadius: 12, overflow: 'hidden',
                          backgroundColor: '#f5f5f5',
                          border: `1px solid ${hasError ? '#ff4d4f' : '#f0f0f0'}`,
                          transition: 'all 0.2s',
                        }}
                      >
                        {isImage && att.previewUrl && !hasError ? (
                          <img
                            src={att.previewUrl}
                            alt={att.file.name}
                            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                          />
                        ) : (
                          <div style={{
                            width: '100%', height: '100%',
                            display: 'flex', flexDirection: 'column',
                            alignItems: 'center', justifyContent: 'center',
                            color: hasError ? '#ff4d4f' : '#8c8c8c',
                            gap: 4
                          }}>
                            {att.uploading ? (
                              <Spin size="small" />
                            ) : (
                              <>
                                <div style={{ fontSize: 28 }}>
                                  {hasError ? '⚠️' : (att.file.type.includes('pdf') ? '📕' : '📄')}
                                </div>
                                <div style={{ fontSize: 10, width: '100%', textAlign: 'center', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', padding: '0 4px' }}>
                                  {att.file.name}
                                </div>
                              </>
                            )}
                          </div>
                        )}
                        {/* Remove button */}
                        {!att.uploading && (
                          <div
                            onClick={() => removeAttachment(att.uid)}
                            style={{
                              position: 'absolute', top: 4, right: 4,
                              width: 20, height: 20, borderRadius: '50%',
                              backgroundColor: 'rgba(0,0,0,0.4)',
                              display: 'flex', alignItems: 'center', justifyContent: 'center',
                              cursor: 'pointer', color: '#fff', fontSize: 12,
                              zIndex: 10,
                              backdropFilter: 'blur(4px)',
                            }}
                          >
                            ×
                          </div>
                        )}
                      </div>
                    </Tooltip>
                  );
                })}
              </div>
            )}
            prefix={(
              <Button
                type="text"
                icon={<PaperClipOutlined style={{ fontSize: 20, color: '#8c8c8c' }} />}
                onClick={handleFileSelect}
                disabled={streaming || attachments.length >= MAX_ATTACHMENTS}
                style={{ width: 36, height: 36, borderRadius: '50%' }}
              />
            )}
          />
          <div style={{ textAlign: 'center', marginTop: 12 }}>
            <Text style={{ fontSize: 12, color: '#bfbfbf' }}>AI responses can be inaccurate. Please verify information.</Text>
          </div>
        </div>

      </div>

      {/* Right File Sidebar */}
      <div style={{
        width: fileSidebarVisible ? 700 : 0,
        opacity: fileSidebarVisible ? 1 : 0,
        height: '100%',
        borderLeft: fileSidebarVisible ? '1px solid #f0f0f0' : 'none',
        backgroundColor: '#fafafa',
        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        overflow: 'hidden',
        flexShrink: 0
      }}>
        {fileSidebarVisible && selectedAgent && (
          <div style={{ height: '100%', width: 700 }}>
            <AgentFileWorkspace
              agentCode={selectedAgent}
              rootPath="home"
              allowCreate
              allowEdit
              allowSave
              compact
              onClose={() => setFileSidebarVisible(false)}
              height="100%"
            />
          </div>
        )}
      </div>

      <Modal
        title="重命名对话"
        open={!!renamingSession}
        onOk={handleRenameSession}
        onCancel={() => setRenamingSession(null)}
        okText="确定"
        cancelText="取消"
        width={360}
      >
        <Input
          value={renameTitle}
          onChange={(e) => setRenameTitle(e.target.value)}
          onPressEnter={handleRenameSession}
          placeholder="请输入对话名称"
          maxLength={100}
          style={{ marginTop: 8 }}
          autoFocus
        />
      </Modal>
    </div>
  );
}
