/**
 * 播放状态。
 *
 * 修正要点：
 * 1. 原 getter 一律写成 if (!state.x) 就回读 sessionStorage，把"未初始化"与
 *    "合法的零值"混为一谈：listIndex=0（第一首）被历史索引替换导致上下首跳错歌，
 *    isPlay=false（刚暂停）被恢复成旧的 true，curTime=0 被旧进度覆盖使进度条停在半路，
 *    listOfSongs 显式清空为 [] 后又被旧缓存填回（搜索无结果时残留上一次列表）。
 *    现在初值为 null，只有 null 才回读。
 * 2. 原 mutation 每次都全量 JSON.stringify；setCurTime 由 timeupdate 高频触发，
 *    等于每秒序列化整个歌单，而进度并不需要跨刷新恢复。现只持久化列表与曲目信息。
 * 3. JSON.parse 加保护，脏数据不再让初始化抛错。
 */
function readCache(key, fallback) {
    const raw = window.sessionStorage.getItem(key);
    if (raw === null) {
        return fallback;
    }
    try {
        const parsed = JSON.parse(raw);
        return parsed === null ? fallback : parsed;
    } catch (e) {
        return fallback;
    }
}

const song = {
    state:{
        // 初值统一为 null，表示"尚未初始化"；
        // 只有 null 才回读 sessionStorage，0 / false / '' 等合法零值原样保留
        listOfSongs: null,     //当前歌曲列表
        isPlay: null,          //是否播放中
        url: '',               //歌曲地址
        id: null,              //歌曲id
        playButtonUrl: null,   //播放状态的图标
        duration: null,        //音乐时长
        curTime:  null,        //当前音乐的播放位置（瞬时状态，不持久化）
        changeTime: null,      //指定播放时刻（瞬时状态，不持久化）
        title: null,           //歌名
        artist: null,          //歌手名
        picUrl: null,          //歌曲图片
        autoNext: null,        //用于自动触发播放下一首
        lyric: null,           //未处理的歌词数据
        tempList: null,        //单个歌单信息或歌手信息
        listIndex: null,       //当前歌曲在歌单中的位置
        volume: null           //音量
    },
    getters: {
        // 只在确实未初始化（null）时回读；0 / false / '' 是有效状态，不再被历史值覆盖。
        // 回读统一走 readCache：脏数据不再让整个播放器初始化失败，并按类型给合理兜底
        listOfSongs: state => state.listOfSongs !== null ? state.listOfSongs : readCache('listOfSongs', []),
        isPlay: state => state.isPlay !== null ? state.isPlay : readCache('isPlay', false),
        url: state => state.url !== null ? state.url : readCache('url', ''),
        id: state => state.id !== null ? state.id : readCache('id', ''),
        playButtonUrl: state => state.playButtonUrl !== null ? state.playButtonUrl : readCache('playButtonUrl', '#icon-bofang'),
        duration: state => state.duration !== null ? state.duration : readCache('duration', 0),
        // 播放进度是高频瞬时状态：不回读旧值，否则进度条一进来就停在半路
        curTime: state => state.curTime !== null ? state.curTime : 0,
        changeTime: state => state.changeTime !== null ? state.changeTime : 0,
        title: state => state.title !== null ? state.title : readCache('title', ''),
        artist: state => state.artist !== null ? state.artist : readCache('artist', ''),
        picUrl: state => state.picUrl !== null ? state.picUrl : readCache('picUrl', ''),
        autoNext: state => state.autoNext !== null ? state.autoNext : readCache('autoNext', true),
        lyric: state => state.lyric !== null ? state.lyric : readCache('lyric', []),
        tempList: state => state.tempList !== null ? state.tempList : readCache('tempList', {}),
        listIndex: state => state.listIndex !== null ? state.listIndex : readCache('listIndex', -1),
        volume: state => state.volume !== null ? state.volume : readCache('volume', 50)
    },
    mutations: {
        setListOfSongs: (state,listOfSongs) => {
            state.listOfSongs = listOfSongs;
            window.sessionStorage.setItem('listOfSongs',JSON.stringify(listOfSongs));
        },
        setIsPlay: (state,isPlay) => {
            state.isPlay = isPlay;
            window.sessionStorage.setItem('isPlay',JSON.stringify(isPlay));
        },
        setUrl: (state,url) => {
            state.url = url;
            window.sessionStorage.setItem('url',JSON.stringify(url));
        },
        setId: (state,id) => {
            state.id = id;
            window.sessionStorage.setItem('id',JSON.stringify(id));
        },
        setPlayButtonUrl: (state,playButtonUrl) => {
            state.playButtonUrl = playButtonUrl;
            window.sessionStorage.setItem('playButtonUrl',JSON.stringify(playButtonUrl));
        },
        setDuration: (state,duration) => {
            state.duration = duration;
            window.sessionStorage.setItem('duration',JSON.stringify(duration));
        },        
        // 播放进度只存内存：避免 timeupdate 高频序列化整个歌单
        setCurTime: (state,curTime) => {
            state.curTime = curTime;
        },
        setChangeTime: (state,changeTime) => {
            state.changeTime = changeTime;
        },
        setTitle: (state,title) => {
            state.title = title;
            window.sessionStorage.setItem('title',JSON.stringify(title));
        },
        setArtist: (state,artist) => {
            state.artist = artist;
            window.sessionStorage.setItem('artist',JSON.stringify(artist));
        },
        setPicUrl: (state,picUrl) => {
            state.picUrl = picUrl;
            window.sessionStorage.setItem('picUrl',JSON.stringify(picUrl));
        },
        setAutoNext: (state,autoNext) => {
            state.autoNext = autoNext;
            window.sessionStorage.setItem('autoNext',JSON.stringify(autoNext));
        },
        setLyric: (state,lyric) => {
            state.lyric = lyric;
            window.sessionStorage.setItem('lyric',JSON.stringify(lyric));
        },
        setTempList: (state,tempList) => {
            state.tempList = tempList;
            window.sessionStorage.setItem('tempList',JSON.stringify(tempList));
        },
        setListIndex: (state,listIndex) => {
            state.listIndex = listIndex;
            window.sessionStorage.setItem('listIndex',JSON.stringify(listIndex));
        },
        setVolume: (state,volume) => {
            state.volume = volume;
            window.sessionStorage.setItem('volume',JSON.stringify(volume));
        }
    }
}

export default song