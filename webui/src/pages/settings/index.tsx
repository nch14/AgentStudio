import { useState, useEffect, useCallback } from 'react';
import {
  Form, Input, Button, message, Spin, Select, Modal, Radio,
  Tag, Popconfirm, Typography,
} from 'antd';
import {
  UserOutlined,
  BellOutlined,
  PlusOutlined,
  LockOutlined,
} from '@ant-design/icons';
import { rotateToken } from '@/services/auth/AuthController';
import { getToken, setToken } from '@/utils/auth';
import { getProfile, createProfile, updateProfile } from '@/services/profile/ProfileController';
import type { ProfileDto, CreateProfileRequest, UpdateProfileRequest } from '@/services/profile/typings';
import {
  listNotifyConfigs,
  createNotifyConfig,
  updateNotifyConfig,
  deleteNotifyConfig,
} from '@/services/notifyConfig/NotifyConfigController';
import type { NotifyConfigDto, NotifyConfigCreateRequest, NotifyConfigUpdateRequest } from '@/services/notifyConfig/typings';
import styles from './index.less';

const { TextArea } = Input;

const tabs = [
  { key: 'profile', icon: <UserOutlined />, label: '个人资料' },
  { key: 'notify', icon: <BellOutlined />, label: '通知配置' },
  { key: 'security', icon: <LockOutlined />, label: '安全' },
];

function ProfileTab() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [profile, setProfile] = useState<ProfileDto | null>(null);
  const [initialized, setInitialized] = useState(false);

  const fetchProfile = async () => {
    try {
      const res = await getProfile();
      if (res.data) {
        setProfile(res.data);
        setInitialized(true);
        form.setFieldsValue(res.data);
      }
    } catch {
      // no profile yet
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (!initialized) {
        await createProfile(values as CreateProfileRequest);
        message.success('初始化成功');
      } else {
        await updateProfile(values as UpdateProfileRequest);
        message.success('更新成功');
      }
      fetchProfile();
    } catch {
      // handled by interceptor
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className={styles.tabLoading}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div className={styles.tabContent}>
      <div className={styles.tabHeader}>
        <h3>个人资料</h3>
        <p className={styles.tabHeaderDesc}>
          {initialized ? '管理你的个人信息' : '首次使用，请先初始化个人资料'}
        </p>
      </div>

      <Form form={form} layout="vertical" className={styles.settingForm}>
        <div className={styles.formSection}>
          <div className={styles.formSectionTitle}>基本信息</div>

          <Form.Item name="displayName" label="显示名称" rules={[{ required: true, message: '请输入显示名称' }]}>
            <Input placeholder="你的显示名称" />
          </Form.Item>

          <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '请输入有效邮箱' }]}>
            <Input placeholder="your@email.com" />
          </Form.Item>

          {!initialized && (
            <>
              <Form.Item name="timezone" label="时区" rules={[{ required: true, message: '请输入时区' }]}>
                <Input placeholder="Asia/Shanghai" />
              </Form.Item>
              <Form.Item name="locale" label="语言" rules={[{ required: true, message: '请输入语言' }]}>
                <Input placeholder="zh-CN" />
              </Form.Item>
            </>
          )}

          {initialized && profile && (
            <div className={styles.localeBadges}>
              <Tag>{profile.timezone}</Tag>
              <Tag>{profile.locale}</Tag>
            </div>
          )}
        </div>

        <div className={styles.formSection}>
          <div className={styles.formSectionTitle}>配置</div>

          <Form.Item name="barkDeviceKey" label="Bark 设备 Key">
            <Input placeholder="用于推送通知的 Bark 设备 Key" />
          </Form.Item>

          <Form.Item name="bio" label="个人简介">
            <TextArea rows={3} placeholder="简单介绍一下自己..." />
          </Form.Item>
        </div>

        <div className={styles.formActions}>
          <Button type="primary" onClick={handleSave} loading={saving}>
            {initialized ? '保存修改' : '初始化'}
          </Button>
        </div>
      </Form>
    </div>
  );
}

