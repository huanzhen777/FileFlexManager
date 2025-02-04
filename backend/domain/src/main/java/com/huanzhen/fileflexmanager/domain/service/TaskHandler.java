package com.huanzhen.fileflexmanager.domain.service;

import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;

public interface TaskHandler<T> {
    TaskType getTaskType();

    @SuppressWarnings("unchecked")
    default T parseTaskParam(Task task) {
        if (task.getPayload() == null) {
            return null;
        }
        return (T) task.getPayload().toJavaObject(task.getType().getParamsClass());
    }

    void handle(Task task);

    /**
     * 取消正在执行的任务
     * 实现类应该尽最大努力取消正在执行的任务，但不保证一定能取消成功
     *
     * @param task 要取消的任务
     */
    default void cancel(Task task) {
        throw new RuntimeException("不支持取消");
    }

    String getTaskDesc(Task task);


} 