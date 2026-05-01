/* eslint-disable */
import { request } from '@umijs/max';
import type {
  TaskTurnListItemResponse,
  TaskTurnDetailResponse,
} from './typings';

const BASE_URL = '/api/task-turns';

/** 查询任务的所有回合列表 */
export async function listTaskTurns(taskCode: string, options?: { [key: string]: any }) {
  return request<{ data: TaskTurnListItemResponse[] }>(`${BASE_URL}/task/${taskCode}`, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 查询回合详情 */
export async function getTurnDetail(turnCode: string, options?: { [key: string]: any }) {
  return request<{ data: TaskTurnDetailResponse }>(`${BASE_URL}/${turnCode}`, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 重置 HANGING 状态的回合为 RUNNING */
export async function resumeTurn(turnCode: string, options?: { [key: string]: any }) {
  return request<{ data: TaskTurnDetailResponse }>(`${BASE_URL}/${turnCode}/resume`, {
    method: 'POST',
    ...(options || {}),
  });
}
