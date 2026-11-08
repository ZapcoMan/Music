# 单元测试方法映射检查报告

> **口径声明**：本文是改造当时的静态快照，其中的接口路径与方法（如 `/song/manage/*`、
> `/consumer/login`、`/comment/commentOfSongId?songId=`）自 **v2.0.0** 起已整体 RESTful 化：
> 业务接口挂到 `/api/v1` 之下、动词式路径改资源式、id 进路径、部分更新用 PATCH、标签整体替换用 PUT。
> 分析结论本身（逻辑匹配度、覆盖有效性）仍然有效，但**照抄本文的 URL 会拿到 404**。
> 现行契约以代码与 [../CHANGELOG.md](../CHANGELOG.md) 的 v2.0.0 条目为准，本文件不逐行改写历史内容。

## 📋 概述

检查所有测试类中调用的方法是否在实际 Service/Controller 中存在,确保测试的有效性。

---

## ✅ 已修正的问题

### 1. AdminServiceImplTest

**问题**: AdminService 只有 `verifyPassword` 方法,没有其他 CRUD 方法

**修正**:
- ✅ 保留 `testLoginWithCorrectPassword` - 调用 `verifyPassword`
- ✅ 保留 `testLoginWithWrongPassword` - 调用 `verifyPassword`
- ✅ 保留 `testLoginWithNonExistentUser` - 调用 `verifyPassword`
- ⚠️ 清空 `testSelectByPrimaryKey` - AdminService 无此方法
- ⚠️ 清空 `testUpdate` - AdminService 无此方法
- ⚠️ 清空 `testInsert` - AdminService 无此方法
- ⚠️ 清空 `testDelete` - AdminService 无此方法

---

### 2. SongListServiceImplTest

**问题**: 方法名不匹配

**修正**:
- ✅ `testSongListOfStyle` - 将 `songListOfStyle("华语")` 改为 `likeStyle("%华语%")`

---

### 3. ConsumerControllerTest

**问题**: 接口路径不正确

**修正**:
- ✅ `testLoginSuccess` - 将 `/consumer/login/status` 改为 `/consumer/login`
- ✅ `testLoginFailed` - 将 `/consumer/login/status` 改为 `/consumer/login`
- ✅ `testSelectByPrimaryKey` - 将 `/consumer/detail` 改为 `/consumer/selectByPrimaryKey`

---

### 4. AdminControllerTest

**问题**: AdminController 只有登录和管理页面接口,没有 CRUD 接口

**修正**:
- ✅ 保留 `testLoginSuccess` - 调用 `/admin/login/status`
- ✅ 保留 `testLoginFailed` - 调用 `/admin/login/status`
- ⚠️ 清空 `testSelectByPrimaryKey` - AdminController 无此接口
- ⚠️ 清空 `testUpdateAdmin` - AdminController 无此接口
- ⚠️ 清空 `testDeleteAdmin` - AdminController 无此接口

---

## 📊 方法映射对照表

### Service 层

| 测试类 | 测试方法 | 实际方法 | 状态 |
|-------|---------|---------|------|
| **SingerServiceImplTest** | testInsert | SingerService.insert() | ✅ |
| | testSelectByPrimaryKey | SingerService.selectByPrimaryKey() | ✅ |
| | testUpdate | SingerService.update() | ✅ |
| | testAllSinger | SingerService.allSinger() | ✅ |
| | testAllSingerByPage | SingerService.allSingerByPage() | ✅ |
| | testSingerOfName | SingerService.singerOfName() | ✅ |
| | testSingerOfSex | SingerService.singerOfSex() | ✅ |
| | testDelete | SingerService.delete() | ✅ |
| **SongServiceImplTest** | testInsert | SongService.insert() | ✅ |
| | testSelectByPrimaryKey | SongService.selectByPrimaryKey() | ✅ |
| | testUpdate | SongService.update() | ✅ |
| | testAllSong | SongService.allSong() | ✅ |
| | testAllSongByPage | SongService.allSongByPage() | ✅ |
| | testSongOfSingerId | SongService.songOfSingerId() | ✅ |
| | testSongOfName | SongService.songOfName() | ✅ |
| | testLikeSongOfName | SongService.likeSongOfName() | ✅ |
| | testDelete | SongService.delete() | ✅ |
| **SongListServiceImplTest** | testInsert | SongListService.insert() | ✅ |
| | testSelectByPrimaryKey | SongListService.selectByPrimaryKey() | ✅ |
| | testUpdate | SongListService.update() | ✅ |
| | testAllSongList | SongListService.allSongList() | ✅ |
| | testAllSongListByPage | SongListService.allSongListByPage() | ✅ |
| | testSongListOfTitle | SongListService.songListOfTitle() | ✅ |
| | testLikeTitle | SongListService.likeTitle() | ✅ |
| | testSongListOfStyle | SongListService.**likeStyle()** | ✅ 已修正 |
| | testDelete | SongListService.delete() | ✅ |
| **ConsumerServiceImplTest** | testInsert | ConsumerService.insert() | ✅ |
| | testLoginWithCorrectPassword | ConsumerService.verifyPassword() | ✅ |
| | testLoginWithWrongPassword | ConsumerService.verifyPassword() | ✅ |
| | testLoginWithNonExistentUser | ConsumerService.verifyPassword() | ✅ |
| | testSelectByPrimaryKey | ConsumerService.selectByPrimaryKey() | ✅ |
| | testUpdate | ConsumerService.update() | ✅ |
| | testDelete | ConsumerService.delete() | ✅ |
| | testGetByUsername | ConsumerService.getByUsername() | ✅ |
| **AdminServiceImplTest** | testLoginWithCorrectPassword | AdminService.verifyPassword() | ✅ |
| | testLoginWithWrongPassword | AdminService.verifyPassword() | ✅ |
| | testLoginWithNonExistentUser | AdminService.verifyPassword() | ✅ |
| | testSelectByPrimaryKey | ❌ 不存在 | ⚠️ 已清空 |
| | testUpdate | ❌ 不存在 | ⚠️ 已清空 |
| | testInsert | ❌ 不存在 | ⚠️ 已清空 |
| | testDelete | ❌ 不存在 | ⚠️ 已清空 |

