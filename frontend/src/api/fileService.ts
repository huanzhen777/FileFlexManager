import api, {Page} from './config';
import {showToast} from 'vant';
import type {FileInfo, FileListParams, SearchParams} from '@/types';

export const fileService = {
    // 获取文件列表
    async listFiles(params: FileListParams): Promise<Page<FileInfo>> {
        const response = await api.post<Page<FileInfo>>('/api/files/queryFiles', params);
        return response.data
    },

    // 获取系统用户列表
    async getSystemUsers(): Promise<string[]> {
        const response = await api.get<string[]>('/api/files/system-users')
        return response.data
    },

    // 创建文件夹
    createDirectory(path: string) {
        return api.post('/api/files/mkdir', null, {
            params: {path}
        });
    },

    // 获取文件内容
    async getFileContent(path: string): Promise<{ data: string }> {
        const response = await api.get<string>('/api/files/content', {
            params: {path}
        })
        return {data: response.data}
    },

    // 保存文件内容
    async saveFileContent(path: string, content: string): Promise<void> {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 30000);

        await api.post('/api/files/content', content, {
            params: {path},
            headers: {'Content-Type': 'text/plain'},
            signal: controller.signal
        }).finally(() => {
            clearTimeout(timeoutId);
        });
    },

    // 上传文件
    async uploadFile(file: File, path: string, options?: {
        onProgress?: (progress: number) => void;
        onSuccess?: () => void;
        onError?: (error: Error) => void;
        signal?: AbortSignal;
    }) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('path', path);
        try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 300000); // 5分钟超时

            const response = await api.post('/api/files/upload', formData, {
                headers: {'Content-Type': 'multipart/form-data'},
                signal: options?.signal || controller.signal,
                onUploadProgress: (progressEvent) => {
                    const percentCompleted = Math.round((progressEvent.loaded * 100) / (progressEvent.total || progressEvent.loaded));
                    options?.onProgress?.(percentCompleted);
                }
            });

            clearTimeout(timeoutId);
            options?.onSuccess?.();
            return response;
        } catch (error: any) {
            if (error.name === 'AbortError') {
                throw new Error('上传超时，请重试');
            }
            options?.onError?.(error);
            throw error;
        }
    },

    // 下载文件
    async downloadFile(path: string) {
        try {
            const token = localStorage.getItem('token');
            const encodedPath = encodeURIComponent(path);
            const downloadUrl = `${import.meta.env.VITE_API_URL}/api/files/download?path=${encodedPath}&authorization=${token}`;

            // 使用window.open直接触发下载
            window.open(downloadUrl, '_blank');

        } catch (error: any) {
            console.error('下载失败:', error);
            showToast({
                type: 'fail',
                message: '下载失败：' + (error.message || '未知错误')
            });
            throw error;
        }
    },

    // 修改文件所有者
    async changeOwner(path: string, owner: string): Promise<void> {
        await api.post('/api/files/change-owner', null, {
            params: {path, owner}
        })
    },

    async searchFiles(params: SearchParams): Promise<Page<FileInfo>> {
        const response = await api.get<Page<FileInfo>>('/api/files/search', {
            params: {
                keyword: params.keyword,
                page: params.page || 1,
                size: params.size || 20
            }
        })
        return response.data
    },

    // 根据标签获取文件
    async getFilesContainAllTags(tagIds: number[], params: { page: number; size: number }): Promise<Page<FileInfo>> {
        const response = await api.get('/api/files/get-files-contain-all-tags', {
            params: {
                tagIds: tagIds.join(','),
                page: params.page,
                size: params.size
            }
        })
        return response.data
    },

    async getFilesContainAnyTags(tagIds: number[], params: { page: number; size: number }): Promise<Page<FileInfo>> {
        const response = await api.get('/api/files/get-files-contain-any-tags', {
            params: {
                tagIds: tagIds.join(','),
                page: params.page,
                size: params.size
            }
        })
        return response.data
    },

    formatFileSize(size: number): string {
        if (size < 1024) return size + ' B';
        if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB';
        if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(1) + ' MB';
        return (size / 1024 / 1024 / 1024).toFixed(1) + ' GB';
    },

    isTextFile(file: FileInfo): boolean {
        if (file.directory) return false
        const textExtensions = ['.txt', '.md', '.json', '.yml', '.yaml', '.xml',
            '.js', '.ts', '.css', '.html', '.conf', '.log',
            '.properties', '.sh', '.py', '.java']
        return textExtensions.some(ext => file.name.toLowerCase().endsWith(ext))
    }
}; 