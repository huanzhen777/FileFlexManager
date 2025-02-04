<template>
  <div class="file-search">
    <!-- 搜索弹出层 -->
    <van-popup
        v-model:show="searchVisible"
        position="top"
        :style="{ width: '100%' }"
        :overlay="false"
        :close-on-click-overlay="false"
    >
      <div class="search-container">
        <!-- 搜索模式切换 -->
        <van-tabs v-model:active="searchMode" class="search-tabs">
          <van-tab title="本地搜索" name="local"/>
          <van-tab title="全局搜索" name="remote"/>
        </van-tabs>

        <van-search
            v-model="searchKeyword"
            :placeholder="searchMode === 'local' ? '在当前目录搜索' : '搜索所有文件'"
            show-action
            @search="handleSearch"
            @cancel="cancelSearch"
            @input="handleLocalSearch"
        >
          <template #action>
            <div @click="cancelSearch">取消</div>
          </template>
        </van-search>
      </div>
    </van-popup>

    <!-- 搜索结果提示 -->
    <div v-if="isSearchMode" class="search-result-tip">
      <van-notice-bar
          :text="`${searchMode === 'local' ? '当前目录' : '全局'}搜索结果: ${resultCount} 个文件`"
          left-icon="info-o"
      >
        <template #right-icon>
          <van-icon name="cross" @click="cancelSearch"/>
        </template>
      </van-notice-bar>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, computed} from 'vue'
import {showToast, showDialog} from 'vant'
import {fileService} from '@/api/fileService'
import {FileInfo} from "@/types";

const props = defineProps<{
  showNavBar?: boolean
  showBreadcrumb?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:files', files: FileInfo[]): void
  (e: 'navigate', path: string): void
}>()

// 搜索状态
const searchVisible = ref(false)
const searchKeyword = ref('')
const isSearchMode = ref(false)
const searchMode = ref<'local' | 'remote'>('local')
const normalModeFiles = ref<FileInfo[]>([])
const loading = ref(false)

// 计算搜索结果数量
const resultCount = computed(() => normalModeFiles.value.length)

// 显示搜索弹出层
const showSearch = (currentFiles: FileInfo[]) => {
  searchVisible.value = true
  normalModeFiles.value = [...currentFiles]
}

// 执行搜索
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    showToast('请输入搜索关键词')
    return
  }

  // 如果是本地搜索，直接使用已经过滤的结果
  if (searchMode.value === 'local') {
    searchVisible.value = false
    return
  }

  // 远程搜索
  searchVisible.value = false
  isSearchMode.value = true
  loading.value = true

  try {
    const response = await fileService.searchFiles({
      keyword: searchKeyword.value,
      page: 1,
      size: 50
    })

    emit('update:files', response.records)
  } catch (error: any) {
    showToast(error.message || '搜索失败')
  } finally {
    loading.value = false
  }
}

// 本地搜索处理
const handleLocalSearch = () => {
  if (searchMode.value === 'local') {
    const keyword = searchKeyword.value.toLowerCase()
    if (!keyword) {
      emit('update:files', [...normalModeFiles.value])
      return
    }

    const filteredFiles = normalModeFiles.value.filter(file =>
        file.name.toLowerCase().includes(keyword) ||
        file.path.toLowerCase().includes(keyword)
    )
    emit('update:files', filteredFiles)
    isSearchMode.value = true
  }
}

// 取消搜索
const cancelSearch = () => {
  isSearchMode.value = false
  searchKeyword.value = ''
  searchVisible.value = false
  searchMode.value = 'local'
  emit('update:files', [...normalModeFiles.value])
}

// 处理文件点击
const handleFileClick = (file: FileInfo) => {
  if (isSearchMode.value && searchMode.value === 'remote' && !file.directory) {
    showDialog({
      title: '文件操作',
      message: `是否前往 ${file.name} 所在目录？`,
      showCancelButton: true,
      confirmButtonText: '前往',
      cancelButtonText: '取消',
    }).then(() => {
      // 获取父目录路径
      const parentPath = file.path.substring(0, file.path.lastIndexOf('/')) || '/'
      // 退出搜索模式
      cancelSearch()
      // 导航到父目录
      emit('navigate', parentPath)
    }).catch(() => {
      // 如果取消，不做任何操作
    })
    return true
  }
  return false
}

// 暴露方法给父组件
defineExpose({
  showSearch,
  cancelSearch,
  isSearchMode,
  handleFileClick
})
</script>

<style scoped>
.search-container {
  position: fixed;
  top: 46px;
  left: 0;
  right: 0;
  z-index: 1000;
  background: var(--van-background-2);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.search-tabs {
  :deep(.van-tabs__wrap) {
    height: 36px;
  }

  :deep(.van-tab) {
    height: 36px;
    line-height: 36px;
  }
}

.search-result-tip {
  position: sticky;
  top: v-bind("props.showNavBar ? (props.showBreadcrumb ? '90px' : '46px') : '0'");
  margin: 8px 16px;
  z-index: 999;
}
</style> 