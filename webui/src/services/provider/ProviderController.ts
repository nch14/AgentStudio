/* eslint-disable */
import { request } from '@umijs/max';
import type { ConfigKeyDescriptor, ProviderSupports } from './typings';

const BASE_URL = '/api/v1/providers';

/** 查询提供商配置描述符 */
export async function listConfigs(options?: { [key: string]: any }) {
  return request<{ data: ConfigKeyDescriptor[] }>(`${BASE_URL}/configs`, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 查询提供商能力支持 */
export async function getProviderSupports(options?: { [key: string]: any }) {
  return request<{ data: ProviderSupports[] }>(`${BASE_URL}/supports`, {
    method: 'GET',
    ...(options || {}),
  });
}
