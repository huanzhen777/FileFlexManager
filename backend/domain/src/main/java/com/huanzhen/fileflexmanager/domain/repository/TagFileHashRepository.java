package com.huanzhen.fileflexmanager.domain.repository;

import com.huanzhen.fileflexmanager.domain.model.entity.TagFileHash;
import java.util.List;

public interface TagFileHashRepository {
    TagFileHash save(TagFileHash tagFileHash);
    
    List<TagFileHash> findByTagId(Long tagId);
    
    List<TagFileHash> findByFileHash(String fileHash);
    
    void deleteByTagId(Long tagId);
    
    int deleteByFileHash(String fileHash);
    
    void deleteByTagIdAndFileHash(Long tagId, String fileHash);
    
    /**
     * 批量保存标签和文件hash的关联
     * @param tagFileHashes 标签文件hash关联列表
     * @return 保存后的关联列表
     */
    List<TagFileHash> saveAll(List<TagFileHash> tagFileHashes);

    /**
     * 根据标签ID列表查询文件hash关联
     * @param tagIds 标签ID列表
     * @return 标签文件hash关联列表
     */
    List<TagFileHash> findByTagIds(List<Long> tagIds);

    /**
     * 根据标签ID和文件hash查询关联
     * @param tagId 标签ID
     * @param fileHash 文件hash
     * @return 标签文件hash关联，如果不存在返回null
     */
    TagFileHash findByTagIdAndFileHash(Long tagId, String fileHash);
} 