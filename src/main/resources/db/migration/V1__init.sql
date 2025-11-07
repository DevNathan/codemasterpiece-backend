CREATE TABLE tbl_category
(
    category_id   VARCHAR(29)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    type          VARCHAR(20)  NOT NULL,
    parent_id     VARCHAR(29),
    link          VARCHAR(255),
    image_file_id VARCHAR(29),
    sort_order    INTEGER      NOT NULL,
    level         INTEGER      NOT NULL,
    CONSTRAINT pk_tbl_category PRIMARY KEY (category_id)
);

CREATE TABLE tbl_comment
(
    comment_id         VARCHAR(29)   NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    content            VARCHAR(2000) NOT NULL,
    post_id            VARCHAR(29)   NOT NULL,
    actor_provider     VARCHAR(16)   NOT NULL,
    actor_id           VARCHAR(100)  NOT NULL,
    parent_id          VARCHAR(29),
    depth              INTEGER       NOT NULL,
    is_hidden          BOOLEAN       NOT NULL,
    is_deleted         BOOLEAN       NOT NULL,
    actor_display_name VARCHAR(60)   NOT NULL,
    actor_image_url    VARCHAR(512),
    guest_pin_hash     VARCHAR(100),
    CONSTRAINT pk_tbl_comment PRIMARY KEY (comment_id)
);

CREATE TABLE tbl_comment_reaction
(
    reaction_id    VARCHAR(29)  NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    comment_id     VARCHAR(29)  NOT NULL,
    actor_provider VARCHAR(16)  NOT NULL,
    actor_id       VARCHAR(100) NOT NULL,
    value          VARCHAR(10)  NOT NULL,
    CONSTRAINT pk_tbl_comment_reaction PRIMARY KEY (reaction_id)
);

CREATE TABLE tbl_file
(
    file_id           VARCHAR(29)  NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status            VARCHAR(12)  NOT NULL,
    storage_path      VARCHAR(512) NOT NULL,
    storage_type      VARCHAR(16)  NOT NULL,
    storage_key       VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255),
    byte_size         BIGINT       NOT NULL,
    content_type      VARCHAR(255),
    ref_count         INTEGER      NOT NULL,
    deletable_at      TIMESTAMP WITHOUT TIME ZONE,
    deleted_at        TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_tbl_file PRIMARY KEY (file_id)
);

CREATE TABLE tbl_file_ref
(
    file_ref_id  VARCHAR(29) NOT NULL,
    file_id      VARCHAR(29) NOT NULL,
    owner_type   VARCHAR(32) NOT NULL,
    owner_id     VARCHAR(29) NOT NULL,
    purpose      VARCHAR(32) NOT NULL,
    sort_order   INTEGER,
    display_name VARCHAR(255),
    CONSTRAINT pk_tbl_file_ref PRIMARY KEY (file_ref_id)
);

CREATE TABLE tbl_file_variant
(
    variant_id   VARCHAR(29)  NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    file_id      VARCHAR(29)  NOT NULL,
    kind         VARCHAR(32)  NOT NULL,
    storage_type VARCHAR(16)  NOT NULL,
    status       VARCHAR(12)  NOT NULL,
    storage_key  VARCHAR(512) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    width        INTEGER,
    height       INTEGER,
    byte_size    BIGINT       NOT NULL,
    CONSTRAINT pk_tbl_file_variant PRIMARY KEY (variant_id)
);

CREATE TABLE tbl_guestbook
(
    guestbook_id       VARCHAR(29)   NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    content            VARCHAR(2000) NOT NULL,
    actor_provider     VARCHAR(16)   NOT NULL,
    actor_id           VARCHAR(100)  NOT NULL,
    actor_display_name VARCHAR(60)   NOT NULL,
    actor_image_url    VARCHAR(512),
    guest_pin_hash     VARCHAR(100),
    CONSTRAINT pk_tbl_guestbook PRIMARY KEY (guestbook_id)
);

CREATE TABLE tbl_page_view
(
    page_view_id    VARCHAR(29) NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    occurred_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    received_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    day             VARCHAR(10) NOT NULL,
    hour_bucket     SMALLINT    NOT NULL,
    cid             VARCHAR(40),
    sid             VARCHAR(40),
    url             VARCHAR(1024),
    url_host        VARCHAR(255),
    url_path        VARCHAR(1024),
    url_query       VARCHAR(1024),
    ref             VARCHAR(1024),
    ref_host        VARCHAR(255),
    ref_path        VARCHAR(1024),
    is_external_ref BOOLEAN     NOT NULL,
    title           VARCHAR(255),
    lang            VARCHAR(16),
    device          VARCHAR(32),
    browser         VARCHAR(64),
    os              VARCHAR(64),
    vp_w            INTEGER,
    vp_h            INTEGER,
    ip_masked       VARCHAR(64),
    country         VARCHAR(2),
    city            VARCHAR(64),
    is_bot          BOOLEAN     NOT NULL,
    utm_source      VARCHAR(80),
    utm_medium      VARCHAR(80),
    utm_campaign    VARCHAR(120),
    utm_term        VARCHAR(120),
    utm_content     VARCHAR(120),
    CONSTRAINT pk_tbl_page_view PRIMARY KEY (page_view_id)
);

