import { message, ConfigProvider } from 'antd';
import type { RequestConfig } from '@umijs/max';
import { history } from '@umijs/max';
import React from 'react';
import { getToken } from '@/utils/auth';

export function rootContainer(container: React.ReactNode) {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#000000', // Minimalist Black
          borderRadius: 12,
          colorBgLayout: '#ffffff', // Pure white background
          colorBgContainer: '#ffffff',
          colorBorder: '#f0f0f0', // Very light borders
          fontFamily: `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif`,
        },
        components: {
          Card: {
            boxShadow: '0 4px 24px rgba(0,0,0,0.04)',
            borderRadiusOuter: 16,
          },
          Button: {
            borderRadius: 8,
          },
        }
      }}
    >
      {container}
    </ConfigProvider>
  );
}

/**
 * 与后端约定的响应数据格式
 */
interface ResponseStructure {
  success: boolean;
  data: any;
  errorCode?: number;
  errorMessage?: string;
}

/**
 * 全局请求配置
 */
export const request: RequestConfig = {
  timeout: 30000,
  errorConfig: {
    errorThrower: (res) => {
      const { success, errorMessage } = res as unknown as ResponseStructure;
      if (!success && errorMessage) {
        const error: any = new Error(errorMessage);
        error.name = 'BizError';
        error.info = res;
        throw error;
      }
    },
    errorHandler: (error: any, opts: any) => {
      if (opts?.skipErrorHandler) throw error;
      if (error.name === 'BizError') {
        message.error(error.message);
      } else if (error.response) {
        if (error.response.status === 401) {
          history.push('/login');
          return;
        }
        message.error(`请求失败，状态码: ${error.response.status}`);
      } else if (error.request) {
        message.error('服务无响应，请重试');
      } else {
        message.error('请求异常，请重试');
      }
    },
  },
  requestInterceptors: [
    (config: any) => {
      const token = getToken();
      if (token) {
        config.headers = {
          ...config.headers,
          Authorization: `Bearer ${token}`,
        };
      }
      return config;
    },
  ],
  responseInterceptors: [
    (response: any) => {
      const { data } = response;
      if (data?.success === false && data?.errorMessage) {
        message.error(data.errorMessage);
      }
      return response;
    },
  ],
};
