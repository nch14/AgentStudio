import { defineConfig } from '@umijs/max';
import proxy from './proxy';
import routes from './routes';

const { REACT_APP_ENV = 'dev' } = process.env;

export default defineConfig({
  hash: true,

  routes,

  proxy: proxy[REACT_APP_ENV as keyof typeof proxy] || proxy.dev,

  fastRefresh: true,

  // 页面标题
  title: 'Agent Studio',

  metas: [
    {
      name: 'viewport',
      content: 'width=device-width, initial-scale=1',
    },
  ],

  // 忽略 moment 本地化
  ignoreMomentLocale: true,

  // antd 配置
  antd: {
    style: 'css',
  },

  // 网络请求配置（启用 request 插件）
  request: {},

  // 避免 esbuild helper 冲突
  esbuildMinifyIIFE: true,
});
