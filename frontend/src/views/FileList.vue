<template>
  <div class="file-list">
    <!-- 导航栏 -->
    <van-nav-bar
        v-if="currentModeConfig.showNavBar"
        :title="currentModeConfig.title || (currentPath === '/' ? '根目录' : currentPath.split('/').pop())"
        :left-text="currentModeConfig.showBack ? '返回' : ''"
        :left-arrow="currentModeConfig.showBack"
        fixed
        @click-left="goBack">
      <template #right>
       
        <!-- 历史记录按钮（仅在文件夹选择模式下显示） -->
        <van-button
            v-if="currentModeConfig.showHistoryButton"
            icon="clock-o"
            size="small"
            @click="showHistoryPopup"
        />
        <!-- 标签浏览按钮 -->
        <van-button
            v-if="currentModeConfig.showTags"
            icon="label-o"
            size="small"
            @click="goToTagFiles"
        />
        <!-- 搜索按钮 -->
        <van-button
            v-if="currentModeConfig.showSearch"
            icon="search"
            size="small"
            @click="showSearchPopup"
        />
        <!-- 隐藏的上传组件 -->
        <van-uploader :after-read="handleUploadWrapper" :disabled="uploading" style="display: none">
          <div ref="uploadTrigger"></div>
        </van-uploader>
        <!-- 更多菜单按钮 -->
        <van-button
            v-if="currentModeConfig.showMoreActions"
            icon="ellipsis"
            size="small"
            @click="showMoreActions"
        />
      </template>
    </van-nav-bar>

    <!-- 集成文件搜索组件 -->
    <FileSearch
        ref="fileSearchRef"
        :show-nav-bar="currentModeConfig.showNavBar"
        :show-breadcrumb="currentModeConfig.showBreadcrumb"
        @update:files="handleSearchFilesUpdate"
        @navigate="handleSearchNavigate"
    />

    <!-- 面包屑导航 (仅在非标签过滤模式下显示) -->
    <div class="breadcrumb" v-if="!fileSearchRef?.isSearchMode && currentModeConfig.showBreadcrumb">
      <div class="breadcrumb-container">
        <van-space wrap class="path-segments">
          <van-tag v-for="(segment, index) in pathSegments" :key="index" plain type="primary" size="medium"
                   @click="navigateToPath(index)">
            {{ segment || '根目录' }}
          </van-tag>
        </van-space>
        <!-- 全选按钮和选中数量（仅在多选模式下显示） -->
        <div v-if="isMultiSelectMode" class="select-all-container">
          <van-space>
            <van-button v-if="props.mode !== FileListModeEnum.FOLDER_SELECT" icon="close" type="primary" 
            @click="isMultiSelectMode = false" size="small" />
            <van-checkbox v-model="isAllSelected" label-position="left"
            @click="toggleSelectAll" >已选 {{ selectedFiles.length }}/{{ files.length }}</van-checkbox>
          </van-space>
        </div>
      </div>
    </div>

    <!-- 文件列表 -->
    <div class="task-list">
      <van-list
          v-model:loading="loading"
          :finished="!hasMore"
          finished-text="没有更多了"
          @load="loadMore"
          :offset="10"
          :immediate-check="false"
          error-text="请求失败，点击重新加载"
      >
        <van-cell-group>
          <van-swipe-cell v-for="file in files" :key="file.path">
            <van-cell
                :class="[getCellClass(file), 'cell-ellipsis']"
                :title="file.name"
                @click="handleFileClick(file)"
                @contextmenu.prevent="showFileActions(file)"
                @touchstart="handleTouchStart(file, $event)"
                @touchend="handleTouchEnd"
                @touchmove="handleTouchMove"
            >
              <template #icon>
                <Icon :icon="getFileIcon(file)" class="file-icon" :class="file.directory ? 'folder' : ''"/>
              </template>
              <template #label>
                <div class="file-info">
                  <div>{{ formatFileInfo(file) }}</div>
                </div>
              </template>
              <template #value>
                <div class="file-tags" v-if="file.tags?.length">
                  <van-tag
                      v-for="tag in file.tags"
                      :key="tag.id"
                      type="primary"
                      plain
                  >
                    {{ tag.name }}
                  </van-tag>
                </div>
              </template>

              <!-- 添加复选框 -->
              <template #right-icon>
                <van-checkbox
                    v-if="isMultiSelectMode"
                    :name="file.path"
                    :modelValue="isFileSelected(file)"
                    @click.stop="handleCheckboxChange(file)"
                    :disabled="props.mode === FileListModeEnum.FOLDER_SELECT && !file.directory && !props.allowFile"
                />
              </template>
            </van-cell>
            <template #right>
              <van-button
                  square
                  type="primary"
                  text="标签"
                  class="swipe-btn"
                  @click="showTagManager(file)"
              />
            </template>
          </van-swipe-cell>
        </van-cell-group>
      </van-list>
    </div>

    <!-- 新建文件夹对话框 -->
    <van-dialog v-model:show="createFolderVisible" title="新建文件夹" show-cancel-button @confirm="confirmCreateFolder">
      <van-field v-model="newFolderName" placeholder="请输入文件夹名称"
                 :rules="[{ required: true, message: '请输入文件夹名称' }]"/>
    </van-dialog>

    <!-- 文件操作菜单 -->
    <van-action-sheet v-model:show="actionSheetVisible" :actions="getFileActions(selectedFile)" cancel-text="取消"
                      @select="handleActionSelect"/>

    <!-- 上传进度 -->
    <van-overlay :show="uploading" :z-index="2000">
      <div class="upload-progress">
        <van-circle v-model:current="uploadProgress" :speed="100" :text="`上传中 ${uploadProgress}%`"/>
      </div>
    </van-overlay>

    <!-- 文本编辑器 -->
    <TextEditor v-model:visible="editorVisible" :file-path="editingFile?.path" :file-name="editingFile?.name"/>

    <!-- 排序选项 -->
    <van-action-sheet v-model:show="sortVisible" :actions="sortActions" cancel-text="取消" @select="handleSortSelect"/>


    <!-- 所有者选择器 -->
    <van-dialog v-model:show="ownerPickerVisible" title="修改所有者" show-cancel-button
                :before-close="handleOwnerDialogClose" @confirm="handleOwnerChange" class="owner-dialog">
      <div class="owner-list">
        <van-radio-group v-model="selectedOwner">
          <van-cell-group>
            <van-cell v-for="user in systemUsers" :key="user" :title="user" clickable @click="selectedOwner = user">
              <template #right-icon>
                <van-radio :name="user"/>
              </template>
            </van-cell>
          </van-cell-group>
        </van-radio-group>
      </div>
    </van-dialog>

    <!-- 标签管理弹窗 -->
    <van-popup
        v-model:show="tagManagerVisible"
        position="bottom"
        round
        :style="{ height: '60%' }"
        class="tag-manager-popup"
    >
      <TagTreeBrowser
          :show-search="true"
          :show-breadcrumb="false"
          :selectable="true"
          :show-nav-bar="true"
          :title="`管理${selectedFile?.name || ''}的标签`"
          left-text="取消"
          right-text="确定"
          :file-path="selectedFile?.path"
          @nav-left="tagManagerVisible = false"
          @nav-right="tagManagerVisible = false"
          @success="loadFiles"
          class="tag-browser"
      />
    </van-popup>

    <!-- 更多操作菜单 右上角--- 菜单 -->
    <van-action-sheet
        v-model:show="moreActionsVisible"
        :actions="moreActions"
        cancel-text="取消"
        close-on-click-action
        @select="handleMoreAction"
    />

    <!-- 添加参数表单相关状态 -->
    <van-dialog v-model:show="paramFormVisible" title="输入操作参数"
                :show-cancel-button="false" :show-confirm-button="false">
      <TaskParamForm
          ref="paramFormRef"
          :params="currentOperation?.paramConfigs || []"
            :default-values="getSelectedPathsToParams(currentOperation)"
          @cancel="paramFormVisible=false"
          :show-actions="true"
          @confirm="handleParamFormSubmit"
      />
    </van-dialog>

    <!-- 添加历史记录弹出层 -->
    <van-popup
        v-model:show="historyVisible"
        position="bottom"
        round
        :style="{ height: '40%' }"
        class="history-popup"
    >
      <div class="history-manager">
        <van-nav-bar
            title="访问历史"
            left-text="关闭"
            @click-left="historyVisible = false"
        />
        <div class="history-list">
          <van-cell-group>
            <van-cell
                v-for="(path, index) in pathHistory"
                :key="index"
                :title="path === '/' ? '根目录' : path.split('/').pop()"
                :label="path"
                @click="navigateToHistory(path)"
                clickable
            >
              <template #icon>
                <van-icon name="folder-o" class="history-icon"/>
              </template>
            </van-cell>
          </van-cell-group>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {showToast} from 'vant'
