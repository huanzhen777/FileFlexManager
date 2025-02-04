<template>
  <div class="tag-tree-browser">
    <!-- 导航栏 -->
    <van-nav-bar
        v-if="showNavBar"
        :title="title"
        :left-text="leftText"
        :right-text="rightText"
        @click-left="handleNavLeft"
        @click-right="handleNavRight"
    />

    <!-- 搜索栏 -->
    <div class="search-bar" v-if="showSearch">
      <van-search
          v-model="searchText"
          placeholder="搜索标签"
          @update:model-value="handleSearch"
      />
    </div>

    <!-- 已选标签展示 -->
    <div class="selected-tags" v-if="selectedTagIds.length > 0">
      <van-space wrap>
        <van-tag
            v-for="tagId in selectedTagIds"
            :key="tagId"
            closeable
            type="primary"
            size="medium"
            @close="removeTag(tagId)"
        >
          {{ getTagName(tagId) }}
        </van-tag>
      </van-space>
    </div>

    <!-- 面包屑导航 -->
    <div class="breadcrumb" v-if="showBreadcrumb && !searchText">
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
              <template v-if="showActions">
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
              </template>
              <van-checkbox
                  v-if="selectable"
                  :model-value="isTagSelected(tag)"
                  @click.stop="toggleTagSelection(tag)"
              />
            </div>
          </template>
        </van-cell>
      </div>
      <van-empty v-else description="暂无标签"/>
    </div>

    <!-- 标签编辑对话框 -->
    <van-dialog
        v-model:show="editDialogVisible"
        :title="dialogTitle"
        show-cancel-button
        @confirm="handleTagSubmit"
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
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {Icon} from '@iconify/vue'
import {tagService} from '@/api/tagService'
import type {TagVO} from '@/types'
import {showToast} from 'vant'
import {showDialog} from 'vant'

const props = withDefaults(defineProps<{
  modelValue?: number[] // 初始标签ID数组，仅用于初始化
  showSearch?: boolean // 是否显示搜索框
  showBreadcrumb?: boolean // 是否显示面包屑导航
  selectable?: boolean // 是否可选择标签
  showActions?: boolean // 是否显示编辑/删除操作
  showNavBar?: boolean // 是否显示导航栏
  title?: string // 导航栏标题
  leftText?: string // 导航栏左侧文本
  rightText?: string // 导航栏右侧文本
  filePath?: string // 当前操作的文件路径
}>(), {
  modelValue: () => [],
  showSearch: true,
  showBreadcrumb: true,
  selectable: true,
  showActions: false,
  showNavBar: false,
  title: '标签管理',
  leftText: '返回',
  rightText: '确定',
  filePath: undefined
})

const emit = defineEmits<{
  (e: 'tag-click', tag: TagVO): void
  (e: 'nav-left'): void
  (e: 'nav-right'): void
  (e: 'success'): void // 添加成功事件
}>()

// 状态管理
const allTags = ref<TagVO[]>([])
const currentTagId = ref<number | null>(null)
const searchText = ref('')
const selectedTagIds = ref<number[]>([]) // 内部维护的选中标签状态

// 编辑相关状态
const editDialogVisible = ref(false)
const editingTag = ref<{
  id?: number
  name: string
  parentId: number | null
  quickAccess: boolean
  bindFile: boolean
}>({
  name: '',
  parentId: null,
  quickAccess: false,
  bindFile: false
})

// 对话框标题
const dialogTitle = computed(() => {
  return editingTag.value.id ? '编辑标签' : '新建标签'
})

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

// 获取标签名称
const getTagName = (tagId: number): string => {
  const tag = allTags.value.find(t => t.id === tagId)
  return tag ? tag.name : ''
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
  emit('tag-click', tag)
}

// 导航到根目录
const navigateToRoot = () => {
  currentTagId.value = null
}

// 导航到指定标签
const navigateToTag = (tag: TagVO) => {
  currentTagId.value = tag.id
}

// 标签选择相关方法
const isTagSelected = (tag: TagVO) => {
  return selectedTagIds.value.includes(tag.id)
}

const toggleTagSelection = (tag: TagVO) => {
  const index = selectedTagIds.value.indexOf(tag.id)
  if (index === -1) {
    selectedTagIds.value.push(tag.id)
  } else {
    selectedTagIds.value.splice(index, 1)
  }
}

const removeTag = async (tagId: number) => {
  // 获取标签信息
  const tag = allTags.value.find(t => t.id === tagId)
  if (tag?.bindFile) {
    // 如果是绑定文件的标签，显示确认对话框
    try {
      await showDialog({
        title: '确认移除标签',
        message: '该标签已绑定文件，移除后将同时移除所有相同文件的关联，是否继续？',
        showCancelButton: true,
      })
    } catch {
      return
    }
  }
  selectedTagIds.value = selectedTagIds.value.filter(id => id !== tagId)
}

// 标签编辑相关方法
const showEditDialog = (tag?: TagVO) => {
  if (tag) {
    editingTag.value = {
      id: tag.id,
      name: tag.name,
      parentId: tag.parentId,
      quickAccess: tag.quickAccess,
      bindFile: tag.bindFile
    }
  } else {
    editingTag.value = {
      name: '',
      parentId: currentTagId.value,
      quickAccess: false,
      bindFile: false
    }
  }
  editDialogVisible.value = true
}

const handleTagSubmit = async () => {
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
    editDialogVisible.value = false
  } catch (error: any) {
    showToast(error.message || '操作失败')
  }
}

// 删除标签
const confirmDelete = async (tag: TagVO) => {
  try {
    await showDialog({
      title: '确认删除',
      message: `确定要删除标签"${tag.name}"吗？这将同时删除其所有子标签。`,
      showCancelButton: true
    })

    await tagService.deleteTag(tag.id)
    showToast('删除成功')
    // 如果删除的是当前标签，返回上一级
    if (currentTagId.value === tag.id) {
      navigateToRoot()
    }
    loadTags()
  } catch (error: any) {
    if (error === 'cancel') return
    showToast(error.message || '删除失败')
  }
}

// 导航栏事件处理
const handleNavLeft = () => {
  emit('nav-left')
}

const handleNavRight = async () => {
  if (props.filePath) {
    try {
      await tagService.updateFileTags(props.filePath, selectedTagIds.value)
      showToast('标签更新成功')
      emit('success')
    } catch (error: any) {
      showToast(error.message || '更新标签失败')
    }
  }
  emit('nav-right')
}

// 加载文件标签
const loadFileTags = async () => {
  if (!props.filePath) return
  
  try {
    const fileTags = await tagService.getFileTags(props.filePath)
    selectedTagIds.value = fileTags.map(tag => tag.id)
  } catch (error: any) {
    showToast(error.message || '加载标签失败')
  }
}

// 监听文件路径变化
watch(() => props.filePath, () => {
  if (props.filePath) {
    loadFileTags()
  }
}, { immediate: true })

// 监听搜索文本变化
watch(() => searchText.value, () => {
  if (!searchText.value) {
    // 清空搜索时返回当前标签
    currentTagId.value = null
  }
})

// 监听初始值变化
watch(() => props.modelValue, (newValue) => {
  if (newValue && newValue.length > 0) {
    selectedTagIds.value = [...newValue]
  }
}, { immediate: true })

onMounted(() => {
  loadTags()
})
</script>

<style scoped>
.tag-tree-browser {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--van-background-2);
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
  flex: 1;
  overflow-y: auto;
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
}

.action-icon {
  font-size: 16px;
  color: var(--van-gray-6);
  cursor: pointer;
}
</style> 