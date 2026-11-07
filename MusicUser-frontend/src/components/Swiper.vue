<!--
 * 滑动块
-->
<template>
    <div class="swiper">
        <el-carousel :interval="200000" type="card" height="280px">
            <el-carousel-item v-for="item in swiperList" :key="item.picImg">
                <img :src="item.picImg" />
            </el-carousel-item>
        </el-carousel>
    </div>
</template>

<script>
import {swiperList} from '../assets/data/swiper'
export default {
  name: 'swiper',
  data () {
    return {
      swiperList: []
    }
  },
  created () {
    this.swiperList = swiperList
  }
}
</script>

<style lang="scss" scoped>
@import '../assets/css/swiper.scss';
</style>
