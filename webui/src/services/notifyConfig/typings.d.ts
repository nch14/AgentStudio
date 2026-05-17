/** NotifyConfig 模块类型定义 */

export interface NotifyConfigItem {
  groupCode: string;
  groupName: string;
  eventCode: string;
  eventName: string;
  description: string;
  enabled: boolean;
  deliveryMode: 'INSTANT' | 'MERGED';
  channels: string[];
  configured: boolean;
}

export interface NotifyConfigUpdateRequest {
  enabled?: boolean;
  deliveryMode?: 'INSTANT' | 'MERGED';
  channels?: string[];
}
