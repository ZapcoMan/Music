<!--
 * 评论列表
-->
<template>
    <div class="content-list">
        <ul class="section-content">
            <li class="content-item" v-for="item in contentList" :key="item.id">
                <div class="kuo" @click="goAlbum(item,item.name)">
                    <img class="item-img" :src="attachImageUrl(item.pic)">
                    <div class="mask">
                        <svg class="icon">
                            <use xlink:href="#icon-bofang"></use>
                        </svg>
                    </div>
                </div>
                <p class="item-name" style="font-size: 22px; font-weight: 600;">{{item.name||item.title}}</p>
            </li>
        </ul>
    </div>
</template>
<script>
import {mixin} from '../mixins';
export default {
    name: 'content-list',
    mixins: [mixin],
    props: {
        // 父组件传的都是列表；给默认空数组是因为 Home 上曾出现过 item.list
        // 尚未就位时把 undefined 传进来，模板里的 v-for 直接报错
        contentList: {
            type: Array,
            default: () => [],
        },
    },
    methods: {
        goAlbum(item,type){
            this.$store.commit("setTempList",item);
            if(type){                   //歌手
                this.$router.push({path:`singer-album/${item.id}`});
            }else{                      //歌单
                this.$router.push({path:`song-list-album/${item.id}`});
            }
        }
    }
}
</script>
<style lang="scss" scoped>
@import '../assets/css/content-list.scss';
</style>
