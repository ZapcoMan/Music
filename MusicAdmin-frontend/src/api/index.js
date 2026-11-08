import {get,post,del,patch} from "./http";

// 判断管理员是否登录成功
export const getLoginStatus = (params) => post(`admin/sessions`,params);
//登出：让后端把当前令牌放进黑名单。只删 localStorage 的话，
//令牌在它自己的有效期内仍然可用，登出实际上没有生效
export const adminLogout = () => del(`admin/sessions`);


//============歌手相关================
//查询歌手
export const getAllSinger =() => get(`singers`);
//分页查询歌手
export const getSingerByPage =(pageNum, pageSize) => get(`singers/page?pageNum=${pageNum}&pageSize=${pageSize}`);
//添加歌手
export const setSinger = (params) => post(`singers`,params);
//编辑歌手
export const updateSinger = (params) => patch(`singers/${params.id}`,params);
//删除歌手
export const delSinger = (id) => del(`singers/${id}`);

//============歌曲相关================
//根据歌手id查询歌曲
export const songOfSingerId =(id) => get(`singers/${id}/songs`);
//编辑歌曲
export const updateSong = (params) => patch(`songs/${params.id}`,params);
//删除歌曲
export const delSong = (id) => del(`songs/${id}`);
//根据歌曲id查询歌曲对象
export const songOfSongId =(id) => get(`songs/${id}`);
//根据歌曲名获取歌曲对象
export const songOfSongName =(songName) => get(`songs?songName=${songName}`);
//查询所有歌曲
export const allSong =() => get(`songs`);

//============歌单相关================
//查询歌单
export const getAllSongList =() => get(`song-lists`);
//分页查询歌单
export const getSongListByPage =(pageNum, pageSize) => get(`song-lists/page?pageNum=${pageNum}&pageSize=${pageSize}`);
//添加歌单
export const setSongList = (params) => post(`song-lists`,params);
//编辑歌单
export const updateSongList = (params) => patch(`song-lists/${params.id}`,params);
//删除歌单
export const delSongList = (id) => del(`song-lists/${id}`);

//============歌单的歌曲相关============
//根据歌单id查询歌曲列表
export const listSongDetail = (songListId) => get(`song-lists/${songListId}/songs`);
//给歌单增加歌曲
export const listSongAdd = (songListId, params) => post(`song-lists/${songListId}/songs`,params);
//删除歌单的歌曲
export const delListSong = (songId,songListId) => del(`song-lists/${songListId}/songs/${songId}`);

//============用户相关================
//查询用户
export const getAllConsumer =() => get(`consumers`);
//分页查询用户
export const getConsumerByPage =(pageNum, pageSize) => get(`consumers/page?pageNum=${pageNum}&pageSize=${pageSize}`);
//添加用户
export const setConsumer = (params) => post(`consumers/registrations`,params);
//编辑用户（管理员侧端点：目标用户取自路径 id，故 id 单独作第一个参数，
//不塞进 body。自助的 consumers/profile 只认令牌身份且
//@RequireUserType("consumer")，admin 令牌打过去必然 403，不能拿来做管理端编辑）
export const updateConsumer = (id, params) => patch(`consumers/${id}`,params);
//删除用户
export const delConsumer = (id) => del(`consumers/${id}`);
//根据用户id查询该用户的详细信息
export const getUserOfId =(id) => get(`/consumers/${id}`);

//===============收藏===================
//指定用户的收藏列表
export const getCollectOfUserId = () => get(`/collections/mine`);
//删除用户收藏的歌曲
export const deleteCollection = (songId) => del(`collections?songId=${songId}`);

//===============评论===================
//指定歌单的评论列表
export const getCommentOfSongListId = (songListId) => get(`/song-lists/${songListId}/comments`);
//删除评论
export const deleteComment = (id) => del(`comments/${id}`);

