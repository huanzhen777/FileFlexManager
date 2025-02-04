import api from './config';
import { showToast } from 'vant';

import { TaskParamConfig } from "@/types/task.ts";

export interface FileOperationVO {
    type: 'COPY' | 'MOVE' | 'COMPRESS' | 'DECOMPRESS' | 'FILE_CHANGE_OWNER' | 'DELETE';
    description: string;
    supportFile: boolean;
    supportDirectory: boolean;
    supportedExtensions: string[] | null;
    isTask: boolean;
    paramConfigs: TaskParamConfig[] | null;
    isSync: boolean;
    supportMultiSelect: boolean;
}

export const fileOperationService = {
    // 获取所有可用的文件操作类型
    async getAllOperationTypes(): Promise<FileOperationVO[]> {
        const response = await api.get<FileOperationVO[]>('/api/file-operations/types');
        return response.data;
    },

    // 执行文件操作
    async executeOperation(operationType: string, params: Record<string, any>): Promise<string> {
        try {
            const response = await api.post(`/api/file-operations/${operationType}/execute`, params);
            return response.data;
        } catch (error: any) {
            showToast(error.response?.data?.message || '操作执行失败');
            throw error;
        }
    },

    // 获取文件支持的操作类型
    getSupportedOperations(operations: FileOperationVO[], file: {
        name: string,
        directory: boolean
    }): FileOperationVO[] {
        return operations.filter(op => {
            // 检查是否支持文件/目录
            if (file.directory && !op.supportDirectory) return false;
            if (!file.directory && !op.supportFile) return false;

            // 如果没有指定支持的扩展名，则支持所有类型
            if (!op.supportedExtensions) return true;

            // 如果是目录，且操作支持目录，则返回true
            if (file.directory && op.supportDirectory) return true;

            // 检查文件扩展名
            const fileExt = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
            return op.supportedExtensions.some(ext => ext.toLowerCase() === fileExt);
        });
    }
}; 