  <!--
 * 歌词
-->
<template>
  <div class="song-lyric">
    <h1 class="lyric-title">歌词</h1>
    <!-- 有歌词 -->
    <ul class="has-lyric" v-if="lyr.length" key="index">
      <li v-for="(item, index) in lyr" v-bind:key="index">
        {{ item[1] }}
      </li>
    </ul>
    <!-- 没有歌词 -->
    <div v-else class="no-lyric" key="no-lyric">
      <span>暂无歌词</span>
    </div>
  </div>
</template>
<script>
import { mixin } from "../mixins";
import { mapGetters } from "vuex";
import { songOfSongId } from "../api/index";

export default {
  name: "lyric",
  mixins: [mixin],
  data() {
    return {
      lyr: [], //当前歌曲的歌词
      songId: "", //当前歌曲id
    };
  },
  computed: {
    ...mapGetters([
      "curTime", //当前歌曲播放到的位置
      "id", //当前播放的歌曲id
      "lyric", //歌词
      "listIndex", //当前歌曲在歌单中的位置
      "listOfSongs", //当前歌单列表
    ]),
  },
  methods: {
    replaceLName(str) {
      let arr = str.split("-");
      return arr[0];
    },
    //获取名字后半部分--歌名
    replaceFName(str) {
      let arr = str.split("-");
      return arr[1];
    },
    //解析歌词
    loadFromQuery() {
      // 从url读取参数
      var songId = this.$route.query.songId;
      if (songId != null && songId != "") {
        songOfSongId(songId)
          .then((res) => {
            // GET /songs/{id} 返回 R<Song>，载荷在 res.data。
            // 此前有两层错位：判空拿整个信封去比 ""（永不成立），
            // 而后面取的都是信封上不存在的键，提交进 store 的
            // id/url/pic/name/lyric 全是 undefined，parseLyric(undefined) 还会抛。
            const song = res.data;
            if (song == null || song === "") {
              return;
            }

            this.$store.commit("setId", song.id);
            this.$store.commit(
              "setUrl",
              this.$store.state.configure.HOST + song.url
            );
            this.$store.commit(
              "setPicUrl",
              this.$store.state.configure.HOST + song.pic
            );
            this.$store.commit("setListIndex", 0);
            this.$store.commit("setTitle", this.replaceFName(song.name));
            this.$store.commit("setArtist", this.replaceLName(song.name));
            this.$store.commit("setLyric", this.parseLyric(song.lyric));
            this.$store.commit("setIsActive", false);
            this.lyr = this.lyric;
          })
          .catch((err) => {
            console.error(err);
            this.notify((err && err.userMessage) || '请求失败，请稍后重试', 'error');
          });
      } else {
            this.lyr = this.lyric;
      }
    },
  },
  mounted() {
    this.loadFromQuery();
  },
  watch: {
    id: function () {
      this.lyr = this.parseLyric(this.listOfSongs[this.listIndex].lyric);
    },
    curTime: function () {
      if (this.lyr.length > 0) {
        for (let i = 0; i < this.lyr.length; i++) {
          if (this.curTime >= this.lyr[i][0]) {
            for (let j = 0; j < this.lyr.length; j++) {
              document.querySelectorAll(".has-lyric li")[j].style.color =
                "#000";
              document.querySelectorAll(".has-lyric li")[j].style.fontSize =
                "15px";
            }
            if (i >= 0) {
              document.querySelectorAll(".has-lyric li")[i].style.color =
                "#95d2f6";
              document.querySelectorAll(".has-lyric li")[i].style.fontSize =
                "25px";
            }
          }
        }
      }
    },
  },
};
</script>
<style lang="scss" scoped>
@import "../assets/css/lyric.scss";
</style>
