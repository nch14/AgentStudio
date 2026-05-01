import { useState, useEffect, useCallback } from 'react';
import {
  Button, Table, Tag, Space, Typography, Spin, Card, Progress, Tooltip, ConfigProvider,
  Modal, Form, Input, Radio, Popconfirm, message
} from 'antd';
import {
  ArrowLeftOutlined, EyeOutlined, PlayCircleOutlined, StopOutlined, DeleteOutlined,
  ReloadOutlined, ClockCircleOutlined, WarningOutlined, EditOutlined, UnorderedListOutlined,
  RobotOutlined, CheckCircleOutlined, CloseCircleOutlined, SyncOutlined
} from '@ant-design/icons';
import { useParams, history } from '@umijs/max';
import { listTasks, cancelTask, retryTask, deleteTask } from '@/services/task/TaskController';
import { getPlanDetail, updatePlan } from '@/services/automationPlan/AutomationPlanController';
import { getAgentDetail, listAgents } from '@/services/agent/AgentController';
import type { TaskListItemResponse } from '@/services/task/typings';
import type { AutomationPlanDetailDTO, AutomationPlanUpdateRequest } from '@/services/automationPlan/typings';
import type { TablePaginationConfig } from 'antd';
import type { AgentDetailResponse } from '@/services/agent/typings';
import { getNextCronTime, cronToReadable, formatRelativeTime, truncateMiddle } from '@/utils/cron';
import CronBuilder from '@/pages/automationPlans/components/CronBuilder';
import './style.less';

const { Text, Title } = Typography;
const { TextArea } = Input;

const statusColorMap: Record<string, string> = {
  CREATED: 'default',
  PENDING: 'default',
  RUNNING: 'processing',
  SUCCEEDED: 'success',
  COMPLETED: 'success',
  FAILED: 'error',
  CANCELLED: 'warning',
};

const statusLabelMap: Record<string, string> = {
  CREATED: '已创建',
  PENDING: '待执行',
  RUNNING: '执行中',
  SUCCEEDED: '已成功',
  COMPLETED: '已完成',
  FAILED: '失败',
  CANCELLED: '已取消',
};

