/* eslint-disable */
import { request } from '@umijs/max';
import type {
  QuestionsResponse,
  QuestionsResolveRequest,
  InstructionResponse,
  InstructionCreateRequest,
} from './typings';

/** 查询任务的问题集列表 */
export async function listQuestions(
  taskCode: string,
  params?: { resolved?: boolean },
  options?: { [key: string]: any },
) {
  return request<{ data: QuestionsResponse[] }>(`/api/tasks/${taskCode}/coordination/questions`, {
    method: 'GET',
    params,
    ...(options || {}),
  });
}

/** 查询问题集详情 */
export async function getQuestionsDetail(
  taskCode: string,
  questionsCode: string,
  options?: { [key: string]: any },
) {
  return request<{ data: QuestionsResponse }>(
    `/api/tasks/${taskCode}/coordination/questions/${questionsCode}`,
    {
      method: 'GET',
      ...(options || {}),
    },
  );
}

/** 查询当前活跃问题集 */
export async function getActiveQuestions(taskCode: string, options?: { [key: string]: any }) {
  // 注意：后端目前主要推荐用 listQuestions(taskCode, { resolved: false })，但原有组件可能还在用这个
  return request<{ data: QuestionsResponse }>(`/api/tasks/${taskCode}/questions/active`, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 解决（回复）问题集 */
export async function resolveQuestions(
  taskCode: string,
  questionsCode: string,
  data: QuestionsResolveRequest,
  options?: { [key: string]: any },
) {
  return request<{ data: QuestionsResponse }>(
    `/api/tasks/${taskCode}/coordination/questions/${questionsCode}/resolve`,
    {
      method: 'POST',
      data,
      ...(options || {}),
    },
  );
}

/** 查询指令列表 */
export async function listInstructions(taskCode: string, options?: { [key: string]: any }) {
  return request<{ data: InstructionResponse[] }>(`/api/tasks/${taskCode}/coordination/instructions`, {
    method: 'GET',
    ...(options || {}),
  });
}

/** 创建指令 */
export async function createInstruction(
  taskCode: string,
  data: InstructionCreateRequest,
  options?: { [key: string]: any },
) {
  return request<{ data: InstructionResponse }>(`/api/tasks/${taskCode}/coordination/instructions`, {
    method: 'POST',
    data,
    ...(options || {}),
  });
}
