<!--
 * 搜索歌曲
-->
<template>
    <div class="search">
        <!-- 搜索历史 -->
        <div class="search-history" v-if="showHistory && historyKeywords.length > 0">
            <div class="history-header">
                <span class="history-title">搜索历史</span>
                <span class="clear-btn" @click="handleClearHistory">清空</span>
            </div>
            <div class="history-tags">
                <el-tag 
                    v-for="(keyword, index) in historyKeywords" 
                    :key="index"
                    class="history-tag"
                    @click="handleSearchByKeyword(keyword)">
                    {{ keyword }}
                </el-tag>
            </div>
        </div>
        
        <nav class="searchList-nav" ref="change">
            <span :class="{isActive: toggle=='Songs'}" @click="handleChangeView('Songs')">歌曲</span>
            <span :class="{isActive: toggle=='SongLists'}" @click="handleChangeView('SongLists')">歌单</span>
        </nav>
        <component :is="currentView"></component>
    </div>
</template>
<script>
import searchSongs from '../components/search/SearchSongs'
import searchSongLists from '../components/search/SearchSongLists'
import { getRecentSearchKeywords, clearSearchHistory } from '../api'
import {mixin} from '../mixins'

export default {
   name: 'search',
   // 本页调用了 this.getSong() 与 this.userId，两者都由 mixin 提供
   // （getSong 定义在 mixins/index.js，userId 来自其 mapGetters）。
   // 此前没有混入，点击搜索历史标签会直接抛 "this.getSong is not a function"，
   // this.userId 也恒为 undefined，导致历史列表根本不会加载。
   mixins: [mixin],
   components: {
       searchSongs,
       searchSongLists
   },
   data(){
       return {
           toggle: 'Songs',
           currentView: 'searchSongs',
           historyKeywords: [],
           showHistory: true
       }
   },
   created() {
       this.loadSearchHistory();
   },
   methods:{
       //切换组件
       handleChangeView(component){
           this.currentView = 'search'+component;
           this.toggle = component;
       },
       //加载搜索历史
       loadSearchHistory() {
           const userId = this.userId;
           if (userId) {
               getRecentSearchKeywords(userId, 10).then(res => {
                   this.historyKeywords = res || [];
               }).catch(err => {
                   console.error('加载搜索历史失败:', err);
               });
           }
       },
       //通过关键词搜索
       handleSearchByKeyword(keyword) {
           this.showHistory = false;
           // 原来这里 commit('setSearchWord', keyword)，但 store 的 user 模块只有
           // setUserId/setUsername/setAvator 三个 mutation，Vuex 对未知 mutation
           // 仅打印警告后静默返回，点击搜索历史因此毫无反应。
           // getSong 依赖 $route.query.keywords，故先写路由再取结果。
           this.$router.replace({ path: '/search', query: { keywords: keyword } })
               .then(() => {
                   this.getSong();
               })
               .catch(() => {
                   // 路由参数未变化会抛 NavigationDuplicated，仍需执行搜索
                   this.getSong();
               });
       },
       //清空搜索历史
       handleClearHistory() {
           const userId = this.userId;
           if (userId) {
               clearSearchHistory(userId).then(() => {
                   this.historyKeywords = [];
                   this.$message.success('清空成功');
               }).catch(err => {
                   this.$message.error('清空失败');
               });
           }
       }
   }
}
</script>
<style lang="scss" scoped>
@import '../assets/css/search.scss';

.search-history {
    padding: 20px;
    background: #fff;
    margin-bottom: 10px;
    border-radius: 4px;
    
    .history-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;
        
        .history-title {
            font-size: 16px;
            font-weight: bold;
            color: #333;
        }
        
        .clear-btn {
            font-size: 14px;
            color: #999;
            cursor: pointer;
            
            &:hover {
                color: #666;
            }
        }
    }
    
    .history-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        
        .history-tag {
            cursor: pointer;
            transition: all 0.3s;
            
            &:hover {
                transform: translateY(-2px);
                box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            }
        }
    }
}
</style>
