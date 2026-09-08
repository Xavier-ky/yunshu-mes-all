<template>
  <div class="app-container designer-workbench">
    <el-alert
      title="可视化 UReport2 设计器需独立 Servlet 服务（Spring Boot 3 迁移中）。当前可在下方编辑报表 XML 并保存，预览请使用右侧按钮。"
      type="info"
      :closable="false"
      show-icon
      class="designer-alert"
    />

    <el-row :gutter="16" class="designer-main">
      <el-col :span="7">
        <el-card shadow="never" class="designer-panel">
          <template #header>
            <div class="panel-head">
              <span>报表文件</span>
              <el-button type="primary" link @click="handleAdd">新建</el-button>
            </div>
          </template>
          <el-table
            class="yunshu-data-table"
            stripe
            border
            v-loading="loading"
            :data="reportList"
            highlight-current-row
            @current-change="handleSelect"
            height="calc(100vh - 280px)"
          >
            <el-table-column label="报表名称" prop="name" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="80" align="center">
              <template #default="scope">
                <el-button type="danger" link @click.stop="handleDelete(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="17">
        <el-card shadow="never" class="designer-panel">
          <template #header>
            <div class="panel-head">
              <span>{{ currentReport.id ? '编辑报表' : '新建报表' }}</span>
              <div class="panel-actions">
                <el-button @click="handlePreview" :disabled="!currentReport.name">预览</el-button>
                <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
              </div>
            </div>
          </template>

          <el-form label-width="88px">
            <el-form-item label="报表名称">
              <el-input v-model="currentReport.name" placeholder="例如 demo_output_report.ureport.xml" />
            </el-form-item>
            <el-form-item label="报表 XML">
              <el-input
                v-model="currentReport.content"
                type="textarea"
                :rows="22"
                placeholder="粘贴或编辑 UReport XML 内容"
                class="designer-xml"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { listReport, getReport, addReport, updateReport, delReport } from '@/yunshu-ui/api/mes/report/ureport'

const DEFAULT_XML = `<?xml version="1.0" encoding="UTF-8"?>
<ureport>
  <cell expand="None" name="A1" row="1" col="1">
    <cell-style font-size="12" align="center" valign="middle"/>
    <simple-value><![CDATA[云枢智造 MES 演示报表]]></simple-value>
  </cell>
  <row row-number="1" height="18"/>
  <column col-number="1" width="80"/>
  <paper type="A4" left-margin="90" right-margin="90"
         top-margin="72" bottom-margin="72" paging-mode="fitpage" fixrows="0" width="595" height="842"/>
</ureport>`

export default {
  name: 'UreportDesignerWorkbench',
  data() {
    return {
      loading: false,
      saving: false,
      reportList: [],
      currentReport: {
        id: null,
        name: '',
        content: DEFAULT_XML
      }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      listReport({ pageNum: 1, pageSize: 100 }).then((res) => {
        this.reportList = res.rows || []
        if (this.reportList.length && !this.currentReport.id) {
          this.handleSelect(this.reportList[0])
        }
      }).finally(() => {
        this.loading = false
      })
    },
    handleSelect(row) {
      if (!row) {
        return
      }
      getReport(row.id).then((res) => {
        const data = res.data || {}
        this.currentReport = {
          id: data.id,
          name: data.name || '',
          content: data.content || DEFAULT_XML
        }
      })
    },
    handleAdd() {
      this.currentReport = {
        id: null,
        name: '',
        content: DEFAULT_XML
      }
    },
    handleSave() {
      if (!this.currentReport.name?.trim()) {
        this.$modal.msgWarning('请填写报表名称')
        return
      }
      this.saving = true
      const payload = {
        id: this.currentReport.id,
        name: this.currentReport.name.trim(),
        content: this.currentReport.content || DEFAULT_XML
      }
      const req = payload.id ? updateReport(payload) : addReport(payload)
      req.then(() => {
        this.$modal.msgSuccess('保存成功')
        this.loadList()
      }).finally(() => {
        this.saving = false
      })
    },
    handlePreview() {
      const name = encodeURIComponent(this.currentReport.name)
      window.open(`/ureport/preview?_u=mysql:${name}`, '_blank')
    },
    handleDelete(row) {
      this.$modal.confirm(`确认删除报表「${row.name}」？`).then(() => delReport(row.id)).then(() => {
        this.$modal.msgSuccess('删除成功')
        if (this.currentReport.id === row.id) {
          this.handleAdd()
        }
        this.loadList()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.designer-workbench {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: calc(100vh - 160px);
}

.designer-alert {
  margin-bottom: 0;
}

.designer-main {
  flex: 1;
}

.designer-panel {
  height: 100%;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.panel-actions {
  display: flex;
  gap: 8px;
}

.designer-xml :deep(textarea) {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
}
</style>
