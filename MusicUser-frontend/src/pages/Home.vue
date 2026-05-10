<!--
 * 首页
-->
<template>
  <div class="home">
    <swiper/>
    <div class="section" v-for="(item, index) in songsList" :key="index">
      <div class="section-title"><i :class="item.icon" style="font-size: 34px;"></i>
        <b style="font-size: 32px;">
          {{ item.name }}
        </b>
      </div>
      <content-list :contentList="item.list"></content-list>
    </div>

<!--    <div v-if="loginIn">-->
<!--      &lt;!&ndash; 如果 condition 为真，渲染这个 div &ndash;&gt;-->
<!--      <div class="section" v-for="(item, index) in recommendedsongsList" :key="index">-->
<!--        <div class="section-title"><i :class="item.icon" style="font-size: 34px;"></i>-->
<!--          <b style="font-size: 32px;">-->
<!--            {{ item.name }}-->
<!--          </b>-->
<!--        </div>-->
<!--        <content-list :contentList="item.list"></content-list>-->
<!--      </div>-->
<!--    </div>-->
    <div v-if="loginIn">
      <!-- 如果 condition 为真，渲染这个 div -->
      <div class="section" v-for="(item, index) in recommendedsongsList" :key="index">
        <div class="section-title"><i :class="item.icon" style="font-size: 34px;"></i>
          <b style="font-size: 32px;">
            {{ item.name }}
          </b>
          <el-tag type="success" size="small" style="margin-left: 10px;">智能推荐</el-tag>
        </div>
        <el-table
          :data="tableData"
          stripe
          style="width: 100%">
          <el-table-column
            prop="name"
            label="歌曲名字"
            width="350">
          </el-table-column>
          <el-table-column
            prop="introduction"
            label="简介"
            width="250">
          </el-table-column>
          <el-table-column
            label="标签"
            width="120">
            <template slot-scope="scope">
              <el-tag v-if="isNewSong(scope.row.createTime)" 
                      type="success" size="mini">
                🎵 新歌
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="200">
            <template slot-scope="scope">
              <el-button 
                size="mini" 
                type="primary"
                icon="el-icon-video-play"
                @click="playSong(scope.row)">
                播放
              </el-button>
              <el-button 
                size="mini" 
                type="success"
                icon="el-icon-star-on"
                @click="collectSong(scope.row)">
                收藏
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
    </div>

</template>

<script>
import Swiper from "../components/Swiper";
import contentList from "../components/ContentList";
import {getAllSinger, getAllSongList, getRecommendedsongsList, setCollect} from "../api/index";
import {mixin} from '../mixins';

export default {
  name: "home",
  mixins: [mixin],
  components: {
    Swiper,
    contentList,
  },
  data() {
   const loginIn = JSON.parse(window.sessionStorage.getItem('loginIn'))
    return {
      loginIn:loginIn,
      songsList: [
        {name: "歌单", list: [], icon: "iconfont icon-r-list"},
        {name: "歌手", list: [], icon: "iconfont icon-r-love"},
      ],
      recommendedsongsList: [
        {name: "推荐歌曲", list: [], icon: "iconfont icon-r-list"},
      ],
      tableData: []  // 初始化为空数组，等待后端数据
    };
  },
  created() {
    this.getSongList();
    this.getSinger();
    this.getRecommendedsongsList();

  },
  methods: {
    getSongList() {
      //获取前十条歌单
      getAllSongList()
        .then((res) => {
          // 处理后端可能返回 List<SongList> 或 R<List<SongList>>
          if (Array.isArray(res)) {
            this.songsList[0].list = res.slice(0, 10);
          } else if (res.code === 20000 || res.success === true) {
            this.songsList[0].list = res.data.slice(0, 10);
          }
        })
        .catch((e) => {
          if (e.data == undefined) {
            if (
              e ==
              "TypeError: Cannot read properties of undefined (reading 'status')"
            ) {
              e = "Network Error";
            }
            this.$message({
              showClose: true,
              message: e,
              type: "error",
              duration: 5000,
            });
          } else {
            this.$message({
              showClose: true,
              message: e.data,
              type: "error",
              duration: 5000,
            });
          }
        });
    },
    getSinger() {
      //获取前十名歌手
      getAllSinger()
        .then((res) => {
          // 处理后端可能返回 List<Singer> 或 R<List<Singer>>
          if (Array.isArray(res)) {
            this.songsList[1].list = res.slice(0, 10);
          } else if (res.code === 20000 || res.success === true) {
            this.songsList[1].list = res.data.slice(0, 10);
          }
        })
        .catch((e) => {
          if (e.data == undefined) {
            if (
              e ==
              "TypeError: Cannot read properties of undefined (reading 'status')"
            ) {
              e = "Network Error";
            }
            this.$message({
              showClose: true,
              message: e,
              type: "error",
              duration: 5000,
            });
          } else {
            this.$message({
              showClose: true,
              message: e.data,
              type: "error",
              duration: 5000,
            });
          }
        });
    },
    getRecommendedsongsList() {
     const userId = JSON.parse(window.sessionStorage.getItem('userId'))
      console.log('userId : '+ userId)
      //获取推荐歌曲
      getRecommendedsongsList(userId)
        .then((res) => {
          // 处理后端返回 R<List<Song>>
          if (res.code === 20000 || res.success === true) {
            this.tableData = res.data;
            this.notify(`为您智能推荐 ${res.data.length} 首歌曲`, 'success');
          } else {
            this.$message({
              showClose: true,
              message: res.message || '获取推荐歌曲失败',
              type: "error",
              duration: 5000,
            });
          }
        })
        .catch((e) => {
          if (e.data == undefined) {
            if (
              e ==
              "TypeError: Cannot read properties of undefined (reading 'status')"
            ) {
              e = "Network Error";
            }
            this.$message({
              showClose: true,
              message: e,
              type: "error",
              duration: 5000,
            });
          } else {
            this.$message({
              showClose: true,
              message: e.data,
              type: "error",
              duration: 5000,
            });
          }
        });
    },
    // 播放歌曲
    playSong(song) {
      this.$store.commit('setShowAside', true);
      this.toplay(song.id, song.url, song.pic, 0, song.name, song.lyric);
      this.notify(`正在播放: ${song.name}`, 'success');
    },
    // 收藏歌曲
    collectSong(song) {
      if (!this.loginIn) {
        this.notify('请先登录', 'warning');
        return;
      }
      
      const params = new URLSearchParams();
      params.append('userId', this.userId);
      params.append('type', 0); // 0=歌曲
      params.append('songId', song.id);
      
      setCollect(params)
        .then((res) => {
          if (res.code === 20000 || res.success === true) {
            this.notify('收藏成功', 'success');
          } else {
            this.notify(res.message || '收藏失败', 'error');
          }
        })
        .catch(() => {
          this.notify('收藏失败', 'error');
        });
    },
    // 判断是否为新歌（30天内）
    isNewSong(createTime) {
      if (!createTime) return false;
      const now = new Date().getTime();
      const create = new Date(createTime).getTime();
      const days = (now - create) / (1000 * 60 * 60 * 24);
      return days <= 30;
    },
  },
};
</script>

<style lang="scss" scoped>
@import "../assets/css/home.scss";
</style>
