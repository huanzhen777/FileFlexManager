// 导入所有组件
import ParamForm from './param/ParamForm.vue'
import QuickAccessFileList from './QuickAccessFileList.vue'
import TaskManager from './task/TaskManager.vue'
import TextEditor from './TextEditor.vue'
import ActivityDrawer from './ActivityDrawer.vue'
import FeatureCards from './FeatureCards.vue'
import FileSearch from './FileSearch.vue'
import FolderPicker from './FolderPicker.vue'
import LabelWithHelp from './LabelWithHelp.vue'
import ListParamForm from './param/ListParamForm.vue'
import TagTreeBrowser from './tag/FileTagManager.vue'

// 统一导出
export {
  ParamForm,
  QuickAccessFileList,
  TaskManager,
  TextEditor,
  ActivityDrawer,
  FeatureCards,
  FileSearch,
  FolderPicker,
  LabelWithHelp,
  ListParamForm,
  TagTreeBrowser
}

// 为了方便类型提示，也导出默认对象
export default {
  ParamForm,
  QuickAccessFileList,
  TaskManager,
  TextEditor,
  ActivityDrawer,
  FeatureCards,
  FileSearch,
  FolderPicker,
  LabelWithHelp,
  ListParamForm,
  TagTreeBrowser
}
