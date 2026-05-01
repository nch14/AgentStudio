/** Provider 模块类型定义 */

export interface ConfigKeyDescriptor {
  provider: string;
  key: string;
  displayName: string;
  description: string;
  required: boolean;
  defaultValue: string;
}

export type AgentProvider = 'CLAUDE_CODE';

export interface ProviderSupports {
  provider: AgentProvider;
  supportChat: boolean;
  supportMessages: boolean;
  supportTask: boolean;
}
