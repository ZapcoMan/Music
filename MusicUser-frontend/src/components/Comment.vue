<!--
 * 评论
-->
<template>
  <div>
    <div class="comment">
      <div style="width: 100%; text-align: center">
        <i class="iconfont icon-r-team" style="font-size: 36px"></i>
        <b style="font-size: 24px"> 评论</b>
      </div>
      <div class="comment-msg">
        <div class="comment-img">
          <img :src="attachImageUrl(avator)"/>
        </div>
        <el-input
          class="comment-input"
          type="textarea"
          :rows="2"
          placeholder="请输入内容"
          v-model="textarea"
        >
        </el-input>
      </div>
      <el-button type="primary" class="sub-btn" @click="postComment" style="font-size: 20px"
      ><i class="iconfont icon-r-yes" style="font-size: 22px"></i> 评论
      </el-button
      >
    </div>

<!--      <el-tag type="info"  v-for="(item, index) in featuredComment" :key="index">{{featuredComment[index]}}</el-tag>-->
    <div class="tag-group">
      <span class="tag-group__title">精选评论：</span>
      <el-tag
        v-for="(item, index) in featuredComment" :key="index" :type="info"
        effect="plain" size="medium">
        {{featuredComment[index]}}
      </el-tag>
    </div>
    <p>精彩评论：共{{ commentList.length }}条评论</p>
    <ul class="popular" v-for="(item, index) in commentList" :key="index">
      <li>
        <div class="popular-img">
          <img :src="attachImageUrl(userPic[index])"/>
        </div>
        <div class="popular-msg">
          <ul>
            <li class="name">{{ userName[index] }}</li>
            <li class="time">{{ item.createTime }}</li>
            <li class="content">{{ item.content }}</li>
          </ul>
        </div>
        <div
          class="up"
          ref="up"
          @click="postUp(item.id, item.up, index)"
        >
          <svg class="icon">
            <use xlink:href="#icon-zan"></use>
          </svg>
          {{ item.up }}
        </div>
      </li>
    </ul>
  </div>
</template>

<script>
  import {mixin} from "../mixins";
  import {mapGetters} from "vuex";
  import {setComment, setLike, getAllComment, getUserOfId, getfeaturedComment} from "../api/index";

  export default {
    name: "comment",
    mixins: [mixin],
    props: [
      "playId", // 歌曲或歌单id
      "type", // 0歌曲、1歌单
    ],
    computed: {
      ...mapGetters([
        "id", // 歌曲或歌单id
        "loginIn", // 用户是否已登录
        "userId", // 当前登录用户id
        "avator", // 当前登录用户头像
      ]),
    },
    data() {
      return {
        textarea: "", // 存放输入的评论内容
        commentList: [], // 存放评论列表
        userPic: [], // 用户的头像
        userName: [], // 用户的昵称
        featuredComment: ['11', '很好', '很棒']
      };
    },
    mounted() {
      this.getComment();
      this.getfeaturedComment();
    },
    methods: {
      // 提交评论
      postComment() {
        if (this.loginIn) {
          let params = new URLSearchParams();
          // eslint-disable-next-line eqeqeq
          if (this.type == 0) {
            params.append("songId", this.playId);
          } else {
            params.append("songListId", this.playId);
          }
          params.append("userId", this.userId);
          params.append("type", this.type);
          params.append("content", this.textarea);
          setComment(params)
            .then((res) => {
              if (res.code == 20000 || res.success === true) {
                this.notify(res.message || "评论成功", "success");
                this.textarea = "";
                this.getComment();
              } else {
                this.notify(res.message || "评论失败", "error");
              }
            })
            .catch((_err) => {
              this.notify("评论失败", "error");
            });
        } else {
          this.rank = null;
          this.notify("请先登录", "warning");
        }
      },
      // 评论列表
      getComment() {
        getAllComment(this.type, this.playId)
          .then((res) => {
            // 后端统一返回 R 后，评论数组在 res.data
            const list = res.data || [];
            this.commentList = list;
            for (let item of list) {
              this.getUsers(item.userId);
            }
          })
          .catch((_err) => {
            this.notify("评论加载失败", "error");
          });
      },
      // 获取用户的头像和昵称
      getUsers(id) {
        getUserOfId(id)
          .then((res) => {
            this.userPic.push(res.avator);
            this.userName.push(res.username);
          })
          .catch((_err) => {
            this.notify("出错了", "error");
          });
      },
      getfeaturedComment(){
        const  id = this.playId;
        console.log('id' , id);
        getfeaturedComment(id).then((res) =>{
          console.log('res.data:'+res.data);
          this.featuredComment = res.data;
        })

      },
      // 给某一个评论点赞
      postUp(id, up, index) {
        if (this.loginIn) {
          let params = new URLSearchParams();

          params.append("id", id);
          params.append("up", up + 1);
          setLike(params)
            .then((res) => {
              if (res.code == 20000 || res.success === true) {
                this.$refs.up[index].children[0].style.color =
                  "#2796cd";
                this.getComment();
              } else {
                this.notify(res.message || "点赞失败", "error");
              }
            })
            .catch((_err) => {
              this.notify("点赞失败", "error");
            });
        } else {
          this.rank = null;
          this.notify("请先登录", "warning");
        }
      },
    },
  };
</script>
<style lang="scss" scoped>
  @import "../assets/css/comment.scss";
  /* element-ui 样式已由 main.js 从 node_modules 引入；
     这里再从 unpkg 拉一份 2.15.14 会与本地 ^2.15.6 版本不一致，
     同一组件出现两套 CSS，改样式时看不出哪份生效 */
</style>
