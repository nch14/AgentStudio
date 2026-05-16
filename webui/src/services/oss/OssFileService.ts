/* eslint-disable */
import { request } from '@umijs/max';

export interface OssTarget {
  uploadUrl: string;
  objectKey: string;
  publicUrl: string;
  contentType: string;
}

/** 申请 OSS 预签名上传地址 */
export async function getUploadUrl(
  filename: string,
  contentType?: string,
  agentCode?: string,
  dir?: string,
  options?: { [key: string]: any },
) {
  return request<OssTarget>('/api/files/upload-url', {
    method: 'GET',
    params: { filename, contentType, agentCode, dir },
    ...(options || {}),
  });
}

/** 将文件直传 OSS */
export async function uploadToOss(uploadUrl: string, file: File, contentType: string) {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': contentType },
    body: file,
  });
  if (!response.ok) {
    throw new Error(`OSS upload failed: ${response.status}`);
  }
}
