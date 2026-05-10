<!--
 * 搜索歌曲列表
-->
<template>
    <div class="search-song-Lists">
       <content-list :contentList="albumDatas"></content-list>
    </div>
</template>
<script>
import ContentList from '../ContentList';
import {getSongListOfLikeTitle, recordSearchHistory} from '../../api/index';
import {mixin} from "../../mixins";

export default {
    name: 'search-song-lists',
    components:{
        ContentList
    },
    mixins: [mixin],
    data(){
        return{
            albumDatas: []
        }
    },
    mounted(){
        this.getSearchList();
    },
    methods:{
        getSearchList(){
            if(!this.$route.query.keywords){
                this.notify('您输入的内容为空','warning')
            }else{
                getSongListOfLikeTitle(this.$route.query.keywords)
                    .then(res =>{
                        if(res){
                            this.albumDatas = res
                        }else{
                            this.notify('暂无该歌曲内容','warning')
                        }
                    })
                
                // 记录搜索历史
                if (this.userId) {
                    recordSearchHistory({
                        userId: this.userId,
                        keywords: this.$route.query.keywords,
                        searchType: 2  // 2表示歌单搜索
                    }).catch(err => {
                        console.error('记录搜索历史失败:', err);
                    });
                }
            }
        }
    }
}
</script>

<style lang="scss" scoped>
@import '../../assets/css/search-song-lists.scss';
</style>
