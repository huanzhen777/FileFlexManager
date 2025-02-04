<template>
  <div class="home">
    <!-- 顶部导航栏 -->
    <van-nav-bar
        title="FileFlexManager"
        fixed
        placeholder
    >
      <template #right>
        <van-icon
            name="setting-o"
            class="nav-icon"
            @click="showActionSheet"
        />
      </template>
    </van-nav-bar>

    <!-- 连接状态提示 -->
    <van-notice-bar
        v-if="message"
        :text="message"
        :background="messageType === 'success' ? '#07c160' : '#ee0a24'"
        color="#fff"
    />

    <!-- 功能卡片区域 -->
    <div class="content">
      <FeatureCards :column-num="3"/>

      <!-- 快速访问区域 -->
      <div class="section quick-access" v-if="quickAccessTags.length > 0">
        <div class="section-header">
          <span class="section-title">快速访问</span>
          <van-icon name="star" class="section-icon"/>
        </div>
        <div class="quick-access-files">
          <QuickAccessFileList
              key="tag-quick-access-files"
              :initial-path="'/'"
              :tag-ids="quickAccessTags.map(t=>t.id)"
          />
        </div>
      </div>
    </div>

    <!-- 用户菜单 -->
    <van-action-sheet
        v-model:show="actionSheetVisible"
        :actions="actions"
        cancel-text="取消"
        close-on-click-action
        @select="onActionSelect"
    />
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import type {ActionSheetAction} from 'vant'
import {showToast} from 'vant'
import {tagService} from '@/api/tagService'
import {TagVO} from "@/types";
import {FeatureCards, QuickAccessFileList} from '@/components'

const router = useRouter()

// 状态管理
const actionSheetVisible = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const quickAccessTags = ref<TagVO[]>([])
const activeTagId = ref<number>()

// 用户菜单选项
const actions: ActionSheetAction[] = [
  {name: '退出登录', color: '#ee0a24', value: 'logout'}
]

// 处理用户菜单选择
const showActionSheet = () => {
  actionSheetVisible.value = true
}

const onActionSelect = async (action: ActionSheetAction) => {
  if (action.name === '退出登录') {
    localStorage.removeItem('token')
    router.push('/login')
    showToast('已退出登录')
  }
}

// 获取快速访问标签
const fetchQuickAccessTags = async () => {
  try {
    const allTags = await tagService.getAllTags()
    quickAccessTags.value = allTags.filter(tag => tag.quickAccess)
    if (quickAccessTags.value.length > 0) {
      activeTagId.value = quickAccessTags.value[0].id
    }
  } catch (error) {
    console.error('获取快速访问标签失败:', error)
  }
}


onMounted(() => {
  fetchQuickAccessTags()
})
</script>

<style scoped>
.home {
  min-height: 100vh;
  background-color: var(--van-gray-1);
  padding-bottom: 20px;
}

.content {
  padding: 16px;
}

/* 快速访问区域样式 */
.quick-access {
  margin-top: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 0 8px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--van-text-color);
}

.section-icon {
  font-size: 20px;
  color: var(--van-warning-color);
}

.quick-access-files {
  /* 适配新的展示方式 */
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quick-access-files::-webkit-scrollbar {
  width: 6px;
}

.quick-access-files::-webkit-scrollbar-thumb {
  background-color: var(--van-gray-3);
  border-radius: 3px;
}

.quick-access-files::-webkit-scrollbar-track {
  background-color: var(--van-gray-1);
}

/* 适配移动端 */
@media (max-width: 768px) {
  .quick-access-files {
    height: 500px;
  }

  .quick-access-tags :deep(.van-tab) {
    padding: 12px 16px;
  }
}

/* 适配深色模式 */
@media (prefers-color-scheme: dark) {
  .quick-access-tags {
    background: var(--van-background);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  }
}

/* 桌面端优化 */
@media (min-width: 768px) {
  .content {
    max-width: 768px;
    margin: 0 auto;
  }

  .van-nav-bar {
    max-width: 768px;
    margin: 0 auto;
  }
}

.nav-icon {
  font-size: 24px;
}
</style> 