package com.huanzhen.fileflexmanager.domain.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzhen.fileflexmanager.domain.model.entity.FileIndex;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface FileIndexRepository {
    FileIndex save(FileIndex fileIndex);
    FileIndex findById(Long id);
    FileIndex findByPath(String path);
    List<FileIndex> findByParentPath(String parentPath);
    void updateFileIndex(FileIndex fileIndex);
    void deleteByPath(String path);
    void deleteById(Long id);
    Page<FileIndex> findAll(int page, int size);
    List<FileIndex> findByMd5(String md5);
    List<FileIndex> findByMd5List(List<String> md5List);
    Page<FileIndex> search(String keyword, int page, int size);
    Page<FileIndex> findByIds(List<Long> ids, int page, int size);
    int deleteStaleIndexes(String rootPath, LocalDateTime beforeTime);

    List<FileIndex> findFileByHash(Set<String> hashes);
}