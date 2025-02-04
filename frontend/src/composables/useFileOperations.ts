import type { FileOperationVO } from '@/api/fileOperationService';
import { fileOperationService } from '@/api/fileOperationService';
import { fileService } from '@/api/fileService';
import type { FileInfo, FileSortField } from '@/types';
import { FileListModeEnum } from "@/types";
import type { ActionSheetAction } from 'vant';
import { showDialog, showToast } from 'vant';
import { UploaderAfterRead } from "vant/lib/uploader/types";
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

export function useFileOperations(props: any, emit: any) {
    const router = useRouter()
    // 状态
    const operations = ref<FileOperationVO[]>([])
    const selectedFile = ref<FileInfo | null>(null)
    const actionSheetVisible = ref(false)
    const paramFormVisible = ref(false)
    const currentOperation = ref<FileOperationVO | null>(null)
    const isMultiSelectMode = ref(false)
    const files = ref<FileInfo[]>([])
    const loading = ref(false)
    const uploading = ref(false)
    const uploadProgress = ref(0)
    const createFolderVisible = ref(false)
    const newFolderName = ref('')
    const sortVisible = ref(false)
    const sortBy = ref<FileSortField>('name')
    const sortDesc = ref(false)
    const currentPage = ref(1)
    const hasMore = ref(true)
    const isLoadingFiles = ref(false)

    // 初始化加载文件
    const loadFiles = async (isLoadMore = false) => {
        if (isLoadingFiles.value) return;
        isLoadingFiles.value = true;

        try {
            let response;
            if (props.mode === FileListModeEnum.TAG_FILTER) {
                if (props.tagIds?.length) {
                    response = await fileService.getFilesContainAllTags(props.tagIds, {
                        page: currentPage.value,
                        size: 20
                    });
                } else {
                    return;
                }
            } else {
                response = await fileService.listFiles({
                    path: currentPath.value,
                    page: currentPage.value,
                    size: props.mode === FileListModeEnum.FOLDER_SELECT ? 1000 : 20
                });
            }

            const filteredRecords = props.mode === FileListModeEnum.FOLDER_SELECT
                ? response.records.filter(file => file.directory)
                : response.records;

            if (isLoadMore) {
                files.value = [...files.value, ...filteredRecords];
            } else {
                files.value = filteredRecords;
            }

            hasMore.value = props.mode === FileListModeEnum.FOLDER_SELECT ? false : currentPage.value < response.pages;
        } catch (error: any) {
            showToast(error.message || '加载失败')
        } finally {
            isLoadingFiles.value = false;
            loading.value = false;
        }
    }

    // 加载所有可用的文件操作类型
    const loadOperations = async () => {
        operations.value = await fileOperationService.getAllOperationTypes()
    }

    // 获取文件操作列表
    const getFileActions = (file: FileInfo | null, isSearchMode = false, searchMode = 'local'): ActionSheetAction[] => {
        if (!file) return []

        const actions: ActionSheetAction[] = []
        if (!isMultiSelectMode.value) {
            actions.push({ name: '多选模式', value: 'multiSelect' })
        } else {
            actions.push({ name: '取消多选', value: 'cancelMultiSelect' })
        }

        // 在全局搜索结果中添加"前往所在目录"选项
        if (isSearchMode && searchMode === 'remote') {
            actions.push({ name: '前往所在目录', value: 'goto' })
        }

        // 添加标签管理选项
        if (!isMultiSelectMode.value) {
            actions.push({ name: '管理标签', value: 'manageTags' })
        }

        // 添加下载和编辑选项（仅文件）
        if (!file.directory) {
            actions.push({ name: '下载', value: 'download' })
            if (fileService.isTextFile(file)) {
                actions.push({ name: '编辑', value: 'edit' })
            }
        }

        // 添加后端定义的文件操作
        let supportedOperations = fileOperationService.getSupportedOperations(operations.value, {
            name: file.name,
            directory: file.directory
        })

        if (isMultiSelectMode.value) {
            supportedOperations = supportedOperations.filter(op => op.supportMultiSelect)
        }

        supportedOperations.forEach(op => {
            actions.push({
                name: op.description,
                value: op.type,
                // 删除操作使用红色
                ...(op.type === 'DELETE' ? { color: '#ee0a24' } : {})
            })
        })

        return actions
    }

    // 执行文件操作
    const executeOperation = async (operation: FileOperationVO, params: Record<string, any>) => {
        // 定义危险操作类型
        const dangerousOperations = ['DELETE', 'CHANGE_OWNER']
        const selectedPaths = getSelectedPathsToParams(operation)
        const selectStr = selectedPaths.selectedPaths ? selectedPaths.selectedPaths.join('\n') : ''

        try {
            // 如果是危险操作，显示确认弹窗
            if (dangerousOperations.includes(operation.type)) {
                await showDialog({
                    title: '确认操作',
                    message: `确定要${operation.description}\n ${selectStr} 吗？`,
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    showCancelButton: true,
                })
            }

            // 用户确认后执行操作
            const executeMsg = await fileOperationService.executeOperation(operation.type, params)
            showToast(executeMsg)
            await loadFiles()
        } catch (error: any) {
            // 如果是用户取消操作，不显示错误提示
            if (error.toString().includes('cancel')) {
                return
            }
            showToast(error.response?.data?.message || `${operation.description}失败`)
            throw error
        }
    }

    // 处理文件下载
    const handleDownload = async (file: FileInfo) => {
        try {
            await fileService.downloadFile(file.path)
        } catch (error: any) {
            showToast(error.message || '下载失败')
        }
    }

    // 处理文件上传
    const handleUpload = async (file: File, currentPath: string) => {
        const hasInvalidChars = /[\\/:*?"<>|]/.test(file.name)
        if (hasInvalidChars) {
            showToast('文件名不能包含特殊字符')
            return
        }

        try {
            const path = currentPath === '/'
                ? `/${file.name}`
                : `${currentPath}/${file.name}`

            await fileService.uploadFile(file, path)
            showToast('上传成功')
            await loadFiles()
        } catch (error: any) {
            showToast(error.message || '上传失败')
            throw error
        }
    }

    // 创建文件夹
    const createFolder = async (folderName: string, currentPath: string) => {
        if (!folderName.trim()) {
            showToast('请输入文件夹名称')
            return
        }

        try {
            const newPath = currentPath === '/'
                ? `/${folderName}`
                : `${currentPath}/${folderName}`

            await fileService.createDirectory(newPath)
            showToast({
                type: 'success',
                message: '创建文件夹成功'
            })
            await loadFiles()
            return true
        } catch (error: any) {
            showToast({
                type: 'fail',
                message: error.message || '创建文件夹失败'
            })
            return false
        }
    }

    // 文件图标定义
    const FILE_ICONS = {
        IMAGE: 'material-symbols:image',
        VIDEO: 'material-symbols:video-file',
        AUDIO: 'material-symbols:audio-file',
        PDF: 'material-symbols:picture-as-pdf',
        WORD: 'vscode-icons:file-type-word',
        EXCEL: 'vscode-icons:file-type-excel',
        POWERPOINT: 'vscode-icons:file-type-powerpoint',
        JAVASCRIPT: 'logos:javascript',
        TYPESCRIPT: 'logos:typescript-icon',
        VUE: 'logos:vue',
        HTML: 'logos:html-5',
        CSS: 'logos:css-3',
        JAVA: 'logos:java',
        PYTHON: 'logos:python',
        PHP: 'logos:php',
        GO: 'logos:go',
        RUST: 'logos:rust',
        ARCHIVE: 'material-symbols:folder-zip',
        TEXT: 'material-symbols:description',
        FOLDER: 'material-symbols:folder',
        DEFAULT: 'material-symbols:file-present'
    } as const

    // 文件类型映射
    const EXTENSION_TYPE_MAP: Record<string, keyof typeof FILE_ICONS> = {
        // 图片文件
        ...['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].reduce((acc, ext) => ({ ...acc, [ext]: 'IMAGE' }), {}),
        // 视频文件
        ...['mp4', 'avi', 'mkv', 'mov', 'wmv'].reduce((acc, ext) => ({ ...acc, [ext]: 'VIDEO' }), {}),
        // 音频文件
        ...['mp3', 'wav', 'ogg', 'flac', 'm4a'].reduce((acc, ext) => ({ ...acc, [ext]: 'AUDIO' }), {}),
        // 文档文件
        pdf: 'PDF',
        ...['doc', 'docx'].reduce((acc, ext) => ({ ...acc, [ext]: 'WORD' }), {}),
        ...['xls', 'xlsx'].reduce((acc, ext) => ({ ...acc, [ext]: 'EXCEL' }), {}),
        ...['ppt', 'pptx'].reduce((acc, ext) => ({ ...acc, [ext]: 'POWERPOINT' }), {}),
        // 代码文件
        ...['js', 'jsx'].reduce((acc, ext) => ({ ...acc, [ext]: 'JAVASCRIPT' }), {}),
        ...['ts', 'tsx'].reduce((acc, ext) => ({ ...acc, [ext]: 'TYPESCRIPT' }), {}),
        vue: 'VUE',
        html: 'HTML',
        css: 'CSS',
        java: 'JAVA',
        py: 'PYTHON',
        php: 'PHP',
        go: 'GO',
        rs: 'RUST',
        // 压缩文件
        ...['zip', 'rar', '7z', 'tar', 'gz'].reduce((acc, ext) => ({ ...acc, [ext]: 'ARCHIVE' }), {}),
        // 文本文件
        ...['txt', 'log', 'md', 'json', 'xml', 'yaml', 'yml'].reduce((acc, ext) => ({ ...acc, [ext]: 'TEXT' }), {})
    }

    const getFileIcon = (file: FileInfo) => {
        if (file.directory) {
            return FILE_ICONS.FOLDER
        }

        const extension = file.name.split('.').pop()?.toLowerCase() || ''
        const type = EXTENSION_TYPE_MAP[extension] || 'DEFAULT'
        return FILE_ICONS[type]
    }


    const tagManagerVisible = ref(false)

    // 显示标签管理器
    const showTagManager = (file: FileInfo) => {
        selectedFile.value = file
        tagManagerVisible.value = true
    }


    const selectedFiles = ref<FileInfo[]>([])
    const isFileSelected = (file: FileInfo) => selectedFiles.value.some(f => f.path === file.path)


    // 处理参数表单提交
    const handleParamFormSubmit = async (formData: Record<string, any>) => {
        if (!currentOperation.value || !selectedFile.value) return

        try {
            // 获取参数表单数据
            const params = formData

            // 添加文件路径参数
            params.path = selectedFile.value.path
            // 执行操作
            await executeOperation(currentOperation.value, params)
            paramFormVisible.value = false
        } catch (error) {
            console.error('Operation failed:', error)
        }
    }

    const getSelectedPathsToParams = (operation: FileOperationVO | null): Record<string, any> => {
        if (operation?.supportMultiSelect) {
            let selectedPaths = []
            if (selectedFiles.value.length > 0) {
                selectedPaths = selectedFiles.value.map(file => file.path);
            } else {
                selectedPaths = [selectedFile.value?.path ?? ''];
            }
            return {
                selectedPaths: selectedPaths
            }
        }
        return {
            selectPath: selectedFile.value?.path ?? ''
        }
    }

    // 处理操作选择
    const handleActionSelect = async (action: { value: string }) => {
        if (!selectedFile.value) return

        try {
            // 处理前端特有的操作
            switch (action.value) {
                case 'manageTags':
                    showTagManager(selectedFile.value)
                    break
                case 'goto':
                    const parentPath = selectedFile.value.path.substring(0, selectedFile.value.path.lastIndexOf('/')) || '/'
                    navigateTo(parentPath)
                    break
                case 'download':
                    await handleDownload(selectedFile.value)
                    break
                case 'edit':
                    openEditor(selectedFile.value)
                    break
                case 'multiSelect':
                    isMultiSelectMode.value = true
                    break
                case 'cancelMultiSelect':
                    isMultiSelectMode.value = false
                    break
                default: {
                    // 查找是否是后端定义的操作
                    const operation = operations.value.find(op => op.type === action.value)
                    if (operation) {
                        let hasAdditionalParams = false
                        if (operation.supportMultiSelect) {
                            hasAdditionalParams = !!(operation.paramConfigs &&
                                operation.paramConfigs.length > 0 &&
                                operation.paramConfigs.some(param => param.key !== 'selectedPaths'))
                        } else {
                            hasAdditionalParams = !!(operation.paramConfigs &&
                                operation.paramConfigs.length > 0 &&
                                operation.paramConfigs.some(param => param.key !== 'selectPath'))
                        }

                        // 如果操作需要参数，显示参数收集对话框
                        if (hasAdditionalParams) {
                            currentOperation.value = operation
                            paramFormVisible.value = true
                        } else {
                            // 如果不需要参数，直接执行
                            await executeOperation(operation, getSelectedPathsToParams(operation))
                        }
                    } else {
                        showToast('操作类型不存在')
                    }
                    break
                }
            }
        } catch (error: any) {
            console.error('File operation failed:', error)
            showToast(error.response?.data?.message || '操作执行失败')
        }

        actionSheetVisible.value = false
    }

    const currentPath = ref(props.initialPath ||
        router.currentRoute.value.query.path as string ||
        localStorage.getItem('currentFilePath') ||
        '/'
    )

    // 修改 navigateTo 函数，添加历史记录
    const navigateTo = (path: string) => {
        currentPath.value = path
        currentPage.value = 1
        hasMore.value = true
        loading.value = false
        loadFiles()
        saveCurrentState()
        addToPathHistory(path)  // 添加到历史记录
        selectedFiles.value = []
    }


    const saveCurrentState = () => {
        if (!props.mode || props.mode === FileListModeEnum.NORMAL) {
            localStorage.setItem('currentFilePath', currentPath.value);
        }
    }

    // 历史记录管理
    const MAX_HISTORY_LENGTH = 10
    const PATH_HISTORY_KEY = 'path_history'
    const pathHistory = ref<string[]>([])

    // 加载历史记录
    const loadPathHistory = () => {
        const history = localStorage.getItem(PATH_HISTORY_KEY)
        if (history) {
            pathHistory.value = JSON.parse(history)
        }
    }


    // 添加历史记录
    const addToPathHistory = (path: string) => {
        // 如果是相同的路径，不添加
        if (pathHistory.value[0] === path) {
            return
        }

        // 从历史记录中移除相同的路径（如果存在）
        pathHistory.value = pathHistory.value.filter(p => p !== path)

        // 添加新路径到开头
        pathHistory.value.unshift(path)

        // 保持最大长度
        if (pathHistory.value.length > MAX_HISTORY_LENGTH) {
            pathHistory.value = pathHistory.value.slice(0, MAX_HISTORY_LENGTH)
        }

        // 保存到本地存储
        localStorage.setItem(PATH_HISTORY_KEY, JSON.stringify(pathHistory.value))
    }

    const navigateToPath = (index: number) => {
        const newPath = '/' + pathSegments.value.slice(0, index + 1).join('/')
        navigateTo(newPath)
    }

    // 计算属性
    const pathSegments = computed(() => {
        return currentPath.value.split('/').filter(Boolean)
    })


    // 导航相关
    const goBack = () => {
        // 如果设置了自定义返回，则触发返回事件
        if (props.customBack) {
            emit('back')
            return
        }

        // 如果是从导航进入的页面，则按照文件路径返回
        if (router.currentRoute.value.query.fromNav) {
            if (currentPath.value !== '/') {
                const parentPath = currentPath.value.substring(0, currentPath.value.lastIndexOf('/'))
                navigateTo(parentPath || '/')
                return
            }
        }

        // 其他情况直接返回上一页
        router.back()
    }

    // 加载更多
    const loadMore = async () => {
        if (!hasMore.value || isLoadingFiles.value || props.mode === FileListModeEnum.FOLDER_SELECT) return;
        currentPage.value++;
        await loadFiles(true);
    }


    // 编辑器相关
    const editorVisible = ref(false)
    const editingFile = ref<FileInfo | null>(null)

    const openEditor = (file: FileInfo) => {
        editingFile.value = file
        editorVisible.value = true
    }

    // 显示文件操作菜单
    const showFileActions = (file: FileInfo) => {
        selectedFile.value = file
        actionSheetVisible.value = true
    }


    // 修改创建文件夹函数
    const confirmCreateFolder = async () => {
        const success = await createFolder(newFolderName.value, currentPath.value)
        if (success) {
            createFolderVisible.value = false
            newFolderName.value = ''
        }
    }


    // 修改文件上传处理函数
    const handleUploadWrapper: UploaderAfterRead = (items) => {
        // 处理单个文件上传
        const file = Array.isArray(items) ? items[0]?.file : items.file
        if (!file) return

        uploading.value = true
        uploadProgress.value = 0

        handleUpload(file, currentPath.value)
            .then(() => {
                showToast('上传成功')
                loadFiles()
            })
            .catch((error: any) => {
                showToast(error.message || '上传失败')
            })
            .finally(() => {
                uploading.value = false
            })
    }


    // 添加全选相关的计算属性和方法
    const isAllSelected = computed(() => {
        return files.value.length > 0 && selectedFiles.value.length === files.value.length
    })

    const toggleSelectAll = () => {
        if (isAllSelected.value) {
            selectedFiles.value = []
        } else {
            selectedFiles.value = [...files.value]
        }
    }

    return {
        operations,
        selectedFile,
        actionSheetVisible,
        paramFormVisible,
        currentOperation,
        isMultiSelectMode,
        tagManagerVisible,
        files,
        loading,
        uploading,
        uploadProgress,
        createFolderVisible,
        newFolderName,
        sortVisible,
        sortBy,
        sortDesc,
        currentPage,
        hasMore,
        pathSegments,
        loadOperations,
        getFileActions,
        executeOperation,
        handleDownload,
        handleUpload,
        createFolder,
        getFileIcon,
        handleActionSelect,
        loadFiles,
        showTagManager,
        navigateTo,
        navigateToPath,
        goBack,
        currentPath,
        loadPathHistory,
        addToPathHistory,
        saveCurrentState,
        pathHistory,
        loadMore,
        editorVisible,
        editingFile,
        openEditor,
        showFileActions,
        confirmCreateFolder,
        handleUploadWrapper,
        selectedFiles,
        isFileSelected,
        handleParamFormSubmit,
        getSelectedPathsToParams,
        isAllSelected,
        toggleSelectAll
    }
} 