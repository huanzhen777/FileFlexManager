package com.huanzhen.fileflexmanager.interfaces.convert;

import com.huanzhen.fileflexmanager.domain.model.entity.Tag;
import com.huanzhen.fileflexmanager.domain.model.vo.TagVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TagVOConvert {
    TagVOConvert INSTANCE = Mappers.getMapper(TagVOConvert.class);

    TagVO toVO(Tag tag);

    List<TagVO> toVOList(List<Tag> tags);

    Tag toEntity(TagVO vo);
} 