import type {FileInfo, FileSortField, MoreAction, SortAction} from '@/types'
import {FileListModeEnum, getModeConfig} from '@/types'
import {fileService} from '@/api/fileService'
import {Icon} from '@iconify/vue'
import {useFileOperations} from '@/composables/useFileOperations'
import {FileSearch, ParamForm as TaskParamForm, TagTreeBrowser, TextEditor} from '@/components'

const router = useRouter()

// 修改 props
const props = withDefaults(defineProps<{
  mode?: FileListModeEnum
  initialPath?: string
  tagIds?: number[]
  customBack?: boolean
  multiSelect?: boolean
  allowFile?: boolean // 新增：是否允许选择文件
}>(), {
  mode: FileListModeEnum.NORMAL,
  initialPath: undefined,
  tagIds: () => [],
  customBack: false,
  multiSelect: true,
  allowFile: true
})

const emit = defineEmits<{
  (e: 'select', path: string, selected: boolean): void
  (e: 'file-click', file: FileInfo): void
  (e: 'back'): void  // 添加返回事件
}>();

// 获取当前模式的配置
const currentModeConfig = computed(() => getModeConfig(props.mode))




const {
  actionSheetVisible,
  selectedFile,
  paramFormVisible,
  currentOperation,
  isMultiSelectMode,
  loadOperations,
  getFileActions,
  getFileIcon,
  loadFiles,
  showTagManager,
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
  navigateTo,
  navigateToPath,
  goBack,
  currentPath,
  loadPathHistory,
  pathHistory,
  loadMore,
  editorVisible,
  editingFile,
  openEditor,
  handleActionSelect,
  showFileActions,
  confirmCreateFolder,
  handleUploadWrapper,
  selectedFiles,
  isFileSelected,
  handleParamFormSubmit,
  getSelectedPathsToParams,
  isAllSelected,
  toggleSelectAll
} = useFileOperations(props, emit)

