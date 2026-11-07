import axios from 'axios';
import router from '../router';

// 后端地址支持构建期覆盖；默认本机端口，保持现有开发方式
const API_BASE_URL = process.env.VUE_APP_API_BASE_URL || 'http://localhost:9171';

/**
 * 与用户端同一套：一个来源派生两个地址，部署只需要 VUE_APP_API_BASE_URL。
 * HOST 给静态资源（/song、/img、/userImages），它们必须留在 /api/v1 之外——
 * <audio>/<img> 带不了 Authorization 头，历史 URL 也按现有前缀存着。
 * API_HOST 给业务接口，与后端 WebMvcConfig 的白名单口径对应。
 */
export const HOST = API_BASE_URL;
export const API_HOST = `${API_BASE_URL}/api/v1`;

axios.defaults.timeout = 15000;
axios.defaults.withCredentials = false;
axios.defaults.baseURL = API_HOST;
// 与用户端同步：不再全局写死 Content-Type 为 x-www-form-urlencoded，
// 否则后端 @RequestBody 接口（singer/consumer 的 add、update）无法接收 JSON

//请求拦截器 - 自动携带Token
axios.interceptors.request.use(
    config => {
        const token = localStorage.getItem('adminToken');
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
    response => response,
    error => {
        // 超时/断网时 error.response 为 undefined，不能直接取其 status
        const status = error.response && error.response.status;
        if (status === 401) {
            router.replace({
                path: '/',
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

/**
 * 封装patch方法：部分更新（改资料）用它，与后端 @PatchMapping 对应；
 * 后端 CORS 已把 PATCH 列入允许方法，否则浏览器预检会先失败
 */
export function patch(url,data={}){
    return new Promise((resolve,reject) => {
        axios.patch(url,data)
        .then(response =>{
            resolve(response.data);
        })
        .catch(err =>{
            reject(err);
        })
    });
}
