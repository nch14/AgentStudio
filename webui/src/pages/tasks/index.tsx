import { useState, useEffect } from 'react';
import { 
  Button, Table, Tag, Drawer, Space, Descriptions, 
  message, Form, Input, Select, Modal, Popconfirm, 
  Card, Steps, Typography, Tooltip, Progress, ConfigProvider 
} from 'antd';
import { 
  PlusOutlined, EyeOutlined, DeleteOutlined, 
  PlayCircleOutlined, StopOutlined, UnorderedListOutlined 
} from '@ant-design/icons';
import type { TablePaginationConfig } from 'antd';
import { listTasks, createTask, deleteTask, retryTask, cancelTask } from '@/services/task/TaskController';
import type { TaskListItemResponse, TaskDetailResponse } from '@/services/task/typings';
import { listAgents } from '@/services/agent/AgentController';
import type { AgentDetailResponse } from '@/services/agent/typings';
import { history } from '@umijs/max';

const { Text } = Typography;

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

export default function TasksPage() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<TaskListItemResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [pagination, setPagination] = useState({ page: 0, size: 10 });
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [agents, setAgents] = useState<AgentDetailResponse[]>([]);
  const [createForm] = Form.useForm();

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await listTasks({ ...pagination, source: 'USER_CREATE' });
      setData(res.data || []);
      setTotal(res.total || 0);
    } catch (e) {
      // error handled by interceptor
    } finally {
      setLoading(false);
    }
  };

  const fetchAgents = async () => {
    try {
      const res = await listAgents({ page: 0, size: 100 });
      setAgents(res.data || []);
    } catch (e) {
      // error handled by interceptor
    }
  };

  useEffect(() => {
    fetchData();
    fetchAgents();
  }, [pagination]);

  const handleTableChange = (pag: TablePaginationConfig) => {
    setPagination({
      page: (pag.current || 1) - 1,
      size: pag.pageSize || 10,
    });
  };

  const handleViewDetail = (record: TaskListItemResponse) => {
    history.push(`/tasks/${record.taskCode}`);
  };

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      await createTask(values);
      message.success('创建成功');
      setCreateModalOpen(false);
      createForm.resetFields();
      fetchData();
    } catch (e) {
      // error handled by interceptor
    }
  };

  const handleDelete = async (taskCode: string) => {
    try {
      await deleteTask(taskCode);
      message.success('已删除');
      fetchData();
    } catch (e) {
      // error handled by interceptor
    }
  };

  const handleRetry = async (taskCode: string) => {
    try {
      await retryTask(taskCode);
      message.success('已触发重试');
      fetchData();
    } catch (e) {
      // error handled by interceptor
    }
  };

  const handleCancel = async (taskCode: string) => {
    try {
      await cancelTask(taskCode);
      message.success('已取消');
      fetchData();
    } catch (e) {
      // error handled by interceptor
    }
  };

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

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ marginBottom: 32, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Typography.Title level={2} style={{ margin: 0, fontWeight: 700, letterSpacing: '-0.5px' }}>Tasks</Typography.Title>
          <Text type="secondary" style={{ fontSize: 16 }}>管理和监控由 Agent 执行的异步任务链</Text>
        </div>
        <Button type="primary" size="large" icon={<PlusOutlined />} onClick={() => setCreateModalOpen(true)} style={{ borderRadius: 12, padding: '0 24px' }}>
          New Task
        </Button>
      </div>

      <Card bordered={false} bodyStyle={{ padding: 0 }} style={{ borderRadius: 12, overflow: 'hidden' }}>
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
      </Card>

      <Modal
        title="新建任务"
        open={createModalOpen}
        onOk={handleCreate}
        onCancel={() => setCreateModalOpen(false)}
        width={500}
        destroyOnClose
        styles={{
          root: { borderRadius: 24 },
          body: { padding: 32 },
          header: { marginBottom: 24 }
        }}
        okButtonProps={{ style: { borderRadius: 8 } }}
        cancelButtonProps={{ style: { borderRadius: 8 } }}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="title" label={<Text strong>任务标题</Text>} rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="输入任务名称" size="large" style={{ borderRadius: 8 }} />
          </Form.Item>
          <ConfigProvider
            theme={{
              components: {
                Select: {
                  optionSelectedBg: 'rgba(0,0,0,0.04)',
                  optionActiveBg: 'rgba(0,0,0,0.02)',
                }
              }
            }}
          >
            <Form.Item name="agentCode" label={<Text strong>负责 Agent</Text>} rules={[{ required: true, message: '请选择 Agent' }]}>
              <Select 
                placeholder="选择一个 Agent 来处理本任务" 
                size="large" 
                options={agents.map((a) => ({ label: a.name, value: a.code }))} 
                style={{ borderRadius: 8 }}
              />
            </Form.Item>
          </ConfigProvider>
          <Form.Item name="description" label={<Text strong>详细描述</Text>}>
            <Input.TextArea rows={3} placeholder="提供任务被执行时的背景信息和要求" size="large" style={{ borderRadius: 8 }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
