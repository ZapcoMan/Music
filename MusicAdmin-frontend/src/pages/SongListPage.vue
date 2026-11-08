<!--
 * 歌单列表页面
-->
<template>
    <div class="table">
        <div class="container">
            <div class="handle-box">
                <el-input v-model="select_word" placeholder="筛选关键字" class="handle-input"></el-input>
                <br><br>
                <el-button type="danger" size="small" style="font-size: 14px;" @click="delAll"><i class="iconfont icon-delete-button" style="font-size: 15px"></i> 批量删除</el-button>
                <el-button type="success" size="small" style="font-size: 14px;" @click="centerDialogVisible = true"><i class="iconfont icon-add-button" style="font-size: 15px"></i> 添加歌单</el-button>
            </div>
        </div>
        <el-table ref="multipleTable" border style="width:100%" height="680px" :data="tableData" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="40"></el-table-column>
            <el-table-column label="歌单图片" width="130" align="center">
                <template slot-scope="scope">
                    <div class="songList-img">
                        <img :src="getUrl(scope.row.pic)" style="width:100%"/>
                    </div>
                    <el-upload :action="uploadUrl(scope.row.id)" :headers="uploadHeaders()" :before-upload="beforeAvatorUpload"
                        :on-success="handleAvatorSuccess">
                        <el-button style="font-size: 14px;" size="small" type="success"><i class="iconfont icon-refresh-button" style="font-size: 15px"></i> 更换</el-button>
                    </el-upload>
                </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" width="120" align="center"></el-table-column>
            <el-table-column label="简介">
                <template slot-scope="scope">
                    <p style="height:100px;overflow:scroll">{{scope.row.introduction}}</p>
                </template>
            </el-table-column>
            <el-table-column prop="style" label="风格" width="120" align="center"></el-table-column>
            <el-table-column label="操作" width="520" align="center">
                <template slot-scope="scope">
                    <el-button style="font-size: 14px;" size="small" type="warning" @click="songEdit(scope.row.id)"><i class="iconfont " style="font-size: 15px"></i> 歌曲管理</el-button>
                    <el-button style="font-size: 14px;" size="small" @click="getComment(data[scope.$index].id)"><i class="iconfont icon-talk-button" style="font-size: 15px"></i> 评论</el-button>
                    <el-button style="font-size: 14px;" size="small" type="success" @click="handleEdit(scope.row)"><i class="iconfont icon-edit-button" style="font-size: 15px"></i> 编辑</el-button>
                    <el-button style="font-size: 14px;" size="small" type="danger" @click="handleDelete(scope.row.id)"><i class="iconfont icon-delete-button" style="font-size: 14px"></i> 删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <div class="pagination">
            <el-pagination
                background
                layout = "total, sizes, prev, pager, next, jumper"
                :current-page="currentPage"
                :page-size="pageSize"
                :total="total"
                :page-sizes="[5, 10, 20, 50]"
                @current-change="handleCurrentChange"
                @size-change="handleSizeChange"
                >
            </el-pagination>
        </div>
      icon-song-manage-button
        <el-dialog title="添加歌单" :visible.sync="centerDialogVisible" width="400px" center>
            <el-form :model="registerForm" ref="registerForm" label-width="80px">
                <el-form-item prop="title" label="标题" size="mini">
                    <el-input v-model="registerForm.title" placeholder="标题"></el-input>
                </el-form-item>
                <el-form-item prop="introduction" label="简介" size="mini">
                    <el-input v-model="registerForm.introduction" placeholder="简介" type="textarea"></el-input>
                </el-form-item>

                <el-form-item prop="style" label="风格" size="mini">
                    <el-select v-model="registerForm.style" placeholder="风格">
                        <el-option key="华语" label="华语" value="华语"></el-option>
                        <el-option key="粤语" label="粤语" value="粤语"></el-option>
                        <el-option key="欧美" label="欧美" value="欧美"></el-option>
                        <el-option key="日韩" label="日韩" value="日韩"></el-option>
                        <el-option key="轻音乐" label="轻音乐" value="轻音乐"></el-option>
                        <el-option key="BGM" label="BGM" value="BGM"></el-option>
                        <el-option key="乐器" label="乐器" value="乐器"></el-option>
                    </el-select>
                </el-form-item>
            </el-form>
            <span slot="footer">
                <el-button size="mini" @click="centerDialogVisible = false">取消</el-button>
                <el-button size="mini" @click="addSongList">确定</el-button>
            </span>
        </el-dialog>

        <el-dialog title="修改歌单" :visible.sync="editVisible" width="400px" center>
            <el-form :model="form" ref="form" label-width="80px">
                <el-form-item prop="title" label="标题" size="mini">
                    <el-input v-model="form.title" placeholder="标题"></el-input>
                </el-form-item>
                <el-form-item prop="introduction" label="简介" size="mini">
                    <el-input v-model="form.introduction" placeholder="简介" type="textarea"></el-input>
                </el-form-item>
                <el-form-item prop="style" label="风格" size="mini">
                    <el-select v-model="form.style" placeholder="风格">
                        <el-option key="华语" label="华语" value="华语"></el-option>
                        <el-option key="粤语" label="粤语" value="粤语"></el-option>
                        <el-option key="欧美" label="欧美" value="欧美"></el-option>
                        <el-option key="日韩" label="日韩" value="日韩"></el-option>
                        <el-option key="轻音乐" label="轻音乐" value="轻音乐"></el-option>
                        <el-option key="BGM" label="BGM" value="BGM"></el-option>
                        <el-option key="乐器" label="乐器" value="乐器"></el-option>
                    </el-select>
                </el-form-item>
            </el-form>
            <span slot="footer">
                <el-button size="mini" @click="editVisible = false">取消</el-button>
                <el-button size="mini" @click="editSave">确定</el-button>
            </span>
        </el-dialog>

        <el-dialog title="删除歌单" :visible.sync="delVisible" width="300px" center>
            <div align="center">删除不可恢复，是否确定删除？</div>
            <span slot="footer">
                <el-button size="mini" @click="delVisible = false">取消</el-button>
                <el-button size="mini" @click="deleteRow">确定</el-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
