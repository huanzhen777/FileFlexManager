<template>
  <div class="tag-browser">
    <!-- 固定头部区域 -->
    <div class="fixed-header">
      <van-nav-bar
          :title="currentTag ? currentTag.name : '标签浏览'"
          left-text="返回"
          left-arrow
          @click-left="goBack"
          fixed
          placeholder
      >
        <template #right>
          <van-button
              type="primary"
              size="small"
              plain
              @click="showAddDialog"
          >
            <template #icon>
              <van-icon name="plus"/>
            </template>
            新建标签
          </van-button>
        </template>
      </van-nav-bar>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <van-search
            v-model="searchText"
            placeholder="搜索标签"
            @update:model-value="handleSearch"
        />
      </div>

      <!-- 已选标签展示 -->
      <div class="selected-tags" v-if="selectedTags.length > 0">
        <van-space wrap>
          <van-tag
              v-for="tag in selectedTags"
              :key="tag.id"
              closeable
              type="primary"
              size="medium"
              @close="removeSelectedTag(tag)"
          >
            {{ getTagPath(tag, '/') }}
          </van-tag>
        </van-space>
      </div>

      <!-- 面包屑导航 -->
      <div class="breadcrumb" v-if="!searchText">
        <van-space wrap>
          <van-tag
              plain
              type="primary"
              size="medium"
              @click="navigateToRoot"
              :class="{ active: !currentTagId }"
          >
            <template #icon>
              <Icon icon="twemoji:label"/>
            </template>
            /
          </van-tag>
          <template v-for="(tag, index) in tagPath" :key="tag.id">
            <van-icon name="arrow"/>
            <van-tag
                plain
                type="primary"
                size="medium"
                @click="navigateToTag(tag)"
                :class="{ active: currentTagId === tag.id }"
            >
              <template #icon>
                <Icon icon="twemoji:label"/>
              </template>
              {{ tag.name }}
            </van-tag>
          </template>
        </van-space>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">
      <!-- 搜索结果 -->
      <div class="tag-list" v-if="searchText">
        <van-cell
            v-for="tag in searchResults"
            :key="tag.id"
            :title="getTagPath(tag)"
            is-link
            @click="handleTagClick(tag)"
        >
          <template #icon>
            <Icon icon="twemoji:label" class="tag-icon"/>
          </template>
          <template #right-icon>
            <van-checkbox
                :model-value="isTagSelected(tag)"
                @click.stop="toggleTagSelection(tag)"
            />
          </template>
        </van-cell>
      </div>

      <!-- 子标签列表 -->
      <div v-else-if="currentChildren.length > 0">
        <van-cell
            v-for="tag in currentChildren"
            :key="tag.id"
            :title="tag.name"
            is-link
            @click="handleTagClick(tag)"
            :label="`标签: ${tag.childCount} | 文件: ${tag.associatedFileCount}`"
        >
          
          <template #icon>
            <Icon icon="twemoji:label" class="tag-icon"/>
          </template>
          <template #value>
            <van-tag v-if="tag.quickAccess" plain>快速访问</van-tag>
            <van-tag v-if="tag.bindFile" plain>绑定文件</van-tag>
          </template>
          <template #right-icon>
            <div class="tag-actions">
              <van-icon
                  name="edit"
                  class="action-icon"
                  @click.stop="showEditDialog(tag)"
              />
              <van-icon
                  name="delete"
                  class="action-icon"
                  @click.stop="confirmDelete(tag)"
              />
              <van-checkbox
                  :model-value="isTagSelected(tag)"
                  @click.stop="toggleTagSelection(tag)"
              />
            </div>
          </template>
        </van-cell>
      </div>

      <!-- 标签对话框 -->
      <van-dialog
          v-model:show="showDialog"
          :title="dialogTitle"
          show-cancel-button
          @confirm="handleAddTag"
      >
        <van-form>
          <van-field
              v-model="editingTag.name"
              label="标签名称"
              placeholder="请输入标签名称"
              :rules="[{ required: true, message: '请输入标签名称' }]"
          />
          <van-cell title="快速访问">
            <template #right-icon>
              <van-switch v-model="editingTag.quickAccess" size="20"/>
            </template>
          </van-cell>
          <van-cell title="绑定文件">
            <template #label>
              <span class="text-xs text-gray-500">启用后，相同内容的文件将自动关联此标签</span>
            </template>
            <template #right-icon>
              <van-switch v-model="editingTag.bindFile" size="20"/>
            </template>
          </van-cell>
        </van-form>
      </van-dialog>

      <!-- 文件列表 -->
      <FileList
          :mode="FileListModeEnum.TAG_FILTER"
          :tag-ids="effectiveTagIds"
          :custom-back="true"
          ref="fileListRef"
          @file-click="handleFileClick"
          @back="goBack"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {showDialog as vantDialog, showToast} from 'vant'
