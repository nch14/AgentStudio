import { useState, useEffect, useCallback } from 'react';
import dayjs from 'dayjs';
import {
  Button, Badge, message, Typography, Space, Spin, Tag, Card, Tabs, Progress, Timeline,
  List, Empty, Input, Tooltip, Popconfirm, Radio, Divider
} from 'antd';
import {
  ArrowLeftOutlined, PlayCircleOutlined, StopOutlined,
  CommentOutlined, HistoryOutlined,
  ReloadOutlined, InfoCircleOutlined, FolderOpenOutlined,
  SyncOutlined, ClockCircleOutlined, QuestionCircleOutlined,
  MessageOutlined
} from '@ant-design/icons';
import { useParams, history } from '@umijs/max';
import { getTaskDetail, cancelTask, retryTask, rollbackTask } from '@/services/task/TaskController';
import { listTaskTurns, resumeTurn } from '@/services/task/TaskTurnController';
import { listQuestions, listInstructions, resolveQuestions } from '@/services/task/CoordinationController';
import { getAgentDetail } from '@/services/agent/AgentController';
import { listMessagesByCursor } from '@/services/conversation/ConversationController';
import AgentFileWorkspace from '@/components/AgentFileWorkspace';
import type {
  TaskDetailResponse, TaskTurnListItemResponse,
  QuestionsResponse, InstructionResponse
} from '@/services/task/typings';
import type { AgentSessionTurnDTO, AgentSessionBlockDTO } from '@/services/conversation/typings';
import './style.less';

const { Text, Title, Paragraph } = Typography;

const statusColorMap: Record<string, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  SUCCEEDED: 'success',
  FAILED: 'error',
  CANCELLED: 'warning',
};

const statusLabelMap: Record<string, string> = {
  PENDING: '待处理',
  RUNNING: '执行中',
  SUCCEEDED: '已成功',
  FAILED: '失败',
  CANCELLED: '已取消',
};

const turnStatusLabelMap: Record<string, string> = {
  RUNNING: '执行中',
  SUSPENDED: '已暂停',
  TERMINATED: '已终止',
  HANGING: '挂起中',
  CANCELLED: '已取消',
  SUCCEEDED: '已成功',
  FAILED: '失败',
};

const sourceLabelMap: Record<string, string> = {
  SCHEDULED_CREATE: '定时任务创建',
  USER_CREATE: '用户手动创建',
  AGENT_CREATE: 'Agent 创建',
};

