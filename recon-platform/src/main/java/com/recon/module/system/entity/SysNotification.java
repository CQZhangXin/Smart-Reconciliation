package com.recon.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统通知
 */
@Data
@Accessors(chain = true)
@TableName("sys_notification")
public class SysNotification implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long userId;

    private String notifyType;

    private String title;

    private String content;

    private String channel;

    /**
     * 是否已读。约定: 0=未读, 1=已读。
     * <p>使用 Integer 类型以兼容数据库 TINYINT 字段和 MyBatis-Plus 的默认映射，
     * 避免 Boolean 类型在某些数据库方言下的类型转换问题。</p>
     */
    private Integer isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    private String relatedType;

    private Long relatedId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