import {tagService} from '@/api/tagService'
import FileList from './FileList.vue'
import {Icon} from '@iconify/vue'
import type {FileInfo, TagVO} from '@/types'
import {FileListModeEnum} from "@/types";
import {CreateTagRequest} from "@/types";

const router = useRouter()
const allTags = ref<TagVO[]>([])
const currentTagId = ref<number | null>(null)
const fileListRef = ref()
const searchText = ref('')

// 搜索结果
const searchResults = computed(() => {
  if (!searchText.value) return []
  const searchLower = searchText.value.toLowerCase()
  return allTags.value.filter(tag =>
      tag.name.toLowerCase().includes(searchLower) ||
      getTagPath(tag).toLowerCase().includes(searchLower)
  ).sort((a, b) => getTagPath(a).localeCompare(getTagPath(b)))
})

// 获取标签完整路径
const getTagPath = (tag: TagVO, separator?: string): string => {
  const path: string[] = [tag.name]
  let current = tag

  while (current.parentId) {
    const parentTag = allTags.value.find(t => t.id === current.parentId)
    if (parentTag) {
      path.unshift(parentTag.name)
      current = parentTag
    } else {
      break
    }
  }

  return path.join(separator || ' > ')
}

// 处理搜索
const handleSearch = () => {
  // 搜索逻辑由 searchResults 计算属性处理
}

// 获取当前标签
const currentTag = computed(() => {
  if (!currentTagId.value) return null
  return allTags.value.find(tag => tag.id === currentTagId.value)
})

// 获取当前标签的子标签
const currentChildren = computed(() => {
  return allTags.value.filter(tag => tag.parentId === currentTagId.value).map(tag => ({
    ...tag,
    childCount: allTags.value.filter(child => child.parentId === tag.id).length,
    associatedFileCount: tag.fileCount || 0
  }))
})

// 获取标签路径
const tagPath = computed(() => {
  const path: TagVO[] = []
  let current = currentTag.value

  while (current) {
    path.unshift(current)
    current = allTags.value.find(tag => tag.id === current?.parentId)
  }

  return path
})

// 当前标签ID数组（包含所有父标签）
const currentTagIds = computed(() => {
  return currentTagId.value ? [currentTagId.value] : []
})

// 新增的状态和计算属性
const selectedTags = ref<TagVO[]>([])

const effectiveTagIds = computed(() => {
  // 如果有选中的标签，使用选中的标签
  if (selectedTags.value.length > 0) {
    return selectedTags.value.map(tag => tag.id)
  }
  // 否则使用当前浏览的标签
  return currentTagIds.value
})

// 新增的方法
const isTagSelected = (tag: TagVO) => {
  return selectedTags.value.some(t => t.id === tag.id)
}

const toggleTagSelection = (tag: TagVO) => {
  const index = selectedTags.value.findIndex(t => t.id === tag.id)
  if (index === -1) {
    selectedTags.value.push(tag)
  } else {
    selectedTags.value.splice(index, 1)
  }
  fileListRef.value?.loadFiles()
}

const removeSelectedTag = (tag: TagVO) => {
  selectedTags.value = selectedTags.value.filter(t => t.id !== tag.id)
  fileListRef.value?.loadFiles()
}

// 加载所有标签
const loadTags = async () => {
  try {
    allTags.value = await tagService.getAllTags()
  } catch (error: any) {
    showToast(error.message || '加载标签失败')
  }
}

// 处理标签点击
const handleTagClick = (tag: TagVO) => {
  currentTagId.value = tag.id
}

// 处理文件点击
const handleFileClick = (file: FileInfo) => {
  if (file.directory) {
    // 如果是文件夹,跳转到该文件夹
    router.push({
      path: '/files',
      query: {
        path: file.path,
        from: 'tag-browser'
      }
    })
  } else {
    // 如果是文件,询问是否跳转到所在目录
    vantDialog({
      title: '文件操作',
      message: `是否前往 ${file.name} 所在目录？`,
      showCancelButton: true,
      confirmButtonText: '前往',
      cancelButtonText: '取消',
    }).then(() => {
      // 获取父目录路径
      const parentPath = file.path.substring(0, file.path.lastIndexOf('/')) || '/'
      // 跳转到父目录
      router.push({
        path: '/files',
        query: {
          path: parentPath,
          from: 'tag-browser'
        }
      })
    }).catch(() => {
      // 用户取消操作，不做任何处理
    })
  }
}

// 导航到根目录
const navigateToRoot = () => {
  currentTagId.value = null
}