export default function TaskDetailPage() {
  const { code } = useParams<{ code: string }>();
  const [task, setTask] = useState<TaskDetailResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('info');
  const [agentName, setAgentName] = useState<string>('');

  // Execution Data
  const [turns, setTurns] = useState<TaskTurnListItemResponse[]>([]);
  const [turnsLoading, setTurnsLoading] = useState(false);

  // Coordination Data
  const [questions, setQuestions] = useState<QuestionsResponse[]>([]);
  const [instructions, setInstructions] = useState<InstructionResponse[]>([]);
  const [coordinationLoading, setCoordinationLoading] = useState(false);

  // Process Data
  const [processTurns, setProcessTurns] = useState<AgentSessionTurnDTO[]>([]);
  const [processLoading, setProcessLoading] = useState(false);

  const fetchTask = useCallback(async () => {
    if (!code) return;
    setLoading(true);
    try {
      const res = await getTaskDetail(code);
      setTask(res.data);
      if (res.data?.agentCode) {
        getAgentDetail(res.data.agentCode).then(agentRes => {
          setAgentName(agentRes.data?.name || '');
        }).catch(() => {});
      }
    } finally {
      setLoading(false);
    }
  }, [code]);

  const fetchTurns = useCallback(async () => {
    if (!code) return;
    setTurnsLoading(true);
    try {
      const res = await listTaskTurns(code);
      setTurns(res.data || []);
    } finally {
      setTurnsLoading(false);
    }
  }, [code]);

  const fetchCoordination = useCallback(async () => {
    if (!code) return;
    setCoordinationLoading(true);
    try {
      const [qRes, iRes] = await Promise.all([
        listQuestions(code),
        listInstructions(code)
      ]);
      setQuestions(qRes.data || []);
      setInstructions(iRes.data || []);
    } finally {
      setCoordinationLoading(false);
    }
  }, [code]);

  const fetchProcess = useCallback(async () => {
    if (!code || !task?.sessionCode || !task?.agentCode) return;
    setProcessLoading(true);
    try {
      const res = await listMessagesByCursor(task.agentCode, task.sessionCode, { cursor: 0, size: 100 });
      setProcessTurns(res.data || []);
    } finally {
      setProcessLoading(false);
    }
  }, [code, task?.sessionCode, task?.agentCode]);

  useEffect(() => {
    fetchTask();
  }, [fetchTask]);

  useEffect(() => {
    if (activeTab === 'execution') fetchTurns();
    if (activeTab === 'coordination') fetchCoordination();
    if (activeTab === 'process') fetchProcess();
  }, [activeTab, fetchTurns, fetchCoordination, fetchProcess]);

  const handleRefresh = useCallback(() => {
    fetchTask();
    if (activeTab === 'execution') fetchTurns();
    if (activeTab === 'coordination') fetchCoordination();
    if (activeTab === 'process') fetchProcess();
  }, [activeTab, fetchTask, fetchTurns, fetchCoordination, fetchProcess]);

  const handleCancel = async () => {
    if (!code) return;
    try {
      await cancelTask(code);
      message.success('任务已取消');
      fetchTask();
    } catch {}
  };

  const handleRetry = async () => {
    if (!code) return;
    try {
      await retryTask(code);
      message.success('重试已触发');
      fetchTask();
    } catch {}
  };

  const handleRollback = async () => {
    if (!code) return;
    try {
      await rollbackTask(code);
      message.success('任务已重新运行');
      fetchTask();
    } catch {}
  };

  const handleResumeTurn = async (turnCode: string) => {
    try {
      await resumeTurn(turnCode);
      message.success('回合已恢复执行');
      fetchTurns();
    } catch {}
  };

  const handleResolveQuestion = async (qCode: string, answers: any) => {
    if (!code) return;
    try {
      await resolveQuestions(code, qCode, { answers });
      message.success('回答已提交');
      fetchCoordination();
    } catch {}
  };

  if (!code) return <div className="page-error">无效的任务编码</div>;

  const renderExecution = () => (
    <div className="tab-pane execution-pane">
      <div className="pane-header">
        <Title level={5}>执行日志与回合</Title>
        <Button size="small" icon={<ReloadOutlined />} onClick={fetchTurns} loading={turnsLoading}>刷新</Button>
      </div>
      <Timeline
        pending={task?.status === 'RUNNING' ? '任务正在执行中...' : false}
        items={turns.map((turn) => ({
          color: turn.runStatus === 'SUCCEEDED' ? 'green' : turn.runStatus === 'FAILED' ? 'red' : turn.runStatus === 'HANGING' ? 'orange' : 'blue',
          dot: turn.runStatus === 'RUNNING' ? <SyncOutlined spin /> : 
               turn.runStatus === 'HANGING' ? <ClockCircleOutlined /> : undefined,
          children: (
            <Card className="turn-card" size="small" bordered={false}>
              <div className="turn-header">
                <Space>
                  <Text strong>Turn #{turn.turnNo}</Text>
                  <Tag color={turn.runStatus === 'SUCCEEDED' ? 'success' : turn.runStatus === 'FAILED' ? 'error' : turn.runStatus === 'HANGING' ? 'warning' : 'processing'}>
                    {turnStatusLabelMap[turn.runStatus] || turn.runStatus}
                  </Tag>
                  <Text type="secondary" style={{ fontSize: 12 }}>{turn.startedAt ? dayjs(turn.startedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}</Text>
                </Space>
                {turn.runStatus === 'HANGING' && (
                  <Button type="primary" size="small" onClick={() => handleResumeTurn(turn.turnCode)}>恢复执行</Button>
                )}
              </div>
              <div className="turn-content">
                <Paragraph ellipsis={{ rows: 2, expandable: true, symbol: '展开' }}>
                  {turn.finalSummary || '等待执行结果...'}
                </Paragraph>
              </div>
            </Card>
          )
        }))}
      />
      {turns.length === 0 && !turnsLoading && <Empty description="暂无回合记录" />}
    </div>
  );

  const renderProcess = () => (
    <div className="tab-pane process-pane">
      <div className="pane-header">
        <Title level={5}>执行过程</Title>
        <Button size="small" icon={<ReloadOutlined />} onClick={fetchProcess} loading={processLoading}>刷新</Button>
      </div>
      {!task?.sessionCode ? (
        <Empty description="任务尚未关联对话会话" />
      ) : (
        <List
          loading={processLoading}
          dataSource={processTurns}
          locale={{ emptyText: '暂无过程消息' }}
          renderItem={(turn) => (
            <Card size="small" className="process-turn-card" bordered={false} style={{ marginBottom: 12 }}>
              <div className="process-turn-header">
                <Tag color="blue" bordered={false}>Turn</Tag>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {turn.turnCode?.substring(0, 12) || '-'}
                </Text>
              </div>
              <div className="process-turn-body">
                {turn.blocks?.map((block: AgentSessionBlockDTO) => (
                  <div key={block.code} className={`process-block process-block-${block.type.toLowerCase()}`}>
                    {block.role === 'USER' && (
                      <div className="process-block-user">
                        <Tag color="geekblue">用户</Tag>
                        <Text>{(block.payload as any)?.text || '-'}</Text>
                      </div>
                    )}
                    {block.type === 'TEXT' && block.role === 'ASSISTANT' && (
                      <div className="process-block-text">
                        <Paragraph>{(block.payload as any)?.text || ''}</Paragraph>
                      </div>
                    )}
                    {block.type === 'THINKING' && (
                      <details className="process-block-thinking">
                        <summary>思考过程</summary>
                        <pre className="thinking-content">{(block.payload as any)?.thinking || ''}</pre>
                      </details>
                    )}
                    {block.type === 'TOOL_USE' && (
                      <div className="process-block-tool">
                        <Tag color="orange">工具调用</Tag>
                        <Text strong>{(block.payload as any)?.name || '-'}</Text>
                        <pre className="tool-input-content">
                          {JSON.stringify((block.payload as any)?.input, null, 2)}
                        </pre>
                      </div>
                    )}
                    {block.type === 'TOOL_RESULT' && (
                      <div className="process-block-tool-result">
                        <Tag color={block.payload && (block.payload as any).isError ? 'red' : 'green'}>工具结果</Tag>
                        <pre className="tool-result-content">
                          {typeof (block.payload as any)?.content === 'string'
                            ? (block.payload as any).content
                            : JSON.stringify((block.payload as any)?.content, null, 2)}
                        </pre>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </Card>
          )}
        />
      )}
    </div>
  );

  const renderCoordination = () => (
    <div className="tab-pane coordination-pane">
      <Tabs defaultActiveKey="questions" items={[
        {
          key: 'questions',
          label: (<span><QuestionCircleOutlined />问题集</span>),
          children: (
            <List
              loading={coordinationLoading}
              dataSource={questions}
              renderItem={(item) => (
                <Card size="small" className={`coordination-card ${item.resolved ? 'resolved' : 'active'}`} style={{ marginBottom: 12 }}>
                  <div className="coord-header">
                    <Space>
                      <Badge status={item.resolved ? 'success' : 'processing'} />
                      <Text strong>问题集 {item.code.substring(0, 8)}</Text>
                      {item.resolved ? <Tag color="success">已解决</Tag> : <Tag color="warning">待回答</Tag>}
                    </Space>
                    <Text type="secondary" style={{ fontSize: 12 }}>{dayjs(item.openedAt).format('YYYY-MM-DD HH:mm:ss')}</Text>
                  </div>
                  <div className="coord-body">
                    {item.questions.map((q) => (
                      <div key={q.code} className="question-item">
                        <Text strong>Q: {q.text}</Text>
                        {!item.resolved && (
                          <div className="answer-box" style={{ marginTop: 8 }}>
                            {q.options && q.options.length > 0 && (
                              <Radio.Group
                                style={{ marginBottom: 8 }}
                                onChange={(e) => handleResolveQuestion(item.code, [{ questionCode: q.code, selectedOption: e.target.value }])}
                                optionType="button"
                                buttonStyle="solid"
                              >
                                {q.options.map((opt) => (
                                  <Radio.Button key={opt} value={opt}>{opt}</Radio.Button>
                                ))}
                              </Radio.Group>
                            )}
                            <Input.Search
                              placeholder="输入你的回答..."
                              enterButton="提交回答"
                              onSearch={(val) => handleResolveQuestion(item.code, [{ questionCode: q.code, userInput: val }])}
                            />
                          </div>
                        )}
                        {item.resolved && (
                          <div className="resolved-answer" style={{ marginTop: 4 }}>
                            <Text type="secondary">A: {item.answers.find(a => a.questionCode === q.code)?.selectedOption || item.answers.find(a => a.questionCode === q.code)?.userInput || '-'}</Text>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </Card>
              )}
            />
          )
        },
        {
          key: 'instructions',
          label: (<span><HistoryOutlined />指令记录</span>),
          children: (
            <List
              loading={coordinationLoading}
              dataSource={instructions}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={<Badge status="default" />}
                    title={item.content}
                    description={`发送时间: ${item.createdAt} | TurnCode: ${item.turnCode}`}
                  />
                  <Tag>{item.status}</Tag>
                </List.Item>
              )}
            />
          )
        }
      ]} />
    </div>
  );

  const renderWorkspace = () => {
    if (!task) return null;
    return (
      <div className="tab-pane workspace-pane">
        <AgentFileWorkspace 
          agentCode={task.agentCode}
          rootPath={`home/workspace/${code}`}
          title="任务工作空间"
          height="550px"
        />
      </div>
    );
  };

  const renderInfo = () => (
    <div className="tab-pane info-pane">
      <Card title="任务需求详情" bordered={false} className="info-card">
        <Paragraph>
          <pre style={{ whiteSpace: 'pre-wrap', background: '#f9fafb', padding: 16, borderRadius: 8 }}>
            {task?.content || '无详细描述'}
          </pre>
        </Paragraph>
        <div className="meta-info">
          <Space direction="vertical" style={{ width: '100%' }}>
            <div className="meta-row"><label>执行 Agent:</label> <Text>{agentName || task?.agentCode}</Text></div>
            <div className="meta-row"><label>创建来源:</label> <Text>{task?.source ? (sourceLabelMap[task.source] || task.source) : '-'}</Text></div>
            <div className="meta-row"><label>完成时间:</label> <Text>{task?.finishedAt ? dayjs(task.finishedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}</Text></div>
          </Space>
        </div>
      </Card>
    </div>
  );

  return (
    <div className="task-detail-page">
      <div className="detail-header">
        <div className="header-top">
          <Space size={16}>
            <Button 
              type="text" 
              icon={<ArrowLeftOutlined />} 
              onClick={() => history.push('/tasks')} 
            />
            <Title level={4} style={{ margin: 0 }}>{task?.title || '任务详情'}</Title>
            {task && (
              <Tag color={statusColorMap[task.status]} style={{ borderRadius: 12, padding: '0 12px' }}>
                {statusLabelMap[task.status]}
              </Tag>
            )}
          </Space>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={handleRefresh} loading={loading}>刷新页面</Button>
            {task?.status === 'RUNNING' && (
              <Popconfirm title="确定要取消此任务吗？" onConfirm={handleCancel}>
                <Button danger icon={<StopOutlined />}>取消任务</Button>
              </Popconfirm>
            )}
            {(task?.status === 'FAILED' || task?.status === 'CANCELLED') && (
              <Button type="primary" icon={<PlayCircleOutlined />} onClick={handleRetry}>重新尝试</Button>
            )}
            {task?.status === 'SUCCEEDED' && (
              <Popconfirm title="确定要重新运行此任务吗？" onConfirm={handleRollback}>
                <Button icon={<PlayCircleOutlined />}>重新运行</Button>
              </Popconfirm>
            )}

          </Space>
        </div>
        
        {task && (
          <div className="header-info">
            <div className="info-item">
              <Text type="secondary">任务编码</Text>
              <Text copyable strong>{task.taskCode}</Text>
            </div>
            <div className="info-item">
              <Text type="secondary">总体进度</Text>
              <div style={{ width: 200 }}>
                <Progress percent={task.progress} size="small" status={task.status === 'FAILED' ? 'exception' : 'active'} />
              </div>
            </div>
            {task.resultSummary && (
              <div className="info-item result-item">
                <Tooltip title={task.resultSummary}>
                  <Text type="secondary"><InfoCircleOutlined /> 结果摘要: </Text>
                  <Text ellipsis style={{ maxWidth: 300 }}>{task.resultSummary}</Text>
                </Tooltip>
              </div>
            )}
          </div>
        )}
      </div>

      <div className="detail-main">
        {loading ? (
          <div className="detail-loading"><Spin size="large" /></div>
        ) : (
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            className="detail-tabs"
            items={[
              { key: 'info', label: (<span><InfoCircleOutlined /> 基本信息</span>), children: renderInfo() },
              { key: 'execution', label: (<span><HistoryOutlined /> 执行历史</span>), children: renderExecution() },
              { key: 'process', label: (<span><MessageOutlined /> 过程消息</span>), children: renderProcess() },
              { key: 'coordination', label: (<span><CommentOutlined /> 协同中心</span>), children: renderCoordination() },
              { key: 'workspace', label: (<span><FolderOpenOutlined /> 工作空间</span>), children: renderWorkspace() },
            ]}
          />
        )}
      </div>
    </div>
  );
}
