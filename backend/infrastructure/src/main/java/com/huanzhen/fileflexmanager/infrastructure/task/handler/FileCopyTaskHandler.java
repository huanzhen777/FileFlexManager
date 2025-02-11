package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileCopyParams;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import com.huanzhen.fileflexmanager.infrastructure.util.RsyncExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Scope("prototype")
@Component
public class FileCopyTaskHandler extends BaseTaskHandler<FileCopyParams> {
    private final RsyncExecutor rsyncExecutor;

    public FileCopyTaskHandler(TaskRepository taskRepository) {
        super(taskRepository);
        this.rsyncExecutor = new RsyncExecutor();
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.FILE_COPY;
    }

    @Override
    protected void doHandle(Task task) throws Exception {
        FileCopyParams params = parseTaskParam(task);
        Assert.notEmpty(params.getSelectedPaths(), "选择文件夹/文件不能为空");

        Path targetDir = Paths.get(params.getTargetDir());
        List<Path> sourcePaths = new ArrayList<>(params.getSelectedPaths().stream().map(Paths::get).toList());

        // 验证所有路径是否存在
        for (Path sourcePath : sourcePaths) {
            if (!Files.exists(sourcePath)) {
                throw new IllegalArgumentException("源文件不存在：" + sourcePath);
            }
        }

        // 检查目标目录是否存在
        if (!Files.exists(targetDir)) {
            throw new IllegalArgumentException("目标目录不存在：" + targetDir);
        }

        log.info("开始复制 {} 个文件到目标目录: {}", sourcePaths.size(), targetDir);
        updateProgress(task, 0, "开始复制...");
        taskRepository.updateTask(task);

        // 使用rsync批量复制
        log.info("使用rsync批量复制 {} 文件", JSON.toJSONString(sourcePaths));
        RsyncExecutor.RsyncOptions options = RsyncExecutor.RsyncOptions.builder()
                .sourcePaths(sourcePaths.stream().map(Path::toString).toList())
                .destinationPath(targetDir.toString())
                .archive(true)
                .verbose(true)
                .showProgress(true)
                .removeSource(false)
                .syncDelete(params.getSyncDelete())
                .build();

        RsyncExecutor.RsyncProgress progress = rsyncExecutor.execute(options, (rsyncProgress) -> {
            updateProgress(task, rsyncProgress.getPercentage(), rsyncProgress.generateMsg());
            taskRepository.updateTask(task);
        });

        log.info(progress.generateMsg());
        task.markAsCompleted(progress.generateMsg());
    }

    @Override
    protected void onCancel(Task task) {
        rsyncExecutor.cancel();
    }

    @Override
    public String getTaskDesc(Task task) {
        FileCopyParams params = parseTaskParam(task);
        return StrUtil.format("批量复制: {} 个文件/目录 -> {}", params.getSelectedPaths().size(), params.getTargetDir());
    }
} 