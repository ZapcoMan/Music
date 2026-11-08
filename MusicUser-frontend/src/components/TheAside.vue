<!--
 * 播放列表
-->
<template>
    <transition name="slide-fade">
        <div class="the-aside" v-if="showAside">
            <h2 class="title">播放列表</h2>
            <ul class="menus">
                <li v-for="item in listOfSongs" :key="item.id" :class="{'is-play': id==item.id}"
                @click="toplay(item.id,item.url,item.pic,item.index,item.name,item.lyric)">
                    {{replaceFName(item.name)}}
                </li>
            </ul>
        </div>
    </transition>
</template>
<script>
import {mapGetters} from 'vuex';
import { getCollectOfUserId } from '../api/index';
import { parseLyric } from '../utils/lyric';

export default {
    name: 'the-aside',
    computed: {
        ...mapGetters([
            'showAside',         //是否显示播放中的歌曲列表
            'listOfSongs',       //当前歌曲列表
            'id',                //播放中的音乐id
            'loginIn',              //用户是否已登录
            'userId',               //当前登录用户的id
            'isActive',             //当前播放的歌曲是否已收藏
        ])
    },
    mounted(){
        // 点击页面任意处收起侧栏。此前用的是匿名函数且从不解除监听：
        // 组件销毁后闭包里的 _this 仍指向已销毁实例，每次进入该页都会多挂一个
        this.closeAside = () => {
            this.$store.commit('setShowAside', false);
        };
        document.addEventListener('click', this.closeAside, true);
    },
    beforeDestroy(){
        // capture 参数必须与注册时一致，否则移除不生效
        document.removeEventListener('click', this.closeAside, true);
    },
    methods:{
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
            // 直接调模块函数：本组件没有 mixins: [mixin]，
            // 原先这里靠的是自己那份会卡死页面的拷贝
            this.$store.commit('setLyric',parseLyric(lyric));
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
    }
}
</script>
<style  lang="scss" scoped>
@import '../assets/css/the-aside.scss';
</style>