import {getSongListByPage,setSongList,updateSongList,delSongList} from '../api/index';
import { mixin } from '../mixins/index';
export default {
    mixins: [mixin],
    data(){
        return{
            centerDialogVisible: false, //添加弹窗是否显示
            editVisible: false,         //编辑弹窗是否显示
            delVisible: false,          //删除弹窗是否显示
            registerForm:{      //添加框
                title: '',
                introduction: '',
                style: ''
            },
            form:{      //编辑框
                id: '',
                title: '',
                introduction: '',
                style: ''
            },
            tableData: [],
            select_word: '',
            pageSize: 10,    //分页每页大小
            currentPage: 1,  //当前页
            total: 0,        //总记录数
            idx: -1,          //当前选择项
            multipleSelection: []   //哪些项已经打勾
        }
    },
    created(){
        this.getData();
    },
    methods:{
        //获取当前页
        handleCurrentChange(val){
            this.currentPage = val;
            this.getData();
        },
        //改变每页大小
        handleSizeChange(val){
            this.pageSize = val;
            this.currentPage = 1;
            this.getData();
        },
        //查询所有歌单（服务端分页）
        getData(){
            getSongListByPage(this.currentPage, this.pageSize).then(res => {
                if (res.code === 20000 || res.success === true) {
                    this.tableData = res.data.list;
                    this.total = res.data.total;
                } else {
                    this.notify(res.message || '获取歌单列表失败', 'error');
                }
            })
        },
        //添加歌单
        addSongList(){
            let params = new URLSearchParams();
            params.append('title',this.registerForm.title);
            params.append('pic','/img/songListPic/123.jpg');
            params.append('introduction',this.registerForm.introduction);
            params.append('style',this.registerForm.style);

            setSongList(params)
            .then(res => {
                if(res.code == 20000 || res.success === true){
                    this.getData();
                    this.notify(res.message || "添加成功","success");
                }else{
                    this.notify(res.message || "添加失败","error");
                }
            })
            .catch(err => {
                console.log(err);
            });
            this.centerDialogVisible = false;
        },
        //弹出编辑页面
        handleEdit(row){
            this.editVisible = true;
            this.form = {
                id: row.id,
                title: row.title,
                introduction: row.introduction,
                style: row.style
            }
        },
        //保存编辑页面修改的数据
        editSave(){
            let params = new URLSearchParams();
            params.append('id',this.form.id);
            params.append('title',this.form.title);
            params.append('introduction',this.form.introduction);
            params.append('style',this.form.style);

            updateSongList(params)
            .then(res => {
                if(res.code == 20000 || res.success === true){
                    this.getData();
                    this.notify(res.message || "修改成功","success");
                }else{
                    this.notify(res.message || "修改失败","error");
                }
            })
            .catch(err => {
                console.log(err);
            });
            this.editVisible = false;
        },
        //更新图片
        // el-upload 走原生 XHR，不经过 axios 请求拦截器，需自行携带 Token
        uploadHeaders() {
            return {
                Authorization: localStorage.getItem("adminToken") || "",
            };
        },
        uploadUrl(id){
            return `${this.$store.state.API_HOST}/song-lists/${id}/picture`
        },
        //删除一个歌单
        deleteRow(){
            delSongList(this.idx)
            .then(res => {
                if(res === true){
                    this.getData();
                    this.notify("删除成功","success");
                }else{
                    this.notify("删除失败","error");
                }
            })
            .catch(err => {
                console.log(err);
            });
            this.delVisible = false;
        },
        //转向歌曲管理页面
        songEdit(id,name){
            this.$router.push({path:`/ListSong`,query:{id}});
        },
        //转向该歌单的评论列表
        getComment(id){
            this.$router.push({path: '/comment',query:{id}})
        }
    }
}
</script>

<style scoped>
    .handle-box{
        margin-bottom: 20px;
    }
    .songList-img{
        width: 100%;
        height: 100px;
        border-radius: 5px;
        margin-bottom: 5px;
        overflow: hidden;
    }
    .handle-input{
        width: 300px;
        display: inline-block;
    }
    .pagination{
        display: flex;
        justify-content: center;
    }
</style>
