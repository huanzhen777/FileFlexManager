package com.huanzhen.fileflexmanager.domain.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import java.util.List;

public interface TaskRepository {
    Task save(Task task);
    Task findById(Long id);
    int updateTask(Task task);
    List<Task> findPendingTasks();
    List<Task> findRunningTasks();
    
    Page<Task> findAll(int page, int size, Boolean includeCompleted);

    /**
     * 查找所有定时任务
     */
    Page<Task> findScheduledTasks(int page, int size);


    List<Task> findScheduledTasks();

    /**
     * 删除任务
     */
    void deleteById(Long id);
} 