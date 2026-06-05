-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`        VARCHAR(50)  NOT NULL                COMMENT '登录用户名',
    `password`        VARCHAR(100) NOT NULL                COMMENT '密码(BCrypt)',
    `nickname`        VARCHAR(50)  NOT NULL                COMMENT '昵称',
    `avatar`          VARCHAR(500) DEFAULT NULL            COMMENT '头像URL',
    `email`           VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
    `bio`             VARCHAR(300) DEFAULT NULL            COMMENT '个人简介',
    `github_url`      VARCHAR(200) DEFAULT NULL            COMMENT 'GitHub主页',
    `website_url`     VARCHAR(200) DEFAULT NULL            COMMENT '个人网站',
    `role`            TINYINT      NOT NULL DEFAULT 0      COMMENT '角色: 0-普通用户 1-管理员',
    `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0-禁用 1-正常',
    `follower_count`  INT          NOT NULL DEFAULT 0      COMMENT '粉丝数',
    `following_count` INT          NOT NULL DEFAULT 0      COMMENT '关注数',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '分类名称',
    `description` VARCHAR(200) DEFAULT NULL            COMMENT '分类描述',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- 3. 标签表
CREATE TABLE IF NOT EXISTS `tag` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `name`        VARCHAR(50) NOT NULL                COMMENT '标签名称',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- 4. 文章表
CREATE TABLE IF NOT EXISTS `article` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `author_id`      BIGINT       NOT NULL               COMMENT '作者ID',
    `category_id`    BIGINT       NOT NULL               COMMENT '分类ID',
    `title`          VARCHAR(200) NOT NULL               COMMENT '文章标题',
    `summary`        VARCHAR(500) DEFAULT NULL           COMMENT '文章摘要',
    `content`        LONGTEXT     NOT NULL               COMMENT '文章正文(Markdown)',
    `cover_img`      VARCHAR(500) DEFAULT NULL           COMMENT '封面图URL',
    `status`         TINYINT      NOT NULL DEFAULT 0     COMMENT '状态: 0-草稿 1-已发布 2-下架',
    `is_top`         TINYINT      NOT NULL DEFAULT 0     COMMENT '是否置顶: 0-否 1-是',
    `view_count`     INT          NOT NULL DEFAULT 0     COMMENT '浏览数',
    `like_count`     INT          NOT NULL DEFAULT 0     COMMENT '点赞数',
    `favorite_count` INT          NOT NULL DEFAULT 0     COMMENT '收藏数',
    `comment_count`  INT          NOT NULL DEFAULT 0     COMMENT '评论数',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_create_time` (`status`, `create_time`),
    KEY `idx_category_status` (`category_id`, `status`),
    KEY `idx_author_id` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- 5. 文章-标签关联表
CREATE TABLE IF NOT EXISTS `article_tag` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id` BIGINT NOT NULL               COMMENT '文章ID',
    `tag_id`     BIGINT NOT NULL               COMMENT '标签ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章-标签关联表';

-- 6. 评论表 (支持楼中楼)
CREATE TABLE IF NOT EXISTS `comment` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `article_id`    BIGINT   NOT NULL               COMMENT '文章ID',
    `user_id`       BIGINT   NOT NULL               COMMENT '评论者ID',
    `parent_id`     BIGINT   NOT NULL DEFAULT 0     COMMENT '父评论ID(0=顶级评论)',
    `reply_user_id` BIGINT   DEFAULT NULL           COMMENT '被回复用户ID',
    `content`       TEXT     NOT NULL               COMMENT '评论内容',
    `status`        TINYINT  NOT NULL DEFAULT 1     COMMENT '状态: 0-隐藏 1-正常',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_parent_time` (`article_id`, `parent_id`, `create_time`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 7. 文章点赞表
CREATE TABLE IF NOT EXISTS `article_like` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id` BIGINT   NOT NULL               COMMENT '文章ID',
    `user_id`    BIGINT   NOT NULL               COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞表';

-- 8. 用户关注表
CREATE TABLE IF NOT EXISTS `user_follow` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT   NOT NULL               COMMENT '关注者ID',
    `follow_user_id` BIGINT   NOT NULL               COMMENT '被关注者ID',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`),
    KEY `idx_follow_user_id` (`follow_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

-- 9. 文章收藏表
CREATE TABLE IF NOT EXISTS `article_favorite` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id` BIGINT   NOT NULL               COMMENT '文章ID',
    `user_id`    BIGINT   NOT NULL               COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章收藏表';
