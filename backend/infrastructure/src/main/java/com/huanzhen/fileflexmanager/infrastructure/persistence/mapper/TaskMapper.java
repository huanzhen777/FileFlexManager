package com.huanzhen.fileflexmanager.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.TaskDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<TaskDO> {
    // 这里不需要定义任何方法，BaseMapper 已经提供了基础的 CRUD 操作
} 