CREATE TABLE tbl_post
(
    post_id       VARCHAR(29)  NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    slug          VARCHAR(200) NOT NULL,
    title         VARCHAR(255) NOT NULL,
    category_id   VARCHAR(29)  NOT NULL,
    head_image_id VARCHAR(29),
    head_content  VARCHAR(1000),
    main_content  TEXT,
    link          VARCHAR(100) NOT NULL,
    view_count    BIGINT       NOT NULL,
    like_count    BIGINT       NOT NULL,
    is_published  BOOLEAN      NOT NULL,
    CONSTRAINT pk_tbl_post PRIMARY KEY (post_id)
);

CREATE TABLE tbl_post_like
(
    like_id        VARCHAR(29)  NOT NULL,
    post_id        VARCHAR(29)  NOT NULL,
    actor_provider VARCHAR(16)  NOT NULL,
    actor_id       VARCHAR(100) NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_tbl_post_like PRIMARY KEY (like_id)
);

CREATE TABLE tbl_post_tag
(
    sort_order INTEGER     NOT NULL,
    post_id    VARCHAR(29) NOT NULL,
    tag_id     VARCHAR(29) NOT NULL,
    CONSTRAINT pk_tbl_post_tag PRIMARY KEY (post_id, tag_id)
);

CREATE TABLE tbl_pv_daily
(
    key_date   date   NOT NULL,
    views      BIGINT NOT NULL,
    uv         BIGINT NOT NULL,
    sessions   BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_tbl_pv_daily PRIMARY KEY (key_date)
);

CREATE TABLE tbl_pv_monthly
(
    key_date   date   NOT NULL,
    views      BIGINT NOT NULL,
    uv         BIGINT NOT NULL,
    sessions   BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_tbl_pv_monthly PRIMARY KEY (key_date)
);

CREATE TABLE tbl_pv_weekly
(
    key_date   date   NOT NULL,
    views      BIGINT NOT NULL,
    uv         BIGINT NOT NULL,
    sessions   BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_tbl_pv_weekly PRIMARY KEY (key_date)
);

CREATE TABLE tbl_tag
(
    tag_id VARCHAR(29) NOT NULL,
    name   VARCHAR(60) NOT NULL,
    CONSTRAINT pk_tbl_tag PRIMARY KEY (tag_id)
);

ALTER TABLE tbl_category
    ADD CONSTRAINT uc_tbl_category_link UNIQUE (link);

ALTER TABLE tbl_category
    ADD CONSTRAINT uc_tbl_category_name UNIQUE (name);

ALTER TABLE tbl_comment_reaction
    ADD CONSTRAINT uk_comment_reaction_actor UNIQUE (comment_id, actor_provider, actor_id);

ALTER TABLE tbl_tag
    ADD CONSTRAINT uk_tags_name UNIQUE (name);

ALTER TABLE tbl_category
    ADD CONSTRAINT uq_category_parent_name UNIQUE (parent_id, name);

ALTER TABLE tbl_post_like
    ADD CONSTRAINT uq_like_post_actor UNIQUE (post_id, actor_provider, actor_id);

ALTER TABLE tbl_post
    ADD CONSTRAINT uq_post_slug UNIQUE (slug);

ALTER TABLE tbl_post_tag
    ADD CONSTRAINT uq_posttag_post_sort UNIQUE (post_id, sort_order);

ALTER TABLE tbl_post_tag
    ADD CONSTRAINT uq_posttag_post_tag UNIQUE (post_id, tag_id);

CREATE INDEX idx_category_type ON tbl_category (type);

CREATE INDEX idx_comment_parent_created ON tbl_comment (parent_id, created_at);

CREATE INDEX idx_comment_post_created ON tbl_comment (post_id, created_at);

CREATE INDEX idx_fref_owner ON tbl_file_ref (owner_type, owner_id);

CREATE INDEX idx_fref_owner_purpose ON tbl_file_ref (owner_type, owner_id, purpose);

CREATE INDEX idx_fref_sort ON tbl_file_ref (owner_type, owner_id, purpose, sort_order);

