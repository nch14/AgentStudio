/** Profile 模块类型定义 */

export interface ProfileDto {
  displayName: string;
  email: string;
  timezone: string;
  locale: string;
  barkDeviceKey: string;
  bio: string;
}

export interface CreateProfileRequest {
  displayName: string;
  email: string;
  barkDeviceKey: string;
  bio: string;
  timezone: string;
  locale: string;
}

export interface UpdateProfileRequest {
  displayName: string;
  email: string;
  barkDeviceKey: string;
  bio: string;
}
