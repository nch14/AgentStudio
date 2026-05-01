import { request } from '@umijs/max';
import type {
  AutomationPlanListItemDTO,
  AutomationPlanDetailDTO,
  AutomationPlanCreateRequest,
  AutomationPlanUpdateRequest,
} from './typings';

const BASE_URL = '/api/v1/automation-plans';

/** 分页查询自动化计划列表 */
export async function listPlans(
  params: { page?: number; size?: number; status?: string },
  options?: { [key: string]: any },
) {
  return request<{ data: AutomationPlanListItemDTO[]; total: number }>(BASE_URL, {
    method: 'GET',
    params,
    ...(options || {}),
  });
}

/** 查询自动化计划详情 */
export async function getPlanDetail(planCode: string, options?: { [key: string]: any }) {
  return request<{ data: AutomationPlanDetailDTO }>(`${BASE_URL}/${planCode}`, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 创建自动化计划 */
export async function createPlan(data: AutomationPlanCreateRequest, options?: { [key: string]: any }) {
  return request<{ data: AutomationPlanDetailDTO }>(BASE_URL, {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

/** 修改自动化计划 */
export async function updatePlan(
  planCode: string,
  data: AutomationPlanUpdateRequest,
  options?: { [key: string]: any },
) {
  return request<{ data: AutomationPlanDetailDTO }>(`${BASE_URL}/${planCode}`, {
    method: 'PUT',
    data,
    ...(options || {}),
  });
}

/** 启用自动化计划 */
export async function enablePlan(planCode: string, options?: { [key: string]: any }) {
  return request<{ data: AutomationPlanDetailDTO }>(`${BASE_URL}/${planCode}/enable`, {
    method: 'POST',
    ...(options || {}),
  });
}

/** 禁用自动化计划 */
export async function disablePlan(planCode: string, options?: { [key: string]: any }) {
  return request<{ data: AutomationPlanDetailDTO }>(`${BASE_URL}/${planCode}/disable`, {
    method: 'POST',
    ...(options || {}),
  });
}

/** 删除自动化计划 */
export async function deletePlan(planCode: string, options?: { [key: string]: any }) {
  return request<{ data: null; message?: string }>(`${BASE_URL}/${planCode}/delete`, {
    method: 'POST',
    ...(options || {}),
  });
}