const paramFormRef = ref<InstanceType<typeof TaskParamForm> | null>(null)

// 排序选项
const sortActions: SortAction[] = [
  {name: '按名称升序', value: 'name-asc'},
  {name: '按名称降序', value: 'name-desc'},
  {name: '按大小升序', value: 'size-asc'},
  {name: '按大小降序', value: 'size-desc'},
  {name: '按修改时间升序', value: 'lastModified-asc'},
  {name: '按修改时间降序', value: 'lastModified-desc'},
]

const handleCheckboxChange = (file: FileInfo) => {
  //if (props.mode === FileListModeEnum.FOLDER_SELECT && !file.directory && !props.allowFile) return // 只有在允许选择文件时才能选择文件
  
  const index = selectedFiles.value.findIndex(f => f === file)
  if (index !== -1) {
    selectedFiles.value.splice(index, 1)
    emit('select', file.path, false)
  } else {
    if (!props.multiSelect && selectedFiles.value.length > 0) {
      // 单选模式下，先取消之前的选择
      const prevFile = selectedFiles.value[0]
      selectedFiles.value = []
      emit('select', prevFile.path, false)
    }
    selectedFiles.value.push(file)
    emit('select', file.path, true)
  }
}


// 长按相关
let longPressTimer: number | null = null
let touchStartX = 0
let touchStartY = 0
const LONG_PRESS_DURATION = 500  // 长按触发时间（毫秒）
const TOUCH_MOVE_THRESHOLD = 10  // 触摸移动阈值（像素）

