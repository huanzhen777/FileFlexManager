package com.huanzhen.fileflexmanager.interfaces.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzhen.fileflexmanager.application.service.FileService;
import com.huanzhen.fileflexmanager.domain.model.BaseResponse;
import com.huanzhen.fileflexmanager.domain.model.entity.FileInfo;
import com.huanzhen.fileflexmanager.domain.model.entity.Tag;
import com.huanzhen.fileflexmanager.interfaces.convert.TagVOConvert;
import com.huanzhen.fileflexmanager.interfaces.model.req.QueryFilesReq;
import com.huanzhen.fileflexmanager.interfaces.model.req.UpdateFileTagsRequest;
import com.huanzhen.fileflexmanager.domain.model.vo.TagVO;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@CrossOrigin
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private static final TagVOConvert tagMapper = TagVOConvert.INSTANCE;

    @Autowired
    private FileService fileService;

    @PostMapping("/queryFiles")
    public BaseResponse<Page<FileInfo>> queryFiles(@RequestBody QueryFilesReq req) {
        return BaseResponse.success(fileService.listFiles(req.path(), req.page(), req.size()));
    }


    @GetMapping("/system-users")
    public BaseResponse<List<String>> getSystemUsers() {
        return BaseResponse.success(fileService.getSystemUsers());
    }

    @PostMapping("/mkdir")
    public BaseResponse<Boolean> createDirectory(@RequestParam String path) {
        return BaseResponse.success(fileService.createDirectory(path));
    }

    @GetMapping("/content")
    public BaseResponse<String> getFileContent(@RequestParam String path) {
        return BaseResponse.success(fileService.readFileContent(path));
    }

    @PostMapping("/content")
    public BaseResponse<Boolean> saveFileContent(@RequestParam String path, @RequestBody String content) {
        return BaseResponse.success(fileService.saveFileContent(path, content));
    }

    @PostMapping("/upload")
    public BaseResponse<Boolean> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("path") String path) {
        return BaseResponse.success(fileService.uploadFile(file, path));
    }

    @GetMapping("/download")
    public void downloadFile(@RequestParam String path, HttpServletResponse response) {
        try {
            // 获取文件名
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

            // 设置响应头
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

            // 写入响应流
            fileService.downloadFile(path, response.getOutputStream());
        } catch (Exception e) {
            logger.error("下载文件失败: {}", e.getMessage());
            throw new RuntimeException("下载文件失败", e);
        }
    }

    @GetMapping("/search")
    public BaseResponse<Page<FileInfo>> searchFiles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return BaseResponse.success(fileService.searchFiles(keyword, page, size));
    }

    @GetMapping("/tags")
    public BaseResponse<List<TagVO>> getFileTags(@RequestParam String path) {
        List<Tag> tags = fileService.getFileTags(path);
        return BaseResponse.success(tagMapper.toVOList(tags));
    }

    @PostMapping("/tags")
    public BaseResponse<Void> updateFileTags(@RequestParam String path, @RequestBody UpdateFileTagsRequest request) {
        fileService.updateFileTags(path, request.tagIds());
        return BaseResponse.success();
    }

    /**
     * 获取包含指定所有标签的文件
     * @param tagIds 标签ID列表
     * @param page 页码
     * @param size 每页大小
     */
    @GetMapping("/get-files-contain-all-tags")
    public BaseResponse<Page<FileInfo>> getFilesContainAllTags(
        @RequestParam String tagIds,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        List<Long> tagIdList = Arrays.stream(tagIds.split(","))
            .map(Long::parseLong)
            .collect(Collectors.toList());
        return BaseResponse.success(fileService.getFilesContainAllTags(tagIdList, page, size));
    }

    /**
     * 获取包含任意一个标签的文件
     * @param tagIds 标签ID列表
     * @param page 页码
     * @param size 每页大小
     */
    @GetMapping("/get-files-contain-any-tags")
    public BaseResponse<Page<FileInfo>> getFilesContainAnyTags(
        @RequestParam String tagIds,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        List<Long> tagIdList = Arrays.stream(tagIds.split(","))
            .map(Long::parseLong)
            .collect(Collectors.toList());
        return BaseResponse.success(fileService.getFilesContainAnyTags(tagIdList, page, size));
    }
}
