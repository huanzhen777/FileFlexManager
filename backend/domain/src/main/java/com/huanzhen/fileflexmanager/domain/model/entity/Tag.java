package com.huanzhen.fileflexmanager.domain.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@NoArgsConstructor
@TableName("tag")
public class Tag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;
    private String path;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean quickAccess;
    private Boolean bindFile;
    private Long fileCount;

    public Tag(String name, Long parentId) {
        this.name = name;
        this.parentId = parentId;
        this.quickAccess = false;
        this.bindFile = false;
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    public void updatePath(String path) {
        this.path = path;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 判断指定的标签是否是当前标签的父标签
     * @param potentialParent 可能的父标签
     * @return 如果是父标签返回true，否则返回false
     */
    public boolean isChildOf(Tag potentialParent) {
        if (potentialParent == null || this.path == null || potentialParent.getPath() == null) {
            return false;
        }
        
        // 如果当前标签的路径长度小于等于潜在父标签的路径长度，则不可能是子标签
        if (this.path.length() <= potentialParent.getPath().length()) {
            return false;
        }

        // 检查当前标签的路径是否以潜在父标签的路径开头，并且紧接着是路径分隔符
        return this.path.startsWith(potentialParent.getPath() + "/");
    }

    /**
     * 获取标签的层级深度
     * @return 标签的层级深度，根标签返回0
     */
    public int getDepth() {
        if (this.path == null || this.path.isEmpty()) {
            return 0;
        }
        return (int) this.path.chars().filter(ch -> ch == '/').count();
    }

} 