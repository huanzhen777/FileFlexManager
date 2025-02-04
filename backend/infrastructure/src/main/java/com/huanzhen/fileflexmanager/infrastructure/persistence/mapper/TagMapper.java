package com.huanzhen.fileflexmanager.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.TagDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagMapper extends BaseMapper<TagDO> {
} 