// 处理触摸开始
const handleTouchStart = (file: FileInfo, event: TouchEvent) => {
  touchStartX = event.touches[0].clientX
  touchStartY = event.touches[0].clientY

  longPressTimer = window.setTimeout(() => {
    showFileActions(file)
  }, LONG_PRESS_DURATION)
}

// 处理触摸结束
const handleTouchEnd = () => {
  if (longPressTimer) {
    clearTimeout(longPressTimer)
    longPressTimer = null
  }
}

// 处理触摸移动
const handleTouchMove = (event: TouchEvent) => {
  if (!longPressTimer) return

  const moveX = Math.abs(event.touches[0].clientX - touchStartX)
  const moveY = Math.abs(event.touches[0].clientY - touchStartY)

  // 如果移动超过阈值，取消长按
  if (moveX > TOUCH_MOVE_THRESHOLD || moveY > TOUCH_MOVE_THRESHOLD) {
    clearTimeout(longPressTimer)
    longPressTimer = null
  }
}

// 文件操作
const handleFileClick = (file: FileInfo) => {
  // 先检查是否是搜索结果的文件点击
  if (fileSearchRef.value?.handleFileClick(file)) {
    return
  }

  // 处理常规文件点击逻辑
  if (props.mode === FileListModeEnum.FOLDER_SELECT) {
    // 文件夹选择模式下，点击时只进行导航
    if (file.directory) {
      navigateTo(file.path)
    }
  } else if (props.mode === FileListModeEnum.TAG_FILTER) {
    // 标签过滤模式下,触发 file-click 事件
    emit('file-click', file)
  } else {
    // 普通模式下的原有逻辑
    if (file.directory) {
      navigateTo(file.path)
    } else if (fileService.isTextFile(file)) {
      openEditor(file)
    } else {
      showFileActions(file)
    }
  }
}

// 在 onMounted 中加载操作类型
onMounted(async () => {
  if (props.initialPath) {
    currentPath.value = props.initialPath
  }

  loadPathHistory()  // 加载历史记录
  await loadOperations() // 加载文件操作类型
  loadFiles()

  if (props.mode === FileListModeEnum.FOLDER_SELECT) {
    isMultiSelectMode.value = true
  }
})




// 其他辅助函数
const formatFileInfo = (file: FileInfo) => {
  const size = file.size == null ? '-' : formatFileSize(file.size);
  const date = new Date(file.lastModified).toLocaleString()
  const owner = file.owner || '-'
  return `${size} | ${owner} | ${date}`
}

const formatFileSize = fileService.formatFileSize


// 生命周期
onMounted(() => {
  if (props.initialPath) {
    currentPath.value = props.initialPath;
  }

  loadPathHistory()  // 加载历史记录
  loadFiles();
})

// 显示排序选项
const showSortOptions = () => {
  sortVisible.value = true
}

// 处理排序选择
const handleSortSelect = (action: { value: string }) => {
  const [field, order] = action.value.split('-');
  sortBy.value = field as FileSortField;
  sortDesc.value = order === 'desc';

  // 只在这里进行排序
  const sorted = [...files.value].sort((a, b) => {
    // 首先按文件夹排序
    if (a.directory !== b.directory) {
      return a.directory ? -1 : 1;
    }
    // 然后按选择的字段排序
    const factor = sortDesc.value ? -1 : 1;
    switch (sortBy.value) {
      case 'name':
        return factor * a.name.localeCompare(b.name);
      case 'size':
        return factor * (a.size - b.size);
      case 'lastModified':
        return factor * (a.lastModified - b.lastModified);
      default:
        return 0;
    }
  });

  files.value = sorted;
  sortVisible.value = false;
}

