import api from './config';
import type {TagVO, CreateTagRequest, UpdateTagRequest, UpdateFileTagsRequest} from '@/types';

export const tagService = {
    // 获取所有标签
    async getAllTags(): Promise<TagVO[]> {
        const response = await api.get<TagVO[]>('/api/tags');
        return response.data;
    },

    // 创建标签
    async createTag(request: CreateTagRequest): Promise<TagVO> {
        const response = await api.post<TagVO>('/api/tags', request);
        return response.data;
    },

    // 删除标签
    async deleteTag(id: number): Promise<void> {
        await api.delete(`/api/tags/${id}`);
    },

    // 获取文件的标签
    async getFileTags(path: string): Promise<TagVO[]> {
        const response = await api.get<TagVO[]>('/api/files/tags', {
            params: {path}
        });
        return response.data;
    },

    // 更新文件的标签
    async updateFileTags(path: string, tagIds: number[], fileHash?: string): Promise<void> {
        const request: UpdateFileTagsRequest = {
            tagIds,
            fileHash
        };
        await api.post('/api/files/tags', request, {
            params: {path}
        });
    },

    // 更新标签
    async updateTag(id: number, request: UpdateTagRequest): Promise<TagVO> {
        const response = await api.put<TagVO>(`/api/tags/${id}`, request);
        return response.data;
    }
}; 