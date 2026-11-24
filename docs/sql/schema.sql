-- MySQL 8 兼容。分数一律用整数，避免浮点误差。
CREATE DATABASE IF NOT EXISTS exam_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE exam_system;

CREATE TABLE IF NOT EXISTS sys_user (
    id           BIGINT       NOT NULL PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL,
    password     VARCHAR(100) NOT NULL,
    nickname     VARCHAR(64)  NOT NULL,
    role         VARCHAR(16)  NOT NULL COMMENT 'ADMIN TEACHER STUDENT',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_name (username)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS question_category (
    id           BIGINT       NOT NULL PRIMARY KEY,
    parent_id    BIGINT       NOT NULL DEFAULT 0,
    name         VARCHAR(64)  NOT NULL,
    sort_no      INT          NOT NULL DEFAULT 0,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS question (
    id           BIGINT        NOT NULL PRIMARY KEY,
    category_id  BIGINT        NOT NULL,
    type         VARCHAR(16)   NOT NULL COMMENT 'SINGLE MULTI JUDGE SHORT',
    difficulty   TINYINT       NOT NULL COMMENT '1易 2中 3难',
    stem         VARCHAR(2000) NOT NULL,
    stem_hash    VARCHAR(96)   NOT NULL,
    options_json JSON          NULL,
    answer       VARCHAR(2000) NOT NULL,
    analysis     VARCHAR(2000) NULL,
    keywords     VARCHAR(500)  NULL,
    score        INT           NOT NULL,
    image_url    VARCHAR(500)  NULL,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stem_hash (stem_hash),
    KEY idx_question_pool (category_id, type, deleted)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS paper (
    id             BIGINT       NOT NULL PRIMARY KEY,
    title          VARCHAR(128) NOT NULL,
    description    VARCHAR(500) NULL,
    duration_min   INT          NOT NULL,
    total_score    INT          NOT NULL,
    pass_score     INT          NOT NULL,
    status         VARCHAR(16)  NOT NULL COMMENT 'DRAFT PUBLISHED CLOSED',
    assemble_mode  VARCHAR(16)  NOT NULL COMMENT 'MANUAL SMART',
    creator_id     BIGINT       NOT NULL,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_paper_status (status, deleted)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS paper_question (
    id           BIGINT NOT NULL PRIMARY KEY,
    paper_id     BIGINT NOT NULL,
    question_id  BIGINT NOT NULL,
    score        INT    NOT NULL,
    sort_no      INT    NOT NULL,
    UNIQUE KEY uk_paper_question (paper_id, question_id),
    KEY idx_pq_paper (paper_id, sort_no)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS paper_rule (
    id           BIGINT      NOT NULL PRIMARY KEY,
    paper_id     BIGINT      NOT NULL,
    category_id  BIGINT      NOT NULL,
    type         VARCHAR(16) NOT NULL,
    question_count INT       NOT NULL,
    score_each   INT         NOT NULL,
    KEY idx_rule_paper (paper_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS exam_record (
    id                BIGINT   NOT NULL PRIMARY KEY,
    paper_id          BIGINT   NOT NULL,
    user_id           BIGINT   NOT NULL,
    start_time        DATETIME NOT NULL,
    deadline          DATETIME NOT NULL,
    submit_time       DATETIME NULL,
    status            VARCHAR(16) NOT NULL COMMENT 'ONGOING SUBMITTED TIMEOUT',
    score             INT      NULL,
    objective_score   INT      NULL,
    subjective_score  INT      NULL,
    ai_comment        VARCHAR(2000) NULL,
    UNIQUE KEY uk_user_paper (user_id, paper_id),
    KEY idx_record_paper (paper_id, status),
    KEY idx_record_deadline (status, deadline)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS exam_answer (
    id           BIGINT        NOT NULL PRIMARY KEY,
    record_id    BIGINT        NOT NULL,
    question_id  BIGINT        NOT NULL,
    user_answer  VARCHAR(2000) NULL,
    score        INT           NOT NULL DEFAULT 0,
    correct      TINYINT       NOT NULL DEFAULT 0,
    comment      VARCHAR(1000) NULL,
    UNIQUE KEY uk_record_question (record_id, question_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS practice_record (
    id           BIGINT        NOT NULL PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    question_id  BIGINT        NOT NULL,
    user_answer  VARCHAR(2000) NULL,
    correct      TINYINT       NOT NULL,
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_practice_user (user_id, question_id),
    KEY idx_practice_question (question_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS banner (
    id           BIGINT       NOT NULL PRIMARY KEY,
    title        VARCHAR(64)  NOT NULL,
    image_url    VARCHAR(500) NOT NULL,
    link_url     VARCHAR(500) NULL,
    sort_no      INT          NOT NULL DEFAULT 0,
    status       TINYINT      NOT NULL DEFAULT 1,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;