// 系统用户选择器状态
const ownerPickerVisible = ref(false)
const systemUsers = ref<string[]>([])
const selectedOwner = ref('')

// 确认修改所有者
const handleOwnerChange = async () => {
  if (!selectedFile.value || !selectedOwner.value) return

  try {
    await fileService.changeOwner(selectedFile.value.path, selectedOwner.value)
    showToast('修改所有者成功')
    loadFiles()
  } catch (error: any) {
    showToast(error.message || '修改所有者失败')
  } finally {
    ownerPickerVisible.value = false
  }
}

// 处理对话框关闭
const handleOwnerDialogClose = (action: string) => {
  if (action === 'confirm' && !selectedOwner.value) {
    showToast('请选择所有者')
    return false
  }
  return true
}

// 修改获取单元格类名的方法
const getCellClass = (file: FileInfo) => {
  if (props.mode === FileListModeEnum.FOLDER_SELECT) {
    return {
      'folder-selectable': file.directory,
      'file-selectable': props.allowFile && !file.directory,
      'non-selectable': !file.directory && !props.allowFile
    }
  }
  return {}
}


// 添加路由跳转方法
const goToTagFiles = () => {
  router.push('/tag-browser')
}

// 监听 tagIds 变化
watch(() => props.tagIds, () => {
  if (props.mode === FileListModeEnum.TAG_FILTER) {
    currentPage.value = 1
    files.value = []
    loadFiles()
  }
}, {immediate: true})

// 监听路径变化
watch(() => currentPath.value, () => {
  currentPage.value = 1
  files.value = []
  loadFiles()
})

// 添加更多操作菜单状态
const moreActionsVisible = ref(false)

// 更多操作菜单选项（改为固定选项）
const moreActions: MoreAction[] = [
  {
    name: '新建文件夹',
    icon: 'plus'
  },
  {
    name: '上传文件',
    icon: 'upgrade'
  },
  {
    name: '历史记录',
    icon: 'clock-o'
  },
  {
    name: '排序方式',
    icon: 'sort'
  }
]

// 显示更多操作菜单
const showMoreActions = () => {
  moreActionsVisible.value = true
}

// 处理更多操作选择
const handleMoreAction = (action: { name: string }) => {
  switch (action.name) {
    case '新建文件夹':
      createFolderVisible.value = true
      break
    case '上传文件':
      const uploadTrigger = document.querySelector('.van-uploader__input')
      if (uploadTrigger) {
        (uploadTrigger as HTMLElement).click()
      }
      break
    case '历史记录':
      showHistoryPopup()
      break
    case '排序方式':
      showSortOptions()
      break
  }
}

// 添加历史记录弹出层
const historyVisible = ref(false)

// 处理历史记录弹出层
const showHistoryPopup = () => {
  historyVisible.value = true
}

// 处理历史记录选择
const navigateToHistory = (path: string) => {
  navigateTo(path)
  historyVisible.value = false
}

// 搜索相关
const fileSearchRef = ref<InstanceType<typeof FileSearch> | null>(null)

// 显示搜索弹出层
const showSearchPopup = () => {
  fileSearchRef.value?.showSearch(files.value)
}

// 处理搜索文件更新
const handleSearchFilesUpdate = (newFiles: FileInfo[]) => {
  files.value = newFiles
}

// 处理搜索导航
const handleSearchNavigate = (path: string) => {
  navigateTo(path)
}



</script>

<style scoped>
.file-list {
  min-height: 100vh;
  background-color: var(--van-background-2);
  padding-top: v-bind("!currentModeConfig.showNavBar ? '0' : '50px'");
}

.filename-container {
  width: 100%;
  overflow: hidden;
}

