<!--
 * 个人资料
-->
<template>
<div>
    <div class="info">
        <div class="title">
            <span>编辑个人资料</span>
        </div>
        <hr/>
        <div class="personal">
            <el-form :model="registerForm" ref="registerForm" label-width="70px" class="demo-ruleForm" :rules="rules">
                <el-form-item prop="username" label="用户名">
                    <el-input v-model="registerForm.username" placeholder="用户名"></el-input>
                </el-form-item>
                <el-form-item prop="password" label="密码">
                    <el-input type="password" v-model="registerForm.password" placeholder="密码"></el-input>
                </el-form-item>
                <el-form-item prop="sex" label="性别">
                    <el-radio-group v-model="registerForm.sex">
                        <el-radio :label="0">女</el-radio>
                        <el-radio :label="1">男</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item prop="phoneNum" label="手机">
                    <el-input v-model="registerForm.phoneNum" placeholder="手机"></el-input>
                </el-form-item>
                <el-form-item prop="email" label="邮箱">
                    <el-input v-model="registerForm.email" placeholder="邮箱"></el-input>
                </el-form-item>
                <el-form-item prop="birth" label="生日">
                    <el-date-picker type='date' :editable="false" v-model="registerForm.birth" placeholder="选择日期" style="width: 100%;"></el-date-picker>
                </el-form-item>
                <el-form-item prop="introduction" label="签名">
                    <el-input v-model="registerForm.introduction" placeholder="签名"></el-input>
                </el-form-item>
                <el-form-item prop="location" label="地区">
                    <el-select v-model="registerForm.location" placeholder="地区" style="width: 100%;">
                        <el-option v-for=" item in cities" :key="item.value" :label="item.label" :value="item.value"></el-option>
                    </el-select>
                </el-form-item>
            </el-form>
            <div class="btn">
                <div @click="saveMsg">保存</div>
                <div @click="goback(-1)">取消</div>
            </div>
        </div>

    </div>
</div>
</template>
<script>
import {mapGetters} from 'vuex'
import {rules,cities} from '../assets/data/form'
import {mixin} from '../mixins'
import {getMyProfile,updateUserMsg} from '../api/index'

export default {
    name: 'info',
    mixins: [mixin],
    data() {
        return {
            registerForm: {
                username: '',       //用户名
                password: '',       //密码
                sex: '',            //性别
                phoneNum: '',       //手机
                email: '',          //邮箱
                birth: '',          //生日
                introduction: '',   //签名
                location: ''       //地区
            },
            cities: [],            //所有的地区--省
            rules: {}               //表单提交的规则
        }
    },
    computed:{
        ...mapGetters([
            'userId'
        ])
    },
    created() {
        this.rules = rules;
        this.cities = cities;
    },
    mounted(){
        this.getMsg();
    },
    methods:{
        getMsg(){
            // 编辑页需要手机号、邮箱等私密字段，改用只返回本人资料的 /consumer/profile
            getMyProfile()
                .then(res =>{
                    // GET /consumers/profile 返回 R<Consumer>，载荷在 res.data。
                    // 此前直接取 res.username 这类信封上不存在的键，
                    // 资料表单打开时是空的——用户看不到现值，只能整张重填。
                    const me = res.data;
                    this.registerForm.username = me.username;
                    // 不回填 me.password：那是 BCrypt 哈希，
                    // 填进表单后再提交会把哈希当作用户新密码二次加密写库，
                    // 结果就是该账号再也登录不上。密码框保持为空表示"不修改密码"。
                    this.registerForm.sex = me.sex;
                    this.registerForm.phoneNum = me.phoneNum;
                    this.registerForm.email = me.email;
                    this.registerForm.birth = me.birth;
                    this.registerForm.introduction = me.introduction;
                    this.registerForm.location = me.location;

                })
                .catch(err => {
                    console.log(err);
                })
        },
        saveMsg(){
            let _this = this;
            let d = new Date(this.registerForm.birth);
            let datetime = d.getFullYear() + '-' +(d.getMonth() + 1) +'-' + d.getDate();
            // 后端 PATCH /consumers/profile 收 @RequestBody，此前这里发
            // URLSearchParams，请求在 Spring 侧就是 415。
            // id 与 username 都不再发送：身份只取令牌（@RequestAttribute userId），
            // 用户名是登录凭据，不允许通过改资料接口变更。
            let payload = {
                sex: this.registerForm.sex,
                phoneNum: this.registerForm.phoneNum,
                email: this.registerForm.email,
                birth: datetime,
                introduction: this.registerForm.introduction,
                location: this.registerForm.location
            };
            // 留空即不发送该字段：新的 ConsumerProfileRequest 对 password 只保留
            // @Size 约束、允许为 null，服务端据此保持原口令不变；
            // 发送空串会被 @Size(min=6) 直接拦下，也不会被当成"把密码改成空"。
            if (this.registerForm.password) {
                payload.password = this.registerForm.password;
            }
            updateUserMsg(payload)
                .then(res => {
                    if(res.code == 20000 || res.success === true){
                        _this.$store.commit('setUsername',this.registerForm.username);
                        _this.notify(res.message || '修改成功','success');
                        setTimeout(function(){
                            _this.$router.push({path: '/'});
                        },2000);
                    }else{
                        _this.notify(res.message || '修改失败','error');
                    }
                })
                .catch(err => {
                    _this.notify('修改失败','error');
                })
        },
        goback(index){
            this.$router.go(index);
        }
    }
}
</script>

<style lang="scss" scoped>
@import '../assets/css/info.scss';
</style>
