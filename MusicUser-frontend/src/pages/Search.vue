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

export default {
   name: 'search',
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
           const userId = this.$store.state.userId;
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
           this.$store.commit('setSearchWord', keyword);
           this.showHistory = false;
           this.getSong();
       },
       //清空搜索历史
       handleClearHistory() {
           const userId = this.$store.state.userId;
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
