<template>
  <van-popup
    v-model:show="show"
    position="right"
    :style="{ width: '100%', height: '100%' }"
  >
    <div class="editor-container">
      <!-- 顶部导航栏 -->
      <van-nav-bar
        :title="fileName || '编辑器'"
        left-text="返回"
        left-arrow
        @click-left="handleClose"
      >
        <template #right>
          <van-button 
            type="primary" 
            size="small" 
            :loading="saving"
            @click="saveContent"
          >
            保存
          </van-button>
        </template>
      </van-nav-bar>

      <!-- 编辑器区域 -->
      <div class="editor-content" ref="editorContainer"></div>

      <!-- 底部工具栏 -->
      <van-action-bar v-if="hasChanges">
        <van-action-bar-icon 
          icon="close" 
          text="放弃更改" 
          @click="discardChanges"
        />
        <van-action-bar-button 
          type="danger" 
          text="保存" 
          @click="saveContent"
        />
      </van-action-bar>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { showToast, showDialog } from 'vant'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import { javascript } from '@codemirror/lang-javascript'
import { json } from '@codemirror/lang-json'
import { markdown } from '@codemirror/lang-markdown'
import { python } from '@codemirror/lang-python'
import { xml } from '@codemirror/lang-xml'
import { oneDark } from '@codemirror/theme-one-dark'
import { fileService } from '@/api/fileService'

const props = defineProps<{
  visible: boolean
  filePath?: string
  fileName?: string
}>()

const emit = defineEmits(['update:visible'])

// 状态管理
const show = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})
const editor = ref<EditorView | null>(null)
const editorContainer = ref<HTMLElement | null>(null)
const originalContent = ref('')
const saving = ref(false)
const hasChanges = computed(() => {
  if (!editor.value) return false
  return editor.value.state.doc.toString() !== originalContent.value
})

// 语言支持
const determineLanguage = (fileName?: string) => {
  if (!fileName) return null
  const ext = fileName.toLowerCase().match(/\.[^.]*$/)
  if (!ext) return null

  const extensionMap: Record<string, any> = {
    '.js': javascript(),
    '.ts': javascript({ typescript: true }),
    '.json': json(),
    '.md': markdown(),
    '.py': python(),
    '.xml': xml(),
    '.html': xml(),
    '.htm': xml()
  }

  return extensionMap[ext[0]] || null
}

// 初始化编辑器
const initEditor = async () => {
  if (!props.visible || !props.filePath) return

  try {
    const response = await fileService.getFileContent(props.filePath)
    const content = response.data
    originalContent.value = content

    if (editor.value) {
      editor.value.destroy()
    }

    await nextTick()

    const language = determineLanguage(props.fileName)
    const extensions = [basicSetup, oneDark]
    if (language) {
      extensions.push(language)
    }

    const state = EditorState.create({
      doc: content,
      extensions
    })

    editor.value = new EditorView({
      state,
      parent: editorContainer.value!
    })
  } catch (error: any) {
    showToast('加载文件内容失败')
    show.value = false
  }
}

// 保存内容
const saveContent = async () => {
  if (!editor.value || !props.filePath) return

  const content = editor.value.state.doc.toString()
  if (content === originalContent.value) {
    showToast('文件内容未改变')
    return
  }

  saving.value = true
  try {
    await fileService.saveFileContent(props.filePath, content)
    showToast({
      type: 'success',
      message: '保存成功'
    })
    originalContent.value = content
  } catch (error: any) {
    showToast({
      type: 'fail',
      message: error.message || '保存失败'
    })
  } finally {
    saving.value = false
  }
}

// 放弃更改
const discardChanges = () => {
  if (!hasChanges.value) return
  
  showDialog({
    title: '确认放弃更改',
    message: '确定要放弃所有更改吗？此操作不可恢复。',
    showCancelButton: true
  }).then(() => {
    if (editor.value) {
      editor.value.dispatch({
        changes: {
          from: 0,
          to: editor.value.state.doc.length,
          insert: originalContent.value
        }
      })
    }
  })
}

// 关闭处理
const handleClose = () => {
  if (hasChanges.value) {
    showDialog({
      title: '确认关闭',
      message: '有未保存的更改，确定要关闭吗？',
      showCancelButton: true
    }).then(() => {
      show.value = false
    })
  } else {
    show.value = false
  }
}

// 监听和生命周期
watch(() => props.visible, async (newVal) => {
  if (newVal) {
    await nextTick()
    initEditor()
  }
})

onMounted(() => {
  if (props.visible) {
    initEditor()
  }
})
</script>

<style scoped>
.editor-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--van-background);
}

.editor-content {
  flex: 1;
  overflow: auto;
}

:deep(.cm-editor) {
  height: 100%;
}

:deep(.cm-scroller) {
  font-family: monospace;
}

/* 桌面端优化 */
@media (min-width: 768px) {
  :deep(.van-popup) {
    max-width: 1200px;
    left: 50% !important;
    transform: translateX(-50%);
  }
}
</style> 