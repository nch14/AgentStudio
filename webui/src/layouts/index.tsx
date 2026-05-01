import { useState, useEffect } from 'react';
import { Link, useLocation, Outlet, Navigate } from '@umijs/max';
import { Layout, Menu, Avatar, Popconfirm, Button } from 'antd';
import {
  RobotOutlined,
  MessageOutlined,
  UnorderedListOutlined,
  ClockCircleOutlined,
  SettingOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import logoImage from '@/assets/agent-studio-logo.png';
import styles from './index.less';
import { getProfile } from '@/services/profile/ProfileController';
import type { ProfileDto } from '@/services/profile/typings';
import { isAuthenticated, removeToken } from '@/utils/auth';
import { history } from '@umijs/max';

const { Header, Content } = Layout;

const menuItems = [
  {
    key: '/agents',
    icon: <RobotOutlined />,
    label: <Link to="/agents">Agents</Link>,
  },
  {
    key: '/chat',
    icon: <MessageOutlined />,
    label: <Link to="/chat">Chat</Link>,
  },
  {
    key: '/automation-plans',
    icon: <ClockCircleOutlined />,
    label: <Link to="/automation-plans">Auto Plans</Link>,
  },
  {
    key: '/tasks',
    icon: <UnorderedListOutlined />,
    label: <Link to="/tasks">Tasks</Link>,
  },
];

export default function LayoutComponent() {
  const { pathname } = useLocation();

  if (!isAuthenticated() && pathname !== '/login') {
    return <Navigate to="/login" replace />;
  }

  if (pathname === '/login') {
    return <Outlet />;
  }

  const [selectedKey, setSelectedKey] = useState(pathname);

  // Sync selected key with pathname manually
  useEffect(() => {
    if (pathname === '/') {
      setSelectedKey('/agents');
    } else {
      setSelectedKey(pathname);
    }
  }, [pathname]);

  useEffect(() => {
    let favicon = document.querySelector<HTMLLinkElement>("link[rel='icon']");
    if (!favicon) {
      favicon = document.createElement('link');
      favicon.rel = 'icon';
      document.head.appendChild(favicon);
    }
    favicon.href = '/brand/agent-studio-logo.png';
  }, []);

  const noPaddingRoutes = ['/chat'];
  const isAgentDetail = pathname.startsWith('/agents/') && pathname !== '/agents';
  const isTaskDetail = pathname.startsWith('/tasks/') && pathname !== '/tasks';
  const isAutoPlanDetail = pathname.startsWith('/autoplans/') && pathname !== '/autoplans';
  const isNoPadding =
    noPaddingRoutes.some((route) => pathname.startsWith(route)) || isAgentDetail || isTaskDetail || isAutoPlanDetail;

  const [profile, setProfile] = useState<ProfileDto | null>(null);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    getProfile()
      .then((res) => {
        if (res.data) {
          setProfile(res.data);
          setInitialized(true);
        }
      })
      .catch(() => {});
  }, []);

  return (
    <Layout className={styles.layout}>
      <Header className={styles.header}>
        <Link to="/agents" className={styles.logo}>
          <span className={styles.logoIconWrap}>
            <img src={logoImage} alt="Agent Studio logo" className={styles.logoIcon} />
          </span>
          <span className={styles.logoText}>Agent Studio</span>
        </Link>
        <Menu
          mode="horizontal"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onSelect={({ key }) => setSelectedKey(key)}
          className={styles.menu}
        />
        <div className={styles.headerRight}>
          <div className={styles.userInfo}>
            {initialized && profile ? (
              <>
                <Avatar className={styles.userAvatar} size={28}>
                  {profile.displayName?.[0]?.toUpperCase()}
                </Avatar>
                <span className={styles.userName}>{profile.displayName}</span>
              </>
            ) : (
              <span className={styles.userName}>未初始化</span>
            )}
          </div>
          <Link to="/settings" className={styles.settingsBtn} title="设置">
            <SettingOutlined />
          </Link>
          <Popconfirm
            title="确认退出登录？"
            onConfirm={() => { removeToken(); history.push('/login'); }}
            okText="确定"
            cancelText="取消"
          >
            <Button type="text" className={styles.logoutBtn} title="退出登录">
              <LogoutOutlined />
            </Button>
          </Popconfirm>
        </div>
      </Header>
      <Layout className={styles.contentWrapper}>
        <Content 
          className={`${styles.content} ${isNoPadding ? styles.contentNoPadding : styles.contentWithPadding}`}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