.filename {
  display: block;
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.file-icon {
  font-size: 24px;
  margin-right: 8px;
  vertical-align: middle;

  &.folder {
    color: var(--van-blue);
  }
}

.swipe-btn {
  height: 100%;
}

.upload-progress {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

/* 桌面端优化 */
@media (min-width: 768px) {
  .van-cell-group {
    max-width: 1200px;
    margin: 0 auto;
  }
}

/* 禁用长按选择文本 */
.van-cell {
  user-select: none;
  -webkit-user-select: none;
  -webkit-touch-callout: none;

  @media (max-width: 767px) {
    padding: 12px 16px;
  }
}

/* 所有者选择器样式 */
.owner-dialog {
  :deep(&) {
    height: 70vh;
    max-height: 70vh;
    display: flex;
    flex-direction: column;

    .van-dialog__content {
      flex: 1;
      overflow: hidden;
    }
  }

  @media (max-width: 767px) {
    :deep(&) {
      height: 80vh;
      max-height: 80vh;
    }
  }
}

.owner-list {
  height: 100%;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 文件夹选择模式样式 */
.folder-selectable {
  cursor: pointer;
}

.non-selectable {
  opacity: 0.5;
  pointer-events: none;
}

/* 添加搜索相关样式 */
.search-container,
.search-tabs {
  /* 删除这些样式 */
}

.search-result-tip {
  position: sticky;
  top: v-bind("currentModeConfig.showNavBar ? (currentModeConfig.showBreadcrumb ? '90px' : '46px') : '0'");
  margin: 8px 16px;
  z-index: 999;
}

/* 搜索模式下的列表样式 */
.file-list {
  &.searching {
    padding-top: 0;
  }
}

.tag-manager-popup {
  display: flex;
  flex-direction: column;
}

.tag-manager {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tag-browser {
  flex: 1;
  overflow: hidden;
}

/* 标签选择器样式优化 */
:deep(.van-tree-select) {
  --van-tree-select-item-active-color: var(--van-primary-color);
}

/* 桌面端优化 */
@media (min-width: 768px) {
  .tag-manager-popup {
    max-width: 768px;
    margin: 0 auto;
  }
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-tags {
  display: flex;
  flex-wrap: wrap;
  flex-direction: row-reverse;
  gap: 4px;
}

.tag-tree {
  padding: 16px;
}

/* 标签选择器样式优化 */
:deep(.van-popup) {
  max-height: 80vh;
}

/* 导航栏按钮样式优化 */
.van-nav-bar :deep(.van-button) {
  margin-left: 8px;
}

/* 导航栏图标大小统一 */
.van-nav-bar :deep(.van-button .van-icon) {
  font-size: 16px; /* 改回较小的尺寸 */
}

/* 更多操作菜单样式 */
:deep(.van-action-sheet__item) {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 14px 16px;
}

:deep(.van-action-sheet__item .van-icon) {
  margin-right: 8px;
  font-size: 20px;
}

/* 历史记录弹出层样式 */
.history-popup {
  display: flex;
  flex-direction: column;
}

.history-manager {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.history-list {
  flex: 1;
  overflow-y: auto;
}

.history-icon {
  font-size: 20px;
  margin-right: 8px;
  color: var(--van-blue);
}

/* 添加面包屑导航固定样式 */
.breadcrumb {
  position: sticky;
  top: v-bind("currentModeConfig.showNavBar ? '46px' : '0'");
  background-color: var(--van-background-2);
  padding: 8px 16px;
  z-index: 999;
  border-bottom: 1px solid var(--van-border-color);
}

.breadcrumb-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.path-segments {
  flex: 1;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  
  &::-webkit-scrollbar {
    display: none;
  }
}

.select-all-container {
  flex-shrink: 0;
  
  :deep(.van-space) {
    align-items: center;
  }
}

/* 添加选中数量样式 */
.selected-count {
  font-size: 14px;
  color: var(--van-text-color-2);
}

/* 调整文件列表内容区域 */
.task-list {
  padding-top: v-bind("currentModeConfig.showBreadcrumb ? '0' : '8px'");
}

:deep(.cell-ellipsis) {
  .van-cell__title {
    flex: 1;
    overflow: hidden;

    span {
      display: block;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
  }
}
</style>