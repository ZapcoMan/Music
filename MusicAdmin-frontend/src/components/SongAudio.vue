<!--
 * 播放器
-->
<template>
    <div class="song-audio">
        <audio id="player"
            :src="url"
            controls = "controls"
            preload = "true"
            @canplay="startPlay"
            @ended="ended"
        ></audio>
    </div>
</template>
<script>
import {mapGetters} from 'vuex';
export default {
    name: 'SongAudio',
    computed: {
        ...mapGetters([
            'id',
            'url',
            'isPlay'
        ])
    },
    watch:{
        //监听播放还是暂停
        isPlay: function(){
            this.togglePlay();
        }
    },
    methods:{
        //获取链接后准备播放
        startPlay(){
            let player = document.querySelector('#player');
            //开始播放
            // play() 返回 Promise：没有用户手势时浏览器会拒绝，
            // 不接住就是控制台里的 Unhandled rejection，且状态仍停在"播放中"
            player.play().catch(() => {
                this.$store.commit('setIsPlay', false);
            });
        },
        //播放完成之后触发
        ended(){
           // isPlay 是 mapGetters 来的只读计算属性，`this.isPlay = false` 只会
           // 触发一条 readOnly 警告，什么也不会改；状态得走 mutation
           this.$store.commit('setIsPlay', false);
        },
        //开始、暂停
        togglePlay() {
            let player = document.querySelector('#player');
            if(this.isPlay){
                player.play().catch(() => {
                    this.$store.commit('setIsPlay', false);
                });
            }else{
                player.pause();
            }
        }
    }
}
</script>

<style>
    .song-audio {
        display: none;
    }
</style>
