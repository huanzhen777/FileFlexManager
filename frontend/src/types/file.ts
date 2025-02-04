// 文件基本信息类型
export interface FileInfo {
    name: string;
    path: string;
    size: number;
    lastModified: number;
    directory: boolean;
    owner: string;
    tags?: TagVO[];
}

// 标签类型
export interface TagVO {
    id: number;
    name: string;
    parentId: number | null;
    path: string;
    quickAccess: boolean;
    bindFile: boolean;
    children?: TagVO[];
    fileCount: number;
}

// 文件操作类型
export interface FileOperation {
    type: string;
    name: string;
    icon?: string;
    paramConfigs?: OperationParamConfig[];
}

// 操作参数配置类型
export interface OperationParamConfig {
    key: string;
    name: string;
    type: 'text' | 'number' | 'select' | 'path';
    required?: boolean;
    options?: { label: string; value: any }[];
    defaultValue?: any;
}

// 文件列表请求参数类型
export interface FileListParams {
    path: string;
    page: number;
    size: number;
}

// 文件列表响应类型
export interface FileListResponse {
    records: FileInfo[];
    total: number;
    size: number;
    current: number;
    pages: number;
}

// 更多操作菜单项类型
export interface MoreAction {
    name: string;
    icon: string;
}

// 排序动作类型
export interface SortAction {
    name: string;
    value: string;
}

// 文件排序类型
export type FileSortField = 'name' | 'size' | 'lastModified';

// 文件操作结果类型
export interface FileOperationResult {
    success: boolean;
    message?: string;
    data?: any;
}

// 搜索参数类型
export interface SearchParams {
    keyword: string;
    page?: number;
    size?: number;
}

// 标签相关请求类型
export interface CreateTagRequest {
    name: string;
    parentId: number | null;
    quickAccess: boolean;
    bindFile: boolean;
}

export interface UpdateTagRequest {
    name: string;
    quickAccess: boolean;
    bindFile: boolean;
}

export interface UpdateFileTagsRequest {
    tagIds: number[];
    fileHash?: string;
}

// 工具函数
// 定义模式配置类
export class ModeConfig {
    constructor({
                    showSearch = true,
                    showBack = true,
                    showTags = true,
                    showMoreActions = true,
                    showBreadcrumb = true,
                    showHistoryButton = false,
                    title = '',
                    showNavBar = true,
                } = {}) {
        this.showSearch = showSearch;
        this.showBack = showBack;
        this.showTags = showTags;
        this.showMoreActions = showMoreActions;
        this.showBreadcrumb = showBreadcrumb;
        this.title = title;
        this.showNavBar = showNavBar;
        this.showHistoryButton = showHistoryButton;
    }

    showNavBar: boolean;
    showHistoryButton: boolean;
    showSearch: boolean;
    showBack: boolean;
    showTags: boolean;
    showMoreActions: boolean;
    showBreadcrumb: boolean;
    title: string;
}

// 定义枚举类型
export enum FileListModeEnum {
    NORMAL = 'NORMAL',
    FOLDER_SELECT = 'FOLDER_SELECT',
    TAG_FILTER = 'TAG_FILTER',
}

// 定义模式配置映射
export const MODE_CONFIGS: Record<FileListModeEnum, ModeConfig> = {
    [FileListModeEnum.NORMAL]: new ModeConfig(),
    [FileListModeEnum.FOLDER_SELECT]: new ModeConfig({
        title: '选择文件夹',
        showHistoryButton: true
    }),
    [FileListModeEnum.TAG_FILTER]: new ModeConfig({
        showSearch: false,
        showMoreActions: false,
        showBreadcrumb: false,
        showNavBar: false,
        title: '标签文件'
    })
} as const;

export function getModeConfig(mode: FileListModeEnum): ModeConfig {
    return MODE_CONFIGS[mode];
}