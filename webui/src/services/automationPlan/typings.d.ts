/** AutomationPlan 模块类型定义 */

export interface AutomationPlanListItemDTO {
  planCode: string;
  name: string;
  agentCode: string;
  scheduleRule: string;
  deliveryMode: string;
  status: string;
  createTime: string;
  updateTime: string;
}

export interface AutomationPlanDetailDTO extends AutomationPlanListItemDTO {
  instruction: string;
}

export interface AutomationPlanCreateRequest {
  name: string;
  agentCode: string;
  instruction: string;
  scheduleRule: string;
  deliveryMode: string;
}

export interface AutomationPlanUpdateRequest {
  name?: string;
  instruction?: string;
  scheduleRule?: string;
  deliveryMode?: string;
}
