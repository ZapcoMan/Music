/*
 * index
 */
import Vue from 'vue'
import Vuex from 'vuex'
// 基础地址只有 api/http.js 一个出处：此前这里写死 http://127.0.0.1:9171，
// 与 axios 的 baseURL 各走一套，换环境时要改两处且极易漏
import { HOST, API_HOST } from '../api/http'
Vue.use(Vuex)

const store = new Vuex.Store({
    state:{
        // HOST 给静态资源（/song、/img、/userImages），API_HOST 给绕过 axios 的接口调用
        HOST,
        API_HOST,
        isPlay: false,          //是否播放中
        url: '',                //歌曲地址
        id: ''                  //歌曲id
    },
    getters: {
        isPlay: state => state.isPlay,
        url: state => state.url,
        id: state => state.id
    },
    mutations: {
        setIsPlay: (state,isPlay) => {state.isPlay = isPlay},
        setUrl: (state,url) => {state.url = url},
        setId: (state,id) => {state.id = id}
    }
})

export default store
