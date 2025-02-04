package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.entity.FileIndex;
import com.huanzhen.fileflexmanager.domain.model.entity.FileTag;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileMoveParams;
import com.huanzhen.fileflexmanager.domain.repository.FileIndexRepository;
import com.huanzhen.fileflexmanager.domain.repository.FileTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FileMoveTaskHandlerTest extends BaseTaskHandlerTest<FileMoveParams> {

    @TempDir
    Path tempDir;
    
    Path sourceFile;
    Path sourceFile2;
    Path targetDir;
    Path sourceDir;

    @Mock
    private FileIndexRepository fileIndexRepository;

    @Mock
    private FileTagRepository fileTagRepository;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        super.setUp();
        
        // 创建源文件
        sourceFile = tempDir.resolve("source.txt");
        Files.write(sourceFile, "test content".getBytes());
        
        // 创建第二个源文件
        sourceFile2 = tempDir.resolve("source2.txt");
        Files.write(sourceFile2, "test content 2".getBytes());
        
        // 创建源目录
        sourceDir = tempDir.resolve("sourceDir");
        Files.createDirectories(sourceDir);
        Files.write(sourceDir.resolve("test.txt"), "test content".getBytes());
        
        // 创建目标目录
        targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
    }

    @Override
    protected void setupTestHandler() {
        taskHandler = new FileMoveTaskHandler(taskRepository, fileIndexRepository, fileTagRepository);
    }

    @Override
    protected TaskType getTaskType() {
        return TaskType.FILE_MOVE;
    }

    @Test
    void testMoveDirectory_WithTags_ShouldMoveAllTags() throws IOException {
        // 准备目录及其文件的索引数据
        FileIndex dirIndex = new FileIndex();
        dirIndex.setId(1L);
        dirIndex.setPath(sourceDir.toString());
        dirIndex.setName(sourceDir.getFileName().toString());
        dirIndex.setIsDir(true);

        FileIndex fileIndex1 = new FileIndex();
        fileIndex1.setId(2L);
        fileIndex1.setPath(sourceDir.resolve("test.txt").toString());
        fileIndex1.setName("test.txt");
        fileIndex1.setIsDir(false);
        fileIndex1.setSize(Files.size(sourceDir.resolve("test.txt")));

        // 准备标签数据
        FileTag dirTag = new FileTag(1L, 101L);
        FileTag fileTag1 = new FileTag(2L, 102L);
        FileTag fileTag2 = new FileTag(2L, 103L);

        // 设置mock行为
        // 第一次调用：获取标签时的查找
        when(fileIndexRepository.findByPath(sourceDir.toString()))
            .thenReturn(dirIndex);
        // 第二次调用：移动标签时的查找
        when(fileIndexRepository.findByPath(sourceDir.toString()))
            .thenReturn(dirIndex);
        // 第一次调用：获取标签时的查找
        when(fileIndexRepository.findByPath(sourceDir.resolve("test.txt").toString()))
            .thenReturn(fileIndex1);
        // 第二次调用：移动标签时的查找
        when(fileIndexRepository.findByPath(sourceDir.resolve("test.txt").toString()))
            .thenReturn(fileIndex1);

        when(fileIndexRepository.findByParentPath(sourceDir.toString()))
            .thenReturn(Arrays.asList(fileIndex1));
        when(fileTagRepository.findByFileId(1L))
            .thenReturn(Arrays.asList(dirTag));
        when(fileTagRepository.findByFileId(2L))
            .thenReturn(Arrays.asList(fileTag1, fileTag2));
        when(fileIndexRepository.save(any()))
            .thenReturn(new FileIndex());

        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(sourceDir.toString()));
        payload.put("targetDir", targetDir.toString());
        payload.put("moveTags", true);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证目录索引被删除
        verify(fileIndexRepository).deleteStaleIndexes(eq(sourceDir.toString()), any(LocalDateTime.class));
        
        // 验证新的文件索引被创建（目录和文件各一个）
        verify(fileIndexRepository, times(2)).save(any(FileIndex.class));
        
        // 验证标签被移动（目录一个标签，文件两个标签）
        verify(fileTagRepository, times(3)).save(any(FileTag.class));
        
        // 验证目录确实被移动了
        assertTrue(Files.exists(targetDir.resolve(sourceDir.getFileName())));
        assertFalse(Files.exists(sourceDir));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testMoveDirectory_WithoutTags_ShouldNotMoveTags() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(sourceDir.toString()));
        payload.put("targetDir", targetDir.toString());
        payload.put("moveTags", false);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证目录索引被删除
        verify(fileIndexRepository).deleteStaleIndexes(eq(sourceDir.toString()), any(LocalDateTime.class));
        
        // 验证没有创建新的文件索引和标签
        verify(fileIndexRepository, never()).save(any(FileIndex.class));
        verify(fileTagRepository, never()).save(any(FileTag.class));
        
        // 验证目录确实被移动了
        assertTrue(Files.exists(targetDir.resolve(sourceDir.getFileName())));
        assertFalse(Files.exists(sourceDir));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testMoveFile_WithTags_ShouldMoveTagsToNewFile() throws IOException {
        // 准备测试数据
        FileIndex sourceFileIndex = new FileIndex();
        sourceFileIndex.setId(1L);
        sourceFileIndex.setPath(sourceFile.toString());
        sourceFileIndex.setName(sourceFile.getFileName().toString());
        sourceFileIndex.setIsDir(false);
        sourceFileIndex.setSize(Files.size(sourceFile));

        FileTag tag1 = new FileTag(1L, 101L);
        FileTag tag2 = new FileTag(1L, 102L);
        List<FileTag> sourceTags = Arrays.asList(tag1, tag2);

        when(fileIndexRepository.findByPath(sourceFile.toString())).thenReturn(sourceFileIndex);
        when(fileTagRepository.findByFileId(1L)).thenReturn(sourceTags);
        when(fileIndexRepository.save(any())).thenReturn(new FileIndex());

        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(sourceFile.toString()));
        payload.put("targetDir", targetDir.toString());
        payload.put("moveTags", true);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证文件索引被删除
        verify(fileIndexRepository).deleteByPath(sourceFile.toString());
        
        // 验证新的文件索引被创建
        verify(fileIndexRepository).save(any(FileIndex.class));
        
        // 验证标签被移动
        verify(fileTagRepository, times(2)).save(any(FileTag.class));
        
        // 验证文件确实被移动了
        assertTrue(Files.exists(targetDir.resolve(sourceFile.getFileName())));
        assertFalse(Files.exists(sourceFile));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testMoveFile_WithoutTags_ShouldNotMoveTags() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(sourceFile.toString()));
        payload.put("targetDir", targetDir.toString());
        payload.put("moveTags", false);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证文件索引被删除
        verify(fileIndexRepository).deleteByPath(sourceFile.toString());
        
        // 验证没有创建新的文件索引和标签
        verify(fileIndexRepository, never()).save(any(FileIndex.class));
        verify(fileTagRepository, never()).save(any(FileTag.class));
        
        // 验证文件确实被移动了
        assertTrue(Files.exists(targetDir.resolve(sourceFile.getFileName())));
        assertFalse(Files.exists(sourceFile));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testMoveDirectory_ShouldDeleteDirectoryIndexes() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(sourceDir.toString()));
        payload.put("targetDir", targetDir.toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证目录下所有文件索引被删除
        verify(fileIndexRepository).deleteStaleIndexes(eq(sourceDir.toString()), any(LocalDateTime.class));
        
        // 验证目录确实被移动了
        assertTrue(Files.exists(targetDir.resolve(sourceDir.getFileName())));
        assertFalse(Files.exists(sourceDir));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testMoveNonExistentFile() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(tempDir.resolve("non_existent.txt").toString()));
        payload.put("targetDir", targetDir.toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务失败
        verifyTaskFailure(task, "源文件不存在：" + tempDir.resolve("non_existent.txt"));
    }

    @Test
    void testMoveToNonExistentDirectory() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(sourceFile.toString()));
        payload.put("targetDir", tempDir.resolve("non_existent_dir").toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务失败
        verifyTaskFailure(task, "目标目录不存在：" + tempDir.resolve("non_existent_dir"));
        
        // 验证源文件仍然存在
        assertTrue(Files.exists(sourceFile));
    }

    @Test
    void testMoveFileWithOverwrite() throws IOException {
        // 在目标目录创建同名文件
        Files.write(targetDir.resolve(sourceFile.getFileName()), "existing content".getBytes());
        
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(sourceFile.toString()));
        payload.put("targetDir", targetDir.toString());
        payload.put("overwrite", true);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证文件索引被删除
        verify(fileIndexRepository).deleteByPath(sourceFile.toString());
        
        // 验证文件被覆盖移动
        Path targetFile = targetDir.resolve(sourceFile.getFileName());
        assertTrue(Files.exists(targetFile));
        assertFalse(Files.exists(sourceFile));
        assertEquals("test content", Files.readString(targetFile));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testMoveFileWithoutOverwrite_WhenTargetExists() throws IOException {
        // 在目标目录创建同名文件
        Files.write(targetDir.resolve(sourceFile.getFileName()), "existing content".getBytes());
        
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(sourceFile.toString()));
        payload.put("targetDir", targetDir.toString());
        payload.put("overwrite", false);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务失败
        verifyTaskFailure(task, "目标文件已存在且不允许覆盖：" + targetDir.resolve(sourceFile.getFileName()));
        
        // 验证源文件和目标文件都保持原样
        assertTrue(Files.exists(sourceFile));
        assertEquals("test content", Files.readString(sourceFile));
        assertEquals("existing content", Files.readString(targetDir.resolve(sourceFile.getFileName())));
    }

    @Test
    void testMoveMultipleFiles_Success() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(
                sourceFile.toString(),
                sourceFile2.toString()
        ));
        payload.put("targetDir", targetDir.toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证文件索引被删除
        verify(fileIndexRepository).deleteByPath(sourceFile.toString());
        verify(fileIndexRepository).deleteByPath(sourceFile2.toString());
        
        // 验证文件确实被移动了
        assertTrue(Files.exists(targetDir.resolve(sourceFile.getFileName())));
        assertTrue(Files.exists(targetDir.resolve(sourceFile2.getFileName())));
        assertFalse(Files.exists(sourceFile));
        assertFalse(Files.exists(sourceFile2));
        
        // 验证文件内容
        assertEquals("test content", Files.readString(targetDir.resolve(sourceFile.getFileName())));
        assertEquals("test content 2", Files.readString(targetDir.resolve(sourceFile2.getFileName())));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testMoveMultipleFiles_WithTags() throws IOException {
        // 准备文件索引数据
        FileIndex sourceFileIndex1 = new FileIndex();
        sourceFileIndex1.setId(1L);
        sourceFileIndex1.setPath(sourceFile.toString());
        sourceFileIndex1.setName(sourceFile.getFileName().toString());
        sourceFileIndex1.setIsDir(false);
        sourceFileIndex1.setSize(Files.size(sourceFile));

        FileIndex sourceFileIndex2 = new FileIndex();
        sourceFileIndex2.setId(2L);
        sourceFileIndex2.setPath(sourceFile2.toString());
        sourceFileIndex2.setName(sourceFile2.getFileName().toString());
        sourceFileIndex2.setIsDir(false);
        sourceFileIndex2.setSize(Files.size(sourceFile2));

        // 准备标签数据
        FileTag tag1 = new FileTag(1L, 101L);
        FileTag tag2 = new FileTag(2L, 102L);

        when(fileIndexRepository.findByPath(sourceFile.toString())).thenReturn(sourceFileIndex1);
        when(fileIndexRepository.findByPath(sourceFile2.toString())).thenReturn(sourceFileIndex2);
        when(fileTagRepository.findByFileId(1L)).thenReturn(Arrays.asList(tag1));
        when(fileTagRepository.findByFileId(2L)).thenReturn(Arrays.asList(tag2));
        when(fileIndexRepository.save(any())).thenReturn(new FileIndex());

        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(
                sourceFile.toString(),
                sourceFile2.toString()
        ));
        payload.put("targetDir", targetDir.toString());
        payload.put("moveTags", true);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证文件索引被删除
        verify(fileIndexRepository).deleteByPath(sourceFile.toString());
        verify(fileIndexRepository).deleteByPath(sourceFile2.toString());
        
        // 验证新的文件索引被创建
        verify(fileIndexRepository, times(2)).save(any(FileIndex.class));
        
        // 验证标签被移动
        verify(fileTagRepository, times(2)).save(any(FileTag.class));
        
        // 验证文件确实被移动了
        assertTrue(Files.exists(targetDir.resolve(sourceFile.getFileName())));
        assertTrue(Files.exists(targetDir.resolve(sourceFile2.getFileName())));
        assertFalse(Files.exists(sourceFile));
        assertFalse(Files.exists(sourceFile2));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testMoveMultipleFiles_PartialFailure() throws IOException {
        // 创建一个不存在的文件路径
        String nonExistentFile = tempDir.resolve("non_existent.txt").toString();

        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(
                sourceFile.toString(),
                nonExistentFile
        ));
        payload.put("targetDir", targetDir.toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务失败
        verifyTaskFailure(task, "源文件不存在：" + nonExistentFile);
        
        // 验证源文件仍然存在
        assertTrue(Files.exists(sourceFile));
    }

    @Test
    void testMoveMultipleFiles_WithOverwrite() throws IOException {
        // 在目标目录创建同名文件
        Files.write(targetDir.resolve(sourceFile.getFileName()), "existing content".getBytes());
        Files.write(targetDir.resolve(sourceFile2.getFileName()), "existing content 2".getBytes());
        
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(
                sourceFile.toString(),
                sourceFile2.toString()
        ));
        payload.put("targetDir", targetDir.toString());
        payload.put("overwrite", true);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证文件索引被删除
        verify(fileIndexRepository).deleteByPath(sourceFile.toString());
        verify(fileIndexRepository).deleteByPath(sourceFile2.toString());
        
        // 验证文件被覆盖移动
        assertTrue(Files.exists(targetDir.resolve(sourceFile.getFileName())));
        assertTrue(Files.exists(targetDir.resolve(sourceFile2.getFileName())));
        assertFalse(Files.exists(sourceFile));
        assertFalse(Files.exists(sourceFile2));
        assertEquals("test content", Files.readString(targetDir.resolve(sourceFile.getFileName())));
        assertEquals("test content 2", Files.readString(targetDir.resolve(sourceFile2.getFileName())));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testMoveMultipleFiles_WithoutOverwrite_WhenTargetExists() throws IOException {
        // 在目标目录创建同名文件
        Files.write(targetDir.resolve(sourceFile.getFileName()), "existing content".getBytes());
        
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(
                sourceFile.toString(),
                sourceFile2.toString()
        ));
        payload.put("targetDir", targetDir.toString());
        payload.put("overwrite", false);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务失败
        verifyTaskFailure(task, "目标文件已存在且不允许覆盖：" + targetDir.resolve(sourceFile.getFileName()));
        
        // 验证所有源文件和目标文件都保持原样
        assertTrue(Files.exists(sourceFile));
        assertTrue(Files.exists(sourceFile2));
        assertEquals("test content", Files.readString(sourceFile));
        assertEquals("existing content", Files.readString(targetDir.resolve(sourceFile.getFileName())));
    }

    @Test
    void testTaskDescription() {
        JSONObject multiPayload = new JSONObject();
        multiPayload.put("selectedPaths", Arrays.asList(sourceFile.toString(), sourceFile2.toString()));
        multiPayload.put("targetDir", targetDir.toString());
        Task multiTask = createTestTask(multiPayload);
        String multiDesc = taskHandler.getTaskDesc(multiTask);
        assertTrue(multiDesc.contains("批量移动:"));
        assertTrue(multiDesc.contains("2 个文件/目录"));
        assertTrue(multiDesc.contains(targetDir.toString()));
    }
} 