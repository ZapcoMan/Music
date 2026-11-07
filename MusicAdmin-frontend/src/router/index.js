import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

const router = new Router({
  routes: [
    {
      path: '/',
      component: resolve => require(['../pages/Login.vue'], resolve)
    },

    {
      path: '/Home',
      component: resolve => require(['../components/Home.vue'], resolve),
      children:[
        {
          path: '/Info',
          component: resolve => require(['../pages/InfoPage.vue'], resolve)
        },
        {
          path: '/Consumer',
          component: resolve => require(['../pages/ConsumerPage.vue'], resolve)
        },
        {
          path: '/Singer',
          component: resolve => require(['../pages/SingerPage.vue'], resolve)
        },
        {
          path: '/SongList',
          component: resolve => require(['../pages/SongListPage.vue'], resolve)
        },
        {
          path: '/Song',
          component: resolve => require(['../pages/SongPage.vue'], resolve)
        },
        {
          path: '/ListSong',
          component: resolve => require(['../pages/ListSongPage.vue'], resolve)
        },
        {
          path: '/Collect',
          component: resolve => require(['../pages/CollectPage.vue'], resolve)
        },
        {
          path: '/Comment',
          component: resolve => require(['../pages/CommentPage.vue'], resolve)
        }

      ]
    }

  ]
})

// 管理端此前没有任何导航守卫：直接输入 /Home 就能进入后台页面壳，
// 数据请求才因无令牌被后端拒绝 —— 结果是白屏而不是一句"请先登录"。
// 后端已有强制鉴权，这里补的是前端侧的引导，不是安全边界。
const PUBLIC_PATHS = ['/'];

router.beforeEach((to, from, next) => {
  if (PUBLIC_PATHS.indexOf(to.path) !== -1) {
    next();
    return;
  }
  if (localStorage.getItem('adminToken')) {
    next();
  } else {
    next('/');
  }
});

export default router;
