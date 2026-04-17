-- 农村电商与物流配送系统数据库初始化脚本
-- Tables are created in the currently connected database (configured via MYSQLDATABASE).

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    user_type TINYINT DEFAULT 1 COMMENT '用户类型：1消费者 2农户 3物流人员 4管理员',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    shop_name VARCHAR(100) COMMENT '店铺名称',
    location VARCHAR(100) COMMENT '所在地区',
    description TEXT COMMENT '店铺描述',
    tags VARCHAR(255) COMMENT '店铺标签',
    main_category VARCHAR(50) COMMENT '主营品类',
    background VARCHAR(255) COMMENT '店铺背景图',
    rating DECIMAL(2,1) DEFAULT 5.0 COMMENT '店铺评分',
    followers INT DEFAULT 0 COMMENT '关注人数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除：0否 1是',
    UNIQUE KEY uk_username (username),
    INDEX idx_phone (phone),
    INDEX idx_user_type (user_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    level TINYINT DEFAULT 1 COMMENT '层级',
    icon VARCHAR(255) COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    unit VARCHAR(20) COMMENT '单位：斤、个、盒等',
    stock INT DEFAULT 0 COMMENT '库存',
    main_image VARCHAR(255) COMMENT '主图',
    images TEXT COMMENT '图片列表JSON',
    category_id BIGINT COMMENT '分类ID',
    farmer_id BIGINT COMMENT '农户ID',
    origin VARCHAR(100) COMMENT '产地',
    sales INT DEFAULT 0 COMMENT '销量',
    status TINYINT DEFAULT 0 COMMENT '状态：0下架 1上架',
    review_status TINYINT DEFAULT 0 COMMENT '审核状态：0待审核 1通过 2驳回',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_category_id (category_id),
    INDEX idx_farmer_id (farmer_id),
    INDEX idx_status (status),
    INDEX idx_review_status (review_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 用户地址表
CREATE TABLE IF NOT EXISTS user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地址ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人电话',
    province VARCHAR(50) COMMENT '省',
    city VARCHAR(50) COMMENT '市',
    district VARCHAR(50) COMMENT '区/县',
    detail_address VARCHAR(255) COMMENT '详细地址',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认：0否 1是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址表';

-- 订单表
CREATE TABLE IF NOT EXISTS order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    farmer_id BIGINT COMMENT '农户ID',
    shop_id BIGINT COMMENT '店铺ID',
    shop_name VARCHAR(100) COMMENT '店铺名称',
    total_amount DECIMAL(10,2) COMMENT '订单总金额',
    freight_amount DECIMAL(10,2) DEFAULT 0 COMMENT '运费',
    discount_amount DECIMAL(10,2) DEFAULT 0 COMMENT '优惠金额',
    pay_amount DECIMAL(10,2) COMMENT '实付金额',
    pay_type TINYINT COMMENT '支付方式：1微信 2支付宝',
    status TINYINT DEFAULT 0 COMMENT '订单状态：0待付款 1待发货 2待收货 3已完成 4已取消',
    cancel_type TINYINT DEFAULT 0 COMMENT '取消类型：0未取消 1用户取消 2商家取消',
    receiver_name VARCHAR(50) COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) COMMENT '收货人电话',
    receiver_address VARCHAR(500) COMMENT '收货地址',
    remark VARCHAR(500) COMMENT '订单备注',
    pay_time DATETIME COMMENT '支付时间',
    ship_time DATETIME COMMENT '发货时间',
    logistics_no VARCHAR(100) COMMENT '物流单号',
    logistics_company VARCHAR(50) COMMENT '物流公司',
    sender_name VARCHAR(50) COMMENT '发货联系人',
    sender_phone VARCHAR(20) COMMENT '发货人电话',
    sender_address VARCHAR(500) COMMENT '发货地址',
    receive_time DATETIME COMMENT '收货时间',
    sales_counted TINYINT DEFAULT 0 COMMENT '销量是否已计算：0否 1是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_farmer_id (farmer_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单商品表
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    farmer_id BIGINT COMMENT '农户ID',
    product_name VARCHAR(200) COMMENT '商品名称',
    product_image VARCHAR(255) COMMENT '商品图片',
    price DECIMAL(10,2) COMMENT '单价',
    unit VARCHAR(20) COMMENT '单位',
    quantity INT COMMENT '数量',
    total_amount DECIMAL(10,2) COMMENT '小计金额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id),
    INDEX idx_farmer_id (farmer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品表';

-- 物流表
CREATE TABLE IF NOT EXISTS logistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '物流ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    tracking_no VARCHAR(50) COMMENT '物流单号',
    delivery_id BIGINT COMMENT '配送员ID',
    delivery_name VARCHAR(50) COMMENT '配送员姓名',
    delivery_phone VARCHAR(20) COMMENT '配送员电话',
    status TINYINT DEFAULT 0 COMMENT '物流状态：0待揽件 1已揽件 2运输中 3派送中 4已签收',
    current_location VARCHAR(200) COMMENT '当前位置',
    pickup_time DATETIME COMMENT '揽件时间',
    delivery_time DATETIME COMMENT '派送时间',
    sign_time DATETIME COMMENT '签收时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_order_id (order_id),
    INDEX idx_tracking_no (tracking_no),
    INDEX idx_delivery_id (delivery_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流表';

-- 物流轨迹表
CREATE TABLE IF NOT EXISTS logistics_track (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    logistics_id BIGINT NOT NULL COMMENT '物流ID',
    status TINYINT COMMENT '状态',
    description VARCHAR(500) COMMENT '描述',
    location VARCHAR(200) COMMENT '位置',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_logistics_id (logistics_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流轨迹表';

-- 轮播图表
CREATE TABLE IF NOT EXISTS banner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    title VARCHAR(100) COMMENT '标题',
    image VARCHAR(255) NOT NULL COMMENT '图片URL',
    link VARCHAR(255) COMMENT '链接',
    sort INT DEFAULT 0 COMMENT '排序',
    position TINYINT DEFAULT 1 COMMENT '位置：1Web端 2APP端',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_position (position),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value VARCHAR(500) COMMENT '配置值',
    description VARCHAR(255) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(100) COMMENT '操作',
    method VARCHAR(200) COMMENT '请求方法',
    params TEXT COMMENT '请求参数',
    ip VARCHAR(50) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 农户发货地址表
CREATE TABLE IF NOT EXISTS farmer_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地址ID',
    farmer_id BIGINT NOT NULL COMMENT '农户ID',
    sender_name VARCHAR(50) NOT NULL COMMENT '发货人姓名',
    sender_phone VARCHAR(20) NOT NULL COMMENT '发货人电话',
    province VARCHAR(50) COMMENT '省',
    city VARCHAR(50) COMMENT '市',
    district VARCHAR(50) COMMENT '区/县',
    detail_address VARCHAR(255) COMMENT '详细地址',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认：0否 1是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_farmer_id (farmer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农户发货地址表';

-- 收藏表
CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_product (user_id, product_id),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- 商品举报表
CREATE TABLE IF NOT EXISTS product_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '举报ID',
    product_id BIGINT NOT NULL COMMENT '被举报商品ID',
    product_name VARCHAR(200) COMMENT '商品名称',
    product_image VARCHAR(255) COMMENT '商品图片',
    product_price DECIMAL(10,2) COMMENT '商品价格',
    product_unit VARCHAR(20) COMMENT '商品单位',
    farmer_id BIGINT COMMENT '农户ID',
    farmer_name VARCHAR(50) COMMENT '农户名称',
    reporter_id BIGINT NOT NULL COMMENT '举报人ID',
    reporter_name VARCHAR(50) COMMENT '举报人姓名',
    report_type TINYINT NOT NULL COMMENT '举报类型：1虚假信息 2侵权 3违禁品 4其他',
    report_content TEXT COMMENT '举报内容',
    report_images TEXT COMMENT '举报图片JSON',
    status TINYINT DEFAULT 0 COMMENT '状态：0待处理 1已通过 2已拒绝',
    admin_remark VARCHAR(500) COMMENT '管理员备注',
    handle_time DATETIME COMMENT '处理时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_product_id (product_id),
    INDEX idx_reporter_id (reporter_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品举报表';

-- 消息通知表
CREATE TABLE IF NOT EXISTS user_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(100) NOT NULL COMMENT '消息标题',
    content TEXT COMMENT '消息内容',
    type TINYINT DEFAULT 1 COMMENT '消息类型：1系统通知 2商品通知 3订单通知',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户消息通知表';

-- 商品评价表
CREATE TABLE IF NOT EXISTS product_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_id BIGINT COMMENT '订单ID',
    rating INT NOT NULL COMMENT '评分：1-5星',
    content TEXT COMMENT '评价内容',
    images TEXT COMMENT '评价图片JSON数组',
    video VARCHAR(255) COMMENT '评价视频URL',
    status TINYINT DEFAULT 1 COMMENT '状态：0待审核 1已发布 2已删除',
    is_anonymous TINYINT DEFAULT 0 COMMENT '是否匿名：0否 1是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_product_id (product_id),
    INDEX idx_user_id (user_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    type TINYINT DEFAULT 1 COMMENT '消息类型：1文本 2图片 3订单卡片 4商品卡片',
    content TEXT NOT NULL COMMENT '消息内容（文本直接存，其他存描述）',
    extra JSON COMMENT '扩展信息（图片URL、订单信息、商品信息等）',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读：0否 1是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sender_receiver (sender_id, receiver_id),
    INDEX idx_receiver (receiver_id, is_read),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- 聊天会话表（用于快速获取最近联系人）
CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    target_id BIGINT NOT NULL COMMENT '对方ID（商家或用户）',
    target_type TINYINT DEFAULT 1 COMMENT '对方类型：1用户 2商家',
    last_message_id BIGINT COMMENT '最后一条消息ID',
    last_message_time DATETIME COMMENT '最后消息时间',
    unread_count INT DEFAULT 0 COMMENT '未读消息数',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_target (user_id, target_id),
    INDEX idx_user_time (user_id, last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- 评价举报表
CREATE TABLE IF NOT EXISTS review_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '举报ID',
    review_id BIGINT NOT NULL COMMENT '被举报评价ID',
    reporter_id BIGINT NOT NULL COMMENT '举报人ID',
    report_type TINYINT NOT NULL COMMENT '举报类型：1垃圾广告 2人身攻击 3虚假评价 4其他',
    report_content TEXT COMMENT '举报内容',
    status TINYINT DEFAULT 0 COMMENT '状态：0待处理 1已通过 2已拒绝',
    admin_remark VARCHAR(500) COMMENT '管理员备注',
    handle_time DATETIME COMMENT '处理时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_review_id (review_id),
    INDEX idx_reporter_id (reporter_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- 店铺关注表
CREATE TABLE IF NOT EXISTS shop_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关注ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    shop_id BIGINT NOT NULL COMMENT '店铺ID（农户ID）',
    shop_name VARCHAR(100) COMMENT '店铺名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    UNIQUE KEY uk_user_shop (user_id, shop_id),
    INDEX idx_user_id (user_id),
    INDEX idx_shop_id (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺关注表';

-- 商品介绍表
CREATE TABLE IF NOT EXISTS product_introduction (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  type VARCHAR(20) NOT NULL COMMENT '类型：text_image(文本+图片), image(仅图片)',
  title VARCHAR(100) COMMENT '标题（text_image类型时可选）',
  content TEXT COMMENT '文本内容（text_image类型时必填）',
  image_url VARCHAR(500) COMMENT '图片URL',
  sort_order INT DEFAULT 0 COMMENT '排序顺序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_product_id (product_id),
  KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品介绍表';

-- 订单统计报表表
CREATE TABLE IF NOT EXISTS order_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    total_orders INT DEFAULT 0 COMMENT '订单总数',
    paid_orders INT DEFAULT 0 COMMENT '支付订单数',
    cancelled_orders INT DEFAULT 0 COMMENT '取消订单数',
    completed_orders INT DEFAULT 0 COMMENT '完成订单数',
    total_amount DECIMAL(12,2) DEFAULT 0 COMMENT '订单总金额',
    paid_amount DECIMAL(12,2) DEFAULT 0 COMMENT '实际支付金额',
    refund_amount DECIMAL(12,2) DEFAULT 0 COMMENT '退款金额',
    new_users INT DEFAULT 0 COMMENT '新增用户数',
    active_users INT DEFAULT 0 COMMENT '活跃用户数',
    stat_type TINYINT DEFAULT 1 COMMENT '统计类型：1日统计 2周统计 3月统计',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_stat_date_type (stat_date, stat_type),
    INDEX idx_stat_date (stat_date),
    INDEX idx_stat_type (stat_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单统计报表表';

-- 定时任务执行日志表
CREATE TABLE IF NOT EXISTS scheduled_task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_desc VARCHAR(255) COMMENT '任务描述',
    status TINYINT DEFAULT 1 COMMENT '执行状态：0失败 1成功',
    result VARCHAR(500) COMMENT '执行结果描述',
    start_time DATETIME COMMENT '执行开始时间',
    end_time DATETIME COMMENT '执行结束时间',
    execution_time BIGINT COMMENT '执行耗时（毫秒）',
    error_msg TEXT COMMENT '异常信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_name (task_name),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志表';

-- 初始化数据

-- 插入管理员账号（密码：admin123）
INSERT IGNORE INTO sys_user (username, password, nickname, user_type, status) VALUES
('admin', '0192023a7bbd73250516f069df18b500', '超级管理员', 4, 1);

-- 插入分类数据
INSERT IGNORE INTO category (name, parent_id, level, sort, status) VALUES
('新鲜水果', 0, 1, 1, 1),
('蔬菜菌菇', 0, 1, 2, 1),
('粮油米面', 0, 1, 3, 1),
('肉禽蛋品', 0, 1, 4, 1),
('干货特产', 0, 1, 5, 1),
('农副加工', 0, 1, 6, 1),
('农资农具', 0, 1, 7, 1),
('苗木花卉', 0, 1, 8, 1);

-- 插入系统配置
INSERT IGNORE INTO sys_config (config_key, config_value, description) VALUES
('site.name', '农鲜达', '网站名称'),
('site.logo', '/logo.png', '网站Logo'),
('site.icp', '京ICP备12345678号', '备案号'),
('commission.rate', '0.05', '平台佣金比例'),
('contact.phone', '400-123-4567', '客服电话'),
('order.auto_cancel_minutes', '30', '订单自动取消时间（分钟）'),
('order.auto_confirm_days', '15', '订单自动确认收货时间（天）');