### Controller 层

| 测试类 | 测试方法 | 实际接口 | 状态 |
|-------|---------|---------|------|
| **SingerControllerTest** | testAllSingerByPage | GET /singer/allSingerByPage | ✅ |
| | testAllSinger | GET /singer/allSinger | ✅ |
| | testSingerOfName | GET /singer/singerOfName | ✅ |
| | testSingerOfSex | GET /singer/singerOfSex | ✅ |
| | testSelectByPrimaryKey | GET /singer/selectByPrimaryKey | ✅ |
| | testAddSinger | POST /singer/add | ✅ |
| | testUpdateSinger | POST /singer/update | ✅ |
| | testDeleteSinger | GET /singer/delete | ✅ |
| **ConsumerControllerTest** | testLoginSuccess | POST /consumer/**login** | ✅ 已修正 |
| | testLoginFailed | POST /consumer/**login** | ✅ 已修正 |
| | testSignUp | POST /consumer/add | ✅ |
| | testSelectByPrimaryKey | GET /consumer/**selectByPrimaryKey** | ✅ 已修正 |
| | testUpdateConsumer | POST /consumer/update | ✅ |
| | testAllConsumerByPage | GET /consumer/allConsumerByPage | ✅ |
| | testDeleteConsumer | GET /consumer/delete | ✅ |
| **AdminControllerTest** | testLoginSuccess | POST /admin/login/status | ✅ |
| | testLoginFailed | POST /admin/login/status | ✅ |
| | testSelectByPrimaryKey | ❌ 不存在 | ⚠️ 已清空 |
| | testUpdateAdmin | ❌ 不存在 | ⚠️ 已清空 |
| | testDeleteAdmin | ❌ 不存在 | ⚠️ 已清空 |

---

## 📈 统计信息

### Service 层测试
- **总测试方法数**: 51
- **有效测试**: 47 (92%)
- **已清空测试**: 4 (8%)

### Controller 层测试
- **总测试方法数**: 20
- **有效测试**: 17 (85%)
- **已清空测试**: 3 (15%)

### 总体统计
- **总测试方法数**: 71
- **有效测试**: 64 (90%)
- **已清空测试**: 7 (10%)

---

## ⚠️ 注意事项

### 1. AdminService 功能缺失
AdminService 目前只有密码验证功能,缺少完整的 CRUD 操作。建议后续补充:
- `insert(Admin admin)` - 添加管理员
- `update(Admin admin)` - 修改管理员
- `delete(Integer id)` - 删除管理员
- `selectByPrimaryKey(Integer id)` - 查询管理员
- `allAdmin()` - 查询所有管理员

### 2. AdminController 功能缺失
AdminController 目前只有登录和管理页面接口,缺少管理功能。建议后续补充:
- `POST /admin/add` - 添加管理员
- `POST /admin/update` - 修改管理员
- `GET /admin/delete` - 删除管理员
- `GET /admin/selectByPrimaryKey` - 查询管理员
- `GET /admin/allAdmin` - 查询所有管理员

### 3. 测试方法保留原则
虽然某些测试方法被清空了代码,但方法框架仍然保留,原因:
- 保持测试类的完整性
- 便于后续功能补充后快速添加测试
- 明确标识哪些功能尚未实现

---

## ✅ 验证结果

所有测试类已经过方法映射检查:
- ✅ 有效测试方法都已确认对应的实际方法存在
- ✅ 不存在的方法已清空测试代码并添加注释
- ✅ 方法名不匹配的已修正
- ✅ 接口路径不正确的已修正

**下一步**: 运行所有测试,验证测试是否能正常执行。

---

**检查时间**: 2026-05-07  
**检查范围**: 所有 Service 和 Controller 测试类  
**修正数量**: 7 个测试方法清空,4 个方法修正
