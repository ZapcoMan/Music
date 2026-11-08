import {mapGetters} from 'vuex';
import { parseLyric } from '../utils/lyric';
import {likeSongOfName,getCollectOfUserId, recordSearchHistory } from '../api/index';

export const mixin = {
    computed: {
        ...mapGetters([            
            'loginIn',              //用户是否已登录
            'userId',               //当前登录用户的id
        ])
    },
    methods: {
        //提示信息
        notify(title,type) {
            this.$notify({
                title: title,
                type: type
            })
        },

        //获取图片地址
        attachImageUrl (srcUrl) {
            return srcUrl? this.$store.state.configure.HOST+srcUrl : this.$store.state.configure.HOST+'/img/user.jpg';
        },
        //根据歌手名字模糊查询歌曲
        getSong() {
            if(!this.$route.query.keywords){
                this.$store.commit('setListOfSongs',[]);
                this.notify('您输入的内容为空','warning');
            }else{
                likeSongOfName(this.$route.query.keywords).then(res => {
                    // 后端统一返回 R 后，搜索结果数组在 res.data
                    const list = res.data || [];
                    if(!list.length){
                        this.$store.commit('setListOfSongs',[]);
                        this.notify('系统暂无符合条件的歌曲','warning');
                    }else{
                        this.$store.commit('setListOfSongs',list);
                    }
                }).catch(err => {
                    console.error(err);
                    this.notify((err && err.userMessage) || '请求失败，请稍后重试', 'error');
                })
                
                // 记录搜索历史
                if (this.userId) {
                    recordSearchHistory({
                        userId: this.userId,
                        keywords: this.$route.query.keywords,
                        searchType: 0  // 0表示歌曲搜索
                    }).catch(err => {
                        console.error('记录搜索历史失败:', err);
                    });
                }
            }
        },
        //获取名字前半部分--歌手名
        replaceLName(str){
            let arr = str.split('-');
            return arr[0];
        },
        //获取名字后半部分--歌名
        replaceFName(str){
            let arr = str.split('-');
            return arr[1];
        },
        //播放
        toplay: function(id,url,pic,index,name,lyric){
            this.$store.commit('setId',id);
            this.$store.commit('setUrl',this.$store.state.configure.HOST+url);
            this.$store.commit('setPicUrl',this.$store.state.configure.HOST+pic);
            this.$store.commit('setListIndex',index);
            this.$store.commit('setTitle',this.replaceFName(name));
            this.$store.commit('setArtist',this.replaceLName(name));
            this.$store.commit('setLyric',this.parseLyric(lyric));
            this.$store.commit('setIsActive',false);
            if(this.loginIn){
                getCollectOfUserId()
                    .then(res =>{
                        // 收藏数组现随 R 一起返回
                        for(let item of res.data || []){
                            if(item.songId == id){
                                this.$store.commit('setIsActive',true);
                                break;
                            }
                        }
                    })
            }
        },
        //解析歌词
        parseLyric,
        //获取生日
        attachBirth(val){
            return val.substr(0,10);
        }
    }
}
