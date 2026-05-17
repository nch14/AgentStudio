import type { IncomingMessage } from 'http';

/**
 * 代理配置
 * 开发环境下将 /api 请求转发到后端服务
 */
export default {
  dev: {
    '/api/': {
      target: 'http://localhost:8685',
      changeOrigin: true,
      onProxyRes: (proxyRes: IncomingMessage) => {
        // Disable response buffering for SSE
        proxyRes.headers['X-Accel-Buffering'] = 'no';
        proxyRes.headers['Cache-Control'] = 'no-cache';
      },
    },
  },
};