// 导航到指定标签
const navigateToTag = (tag: TagVO) => {
  currentTagId.value = tag.id
}

// 返回上一级
const goBack = () => {
  // 如果有选中的标签，先清空选中的标签
  if (selectedTags.value.length > 0) {
    selectedTags.value = []
    return
  }

  const parentTag = allTags.value.find(tag => tag.id === currentTag.value?.parentId)
  if (parentTag) {
    currentTagId.value = parentTag.id
  } else if (currentTagId.value) {
    currentTagId.value = null
  } else {
    router.back()
  }
}

// 标签编辑相关
const showDialog = ref(false)
const editingTag = ref<CreateTagRequest & { id?: number }>({
  name: '',
  parentId: null,
  quickAccess: false,
  bindFile: false
})

// 对话框标题
const dialogTitle = computed(() => {
  if (editingTag.value.id) {
    return '编辑标签'
  }
  return currentTagId.value
      ? `添加"${currentTag.value?.name}"的子标签`
      : '添加根标签'
})

// 显示添加对话框
const showAddDialog = () => {
  editingTag.value = {
    name: '',
    parentId: currentTagId.value,
    quickAccess: false,
    bindFile: false
  }
  showDialog.value = true
}

// 显示编辑对话框
const showEditDialog = (tag: TagVO) => {
  editingTag.value = {
    id: tag.id,
    name: tag.name,
    parentId: tag.parentId,
    quickAccess: tag.quickAccess,
    bindFile: tag.bindFile
  }
  showDialog.value = true
}

// 处理添加/编辑标签
const handleAddTag = async () => {
  if (!editingTag.value.name?.trim()) {
    showToast('请输入标签名称')
    return
  }

  try {
    if (editingTag.value.id) {
      await tagService.updateTag(editingTag.value.id, editingTag.value)
      showToast('更新成功')
    } else {
      await tagService.createTag(editingTag.value)
      showToast('添加成功')
    }
    loadTags()
    showDialog.value = false
  } catch (error: any) {
    console.error('标签操作失败:', error)
    showToast(error.message || '操作失败')
  }
}

// 确认删除标签
const confirmDelete = async (tag: TagVO) => {
  try {
    await vantDialog({
      title: '确认删除',
      message: `确定要删除标签"${tag.name}"吗？这将同时删除其所有子标签。`,
      showCancelButton: true
    })

    await tagService.deleteTag(tag.id)
    showToast('删除成功')
    // 如果删除的是当前标签，返回上一级
    if (currentTagId.value === tag.id) {
      goBack()
    }
    loadTags()
  } catch (error: any) {
    if (error === 'cancel') return
    showToast(error.message || '删除失败')
  }
}

onMounted(() => {
  loadTags()
})
</script>

<style scoped>
.tag-browser {
  min-height: 100vh;
  background-color: var(--van-background-2);
  padding-bottom: env(safe-area-inset-bottom);
}

.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 99;
  background-color: var(--van-background-2);
}

/* 为了适配桌面端，给固定头部添加最大宽度 */
@media (min-width: 768px) {
  .fixed-header {
    max-width: 768px;
    left: 50%;
    transform: translateX(-50%);
  }
}

.search-bar {
  background-color: #fff;
}

.breadcrumb {
  padding: 12px 16px;
  background-color: #fff;
  overflow-x: auto;
  white-space: nowrap;
  border-bottom: 1px solid var(--van-gray-2);
}

.selected-tags {
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid var(--van-gray-2);
}

.content-area {
  padding-top: v-bind("searchText ? '140px' : (selectedTags.length > 0 ? '220px' : '180px')");
  min-height: 100vh;
}

.tag-list {
  background-color: #fff;
  margin-bottom: 12px;
}

.tag-icon {
  font-size: 20px;
  margin-right: 8px;
  color: var(--van-primary-color);
}

.active {
  background-color: var(--van-primary-color) !important;
  color: white !important;
  border-color: var(--van-primary-color) !important;
}

.active :deep(.van-icon) {
  color: white !important;
}

/* 桌面端优化 */
@media (min-width: 768px) {
  .tag-browser {
    max-width: 768px;
    margin: 0 auto;
  }
}

.selected-tags .van-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}

/* 修改标签基础样式 */
.van-tag {
  border-radius: 12px !important;
  padding: 2px 8px !important;
  font-size: 8px !important;
  font-weight: normal !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tag-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 12px;
  justify-content: center;
}

.action-icon {
  font-size: 16px;
  color: var(--van-gray-6);
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

:deep(.van-checkbox) {
  height: 100%;
  display: flex;
  align-items: center;
}

:deep(.van-cell__value) {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

:deep(.van-tag) {
  margin-right: 8px;
  display: inline-flex;
  align-items: center;
}
</style> 