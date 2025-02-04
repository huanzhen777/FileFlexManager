package com.huanzhen.fileflexmanager.interfaces.test.mock;

import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import com.huanzhen.fileflexmanager.infrastructure.task.handler.BaseTaskHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class TestTaskHandler extends BaseTaskHandler<Void> {

    @Autowired
    public TestTaskHandler(TaskRepository taskRepository) {
        super(taskRepository);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.TEST;
    }

    @Override
    protected void doHandle(Task task) throws Exception {
        // 只运行3次，然后结束
        for (int i = 0; i < 3; i++) {
            assertNotCancelled(task, "任务被用户取消");
            Thread.sleep(100);
            updateProgress(task, (i + 1) * 33, "进度：" + ((i + 1) * 33) + "%");
            taskRepository.updateTask(task);
        }
    }

    @Override
    public String getTaskDesc(Task task) {
        return "测试任务";
    }

    @Override
    protected void onCancel(Task task) {

    }
}