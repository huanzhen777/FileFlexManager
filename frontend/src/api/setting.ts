import api from './config'

import {TaskParamConfig} from "@/types/task.ts";

// 定义配置项接口
export interface ConfigItem {
    name: string
    title: string
    description: string
    value: any
    type: string
    typeDesc: string;
    paramConfig: TaskParamConfig;
}

// 获取配置列表
export function getConfigs() {
    return api.get<ConfigItem[]>('/api/configs')
}

// 更新配置
export interface UpdateConfigParams {
    name: string
    value: string
}

export function updateConfig(data: UpdateConfigParams) {
    return api.put<void>('/api/configs', data)
} 