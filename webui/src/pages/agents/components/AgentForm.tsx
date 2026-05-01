import React, { useState, useEffect } from 'react';
import { Form, Input, Select, Typography, ConfigProvider, Switch, Divider, Space, Tooltip } from 'antd';
import { CodeOutlined, SettingOutlined } from '@ant-design/icons';
import { ConfigKeyDescriptor } from '@/services/provider/typings';

const { TextArea } = Input;
const { Text } = Typography;

interface AgentFormProps {
  form: any;
  mode: 'create' | 'edit';
  descriptors: Record<string, ConfigKeyDescriptor[]>;
  initialValues?: any;
}

const AgentForm: React.FC<AgentFormProps> = ({ form, mode, descriptors, initialValues }) => {
  const [provider, setProvider] = useState<string | undefined>(initialValues?.provider);
  const [jsonMode, setJsonMode] = useState(false);

  // 当初始值变化时更新 provider 状态
  useEffect(() => {
    if (initialValues?.provider) {
      setProvider(initialValues.provider);
    }
  }, [initialValues]);

  const currentDescriptors = provider ? descriptors[provider] || [] : [];

  const handleProviderChange = (value: string) => {
    setProvider(value);
  };

  return (
    <Form
      form={form}
      layout="vertical"
      initialValues={initialValues}
    >
      <Form.Item name="name" label={<Text strong>助理名称</Text>} rules={[{ required: true, message: '请输入名称' }]}>
        <Input placeholder="例如：新闻摘要助手" size="large" style={{ borderRadius: 8 }} />
      </Form.Item>

      <Form.Item name="responsibility" label={<Text strong>角色定位</Text>}>
        <TextArea rows={4} placeholder="描述该助理应当如何表现..." size="large" style={{ borderRadius: 8 }} />
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
        <Form.Item name="provider" label={<Text strong>实现方案</Text>} rules={[{ required: true, message: '请选择实现方案' }]}>
          <Select
            placeholder="选择 Agent 实现方案"
            size="large"
            style={{ borderRadius: 8 }}
            onChange={handleProviderChange}
            disabled={mode === 'edit'} // 通常编辑时不建议修改 provider，以免配置冲突
          >
            {Object.keys(descriptors).map(p => (
              <Select.Option key={p} value={p}>{p}</Select.Option>
            ))}
            {/* 保底选项 */}
            {!descriptors[provider || ''] && provider && (
               <Select.Option key={provider} value={provider}>{provider}</Select.Option>
            )}
          </Select>
        </Form.Item>
      </ConfigProvider>

      <Divider orientation="left" plain>
        <Space>
          <SettingOutlined />
          <Text type="secondary" style={{ fontSize: 13 }}>实现配置</Text>
          <Tooltip title="切换 JSON 编辑模式">
            <Switch
              size="small"
              checked={jsonMode}
              onChange={setJsonMode}
              checkedChildren={<CodeOutlined />}
              unCheckedChildren={<SettingOutlined />}
            />
          </Tooltip>
        </Space>
      </Divider>

      {jsonMode ? (
        <Form.Item
          name="providerConfigRaw"
          label={<Text strong>高级配置 (JSON)</Text>}
          tooltip="手动编辑 JSON 格式的个性化配置"
          rules={[
            {
              validator: async (_, value) => {
                if (value) {
                  try {
                    JSON.parse(value);
                  } catch (e) {
                    throw new Error('请输入合法的 JSON');
                  }
                }
              }
            }
          ]}
        >
          <TextArea rows={6} placeholder='{\n  "apiKey": "sk-...",\n  "model": "gpt-4"\n}' style={{ fontFamily: 'monospace', borderRadius: 8 }} />
        </Form.Item>
      ) : (
        <div className="dynamic-config-fields">
          {currentDescriptors.length > 0 ? (
            currentDescriptors.map(desc => (
              <Form.Item
                key={desc.key}
                name={['providerConfig', desc.key]}
                label={<Text strong>{desc.displayName || desc.key}</Text>}
                extra={desc.description}
                rules={[{ required: desc.required, message: `${desc.displayName || desc.key} 是必填项` }]}
                initialValue={desc.defaultValue}
              >
                <Input placeholder={desc.defaultValue} size="large" style={{ borderRadius: 8 }} />
              </Form.Item>
            ))
          ) : (
            <div style={{ padding: '20px 0', textAlign: 'center', background: '#fafafa', borderRadius: 8, border: '1px dashed #d9d9d9' }}>
              <Text type="secondary">该提供商暂无特定的元数据配置项，请切换至 JSON 模式进行设置</Text>
            </div>
          )}
        </div>
      )}
    </Form>
  );
};

export default AgentForm;
