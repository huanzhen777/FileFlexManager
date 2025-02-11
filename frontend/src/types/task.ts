export interface Task {
    id: number;
    type: string;
    typeDesc: string;
    status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
    progress: number;
    message: string;
    desc: string;
    createTime: number;
    updateTime: number;
    beginTime?: number;
    endTime?: number;
    scheduled?: boolean;
    cronExpression?: string;
    enabled?: boolean;
    nextExecuteTime?: number;
    executeCount?: number;
    lastExecuteTime?: number;
}

export interface TaskQueryParams {
    page: number;
    size: number;
    includeCompleted?: boolean;
}

export interface CreateScheduledTaskRequest {
    type: string;
    payload: Record<string, any>;
    cronExpression: string;
    desc?: string;
}

export interface UpdateScheduledTaskRequest {
    id: number;
    type: string;
    payload: Record<string, any>;
    cronExpression: string;
    desc?: string;
}

export interface TaskParamOption {
    label: string;
    value: string;
}

export interface TaskParamConfig {
    name: string;
    key: string;
    type: 'FOLDER' | 'FILE' | 'TEXT' | 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'FOLDER_FILE' | 'LIST' | 'FOLDER_MULTI_SELECT' | 'FOLDER_FILE_MULTI_SELECT';
    required: boolean;
    description: string;
    defaultValue?: any;
    options?: TaskParamOption[];
    paramConfigs?: TaskParamConfig[];
}

export interface TaskType {
    type: string;
    description: string;
    supportScheduled: boolean;
    paramConfigs: TaskParamConfig[];
    manuallyAdd: boolean;
}