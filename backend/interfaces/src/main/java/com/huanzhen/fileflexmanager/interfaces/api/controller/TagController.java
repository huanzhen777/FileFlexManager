package com.huanzhen.fileflexmanager.interfaces.api.controller;

import com.huanzhen.fileflexmanager.application.service.TagService;
import com.huanzhen.fileflexmanager.domain.model.BaseResponse;
import com.huanzhen.fileflexmanager.domain.model.entity.Tag;
import com.huanzhen.fileflexmanager.interfaces.convert.TagVOConvert;
import com.huanzhen.fileflexmanager.interfaces.model.req.CreateTagRequest;
import com.huanzhen.fileflexmanager.domain.model.vo.TagVO;
import com.huanzhen.fileflexmanager.interfaces.model.req.UpdateTagRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@CrossOrigin
public class TagController {
    private static final Logger logger = LoggerFactory.getLogger(TagController.class);
    private static final TagVOConvert mapper = TagVOConvert.INSTANCE;

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public BaseResponse<List<TagVO>> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        return BaseResponse.success(mapper.toVOList(tags));
    }

    @PostMapping
    public BaseResponse<TagVO> createTag(@RequestBody CreateTagRequest request) {
        Tag tag = new Tag(request.name(), request.parentId());
        tag.setQuickAccess(request.quickAccess());
        tag.setBindFile(request.bindFile());
        Tag createdTag = tagService.createTag(tag);
        return BaseResponse.success(mapper.toVO(createdTag));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return BaseResponse.success();
    }

    @PutMapping("/{id}")
    public BaseResponse<TagVO> updateTag(@PathVariable Long id, @RequestBody UpdateTagRequest request) {

        // 构建标签对象
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(request.name());
        tag.setQuickAccess(request.quickAccess());
        tag.setBindFile(request.bindFile());
        
        // 调用服务层更新标签
        Tag updatedTag = tagService.updateTag(tag);
        return BaseResponse.success(mapper.toVO(updatedTag));
    }
}

