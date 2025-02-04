package com.huanzhen.fileflexmanager.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskStatus;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import com.huanzhen.fileflexmanager.infrastructure.persistence.converter.TaskDOConvert;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.TaskDO;
import com.huanzhen.fileflexmanager.infrastructure.persistence.mapper.TaskMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TaskRepositoryImpl implements TaskRepository {
    private final TaskMapper taskMapper;
    private final TaskDOConvert taskDOConvert;

    public TaskRepositoryImpl(TaskMapper taskMapper, TaskDOConvert taskDOConvert) {
        this.taskMapper = taskMapper;
        this.taskDOConvert = taskDOConvert;
    }

    @Override
    public Task save(Task task) {
        TaskDO taskDO = taskDOConvert.toTaskDO(task);
        taskMapper.insert(taskDO);
        return taskDOConvert.toTask(taskDO);
    }

    @Override
    public Task findById(Long id) {
        TaskDO taskDO = taskMapper.selectById(id);
        return taskDO != null ? taskDOConvert.toTask(taskDO) : null;
    }

    @Override
    public List<Task> findPendingTasks() {
        LambdaQueryWrapper<TaskDO> query = new LambdaQueryWrapper<>();
        query.eq(TaskDO::getStatus, TaskStatus.PENDING);
        return selectList(query);
    }

    @Override
    public int updateTask(Task task) {
        task.setUpdateTime(LocalDateTime.now());
        return taskMapper.updateById(taskDOConvert.toTaskDO(task));
    }

    @Override
    public List<Task> findRunningTasks() {
        LambdaQueryWrapper<TaskDO> query = new LambdaQueryWrapper<>();
        query.in(TaskDO::getStatus, List.of(TaskStatus.RUNNING));
        query.orderByDesc(TaskDO::getCreateTime);
        return selectList(query);
    }

    private List<Task> selectList(LambdaQueryWrapper<TaskDO> query) {
        return taskMapper.selectList(query)
                .stream()
                .map(taskDOConvert::toTask)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Task> findAll(int page, int size, Boolean includeCompleted) {
        LambdaQueryWrapper<TaskDO> query = new LambdaQueryWrapper<>();
        if (includeCompleted != null && !includeCompleted) {
            query.in(TaskDO::getStatus, Arrays.asList(TaskStatus.PENDING, TaskStatus.RUNNING));
        }
        query.orderByDesc(TaskDO::getCreateTime);

        Page<TaskDO> pageParam = new Page<>(page, size);
        return sleectPage(pageParam, query);
    }


    @Override
    public Page<Task> findScheduledTasks(int page, int size) {
        Page<TaskDO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TaskDO> wrapper = new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getScheduled, true);
        return sleectPage(pageParam, wrapper);
    }

    @Override
    public List<Task> findScheduledTasks() {
        LambdaQueryWrapper<TaskDO> wrapper = new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getScheduled, true);
        return selectList(wrapper);
    }

    private Page<Task> sleectPage(Page<TaskDO> pageParam, LambdaQueryWrapper<TaskDO> wrapper) {
        Page<TaskDO> result = taskMapper.selectPage(pageParam, wrapper);
        Page<Task> taskPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        taskPage.setRecords(result.getRecords().stream()
                .map(taskDOConvert::toTask)
                .collect(Collectors.toList()));
        return taskPage;
    }

    @Override
    public void deleteById(Long id) {
        taskMapper.deleteById(id);
    }
} 