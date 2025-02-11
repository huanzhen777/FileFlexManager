import api, {Page} from './config';
import {CreateScheduledTaskRequest, Task, TaskQueryParams, TaskType, UpdateScheduledTaskRequest} from "@/types/task.ts";

export const taskService = {
    // 获取所有运行中的任务
    async getRunningTasks(): Promise<Task[]> {
        const response = await api.get<Task[]>('/api/tasks/running');
        return response.data;
    },

    // 获取任务详情
    async getTaskProgress(taskId: number): Promise<Task> {
        const response = await api.get<Task>(`/api/tasks/${taskId}`);
        return response.data;
    },

    // 取消任务
    async cancelTask(taskId: number): Promise<boolean> {
        const response = await api.post(`/api/tasks/${taskId}/cancel`);
        return response.data;
    },

    // 获取所有任务（支持分页）
    async getAllTasks(params: TaskQueryParams): Promise<Page<Task>> {
        return api.get('/api/tasks', {params}).then(res => res.data);
    },

    async getScheduledTasks(params: TaskQueryParams): Promise<Page<Task>> {
        return api.get('/api/tasks/getScheduledTasks', {params}).then(res => res.data);
    },

    /**
     * 创建定时任务
     */
    async createScheduledTask(request: CreateScheduledTaskRequest): Promise<Task> {
        const response = await api.post('/api/tasks/scheduled', request);
        return response.data;
    },

    /**
     * 启用定时任务
     */
    async enableScheduledTask(taskId: number): Promise<Task> {
        const response = await api.post(`/api/tasks/scheduled/${taskId}/enable`);
        return response.data;
    },

    /**
     * 禁用定时任务
     */
    async disableScheduledTask(taskId: number): Promise<Task> {
        const response = await api.post(`/api/tasks/scheduled/${taskId}/disable`);
        return response.data;
    },


    /**
     * 获取支持定时任务的任务类型
     */
    async getScheduledTaskTypes(): Promise<TaskType[]> {
        const response = await api.get('/api/tasks/scheduled/types');
        return response.data;
    },

    // 添加删除定时任务的方法
    async deleteScheduledTask(id: number): Promise<void> {
        await api.delete(`/api/tasks/scheduled/${id}`);
    },

    /**
     * 更新定时任务
     */
    async updateScheduledTask(request: UpdateScheduledTaskRequest): Promise<Task> {
        const response = await api.post(`/api/tasks/updateScheduledTask`, request);
        return response.data;
    }
}; 