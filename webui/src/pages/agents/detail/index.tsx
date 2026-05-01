import { useState, useEffect, useCallback } from 'react';
import {
  Button, Input, Badge, message, Typography, Space, Spin, Tag, Form, Popconfirm
} from 'antd';
import {
  ArrowLeftOutlined, SaveOutlined,
  EditOutlined, CloseOutlined, CloudUploadOutlined, CloudDownloadOutlined
} from '@ant-design/icons';
import { useParams, history } from '@umijs/max';
import { getAgentDetail, updateAgent, enableAgent, disableAgent } from '@/services/agent/AgentController';
import { backupAgent, restoreAgent } from '@/services/agent/CloudFileController';
import { listConfigs } from '@/services/provider/ProviderController';
import type { AgentDetailResponse } from '@/services/agent/typings';
import type { ConfigKeyDescriptor } from '@/services/provider/typings';
import AgentForm from '../components/AgentForm';
import AgentFileWorkspace from '@/components/AgentFileWorkspace';
import './style.less';

const { Text, Title } = Typography;

export default function AgentDetailPage() {
  const { code } = useParams<{ code: string }>();
  const [agent, setAgent] = useState<AgentDetailResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const [editMode, setEditMode] = useState(false);
  const [descriptors, setDescriptors] = useState<Record<string, ConfigKeyDescriptor[]>>({});
  const [basicForm] = Form.useForm();

  const [backupLoading, setBackupLoading] = useState(false);
  const [restoreLoading, setRestoreLoading] = useState(false);

  const fetchAgent = useCallback(async () => {
    if (!code) return;
    setLoading(true);
    try {
      const res = await getAgentDetail(code);
      setAgent(res.data);
    } finally {
      setLoading(false);
    }
  }, [code]);

  useEffect(() => {
    fetchAgent();
    
    // 获取配置描述符
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
  }, [fetchAgent]);

  const handleCancelEdit = () => {
    basicForm.resetFields(); // 重置表单到初始状态
    setEditMode(false);
  };

  const handleSaveBasicInfo = async () => {
    if (!code || !agent) return;
    try {
      const values = await basicForm.validateFields();
      const { providerConfigRaw, providerConfig, ...rest } = values;
      
      let finalConfig = providerConfig || {};
      
      // 如果存在 providerConfigRaw (JSON 模式)，优先使用它
      if (providerConfigRaw) {
        try {
          finalConfig = JSON.parse(providerConfigRaw);
        } catch {
          message.error('高级配置 JSON 格式不正确');
          return;
        }
      }

      const payload = {
        ...rest,
        providerConfig: finalConfig,
      };

      await updateAgent(code, payload);
      message.success('基本信息已保存');
      setEditMode(false);
      fetchAgent();
    } catch {
      // handled by form validation or interceptor
    }
  };

  const handleToggleStatus = async () => {
    if (!code) return;
    try {
      if (agent?.status === 'ENABLED') {
        await disableAgent(code);
        message.success('Agent 已休眠');
      } else {
        await enableAgent(code);
        message.success('Agent 已唤醒');
      }
      fetchAgent();
    } catch {
      // handled by interceptor
    }
  };

  const handleBackup = async () => {
    if (!code) return;
    setBackupLoading(true);
    try {
      const res = await backupAgent(code);
      const { backedUpCount, skippedCount, failedCount } = res.data;
      message.success(
        `同步完成：已上传 ${backedUpCount} 个文件，跳过 ${skippedCount} 个${failedCount > 0 ? `，失败 ${failedCount} 个` : ''}`
      );
    } finally {
      setBackupLoading(false);
    }
  };

  const handleRestore = async () => {
    if (!code) return;
    setRestoreLoading(true);
    try {
      const res = await restoreAgent(code);
      message.success(`已从云端恢复 ${res.data.restoredCount} 个文件`);
    } finally {
      setRestoreLoading(false);
    }
  };

  if (!code) {
    return <div style={{ padding: 40 }}>无效的 Agent 编码</div>;
  }

  return (
    <div className="agent-detail-page">
      <div className="detail-header">
        <div className="detail-header-left">
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => history.push('/agents')}
            style={{ marginRight: 8, fontSize: 16 }}
          />
          <Title level={4} style={{ margin: 0 }}>
            {agent?.name || '加载中...'}
          </Title>
          {agent && (
            <Badge
              status={agent.status === 'ENABLED' ? 'success' : 'default'}
              text={agent.status === 'ENABLED' ? '运行中' : '已休眠'}
              style={{ marginLeft: 12 }}
            />
          )}
        </div>
        <div className="detail-header-right">
          <Space>
            {agent && (
              <Button onClick={handleToggleStatus}>
                {agent.status === 'ENABLED' ? '休眠' : '唤醒'}
              </Button>
            )}
            <Popconfirm
              title="同步到云端"
              description="将本地文件系统中变更过的文件上传到云端备份"
              onConfirm={handleBackup}
              okText="确认同步"
              cancelText="取消"
            >
              <Button loading={backupLoading} icon={<CloudUploadOutlined />}>
                同步到云端
              </Button>
            </Popconfirm>
            <Popconfirm
              title="从云端恢复"
              description="从 OSS 备份拉取文件到本地，会覆盖当前本地文件。确认继续？"
              onConfirm={handleRestore}
              okText="确认恢复"
              cancelText="取消"
            >
              <Button loading={restoreLoading} danger icon={<CloudDownloadOutlined />}>
                从云端恢复
              </Button>
            </Popconfirm>
          </Space>
        </div>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>}

      {agent && !loading && (
        <div className="detail-body">
          <div className="detail-left">
            <div className="detail-section basic-info-card">
              <div className="basic-info-header">
                <Title level={5} style={{ margin: 0 }}>基本信息</Title>
                {!editMode ? (
                  <Button type="primary" ghost icon={<EditOutlined />} onClick={() => {
                    if (agent) {
                      basicForm.setFieldsValue({
                        name: agent.name,
                        responsibility: agent.responsibility,
                        provider: agent.provider,
                        providerConfig: agent.providerConfig || {},
                        providerConfigRaw: agent.providerConfig ? JSON.stringify(agent.providerConfig, null, 2) : undefined
                      });
                    }
                    setEditMode(true);
                  }}>
                    编辑
                  </Button>
                ) : (
                  <Space>
                    <Button icon={<CloseOutlined />} onClick={handleCancelEdit}>取消</Button>
                    <Button type="primary" icon={<SaveOutlined />} onClick={handleSaveBasicInfo}>保存</Button>
                  </Space>
                )}
              </div>

              {!editMode && (
                <div className="basic-info-display">
                  <div className="info-row">
                    <label>名称</label>
                    <Text>{agent.name}</Text>
                  </div>
                  <div className="info-row">
                    <label>编码</label>
                    <Text className="readonly-value">{agent.code}</Text>
                  </div>
                  <div className="info-row">
                    <label>职责分类</label>
                    <Text>{agent.responsibility || '未配置'}</Text>
                  </div>
                  <div className="info-row">
                    <label>模型提供方</label>
                    <Text>{agent.provider}</Text>
                  </div>
                  <div className="info-row">
                    <label>高级配置</label>
                    <pre className="readonly-json">
                      {agent.providerConfig ? JSON.stringify(agent.providerConfig, null, 2) : '未配置'}
                    </pre>
                  </div>
                </div>
              )}

              {editMode && (
                <div className="basic-info-form">
                  <div className="detail-field" style={{ marginBottom: 24 }}>
                    <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>编码</label>
                    <div style={{ padding: '8px 12px', background: '#f5f5f5', borderRadius: 8, border: '1px solid #d9d9d9', fontFamily: 'monospace' }}>
                      {agent.code}
                    </div>
                  </div>
                  <AgentForm 
                    form={basicForm} 
                    mode="edit" 
                    descriptors={descriptors} 
                    initialValues={{
                      ...agent,
                      providerConfigRaw: agent.providerConfig ? JSON.stringify(agent.providerConfig, null, 2) : undefined
                    }}
                  />
                </div>
              )}
            </div>
          </div>

          <div className="detail-right">
            <AgentFileWorkspace
              agentCode={code}
              rootPath="home"
              allowCreate
              allowEdit
              allowSave
              title="Agent Profile 文件"
              height="calc(100vh - 200px)"
            />
          </div>
        </div>
      )}
    </div>
  );
}
