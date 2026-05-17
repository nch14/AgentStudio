import { useState, useEffect, useCallback } from 'react';
import {
  Form, Input, Button, message, Spin, Select, Modal, Radio,
  Tag, Popconfirm, Typography, Switch, Divider,
} from 'antd';
import {
  UserOutlined,
  BellOutlined,
  LockOutlined,
} from '@ant-design/icons';
import { rotateToken } from '@/services/auth/AuthController';
import { getToken, setToken } from '@/utils/auth';
import { getProfile, createProfile, updateProfile } from '@/services/profile/ProfileController';
import type { ProfileDto, CreateProfileRequest, UpdateProfileRequest } from '@/services/profile/typings';
import {
  listNotifyConfigs,
  updateNotifyConfig,
} from '@/services/notifyConfig/NotifyConfigController';
import type { NotifyConfigItem, NotifyConfigUpdateRequest } from '@/services/notifyConfig/typings';
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

/** 单个事件配置行 */
function EventConfigRow({
  config,
  onUpdate,
}: {
  config: NotifyConfigItem;
  onUpdate: (eventCode: string, data: NotifyConfigUpdateRequest) => void;
}) {
  const [deliveryMode, setDeliveryMode] = useState<'INSTANT' | 'MERGED'>(config.deliveryMode as 'INSTANT' | 'MERGED');
  const [channels, setChannels] = useState<string[]>(config.channels || []);

  const handleToggleEnabled = async () => {
    await onUpdate(config.eventCode, { enabled: !config.enabled });
  };

  const handleDeliveryModeChange = async (value: 'INSTANT' | 'MERGED') => {
    setDeliveryMode(value);
    await onUpdate(config.eventCode, { deliveryMode: value });
  };

  const handleChannelsChange = async (value: string[]) => {
    setChannels(value);
    await onUpdate(config.eventCode, { channels: value });
  };

  const channelPills = [
    { value: 'BARK', label: 'Bark' },
    { value: 'EMAIL', label: 'Email' },
  ];

  return (
    <div className={styles.configCard}>
      <div className={styles.eventRow}>
        <div className={styles.eventInfo}>
          <div className={styles.eventNameLine}>
            <span className={styles.eventName}>{config.eventName}</span>
            <Tag color={config.groupCode === 'coordination' ? 'purple' : 'blue'} bordered={false}>
              {config.groupName}
            </Tag>
          </div>
          <span className={styles.eventDesc}>{config.description}</span>
        </div>

        <div className={styles.eventControls}>
          {/* 启用/禁用开关 */}
          <div className={styles.controlItem}>
            <span className={styles.controlLabel}>启用</span>
            <Switch size="small" checked={config.enabled} onChange={handleToggleEnabled} />
          </div>

          {/* 渠道选择 pill */}
          <div className={styles.controlItem}>
            <span className={styles.controlLabel}>渠道</span>
            <div className={styles.channelPills}>
              {channelPills.map((pill) => {
                const active = channels.includes(pill.value);
                return (
                  <span
                    key={pill.value}
                    className={`${styles.channelPill} ${active ? styles.channelPillActive : ''}`}
                    onClick={() => {
                      const next = active
                        ? channels.filter((c) => c !== pill.value)
                        : [...channels, pill.value];
                      handleChannelsChange(next);
                    }}
                  >
                    {pill.label}
                  </span>
                );
              })}
            </div>
          </div>

          {/* 投递模式 */}
          <div className={styles.controlItem}>
            <span className={styles.controlLabel}>投递</span>
            <Select
              size="small"
              value={deliveryMode}
              onChange={handleDeliveryModeChange}
              style={{ width: 110 }}
              options={[
                { label: '立即发送', value: 'INSTANT' },
                { label: '合并定时发送', value: 'MERGED' },
              ]}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

/** 按分组展示事件配置 */
function NotifyGroup({
  groupCode,
  groupName,
  configs,
  onUpdate,
}: {
  groupCode: string;
  groupName: string;
  configs: NotifyConfigItem[];
  onUpdate: (eventCode: string, data: NotifyConfigUpdateRequest) => void;
}) {
  const allEnabled = configs.length > 0 && configs.every((c) => c.enabled);
  const anyEnabled = configs.some((c) => c.enabled);

  const handleToggleGroup = async () => {
    const targetEnabled = !allEnabled;
    for (const config of configs) {
      await onUpdate(config.eventCode, { enabled: targetEnabled });
    }
  };

  return (
    <div className={styles.notifyGroup}>
      <div className={styles.groupHeader}>
        <Tag color={groupCode === 'coordination' ? 'purple' : 'blue'} bordered={false}>
          {groupName}
        </Tag>
        <span className={styles.groupToggle}>
          <span className={styles.controlLabel}>批量启用</span>
          <Switch size="small" checked={allEnabled} onChange={handleToggleGroup} />
        </span>
      </div>
      <Divider style={{ margin: '8px 0' }} />
      {configs.map((config) => (
        <EventConfigRow key={config.eventCode} config={config} onUpdate={onUpdate} />
      ))}
    </div>
  );
}

function NotifyTab() {
  const [configs, setConfigs] = useState<NotifyConfigItem[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchConfigs = useCallback(async () => {
    try {
      const res = await listNotifyConfigs();
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

  const handleUpdate = async (eventCode: string, data: NotifyConfigUpdateRequest) => {
    try {
      await updateNotifyConfig(eventCode, data);
      fetchConfigs();
    } catch {
      // handled by interceptor
    }
  };

  if (loading) {
    return (
      <div className={styles.tabLoading}>
        <Spin size="large" />
      </div>
    );
  }

  // 按分组聚合
  const groups = new Map<string, NotifyConfigItem[]>();
  for (const config of configs) {
    const list = groups.get(config.groupCode) || [];
    list.push(config);
    groups.set(config.groupCode, list);
  }

  return (
    <div className={styles.tabContent}>
      <div className={styles.tabHeader}>
        <h3>通知配置</h3>
        <p className={styles.tabHeaderDesc}>按事件管理通知的启用状态、推送渠道和投递策略</p>
      </div>

      {configs.length === 0 ? (
        <div className={styles.emptyState}>
          <p>暂无通知事件配置，请确认服务端已初始化</p>
        </div>
      ) : (
        <div>
          {Array.from(groups.entries()).map(([groupCode, groupConfigs]) => (
            <NotifyGroup
              key={groupCode}
              groupCode={groupCode}
              groupName={groupConfigs[0].groupName}
              configs={groupConfigs}
              onUpdate={handleUpdate}
            />
          ))}
        </div>
      )}
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