function NotifyConfigItem({
  config,
  onUpdate,
  onDelete,
}: {
  config: NotifyConfigDto;
  onUpdate: (configCode: string, data: NotifyConfigUpdateRequest) => void;
  onDelete: (configCode: string) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [deliveryMode, setDeliveryMode] = useState(config.deliveryMode);
  const [channels, setChannels] = useState<string[]>(config.channels ? config.channels.split(',').filter(Boolean) : []);
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      await onUpdate(config.configCode, {
        deliveryMode,
        channels: channels.join(','),
      });
      setEditing(false);
    } finally {
      setSaving(false);
    }
  };

  const channelOptions = [
    { label: 'Bark', value: 'BARK' },
    { label: 'Email', value: 'EMAIL' },
  ];

  return (
    <div className={styles.configCard}>
      <div className={styles.configMain}>
        <div className={styles.configTitleLine}>
          <span className={styles.configName}>{config.name}</span>
          <code className={styles.configCode}>{config.configCode}</code>
        </div>
        <div className={styles.configMetaLine}>
          <Tag color={config.deliveryMode === 'INSTANT' ? 'blue' : 'orange'} bordered={false} className={styles.configTag}>
            {config.deliveryMode === 'INSTANT' ? '立即发送' : '合并定时发送'}
          </Tag>
          <div className={styles.configChannels}>
            {config.channels
              ? config.channels.split(',').map((ch) => (
                  <Tag key={ch} bordered={false} className={styles.channelTag}>
                    {ch}
                  </Tag>
                ))
              : <span className={styles.noChannels}>未配置渠道</span>}
          </div>
        </div>
      </div>
      <div className={styles.configActions}>
        <Button size="small" type="text" onClick={() => setEditing(true)}>编辑</Button>
        <Popconfirm title="确定删除此配置？" onConfirm={() => onDelete(config.configCode)} okText="确定" cancelText="取消">
          <Button size="small" type="text" danger>删除</Button>
        </Popconfirm>
      </div>

      <Modal
        title={`编辑通知配置: ${config.name}`}
        open={editing}
        onCancel={() => setEditing(false)}
        okText="保存"
        cancelText="取消"
        onOk={handleSave}
        confirmLoading={saving}
      >
        <Form layout="vertical">
          <Form.Item label="投递模式">
            <Radio.Group value={deliveryMode} onChange={(e) => setDeliveryMode(e.target.value)}>
              <Radio value="INSTANT">立即发送</Radio>
              <Radio value="MERGED">合并定时发送</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item label="通知渠道">
            <Select
              mode="multiple"
              value={channels}
              onChange={setChannels}
              options={channelOptions}
              placeholder="选择通知渠道"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

function NotifyTab() {
  const [configs, setConfigs] = useState<NotifyConfigDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [createModal, setCreateModal] = useState(false);
  const [createForm] = Form.useForm();
  const [creating, setCreating] = useState(false);

  const fetchConfigs = useCallback(async () => {
    try {
      const res = await listNotifyConfigs({ page: 0, size: 50 });
      setConfigs(res.data || []);
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConfigs();
  }, []);

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    setCreating(true);
    try {
      await createNotifyConfig({
        name: values.name,
        deliveryMode: values.deliveryMode || 'INSTANT',
        channels: (values.channels || []).join(','),
      });
      message.success('创建成功');
      setCreateModal(false);
      createForm.resetFields();
      fetchConfigs();
    } finally {
      setCreating(false);
    }
  };

  const handleUpdate = async (configCode: string, data: NotifyConfigUpdateRequest) => {
    await updateNotifyConfig(configCode, data);
    message.success('更新成功');
    fetchConfigs();
  };

  const handleDelete = async (configCode: string) => {
    await deleteNotifyConfig(configCode);
    message.success('已删除');
    fetchConfigs();
  };

  if (loading) {
    return (
      <div className={styles.tabLoading}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div className={styles.tabContent}>
      <div className={styles.tabHeader}>
        <div className={styles.tabHeaderMain}>
          <h3>通知配置</h3>
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => setCreateModal(true)}>
            新建配置
          </Button>
        </div>
        <p className={styles.tabHeaderDesc}>管理不同场景下的通知投递策略</p>
      </div>

      {configs.length === 0 ? (
        <div className={styles.emptyState}>
          <p>暂无通知配置</p>
        </div>
      ) : (
        <div className={styles.configList}>
          {configs.map((config) => (
            <NotifyConfigItem
              key={config.configCode}
              config={config}
              onUpdate={handleUpdate}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}

      <Modal
        title="新建通知配置"
        open={createModal}
        onCancel={() => setCreateModal(false)}
        okText="创建"
        cancelText="取消"
        onOk={handleCreate}
        confirmLoading={creating}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="name" label="配置名称" rules={[{ required: true, message: '请输入配置名称' }]}>
            <Input placeholder="例如：任务完成通知" />
          </Form.Item>
          <Form.Item name="deliveryMode" label="投递模式" initialValue="INSTANT">
            <Radio.Group>
              <Radio value="INSTANT">立即发送</Radio>
              <Radio value="MERGED">合并定时发送</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item name="channels" label="通知渠道">
            <Select
              mode="multiple"
              options={[
                { label: 'Bark', value: 'BARK' },
                { label: 'Email', value: 'EMAIL' },
              ]}
              placeholder="选择通知渠道"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

function SecurityTab() {
  const [newSecret, setNewSecret] = useState('');
  const [rotating, setRotating] = useState(false);
  const [newTokenModal, setNewTokenModal] = useState(false);
  const [newToken, setNewToken] = useState('');
  const currentToken = getToken() || '';

  const handleRotate = async () => {
    if (!newSecret.trim()) {
      message.error('请输入新的 Secret');
      return;
    }
    setRotating(true);
    try {
      const res = await rotateToken(newSecret.trim());
      if (res.data?.token) {
        setToken(res.data.token);
        setNewToken(res.data.token);
        setNewSecret('');
        setNewTokenModal(true);
      }
    } catch {
      // handled by interceptor
    } finally {
      setRotating(false);
    }
  };

  return (
    <div className={styles.tabContent}>
      <div className={styles.tabHeader}>
        <h3>安全</h3>
        <p className={styles.tabHeaderDesc}>管理你的 API Token</p>
      </div>

      <div className={styles.formSection}>
        <div className={styles.formSectionTitle}>当前 Token</div>
        <p className={styles.tokenHint}>在其他设备或浏览器登录时，粘贴此 Token 到登录框即可。</p>
        <Typography.Paragraph
          copyable={{ text: currentToken, tooltips: ['复制', '已复制'] }}
          code
          className={styles.tokenDisplay}
        >
          {currentToken || '未找到 Token'}
        </Typography.Paragraph>
      </div>

      <div className={styles.formSection}>
        <div className={styles.formSectionTitle}>轮换 Token</div>
        <p className={styles.tokenHint}>输入一个新的 Secret，系统将生成并替换当前 Token。旧 Token 立即失效，所有已登录设备需重新验证。</p>
        <Input.Password
          placeholder="输入新的 Secret"
          value={newSecret}
          onChange={(e) => setNewSecret(e.target.value)}
          style={{ marginBottom: 12 }}
        />
        <Popconfirm
          title="确认轮换 Token？"
          description="旧 Token 将立即失效，其他设备需重新登录。"
          onConfirm={handleRotate}
          okText="确认轮换"
          cancelText="取消"
          disabled={!newSecret.trim()}
        >
          <Button danger loading={rotating} disabled={!newSecret.trim()}>
            轮换 Token
          </Button>
        </Popconfirm>
      </div>

      <Modal
        title="Token 已轮换"
        open={newTokenModal}
        onOk={() => setNewTokenModal(false)}
        okText="已保存，关闭"
        closable={false}
        maskClosable={false}
        cancelButtonProps={{ style: { display: 'none' } }}
      >
        <p style={{ marginBottom: 12, color: 'rgba(0,0,0,0.65)' }}>
          新的 Token 已生成并保存到当前浏览器。请复制并妥善保存，在其他设备登录时需要用到。
        </p>
        <Typography.Paragraph
          copyable={{ text: newToken, tooltips: ['复制', '已复制'] }}
          code
          style={{ wordBreak: 'break-all' }}
        >
          {newToken}
        </Typography.Paragraph>
      </Modal>
    </div>
  );
}

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState('profile');

  return (
    <div className={styles.settingsContainer}>
      <div className={styles.sidebar}>
        {tabs.map((tab) => (
          <div
            key={tab.key}
            className={`${styles.sidebarItem} ${activeTab === tab.key ? styles.sidebarItemActive : ''}`}
            onClick={() => setActiveTab(tab.key)}
          >
            <span className={styles.sidebarItemIcon}>{tab.icon}</span>
            <span className={styles.sidebarItemLabel}>{tab.label}</span>
          </div>
        ))}
      </div>
      <div className={styles.contentArea}>
        {activeTab === 'profile' && <ProfileTab />}
        {activeTab === 'notify' && <NotifyTab />}
        {activeTab === 'security' && <SecurityTab />}
      </div>
    </div>
  );
}
