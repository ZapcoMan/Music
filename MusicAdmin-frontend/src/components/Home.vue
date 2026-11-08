<!--
 * 主页
-->
<template>
    <div>
        <the-header></the-header>
        <the-aside></the-aside>
        <div class="content-box" :class="{'content-collapse':collapse}">
            <router-view></router-view>
        </div>
    </div>
</template>

<script>
import TheHeader from './TheHeader';
import TheAside from './TheAside';
import bus from "../assets/js/bus";

export default {
    components:{
        TheHeader,
        TheAside
    },
    data(){
        return {
            collapse: false
        }
    },
    created(){
        //通过Bus进行组件间的通信，来折叠侧边栏
        bus.$on('collapse', this.handleCollapse)
    },
    beforeDestroy(){
        // bus 是全局单例，不在这里摘掉的话，每次重新挂载布局都会多留一个
        // 指向已销毁组件的监听器：事件一发出，旧实例的 this 也被跟着复活
        bus.$off('collapse', this.handleCollapse)
    },
    methods: {
        handleCollapse(msg) {
            this.collapse = msg
        }
    }
}
</script>
