<!--
 * 页面头部
-->
<template>
    <div class="header">
        <!-- 折叠图片 -->
        <div class="collapse-btn" @click="collapseChange">
            <i class="el-icon-menu"></i>
        </div>
        <div class="logo">音乐网后台管理系统</div>
        <div class="header-right">
            <div class="btn-fullscreen" @click="handleFullScreen">
                <el-tooltip :content="fullscreen?`取消全屏`:`全屏`" placement="bottom">
                    <i class="el-icon-rank"></i>
                </el-tooltip>
            </div>
            <div class="user-avator">
                <img src="../assets/img/user.jpg"/>
            </div>
            <el-dropdown class="user-name" trigger="click" @command="handleCommand">
                <span class="el-dropdown-link">
                    {{userName}}
                    <i class="el-icon-caret-bottom"></i>
                </span>
                <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
            </el-dropdown>
        </div>
    </div>
</template>

<script>
import bus from "../assets/js/bus";
import {adminLogout} from "../api/index";
export default {
    data(){
        return{
            collapse: false,
            fullscreen:false
        }
    },
    computed:{
        userName(){
            return localStorage.getItem('userName');
        }
    },
    methods:{
        //侧边栏折叠
        collapseChange(){
            this.collapse = !this.collapse;
            bus.$emit('collapse',this.collapse);
        },
        //全屏事件
        handleFullScreen(){
            if(this.fullscreen){
                if(document.exitFullscreen){
                   document.exitFullscreen();
                }else if(document.webkitCancelFullScreen){      //safari 、Chrome
                    document.webkitCancelFullScreen();
                }else if (document.mozCancelFullScreen){        //firefox
                    document.mozCancelFullScreen();
                }else if(document.msExitFullScreen){            //ie
                    document.msExitFullScreen();
                }
            }else{
                let element = document.documentElement;
                if(element.requestFullscreen){
                    element.requestFullscreen();
                }else if(element.webkitRequestFullScreen){      //safari 、Chrome
                    element.webkitRequestFullScreen();
                }else if(element.mozRequestFullScreen){         //firefox
                    element.mozRequestFullScreen();
                }else if (element.msRequestFullScreen){         //ie
                    element.msRequestFullScreen();
                }
            }
            this.fullscreen = !this.fullscreen;
        },
        handleCommand(command){
            if(command == "logout"){
                // 先让后端把当前令牌放进黑名单，再清本地。
                // 此前只删 localStorage：令牌在它自己的有效期内仍可调用，
                // "退出登录"实际只是把界面切回登录页。
                // 吊销失败也照样清理并跳转，不能因为后端不可达把用户
                // 困在已登录状态、连换账号都做不到。
                const cleanup = () => {
                    localStorage.removeItem('adminToken');
                    localStorage.removeItem('userName');
                    this.$router.push("/");
                };
                adminLogout()
                    .then(cleanup)
                    .catch(cleanup);
            }
        }
    }
}
</script>

<style scoped>
.header{
    position: relative;
    background-color: #3186cb;
    box-sizing: border-box;
    width: 100%;
    height: 70px;
    font-size: 22px;
    color: #ffffff;
}

.collapse-btn{
    float: left;
    padding: 0 21px;
    cursor: pointer;
    line-height: 70px;
}

.header .logo{
    float: left;
    line-height: 70px;
}

.header-right{
    float: right;
    padding-right: 50px;
    display:flex;
    height: 70px;
    align-items: center;
}

.btn-fullscreen{
    transform: rotate(45deg);
    margin-right: 5px;
    font-size: 24px;
}

.user-avator{
    margin-left: 20px;
}

.user-avator img{
    display: block;
    width: 63px;
    height: 63px;
    border-radius: 5px;
}

.user-name{
    margin-left: 10px;
    font-size: 22px;
}

.el-dropdown-link{
    color: #ffffff;
    cursor: pointer;
}
</style>
