-- =============================================
-- 苍穹外卖 - 数据库性能优化索引脚本
-- 说明：为统计/工作台模块的时间范围查询添加索引
-- 执行方式：在 MySQL 中执行此 SQL 文件
-- =============================================

-- 1. 订单表：订单时间单列索引
-- 覆盖场景：pageQuery、countByMap、sumByMap 等按时间范围扫描的查询
CREATE INDEX idx_orders_order_time ON orders(order_time);

-- 2. 订单表：(status, order_time) 复合索引（核心优化）
-- 覆盖场景：
--   - 工作台 getOrderOverView（同一时间范围 + 不同 status 过滤）
--   - 工作台 getBusinessData（status=COMPLETED + 时间范围）
--   - 报表 turnoverStatistics / orderStatistics（status 过滤 + GROUP BY 时间）
--   - countOverViewByMap / sumAndCountByMap 聚合查询
-- 原因：status 等值过滤在前，时间范围扫描在后，符合最左前缀原则
CREATE INDEX idx_orders_status_time ON orders(status, order_time);

-- 3. 用户表：创建时间索引
-- 覆盖场景：用户统计 countByDateRange、工作台新增用户统计
CREATE INDEX idx_user_create_time ON user(create_time);


-- =============================================
-- 验证索引是否生效（执行后可取消注释以下语句验证）
-- =============================================

-- -- 查看 orders 表索引
-- SHOW INDEX FROM orders;
--
-- -- 验证聚合查询是否走索引（type 应为 range 或 ref，key 应显示使用的索引）
-- EXPLAIN select
--     count(*) as allOrders,
--     sum(case when status = 2 then 1 else 0 end) as waitingOrders,
--     sum(case when status = 3 then 1 else 0 end) as deliveredOrders,
--     sum(case when status = 5 then 1 else 0 end) as completedOrders,
--     sum(case when status = 6 then 1 else 0 end) as cancelledOrders
-- from orders
-- where order_time >= '2024-01-01 00:00:00'
--   and order_time < '2024-02-01 00:00:00';
