export const mixin = {
    methods:{
        //提示信息
        notify(title,type){
            this.$notify({
                title: title,
                type: type
            })
        },
        //根据相对地址获取绝对地址
        getUrl(url){
            return `${this.$store.state.HOST}/${url}`
        },
        //获取性别中文
        changeSex(value){
            // 用严格比较：原先 value == 0 会把空串判成 0，
            // 于是性别未设置的歌手在列表里显示"女"
            if(value === 0){
                return '女';
            }
            if(value === 1){
                return '男';
            }
            if(value === 2){
                return '组合';
            }
            if(value === 3){
                return '不明';
            }
            // 未知编码返回空，不把原始数字直接渲染到表格上
            return '';
        },
        //获取生日
        attachBirth(val){
            // 数据为缺失时返回空串：String(null) 会得到 "null" 并显示在界面上，
            // 与本轮在数据侧清理的 "null" 字面量是同一类问题
            if(val === null || val === undefined || val === ''){
                return '';
            }
            return String(val).substr(0,10);
        },
        //上传图片之前的校验
        beforeAvatorUpload(file){
            const isJPG = (file.type === 'image/jpeg')||(file.type === 'image/png');
            if(!isJPG){
                this.$message.error('上传头像图片只能是jpg或png格式');
                return false;
            }
            const isLt2M = (file.size / 1024 /1024) < 2;
            if(!isLt2M){
                this.$message.error('上传头像图片大小不能超过2MB');
                return false;
            }
            return true;
        },
        //上传图片成功之后要做的工作
        handleAvatorSuccess(res){
            let _this = this;
            // 后端 ResultCodeEnum 成功码是 20000，这里的 1 是历史遗留判定，
            // 永远不成立，导致上传成功后既不刷新也不提示
            if(res.code == 20000 || res.success === true){
                _this.getData();
                _this.$notify({
                    title: '上传成功',
                    type: 'success'
                });
            }else{
                // 用后端给的文案：FileStorageService 会区分"文件超过大小上限 10MB"
                // "不支持的文件类型：exe""文件内容与扩展名不符"这几类原因，
                // 一律显示"上传失败"等于把可操作的提示压成一句废话
                _this.$notify({
                    title: res.message || '上传失败',
                    type: 'error'
                });
            }
        },
        //弹出删除窗口
        handleDelete(id){
            this.idx = id;
            this.delVisible = true;
        },
        //把已经选择的项赋值给multipleSelection
        handleSelectionChange(val){
            this.multipleSelection = val;
        },
        //批量删除已经选择的项
        delAll(){
            for(let item of this.multipleSelection){
                this.handleDelete(item.id);
                this.deleteRow();
            }
            this.multipleSelection = [];
        }
    }
}