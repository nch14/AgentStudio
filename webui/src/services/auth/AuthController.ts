import { request } from '@umijs/max';

const BASE_URL = '/api/auth';

/** 初始化 API Token（仅首次调用有效） */
export async function initToken(secret: string) {
  return request<{ data: { token: string } }>(`${BASE_URL}/init`, {
    method: 'POST',
    data: { secret },
  });
}

/** 验证当前 Token 是否有效 */
export async function validateToken() {
  return request<{ data: { valid: boolean } }>(`${BASE_URL}/validate`, {
    method: 'GET',
  });
}

/** 轮换 API Token（需要当前有效 token） */
export async function rotateToken(newSecret: string) {
  return request<{ data: { token: string } }>(`${BASE_URL}/rotate`, {
    method: 'POST',
    data: { newSecret },
  });
}
