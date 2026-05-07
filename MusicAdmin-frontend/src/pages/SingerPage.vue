<!--
 * 歌手管理页面
-->
<template>
    <div class="table">
        <div class="container">
            <div class="handle-box">
                <el-input
                    v-model="select_word"
                    placeholder="请输入歌手名"
                    class="handle-input"
                ></el-input>
                <br /><br />
                <el-button type="danger" size="small" style="font-size: 14px" @click="delAll"
                    ><i
                        class="iconfont icon-delete-button"
                        style="font-size: 15px"
                    ></i>
                    批量删除</el-button
                >
                <el-button
                    type="success"
                    size="small"
                    style="font-size: 14px"
                    @click="centerDialogVisible = true"
                    ><i class="iconfont icon-add-button" style="font-size: 15px"></i>
                    添加歌手</el-button
                >
            </div>
        </div>
        <el-table
            ref="multipleTable"
            border
            style="width: 100%"
            height="680px"
            :data="tableData"
            @selection-change="handleSelectionChange"
        >
            <el-table-column type="selection" width="40"></el-table-column>
            <el-table-column label="歌手图片" width="140" align="center">
                <template slot-scope="scope">
                    <div class="singer-img">
                        <img :src="getUrl(scope.row.pic)" style="width: 100%" />
                    </div>
                    <el-upload
                        :action="uploadUrl(scope.row.id)"
                        :before-upload="beforeAvatorUpload"
                        :on-success="handleAvatorSuccess"
                    >
                        <el-button type="success" style="font-size: 14px;" size="small"
                            ><i
                                class="iconfont icon-refresh-button"
                                style="font-size: 15px"
                            ></i>
                             更换</el-button
                        >
                    </el-upload>
                </template>
            </el-table-column>
            <el-table-column
                prop="name"
                label="歌手"
                width="120"
                align="center"
            ></el-table-column>
            <el-table-column label="性别" width="50" align="center">
                <template slot-scope="scope">
                    {{ changeSex(scope.row.sex) }}
                </template>
            </el-table-column>
            <el-table-column label="生日" width="120" align="center">
                <template slot-scope="scope">
                    {{ attachBirth(scope.row.birth) }}
                </template>
            </el-table-column>
            <el-table-column
                prop="location"
                label="地区"
                width="100"
                align="center"
            ></el-table-column>
            <el-table-column label="简介">
                <template slot-scope="scope">
                    <p style="height: 100px; overflow: scroll">
                        {{ scope.row.introduction }}
                    </p>
                </template>
            </el-table-column>
            <el-table-column
                label="操作"
                width="420"
                fixed="right"
                align="center"
            >
                <template slot-scope="scope">
                    <el-button
                        style="font-size: 14px"
                        size="small"
                        type="warning"
                        @click="songEdit(scope.row.id, scope.row.name)"
                        ><i
                            class="iconfont icon-song-manage-button"
                            style="font-size: 15px"
                        ></i>
                        歌曲管理</el-button
                    >
                    <el-button
                        style="font-size: 14px"
                        size="small"
                        type="success"
                        @click="handleEdit(scope.row)"
                        ><i
                            class="iconfont icon-edit-button"
                            style="font-size: 15px"
                        ></i>
                        编辑</el-button
                    >
                    <el-button
                        style="font-size: 14px"
                        size="small"
                        type="danger"
                        @click="handleDelete(scope.row.id)"
                        ><i
                            class="iconfont icon-delete-button"
                            style="font-size: 15px"
                        ></i>
                        删除</el-button
                    >
                </template>
            </el-table-column>
        </el-table>
        <div class="pagination">
            <el-pagination
                background
                layout="total, sizes, prev, pager, next, jumper"
                :current-page="currentPage"
                :page-size="pageSize"
                :total="total"
                :page-sizes="[5, 10, 20, 50]"
                @current-change="handleCurrentChange"
                @size-change="handleSizeChange"
            >
            </el-pagination>
        </div>

        <el-dialog
            title="添加歌手"
            :visible.sync="centerDialogVisible"
            width="400px"
            center
        >
            <el-form
                :model="registerForm"
                ref="registerForm"
                label-width="80px"
            >
                <el-form-item prop="name" label="歌手名" size="mini">
                    <el-input
                        v-model="registerForm.name"
                        placeholder="歌手名"
                    ></el-input>
                </el-form-item>
                <el-form-item label="性别" size="mini">
                    <el-radio-group v-model="registerForm.sex">
                        <el-radio :label="0">女</el-radio>
                        <el-radio :label="1">男</el-radio>
                        <el-radio :label="2">组合</el-radio>
                        <el-radio :label="3">不明</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item prop="birth" label="生日" size="mini">
                    <el-date-picker
                        type="date"
                        placeholder="选择日期"
                        v-model="registerForm.birth"
                        style="width: 100%"
                    ></el-date-picker>
                </el-form-item>
                <el-form-item prop="location" label="地区" size="mini">
                    <el-input
                        v-model="registerForm.location"
                        placeholder="地区"
                    ></el-input>
                </el-form-item>
                <el-form-item prop="introduction" label="简介" size="mini">
                    <el-input
                        v-model="registerForm.introduction"
                        placeholder="简介"
                        type="textarea"
                    ></el-input>
                </el-form-item>
            </el-form>
            <span slot="footer">
                <el-button size="mini" @click="centerDialogVisible = false"
                    >取消</el-button
                >
                <el-button size="mini" @click="addSinger">确定</el-button>
            </span>
        </el-dialog>

        <el-dialog
            title="修改歌手"
            :visible.sync="editVisible"
            width="400px"
            center
        >
            <el-form :model="form" ref="form" label-width="80px">
                <el-form-item prop="name" label="歌手名" size="mini">
                    <el-input
                        v-model="form.name"
                        placeholder="歌手名"
                    ></el-input>
                </el-form-item>
                <el-form-item label="性别" size="mini">
                    <el-radio-group v-model="form.sex">
                        <el-radio :label="0">女</el-radio>
                        <el-radio :label="1">男</el-radio>
                        <el-radio :label="2">组合</el-radio>
                        <el-radio :label="3">不明</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="生日" size="mini">
                    <el-date-picker
                        type="date"
                        placeholder="选择日期"
                        v-model="form.birth"
                        style="width: 100%"
                    ></el-date-picker>
                </el-form-item>
                <el-form-item prop="location" label="地区" size="mini">
                    <el-input
                        v-model="form.location"
                        placeholder="地区"
                    ></el-input>
                </el-form-item>
                <el-form-item prop="introduction" label="简介" size="mini">
                    <el-input
                        v-model="form.introduction"
                        placeholder="简介"
                        type="textarea"
                    ></el-input>
                </el-form-item>
            </el-form>
            <span slot="footer">
                <el-button size="mini" @click="editVisible = false"
                    >取消</el-button
                >
                <el-button size="mini" @click="editSave">确定</el-button>
            </span>
        </el-dialog>

        <el-dialog
            title="删除歌手"
            :visible.sync="delVisible"
            width="300px"
            center
        >
            <div align="center">删除不可恢复，是否确定删除？</div>
            <span slot="footer">
                <el-button size="mini" @click="delVisible = false"
                    >取消</el-button
                >
                <el-button size="mini" @click="deleteRow">确定</el-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
