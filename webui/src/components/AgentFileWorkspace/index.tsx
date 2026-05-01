import { useState, useEffect, useCallback } from 'react';
import {
  Button, Input, message, Modal, Typography, Space, Spin, Tree, Empty, Tooltip, Form, Switch
} from 'antd';
import {
  FileMarkdownOutlined, SaveOutlined, PlusOutlined,
  FolderOpenOutlined, ReloadOutlined, ArrowLeftOutlined, MenuUnfoldOutlined
} from '@ant-design/icons';
import XMarkdown from '@ant-design/x-markdown';
import { listLocalFiles, readLocalFileContent, createLocalFile, writeLocalFileContent } from '@/services/agent/LocalFileController';
import type { LocalFileDetailDTO } from '@/services/agent/typings';
import './style.less';

const { Text } = Typography;

export interface FileWorkspaceProps {
  agentCode: string;
  rootPath: string;
  allowCreate?: boolean;
  allowEdit?: boolean;
  allowSave?: boolean;
  title?: string;
  height?: string | number;
  /** 紧凑模式：文件树与编辑器互斥显示，适用于侧边栏等窄空间场景 */
  compact?: boolean;
  /** 关闭回调，compact 模式下会在 header 显示关闭按钮 */
  onClose?: () => void;
}

interface TreeNodeFile {
  path: string;
  name: string;
  fileSize?: number;
  lastModifiedTime?: string;
}

interface TreeNode {
  title: React.ReactNode;
  key: string;
  isDir?: boolean;
  isLeaf?: boolean;
  file?: TreeNodeFile;
  children?: TreeNode[];
  nodeTitle?: string;
}

