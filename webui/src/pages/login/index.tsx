import { useState, useEffect } from 'react';
import { Button, Input, Modal, Typography, Tabs, message } from 'antd';
import { LoginOutlined, RocketOutlined } from '@ant-design/icons';
import { initToken, validateToken } from '@/services/auth/AuthController';
import { setToken, getToken } from '@/utils/auth';
import { history } from '@umijs/max';
import styles from './style.less';

export default function LoginPage() {
  const [loginToken, setLoginToken] = useState('');
  const [initSecret, setInitSecret] = useState('');
  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(false);
  const [generatedToken, setGeneratedToken] = useState('');
  const [showTokenModal, setShowTokenModal] = useState(false);
  const [activeTab, setActiveTab] = useState<'login' | 'init'>('login');

  useEffect(() => {
    const savedToken = getToken();
    if (savedToken) {
      setChecking(true);
      validateToken()
        .then((res) => {
          if (res.data?.valid) {
            history.push('/agents');
          } else {
            setChecking(false);
          }
        })
        .catch(() => {
          setChecking(false);
        });
    }
  }, []);

  const handleLogin = async () => {
    if (!loginToken.trim()) {
      message.error('请输入 Token');
      return;
    }
    setLoading(true);
    try {
      setToken(loginToken.trim());
      const res = await validateToken();
      if (res.data?.valid) {
        message.success('登录成功');
        history.push('/agents');
      } else {
        message.error('Token 无效，请检查后重试');
      }
    } catch {
      message.error('Token 无效或网络异常');
    } finally {
      setLoading(false);
    }
  };

  const handleInit = async () => {
    if (!initSecret.trim()) {
      message.error('请输入 Secret');
      return;
    }
    setLoading(true);
    try {
      const res = await initToken(initSecret.trim());
      if (res.data?.token) {
        setToken(res.data.token);
        setGeneratedToken(res.data.token);
        setShowTokenModal(true);
      }
    } catch {
      message.error('初始化失败，请检查 Secret 是否正确');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      activeTab === 'login' ? handleLogin() : handleInit();
    }
  };

  const tabItems = [
    {
      key: 'login',
      label: '登录',
      icon: <LoginOutlined />,
      children: (
        <>
          <div className={styles.subtitle}>请输入 API Token 登录</div>
          <Input.Password
            className={styles.input}
            placeholder="粘贴或输入 Token"
            value={loginToken}
            onChange={(e) => setLoginToken(e.target.value)}
            onKeyDown={handleKeyDown}
            size="large"
          />
          <Button
            type="primary"
            className={styles.button}
            size="large"
            block
            loading={loading}
            onClick={handleLogin}
          >
            登录
          </Button>
        </>
      ),
    },
    {
      key: 'init',
      label: '初始化',
      icon: <RocketOutlined />,
      children: (
        <>
          <div className={styles.subtitle}>首次使用时，请输入 Secret 初始化系统</div>
          <Input.Password
            className={styles.input}
            placeholder="输入 Secret"
            value={initSecret}
            onChange={(e) => setInitSecret(e.target.value)}
            onKeyDown={handleKeyDown}
            size="large"
          />
          <Button
            type="primary"
            className={styles.button}
            size="large"
            block
            loading={loading}
            onClick={handleInit}
          >
            初始化
          </Button>
        </>
      ),
    },
  ];

  if (checking) {
    return (
      <div className={styles.container}>
        <div className={styles.form}>
          <div className={styles.title}>Agent Studio</div>
          <div className={styles.loadingText}>正在验证 Token...</div>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className={styles.container}>
        <div className={styles.form}>
          <div className={styles.title}>Agent Studio</div>
          <Tabs
            activeKey={activeTab}
            onChange={(key) => setActiveTab(key as 'login' | 'init')}
            items={tabItems}
            size="large"
            className={styles.tabWrap}
            centered
          />
        </div>
      </div>

      <Modal
        title="Token 初始化成功"
        open={showTokenModal}
        onOk={() => { setShowTokenModal(false); history.push('/agents'); }}
        okText="已保存，进入系统"
        closable={false}
        maskClosable={false}
        cancelButtonProps={{ style: { display: 'none' } }}
      >
        <p style={{ marginBottom: 12, color: 'rgba(0,0,0,0.65)' }}>
          你的 API Token 已生成。请复制并妥善保存——在其他设备或浏览器登录时，需要粘贴此 Token。
        </p>
        <Typography.Paragraph copyable={{ text: generatedToken }} code style={{ wordBreak: 'break-all' }}>
          {generatedToken}
        </Typography.Paragraph>
        <p style={{ marginTop: 8, fontSize: 12, color: 'rgba(0,0,0,0.4)' }}>
          Token 也可以在登录后的「设置 → 安全」中随时查看和轮换。
        </p>
      </Modal>
    </>
  );
}
