/** Task 模块类型 definition */

export interface TaskDetailResponse {
  taskCode: string;
  title: string;
  content: string;
  agentCode: string;
  sessionCode?: string;
  source: string;
  status: string;
  currentTurnCode?: string;
  currentTurnNo?: number;
  progress: number;
  resultSummary?: string;
  finishedAt?: string;
}

export interface TaskListItemResponse {
  taskCode: string;
  title: string;
  agentCode: string;
  source: string;
  status: string;
  progress: number;
  finishedAt?: string;
  createTime?: string;
  updateTime?: string;
}

/** 任务回合列表项 */
export interface TaskTurnListItemResponse {
  turnCode: string;
  turnNo: number;
  runStatus: string;
  finished: boolean;
  startedAt: string;
  finishedAt?: string;
  finalSummary?: string;
}

/** 任务回合详情 */
export interface TaskTurnDetailResponse extends TaskTurnListItemResponse {
  taskCode: string;
  finalDetail?: string;
}

/** 任务生成步骤（已更名为 Turn/回合） */
export interface TaskStepResponse {
  code: string;
  turnNo: number;
  taskCode: string;
  status: string;
  result?: string;
  createTime: string;
  updateTime: string;
}

/** 协同 - 问题集 */
export interface QuestionsResponse {
  code: string;
  resolved: boolean;
  openedAt: string;
  resolvedAt?: string;
  questions: QuestionResponse[];
  answers: AnswerResponse[];
}

/** 协同 - 单个问题 */
export interface QuestionResponse {
  code: string;
  text: string;
  options?: string[];
}

/** 协同 - 答案 */
export interface AnswerResponse {
  questionCode: string;
  selectedOption?: string;
  userInput?: string;
}

/** 协同 - 指令 */
export interface InstructionResponse {
  code: string;
  taskCode: string;
  turnCode: string;
  content: string;
  status: string;
  createdAt: string;
}

export interface TaskCreateRequest {
  title: string;
  agentCode: string;
  description?: string;
}

export interface TaskUpdateRequest {
  title?: string;
  description?: string;
}

export interface TaskCancelRequest {
  reason?: string;
}

export interface QuestionsResolveRequest {
  answers: AnswerResponse[];
}

export interface InstructionCreateRequest {
  turnCode: string;
  content: string;
}
