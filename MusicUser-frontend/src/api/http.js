import axios from 'axios';
import router from '../router';

// 后端地址支持构建期覆盖，不再只能改源码；默认仍指向本地端口，保持现有开发方式
const API_BASE_URL = process.env.VUE_APP_API_BASE_URL || 'http://localhost:9171';

/**
 * 同一个来源派生出两个地址，部署时仍然只需要给 VUE_APP_API_BASE_URL 一个变量。
 *
 * HOST 是静态资源根目录：/song/{文件}、/img/**、/userImages/** 都挂在这里，不带 /api/v1。
 * 这些路径必须保持裸前缀——<audio> 与 <img> 发不出 Authorization 头，
 * 而且库里存的历史 URL 本身就是 /song/1715868205989 这种形态，加了前缀会集体 404。
 * API_HOST 是业务接口根目录，与后端 WebMvcConfig 的白名单口径一一对应。
 */
export const HOST = API_BASE_URL;
export const API_HOST = `${API_BASE_URL}/api/v1`;

// 推荐接口首次生成需要跑召回与统计，5 秒极易超时，放宽到 15 秒
axios.defaults.timeout = 15000;
// 令牌通过 Authorization 头传递，不需要携带 Cookie；
// 关闭 credentials 也避免与后端 CORS 白名单策略相互牵制
axios.defaults.withCredentials = false;
axios.defaults.baseURL = API_HOST;
// 说明：此前这里全局写死了
// headers.post['Content-Type'] = 'application/x-www-form-urlencoded'，
// 会让所有 POST 都按表单发送。后端 SingerController/ConsumerController 的
// add、update 使用 @RequestBody，需要 application/json，
// 于是请求在消息转换器阶段就被拒（415）或参数绑定不上。
// 现交还 axios：传对象自动用 JSON，传 URLSearchParams 自动用表单。

//请求拦截器 - 自动携带Token
axios.interceptors.request.use(
    config => {
        const token = localStorage.getItem('userToken');
        if (token) {
            config.headers.Authorization = token;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

//响应拦截器
axios.interceptors.response.use(
    // axios 已保证进入成功分支时 status 为 2xx，无需再手写 == 200 判断
    response => response,
    error => {
        // 超时与网络不可达时 error.response 为 undefined，
        // 原实现直接读 error.response.status 会让拦截器自身抛 TypeError，
        // 且异常不再 reject 出去，调用方的 catch 收不到任何提示
        const status = error.response && error.response.status;
        if (status === 401) {
            // 用户端登录页是 /login-in，'/' 是首页，跳首页等于没提示
            router.replace({
                path: '/login-in',
                query: {
                    redirect: router.currentRoute.fullPath
                }
            });
        }
        return Promise.reject(error.response || error);
    }
);

/**
 * 封装get方法
 */
export function get(url,params={}){
    return new Promise((resolve,reject) => {
        axios.get(url,{params:params})
        .then(response =>{
            resolve(response.data);
        })
        .catch(err =>{
            reject(err);
        })
    });
}

/**
 * 封装post方法
 */
export function post(url,data={}){
    return new Promise((resolve,reject) => {
        axios.post(url,data)
        .then(response =>{
            resolve(response.data);
        })
        .catch(err =>{
            reject(err);
        })
    });
}

/**
 * 封装delete方法，供 REST 语义改造使用
 */
export function del(url,params={}){
    return new Promise((resolve,reject) => {
        axios.delete(url,{params:params})
        .then(response =>{
            resolve(response.data);
        })
        .catch(err =>{
            reject(err);
        })
    });
}
