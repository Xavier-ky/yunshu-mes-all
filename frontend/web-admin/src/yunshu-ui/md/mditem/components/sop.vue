<template>
    <div class="app-container">
        <el-row v-if="optType != 'view'" :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button
                type="primary"
                plain
                icon="el-icon-plus"
                size="default"
                @click="handleAdd"
                v-hasPermi="['mes:md:mditem:add']"
                >新增</el-button>
            </el-col>
        </el-row>
        <div class="header">
            <el-dialog :title="title" v-model="open" width="960px" append-to-body>
                <el-form ref="form" :model="form" :rules="rules" label-width="100px">
                    <el-form-item label="标题" prop="sopTitle">
                        <el-input v-model="form.sopTitle" placeholder="请输入标题"></el-input>
                    </el-form-item>
                    <el-form-item label="展示顺序">
                        <el-input-number :min="1" v-model="form.orderNum"></el-input-number>
                    </el-form-item>
                    <el-form-item label="内容说明">
                        <el-input type="textarea" v-model="form.sopDescription" placeholder="请输入说明信息"></el-input>
                    </el-form-item>
                    <el-form-item label="所属工序" prop="processId">
                        <el-select v-model="form.processId" placeholder="请选择工序">
                            <el-option
                            v-for="item in processOptions"
                            :key="item.processId"
                            :label="item.processName"
                            :value="item.processId"
                            ></el-option>
                        </el-select>
                    </el-form-item>
                    <el-form-item label="图片">
                        <ImageUpload :limit="1" :value="form.sopUrl" :fileSize="5" @onUploaded="handleImgUplaoded" @onRemoved="handleImgRemoved" ></ImageUpload>
                    </el-form-item>                    
                </el-form>
                <template #footer><div class="dialog-footer">
                    <el-button type="primary" @click="submitForm">提 交</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div></template>
            </el-dialog>
        </div>
        <div class="images">
            <div v-for="(item,index) in sopList" :key="index" class="image-middle">
                <el-card shadow="hover" :body-style="{pading: '10px'}">
                    <el-popover>
                        <template #reference>
                        <img :src="sopList[index].sopUrl" class="image"/>
                        </template>
                        <el-image class="imagePreview" :src="sopList[index].sopUrl" :preview-src-list="imageList"></el-image>                        
                    </el-popover>
                    <div style="text-align:center;padding-top:12px">
                        <span>
                            {{sopList[index].sopTitle}}
                        </span>
                        <el-button @click="handleUpdate(sopList[index])" type="primary" icon="el-icon-edit"></el-button>
                        <el-button @click="handleDelete(sopList[index])" type="danger" icon="el-icon-delete"></el-button>
                    </div>
                </el-card>
            </div>
        </div>
    </div>
</template>
<script>
import { listSop, getSop, delSop, addSop, updateSop } from "@/yunshu-ui/api/mes/md/sop";
import ImageUpload from "@/yunshu-ui/components/ImageUpload/index.vue"
import {listAllProcess} from "@/yunshu-ui/api/mes/pro/process";
export default{
    name: "SOPTab",
    components: {ImageUpload},
    props:{
        itemId: null,
        optType: null,
    },
    data(){
        return {
            title: "新增SOP信息",
            loading: true,
            open: false,      
            // 表单参数
            form: {},
            // 表单校验
            rules: {
                itemId: [
                    { required: true, message: "物料产品ID不能为空", trigger: "blur" }
                ],
            },     
            // 产品SOP表格数据
            sopList: [], 
            //用于图片预览的清单
            imageList: [],
            //工序选项
            processOptions:[],
            queryParams: {
                itemId: this.itemId
            }            
        }
    },
    created(){
        this.getList();
        this.getProcess();
    },
    methods: {
        //获取当前产品的SOP资料清单
        getList(){
            listSop(this.queryParams).then(response =>{
                debugger;
                this.imageList = [];
                this.sopList = response.rows;
                this.sopList.forEach(row => {
                    this.imageList.push(row.sopUrl);
                });
            });
        },
         //查询工序信息
        getProcess(){
            listAllProcess().then( response =>{
                this.processOptions = response.data;
            });
        },
        // 取消按钮
        cancel() {
            this.open = false;
            this.reset();
        },
        // 表单重置
        reset() {
            this.form = {
                sopId: null,
                itemId: this.itemId,
                orderNum: null,
                processId: null,
                processCode: null,
                processName: null,
                sopTitle: null,
                sopDescription: null,
                sopUrl: null               
            };
            this.resetForm("form");
        },
        /**
         * 新增操作
         */
        handleAdd(){
            this.reset();
            this.open = true;
            this.title = "添加产品SOP";
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            const sopId = row.sopId || this.ids
            getSop(sopId).then(response => {
                this.form = response.data;
                this.open = true;
                this.title = "修改产品SOP";
            });
        },
        //图片上传成功
        handleImgUplaoded(imgUrl){
            console.log(imgUrl);
            this.form.sopUrl = imgUrl;
        },
        //图片移除
        handleImgRemoved(imgUrl){
            console.log(imgUrl);
            this.form.sopUrl = null;            
        },
        //提交
        submitForm(){
            this.$refs["form"].validate(valid => {
                    if (valid) {
                    if (this.form.sopId != null) {
                        updateSop(this.form).then(response => {
                        this.$modal.msgSuccess("修改成功");
                        this.open = false;
                        this.getList();
                        });
                    } else {
                        addSop(this.form).then(response => {
                        this.$modal.msgSuccess("新增成功");
                        this.open = false;
                        this.getList();
                        });
                    }
                    }
                });
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const sopIds = row.sopId || this.ids;
            this.$modal.confirm('是否确认删除产品SOP编号为"' + sopIds + '"的数据项？').then(function() {
                return delSop(sopIds);
            }).then(() => {
                this.getList();
                this.$modal.msgSuccess("删除成功");
            }).catch(() => {});
        },
    }
}
</script>
<style scoped>
    .header {
        margin-left: 15px;
        font-size: 30px;
        font-weight: 600;
    }

    .images {
        display: flex;
        margin-top: 20px;
        margin-left: 21px;
        margin-right: 20px;
        flex-wrap: wrap;
    }

    .image-middle{
        margin-right: 15px;
        margin-bottom: 15px;
    }

    .image{
        width:110px;
        height: 110px;
    }

    .imagePreview {
        width: 600px;
        height: 500px;
    }
</style>