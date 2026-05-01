import { useState, useEffect, useRef, useCallback } from 'react';
import { useSearchParams } from 'umi';

import { message, Avatar, Typography, Button, Spin, ConfigProvider, Tooltip, Modal, Input } from 'antd';
import {
  RobotOutlined, UserOutlined, PlusOutlined,
  MessageOutlined, DeleteOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
  CheckCircleOutlined, ToolOutlined, CoffeeOutlined,
  FolderOutlined, InboxOutlined, EditOutlined
} from '@ant-design/icons';
import { Bubble, Sender, Welcome, Conversations, Think, ThoughtChain } from '@ant-design/x';
import XMarkdown from '@ant-design/x-markdown';
import './style.less';
import AgentSelect from './components/AgentSelect';
import { listAgents } from '@/services/agent/AgentController';
import type { AgentDetailResponse } from '@/services/agent/typings';
import { listSessions, listMessages, deleteSession, archiveSession, unarchiveSession, renameSession } from '@/services/conversation/ConversationController';
import { chatStream } from '@/services/conversation/AgentMessageService';
import type { AgentSessionResponse, AgentMessageResponse, ChatStreamData, ContentBlock } from '@/services/conversation/typings';
import AgentFileWorkspace from '@/components/AgentFileWorkspace';

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
  
  // Messaging states
  const [messages, setMessages] = useState<AgentMessageResponse[]>([]);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [streaming, setStreaming] = useState(false);
  const [currentContent, setCurrentContent] = useState('');
  const [currentBlocks, setCurrentBlocks] = useState<ContentBlock[]>([]);
  const [currentProtocolType, setCurrentProtocolType] = useState<string>('');
  const abortControllerRef = useRef<AbortController | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const senderRef = useRef<any>(null);
  const skipNextFetchRef = useRef(false);

  const fetchAgents = async () => {
    try {
      const res = await listAgents({ page: 0, size: 100 });
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
          setMessages([]);
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

  const fetchSessionMessages = async (agentCode: string, sessionCode: string) => {
    try {
      setLoadingMessages(true);
      const res = await listMessages(agentCode, sessionCode, { page: 0, size: 100 });
      setMessages(res.data || []);
    } catch (e) {
      message.error('无法加载历史消息');
    } finally {
      setLoadingMessages(false);
    }
  };

  useEffect(() => {
    if (selectedAgent && selectedSessionCode) {
      if (skipNextFetchRef.current) {
        skipNextFetchRef.current = false;
        return;
      }
      fetchSessionMessages(selectedAgent, selectedSessionCode);
    } else {
      setMessages([]);
    }
  }, [selectedSessionCode, selectedAgent]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'auto' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, currentContent]);

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



  const handleCreateNewChat = () => {
    setSelectedSessionCode('');
    setMessages([]);
  };

  const handleDeleteSession = async (session: AgentSessionResponse) => {
    if (!selectedAgent) return;
    try {
      await deleteSession(selectedAgent, session.code);
      message.success('已删除对话');
      if (selectedSessionCode === session.code) {
        setSelectedSessionCode('');
        setMessages([]);
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
        setMessages([]);
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
    if (!value.trim() || !selectedAgent) return;

    // Optimistic UI
    const tempUserMsg: AgentMessageResponse = {
      code: `temp-user-${Date.now()}`,
      role: 'USER',
      protocolType: '',
      status: '',
      payloadJson: JSON.stringify({ role: 'user', content: value }),
      errorPayloadJson: '',
      externalMessageId: '',
      messageIndex: messages.length + 1,
    };

    setMessages((prev) => [...prev, tempUserMsg]);
    setStreaming(true);
    setCurrentContent('');
    senderRef.current?.clear?.();

    let accumulatedThought = '';
    let accumulatedContent = '';
    let currentSessionCode = selectedSessionCode;
    let resolvedProtocolType = '';
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
      }
      if (data.protocolType) {
        resolvedProtocolType = data.protocolType;
        setCurrentProtocolType(data.protocolType);
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
        const assistantMsg: AgentMessageResponse = {
          code: `temp-ai-${Date.now()}`,
          role: 'ASSISTANT',
          protocolType: resolvedProtocolType,
          status: 'COMPLETED',
          payloadJson: JSON.stringify({ 
            role: 'assistant', 
            content: accumulatedContent, 
            thinking: accumulatedThought,
            blocks: resolvedBlocks
          }),
          errorPayloadJson: '',
          externalMessageId: '',
          messageIndex: messages.length + 2,
        };
        setMessages((prev) => [...prev, assistantMsg]);
        setCurrentContent('');
        setCurrentBlocks([]);
        setCurrentProtocolType('');
        setStreaming(false);

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
      // 忽略用户主动中止或页面跳转导致的 AbortError
      const errorMsg = err instanceof Error ? err.message : String(err);
      if (errorMsg.includes('aborted') || errorMsg === 'AbortError' || errorMsg.includes('User aborted')) {
        // 用户主动中止，无需处理
      } else {
        if (accumulatedContent || accumulatedThought) {
          const assistantMsg: AgentMessageResponse = {
            code: `temp-ai-${Date.now()}`,
            role: 'ASSISTANT',
            protocolType: resolvedProtocolType,
            status: 'FAILED',
            payloadJson: JSON.stringify({ 
              role: 'assistant', 
              content: accumulatedContent, 
              thinking: accumulatedThought,
              blocks: resolvedBlocks
            }),
            errorPayloadJson: '',
            externalMessageId: '',
            messageIndex: messages.length + 2,
          };
          setMessages((prev) => [...prev, assistantMsg]);

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
      setCurrentProtocolType('');
      setStreaming(false);
    };

    const onStreamClose = () => {
      setStreaming(false);
    };

    // 创建新的 AbortController
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
    );
  }, [selectedAgent, selectedSessionCode, messages.length]);

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
    const parsePayload = (msg: AgentMessageResponse) => {
      const payload = msg.payloadJson || '';
      if (!payload) return { content: '', thought: '', blocks: [] };
      try {
        const parsed = JSON.parse(payload);

        // 1. 兼容前端乐观更新格式 (handleSend 中保存的格式)
        if (parsed.blocks && Array.isArray(parsed.blocks)) {
          return {
            content: typeof parsed.content === 'string' ? parsed.content : '',
            thought: parsed.thinking || parsed.thought || '',
            blocks: parsed.blocks as ContentBlock[]
          };
        }

        // 2. 兼容 Anthropic 标准消息格式 (后端存储的格式)
        if (Array.isArray(parsed.content)) {
          const blocks: ContentBlock[] = parsed.content.map((b: any, idx: number) => {
            if (b.type === 'text') {
              return { index: idx, type: 'text', content: b.text || '', status: 'completed' };
            }
            if (b.type === 'thinking') {
              return { index: idx, type: 'thinking', content: b.thinking || '', status: 'completed' };
            }
            if (b.type === 'tool_use') {
              return {
                index: idx,
                type: 'tool_use',
                content: b.name || '',
                toolName: b.name,
                toolInput: typeof b.input === 'object' ? JSON.stringify(b.input, null, 2) : (b.input || ''),
                status: 'completed'
              };
            }
            return { index: idx, type: b.type, content: JSON.stringify(b), status: 'completed' };
          });

          // 提取纯文本用于兜底展示
          const plainContent = blocks.filter(b => b.type === 'text').map(b => b.content).join('\n');
          const thought = blocks.filter(b => b.type === 'thinking').map(b => b.content).join('\n');

          return { content: plainContent, thought, blocks };
        }

        // 3. 兼容 OpenAI 标准消息格式
        if (parsed.choices && parsed.choices[0]?.message) {
          const message = parsed.choices[0].message;
          return {
            content: message.content || '',
            thought: message.reasoning_content || '', // 某些模型可能有推理内容
            blocks: []
          };
        }

        // 4. 兼容用户消息或简单格式 { role, content }
        if (parsed.role && parsed.content) {
            return { content: typeof parsed.content === 'string' ? parsed.content : JSON.stringify(parsed.content), thought: '', blocks: [] };
        }

        // 兜底：处理可能是 raw string 的情况
        const content = parsed.content || parsed.text || (typeof parsed === 'string' ? parsed : '');
        return { content: typeof content === 'string' ? content : JSON.stringify(content), thought: '', blocks: [] };
      } catch (e) {
        return { content: payload, thought: '', blocks: [] };
      }
    };

    const allMessages: { 
      role: string, 
      content: string, 
      id: string, 
      thought?: string, 
      protocolType?: string, 
      blocks?: ContentBlock[] 
    }[] = messages.map(msg => {
      const { content, thought, blocks } = parsePayload(msg);
      return {
        role: msg.role === 'USER' || msg.role === 'user' ? 'local' : 'ai',
        content,
        thought,
        protocolType: msg.protocolType,
        blocks,
        id: msg.code
      };
    });

    if (streaming) {
      allMessages.push({ 
        role: 'ai', 
        content: currentContent, 
        id: 'streaming', 
        protocolType: currentProtocolType,
        blocks: currentBlocks 
      });
    }

    const blocksToItems = (blocks: ContentBlock[]) => {
      return blocks.map((block) => {
        const isTool = block.type === 'tool_use';
        const isThinking = block.type === 'thinking';
        const isStreaming = block.status === 'streaming';

        let iconCls = 'tc-icon-wrapper';
        let iconEl: React.ReactNode = <CheckCircleOutlined />;
        let title = '回复';
        let content: React.ReactNode = (
          <XMarkdown>{block.content}</XMarkdown>
        );

        if (isThinking) {
          iconCls += ' tc-icon-thinking';
          if (isStreaming) iconCls += ' tc-icon-streaming';
          iconEl = isStreaming ? <CoffeeOutlined spin /> : <CoffeeOutlined />;
          title = '深度思考';
        } else if (isTool) {
          iconCls += ' tc-icon-tool';
          if (isStreaming) iconCls += ' tc-icon-streaming';
          iconEl = <ToolOutlined />;
          title = `调用工具: ${block.toolName || '未知'}`;
          content = (
            <div style={{
              backgroundColor: '#fafafa',
              padding: '12px 16px',
              borderRadius: '12px',
              fontSize: '13px',
              fontFamily: "'Cascadia Code', 'Fira Code', monospace",
              border: '1px solid #f0f0f0',
              lineHeight: '1.6',
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all',
              color: '#434343'
            }}>
              <div style={{ color: '#8c8c8c', marginBottom: 4, fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Input Parameters</div>
              {block.toolInput}
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

    return allMessages.map((msg) => {
      if (msg.role === 'local') {
        return (
          <Bubble
            key={msg.id}
            content={msg.content}
            role="user"
            placement="end"
            style={{ maxWidth: '85%', marginBottom: 32 }}
            styles={{
              content: {
                backgroundColor: '#f5f5f5',
                color: '#000',
                borderRadius: 16,
                padding: '12px 18px',
              }
            }}
          />
        );
      }

      // AI Message
      const isAnthropic = msg.protocolType === 'ANTHROPIC_MESSAGES';
      
      if (isAnthropic && msg.blocks && msg.blocks.length > 0) {
        return (
          <div key={msg.id} style={{ marginBottom: 32 }}>
             <div className="thought-chain-container">
               <ThoughtChain 
                  items={blocksToItems(msg.blocks)} 
                  style={{ width: '100%' }}
               />
             </div>
          </div>
        );
      }

      // Default OpenAI or fallback
      const getDisplayContent = (content: string, thought?: string) => {
        if (thought) {
          return `<think>${thought}</think>${content}`;
        }
        return content;
      };

      const displayContent = msg.id === 'streaming' ? msg.content : getDisplayContent(msg.content, msg.thought);

      return (
        <Bubble
          key={msg.id}
          content={displayContent}
          contentRender={(content) => (
            <XMarkdown
              components={{
                think: ({ children }) => (
                  <Think title="正在思考..." children={children} />
                )
              }}
            >
              {content}
            </XMarkdown>
          )}
          role="ai"
          placement="start"
          style={{ maxWidth: '85%', marginBottom: 32 }}
          styles={{
            content: {
              backgroundColor: 'transparent',
              color: '#000',
              padding: '4px 0px',
              fontSize: '15px',
              lineHeight: '1.6',
            }
          }}
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

        <div style={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column', position: 'relative' }}>
          {loadingMessages && (
            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, zIndex: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(255,255,255,0.7)' }}>
              <Spin size="large" />
            </div>
          )}
          <div style={{ padding: '60px 10% 80px', display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
            {messages.length === 0 && !streaming ? (
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
          <div style={{
            backgroundColor: '#f5f5f5',
            borderRadius: 24,
            overflow: 'hidden',
            padding: '4px 8px'
          }}>
            <Sender
              ref={senderRef}
              loading={streaming}
              onSubmit={handleSend}
              onCancel={() => {
                if (abortControllerRef.current) {
                  abortControllerRef.current.abort();
                  abortControllerRef.current = null;
                }
              }}
              placeholder="Message..."
              styles={{
                input: { border: 'none', boxShadow: 'none', backgroundColor: 'transparent' },
              }}
            />
          </div>
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
