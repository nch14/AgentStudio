import { request } from '@umijs/max';
import type { NotifyConfigItem, NotifyConfigUpdateRequest } from './typings';

const BASE_URL = '/api/v1/notify-configs';

/** 查询所有事件配置列表，按分组排序 */
export async function listNotifyConfigs(options?: { [key: string]: any }) {
  return request<{ data: NotifyConfigItem[] }>(BASE_URL, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 查询指定事件的配置详情 */
export async function getNotifyConfig(eventCode: string, options?: { [key: string]: any }) {
  return request<{ data: NotifyConfigItem }>(`${BASE_URL}/${eventCode}`, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 更新事件通知配置 */
export async function updateNotifyConfig(eventCode: string, data: NotifyConfigUpdateRequest, options?: { [key: string]: any }) {
  return request<{ data: NotifyConfigItem }>(`${BASE_URL}/${eventCode}`, {
    method: 'PUT',
    data,
    ...(options || {}),
  });
}
