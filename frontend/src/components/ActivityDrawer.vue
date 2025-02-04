<template>
  <van-popup
    v-model:show="visible"
    position="left"
    :style="{ width: '85%', height: '100%' }"
  >
    <div class="activity-drawer">
      <!-- 顶部标题栏 -->
      <van-nav-bar
        title="快捷功能"
        left-text="主页"
        left-arrow
        right-text="关闭"
        @click-left="goHome"
        @click-right="visible = false"
      />

      <!-- 功能卡片区域 -->
      <div class="content">
        <FeatureCards 
          :column-num="2" 
          @item-click="visible = false"
        />

      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import FeatureCards from './FeatureCards.vue'

const visible = ref(false)
const router = useRouter()

// 添加返回主页方法
const goHome = () => {
  router.push('/')
  visible.value = false
}

// 暴露方法给外部使用
defineExpose({
  show: () => visible.value = true,
  hide: () => visible.value = false
})
</script>

<style scoped>
.activity-drawer {
  height: 100%;
  background-color: var(--van-background-2);
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

/* 快速访问区域样式 */
.quick-access {
  margin-top: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--van-text-color);
  margin-bottom: 12px;
  padding-left: 8px;
}

.quick-access-item {
  background: white;
  border-radius: 8px;
  margin-bottom: 1px;
}

.quick-access-item:deep(.van-cell__title) {
  font-size: 15px;
}

.quick-access-item:deep(.van-icon) {
  font-size: 20px;
  color: var(--van-primary-color);
}

/* 桌面端优化 */
@media (min-width: 768px) {
  .content {
    max-width: 768px;
    margin: 0 auto;
  }
}

.content.blur-bg {
  filter: blur(2px);
  pointer-events: none;
}
</style> 