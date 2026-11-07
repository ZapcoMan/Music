<!--
 * 页面头部
-->
<template>
  <div class="the-header">
    <div class="header-logo" @click="goHome">
        <img :src="require('../assets/img/logo02.jpg')" style="height: 30px;">
        <span style="color: rgba(67,52,166,0.71)">音乐网</span>
    </div>
    <ul class="navbar">
        <li :class="{active: item.name == activeName}" v-for="item in navMsg" :key="item.path" @click="goPage(item.path,item.name)">
            {{item.name}}
        </li>
        <li>
            <div class="header-search">
                <input type="text" placeholder="搜索音乐" @keyup.enter="goSearch()" v-model="keywords">
                <div class="search-btn" @click="goSearch()">
<!--                    <i class="iconfont icon-search-button" style="font-size: 28px;color:white;"></i>-->
                </div>
            </div>
        </li>
        <li v-show="!loginIn" :class="{active: item.name == activeName}" v-for="item in loginMsg" :key="item.path" @click="goPage(item.path,item.name)">
            {{item.name}}
        </li>
    </ul>
    <div class="header-right" v-show="loginIn">
        <div id='user'>
            <img :src='attachImageUrl(avator)'>
        </div>
        <ul class="menu">
            <li v-for="item in menuList" :key="item.path" @click="goMenuList(item.path)">{{item.name}}</li>
        </ul>
    </div>
  </div>
</template>

<script>
import {mapGetters} from 'vuex';
import {navMsg,loginMsg,menuList} from '../assets/data/header';

export default {
  name: 'the-header',
  data() {
      return {
          navMsg: [],    //左侧导航栏
          keywords: '',  //搜索关键字
          loginMsg: [],  //右侧导航栏
          menuList: [], //用户下拉菜单
      }
  },
  computed:{
      ...mapGetters([
          'activeName',
          'loginIn',
          'avator'
      ])
  },
  created() {
      this.navMsg = navMsg;
      this.loginMsg = loginMsg;
      this.menuList = menuList;
  },
  mounted(){
      // 此前三个监听全是匿名函数、且整个组件没有 beforeDestroy：路由切换后监听器
      // 随已销毁的实例残留下来，反复进出会不断叠加。
      // 另外 querySelector 没做空值保护，#user 不在当前 DOM 时这里直接抛 TypeError，
      // 把 mounted 后续逻辑一起带崩。
      const user = document.querySelector('#user');
      const menu = document.querySelector('.menu');
      if (!user || !menu) {
          return;
      }
      this.showMenu = (e) => {
          menu.classList.add("show");
          e.stopPropagation();           //关键在于阻止冒泡
      };
      this.stopMenuBubble = (e) => {
          e.stopPropagation();           //点击菜单内部时，阻止冒泡，这样点击内部时菜单不会关闭
      };
      this.hideMenu = () => {
          menu.classList.remove('show');
      };
      user.addEventListener('click', this.showMenu, false);
      menu.addEventListener('click', this.stopMenuBubble, false);
      document.addEventListener('click', this.hideMenu, false);
  },
  beforeDestroy(){
      const user = document.querySelector('#user');
      const menu = document.querySelector('.menu');
      if (user) {
          user.removeEventListener('click', this.showMenu, false);
      }
      if (menu) {
          menu.removeEventListener('click', this.stopMenuBubble, false);
          menu.classList.remove('show');
      }
      document.removeEventListener('click', this.hideMenu, false);
  },

  methods: {
       //提示信息
        notify(title,type) {
            this.$notify({
                title: title,
                type: type
            })
        },
      goHome() {
          this.$router.push({path: "/"});
      },
      goPage(path,name) {
          if(!this.loginIn && path=='/my-music'){
              this.notify('请先登录','warning');
          }else{
            this.$store.commit('setActiveName',name);
            this.$router.push({path: path});
          }
      },
      goSearch(){
          this.$router.push({path:'/search',query:{keywords: this.keywords}})
      },
      //获取图片地址
      attachImageUrl (srcUrl) {
        return srcUrl? this.$store.state.configure.HOST+srcUrl : '../assets/img/user.jpg';
      },
      goMenuList(path){
          if(path == 0){
              // 清除Token和用户信息
              localStorage.removeItem('userToken');
              this.$store.commit('setLoginIn',false);
              this.$store.commit('setIsActive',false);
              this.$router.go(0);
          }else{
              this.$router.push({path:path});
          }
      }
  }

}
</script>

<style lang="scss" scoped>
@import '../assets/css/the-header.scss';
</style>
