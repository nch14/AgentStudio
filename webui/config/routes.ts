/**
 * umi 路由配置
 */
export default [
  {
    path: '/login',
    name: 'Login',
    component: './login',
    hideInMenu: true,
    layout: false,
  },
  {
    path: '/',
    redirect: '/agents',
  },
  {
    path: '/agents',
    name: 'Agents',
    component: './agents',
  },
  {
    path: '/agents/:code',
    name: 'Agent Detail',
    component: './agents/detail',
    hideInMenu: true,
  },
  {
    path: '/chat',
    name: 'Chat',
    component: './chat',
  },
  {
    path: '/tasks',
    name: 'Tasks',
    component: './tasks',
  },
  {
    path: '/tasks/:code',
    name: 'Task Detail',
    component: './tasks/detail',
    hideInMenu: true,
  },
  {
    path: '/automation-plans',
    name: 'Automation Plans',
    component: './automationPlans',
  },
  {
    path: '/settings',
    name: 'Settings',
    component: './settings',
    hideInMenu: true,
  },
  {
    path: '/autoplans/:code',
    name: 'AutoPlan Detail',
    component: './autoplans/detail',
    hideInMenu: true,
  },
];
