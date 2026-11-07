import Axios from "axios";
import {get,post,del,patch} from "./http";

//============歌手相关================
//查询歌手
export const getAllSinger =() => get(`singers`);
//根据性别查询歌手
export const getSingerOfSex = (sex) => get(`singers?sex=${sex}`);

//============歌曲相关================
//根据歌手id查询歌曲
export const songOfSingerId =(id) => get(`song/manage/singer/detail?singerId=${id}`);
//根据歌曲id查询歌曲对象
export const songOfSongId =(id) => get(`song/manage/detail?songId=${id}`);
//根据歌手名字模糊查询歌曲
export const likeSongOfName =(keywords) => get(`song/manage/likeSongOfName?songName=${keywords}`);

//============歌单相关================
//查询歌单
export const getAllSongList =() => get(`songList/allSongList`);
//返回标题包含文字的歌单列表
export const getSongListOfLikeTitle = (keywords) => get(`songList/likeTitle?title=${keywords}`);
//根据风格模糊查询歌单列表
export const getSongListOfLikeStyle = (style) => get(`songList/likeStyle?style=${style}`);
export const getRecommendedsongsList = (id) => post(`songList/queryRecommendedSongListById?id=${id}`)

//============歌单的歌曲相关============
//根据歌单id查询歌曲列表
export const listSongDetail = (songListId) => get(`listSong/detail?songListId=${songListId}`);

//============用户相关================
//查询用户
export const getAllConsumer =() => get(`consumers`);
//注册
export const SignUp =(params) => post(`/consumers/registrations`,params);
//登录
export const loginIn =(params) => post(`/consumers/sessions`,params);
//根据用户id查询该用户的公开信息（账号/头像），用于评论区展示他人身份
export const getUserOfId =(id) => get(`/consumers/${id}`);
//查询本人完整资料（含手机号、邮箱等私密字段），身份由令牌决定
export const getMyProfile =() => get(`/consumers/profile`);
//更新用户信息
export const updateUserMsg =(params) => patch(`/consumers/profile`,params);


//下载音乐
export const download = (url)  => Axios({
    method: 'get',
    url: url,
    responseType: 'blob'
});

//===========评价======================
//提交评分
export const setRank =(params) => post(`/rank/add`,params);
//获取指定歌单的平均分
export const getRankOfSongListId = (songListId) => get(`/rank?songListId=${songListId}`);

//===========评论======================
//提交评论
export const setComment =(params) => post(`/comment/add`,params);
//点赞
export const setLike =(params) => post(`/comment/like`,params);
//返回当前歌单或歌曲的评论列表
export const getAllComment = (type,id) => {
    if(type == 0){              //歌曲
        return get(`/comment/commentOfSongId?songId=${id}`);
    }else{                      //歌单
        return get(`/comment/commentOfSongListId?songListId=${id}`);
    }
}

//===============收藏===================
//新增收藏
export const setCollect =(params) => post(`/collect/add`,params);
//指定用户的收藏列表
export const getCollectOfUserId = (userId) => get(`/collect/collectOfUserId?userId=${userId}`);
export const  delCollectOfUserId = (userId,songId) => del(`/collect/delete?userId=${userId}&songId=${songId}`)
export const getfeaturedComment =(songId) => get(`/comment/frequent-words?songId=${songId}`)

//===========搜索历史==================
//记录搜索历史
export const recordSearchHistory = (params) => post(`/search-history/record`, params);
//获取最近搜索历史
export const getRecentSearchHistory = (userId, limit = 20) => get(`/search-history/recent?userId=${userId}&limit=${limit}`);
//获取最近搜索关键词
export const getRecentSearchKeywords = (userId, limit = 10) => get(`/search-history/keywords?userId=${userId}&limit=${limit}`);
//清空搜索历史
export const clearSearchHistory = (userId) => Axios({
    method: 'delete',
    url: `/search-history/clear?userId=${userId}`
});
