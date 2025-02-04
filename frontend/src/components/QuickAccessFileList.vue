<template>
  <div class="quick-access-file-list">
    <van-cell-group>
      <van-cell
          v-for="file in files"
          :key="file.path"
          :title="file.name"
          @click="handleFileClick(file)"
      >
        <template #icon>
          <van-icon
              :name="file.directory ? 'folder-o' : 'file-o'"
              :class="['file-icon', file.directory ? 'folder' : '']"
          />
        </template>
        <template #value>
          <div>
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
          </div>
        </template>
        <template #label>
          <div>{{ formatFileInfo(file) }}</div>
        </template>
      </van-cell>
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {showToast} from 'vant'
import {fileService} from '@/api/fileService'
import {FileInfo} from "@/types";

const props = defineProps<{
  initialPath: string
  tagIds?: number[]
}>()

const router = useRouter()
const files = ref<FileInfo[]>([])

// 加载文件列表
const loadFiles = async () => {
  if (!props.tagIds?.length) return

  try {
    const response = await fileService.getFilesContainAnyTags(props.tagIds, {
      page: 1,
      size: 20
    })

    files.value = response.records;
  } catch (error: any) {
    console.error('加载文件失败:', error)
    showToast(error.message || '加载失败')
  }
}

// 处理文件点击
const handleFileClick = (file: FileInfo) => {
  // 无论是文件还是文件夹，都跳转到文件所在目录
  const dirPath = file.directory ? file.path : file.path.substring(0, file.path.lastIndexOf('/'))
  router.push({
    path: '/files',
    query: {
      path: dirPath,
      highlight: file.directory ? undefined : file.path // 如果是文件，传入完整路径用于高亮显示
    }
  })
}

// 格式化文件信息
const formatFileInfo = (file: FileInfo) => {
  return `${file.path} - ${fileService.formatFileSize(file.size)}`;
}

onMounted(() => {
  loadFiles()
})
</script>

<style scoped>
.quick-access-file-list {
  background: transparent;
}

.file-icon {
  font-size: 20px;
  margin-right: 8px;
}

.file-icon.folder {
  color: var(--van-primary-color);
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: flex-end; /* 使内容靠右对齐 */
}

.van-cell {
  background: transparent;
}

.van-cell-group {
  background: transparent;
}

:deep(.van-list) {
  padding: 0;
}

:deep(.van-cell) {
  padding: 12px 16px;
}

:deep(.van-cell::after) {
  border-bottom: none;
}

:deep(.van-cell:active) {
  background-color: var(--van-gray-2);
}

:deep(.van-tag) {
  font-size: 10px;
  padding: 0 6px;
  line-height: 16px;
  border-radius: 4px;
}
</style> 