export default function AutoPlanDetailPage() {
  const { code } = useParams<{ code: string }>();
  const [plan, setPlan] = useState<AutomationPlanDetailDTO | null>(null);
  const [planLoading, setPlanLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<TaskListItemResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [pagination, setPagination] = useState({ page: 0, size: 10 });
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editForm] = Form.useForm();

  // 执行 Agent 名称
  const [agentName, setAgentName] = useState<string>('');

  // Agent 列表（用于任务表格中显示 agent name）
  const [agents, setAgents] = useState<AgentDetailResponse[]>([]);

  // 任务统计
  const [taskStats, setTaskStats] = useState<{ success: number; cancelled: number; running: number }>({
    success: 0,
    cancelled: 0,
    running: 0,
  });

  const fetchPlan = useCallback(async () => {
    if (!code) return;
    setPlanLoading(true);
    try {
      const res = await getPlanDetail(code);
      setPlan(res.data);
      // 获取 Agent 名称
      if (res.data?.agentCode) {
        getAgentDetail(res.data.agentCode).then(agentRes => {
          setAgentName(agentRes.data?.name || '');
        }).catch(() => {
          // 忽略失败
        });
      }
    } catch {
      // error handled by interceptor
    } finally {
      setPlanLoading(false);
    }
  }, [code]);

  const fetchData = useCallback(async () => {
    if (!code) return;
    setLoading(true);
    try {
      const res = await listTasks({ ...pagination, source: 'SCHEDULED_CREATE', sourceRef: code });
      setData(res.data || []);
      setTotal(res.total || 0);
    } catch {
      // error handled by interceptor
    } finally {
      setLoading(false);
    }
  }, [code, pagination]);

  // 获取各状态任务总数
  const fetchStats = useCallback(async () => {
    if (!code) return;
    try {
      const [successRes, cancelledRes, runningRes] = await Promise.all([
        listTasks({ page: 0, size: 1, source: 'SCHEDULED_CREATE', sourceRef: code, status: 'SUCCEEDED' }),
        listTasks({ page: 0, size: 1, source: 'SCHEDULED_CREATE', sourceRef: code, status: 'CANCELLED' }),
        listTasks({ page: 0, size: 1, source: 'SCHEDULED_CREATE', sourceRef: code, status: 'RUNNING' }),
      ]);
      setTaskStats({
        success: successRes.total || 0,
        cancelled: cancelledRes.total || 0,
        running: runningRes.total || 0,
      });
    } catch {
      // ignore
    }
  }, [code]);

  useEffect(() => {
    fetchPlan();
    fetchData();
    fetchStats();
    fetchAgents();
  }, [fetchPlan, fetchData, fetchStats]);

  const fetchAgents = useCallback(async () => {
    try {
      const res = await listAgents({ page: 0, size: 100 });
      setAgents(res.data || []);
    } catch {
      // handled
    }
  }, []);

  const handleTableChange = (pag: TablePaginationConfig) => {
    setPagination({
      page: (pag.current || 1) - 1,
      size: pag.pageSize || 10,
    });
  };

  const handleViewDetail = (record: TaskListItemResponse) => {
    history.push(`/tasks/${record.taskCode}`);
  };

  const handleRetry = async (taskCode: string) => {
    try {
      await retryTask(taskCode);
      fetchData();
      fetchStats();
    } catch {
      // handled
    }
  };

  const handleCancel = async (taskCode: string) => {
    try {
      await cancelTask(taskCode);
      fetchData();
      fetchStats();
    } catch {
      // handled
    }
  };

  const handleDelete = async (taskCode: string) => {
    try {
      await deleteTask(taskCode);
      fetchData();
      fetchStats();
    } catch {
      // handled
    }
  };

  const handleEdit = () => {
    if (!plan) return;
    editForm.setFieldsValue({
      name: plan.name,
      instruction: plan.instruction,
      scheduleRule: plan.scheduleRule,
      deliveryMode: plan.deliveryMode,
    });
    setEditModalOpen(true);
  };

  const handleSaveEdit = async () => {
    const values = await editForm.validateFields();
    if (!code) return;
    try {
      const payload: AutomationPlanUpdateRequest = {
        name: values.name,
        instruction: values.instruction,
        scheduleRule: values.scheduleRule,
        deliveryMode: values.deliveryMode,
      };
      await updatePlan(code, payload);
      message.success('更新成功');
      setEditModalOpen(false);
      fetchPlan();
      fetchStats();
    } catch {
      // handled
    }
  };

  const nextRunTime = plan?.scheduleRule ? getNextCronTime(plan.scheduleRule) : null;
  const cronReadable = plan?.scheduleRule ? cronToReadable(plan.scheduleRule) : null;

  const columns = [
    {
      title: '任务编码',
      dataIndex: 'taskCode',
      width: 180,
      ellipsis: true,
      render: (t: string) => <Text type="secondary" copyable>{t}</Text>
    },
    {
      title: '任务标题',
      dataIndex: 'title',
      ellipsis: true,
      render: (t: string) => <Text strong>{t}</Text>
    },
    {
      title: '执行 Agent',
      dataIndex: 'agentCode',
      width: 150,
      render: (code: string) => {
        const agent = agents.find(a => a.code === code);
        return agent ? <Tag color="blue">{agent.name}</Tag> : <Text type="secondary">{code}</Text>;
      }
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (status: string) => (
        <Tag color={statusColorMap[status]} style={{ width: '60px', textAlign: 'center' }}>
          {statusLabelMap[status]}
        </Tag>
      ),
    },
    {
      title: '进度',
      dataIndex: 'progress',
      width: 140,
      render: (p: number, record: TaskListItemResponse) => {
        const statusMap = {
          FAILED: 'exception',
          SUCCEEDED: 'success',
          COMPLETED: 'success',
          RUNNING: 'active',
          CREATED: 'normal',
          PENDING: 'normal',
          CANCELLED: 'normal'
        } as any;
        return <Progress percent={p} size="small" status={statusMap[record.status] || 'normal'} />;
      },
    },
    {
      title: '操作',
      width: 220,
      render: (_: any, record: TaskListItemResponse) => (
        <Space>
          <Tooltip title="查看详情">
            <Button type="text" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)} />
          </Tooltip>
          {(record.status === 'FAILED' || record.status === 'CANCELLED') && (
            <Tooltip title="重新执行">
              <Button type="text" icon={<PlayCircleOutlined />} onClick={() => handleRetry(record.taskCode)} />
            </Tooltip>
          )}
          {record.status === 'RUNNING' && (
            <Popconfirm title="确定取消此任务？" onConfirm={() => handleCancel(record.taskCode)}>
              <Tooltip title="取消任务">
                <Button type="text" danger icon={<StopOutlined />} />
              </Tooltip>
            </Popconfirm>
          )}
          <Popconfirm title="确定删除此任务？" onConfirm={() => handleDelete(record.taskCode)}>
            <Tooltip title="删除">
              <Button type="text" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  if (!code) {
    return <div style={{ padding: 40 }}>无效的 Plan 编码</div>;
  }

  return (
    <div className="autoplan-detail-page">
      {/* Header */}
      <div className="detail-header">
        <div className="header-top">
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => history.push('/automation-plans')}
            style={{ marginRight: 8, fontSize: 16 }}
          />
          <Title level={4} style={{ margin: 0 }}>{plan?.name || 'AutoPlan 详情'}</Title>
        </div>
        <div className="header-actions">
          <Button icon={<EditOutlined />} onClick={handleEdit}>编辑</Button>
          <Button icon={<ReloadOutlined />} onClick={() => { fetchPlan(); fetchData(); fetchStats(); }} loading={planLoading || loading}>刷新</Button>
        </div>
      </div>

      {/* Body: two columns */}
      <div className="detail-body">
        <div className="detail-left">
          {/* Plan instruction card */}
          <Card title="任务指令" bordered={false} className="info-card">
            <pre className="instruction-content">{plan?.instruction || '暂无指令'}</pre>
          </Card>

          {/* Plan meta card */}
          <Card title="计划信息" bordered={false} className="info-card" style={{ marginTop: 16 }}>
            <div className="meta-grid">
              <div className="meta-item">
                <Text type="secondary">Plan 编码</Text>
                <Tooltip title={code}>
                  <Text copyable strong style={{ fontSize: 13 }}>{truncateMiddle(code)}</Text>
                </Tooltip>
              </div>
              <div className="meta-item">
                <Text type="secondary">执行 Agent</Text>
                <Space>
                  <RobotOutlined style={{ color: '#6366f1' }} />
                  {agentName ? (
                    <Tooltip title={plan?.agentCode}>
                      <Text strong>{agentName}</Text>
                    </Tooltip>
                  ) : (
                    <Text strong>{plan?.agentCode}</Text>
                  )}
                </Space>
              </div>
              <div className="meta-item">
                <Text type="secondary">调度规则</Text>
                <Space>
                  <Text code>{plan?.scheduleRule || '—'}</Text>
                  {cronReadable && <Tag color="purple" style={{ margin: 0 }}>{cronReadable}</Tag>}
                </Space>
              </div>
              <div className="meta-item">
                <Text type="secondary">下次执行</Text>
                {nextRunTime ? (
                  <Space>
                    <ClockCircleOutlined style={{ color: '#6366f1' }} />
                    <Space direction="vertical" size={2}>
                      <Text strong style={{ fontSize: 13 }}>{formatRelativeTime(nextRunTime)}</Text>
                      <Text type="secondary" style={{ fontSize: 12 }}>{new Date(nextRunTime).toLocaleString('zh-CN')}</Text>
                    </Space>
                  </Space>
                ) : plan?.scheduleRule ? (
                  <Space><WarningOutlined style={{ color: '#94a3b8' }} /><Text type="secondary" style={{ fontSize: 13 }}>无法解析</Text></Space>
                ) : (
                  <Text type="secondary" style={{ fontSize: 13 }}>—</Text>
                )}
              </div>
            </div>
          </Card>
        </div>

        {/* Right: task stats */}
        <div className="detail-right">
          <Card bordered={false} className="stats-card">
            <div className="stats-main">
              <div className="stats-icon"><UnorderedListOutlined /></div>
              <div className="stats-value">{total}</div>
              <div className="stats-label">任务总数</div>
            </div>
            <div className="stats-divider" />
            <div className="stats-sub">
              <div className="stat-row">
                <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 16 }} />
                <span className="stat-label">成功</span>
                <span className="stat-value">{taskStats.success}</span>
              </div>
              <div className="stat-row">
                <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: 16 }} />
                <span className="stat-label">已取消</span>
                <span className="stat-value">{taskStats.cancelled}</span>
              </div>
              <div className="stat-row">
                <SyncOutlined spin={taskStats.running > 0} style={{ color: '#1890ff', fontSize: 16 }} />
                <span className="stat-label">执行中</span>
                <span className="stat-value">{taskStats.running}</span>
              </div>
            </div>
          </Card>
        </div>
      </div>

      {/* Task table */}
      <div className="task-section">
        <Card bordered={false} bodyStyle={{ padding: 0 }} style={{ borderRadius: 12, overflow: 'hidden' }}>
          <ConfigProvider
            theme={{
              components: {
                Table: {
                  headerBg: '#f8fafc',
                  headerColor: '#475569',
                  rowHoverBg: '#f8fafc',
                }
              }
            }}
          >
            <Table
              rowKey="taskCode"
              columns={columns}
              dataSource={data}
              loading={loading}
              pagination={{
                current: pagination.page + 1,
                pageSize: pagination.size,
                total,
                showSizeChanger: true,
                showTotal: (t) => `共 ${t} 条任务记录`,
                style: { padding: '16px 24px' }
              }}
              onChange={handleTableChange}
            />
          </ConfigProvider>
        </Card>
      </div>

      {/* Edit Modal */}
      <Modal
        title="编辑自动化计划"
        open={editModalOpen}
        onOk={handleSaveEdit}
        onCancel={() => setEditModalOpen(false)}
        width={560}
        destroyOnClose
        styles={{
          content: { borderRadius: 24, padding: 32 },
          header: { marginBottom: 24 },
        }}
        okButtonProps={{ style: { borderRadius: 8 } }}
        cancelButtonProps={{ style: { borderRadius: 8 } }}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item name="name" label={<Text strong>计划名称</Text>} rules={[{ required: true, message: '请输入计划名称' }]}>
            <Input size="large" style={{ borderRadius: 8 }} />
          </Form.Item>
          <Form.Item name="instruction" label={<Text strong>任务指令</Text>} rules={[{ required: true, message: '请输入任务指令' }]}>
            <TextArea rows={3} size="large" style={{ borderRadius: 8 }} />
          </Form.Item>
          <Form.Item name="scheduleRule" label={<Text strong>调度规则</Text>} rules={[{ required: true, message: '请设置调度规则' }]}>
            <CronBuilder />
          </Form.Item>
          <Form.Item name="deliveryMode" label={<Text strong>交付方式</Text>} rules={[{ required: true, message: '请选择交付方式' }]}>
            <Radio.Group>
              <Radio value="PUSH">推送通知</Radio>
              <Radio value="CONVERSATION">对话交互</Radio>
            </Radio.Group>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
