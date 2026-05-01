/* eslint-disable */
import { request } from '@umijs/max';
import type {
  TaskDetailResponse,
  TaskListItemResponse,
  TaskCreateRequest,
  TaskUpdateRequest,
  TaskCancelRequest,
} from './typings';

const BASE_URL = '/api/tasks';

/** 分页查询任务列表 */
export async function listTasks(
  params: { page?: number; size?: number; source?: string; status?: string; type?: string; sourceRef?: string },
  options?: { [key: string]: any },
) {
  return request<{ data: TaskListItemResponse[]; total: number }>(BASE_URL, {
    method: 'GET',
    params,
    ...(options || {}),
  });
}

/** 查询任务详情 */
export async function getTaskDetail(taskCode: string, options?: { [key: string]: any }) {
  return request<{ data: TaskDetailResponse }>(`${BASE_URL}/${taskCode}`, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 取消任务 */
export async function cancelTask(
  taskCode: string,
  data?: TaskCancelRequest,
  options?: { [key: string]: any },
) {
  return request<{ data: TaskDetailResponse }>(`${BASE_URL}/${taskCode}/cancel`, {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

/** 重试任务 */
export async function retryTask(taskCode: string, options?: { [key: string]: any }) {
  return request<{ data: TaskDetailResponse }>(`${BASE_URL}/${taskCode}/retry`, {
    method: 'POST',
    ...(options || {}),
  });
}

/** 创建任务 */
export async function createTask(data: TaskCreateRequest, options?: { [key: string]: any }) {
  return request<{ data: TaskDetailResponse }>(BASE_URL, {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

/** 修改任务属性 */
export async function updateTask(
  taskCode: string,
  data: TaskUpdateRequest,
  options?: { [key: string]: any },
) {
  return request<{ data: TaskDetailResponse }>(`${BASE_URL}/${taskCode}`, {
    method: 'PUT',
    data,
    ...(options || {}),
  });
}

/** 删除任务 */
export async function deleteTask(taskCode: string, options?: { [key: string]: any }) {
  return request<void>(`${BASE_URL}/${taskCode}/delete`, {
    method: 'POST',
    ...(options || {}),
  });
}

/** 重新运行任务（回滚已完成任务为 RUNNING） */
export async function rollbackTask(taskCode: string, options?: { [key: string]: any }) {
  return request<{ data: TaskDetailResponse }>(`${BASE_URL}/${taskCode}/rollback`, {
    method: 'POST',
    ...(options || {}),
  });
}
