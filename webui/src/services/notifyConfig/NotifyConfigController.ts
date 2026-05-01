import { request } from '@umijs/max';
import type { NotifyConfigDto, NotifyConfigCreateRequest, NotifyConfigUpdateRequest, NotifyConfigPageResponse } from './typings';

const BASE_URL = '/api/v1/notify-configs';

/** 分页查询通知配置列表 */
export async function listNotifyConfigs(params?: { page?: number; size?: number }, options?: { [key: string]: any }) {
  return request<NotifyConfigPageResponse>(BASE_URL, {
    method: 'GET',
    params: { page: params?.page ?? 0, size: params?.size ?? 20 },
    ...(options || {}),
  });
}

/** 查询通知配置详情 */
export async function getNotifyConfig(configCode: string, options?: { [key: string]: any }) {
  return request<{ data: NotifyConfigDto }>(`${BASE_URL}/${configCode}`, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 创建通知配置 */
export async function createNotifyConfig(data: NotifyConfigCreateRequest, options?: { [key: string]: any }) {
  return request<{ data: NotifyConfigDto }>(BASE_URL, {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

/** 更新通知配置 */
export async function updateNotifyConfig(configCode: string, data: NotifyConfigUpdateRequest, options?: { [key: string]: any }) {
  return request<{ data: NotifyConfigDto }>(`${BASE_URL}/${configCode}`, {
    method: 'PUT',
    data,
    ...(options || {}),
  });
}

/** 删除通知配置 */
export async function deleteNotifyConfig(configCode: string, options?: { [key: string]: any }) {
  return request(`${BASE_URL}/${configCode}`, {
    method: 'DELETE',
    ...(options || {}),
  });
}
