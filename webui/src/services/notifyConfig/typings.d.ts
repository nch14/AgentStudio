/** NotifyConfig 模块类型定义 */

export interface NotifyConfigDto {
  configCode: string;
  name: string;
  deliveryMode: 'INSTANT' | 'MERGED';
  channels: string;
  createTime: string;
  updateTime: string;
}

export interface NotifyConfigCreateRequest {
  name: string;
  deliveryMode: 'INSTANT' | 'MERGED';
  channels: string;
}

export interface NotifyConfigUpdateRequest {
  name?: string;
  deliveryMode?: 'INSTANT' | 'MERGED';
  channels?: string;
}

export interface NotifyConfigPageResponse {
  data: NotifyConfigDto[];
  total: number;
  page: number;
  size: number;
}
