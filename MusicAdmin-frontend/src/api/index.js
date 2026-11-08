import {get,post,del,patch} from "./http";

// 判断管理员是否登录成功
export const getLoginStatus = (params) => post(`admin/sessions`,params);


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
export const listSongDetail = (songListId) => get(`listSong/detail?songListId=${songListId}`);
//给歌单增加歌曲
export const listSongAdd = (params) => post(`listSong/add`,params);
//删除歌单的歌曲
export const delListSong = (songId,songListId) => del(`listSong/delete?songId=${songId}&songListId=${songListId}`);

//============用户相关================
//查询用户
export const getAllConsumer =() => get(`consumers`);
//分页查询用户
export const getConsumerByPage =(pageNum, pageSize) => get(`consumers/page?pageNum=${pageNum}&pageSize=${pageSize}`);
//添加用户
export const setConsumer = (params) => post(`consumers/registrations`,params);
//编辑用户
export const updateConsumer = (params) => patch(`consumers/profile`,params);
//删除用户
export const delConsumer = (id) => del(`consumers/${id}`);
//根据用户id查询该用户的详细信息
export const getUserOfId =(id) => get(`/consumers/${id}`);

//===============收藏===================
//指定用户的收藏列表
export const getCollectOfUserId = (userId) => get(`/collect/collectOfUserId?userId=${userId}`);
//删除用户收藏的歌曲
export const deleteCollection = (userId,songId) => del(`collect/delete?userId=${userId}&songId=${songId}`);

//===============评论===================
//指定歌单的评论列表
export const getCommentOfSongListId = (songListId) => get(`/comment/commentOfSongListId?songListId=${songListId}`);
//删除评论
export const deleteComment = (id) => del(`comment/delete?id=${id}`);

