<!--
 *  首页统计页面
-->
<template>
    <div style="padding: 20px;">
        <el-row :gutter="20" class="mgb20">
            <el-col :span="6">
                <el-card>
                    <div class="grid-content">
                        <div class="grid-cont-center" style="color: #DC143C;">
                            <div class="grid-num">{{consumerCount}}</div>
                            <div><i class="iconfont icon-r-team" style="font-size: 25px;"> 用户总数</i></div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card>
                    <div class="grid-content">
                        <div class="grid-cont-center" style="color: #6495ED;">
                            <div class="grid-num">{{songCount}}</div>
                            <div>
                                <i class="iconfont icon-r-paper" style="font-size: 25px;"> 歌曲总数</i></div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card>
                    <div class="grid-content">
                        <div class="grid-cont-center" style="color: #2E8B57;">
                            <div class="grid-num">{{singerCount}}</div>
                            <div>
                                <i class="iconfont icon-r-love" style="font-size: 25px;"> 歌手数量</i>
                            </div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card>
                    <div class="grid-content">
                        <div class="grid-cont-center" style="color: #DAA520;">
                            <div class="grid-num">{{songListCount}}</div>
                            <div>
                                <i class="iconfont icon-r-list" style="font-size: 25px;"> 歌单数量</i>
                            </div>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>
        <el-row :gutter="20" class="mgb20">
            <el-col :span="12">
                <h3 class="mgb20"><i class="iconfont icon-r-find" style="font-size: 28px;"></i> 用户性别比例</h3>
                <div style="background-color:white">
                    <ve-pie :data="consumerSex" :theme="options"></ve-pie>
                </div>
            </el-col>
            <el-col :span="12">
                <h3 class="mgb20"><i class="iconfont icon-r-find" style="font-size: 28px;"></i> 歌曲类型分布</h3>
                <div style="background-color:white">
                    <ve-histogram :data="songStyle"></ve-histogram>
                </div>
            </el-col>
        </el-row>
        <el-row :gutter="20" class="mgb20">
            <el-col :span="12">
                <h3 class="mgb20"><i class="iconfont icon-r-find" style="font-size: 28px;"></i> 歌手性别比例</h3>
                <div style="background-color:white">
                    <ve-pie :data="singerSex"></ve-pie>
                </div>
            </el-col>
            <el-col :span="12">
                <h3 class="mgb20"><i class="iconfont icon-r-find" style="font-size: 28px;"></i> 歌手国籍分布</h3>
                <div style="background-color:white">
                    <ve-histogram :data="country" :theme="options1"></ve-histogram>
                </div>
            </el-col>
        </el-row>
    </div>
</template>
<script>
import {getStatisticsOverview} from '../api/index';
export default {
    data(){
        return {
            consumerCount: 0,       //用户总数
            songCount: 0,           //歌曲总数
            singerCount: 0,         //歌手数量
            songListCount: 0,        //歌单数量
            consumerSex:{           //按性别分类的用户数
                columns: ['性别','总数'],
                rows: [
                    {'性别': '男','总数': 0},
                    {'性别': '女','总数': 0}
                ]
            },
            options: {
                color: ['#87cefa','#ffc0cb']
            },
            options1: {
                color: ['yellow']
            },
            songStyle:{           //按歌单风格分类
                columns: ['风格','总数'],
                rows: [
                    {'风格': '华语','总数': 0},
                    {'风格': '粤语','总数': 0},
                    {'风格': '欧美','总数': 0},
                    {'风格': '日韩','总数': 0},
                    {'风格': 'BGM','总数': 0},
                    {'风格': '轻音乐','总数': 0},
                    {'风格': '乐器','总数': 0}
                ]
            },
            singerSex:{           //按性别分类的歌手数
                columns: ['性别','总数'],
                rows: [
                    {'性别': '女','总数': 0},
                    {'性别': '男','总数': 0},
                    {'性别': '组合','总数': 0},
                    {'性别': '不明','总数': 0}
                ]
            },
            country:{
                columns: ['国籍','总数'],
                rows: [
                    {'国籍': '中国','总数': 0},
                    {'国籍': '韩国','总数': 0},
                    {'国籍': '日本','总数': 0},
                    {'国籍': '美国','总数': 0},
                    {'国籍': '新加坡','总数': 0},
                    {'国籍': '意大利','总数': 0},
                    {'国籍': '马来西亚','总数': 0},
                    {'国籍': '西班牙','总数': 0}
                ]
            }
        }
    },
    mounted() {
        this.getOverview();
    },
    methods: {
        /**
         * 四个总数与三张分布图都取自后端的 count/group by。
         *
         * 此前这里分四个请求拉 consumers / songs / singers / song-lists 的不分页版本，
         * 只为在前端数一下长度——全部用户的手机号、邮箱因此整表出现在网络响应与浏览器内存里，
         * 首屏还要等四次往返。现在出参只有几十个数字。
         */
        getOverview() {
            getStatisticsOverview().then(res => {
                const d = res.data || {};
                this.consumerCount = d.consumerTotal || 0;
                this.songCount = d.songTotal || 0;
                this.singerCount = d.singerTotal || 0;
                this.songListCount = d.songListTotal || 0;
                // 性别码到中文标签的映射属于图表口径，留在页面里而不是写进 SQL
                this.consumerSex.rows[0]['总数'] = this.countOf(d.consumerSex, '1');   //男
                this.consumerSex.rows[1]['总数'] = this.countOf(d.consumerSex, '0');   //女
                this.singerSex.rows[0]['总数'] = this.countOf(d.singerSex, '0');       //女
                this.singerSex.rows[1]['总数'] = this.countOf(d.singerSex, '1');       //男
                this.singerSex.rows[2]['总数'] = this.countOf(d.singerSex, '2');       //组合
                this.singerSex.rows[3]['总数'] = this.countOf(d.singerSex, '3');       //不明
                // 地区与风格在库里是"中国台湾""华语,流行"这类组合串，
                // 同一个值可以落进多个桶——与改造前的 includes 口径一致
                this.country.rows.forEach(row => {
                    row['总数'] = this.containsCount(d.singerLocation, row['国籍']);
                });
                this.songStyle.rows.forEach(row => {
                    row['总数'] = this.containsCount(d.songListStyle, row['风格']);
                });
            }).catch(err => {
                // 统计失败必须可见：原先四个请求都没有 catch，接口一挂页面就显示四个 0，
                // 看上去像"库里真的没有数据"
                this.$message({
                    type: 'error',
                    message: (err && err.userMessage) || '统计数据加载失败',
                });
            });
        },
        countOf(groups, label) {              //按分组原值取该标签的行数
            let count = 0;
            (groups || []).forEach(item => {
                if (String(item.label) === label) {
                    count += item.total || 0;
                }
            });
            return count;
        },
        containsCount(groups, keyword) {      //把分组原值按"包含"归进图表桶
            let count = 0;
            (groups || []).forEach(item => {
                // 空值后端已过滤，这里再兜一层：库里历史脏数据不该让整个统计抛错
                if (item.label && item.label.indexOf(keyword) > -1) {
                    count += item.total || 0;
                }
            });
            return count;
        }
    }
}

</script>

<style scoped>
.grid-content {
    display: flex;
    align-items: center;
    height: 100px;
}

.grid-cont-center {
    flex: 1;
    text-align: center;
    font-size: 14px;
    color: darkgray;
}

.grid-num {
    font-size: 42px;
    font-weight: bold;
}
</style>
