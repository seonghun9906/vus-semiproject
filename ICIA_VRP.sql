USE ICIA_VRP;

DROP TABLE IF EXISTS node;
CREATE TABLE `node` (
 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '노드ID',
 `name` varchar(100) COLLATE utf8_bin NOT NULL,
 `address` varchar(100) COLLATE utf8_bin NOT NULL,
 `phone` varchar(100) COLLATE utf8_bin NOT NULL,
 `x` double(9,6) DEFAULT NULL COMMENT '경도',
 `y` double(9,6) DEFAULT NULL COMMENT '위도',
 `reg_dt` datetime NOT NULL COMMENT '등록일시',
 `mod_dt` datetime DEFAULT NULL COMMENT '수정일시',
 PRIMARY KEY (`id`),
 UNIQUE KEY `UK_node_address` (`address`)
) ENGINE=InnoDB  COMMENT='노드';

DROP TABLE IF EXISTS node_cost;
CREATE TABLE `node_cost` (
 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '노드비용ID',
 `start_node_id` bigint(20) NOT NULL COMMENT '시작노드ID',
 `end_node_id` bigint(20) NOT NULL COMMENT '종료노드ID',
 `distance_meter` bigint(20) DEFAULT NULL COMMENT '이동거리(미터)',
 `duration_second` bigint(20) DEFAULT NULL COMMENT '이동시간(초)',
 `toll_fare` int(11) DEFAULT NULL COMMENT '통행 요금(톨게이트)',
 `taxi_fare` int(11) DEFAULT NULL COMMENT '택시 요금(지자체별, 심야, 시경계, 복합, 콜비 감안)',
 `fuel_price` int(11) DEFAULT NULL COMMENT '해당 시점의 전국 평균 유류비와 연비를 감안한 유류비',
 `path_json` text CHARACTER SET utf8 DEFAULT NULL COMMENT '이동경로JSON [[x,y],[x,y]]',
 `reg_dt` datetime NOT NULL COMMENT '등록일시',
 `mod_dt` datetime DEFAULT NULL COMMENT '수정일시',
 PRIMARY KEY (`id`),
 KEY `idx_node_cost_start_node_id` (`start_node_id`),
 KEY `idx_node_cost_end_node_id` (`end_node_id`)
) ENGINE=InnoDB COMMENT='노드비용';

SELECT * FROM node;
SELECT * FROM node_cost;