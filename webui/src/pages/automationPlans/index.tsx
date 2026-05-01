import { useState, useEffect } from 'react';
import {
  Button, Table, Tag, Modal, Form, Input, Select, Radio, message, Typography, Card, Space, Tooltip, Popconfirm, ConfigProvider
} from 'antd';
import {
  PlusOutlined, DeleteOutlined,
  PlayCircleOutlined, StopOutlined, EyeOutlined
} from '@ant-design/icons';
import type { TablePaginationConfig } from 'antd';
import { listPlans, createPlan, deletePlan, enablePlan, disablePlan } from '@/services/automationPlan/AutomationPlanController';
import type { AutomationPlanListItemDTO, AutomationPlanCreateRequest } from '@/services/automationPlan/typings';
import { listAgents } from '@/services/agent/AgentController';
import type { AgentDetailResponse } from '@/services/agent/typings';
import { history } from '@umijs/max';
import CronBuilder from './components/CronBuilder';

const { Text } = Typography;
const { TextArea } = Input;

const statusColorMap: Record<string, string> = {
  ENABLED: 'green',
  DISABLED: 'default',
};

const statusLabelMap: Record<string, string> = {
  ENABLED: '已启用',
  DISABLED: '已禁用',
};

const deliveryModeLabelMap: Record<string, string> = {
  PUSH: '推送',
  CONVERSATION: '对话',
};

export default function AutomationPlansPage() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<AutomationPlanListItemDTO[]>([]);
  const [total, setTotal] = useState(0);
  const [pagination, setPagination] = useState({ page: 0, size: 10 });
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [agents, setAgents] = useState<AgentDetailResponse[]>([]);
  const [form] = Form.useForm();

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await listPlans(pagination);
      setData(res.data || []);
      setTotal(res.total || 0);
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  };

  const fetchAgents = async () => {
    try {
      const res = await listAgents({ page: 0, size: 100 });
      setAgents(res.data || []);
    } catch {
      // handled by interceptor
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

  const openCreateModal = () => {
    form.resetFields();
    setCreateModalOpen(true);
  };

  const handleCreate = async () => {
    const values = await form.validateFields();
    try {
      await createPlan(values as AutomationPlanCreateRequest);
      message.success('创建成功');
      setCreateModalOpen(false);
      fetchData();
    } catch {
      // handled by interceptor
    }
  };

  const handleToggleStatus = async (record: AutomationPlanListItemDTO) => {
    try {
      if (record.status === 'ENABLED') {
        await disablePlan(record.planCode);
        message.success('已禁用');
      } else {
        await enablePlan(record.planCode);
        message.success('已启用');
      }
      fetchData();
    } catch {
      // handled by interceptor
    }
  };

  const handleDelete = async (planCode: string) => {
    try {
      await deletePlan(planCode);
      message.success('已删除');
      fetchData();
    } catch {
      // handled by interceptor
    }
  };

  const columns = [
    {
      title: '计划编码',
      dataIndex: 'planCode',
      width: 180,
      ellipsis: true,
      render: (t: string) => <Text type="secondary" copyable>{t}</Text>,
    },
    {
      title: '计划名称',
      dataIndex: 'name',
      ellipsis: true,
      render: (t: string) => <Text strong>{t}</Text>,
    },
    {
      title: '关联 Agent',
      dataIndex: 'agentCode',
      width: 150,
      render: (code: string) => {
        const agent = agents.find(a => a.code === code);
        return agent ? <Tag color="blue">{agent.name}</Tag> : <Text type="secondary">{code}</Text>;
      },
    },
    {
      title: '调度规则',
      dataIndex: 'scheduleRule',
      width: 160,
      render: (t: string) => <Text code style={{ fontSize: 12 }}>{t}</Text>,
    },
    {
      title: '交付方式',
      dataIndex: 'deliveryMode',
      width: 100,
      render: (mode: string) => <Tag>{deliveryModeLabelMap[mode] || mode}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={statusColorMap[status]} style={{ width: '60px', textAlign: 'center' }}>
          {statusLabelMap[status]}
        </Tag>
      ),
    },
    {
      title: '操作',
      width: 180,
      render: (_: any, record: AutomationPlanListItemDTO) => (
        <Space>
          <Tooltip title="查看详情">
            <Button type="text" icon={<EyeOutlined />} onClick={() => history.push(`/autoplans/${record.planCode}`)} />
          </Tooltip>
          <Tooltip title={record.status === 'ENABLED' ? '禁用' : '启用'}>
            <Button
              type="text"
              icon={record.status === 'ENABLED' ? <StopOutlined /> : <PlayCircleOutlined />}
              onClick={() => handleToggleStatus(record)}
            />
          </Tooltip>
          <Popconfirm title="确定删除此计划？" onConfirm={() => handleDelete(record.planCode)}>
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
          <Typography.Title level={2} style={{ margin: 0, fontWeight: 700, letterSpacing: '-0.5px' }}>Automation Plans</Typography.Title>
          <Text type="secondary" style={{ fontSize: 16 }}>管理定时执行的自动化任务计划</Text>
        </div>
        <Button type="primary" size="large" icon={<PlusOutlined />} onClick={openCreateModal} style={{ borderRadius: 12, padding: '0 24px' }}>
          New Plan
        </Button>
      </div>

      <Card bordered={false} bodyStyle={{ padding: 0 }} style={{ borderRadius: 12, overflow: 'hidden' }}>
        <Table
          rowKey="planCode"
          columns={columns}
          dataSource={data}
          loading={loading}
          pagination={{
            current: pagination.page + 1,
            pageSize: pagination.size,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条计划`,
            style: { padding: '16px 24px' },
          }}
          onChange={handleTableChange}
        />
      </Card>

      <Modal
        title="新建自动化计划"
        open={createModalOpen}
        onOk={handleCreate}
        onCancel={() => setCreateModalOpen(false)}
        width={560}
        destroyOnClose
        styles={{
          body: { padding: 32 },
          header: { marginBottom: 24 },
        }}
        okButtonProps={{ style: { borderRadius: 8 } }}
        cancelButtonProps={{ style: { borderRadius: 8 } }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label={<Text strong>计划名称</Text>} rules={[{ required: true, message: '请输入计划名称' }]}>
            <Input size="large" style={{ borderRadius: 8 }} placeholder="例如：每日晨报" />
          </Form.Item>

          <ConfigProvider
            theme={{
              components: {
                Select: {
                  optionSelectedBg: 'rgba(0,0,0,0.04)',
                  optionActiveBg: 'rgba(0,0,0,0.02)',
                },
              },
            }}
          >
            <Form.Item name="agentCode" label={<Text strong>关联 Agent</Text>} rules={[{ required: true, message: '请选择 Agent' }]}>
              <Select
                size="large"
                placeholder="选择执行此计划的 Agent"
                options={agents.map(a => ({ label: a.name, value: a.code }))}
                style={{ borderRadius: 8 }}
              />
            </Form.Item>
          </ConfigProvider>

          <Form.Item name="instruction" label={<Text strong>任务指令</Text>} rules={[{ required: true, message: '请输入任务指令' }]}>
            <TextArea rows={3} size="large" style={{ borderRadius: 8 }} placeholder="描述该计划被执行时应当做什么..." />
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