export default function AgentFileWorkspace({
  agentCode,
  rootPath,
  allowCreate = false,
  allowEdit = false,
  allowSave = false,
  title,
  height = '600px',
  compact = false,
  onClose
}: FileWorkspaceProps) {
  const [files, setFiles] = useState<LocalFileDetailDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedFile, setSelectedFile] = useState<TreeNodeFile | null>(null);
  const [fileContent, setFileContent] = useState('');
  const [fileContentOriginal, setFileContentOriginal] = useState('');
  const [fileSaving, setFileSaving] = useState(false);
  const [previewMode, setPreviewMode] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createForm] = Form.useForm();

  // Normalizing rootPath to ensure it ends with /
  const normalizedRootPath = rootPath.endsWith('/') ? rootPath : `${rootPath}/`;

  const fetchFiles = useCallback(async () => {
    if (!agentCode) return;
    setLoading(true);
    try {
      const res = await listLocalFiles(agentCode, { path: rootPath });
      setFiles(res.data || []);
    } catch (e) {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  }, [agentCode, rootPath]);

  useEffect(() => {
    fetchFiles();
  }, [fetchFiles]);

  const handleSelectFile = async (file: TreeNodeFile) => {
    setSelectedFile(file);
    // Automatically enable preview mode for .md files
    if (file.name.toLowerCase().endsWith('.md')) {
      setPreviewMode(true);
    } else {
      setPreviewMode(false);
    }
    try {
      const res = await readLocalFileContent(agentCode, file.path + file.name);
      setFileContent(res.data);
      setFileContentOriginal(res.data);
    } catch (e) {
      // handled
    }
  };

  /** compact 模式下返回文件列表 */
  const handleBackToList = () => {
    setSelectedFile(null);
    setFileContent('');
    setFileContentOriginal('');
  };

  const handleSaveFileContent = async () => {
    if (!agentCode || !selectedFile) return;
    setFileSaving(true);
    try {
      await writeLocalFileContent(agentCode, { path: selectedFile.path + selectedFile.name, content: fileContent });
      message.success('文件内容已保存');
      setFileContentOriginal(fileContent);
      fetchFiles();
    } finally {
      setFileSaving(false);
    }
  };

  const handleCreateFile = async () => {
    try {
      const values = await createForm.validateFields();
      // User inputs relative path like 'memory/daily/note.md'
      const userPath = values.path;
      const parts = userPath.replace(/\\/g, '/').split('/').filter(Boolean);
      const fileName = parts.pop() || 'untitled';
      const relativeDir = parts.length > 0 ? parts.join('/') : '';
      const fullPath = relativeDir
        ? `${normalizedRootPath}${relativeDir}`
        : normalizedRootPath;
      await createLocalFile(agentCode, { path: fullPath, name: fileName, content: values.content });
      message.success('文件已创建');
      setCreateModalOpen(false);
      createForm.resetFields();
      fetchFiles();
    } catch (e) {
      // handled
    }
  };

  const buildTree = (fileList: LocalFileDetailDTO[]) => {
    const treeData: TreeNode[] = [];

    fileList.forEach(file => {
      let relativePath = file.path;
      if (relativePath.startsWith(normalizedRootPath)) {
        relativePath = relativePath.substring(normalizedRootPath.length);
      } else if (relativePath.startsWith('/' + normalizedRootPath)) {
        relativePath = relativePath.substring(normalizedRootPath.length + 1);
      }

      const parts = relativePath.split('/').filter(Boolean);
      let currentLevel = treeData;

      parts.forEach((part, index) => {
        let node = currentLevel.find(n => n.nodeTitle === part && n.isDir);
        if (!node) {
          node = {
            title: (
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, textAlign: 'left' }}>
                <FolderOpenOutlined style={{ color: '#64748b' }} />
                <span style={{ fontSize: 13 }}>{part}</span>
              </div>
            ),
            nodeTitle: part, // For searching
            key: `dir-${parts.slice(0, index + 1).join('/')}`,
            isDir: true,
            children: [],
          };
          currentLevel.push(node);
        }
        currentLevel = node.children as TreeNode[];
      });

      // Use file.path + file.name as unique key (full logical path)
      const fullFilePath = file.path + file.name;
      if (!currentLevel.find(n => n.key === fullFilePath)) {
        currentLevel.push({
          title: (
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              width: '100%',
              overflow: 'hidden',
              textAlign: 'left'
            }}>
              <FileMarkdownOutlined style={{ color: '#6366f1', flexShrink: 0 }} />
              <span style={{
                flex: 1,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                fontSize: 13
              }}>
                {file.name}
              </span>
            </div>
          ),
          key: fullFilePath,
          isLeaf: true,
          file: { path: file.path, name: file.name, fileSize: file.fileSize, lastModifiedTime: file.lastModifiedTime },
        });
      }
    });

    return treeData;
  };

  const treeData = buildTree(files);
  const fileHasChanges = selectedFile && fileContent !== fileContentOriginal;

  // compact 模式：文件树与编辑器互斥显示
  const showEditor = compact ? !!selectedFile : true;
  const showSidebar = compact ? !selectedFile : true;

  return (
    <div className={`agent-file-workspace ${compact ? 'compact' : ''}`} style={{ height }}>
      {showSidebar && (
        <div className="workspace-sidebar" style={compact ? { width: '100%' } : undefined}>
          <div className="sidebar-header">
            <Text strong style={{ fontSize: 14 }}>{title || 'Files'}</Text>
            <Space>
              {allowCreate && (
                <Button
                  type="text"
                  size="small"
                  icon={<PlusOutlined />}
                  onClick={() => setCreateModalOpen(true)}
                />
              )}
              <Tooltip title="刷新列表">
                <Button
                  type="text"
                  size="small"
                  icon={<ReloadOutlined />}
                  onClick={fetchFiles}
                  loading={loading}
                />
              </Tooltip>
              {compact && onClose && (
                <Tooltip title="收起面板">
                  <Button
                    type="text"
                    icon={<MenuUnfoldOutlined />}
                    onClick={onClose}
                    style={{ fontSize: 18, color: '#666', width: 40, height: 40, borderRadius: 8 }}
                  />
                </Tooltip>
              )}
            </Space>
          </div>
          <div className="tree-container">
            {loading && files.length === 0 ? (
              <div className="loading-overlay"><Spin size="small" /></div>
            ) : files.length === 0 ? (
              <Empty description="暂无文件" image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ marginTop: 40 }} />
            ) : (
              <Tree.DirectoryTree
                showIcon={false}
                blockNode
                className="file-tree"
                treeData={treeData}
                onSelect={(keys, info: any) => {
                  if (info.node.isLeaf && info.node.file) {
                    handleSelectFile(info.node.file);
                  }
                }}
                selectedKeys={selectedFile ? [selectedFile.path] : []}
              />
            )}
          </div>
        </div>
      )}

      {showEditor && (
        <div className="workspace-editor" style={compact ? { width: '100%' } : undefined}>
          {selectedFile ? (
            <div className="editor-container">
              <div className="editor-header">
                <div className="file-info">
                  {compact && (
                    <Tooltip title="返回文件列表">
                      <Button
                        type="text"
                        size="small"
                        icon={<ArrowLeftOutlined />}
                        onClick={handleBackToList}
                        style={{ marginRight: 4 }}
                      />
                    </Tooltip>
                  )}
                  <Text strong>{selectedFile.name}</Text>
                </div>
                <Space>
                  {selectedFile.name.toLowerCase().endsWith('.md') && (
                    <Tooltip title={previewMode ? '查看源码' : '预览渲染'}>
                      <Switch
                        checked={previewMode}
                        onChange={(checked) => setPreviewMode(checked)}
                        checkedChildren="Markdown"
                        unCheckedChildren="源代码"
                      />
                    </Tooltip>
                  )}
                  {allowSave && !previewMode && (
                    <Button
                      type="primary"
                      icon={<SaveOutlined />}
                      size="small"
                      onClick={handleSaveFileContent}
                      loading={fileSaving}
                      disabled={!fileHasChanges}
                    >
                      保存修改
                    </Button>
                  )}
                  {compact && onClose && (
                    <Tooltip title="收起面板">
                      <Button
                        type="text"
                        icon={<MenuUnfoldOutlined />}
                        onClick={onClose}
                        style={{ fontSize: 18, color: '#666', width: 40, height: 40, borderRadius: 8 }}
                      />
                    </Tooltip>
                  )}
                </Space>
              </div>
              {previewMode ? (
                <div className="markdown-preview-area">
                  <XMarkdown>{fileContent}</XMarkdown>
                </div>
              ) : (
                <Input.TextArea
                  className="editor-textarea"
                  value={fileContent}
                  onChange={(e) => setFileContent(e.target.value)}
                  readOnly={!allowEdit}
                  style={{ fontFamily: 'monospace' }}
                />
              )}
            </div>
          ) : (
            <div className="editor-empty">
              <FileMarkdownOutlined style={{ fontSize: 48, color: '#e5e7eb', marginBottom: 16 }} />
              <Text type="secondary">选择一个文件进行查看或编辑</Text>
            </div>
          )}
        </div>
      )}

      <Modal
        title="新建文件"
        open={createModalOpen}
        onOk={handleCreateFile}
        onCancel={() => setCreateModalOpen(false)}
        okButtonProps={{ style: { borderRadius: 8 } }}
        cancelButtonProps={{ style: { borderRadius: 8 } }}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="path" label="文件路径" rules={[{ required: true, message: '请输入文件路径' }]}>
            <Input
              addonBefore={<span style={{ fontSize: 12, color: '#64748b', whiteSpace: 'nowrap' }}>{rootPath}/</span>}
              placeholder="例如：memory/daily/note.md"
            />
          </Form.Item>
          <Form.Item name="content" label="初始内容" rules={[{ required: true, message: '请输入文件内容' }]}>
            <Input.TextArea rows={8} style={{ fontFamily: 'monospace' }} placeholder="文件内容..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
