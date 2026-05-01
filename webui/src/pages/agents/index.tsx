import { useState, useEffect } from 'react';
import {
  Button, List, Modal, Form, Input,
  message, Typography, Badge, Space, Tooltip
} from 'antd';
import {
  PlusOutlined,
  ApiOutlined
} from '@ant-design/icons';
import { listAgents, createAgent } from '@/services/agent/AgentController';
import type { AgentDetailResponse, AgentCreateRequest } from '@/services/agent/typings';
import { listConfigs } from '@/services/provider/ProviderController';
import type { ConfigKeyDescriptor } from '@/services/provider/typings';
import AgentForm from './components/AgentForm';
import { history } from '@umijs/max';
import './index.less';

const { TextArea } = Input;
const { Text, Paragraph, Title } = Typography;

export default function AgentsPage() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<AgentDetailResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [pagination, setPagination] = useState({ page: 0, size: 12 });
  const [modalOpen, setModalOpen] = useState(false);
  const [descriptors, setDescriptors] = useState<Record<string, ConfigKeyDescriptor[]>>({});
  const [form] = Form.useForm();

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await listAgents(pagination);
      setData(res.data || []);
      setTotal(res.total || 0);
    } catch (e) {
      // error handled by interceptor
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [pagination]);

  useEffect(() => {
    const fetchDescriptors = async () => {
      try {
        const res = await listConfigs();
        const grouped = (res.data || []).reduce((acc, curr) => {
          if (!acc[curr.provider]) acc[curr.provider] = [];
          acc[curr.provider].push(curr);
          return acc;
        }, {} as Record<string, ConfigKeyDescriptor[]>);
        setDescriptors(grouped);
      } catch (e) {
        // handled
      }
    };
    fetchDescriptors();
  }, []);

  const handlePageChange = (page: number, pageSize: number) => {
    setPagination({
      page: page - 1,
      size: pageSize,
    });
  };

  const openCreateModal = () => {
    form.resetFields();
    setModalOpen(true);
  };

  const handleModalOk = async () => {
    const values = await form.validateFields();
    const { providerConfigRaw, providerConfig, ...rest } = values;
    
    let finalConfig = providerConfig || {};
    
    // 如果存在 providerConfigRaw (JSON 模式)，优先使用它
    if (providerConfigRaw) {
      try {
        finalConfig = JSON.parse(providerConfigRaw);
      } catch {
        message.error('提供者配置必须是合法的 JSON');
        return;
      }
    }

    const payload = {
      ...rest,
      providerConfig: finalConfig,
    };

    try {
      await createAgent(payload as AgentCreateRequest);
      message.success('创建成功');
      setModalOpen(false);
      fetchData();
    } catch (e) {
      // error handled by interceptor
    }
  };

  // 生成一个基于名字的渐变色背景（Ultra-modern 风格）
  const getGradientForName = (name: string) => {
    const gradients = [
      'linear-gradient(135deg, #a8c0ff 0%, #3f2b96 100%)',
      'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
      'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)',
      'linear-gradient(135deg, #c471f5 0%, #fa71cd 100%)'
    ];
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return gradients[Math.abs(hash) % gradients.length];
  };

  return (
    <div className="agents-page" style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ marginBottom: 32, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2} style={{ margin: 0, fontWeight: 700, letterSpacing: '-0.5px' }}>Agents</Title>
          <Text type="secondary" style={{ fontSize: 16 }}>配置并管理各种构造方案的 AI 助理</Text>
        </div>
        <Button type="primary" size="large" icon={<PlusOutlined />} onClick={openCreateModal} style={{ borderRadius: 12, padding: '0 24px' }}>
          New Agent
        </Button>
      </div>
      
      <div style={{ flex: 1, overflow: 'auto', paddingBottom: 40 }}>
        <List
          grid={{ gutter: 24, xxl: 4, xl: 4, lg: 3, md: 2, sm: 1, xs: 1 }}
          dataSource={data}
          loading={loading}
          pagination={{
            current: pagination.page + 1,
            pageSize: pagination.size,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 个助理`,
            onChange: handlePageChange,
            style: { marginTop: 32, textAlign: 'center' }
          }}
          renderItem={(item) => {
            const isActive = item.status === 'ENABLED';
            
            return (
              <List.Item>
                <div
                  className="modern-agent-card"
                  style={{
                    backgroundColor: '#fff',
                    borderRadius: 20,
                    padding: 24,
                    border: '1px solid #f0f0f0',
                    boxShadow: '0 4px 20px rgba(0,0,0,0.02)',
                    transition: 'all 0.3s ease',
                    position: 'relative',
                    cursor: 'pointer',
                    minHeight: 280,
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column'
                  }}
                  onClick={() => history.push(`/agents/${item.code}`)}
                >
                  {/* Card Header: Avatar & Menu */}
                  <div style={{ display: 'flex', alignItems: 'flex-start', marginBottom: 20 }}>
                    <div
                      style={{ 
                        width: 48, 
                        height: 48, 
                        borderRadius: 16, 
                        background: getGradientForName(item.name),
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#fff',
                        fontSize: 20,
                        fontWeight: 600,
                        boxShadow: '0 8px 16px rgba(0,0,0,0.1)',
                        opacity: isActive ? 1 : 0.6,
                        filter: isActive ? 'none' : 'grayscale(100%)'
                      }}
                    >
                      {item.name.charAt(0).toUpperCase()}
                    </div>
                  </div>
                  
                  {/* Card Body: Info */}
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                      <Title level={4} style={{ margin: 0, fontWeight: 600 }}>{item.name}</Title>
                      <Tooltip title={isActive ? 'Active' : 'Inactive'}>
                        <Badge status={isActive ? 'success' : 'default'} />
                      </Tooltip>
                    </div>
                    
                    <Paragraph 
                      type="secondary" 
                      ellipsis={{ rows: 2 }} 
                      style={{ fontSize: 14, color: '#64748b', margin: 0, lineHeight: 1.6 }}
                    >
                      {item.responsibility || '未配置任何职责设定'}
                    </Paragraph>
                  </div>
                  
                  <div style={{ marginTop: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ 
                      padding: '4px 12px', 
                      backgroundColor: '#f1f5f9', 
                      borderRadius: 20,
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 6,
                      fontSize: 12,
                      fontWeight: 500,
                      color: '#475569'
                    }}>
                      <ApiOutlined />
                      {item.provider}
                    </div>
                    
                    <Text type="secondary" style={{ fontSize: 12, fontFamily: 'monospace', opacity: 0.5 }} copyable={{ text: item.code }}>
                      {item.code.substring(0, 8)}
                    </Text>
                  </div>
                </div>
              </List.Item>
            );
          }}
        />
      </div>

      <Modal
        title="新建智能助理"
        open={modalOpen}
        onOk={handleModalOk}
        onCancel={() => setModalOpen(false)}
        width={560}
        destroyOnClose
        styles={{
          body: { borderRadius: 24, padding: 32 },
          header: { marginBottom: 24 }
        }}
        okButtonProps={{ style: { borderRadius: 8 } }}
        cancelButtonProps={{ style: { borderRadius: 8 } }}
      >
        <AgentForm
          form={form}
          mode="create"
          descriptors={descriptors}
        />
      </Modal>
    </div>
  );
}