import { setSinger, getSingerByPage, updateSinger, delSinger } from "../api/index";
import { mixin } from "../mixins/index";
export default {
    mixins: [mixin],
    data() {
        return {
            centerDialogVisible: false, //添加弹窗是否显示
            editVisible: false, //编辑弹窗是否显示
            delVisible: false, //删除弹窗是否显示
            registerForm: {
                //添加框
                name: "",
                sex: "",
                birth: "",
                location: "",
                introduction: "",
            },
            form: {
                //编辑框
                id: "",
                name: "",
                sex: "",
                birth: "",
                location: "",
                introduction: "",
            },
            tableData: [],
            select_word: "",
            pageSize: 10, //分页每页大小
            currentPage: 1, //当前页
            total: 0, //总记录数
            idx: -1, //当前选择项
            multipleSelection: [], //哪些项已经打勾
        };
    },
    created() {
        this.getData();
    },
    methods: {
        //获取当前页
        handleCurrentChange(val) {
            this.currentPage = val;
            this.getData();
        },
        //改变每页大小
        handleSizeChange(val) {
            this.pageSize = val;
            this.currentPage = 1;
            this.getData();
        },
        //查询所有歌手（服务端分页）
        getData() {
            getSingerByPage(this.currentPage, this.pageSize).then((res) => {
                if (res.code === 20000 || res.success === true) {
                    this.tableData = res.data.list;
                    this.total = res.data.total;
                } else {
                    this.notify(res.message || '获取歌手列表失败', 'error');
                }
            });
        },
        //添加歌手
        addSinger() {
            let d = this.registerForm.birth;
            let datetime =
                d.getFullYear() + "-" + (d.getMonth() + 1) + "-" + d.getDate();
            let params = new URLSearchParams();
            params.append("name", this.registerForm.name);
            params.append("sex", this.registerForm.sex);
            params.append("pic", "/img/singerPic/hhh.jpg");
            params.append("birth", datetime);
            params.append("location", this.registerForm.location);
            params.append("introduction", this.registerForm.introduction);

            setSinger(params)
                .then((res) => {
                    if (res.code == 20000 || res.success === true) {
                        this.getData();
                        this.notify(res.message || "添加成功", "success");
                    } else {
                        this.notify(res.message || "添加失败", "error");
                    }
                })
                .catch((err) => {
                    console.log(err);
                });
            this.centerDialogVisible = false;
        },
        //弹出编辑页面
        handleEdit(row) {
            this.editVisible = true;
            this.form = {
                id: row.id,
                name: row.name,
                sex: row.sex,
                birth: row.birth,
                location: row.location,
                introduction: row.introduction,
            };
        },
        //保存编辑页面修改的数据
        editSave() {
            let d = new Date(this.form.birth);
            let datetime =
                d.getFullYear() + "-" + (d.getMonth() + 1) + "-" + d.getDate();
            let params = new URLSearchParams();
            params.append("id", this.form.id);
            params.append("name", this.form.name);
            params.append("sex", this.form.sex);
            params.append("birth", datetime);
            params.append("location", this.form.location);
            params.append("introduction", this.form.introduction);

            updateSinger(params)
                .then((res) => {
                    if (res.code == 20000 || res.success === true) {
                        this.getData();
                        this.notify(res.message || "修改成功", "success");
                    } else {
                        this.notify(res.message || "修改失败", "error");
                    }
                })
                .catch((err) => {
                    console.log(err);
                });
            this.editVisible = false;
        },
        //更新图片
        uploadUrl(id) {
            return `${this.$store.state.HOST}/singer/updateSingerPic?id=${id}`;
        },
        //删除一名歌手
        deleteRow() {
            delSinger(this.idx)
                .then((res) => {
                    if (res === true) {
                        this.getData();
                        this.notify("删除成功", "success");
                    } else {
                        this.notify("删除失败", "error");
                    }
                })
                .catch((err) => {
                    console.log(err);
                });
            this.delVisible = false;
        },
        //转向歌曲管理页面
        songEdit(id, name) {
            this.$router.push({ path: `/Song`, query: { id, name } });
        },
    },
};
</script>

<style scoped>
.handle-box {
    margin-bottom: 20px;
}
.singer-img {
    width: 100%;
    height: 120px;
    border-radius: 5px;
    margin-bottom: 5px;
    overflow: hidden;
}
.handle-input {
    width: 300px;
    display: inline-block;
}
.pagination {
    display: flex;
    justify-content: center;
}
</style>