CREATE INDEX idx_guestbook_created_id_desc ON tbl_guestbook (created_at DESC, guestbook_id DESC);

CREATE INDEX idx_like_actor ON tbl_post_like (actor_provider, actor_id);

CREATE INDEX idx_like_post_created ON tbl_post_like (post_id, created_at);

CREATE INDEX idx_post_cat_created ON tbl_post (category_id, created_at);

CREATE INDEX idx_post_cat_likes ON tbl_post (category_id, like_count);

CREATE INDEX idx_post_cat_published ON tbl_post (category_id, is_published);

CREATE INDEX idx_post_cat_title ON tbl_post (category_id, title);

CREATE INDEX idx_post_cat_updated ON tbl_post (category_id, updated_at);

CREATE INDEX idx_post_cat_views ON tbl_post (category_id, view_count);

CREATE INDEX idx_post_created_at ON tbl_post (created_at);

CREATE INDEX idx_post_like_count ON tbl_post (like_count);

CREATE INDEX idx_post_link ON tbl_post (link);

CREATE INDEX idx_post_link_pub_created ON tbl_post (link, is_published, created_at);

CREATE INDEX idx_post_title ON tbl_post (title);

CREATE INDEX idx_post_updated_at ON tbl_post (updated_at);

CREATE INDEX idx_post_view_count ON tbl_post (view_count);

CREATE INDEX idx_posttag_post_sort ON tbl_post_tag (post_id, sort_order);

CREATE INDEX idx_pv_bot_day ON tbl_page_view (is_bot, day);

CREATE INDEX idx_pv_cid_day ON tbl_page_view (cid, day);

CREATE INDEX idx_pv_day ON tbl_page_view (day);

CREATE INDEX idx_pv_device_day ON tbl_page_view (device, day);

CREATE INDEX idx_pv_host_day ON tbl_page_view (url_host, day);

CREATE INDEX idx_pv_path_day ON tbl_page_view (url_path, day);

CREATE INDEX idx_pv_received ON tbl_page_view (received_at);

CREATE INDEX idx_pv_ref_host_day ON tbl_page_view (ref_host, day);

CREATE INDEX idx_pv_sid_day ON tbl_page_view (sid, day);

CREATE INDEX idx_pv_utm_day ON tbl_page_view (utm_source, utm_campaign, day);

ALTER TABLE tbl_category
    ADD CONSTRAINT FK_CATEGORY_IMAGE_FILE FOREIGN KEY (image_file_id) REFERENCES tbl_file (file_id);

ALTER TABLE tbl_category
    ADD CONSTRAINT FK_CATEGORY_PARENT FOREIGN KEY (parent_id) REFERENCES tbl_category (category_id);

ALTER TABLE tbl_comment
    ADD CONSTRAINT FK_COMMENT_PARENT FOREIGN KEY (parent_id) REFERENCES tbl_comment (comment_id) ON DELETE CASCADE;

ALTER TABLE tbl_comment
    ADD CONSTRAINT FK_COMMENT_POST FOREIGN KEY (post_id) REFERENCES tbl_post (post_id) ON DELETE CASCADE;

ALTER TABLE tbl_comment_reaction
    ADD CONSTRAINT FK_COMMENT_REACTION_COMMENT FOREIGN KEY (comment_id) REFERENCES tbl_comment (comment_id) ON DELETE CASCADE;

ALTER TABLE tbl_file_ref
    ADD CONSTRAINT FK_FREF_FILE FOREIGN KEY (file_id) REFERENCES tbl_file (file_id);

CREATE INDEX idx_fref_file ON tbl_file_ref (file_id);

ALTER TABLE tbl_post_like
    ADD CONSTRAINT FK_LIKE_POST FOREIGN KEY (post_id) REFERENCES tbl_post (post_id);

ALTER TABLE tbl_post_tag
    ADD CONSTRAINT FK_POSTTAG_POST FOREIGN KEY (post_id) REFERENCES tbl_post (post_id);

ALTER TABLE tbl_post_tag
    ADD CONSTRAINT FK_POSTTAG_TAG FOREIGN KEY (tag_id) REFERENCES tbl_tag (tag_id);

ALTER TABLE tbl_post
    ADD CONSTRAINT FK_POST_CATEGORY FOREIGN KEY (category_id) REFERENCES tbl_category (category_id);

ALTER TABLE tbl_post
    ADD CONSTRAINT FK_POST_IMAGE_FILE FOREIGN KEY (head_image_id) REFERENCES tbl_file (file_id);

ALTER TABLE tbl_file_variant
    ADD CONSTRAINT FK_VARIANT_FILE FOREIGN KEY (file_id) REFERENCES tbl_file (file_id);