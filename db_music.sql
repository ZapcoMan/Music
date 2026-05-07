/*
 Navicat Premium Data Transfer

 Source Server         : phpstudy mysql8.0
 Source Server Type    : MySQL
 Source Server Version : 80012
 Source Host           : localhost:3306
 Source Schema         : db_music

 Target Server Type    : MySQL
 Target Server Version : 80012
 File Encoding         : 65001

 Date: 07/05/2026 20:14:13
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '账号',
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '管理员' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES (1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

-- ----------------------------
-- Table structure for collect
-- ----------------------------
DROP TABLE IF EXISTS `collect`;
CREATE TABLE `collect`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int(11) NULL DEFAULT NULL COMMENT '用户id',
  `type` tinyint(1) NULL DEFAULT NULL COMMENT '收藏类型（0歌曲1歌单）',
  `song_id` int(11) NULL DEFAULT NULL COMMENT '歌曲id',
  `song_list_id` int(11) NULL DEFAULT NULL COMMENT '歌单id',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 184 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '收藏' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of collect
-- ----------------------------
INSERT INTO `collect` VALUES (114, 35, 0, 160, NULL, '2024-05-20 14:24:11');
INSERT INTO `collect` VALUES (115, 35, 0, 156, NULL, '2024-05-20 14:24:26');
INSERT INTO `collect` VALUES (117, 35, 0, 27, NULL, '2024-05-20 15:06:35');
INSERT INTO `collect` VALUES (122, 35, 0, 138, NULL, '2024-05-20 21:43:57');
INSERT INTO `collect` VALUES (123, 49, 0, 27, NULL, '2024-05-20 22:32:41');
INSERT INTO `collect` VALUES (124, 49, 0, 166, NULL, '2024-05-20 22:32:43');
INSERT INTO `collect` VALUES (125, 49, 0, 167, NULL, '2024-05-20 22:32:45');
INSERT INTO `collect` VALUES (126, 49, 0, 159, NULL, '2024-05-20 22:32:53');
INSERT INTO `collect` VALUES (127, 49, 0, 118, NULL, '2024-05-20 22:32:54');
INSERT INTO `collect` VALUES (128, 49, 0, 171, NULL, '2024-05-20 22:36:57');
INSERT INTO `collect` VALUES (129, 49, 0, 127, NULL, '2024-05-20 22:36:58');
INSERT INTO `collect` VALUES (130, 35, 0, 161, NULL, '2024-05-20 22:37:34');
INSERT INTO `collect` VALUES (131, 35, 0, 167, NULL, '2024-05-20 22:37:36');
INSERT INTO `collect` VALUES (132, 35, 0, 159, NULL, '2024-05-20 22:37:45');
INSERT INTO `collect` VALUES (133, 35, 0, 127, NULL, '2024-05-20 22:37:56');
INSERT INTO `collect` VALUES (134, 47, 0, 116, NULL, '2024-05-20 22:38:29');
INSERT INTO `collect` VALUES (135, 47, 0, 167, NULL, '2024-05-20 22:38:36');
INSERT INTO `collect` VALUES (136, 47, 0, 160, NULL, '2024-05-20 22:38:41');
INSERT INTO `collect` VALUES (137, 47, 0, 139, NULL, '2024-05-20 22:38:56');
INSERT INTO `collect` VALUES (138, 42, 0, 27, NULL, '2024-05-20 22:39:21');
INSERT INTO `collect` VALUES (139, 42, 0, 160, NULL, '2024-05-20 22:39:27');
INSERT INTO `collect` VALUES (140, 42, 0, 118, NULL, '2024-05-20 22:39:29');
INSERT INTO `collect` VALUES (141, 42, 0, 139, NULL, '2024-05-20 22:39:42');
INSERT INTO `collect` VALUES (142, 40, 0, 27, NULL, '2024-05-20 22:40:29');
INSERT INTO `collect` VALUES (143, 40, 0, 158, NULL, '2024-05-20 22:40:35');
INSERT INTO `collect` VALUES (144, 40, 0, 160, NULL, '2024-05-20 22:40:38');
INSERT INTO `collect` VALUES (145, 40, 0, 138, NULL, '2024-05-20 22:40:39');
INSERT INTO `collect` VALUES (146, 40, 0, 169, NULL, '2024-05-20 22:40:44');
INSERT INTO `collect` VALUES (147, 48, 0, 27, NULL, '2024-05-20 22:41:05');
INSERT INTO `collect` VALUES (148, 48, 0, 167, NULL, '2024-05-20 22:41:07');
INSERT INTO `collect` VALUES (149, 48, 0, 160, NULL, '2024-05-20 22:41:12');
INSERT INTO `collect` VALUES (150, 48, 0, 169, NULL, '2024-05-20 22:41:19');
INSERT INTO `collect` VALUES (151, 46, 0, 116, NULL, '2024-05-20 22:41:39');
INSERT INTO `collect` VALUES (152, 46, 0, 167, NULL, '2024-05-20 22:41:41');
INSERT INTO `collect` VALUES (153, 46, 0, 138, NULL, '2024-05-20 22:41:46');
INSERT INTO `collect` VALUES (154, 46, 0, 169, NULL, '2024-05-20 22:41:49');
INSERT INTO `collect` VALUES (155, 46, 0, 127, NULL, '2024-05-20 22:41:55');
INSERT INTO `collect` VALUES (156, 46, 0, 171, NULL, '2024-05-20 22:41:57');
INSERT INTO `collect` VALUES (157, 45, 0, 27, NULL, '2024-05-20 22:42:19');
INSERT INTO `collect` VALUES (158, 45, 0, 167, NULL, '2024-05-20 22:42:20');
INSERT INTO `collect` VALUES (159, 45, 0, 116, NULL, '2024-05-20 22:42:22');
INSERT INTO `collect` VALUES (160, 45, 0, 161, NULL, '2024-05-20 22:42:25');
INSERT INTO `collect` VALUES (161, 45, 0, 166, NULL, '2024-05-20 22:42:27');
INSERT INTO `collect` VALUES (162, 45, 0, 159, NULL, '2024-05-20 22:42:31');
INSERT INTO `collect` VALUES (163, 45, 0, 118, NULL, '2024-05-20 22:42:32');
INSERT INTO `collect` VALUES (164, 45, 0, 139, NULL, '2024-05-20 22:42:34');
INSERT INTO `collect` VALUES (165, 45, 0, 158, NULL, '2024-05-20 22:42:35');
INSERT INTO `collect` VALUES (166, 45, 0, 169, NULL, '2024-05-20 22:42:42');
INSERT INTO `collect` VALUES (167, 45, 0, 127, NULL, '2024-05-20 22:42:48');
INSERT INTO `collect` VALUES (168, 45, 0, 171, NULL, '2024-05-20 22:42:49');
INSERT INTO `collect` VALUES (169, 44, 0, 166, NULL, '2024-05-20 22:43:23');
INSERT INTO `collect` VALUES (170, 44, 0, 161, NULL, '2024-05-20 22:43:24');
INSERT INTO `collect` VALUES (171, 44, 0, 118, NULL, '2024-05-20 22:43:28');
INSERT INTO `collect` VALUES (172, 44, 0, 127, NULL, '2024-05-20 22:43:37');
INSERT INTO `collect` VALUES (173, 44, 0, 171, NULL, '2024-05-20 22:43:40');
INSERT INTO `collect` VALUES (174, 44, 0, 139, NULL, '2024-05-20 22:43:41');
INSERT INTO `collect` VALUES (175, 50, 0, 116, NULL, '2024-05-20 22:44:43');
INSERT INTO `collect` VALUES (176, 50, 0, 27, NULL, '2024-05-20 22:44:44');
INSERT INTO `collect` VALUES (177, 50, 0, 161, NULL, '2024-05-20 22:44:45');
INSERT INTO `collect` VALUES (178, 50, 0, 158, NULL, '2024-05-20 22:44:51');
INSERT INTO `collect` VALUES (179, 50, 0, 159, NULL, '2024-05-20 22:44:53');
INSERT INTO `collect` VALUES (180, 51, 0, 27, NULL, '2024-05-20 22:45:47');
INSERT INTO `collect` VALUES (181, 51, 0, 166, NULL, '2024-05-20 22:45:48');
INSERT INTO `collect` VALUES (182, 51, 0, 158, NULL, '2024-05-20 22:45:51');
INSERT INTO `collect` VALUES (183, 51, 0, 169, NULL, '2024-05-20 22:45:56');

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int(11) NULL DEFAULT NULL COMMENT '用户id',
  `type` tinyint(1) NULL DEFAULT NULL COMMENT '评论类型（0歌曲1歌单）',
  `song_id` int(11) NULL DEFAULT NULL COMMENT '歌曲id',
  `song_list_id` int(11) NULL DEFAULT NULL COMMENT '歌单id',
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '评论内容',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '收藏时间',
  `up` int(11) NULL DEFAULT 0 COMMENT '评论点赞数',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 66 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '评论' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of comment
-- ----------------------------
INSERT INTO `comment` VALUES (63, 33, 1, NULL, 87, '很好听的歌曲', '2023-11-26 10:36:51', 0);
INSERT INTO `comment` VALUES (64, 33, 1, NULL, 91, '11', '2023-11-26 15:32:36', 0);
INSERT INTO `comment` VALUES (65, 35, 1, NULL, 86, '非常不错', '2023-11-28 21:34:41', 0);

-- ----------------------------
-- Table structure for consumer
-- ----------------------------
DROP TABLE IF EXISTS `consumer`;
CREATE TABLE `consumer`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '账号',
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
  `sex` tinyint(1) NULL DEFAULT NULL COMMENT '性别（1男0女）',
  `phone_num` char(15) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '电话',
  `email` char(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `birth` datetime(0) NULL DEFAULT NULL COMMENT '生日',
  `introduction` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '签名',
  `location` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '地区',
  `avator` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '头像',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 52 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '前端用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of consumer
-- ----------------------------
INSERT INTO `consumer` VALUES (35, 'user', '123456', 1, '18677772222', '1@qq.com', '2023-11-26 00:00:00', '一个厉害的歌手', '背景', '/userImages/1715071055423mmexport1712058796304.jpg', '2023-11-26 17:43:42', '2024-05-07 16:37:35');
INSERT INTO `consumer` VALUES (36, 'user2', '123456', 1, '18766665555', '2@qq.com', '2023-11-26 00:00:00', '一个非常优秀的歌手', '杭州', '/userImages/1715071061804Screenshot_2024-01-14-17-38-13-545_com.viva.time_.jpg', '2023-11-27 22:21:23', '2024-05-07 16:37:41');
INSERT INTO `consumer` VALUES (39, 'abc', '123456', 1, '151618128', '', '2001-11-28 00:00:00', '', '', '/userImages/1715070549283IMG_20200911_123802.jpg', '2024-05-07 16:25:09', '2024-05-07 16:29:09');
INSERT INTO `consumer` VALUES (40, '快乐凤男', '123456', 1, '', '', '2024-05-20 00:00:00', '', '', '/img/user.jpg', '2024-05-20 21:58:11', '2024-05-20 21:58:11');
INSERT INTO `consumer` VALUES (41, '996', '123456', 1, '', '', '2024-05-06 00:00:00', '', '', '/img/user.jpg', '2024-05-20 21:58:44', '2024-05-20 21:58:44');
INSERT INTO `consumer` VALUES (42, '上早八', '123456', 1, '', '', '2024-05-14 00:00:00', '', '', '/img/user.jpg', '2024-05-20 21:59:16', '2024-05-20 21:59:16');
INSERT INTO `consumer` VALUES (43, '军训吃西瓜', '123456', 0, '', '', '2024-05-01 00:00:00', '', '', '/img/user.jpg', '2024-05-20 21:59:47', '2024-05-20 21:59:47');
INSERT INTO `consumer` VALUES (44, '青稞第一超霸', '123456', 1, '', '', '2024-05-02 00:00:00', '', '', '/img/user.jpg', '2024-05-20 22:00:59', '2024-05-20 22:00:59');
INSERT INTO `consumer` VALUES (45, '这波血亏', '123456', 0, '', '', '2024-05-01 00:00:00', '', '', '/img/user.jpg', '2024-05-20 22:01:21', '2024-05-20 22:01:21');
INSERT INTO `consumer` VALUES (46, '私信已关', '123456', 0, '', '', '2024-05-02 00:00:00', '', '', '/img/user.jpg', '2024-05-20 22:02:01', '2024-05-20 22:02:01');
INSERT INTO `consumer` VALUES (47, '华强买瓜', '123456', 1, '', '', '2024-05-03 00:00:00', '', '', '/img/user.jpg', '2024-05-20 22:02:23', '2024-05-20 22:02:23');
INSERT INTO `consumer` VALUES (48, '爱喝奶茶', '123456', 0, '', '', '2024-05-06 00:00:00', '', '', '/img/user.jpg', '2024-05-20 22:02:45', '2024-05-20 22:02:45');
INSERT INTO `consumer` VALUES (49, '顺利毕业版', '123456', 0, '', '', '2024-05-07 00:00:00', '', '', '/img/user.jpg', '2024-05-20 22:03:20', '2024-05-20 22:03:20');
INSERT INTO `consumer` VALUES (50, '喜欢恶习差评', '123456', 0, '', '', '2024-05-10 00:00:00', '', '', '/img/user.jpg', '2024-05-20 22:44:29', '2024-05-20 22:44:29');
INSERT INTO `consumer` VALUES (51, '总是给好评', '123456', 1, '', '', '2011-05-03 00:00:00', '', '', '/img/user.jpg', '2024-05-20 22:45:29', '2024-05-20 22:45:29');

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS `flyway_schema_history`;
CREATE TABLE `flyway_schema_history`  (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `checksum` int(11) NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `installed_on` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `flyway_schema_history_s_idx`(`success`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for list_song
-- ----------------------------
DROP TABLE IF EXISTS `list_song`;
CREATE TABLE `list_song`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `song_id` int(11) NULL DEFAULT NULL COMMENT '歌曲id',
  `song_list_id` int(11) NULL DEFAULT NULL COMMENT '歌单id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 231 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '歌单包含歌曲列表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of list_song
-- ----------------------------
INSERT INTO `list_song` VALUES (213, 27, 86);
INSERT INTO `list_song` VALUES (214, 116, 86);
INSERT INTO `list_song` VALUES (216, 166, 86);
INSERT INTO `list_song` VALUES (217, 161, 86);
INSERT INTO `list_song` VALUES (218, 167, 86);
INSERT INTO `list_song` VALUES (219, 171, 89);
INSERT INTO `list_song` VALUES (220, 127, 89);
INSERT INTO `list_song` VALUES (221, 156, 89);
INSERT INTO `list_song` VALUES (222, 139, 89);
INSERT INTO `list_song` VALUES (223, 158, 87);
INSERT INTO `list_song` VALUES (224, 159, 87);
INSERT INTO `list_song` VALUES (225, 160, 87);
INSERT INTO `list_song` VALUES (226, 139, 87);
INSERT INTO `list_song` VALUES (227, 118, 87);
INSERT INTO `list_song` VALUES (228, 138, 87);
INSERT INTO `list_song` VALUES (229, 137, 87);
INSERT INTO `list_song` VALUES (230, 169, 88);

-- ----------------------------
-- Table structure for ranks
-- ----------------------------
DROP TABLE IF EXISTS `ranks`;
CREATE TABLE `ranks`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `song_list_id` int(11) NOT NULL COMMENT '歌单id',
  `consumer_id` int(11) NOT NULL COMMENT '用户id',
  `score` int(11) NULL DEFAULT NULL COMMENT '评分',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `consumer_id`(`song_list_id`, `consumer_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 113 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '评价' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ranks
-- ----------------------------
INSERT INTO `ranks` VALUES (38, 86, 33, 5);
INSERT INTO `ranks` VALUES (39, 86, 35, 7);
INSERT INTO `ranks` VALUES (40, 87, 36, 10);
INSERT INTO `ranks` VALUES (41, 89, 36, 9);
INSERT INTO `ranks` VALUES (42, 90, 33, 5);
INSERT INTO `ranks` VALUES (43, 89, 35, 7);
INSERT INTO `ranks` VALUES (44, 89, 33, 4);
INSERT INTO `ranks` VALUES (45, 87, 33, 10);
INSERT INTO `ranks` VALUES (46, 90, 36, 10);
INSERT INTO `ranks` VALUES (47, 90, 39, 10);
INSERT INTO `ranks` VALUES (48, 86, 39, 7);
INSERT INTO `ranks` VALUES (49, 87, 39, 4);
INSERT INTO `ranks` VALUES (50, 88, 39, 10);
INSERT INTO `ranks` VALUES (51, 86, 49, 8);
INSERT INTO `ranks` VALUES (52, 87, 49, 10);
INSERT INTO `ranks` VALUES (53, 88, 49, 4);
INSERT INTO `ranks` VALUES (54, 89, 49, 7);
INSERT INTO `ranks` VALUES (55, 90, 49, 10);
INSERT INTO `ranks` VALUES (59, 87, 35, 7);
INSERT INTO `ranks` VALUES (62, 88, 35, 6);
INSERT INTO `ranks` VALUES (63, 90, 35, 5);
INSERT INTO `ranks` VALUES (64, 86, 47, 2);
INSERT INTO `ranks` VALUES (69, 87, 47, 2);
INSERT INTO `ranks` VALUES (70, 89, 47, 6);
INSERT INTO `ranks` VALUES (71, 90, 47, 6);
INSERT INTO `ranks` VALUES (72, 86, 42, 7);
INSERT INTO `ranks` VALUES (73, 87, 42, 3);
INSERT INTO `ranks` VALUES (74, 88, 42, 6);
INSERT INTO `ranks` VALUES (75, 89, 42, 7);
INSERT INTO `ranks` VALUES (76, 90, 42, 5);
INSERT INTO `ranks` VALUES (77, 86, 40, 8);
INSERT INTO `ranks` VALUES (78, 87, 40, 7);
INSERT INTO `ranks` VALUES (79, 88, 40, 8);
INSERT INTO `ranks` VALUES (80, 89, 40, 7);
INSERT INTO `ranks` VALUES (81, 90, 40, 6);
INSERT INTO `ranks` VALUES (82, 86, 48, 7);
INSERT INTO `ranks` VALUES (83, 87, 48, 8);
INSERT INTO `ranks` VALUES (84, 88, 48, 7);
INSERT INTO `ranks` VALUES (85, 90, 48, 8);
INSERT INTO `ranks` VALUES (86, 86, 46, 5);
INSERT INTO `ranks` VALUES (87, 89, 46, 8);
INSERT INTO `ranks` VALUES (88, 90, 46, 8);
INSERT INTO `ranks` VALUES (89, 86, 45, 7);
INSERT INTO `ranks` VALUES (90, 87, 45, 8);
INSERT INTO `ranks` VALUES (91, 88, 45, 8);
INSERT INTO `ranks` VALUES (92, 89, 45, 6);
INSERT INTO `ranks` VALUES (93, 90, 45, 5);
INSERT INTO `ranks` VALUES (94, 86, 44, 6);
INSERT INTO `ranks` VALUES (95, 87, 44, 7);
INSERT INTO `ranks` VALUES (96, 88, 44, 6);
INSERT INTO `ranks` VALUES (97, 89, 44, 10);
INSERT INTO `ranks` VALUES (98, 90, 44, 5);
INSERT INTO `ranks` VALUES (99, 86, 50, 10);
INSERT INTO `ranks` VALUES (100, 87, 50, 2);
INSERT INTO `ranks` VALUES (101, 88, 50, 2);
INSERT INTO `ranks` VALUES (102, 89, 50, 2);
INSERT INTO `ranks` VALUES (103, 90, 50, 1);
INSERT INTO `ranks` VALUES (104, 86, 51, 1);
INSERT INTO `ranks` VALUES (105, 87, 51, 9);
INSERT INTO `ranks` VALUES (106, 88, 51, 10);
INSERT INTO `ranks` VALUES (107, 89, 51, 10);
INSERT INTO `ranks` VALUES (108, 90, 51, 9);

-- ----------------------------
-- Table structure for singer
-- ----------------------------
DROP TABLE IF EXISTS `singer`;
CREATE TABLE `singer`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '姓名',
  `sex` tinyint(1) NULL DEFAULT NULL COMMENT '性别（0女1男2组合3不明）',
  `pic` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '头像',
  `birth` datetime(0) NULL DEFAULT NULL COMMENT '生日',
  `location` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '地区',
  `introduction` varchar(1600) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '简介',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 60 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '歌手' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of singer
-- ----------------------------
INSERT INTO `singer` VALUES (5, 'G.E.M.邓紫棋', 0, '/img/singerPic/1715138742584QQ图片20240508111120.jpg', '1998-07-27 00:00:00', '中国上海', '成长于一个音乐世家，其母亲为上海音乐学院声乐系毕业生，外婆教唱歌，舅父拉小提琴，外公在乐团吹色士风。在家人的熏陶下，自小便热爱音乐，喜爱唱歌，与音乐结下不解之缘。G.E.M.在5岁的时候已经开始尝试作曲及填词，13岁完成了8级钢琴。G.E.M.在小学时期就读位于太子道西的中华基督教会协和小学上午校，为2003年的毕业生，同时亦为校内诗歌班成员。其英文名G.E.M.是Get Everybody Moving的缩写，象徵著她希望透过音乐让大家动起来的梦想。');
INSERT INTO `singer` VALUES (47, '五月天', 2, '/img/singerPic/1715138842245QQ图片20240508112710.jpg', '2024-05-05 00:00:00', '台湾', '无');
INSERT INTO `singer` VALUES (48, '周杰伦', 1, '/img/singerPic/1715138750568QQ图片20240508111209.jpg', '2024-04-28 00:00:00', '中国-台湾', '无');
INSERT INTO `singer` VALUES (49, '陈奕迅', 1, '/img/singerPic/1715138760086QQ图片20240508111240.jpg', '2024-05-06 00:00:00', '', '');
INSERT INTO `singer` VALUES (50, '张碧晨', 0, '/img/singerPic/1715138856954QQ图片20240508112728.jpg', '2024-05-08 00:00:00', '中国', '');
INSERT INTO `singer` VALUES (51, '汪苏泷', 1, '/img/singerPic/hhh.jpg', '2024-04-29 00:00:00', '', '');
INSERT INTO `singer` VALUES (52, '李荣浩', 1, '/img/singerPic/hhh.jpg', '2024-04-30 00:00:00', '', '');
INSERT INTO `singer` VALUES (53, '林俊杰', 1, '/img/singerPic/hhh.jpg', '2024-05-07 00:00:00', '', '');
INSERT INTO `singer` VALUES (54, '苏打绿', 2, '/img/singerPic/hhh.jpg', '2024-05-06 00:00:00', '', '');
INSERT INTO `singer` VALUES (55, '王菲', 0, '/img/singerPic/hhh.jpg', '2024-05-07 00:00:00', '', '');
INSERT INTO `singer` VALUES (56, 'AGA', 0, '/img/singerPic/hhh.jpg', '2024-05-07 00:00:00', '', '');
INSERT INTO `singer` VALUES (57, 'Akon', 1, '/img/singerPic/hhh.jpg', '2024-04-30 00:00:00', '美国', '');
INSERT INTO `singer` VALUES (58, 'Olly Murs', 1, '/img/singerPic/hhh.jpg', '2024-05-14 00:00:00', '', '');
INSERT INTO `singer` VALUES (59, 'Taylor Swift', 0, '/img/singerPic/hhh.jpg', '2024-05-22 00:00:00', '', '');

-- ----------------------------
-- Table structure for song
-- ----------------------------
DROP TABLE IF EXISTS `song`;
CREATE TABLE `song`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `singer_id` int(11) NULL DEFAULT NULL COMMENT '歌手id',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '革命',
  `introduction` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '简介',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `pic` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '歌曲图片',
  `lyric` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '歌词',
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '歌曲地址',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 172 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '歌曲' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of song
-- ----------------------------
INSERT INTO `song` VALUES (27, 5, 'G.E.M.邓紫棋-泡沫', 'Xposed', '2022-04-15 12:37:11', '2024-05-08 11:24:31', '/img/songPic/1715138671859QQ图片20240508112405.png', '[00:00.00] 作曲 : G.E.M.\n[00:00.169] 作词 : G.E.M.\n[00:00.509]阳光下的泡沫 是彩色的 就像被骗的我 是幸福的\n[00:14.940]追究什么对错 你的谎言 基于你还爱我\n[00:27.819]美丽的泡沫 虽然一刹花火 你所有承诺 虽然都太脆弱\n[00:41.780]但爱像泡沫 如果能够看破 有什么难过\n[00:53.750]\n[00:57.230]早该知道泡沫 一触就破 就像已伤的心 不胜折磨\n[01:11.040]也不是谁的错 谎言再多 基于你还爱我\n[01:24.200]美丽的泡沫 虽然一刹花火 你所有承诺 虽然都太脆弱\n[01:38.800]爱本是泡沫 如果能够看破 有什么难过\n[01:50.560]\n[01:52.629]再美的花朵 盛开过就凋落 再亮眼的星 一闪过就坠落\n[02:06.650]爱本是泡沫 如果能够看破 有什么难过\n[02:20.889]为什么难过 有什么难过 为什么难过\n[02:41.650]\n[02:45.799]全都是泡沫 只一刹的花火 你所有承诺 全部都太脆弱\n[02:59.560]而你的轮廓 怪我没有看破 才如此难过\n[03:13.769]相爱的把握 要如何再搜索 相拥着寂寞 难道就不寂寞\n[03:28.820]爱本是泡沫 怪我没有看破 才如此难过\n[03:39.980]\n[03:43.829]在雨下的泡沫 一触就破 当初炽热的心 早已沉没\n[03:57.940]说什么你爱我 如果骗我 我宁愿你沉默\n[04:10.000]\n[04:10.209]\n[59:59.700]', '/song/16877835416171679030969249泡沫.mp3');
INSERT INTO `song` VALUES (116, 5, 'G.E.M.邓紫棋-光年之外', '光年之外', '2023-06-28 00:12:11', '2024-05-08 11:22:11', '/img/songPic/1715138531733QQ图片20240508112114.png', '[00:01.82]光年之外 - G.E.M.邓紫棋\r\n[00:03.86]词：G.E.M. 邓紫棋\r\n[00:05.93]曲：G.E.M. 邓紫棋\r\n[00:08.01]编曲：Lupo Groinig\r\n[00:10.6]感受停在我发端的指尖\r\n[00:14.7]如何瞬间冻结时间\r\n[00:21.47]记住望着我坚定的双眼\r\n[00:25.54]也许已经没有明天\r\n[00:32.09]面对浩瀚的星海\r\n[00:34.68]我们微小得像尘埃\r\n[00:37.05]漂浮在一片无奈\r\n[00:43.03]缘分让我们相遇乱世以外\r\n[00:48.36]命运却要我们危难中相爱\r\n[00:53.83]也许未来遥远在光年之外\r\n[00:59.21]我愿守候未知里为你等待\r\n[01:03.29]我没想到为了你我能疯狂到\r\n[01:08.71]山崩海啸没有你根本不想逃\r\n[01:14.25]我的大脑为了你已经疯狂到\r\n[01:19.65]脉搏心跳没有你根本不重要\r\n[01:26.92]一双围在我胸口的臂弯\r\n[01:31.0]足够抵挡天旋地转\r\n[01:37.729996]一种执迷不放手的倔强\r\n[01:41.770004]足以点燃所有希望\r\n[01:48.46]宇宙磅礡而冷漠\r\n[01:50.979996]我们的爱微小却闪烁\r\n[01:53.45]颠簸却如此忘我\r\n[01:59.28]缘分让我们相遇乱世以外\r\n[02:04.75]命运却要我们危难中相爱\r\n[02:10.2]也许未来遥远在光年之外\r\n[02:15.71]我愿守候未知里为你等待\r\n[02:19.61]我没想到为了你我能疯狂到\r\n[02:25.12]山崩海啸没有你根本不想逃\r\n[02:30.58]我的大脑为了你已经疯狂到\r\n[02:36.11]脉搏心跳没有你根本不重要\r\n[02:42.41]也许航道以外是醒不来的梦\r\n[02:54.5]乱世以外是纯粹的相拥\r\n[03:03.36]我没想到为了你我能疯狂到\r\n[03:08.78]山崩海啸没有你根本不想逃\r\n[03:14.23]我的大脑为了你已经疯狂到\r\n[03:19.75]脉搏心跳没有你根本不重要\r\n[03:25.58]相遇乱世以外危难中相爱\r\n[03:36.52]相遇乱世以外危难中相爱\r\n[03:47.02]我没想到', '/song/1701179468969光年之外.mp3');
INSERT INTO `song` VALUES (118, 47, '五月天-步步', '自选作品辑 the Best of 1999-2013', '2024-05-07 22:13:30', '2024-05-08 11:25:22', '/img/songPic/1715138722589QQ图片20240508112454.png', '[00:00:00]暂无歌词', '/song/1715138627186C400002drhE30SW04m.mp3');
INSERT INTO `song` VALUES (119, 5, 'G.E.M.邓紫棋-多远都要在一起', '', '2024-05-07 22:13:51', '2024-05-08 11:22:19', '/img/songPic/1715138539681QQ图片20240508112108.png', '多远都要在一起 - G.E.M. 邓紫棋\n词：G.E.M. 邓紫棋\n曲：G.E.M. 邓紫棋\n编曲：Lupo Groinig\nOP：蜂鸟音乐\nSP：百纳娱乐\n想听你听过的音乐\n想看你看过的小说\n我想收集每一刻\n我想看到你眼里的世界\n想到你到过的地方\n和你曾渡过的时光\n不想错过每一刻\n多希望我一直在你身旁\n未来何从何去\n你快乐我也就没关系\n对你我最熟悉\n你爱自由我却更爱你\n我能习惯远距离\n爱总是身不由己\n宁愿换个方式至少还能遥远爱着你\n爱能克服远距离\n多远都要在一起\n你已经不再存在我世界里\n请不要离开我的回忆\n想你说爱我的语气\n想你望着我的眼睛\n不想忘记每一刻\n用思念让我们一直前进\n想像你失落的唇印\n想像你失约的旅行\n想像你离开的一刻\n如果我有留下你的勇气\n我能习惯远距离\n爱总是身不由己\n宁愿换个方式至少还能遥远爱着你\n爱能克服远距离\n多远都要在一起\n我已经不再存在你的心里\n就让我独自守着回忆\n如果阳光永远都炽热\n如果彩虹不会掉颜色\n你能不能不离开呢\n我能习惯远距离\n爱总是身不由己\n宁愿换个方式至少还能遥远爱着你\n爱能克服远距离\n多远都要在一起\n你已经不再存在我世界里\n请不要离开我的回忆\n请不要离开不要离开我的回忆', '/song/1715091307306C400000n57Y03eNvvF.mp3');
INSERT INTO `song` VALUES (120, 48, '周杰伦-晴天', '叶惠美', '2024-05-07 22:25:21', '2024-05-08 11:22:45', '/img/songPic/1715138565339QQ图片20240508112121.png', '[00:00:00]暂无歌词', '/song/1715091932102C400002202B43Cq4V4.mp3');
INSERT INTO `song` VALUES (121, 49, '陈奕迅-最佳损友', '', '2024-05-07 22:33:10', '2024-05-16 21:49:04', NULL, '[00:00:00]暂无歌词', '/song/1715092401917C400003C9fBv2K4x8b.mp3');
INSERT INTO `song` VALUES (122, 50, '张碧晨-字字句句', '', '2024-05-07 22:46:16', '2024-05-08 11:45:54', '/img/songPic/1715138929685QQ图片20240508112836.png', '[00:00:00]暂无歌词', '/song/1715093194078C400001rw4YQ1phDiB.mp3');
INSERT INTO `song` VALUES (124, 5, 'G.E.M.邓紫棋-null', 'null', '2024-05-15 14:37:40', '2024-05-15 14:39:47', NULL, '[00:00:00]暂无歌词', '/song/1715755187230C400001rw4YQ1phDiB.mp3');
INSERT INTO `song` VALUES (125, 48, '周杰伦-一路向北', 'null', '2024-05-16 21:41:04', '2024-05-16 21:41:15', NULL, '[00:00:00]暂无歌词', '/song/1715866864636周杰伦 一路向北.mp3');
INSERT INTO `song` VALUES (126, 48, '周杰伦-青花瓷', 'null', '2024-05-16 21:42:54', '2024-05-16 21:43:03', NULL, '[00:00:00]暂无歌词', '/song/1715866974802周杰伦 青花瓷.mp3');
INSERT INTO `song` VALUES (127, 48, '周杰伦-反方向的钟', 'null', '2024-05-16 21:43:39', '2024-05-16 21:43:53', NULL, '[00:00:00]暂无歌词', '/song/1715867019094周杰伦 反方向的钟.mp3');
INSERT INTO `song` VALUES (128, 48, '周杰伦-七里香', 'null', '2024-05-16 21:44:34', '2024-05-16 21:44:43', NULL, '[00:00:00]暂无歌词', '/song/1715867074758周杰伦 七里香.mp3');
INSERT INTO `song` VALUES (129, 48, '周杰伦-花海', 'null', '2024-05-16 21:45:33', '2024-05-16 21:45:44', NULL, '[00:00:00]暂无歌词', '/song/1715867133284周杰伦 花海.mp3');
INSERT INTO `song` VALUES (130, 48, '周杰伦-兰亭序', 'null', '2024-05-16 21:46:19', '2024-05-16 21:46:31', NULL, '[00:00:00]暂无歌词', '/song/1715867179530周杰伦 兰亭序.mp3');
INSERT INTO `song` VALUES (131, 49, '陈奕迅-等你爱我', 'null', '2024-05-16 21:47:41', '2024-05-16 21:47:50', NULL, '[00:00:00]暂无歌词', '/song/1715867261019陈奕迅 等你爱我.mp3');
INSERT INTO `song` VALUES (132, 49, '陈奕迅-单车', 'null', '2024-05-16 21:50:29', '2024-05-16 21:50:39', NULL, '[00:00:00]暂无歌词', '/song/1715867429487陈奕迅 单车.mp3');
INSERT INTO `song` VALUES (133, 49, '陈奕迅-因为爱情', 'null', '2024-05-16 21:51:29', '2024-05-16 21:51:41', NULL, '[00:00:00]暂无歌词', '/song/1715867489572王菲 陈奕迅 因为爱情.mp3');
INSERT INTO `song` VALUES (134, 49, '陈奕迅-爱情转移', 'null', '2024-05-16 21:52:00', '2024-05-16 21:52:07', NULL, '[00:00:00]暂无歌词', '/song/1715867520104陈奕迅 爱情转移.mp3');
INSERT INTO `song` VALUES (135, 49, '陈奕迅-富士山下', 'null', '2024-05-16 21:52:21', '2024-05-16 21:52:45', NULL, '[00:00:00]暂无歌词', '/song/1715867541325陈奕迅 富士山下.mp3');
INSERT INTO `song` VALUES (136, 49, '陈奕迅-十年', 'null', '2024-05-16 21:53:05', '2024-05-16 21:53:16', NULL, '[00:00:00]暂无歌词', '/song/1715867585897陈奕迅 十年.mp3');
INSERT INTO `song` VALUES (137, 47, '五月天-后来的我们', 'null', '2024-05-16 21:53:45', '2024-05-16 21:53:56', NULL, '[00:00:00]暂无歌词', '/song/1715867625338五月天 后来的我们.mp3');
INSERT INTO `song` VALUES (138, 47, '五月天-你不是真正的快乐', 'null', '2024-05-16 21:54:07', '2024-05-16 21:54:31', NULL, '[00:00:00]暂无歌词', '/song/1715867647287五月天 你不是真正的快乐.mp3');
INSERT INTO `song` VALUES (139, 47, '五月天-突然好想你', 'null', '2024-05-16 21:54:58', '2024-05-16 21:55:08', NULL, '[00:00:00]暂无歌词', '/song/1715867698531五月天 突然好想你.mp3');
INSERT INTO `song` VALUES (140, 50, '张碧晨-明天会更好', 'null', '2024-05-16 21:55:36', '2024-05-16 21:55:47', NULL, '[00:00:00]暂无歌词', '/song/1715867736903张碧晨 明天会更好.mp3');
INSERT INTO `song` VALUES (141, 50, '张碧晨-年轮', 'null', '2024-05-16 21:56:09', '2024-05-16 21:56:19', NULL, '[00:00:00]暂无歌词', '/song/1715867769738张碧晨 年轮.mp3');
INSERT INTO `song` VALUES (142, 51, '汪苏泷-不分手的恋爱', 'null', '2024-05-16 21:57:41', '2024-05-16 21:57:51', NULL, '[00:00:00]暂无歌词', '/song/1715867861209汪苏泷 不分手的恋爱.mp3');
INSERT INTO `song` VALUES (143, 51, '汪苏泷-小星星', 'null', '2024-05-16 21:58:04', '2024-05-16 21:58:15', NULL, '[00:00:00]暂无歌词', '/song/1715867884849汪苏泷 小星星.mp3');
INSERT INTO `song` VALUES (144, 51, '汪苏泷-一笑倾城', 'null', '2024-05-16 21:58:27', '2024-05-16 21:58:36', NULL, '[00:00:00]暂无歌词', '/song/1715867907890汪苏泷 一笑倾城.mp3');
INSERT INTO `song` VALUES (145, 52, '李荣浩-不将就', 'null', '2024-05-16 21:59:38', '2024-05-16 21:59:45', NULL, '[00:00:00]暂无歌词', '/song/1715867978266李荣浩 不将就.mp3');
INSERT INTO `song` VALUES (146, 52, '李荣浩-大城小爱', 'null', '2024-05-16 22:00:33', '2024-05-16 22:00:43', NULL, '[00:00:00]暂无歌词', '/song/1715868033592李荣浩 大城小爱.mp3');
INSERT INTO `song` VALUES (147, 52, '李荣浩-李白', 'null', '2024-05-16 22:00:53', '2024-05-16 22:01:01', NULL, '[00:00:00]暂无歌词', '/song/1715868053796李荣浩 李白.mp3');
INSERT INTO `song` VALUES (148, 52, '李荣浩-模特', 'null', '2024-05-16 22:01:09', '2024-05-16 22:01:20', NULL, '[00:00:00]暂无歌词', '/song/1715868069123李荣浩 模特.mp3');
INSERT INTO `song` VALUES (149, 52, '李荣浩-年少有为', 'null', '2024-05-16 22:01:29', '2024-05-16 22:01:44', NULL, '[00:00:00]暂无歌词', '/song/1715868089392李荣浩 年少有为.mp3');
INSERT INTO `song` VALUES (150, 52, '李荣浩-女孩', 'null', '2024-05-16 22:01:54', '2024-05-16 22:02:03', NULL, '[00:00:00]暂无歌词', '/song/1715868114264李荣浩 女孩.mp3');
INSERT INTO `song` VALUES (151, 53, '林俊杰-曹操', 'null', '2024-05-16 22:02:36', '2024-05-16 22:02:47', NULL, '[00:00:00]暂无歌词', '/song/1715868156226林俊杰 曹操.mp3');
INSERT INTO `song` VALUES (152, 53, '林俊杰-爱笑的眼睛', 'null', '2024-05-16 22:02:59', '2024-05-16 22:03:12', NULL, '[00:00:00]暂无歌词', '/song/1715868179496林俊杰 爱笑的眼睛.mp3');
INSERT INTO `song` VALUES (153, 53, '林俊杰-背对背拥抱', 'null', '2024-05-16 22:03:25', '2024-05-16 22:03:34', NULL, '[00:00:00]暂无歌词', '/song/1715868205989林俊杰 背对背拥抱 .mp3');
INSERT INTO `song` VALUES (154, 53, '林俊杰-江南', 'null', '2024-05-16 22:03:46', '2024-05-16 22:03:53', NULL, '[00:00:00]暂无歌词', '/song/1715868226121林俊杰 江南.mp3');
INSERT INTO `song` VALUES (155, 53, '林俊杰-可惜没如果', 'null', '2024-05-16 22:04:04', '2024-05-16 22:04:17', NULL, '[00:00:00]暂无歌词', '/song/1715868244499林俊杰 可惜没如果.mp3');
INSERT INTO `song` VALUES (156, 53, '林俊杰-那些你很冒险的梦', 'null', '2024-05-16 22:04:29', '2024-05-16 22:05:07', NULL, '[00:00:00]暂无歌词', '/song/1715868269048林俊杰 那些你很冒险的梦.mp3');
INSERT INTO `song` VALUES (157, 53, '林俊杰-修炼爱情', 'null', '2024-05-16 22:04:59', '2024-05-16 22:05:17', NULL, '[00:00:00]暂无歌词', '/song/1715868299900林俊杰 修炼爱情.mp3');
INSERT INTO `song` VALUES (158, 54, '苏打绿-我好想你', 'null', '2024-05-16 22:05:49', '2024-05-16 22:05:59', NULL, '[00:00:00]暂无歌词', '/song/1715868349327苏打绿 我好想你.mp3');
INSERT INTO `song` VALUES (159, 54, '苏打绿-无与伦比的美丽', 'null', '2024-05-16 22:06:08', '2024-05-16 22:06:18', NULL, '[00:00:00]暂无歌词', '/song/1715868367999苏打绿 无与伦比的美丽.mp3');
INSERT INTO `song` VALUES (160, 54, '苏打绿-小情歌', 'null', '2024-05-16 22:06:31', '2024-05-16 22:06:40', NULL, '[00:00:00]暂无歌词', '/song/1715868391024苏打绿 小情歌.mp3');
INSERT INTO `song` VALUES (161, 55, '王菲-百年孤寂', 'null', '2024-05-16 22:07:10', '2024-05-16 22:07:19', NULL, '[00:00:00]暂无歌词', '/song/1715868430173王菲 百年孤寂.mp3');
INSERT INTO `song` VALUES (162, 55, '王菲-因为爱情', 'null', '2024-05-16 22:07:28', '2024-05-16 22:07:36', NULL, '[00:00:00]暂无歌词', '/song/1715868448695王菲 陈奕迅 因为爱情.mp3');
INSERT INTO `song` VALUES (163, 55, '王菲-匆匆那年', 'null', '2024-05-16 22:07:57', '2024-05-16 22:08:06', NULL, '[00:00:00]暂无歌词', '/song/1715868477517王菲 匆匆那年.mp3');
INSERT INTO `song` VALUES (164, 55, '王菲-红豆', 'null', '2024-05-16 22:08:16', '2024-05-16 22:08:25', NULL, '[00:00:00]暂无歌词', '/song/1715868496780王菲 红豆.mp3');
INSERT INTO `song` VALUES (165, 55, '王菲-开到茶麓', 'null', '2024-05-16 22:08:37', '2024-05-16 22:09:02', NULL, '[00:00:00]暂无歌词', '/song/1715868517646王菲 开到茶蘼.mp3');
INSERT INTO `song` VALUES (166, 55, '王菲-人间', 'null', '2024-05-16 22:09:17', '2024-05-16 22:09:34', NULL, '[00:00:00]暂无歌词', '/song/1715868557472王菲 人间.mp3');
INSERT INTO `song` VALUES (167, 55, '王菲-致青春', 'null', '2024-05-16 22:09:42', '2024-05-16 22:09:54', NULL, '[00:00:00]暂无歌词', '/song/1715868582872王菲 致青春.mp3');
INSERT INTO `song` VALUES (168, 56, 'AGA-Wonderful U', 'null', '2024-05-16 22:11:19', '2024-05-16 22:11:42', NULL, '[00:00:00]暂无歌词', '/song/1715868679211AGA Wonderful U.mp3');
INSERT INTO `song` VALUES (169, 57, 'Akon-Right Now', 'null', '2024-05-16 22:12:36', '2024-05-16 22:28:32', NULL, 'eee', '/song/1715868756417Akon Right Now.mp3');
INSERT INTO `song` VALUES (170, 58, 'Olly Murs-That Girl', 'null', '2024-05-16 22:13:51', '2024-05-16 22:14:00', NULL, '[00:00:00]暂无歌词', '/song/1715868831567Olly Murs  That Girl.mp3');
INSERT INTO `song` VALUES (171, 59, 'Taylor Swift-Love Story', 'null', '2024-05-16 22:15:16', '2024-05-16 22:15:25', NULL, '[00:00:00]暂无歌词', '/song/1715868916006Taylor Swift  Love Story.mp3');

-- ----------------------------
-- Table structure for song_list
-- ----------------------------
DROP TABLE IF EXISTS `song_list`;
CREATE TABLE `song_list`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '标题',
  `pic` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '歌单图片',
  `introduction` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '简介',
  `style` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '风格',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 92 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '歌单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of song_list
-- ----------------------------
INSERT INTO `song_list` VALUES (86, '华语伤感女声', '/img/songListPic/1715138209676QQ图片20240508111419.png', '细心聆听那些触动你内心的华语伤感女声。', '华语');
INSERT INTO `song_list` VALUES (87, '华语组合金曲', '/img/songListPic/1715138217958QQ图片20240508111404.jpg', '香港金曲精选', '粤语');
INSERT INTO `song_list` VALUES (88, '欧美流行', '/img/songListPic/1715138229194QQ图片20240508111414.png', '', '欧美');
INSERT INTO `song_list` VALUES (89, '钢琴后摇 I 初雪点地，窸窣低语', '/img/songListPic/1715138233562QQ图片20240508111410.png', '', '乐器');
INSERT INTO `song_list` VALUES (90, '华语民谣，因为懂得才有共鸣', '/img/songListPic/1715138240194QQ图片20240508111450.png', '', '华语');

-- ----------------------------
-- Table structure for user_ratings
-- ----------------------------
DROP TABLE IF EXISTS `user_ratings`;
CREATE TABLE `user_ratings`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NULL DEFAULT NULL,
  `song_list_id` int(11) NULL DEFAULT NULL,
  `score` int(11) NULL DEFAULT NULL,
  `timestamp` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id`) USING BTREE,
  INDEX `song_list_id`(`song_list_id`) USING BTREE,
  CONSTRAINT `user_ratings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `consumer` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_ratings_ibfk_2` FOREIGN KEY (`song_list_id`) REFERENCES `song_list` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_ratings
-- ----------------------------
INSERT INTO `user_ratings` VALUES (1, 33, 86, 5, '2024-05-15 18:06:38');

SET FOREIGN_KEY_CHECKS = 1;
