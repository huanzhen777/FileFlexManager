package com.huanzhen.fileflexmanager.domain.repository;

import com.huanzhen.fileflexmanager.domain.model.entity.FileTag;
import java.util.List;

public interface FileTagRepository {
    FileTag save(FileTag fileTag);
    List<FileTag> findByFileId(Long fileId);
    List<FileTag> findByTagId(Long tagId);
    int deleteByFileId(Long fileId);
    void deleteByTagId(Long tagId);
    void deleteByFileIdAndTagId(Long fileId, Long tagId);
    
    /**
     * 批量删除文件的标签关联
     * @param fileIds 文件ID列表
     */
    void deleteByFileIds(List<Long> fileIds);

    /**
     * 根据标签ID列表查询文件标签关联
     * @param tagIds 标签ID列表
     * @return 文件标签关联列表
     */
    List<FileTag> findByTagIds(List<Long> tagIds);
} 