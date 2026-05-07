<!--
 * 后台登录页
-->
<template>
    <div class="login-wrap">
        <div class="ms-login">
            <div class="ms-title">
              <i class="iconfont icon-login-title" style="font-size: 32px;"></i>
              <b style="font-size: 28px;color: #e75c09"> 音乐后台管理系统</b>
            </div>
            <el-form :model="ruleForm" :rules="rules" ref="ruleForm">
                <el-form-item prop="username">
                    <el-input
                        v-model="ruleForm.username"
                        placeholder="用户名"
                    ></el-input>
                </el-form-item>
                <el-form-item prop="password">
                    <el-input
                        type="password"
                        v-model="ruleForm.password"
                        placeholder="密码"
                        @keyup.enter.native="submitForm"
                    ></el-input>
                </el-form-item>
                <div class="login-btn">
                    <el-button type="success" size="small" @click="submitForm">
                      <i class="iconfont icon-login-button" style="font-size: 15px;"></i><b style="font-size: 20px;"> 登录</b>
                    </el-button>
                </div>
            </el-form>
        </div>
    </div>
</template>

<script>
import { mixin } from "../mixins/index";
import { getLoginStatus } from "../api/index";
export default {
    mixins: [mixin],
    data: function () {
        return {
            ruleForm: {
                username: "admin",
                password: "123456",
            },
            rules: {
                username: [
                    {
                        required: true,
                        message: "请输入用户名",
                        trigger: "blur",
                    },
                ],
                password: [
                    { required: true, message: "请输入密码", trigger: "blur" },
                ],
            },
        };
    },
    methods: {
        submitForm() {
            let params = new URLSearchParams();
            params.append("name", this.ruleForm.username);
            params.append("password", this.ruleForm.password);
            getLoginStatus(params)
                .then((res) => {
                    if (res.code == 20000 || res.success === true) {
                        // 保存Token和用户名
                        if (res.data && res.data.token) {
                            localStorage.setItem('adminToken', res.data.token);
                        }
                        localStorage.setItem(
                            "userName",
                            this.ruleForm.username
                        );
                        this.$router.push("/Info");
                        this.notify(res.message || "登录成功", "success");
                    } else {
                        this.notify(res.message || "登录失败", "error");
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
                            message: e.data.msg || e.data.message || "登录失败",
                            type: "error",
                            duration: 5000,
                        });
                    }
                });
        },
    },
};
</script>

<style scoped>
.login-wrap {
    position: relative;
    background: url("../assets/img/background.jpg");
    background-attachment: fixed;
    background-position: center;
    background-size: cover;
    width: 100%;
    height: 100%;
}
.ms-title {
    text-align: center;
    font-size: 22px;
    font-weight: 600;
    color: black;
    padding-bottom: 25px;
}
.ms-login {
    opacity: 0.9;
    position: absolute;
    left: 47%;
    top: 50%;
    width: 350px;
    height: 230px;
    margin-left: -190px;
    margin-top: -150px;
    padding: 40px;
    border-radius: 5px;
    background: white;
    border: 1px #ffffff solid;
}
.login-btn {
    text-align: center;
}
</style>
