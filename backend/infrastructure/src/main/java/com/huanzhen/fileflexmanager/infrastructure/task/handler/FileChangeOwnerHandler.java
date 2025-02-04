/*
package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.TaskParams.FileChangeOwnerParams;
import com.huanzhen.fileflexmanager.domain.service.TaskHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileChangeOwnerHandler implements TaskHandler<FileChangeOwnerParams> {


    @Override
    public TaskType getTaskType() {
        return TaskType.FILE_CHANGE_OWNER;
    }



    @Override
    public void handle(Task task) {

        FileChangeOwnerParams param = parseTaskParam(task);
        String path = param.getSelectPath();
        String owner = param.getOwner();
        Assert.isTrue(FileUtil.exist(path));
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "sudo",
                    "chown",
                    "-R",  // 递归修改
                    owner,
                    path
            );

            // 合并错误流到标准输出流
            pb.redirectErrorStream(true);

            // 启动进程
            Process process = pb.start();

            // 读取命令输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("chown output: {}", line);
                }
            }

            // 等待命令执行完成
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("修改文件所有者失败，退出码: {}", exitCode);
                task.markAsFailed(StrUtil.format("修改文件所有者失败，退出码: {}", exitCode));
            }
            task.markAsCompleted();
        } catch (Exception e) {
            log.error("修改文件所有者失败: {}, 错误: {}", path, e.getMessage());
            task.markAsCompleted(e.getMessage());
        }
    }

    @Override
    public String getTaskDesc(Task task) {
        String path = task.getPayload().getString("path");
        String owner = task.getPayload().getString("owner");
        return StrUtil.format("{} 修改文件所有者到 {}", path, owner);
    }

}
*/
