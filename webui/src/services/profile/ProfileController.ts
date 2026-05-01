import { request } from '@umijs/max';
import type { ProfileDto, CreateProfileRequest, UpdateProfileRequest } from './typings';

const BASE_URL = '/api/profile';

/** 获取当前用户资料 */
export async function getProfile(options?: { [key: string]: any }) {
  return request<{ data: ProfileDto }>(BASE_URL, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 创建用户资料（仅初始化时调用一次） */
export async function createProfile(data: CreateProfileRequest, options?: { [key: string]: any }) {
  return request<{ data: ProfileDto }>(BASE_URL, {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

/** 更新用户资料 */
export async function updateProfile(data: UpdateProfileRequest, options?: { [key: string]: any }) {
  return request<{ data: ProfileDto }>(BASE_URL, {
    method: 'PUT',
    data,
    ...(options || {}),
  });
}
