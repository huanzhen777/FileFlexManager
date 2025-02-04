<template>
  <div class="label-with-help">
    <span class="label-text">{{ label }}</span>
    <van-popover
      v-if="description"
      v-model:show="showPopover"
      :placement="placement"
      trigger="manual"
      theme="light"
      :offset="[0, 8]"
      :show-arrow="true"
      teleport="body"
      :overlay="false"
    >
      <template #reference>
        <van-icon 
          name="question-o" 
          class="help-icon"
          @mouseenter="showPopover = true"
          @mouseleave="showPopover = false"
        />
      </template>
      <div class="help-content">
        <div class="help-title">{{ label }}</div>
        <div class="help-description">{{ description }}</div>
      </div>
    </van-popover>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

type PopoverPlacement = 'top' | 'top-start' | 'top-end' | 'left' | 'left-start' | 'left-end' | 'right' | 'right-start' | 'right-end' | 'bottom' | 'bottom-start' | 'bottom-end';

const props = withDefaults(defineProps<{
  label: string
  description?: string
  placement?: PopoverPlacement
}>(), {
  placement: 'right'
});

const showPopover = ref(false);
const placement = computed(() => props.placement);
</script>

<style scoped>
.label-with-help {
  display: inline-flex;
  align-items: flex-start;
  gap: 2px;
  position: relative;
  padding-right: 2px;
}

.help-icon {
  font-size: 10px;
  color: var(--van-gray-5);
  opacity: 0.5;
  cursor: help;
  transition: opacity 0.2s ease;
  position: relative;
  top: -2px;
  transform: scale(0.85);
  margin-left: -1px;
}

.help-icon:hover {
  opacity: 0.85;
}

.help-content {
  max-width: 240px;
  padding: 2px 0;
}

.help-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--van-text-color);
  margin-bottom: 4px;
}

.help-description {
  font-size: 12px;
  line-height: 1.5;
  color: var(--van-text-color-2);
  word-break: break-word;
  white-space: pre-line;
}

.label-text {
  vertical-align: middle;
}

:deep(.van-popover__content) {
  padding: 8px 12px;
  border-radius: 6px;
  box-shadow: 0 2px 12px rgba(100, 100, 100, 0.12);
}
</style> 