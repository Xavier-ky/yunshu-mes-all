<template>
  <div class="app-container wmstock-command-center wmstock-hub">
    <el-row :gutter="16" class="wmstock-hub-row">
      <el-col :span="5" :xs="24" class="wmstock-hub-aside">
        <div class="hub-mode-switch">
          <el-radio-group v-model="hubMode" size="small" @change="onHubModeChange">
            <el-radio-button label="stock">现有量</el-radio-button>
            <el-radio-button label="warehouse">仓库</el-radio-button>
            <el-radio-button label="batch">批次</el-radio-button>
          </el-radio-group>
        </div>

        <template v-if="hubMode === 'stock'">
          <div class="stock-aside-summary">
            <div class="aside-summary-head">
              <span>库存总览</span>
              <button class="aside-reset" type="button" @click="resetScope">全部库存</button>
            </div>
            <div class="aside-stat-grid">
              <div><b>{{ formatQty(overview.summary.skuCount) }}</b><span>库存品种</span></div>
              <div><b>{{ formatQty(overview.summary.quantityOnhand) }}</b><span>在库总量</span></div>
              <div><b>{{ formatQty(overview.summary.frozenCount) }}</b><span>冻结记录</span></div>
              <div><b>{{ formatQty(overview.summary.expiringCount) }}</b><span>临期批次</span></div>
            </div>
          </div>

          <div class="stock-aside-section">
            <div class="aside-section-title"><span>库存范围</span><small>按类别快速定位</small></div>
            <div class="stock-category-chips">
              <button
                v-for="category in quickCategories"
                :key="category.id"
                type="button"
                :class="{ active: String(queryParams.itemTypeId) === String(category.id) }"
                @click="applyCategory(category.id)"
              >
                <span>{{ category.label }}</span>
                <b>{{ categoryCount(category.id) }}</b>
              </button>
            </div>
          </div>

          <div class="stock-aside-section stock-aside-tree stock-aside-tree--fill">
            <div class="aside-section-title"><span>物料分类</span><small>点击展开二级</small></div>
            <el-input
              v-model="itemTypeName"
              placeholder="搜索分类"
              clearable
              size="small"
              prefix-icon="el-icon-search"
            />
            <div class="hub-tree-wrap">
              <el-tree
                :data="itemTypeOptions"
                :props="defaultProps"
                :expand-on-click-node="false"
                :filter-node-method="filterNode"
                :default-expand-all="false"
                ref="tree"
                @node-click="handleNodeClick"
              />
            </div>
          </div>

          <div class="stock-aside-section stock-aside-warehouse">
            <div class="aside-section-title"><span>仓库分布</span><small>点击筛选</small></div>
            <button
              v-for="warehouse in overview.warehouses"
              :key="warehouse.warehouseId"
              class="warehouse-scope"
              :class="{ active: String(queryParams.warehouseId) === String(warehouse.warehouseId) }"
              type="button"
              @click="applyWarehouse(warehouse.warehouseId)"
            >
              <span>{{ warehouse.warehouseName || '未分配仓库' }}</span>
              <b>{{ formatQty(warehouse.quantityOnhand) }}</b>
            </button>
          </div>

          <div class="stock-aside-section stock-status-filter">
            <div class="aside-section-title"><span>库存状态</span><small>状态筛选</small></div>
            <div class="status-filter-buttons">
              <button type="button" :class="{ active: !queryParams.frozenFlag && !queryParams.nearExpiry }" @click="applyStatus('all')">全部</button>
              <button type="button" :class="{ active: queryParams.frozenFlag === 'N' }" @click="applyStatus('normal')">正常</button>
              <button type="button" :class="{ active: queryParams.frozenFlag === 'Y' }" @click="applyStatus('frozen')">冻结</button>
              <button type="button" :class="{ active: queryParams.nearExpiry === 'Y' }" @click="applyStatus('expiry')">临期</button>
            </div>
          </div>
        </template>

        <template v-else-if="hubMode === 'warehouse'">
          <div class="stock-aside-summary">
            <div class="aside-summary-head">
              <span>仓储总览</span>
              <button class="aside-reset" type="button" @click="resetWarehouseScope">全部仓库</button>
            </div>
            <div class="aside-stat-grid">
              <div><b>{{ formatQty(warehouseSummary.warehouseCount) }}</b><span>仓库</span></div>
              <div><b>{{ formatQty(warehouseSummary.locationCount) }}</b><span>库区</span></div>
              <div><b>{{ formatQty(warehouseSummary.areaCount) }}</b><span>库位</span></div>
              <div><b>{{ formatQty(warehouseSummary.enabledCount) }}</b><span>启用</span></div>
            </div>
          </div>
          <div class="stock-aside-section">
            <div class="aside-section-title"><span>层级快捷</span><small>切换右侧表</small></div>
            <div class="status-filter-buttons">
              <button type="button" :class="{ active: whPanel === 'warehouse' }" @click="setWhPanel('warehouse')">全部仓库</button>
              <button type="button" :class="{ active: whPanel === 'location' }" @click="setWhPanel('location')">仅看库区</button>
              <button type="button" :class="{ active: whPanel === 'area' }" @click="setWhPanel('area')">仅看库位</button>
            </div>
          </div>
          <div class="stock-aside-section stock-aside-tree stock-aside-tree--fill">
            <div class="aside-section-title"><span>层级导航</span><small>仓库→库区→库位</small></div>
            <div class="hub-tree-wrap">
              <el-tree
                :data="warehouseTree"
                :props="warehouseTreeProps"
                :expand-on-click-node="false"
                :default-expand-all="false"
                highlight-current
                @node-click="handleWarehouseTreeClick"
              />
            </div>
          </div>
          <div class="stock-aside-section stock-status-filter">
            <div class="aside-section-title"><span>启用状态</span><small>快速筛选</small></div>
            <div class="status-filter-buttons">
              <button type="button" :class="{ active: !whQuery.enableFlag }" @click="applyWhEnable(null)">全部</button>
              <button type="button" :class="{ active: whQuery.enableFlag === 'Y' }" @click="applyWhEnable('Y')">启用</button>
              <button type="button" :class="{ active: whQuery.enableFlag === 'N' }" @click="applyWhEnable('N')">停用</button>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="stock-aside-summary">
            <div class="aside-summary-head">
              <span>批次总览</span>
              <button class="aside-reset" type="button" @click="resetBatchScope">全部批次</button>
            </div>
            <div class="aside-stat-grid">
              <div><b>{{ formatQty(batchSummary.batchCount) }}</b><span>批次数</span></div>
              <div><b>{{ formatQty(batchSummary.nearExpiry) }}</b><span>临期</span></div>
              <div><b>{{ formatQty(batchSummary.expired) }}</b><span>已过期</span></div>
              <div><b>{{ formatQty(batchSummary.vendorCount) }}</b><span>供应商</span></div>
            </div>
          </div>
          <div class="stock-aside-section stock-aside-section--grow">
            <div class="aside-section-title"><span>物料类别</span><small>批次范围</small></div>
            <div class="stock-category-chips">
              <button
                v-for="category in quickCategories"
                :key="'batch-' + category.id"
                type="button"
                :class="{ active: String(batchQuery.itemTypeId) === String(category.id) }"
                @click="applyBatchCategory(category)"
              >
                <span>{{ category.label }}</span>
                <b>{{ categoryCount(category.id) }}</b>
              </button>
            </div>
          </div>
          <div class="stock-aside-section stock-status-filter">
            <div class="aside-section-title"><span>效期状态</span><small>快速查看</small></div>
            <div class="status-filter-buttons">
              <button type="button" :class="{ active: batchExpiryFilter === 'all' }" @click="applyBatchExpiry('all')">全部</button>
              <button type="button" :class="{ active: batchExpiryFilter === 'normal' }" @click="applyBatchExpiry('normal')">正常</button>
              <button type="button" :class="{ active: batchExpiryFilter === 'near' }" @click="applyBatchExpiry('near')">临期</button>
              <button type="button" :class="{ active: batchExpiryFilter === 'expired' }" @click="applyBatchExpiry('expired')">已过期</button>
            </div>
          </div>
          <div class="stock-aside-section stock-status-filter">
            <div class="aside-section-title"><span>供应来源</span><small>关联维度</small></div>
            <div class="status-filter-buttons">
              <button type="button" :class="{ active: batchSourceFilter === 'vendor' }" @click="applyBatchSource('vendor')">有供应商</button>
              <button type="button" :class="{ active: batchSourceFilter === 'client' }" @click="applyBatchSource('client')">有客户</button>
              <button type="button" :class="{ active: batchSourceFilter === 'workorder' }" @click="applyBatchSource('workorder')">有工单</button>
            </div>
          </div>
        </template>
      </el-col>

      <el-col :span="19" :xs="24" class="wmstock-hub-main">
        <div v-show="hubMode === 'stock'" class="hub-panel hub-panel--stock">
          <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" class="stock-search-form" label-width="0">
        <el-form-item prop="itemCode">
          <el-input
            v-model="queryParams.itemCode"
            placeholder="物料编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="itemName">
          <el-input
            v-model="queryParams.itemName"
            placeholder="物料名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="batchCode">
          <el-input
            v-model="queryParams.batchCode"
            placeholder="批次号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item class="stock-search-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
          <el-button text @click="showMoreFilters = !showMoreFilters">{{ showMoreFilters ? '收起筛选' : '更多筛选' }}</el-button>
        </el-form-item>
      </el-form>

      <el-form v-show="showMoreFilters" :model="queryParams" class="stock-advanced-form" :inline="true" label-width="72px">
        <el-form-item label="仓库" prop="warehouseName"><el-input v-model="queryParams.warehouseName" placeholder="仓库名称" clearable /></el-form-item>
        <el-form-item label="库区" prop="locationName"><el-input v-model="queryParams.locationName" placeholder="库区名称" clearable /></el-form-item>
        <el-form-item label="库位" prop="areaCode"><el-input v-model="queryParams.areaCode" placeholder="库位编码" clearable /></el-form-item>
        <el-form-item label="状态" prop="frozenFlag"><el-select v-model="queryParams.frozenFlag" clearable placeholder="全部"><el-option label="正常" value="N" /><el-option label="冻结" value="Y" /></el-select></el-form-item>
      </el-form>

      <div class="stock-table-toolbar">
        <div class="toolbar-left">
          <span class="toolbar-page-hint">库存现有量</span>
          <el-button
            type="warning"
            plain
            icon="el-icon-download"
            size="default"
            @click="handleExport"
            v-hasPermi="['mes:wm:wmstock:export']"
          >导出</el-button>
          <el-button
            type="primary"
            plain
            icon="el-icon-office-building"
            size="default"
            @click="openBinMap"
          >库位态势</el-button>
        </div>
        <div class="toolbar-meta">
          <span>共 {{ total }} 条 · {{ currentScopeLabel }}</span>
          <span>可用 {{ filteredAvailableQty }}</span>
          <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
        </div>
      </div>

      <div class="stock-table-frame">
        <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table stock-main-table"
        stripe
        border
        height="100%"
        :data="wmstockList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="40" align="center" />
        <el-table-column label="物料编码" width="112" align="center" header-align="center" prop="itemCode" show-overflow-tooltip />
        <el-table-column label="物料名称" width="118" align="center" header-align="center" prop="itemName" show-overflow-tooltip/>
        <el-table-column label="规格" width="78" align="center" header-align="center" prop="specification" show-overflow-tooltip/>
        <el-table-column label="在库" width="68" align="center" header-align="center" prop="quantityOnhand" />
        <el-table-column label="锁定" width="68" align="center" header-align="center" prop="quantityReserved" />
        <el-table-column label="可用" width="68" align="center" header-align="center">
          <template #default="scope">{{ availableQty(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="单位" width="48" align="center" header-align="center" prop="unitName" />
        <el-table-column label="批次号" min-width="120" align="center" header-align="center" prop="batchCode" class-name="col-batch">
          <template #default="scope">
            <button
              type="button"
              class="batch-link-btn"
              @click.stop.prevent="handleBatchClick(scope.row)"
            >{{ scope.row.batchCode || '—' }}</button>
          </template>
        </el-table-column>
        <el-table-column label="仓库" width="82" align="center" header-align="center" prop="warehouseName" show-overflow-tooltip />
        <el-table-column label="库区" width="88" align="center" header-align="center" prop="locationName" show-overflow-tooltip />
        <el-table-column label="库位" width="88" align="center" header-align="center" prop="areaName" class-name="col-area">
          <template #default="scope">
          <button
            type="button"
            class="batch-link-btn"
            @click.stop.prevent="handleAreaClick(scope.row)"
          >{{ scope.row.areaName || '—' }}</button>
        </template>
        </el-table-column>
        <el-table-column label="入库日期" align="center" header-align="center" prop="recptDate" width="100">
          <template #default="scope">
            <span>{{ parseTime(scope.row.recptDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="冻结" align="center" header-align="center" width="78">
          <template #default="scope">
            <el-switch
              v-model="scope.row.frozenFlag"
              active-value="Y"
              inactive-value="N"
              @change="handleFrozenChange(scope.row)"
            ></el-switch>
          </template>
        </el-table-column>
      </el-table>
      </div>
    
      <pagination
        v-show="total>0"
        :total="total"
        :page="queryParams.pageNum"
        :limit="queryParams.pageSize"
        @update:page="queryParams.pageNum = $event"
        @update:limit="queryParams.pageSize = $event"
        @pagination="getList"
      />
        </div>

        <div v-show="hubMode === 'warehouse'" class="hub-panel hub-panel--stock hub-panel--warehouse">
          <el-form :model="whQuery" ref="whQueryForm" size="small" :inline="true" class="stock-search-form" label-width="0">
            <template v-if="whPanel === 'warehouse'">
              <el-form-item prop="warehouseCode">
                <el-input v-model="whQuery.warehouseCode" placeholder="仓库编码" clearable @keyup.enter="handleWhQuery" />
              </el-form-item>
              <el-form-item prop="warehouseName">
                <el-input v-model="whQuery.warehouseName" placeholder="仓库名称" clearable @keyup.enter="handleWhQuery" />
              </el-form-item>
            </template>
            <template v-else-if="whPanel === 'location'">
              <el-form-item prop="locationCode">
                <el-input v-model="whQuery.locationCode" placeholder="库区编码" clearable @keyup.enter="handleWhQuery" />
              </el-form-item>
              <el-form-item prop="locationName">
                <el-input v-model="whQuery.locationName" placeholder="库区名称" clearable @keyup.enter="handleWhQuery" />
              </el-form-item>
            </template>
            <template v-else>
              <el-form-item prop="areaCode">
                <el-input v-model="whQuery.areaCode" placeholder="库位编码" clearable @keyup.enter="handleWhQuery" />
              </el-form-item>
              <el-form-item prop="areaName">
                <el-input v-model="whQuery.areaName" placeholder="库位名称" clearable @keyup.enter="handleWhQuery" />
              </el-form-item>
            </template>
            <el-form-item class="stock-search-actions">
              <el-button type="primary" icon="el-icon-search" size="default" @click="handleWhQuery">搜索</el-button>
              <el-button icon="el-icon-refresh" size="default" @click="resetWhQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <div class="stock-table-toolbar">
            <span class="toolbar-level">{{ whPanelLabel }}</span>
            <div class="toolbar-meta">
              <span>共 {{ whTotal }} 条</span>
            </div>
          </div>

          <div class="stock-table-frame">
            <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
            <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
            <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
            <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
            <el-table v-loading="whLoading" class="yunshu-data-table stock-main-table" stripe border :data="whList">
              <template v-if="whPanel === 'warehouse'">
                <el-table-column label="仓库编码" min-width="120" align="center" header-align="center" prop="warehouseCode" show-overflow-tooltip />
                <el-table-column label="仓库名称" min-width="140" align="center" header-align="center" prop="warehouseName" show-overflow-tooltip />
                <el-table-column label="位置" min-width="140" align="center" header-align="center" prop="location" show-overflow-tooltip />
                <el-table-column label="面积" min-width="90" align="center" header-align="center" prop="area" />
                <el-table-column label="负责人" min-width="100" align="center" header-align="center" prop="charge" />
                <el-table-column label="启用" min-width="90" align="center" header-align="center">
                  <template #default="scope">{{ scope.row.enableFlag === 'N' ? '停用' : '启用' }}</template>
                </el-table-column>
                <el-table-column label="操作" min-width="120" align="center" header-align="center" class-name="col-actions">
                  <template #default="scope">
                    <div class="yunshu-row-actions">
                      <el-button type="text" size="default" @click="drillToLocations(scope.row)">库区</el-button>
                    </div>
                  </template>
                </el-table-column>
              </template>
              <template v-else-if="whPanel === 'location'">
                <el-table-column label="库区编码" min-width="120" align="center" header-align="center" prop="locationCode" show-overflow-tooltip />
                <el-table-column label="库区名称" min-width="140" align="center" header-align="center" prop="locationName" show-overflow-tooltip />
                <el-table-column label="所属仓库" min-width="120" align="center" header-align="center" prop="warehouseName" show-overflow-tooltip />
                <el-table-column label="面积" min-width="90" align="center" header-align="center" prop="area" />
                <el-table-column label="启用" min-width="90" align="center" header-align="center">
                  <template #default="scope">{{ scope.row.enableFlag === 'N' ? '停用' : '启用' }}</template>
                </el-table-column>
                <el-table-column label="操作" min-width="120" align="center" header-align="center" class-name="col-actions">
                  <template #default="scope">
                    <div class="yunshu-row-actions">
                      <el-button type="text" size="default" @click="drillToAreas(scope.row)">库位</el-button>
                    </div>
                  </template>
                </el-table-column>
              </template>
              <template v-else>
                <el-table-column label="库位编码" min-width="120" align="center" header-align="center" prop="areaCode" show-overflow-tooltip />
                <el-table-column label="库位名称" min-width="140" align="center" header-align="center" prop="areaName" show-overflow-tooltip />
                <el-table-column label="所属库区" min-width="120" align="center" header-align="center" prop="locationName" show-overflow-tooltip />
                <el-table-column label="最大载重" min-width="100" align="center" header-align="center" prop="maxLoa" />
                <el-table-column label="产品混放" min-width="90" align="center" header-align="center">
                  <template #default="scope">{{ scope.row.productMixing === 'Y' ? '是' : '否' }}</template>
                </el-table-column>
                <el-table-column label="批次混放" min-width="90" align="center" header-align="center">
                  <template #default="scope">{{ scope.row.batchMixing === 'Y' ? '是' : '否' }}</template>
                </el-table-column>
                <el-table-column label="启用" min-width="90" align="center" header-align="center">
                  <template #default="scope">{{ scope.row.enableFlag === 'N' ? '停用' : '启用' }}</template>
                </el-table-column>
              </template>
            </el-table>
          </div>

          <pagination
            v-show="whTotal > 0"
            :total="whTotal"
            :page="whQuery.pageNum"
            :limit="whQuery.pageSize"
            @update:page="whQuery.pageNum = $event"
            @update:limit="whQuery.pageSize = $event"
            @pagination="getWhList"
          />
        </div>

        <div v-show="hubMode === 'batch'" class="hub-panel hub-panel--stock hub-panel--batch">
          <el-form :model="batchQuery" ref="batchQueryForm" size="small" :inline="true" class="stock-search-form" label-width="0">
            <el-form-item prop="batchCode">
              <el-input v-model="batchQuery.batchCode" placeholder="批次编号" clearable @keyup.enter="handleBatchQuery" />
            </el-form-item>
            <el-form-item prop="itemCode">
              <el-input v-model="batchQuery.itemCode" placeholder="物料编码" clearable @keyup.enter="handleBatchQuery" />
            </el-form-item>
            <el-form-item prop="itemName">
              <el-input v-model="batchQuery.itemName" placeholder="物料名称" clearable @keyup.enter="handleBatchQuery" />
            </el-form-item>
            <el-form-item class="stock-search-actions">
              <el-button type="primary" icon="el-icon-search" size="default" @click="handleBatchQuery">搜索</el-button>
              <el-button icon="el-icon-refresh" size="default" @click="resetBatchQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <div class="stock-table-toolbar">
            <span class="toolbar-level">批次台账</span>
            <div class="toolbar-meta">
              <span>共 {{ batchTotal }} 条 · {{ batchScopeLabel }}</span>
            </div>
          </div>

          <div class="stock-table-frame">
            <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
            <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
            <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
            <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
            <el-table v-loading="batchLoading" class="yunshu-data-table stock-main-table" stripe border :data="batchList">
              <el-table-column label="批次编号" min-width="140" align="center" header-align="center" prop="batchCode" class-name="col-batch">
                <template #default="scope">
                  <button type="button" class="batch-link-btn" @click.stop.prevent="handleBatchClick(scope.row)">{{ scope.row.batchCode || '—' }}</button>
                </template>
              </el-table-column>
              <el-table-column label="物料编码" min-width="120" align="center" header-align="center" prop="itemCode" show-overflow-tooltip />
              <el-table-column label="物料名称" min-width="140" align="center" header-align="center" prop="itemName" show-overflow-tooltip />
              <el-table-column label="规格型号" min-width="110" align="center" header-align="center" prop="specification" show-overflow-tooltip />
              <el-table-column label="单位" min-width="70" align="center" header-align="center" prop="unitOfMeasure" />
              <el-table-column label="生产日期" min-width="112" align="center" header-align="center" prop="produceDate">
                <template #default="scope">
                  <span>{{ parseTime(scope.row.produceDate, '{y}-{m}-{d}') }}</span>
                </template>
              </el-table-column>
              <el-table-column label="有效期" min-width="112" align="center" header-align="center" prop="expireDate">
                <template #default="scope">
                  <span>{{ parseTime(scope.row.expireDate, '{y}-{m}-{d}') }}</span>
                </template>
              </el-table-column>
              <el-table-column label="入库日期" min-width="112" align="center" header-align="center" prop="recptDate">
                <template #default="scope">
                  <span>{{ parseTime(scope.row.recptDate, '{y}-{m}-{d}') }}</span>
                </template>
              </el-table-column>
              <el-table-column label="供应商" min-width="120" align="center" header-align="center" prop="vendorName" show-overflow-tooltip />
              <el-table-column label="质量状态" min-width="100" align="center" header-align="center" prop="qualityStatus" />
            </el-table>
          </div>

          <pagination
            v-show="batchTotal > 0"
            :total="batchTotal"
            :page="batchQuery.pageNum"
            :limit="batchQuery.pageSize"
            @update:page="batchQuery.pageNum = $event"
            @update:limit="batchQuery.pageSize = $event"
            @pagination="getBatchList"
          />
        </div>
      </el-col>
    </el-row>

 <!-- 添加或修改库位设置对话框 -->
 <el-dialog
      v-if="open"
      title="储位详情"
      v-model="open"
      width="960px"
      append-to-body
      destroy-on-close
      align-center
      :z-index="5000"
      @closed="onAreaDialogClosed"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="14">
            <el-row>
              <el-col :span="16">
                <el-form-item label="库位编码" prop="areaCode">
                  <el-input v-model="form.areaCode" readonly="readonly" maxlength="64"/>
                </el-form-item>
              </el-col>
              <el-col :span="8">

              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="库位名称" prop="areaName">
                  <el-input v-model="form.areaName" readonly="readonly" maxlength="255"/>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="面积" prop="area">
                  <el-input-number :min="0" :max="99999999" :step="1" :percision="2" v-model="form.area" readonly="readonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最大载重量" prop="maxLoa">
                  <el-input-number v-model="form.maxLoa" :max="99999999" :step="1" :percision="2" readonly="readonly" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="允许产品混放" prop="productMixing">
                  <el-radio-group v-model="form.productMixing">
                    <el-radio
                      v-for="dict in dict.type.sys_yes_no"
                      :key="dict.value"
                      :label="dict.value"
                    >{{dict.label}}</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="允许批次混放" prop="batchMixing">
                  <el-radio-group v-model="form.batchMixing">
                    <el-radio
                      v-for="dict in dict.type.sys_yes_no"
                      :key="dict.value"
                      :label="dict.value"
                    >{{dict.label}}</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
          </el-col>
          <el-col :span="10">
            <BarcodeImg ref="barcodeImg" :bussinessId="form.areaId" :bussinessCode="form.areaCode" barcodeType="AREA"></BarcodeImg>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="库位位置X" prop="positionX">
              <el-input-number :min="0" :max="99999999" :step="1" v-model="form.positionX" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库位位置y" prop="positionY">
              <el-input-number :min="0" :max="99999999" :step="1" v-model="form.positionY" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库位位置z" prop="positionZ">
              <el-input-number :min="0" :max="99999999" :step="1" v-model="form.positionZ" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" maxlength="500" readonly="readonly"/>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeArea">关 闭</el-button>
      </div>
      </template>
    </el-dialog>

    <el-dialog
      v-if="batchOpen"
      title="批次详情"
      v-model="batchOpen"
      width="960px"
      append-to-body
      destroy-on-close
      align-center
      :z-index="5000"
      @closed="onBatchDialogClosed"
    >
      <el-form ref="batchform" :model="batchform" label-width="100px">
        <el-row>
          <el-col :span="14">
            <el-row>
              <el-col :span="24">
                <el-form-item label="批次编号" prop="batchCode">
                  <el-input v-model="batchform.batchCode" readonly="readonly"/>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="产品物料编码" prop="itemCode">
                  <el-input v-model="batchform.itemCode" readonly="readonly" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="产品物料名称" prop="itemName">
                  <el-input v-model="batchform.itemName" readonly="readonly" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="规格型号" prop="specification">
                  <el-input v-model="batchform.specification" type="textarea" readonly="readonly" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-col>
          <el-col :span="10">
            <BarcodeImg ref="batchBarcodeImg" :bussinessId="batchform.batchId || -1" :bussinessCode="batchform.batchCode" barcodeType="BATCH"></BarcodeImg>
          </el-col>
        </el-row>      
        <el-row>
          <el-col :span="8">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="batchform.unitName" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产日期" prop="produceDate">
              <el-input :model-value="formatDialogDate(batchform.produceDate)" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="有效期" prop="expireDate">
              <el-input :model-value="formatDialogDate(batchform.expireDate)" readonly />
            </el-form-item>
          </el-col>
        </el-row>  
        <el-row>
              <el-col :span="12">
                <el-form-item label="供应商名称" prop="vendorName">
                  <el-input v-model="batchform.vendorName" readonly="readonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="客户名称" prop="clientName">
                  <el-input v-model="batchform.clientName" readonly="readonly" />
                </el-form-item>
              </el-col>
            </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="销售订单编号" prop="coCode">
              <el-input v-model="batchform.coCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="采购订单编号" prop="poCode">
              <el-input v-model="batchform.poCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产工单" prop="workorderCode">
              <el-input v-model="batchform.workorderCode" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="生产任务" prop="taskCode">
              <el-input v-model="batchform.taskCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作站编码" prop="workstationCode">
              <el-input v-model="batchform.workstationCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产批号" prop="productCode">
              <el-input v-model="batchform.productCode" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeBatch">关 闭</el-button>
      </div>
      </template>
    </el-dialog>

    <WmBinMapDrawer ref="binMapDrawer" @locate="applyBinLocate" />

  </div>
</template>

<script>
import { listWmstock, changeFrozenState, getWmstockOverview } from "@/yunshu-ui/api/mes/wm/wmstock";
import { getArea, listArea } from "@/yunshu-ui/api/mes/wm/area";
import { listBatch, getBatch } from "@/yunshu-ui/api/mes/wm/batch";
import { getTreeList, listWarehouse } from "@/yunshu-ui/api/mes/wm/warehouse";
import { listLocation } from "@/yunshu-ui/api/mes/wm/location";
import { treeselect } from "@/yunshu-ui/api/mes/md/itemtype";
import BarcodeImg from "@/yunshu-ui/components/barcodeImg/index.vue";
import WmBinMapDrawer from "./components/WmBinMapDrawer.vue";

export default {
  name: "Wmstock",
  dicts: ['sys_yes_no'],
  components: { BarcodeImg, WmBinMapDrawer },
  data() {
    return {
      hubMode: "stock",
      showMoreFilters: false,
      overview: {
        summary: { skuCount: 0, quantityOnhand: 0, frozenCount: 0, expiringCount: 0 },
        categories: [],
        warehouses: [],
      },
      quickCategories: [
        { id: 4, label: "原材料" },
        { id: 5, label: "半成品" },
        { id: 3, label: "成品" },
        { id: 6, label: "辅料" },
        { id: 7, label: "备品备件" },
      ],
      whPanel: "warehouse",
      whLoading: false,
      whList: [],
      whTotal: 0,
      whQuery: {
        pageNum: 1,
        pageSize: 10,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaCode: null,
        areaName: null,
        enableFlag: null,
      },
      warehouseTree: [],
      warehouseTreeProps: {
        children: "children",
        label: "label",
      },
      batchLoading: false,
      batchList: [],
      batchTotal: 0,
      batchAllCache: [],
      batchExpiryFilter: "all",
      batchSourceFilter: null,
      batchQuery: {
        pageNum: 1,
        pageSize: 10,
        batchCode: null,
        itemCode: null,
        itemName: null,
        itemTypeId: null,
        vendorName: null,
        clientName: null,
        workorderCode: null,
      },
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      itemTypeName: null,
      defaultProps: {
        children: "children",
        label: "label"
      },
      // 总条数
      total: 0,
      //物料产品分类树
      itemTypeOptions: undefined,
      // 库存记录表格数据
      wmstockList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      batchOpen: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        itemTypeId: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        batchCode: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        vendorNick: null,
        quantityOnhand: null,
        workorderCode: null,
        expireDate: null,
        nearExpiry: null,
      },
      // 表单参数
      form: {},
      batchform: {},
    };
  },
  watch: {
    // 根据名称筛选分类树
    itemTypeName(val) {
      this.$refs.tree && this.$refs.tree.filter(val);
    },
    "$route.query.mode"() {
      this.syncModeFromRoute();
    },
  },
  created() {
    this.syncModeFromRoute();
    this.getList();
    this.getTreeselect();
    this.loadWarehouseTree();
    this.loadOverview();
    if (this.hubMode === "warehouse") this.getWhList();
    if (this.hubMode === "batch") this.getBatchList();
  },
  beforeUnmount() {
    this.batchOpen = false;
    this.open = false;
    this.clearOrphanDialogOverlays();
  },
  computed: {
    currentScopeLabel() {
      if (this.queryParams.nearExpiry === "Y") return "临期库存";
      if (this.queryParams.frozenFlag === "Y") return "冻结库存";
      const category = this.quickCategories.find((item) => String(item.id) === String(this.queryParams.itemTypeId));
      if (category) return category.label + "库存";
      const warehouse = this.overview.warehouses.find((item) => String(item.warehouseId) === String(this.queryParams.warehouseId));
      if (warehouse) return warehouse.warehouseName + "库存";
      return "全部库存";
    },
    filteredAvailableQty() {
      return this.formatQty(this.wmstockList.reduce((sum, row) => sum + this.availableQty(row), 0));
    },
    warehouseSummary() {
      let locationCount = 0;
      let areaCount = 0;
      let enabledCount = 0;
      (this.warehouseTree || []).forEach((wh) => {
        if (wh.enableFlag !== "N") enabledCount += 1;
        (wh.children || []).forEach((loc) => {
          locationCount += 1;
          areaCount += (loc.children || []).length;
        });
      });
      return {
        warehouseCount: (this.warehouseTree || []).length,
        locationCount,
        areaCount,
        enabledCount,
      };
    },
    whPanelLabel() {
      if (this.whPanel === "location") return "库区列表";
      if (this.whPanel === "area") return "库位列表";
      return "仓库列表";
    },
    batchSummary() {
      const rows = this.batchAllCache || [];
      const now = new Date();
      const threshold = new Date(now);
      threshold.setDate(now.getDate() + 30);
      let nearExpiry = 0;
      let expired = 0;
      const vendors = new Set();
      rows.forEach((row) => {
        if (row.vendorName) vendors.add(row.vendorName);
        if (!row.expireDate) return;
        const exp = new Date(row.expireDate);
        if (exp < now) expired += 1;
        else if (exp <= threshold) nearExpiry += 1;
      });
      return {
        batchCount: this.batchTotal || rows.length,
        nearExpiry,
        expired,
        vendorCount: vendors.size,
      };
    },
    batchScopeLabel() {
      if (this.batchExpiryFilter === "near") return "临期批次";
      if (this.batchExpiryFilter === "expired") return "已过期";
      if (this.batchExpiryFilter === "normal") return "正常效期";
      if (this.batchSourceFilter === "vendor") return "有供应商";
      if (this.batchSourceFilter === "client") return "有客户";
      if (this.batchSourceFilter === "workorder") return "有工单";
      const category = this.quickCategories.find((item) => String(item.id) === String(this.batchQuery.itemTypeId));
      if (category) return category.label + "批次";
      return "全部批次";
    },
  },
  methods: {
    syncModeFromRoute() {
      const mode = this.$route.query.mode;
      if (mode === "warehouse" || mode === "batch" || mode === "stock") {
        this.hubMode = mode;
      }
      ["itemTypeId", "warehouseId", "frozenFlag", "nearExpiry"].forEach((key) => {
        if (this.$route.query[key] !== undefined) {
          this.queryParams[key] = this.$route.query[key];
        }
      });
      if (this.hubMode === "warehouse") {
        if (this.$route.query.locationId) {
          this.whPanel = "area";
          this.whQuery.locationId = this.$route.query.locationId;
        } else if (this.$route.query.warehouseId) {
          this.whPanel = "location";
          this.whQuery.warehouseId = this.$route.query.warehouseId;
        } else {
          this.whPanel = "warehouse";
        }
      }
    },
    onHubModeChange(mode) {
      const query = { ...this.$route.query, mode };
      if (mode !== "warehouse") {
        delete query.warehouseId;
        delete query.locationId;
      }
      this.$router.replace({ path: "/app/inventory/wmstock", query }).catch(() => {});
      if (mode === "warehouse") {
        this.whPanel = "warehouse";
        this.loadWarehouseTree();
        this.getWhList();
      } else if (mode === "batch") {
        this.getBatchList();
      } else {
        this.getList();
      }
    },
    mapWarehouseTree(nodes) {
      return (nodes || []).map((wh) => ({
        ...wh,
        nodeType: "warehouse",
        label: wh.warehouseName || wh.warehouseCode,
        children: (wh.children || []).map((loc) => ({
          ...loc,
          nodeType: "location",
          label: loc.locationName || loc.locationCode,
          children: (loc.children || []).map((area) => ({
            ...area,
            nodeType: "area",
            label: area.areaName || area.areaCode,
          })),
        })),
      }));
    },
    loadWarehouseTree() {
      getTreeList().then((response) => {
        this.warehouseTree = this.mapWarehouseTree(response.data || []);
      }).catch(() => {
        this.warehouseTree = [];
      });
    },
    loadOverview() {
      getWmstockOverview().then((response) => {
        this.overview = {
          summary: response.data?.summary || {},
          categories: response.data?.categories || [],
          warehouses: response.data?.warehouses || [],
        };
      }).catch(() => {
        // 兼容后端尚未热重启的场景：用现有列表即时构建导航舱摘要。
        listWmstock({ pageNum: 1, pageSize: 1000 }).then((response) => {
          this.overview = this.buildOverview(response.rows || []);
        }).catch(() => {});
      });
    },
    buildOverview(rows) {
      const categoryMap = new Map();
      const warehouseMap = new Map();
      const uniqueItems = new Set();
      let quantityOnhand = 0;
      let frozenCount = 0;
      let expiringCount = 0;
      const now = new Date();
      const threshold = new Date(now);
      threshold.setDate(now.getDate() + 30);
      rows.forEach((row) => {
        const quantity = Number(row.quantityOnhand) || 0;
        uniqueItems.add(row.itemCode);
        quantityOnhand += quantity;
        if (row.frozenFlag === "Y") frozenCount += 1;
        if (row.expireDate && new Date(row.expireDate) >= now && new Date(row.expireDate) <= threshold) expiringCount += 1;
        const category = categoryMap.get(String(row.itemTypeId)) || {
          itemTypeId: row.itemTypeId,
          itemTypeName: "",
          skuSet: new Set(),
          quantityOnhand: 0,
        };
        category.skuSet.add(row.itemCode);
        category.quantityOnhand += quantity;
        categoryMap.set(String(row.itemTypeId), category);
        const warehouse = warehouseMap.get(String(row.warehouseId)) || {
          warehouseId: row.warehouseId,
          warehouseName: row.warehouseName,
          skuSet: new Set(),
          quantityOnhand: 0,
        };
        warehouse.skuSet.add(row.itemCode);
        warehouse.quantityOnhand += quantity;
        warehouseMap.set(String(row.warehouseId), warehouse);
      });
      return {
        summary: { skuCount: uniqueItems.size, quantityOnhand, frozenCount, expiringCount },
        categories: [...categoryMap.values()].map((item) => ({
          ...item,
          skuCount: item.skuSet.size,
        })),
        warehouses: [...warehouseMap.values()].map((item) => ({
          ...item,
          skuCount: item.skuSet.size,
        })),
      };
    },
    formatQty(value) {
      return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 0 }).format(Number(value) || 0);
    },
    formatDialogDate(value) {
      if (!value) return "";
      return this.parseTime(value, "{y}-{m}-{d}") || String(value).slice(0, 10);
    },
    categoryCount(itemTypeId) {
      const category = this.overview.categories.find((item) => String(item.itemTypeId) === String(itemTypeId));
      return this.formatQty(category?.quantityOnhand);
    },
    applyCategory(itemTypeId) {
      this.queryParams.itemTypeId = this.queryParams.itemTypeId === itemTypeId ? null : itemTypeId;
      this.queryParams.pageNum = 1;
      this.persistScope();
      this.getList();
    },
    applyWarehouse(warehouseId) {
      this.queryParams.warehouseId = this.queryParams.warehouseId === warehouseId ? null : warehouseId;
      this.queryParams.pageNum = 1;
      this.persistScope();
      this.getList();
    },
    applyStatus(status) {
      this.queryParams.frozenFlag = status === "frozen" ? "Y" : status === "normal" ? "N" : null;
      this.queryParams.nearExpiry = status === "expiry" ? "Y" : null;
      this.queryParams.pageNum = 1;
      this.persistScope();
      this.getList();
    },
    resetScope() {
      this.queryParams.itemTypeId = null;
      this.queryParams.warehouseId = null;
      this.queryParams.frozenFlag = null;
      this.queryParams.nearExpiry = null;
      this.queryParams.pageNum = 1;
      this.persistScope();
      this.getList();
    },
    persistScope() {
      const query = { ...this.$route.query };
      ["itemTypeId", "warehouseId", "frozenFlag", "nearExpiry"].forEach((key) => {
        const value = this.queryParams[key];
        if (value === null || value === undefined || value === "") delete query[key];
        else query[key] = String(value);
      });
      this.$router.replace({ path: "/app/inventory/wmstock", query }).catch(() => {});
    },
    setWhPanel(panel) {
      this.whPanel = panel;
      this.whQuery.pageNum = 1;
      if (panel === "warehouse") {
        this.whQuery.warehouseId = null;
        this.whQuery.locationId = null;
      } else if (panel === "location") {
        this.whQuery.locationId = null;
      }
      this.getWhList();
    },
    applyWhEnable(flag) {
      this.whQuery.enableFlag = flag;
      this.whQuery.pageNum = 1;
      this.getWhList();
    },
    resetWarehouseScope() {
      this.whPanel = "warehouse";
      this.whQuery = {
        pageNum: 1,
        pageSize: 10,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaCode: null,
        areaName: null,
        enableFlag: null,
      };
      this.$router.replace({ path: "/app/inventory/wmstock", query: { mode: "warehouse" } }).catch(() => {});
      this.getWhList();
    },
    handleWhQuery() {
      this.whQuery.pageNum = 1;
      this.getWhList();
    },
    resetWhQuery() {
      this.whQuery.warehouseCode = null;
      this.whQuery.warehouseName = null;
      this.whQuery.locationCode = null;
      this.whQuery.locationName = null;
      this.whQuery.areaCode = null;
      this.whQuery.areaName = null;
      this.handleWhQuery();
    },
    getWhList() {
      this.whLoading = true;
      const params = {
        pageNum: this.whQuery.pageNum,
        pageSize: this.whQuery.pageSize,
        enableFlag: this.whQuery.enableFlag || undefined,
      };
      let request;
      if (this.whPanel === "location") {
        request = listLocation({
          ...params,
          warehouseId: this.whQuery.warehouseId || undefined,
          locationCode: this.whQuery.locationCode || undefined,
          locationName: this.whQuery.locationName || undefined,
        });
      } else if (this.whPanel === "area") {
        request = listArea({
          ...params,
          locationId: this.whQuery.locationId || undefined,
          warehouseId: this.whQuery.warehouseId || undefined,
          areaCode: this.whQuery.areaCode || undefined,
          areaName: this.whQuery.areaName || undefined,
        });
      } else {
        request = listWarehouse({
          ...params,
          warehouseCode: this.whQuery.warehouseCode || undefined,
          warehouseName: this.whQuery.warehouseName || undefined,
        });
      }
      request.then((response) => {
        this.whList = response.rows || response.data || [];
        this.whTotal = response.total || this.whList.length;
        this.whLoading = false;
      }).catch(() => {
        this.whList = [];
        this.whTotal = 0;
        this.whLoading = false;
      });
    },
    drillToLocations(row) {
      this.whPanel = "location";
      this.whQuery.warehouseId = row.warehouseId;
      this.whQuery.pageNum = 1;
      this.$router.replace({
        path: "/app/inventory/wmstock",
        query: { mode: "warehouse", warehouseId: String(row.warehouseId) },
      }).catch(() => {});
      this.getWhList();
    },
    drillToAreas(row) {
      this.whPanel = "area";
      this.whQuery.locationId = row.locationId;
      this.whQuery.warehouseId = row.warehouseId;
      this.whQuery.pageNum = 1;
      this.$router.replace({
        path: "/app/inventory/wmstock",
        query: { mode: "warehouse", locationId: String(row.locationId) },
      }).catch(() => {});
      this.getWhList();
    },
    async handleWarehouseTreeClick(data) {
      if (data.nodeType === "warehouse" || (data.warehouseId && !data.locationId)) {
        this.whPanel = "location";
        this.whQuery.warehouseId = data.warehouseId;
        this.whQuery.locationId = null;
        this.whQuery.pageNum = 1;
        await this.$router.replace({
          path: "/app/inventory/wmstock",
          query: { mode: "warehouse", warehouseId: String(data.warehouseId) },
        }).catch(() => {});
        this.getWhList();
        return;
      }
      if (data.nodeType === "location" || (data.locationId && !data.areaId)) {
        this.whPanel = "area";
        this.whQuery.locationId = data.locationId;
        this.whQuery.warehouseId = data.warehouseId;
        this.whQuery.pageNum = 1;
        await this.$router.replace({
          path: "/app/inventory/wmstock",
          query: { mode: "warehouse", warehouseId: String(data.warehouseId || ""), locationId: String(data.locationId) },
        }).catch(() => {});
        this.getWhList();
        return;
      }
      if (data.nodeType === "area" || data.areaId) {
        this.whPanel = "area";
        this.whQuery.locationId = data.locationId;
        this.whQuery.areaCode = data.areaCode || null;
        this.whQuery.pageNum = 1;
        this.getWhList();
      }
    },
    handleBatchQuery() {
      this.batchQuery.pageNum = 1;
      this.getBatchList();
    },
    resetBatchQuery() {
      this.batchQuery.batchCode = null;
      this.batchQuery.itemCode = null;
      this.batchQuery.itemName = null;
      this.handleBatchQuery();
    },
    resetBatchScope() {
      this.batchExpiryFilter = "all";
      this.batchSourceFilter = null;
      this.batchQuery = {
        pageNum: 1,
        pageSize: 10,
        batchCode: null,
        itemCode: null,
        itemName: null,
        itemTypeId: null,
        vendorName: null,
        clientName: null,
        workorderCode: null,
      };
      this.getBatchList();
    },
    applyBatchCategory(category) {
      this.batchQuery.itemTypeId = this.batchQuery.itemTypeId === category.id ? null : category.id;
      this.batchQuery.itemName = this.batchQuery.itemTypeId ? category.label : null;
      this.batchQuery.pageNum = 1;
      this.getBatchList();
    },
    applyBatchExpiry(filter) {
      this.batchExpiryFilter = filter;
      this.batchQuery.pageNum = 1;
      this.getBatchList();
    },
    applyBatchSource(source) {
      this.batchSourceFilter = this.batchSourceFilter === source ? null : source;
      this.batchQuery.pageNum = 1;
      this.getBatchList();
    },
    filterBatchRows(rows) {
      const now = new Date();
      const threshold = new Date(now);
      threshold.setDate(now.getDate() + 30);
      return (rows || []).filter((row) => {
        if (this.batchExpiryFilter === "near") {
          if (!row.expireDate) return false;
          const exp = new Date(row.expireDate);
          if (!(exp >= now && exp <= threshold)) return false;
        } else if (this.batchExpiryFilter === "expired") {
          if (!row.expireDate || new Date(row.expireDate) >= now) return false;
        } else if (this.batchExpiryFilter === "normal") {
          if (row.expireDate) {
            const exp = new Date(row.expireDate);
            if (exp < now || exp <= threshold) return false;
          }
        }
        if (this.batchSourceFilter === "vendor" && !row.vendorName && !row.vendorId) return false;
        if (this.batchSourceFilter === "client" && !row.clientName && !row.clientId) return false;
        if (this.batchSourceFilter === "workorder" && !row.workorderCode && !row.workorderId) return false;
        return true;
      });
    },
    getBatchList() {
      this.batchLoading = true;
      const params = {
        pageNum: 1,
        pageSize: 500,
        batchCode: this.batchQuery.batchCode || undefined,
        itemCode: this.batchQuery.itemCode || undefined,
        itemName: this.batchQuery.itemName || undefined,
      };
      listBatch(params).then((response) => {
        const rows = response.rows || response.data || [];
        this.batchAllCache = rows;
        const filtered = this.filterBatchRows(rows);
        this.batchTotal = filtered.length;
        const start = (this.batchQuery.pageNum - 1) * this.batchQuery.pageSize;
        this.batchList = filtered.slice(start, start + this.batchQuery.pageSize);
        this.batchLoading = false;
      }).catch(() => {
        this.batchList = [];
        this.batchTotal = 0;
        this.batchAllCache = [];
        this.batchLoading = false;
      });
    },
    /** 查询库存记录列表 */
    getList() {
      this.loading = true;
      listWmstock(this.queryParams).then(response => {
        this.wmstockList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 查询分类下拉树结构 */
    getTreeselect() {
      treeselect().then(response => {
        this.itemTypeOptions = response.data;
      });
    },
    /**
     * 冻结状态变更
     * @param row 
     */
    handleFrozenChange(row){
      let text = row.frozenFlag === "Y" ? "冻结" : "解冻";
      this.$modal.confirm('确认要"' + text + '""' + row.materialStockId + '"此库存吗？').then(function() {
        return changeFrozenState(row.materialStockId,row.frozenFlag);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(function() {
        row.frozenFlag = row.frozenFlag === "N" ? "Y" : "N";
      });

    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.itemTypeId = data.id;
      this.queryParams.pageNum = 1;
      this.persistScope();
      this.getList();
    },
    // 表单重置
    reset() {
      this.form = {
        areaId: null,
        areaCode: null,
        areaName: null,
        locationId: null,
        area: null,
        maxLoa: null,
        productMixing: 'N',
        batchMixing: 'N',
        positionX: null,
        positionY: null,
        positionZ: null,
        enableFlag: 'Y',
        remark: null,
        attr1: null,
        attr2: null,
        attr3: null,
        attr4: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      };
      this.resetForm("form");
    },
    resetBatch(){
      this.batchform = {
        batchId: null,
        batchCode: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitName: null,
        produceDate: null,
        expireDate: null,
        vendorName: null,
        clientName: null,
        coCode: null,
        poCode: null,
        workorderCode: null,
        taskCode: null,
        workstationCode: null,
        productCode: null,
      };
    },
    clearOrphanDialogOverlays() {
      // 清理历史残留的隐藏 overlay，避免挡住点击或叠层错乱
      document.querySelectorAll("body > .el-overlay.el-modal-dialog").forEach((el) => {
        const style = window.getComputedStyle(el);
        if (style.display === "none") {
          el.parentNode && el.parentNode.removeChild(el);
        }
      });
    },
    //库位点击事件
    handleAreaClick(row){
      if (!row) return;
      this.reset();
      const areaId = row.areaId;
      const openWith = (data) => {
        this.clearOrphanDialogOverlays();
        this.form = Object.assign({}, this.form, data || {});
        this.open = true;
        this.$nextTick(() => {
          this.$refs.barcodeImg?.getBarcode?.();
        });
      };
      if (!areaId) {
        openWith({
          areaId: null,
          areaCode: row.areaCode,
          areaName: row.areaName,
          locationId: row.locationId,
          area: row.area,
          maxLoa: row.maxLoa,
          productMixing: row.productMixing || "N",
          batchMixing: row.batchMixing || "N",
          positionX: row.positionX,
          positionY: row.positionY,
          positionZ: row.positionZ,
          enableFlag: row.enableFlag || "Y",
          remark: row.remark,
        });
        return;
      }
      getArea(areaId).then(response => {
        if (response?.data) openWith(response.data);
        else openWith(row);
      }).catch(() => openWith(row));
    },
    closeArea(){
      this.open = false;
    },
    onAreaDialogClosed() {
      this.open = false;
      this.clearOrphanDialogOverlays();
    },
    //批次点击：多数库存行只有 batchCode、无 batchId，不能请求 /batch/null
    handleBatchClick(row){
      if (!row) return;
      this.resetBatch();
      const batchId = row.batchId;
      const fromStockRow = () => ({
        batchId: row.batchId || null,
        batchCode: row.batchCode,
        itemCode: row.itemCode,
        itemName: row.itemName,
        specification: row.specification,
        unitName: row.unitName || row.unitOfMeasure,
        produceDate: row.productionDate || row.produceDate || null,
        expireDate: row.expireDate || null,
        vendorName: row.vendorName,
        clientName: row.clientName,
        coCode: row.coCode,
        poCode: row.poCode,
        workorderCode: row.workorderCode,
        taskCode: row.taskCode,
        workstationCode: row.workstationCode,
        productCode: row.productCode,
      });
      const openWith = (data) => {
        this.clearOrphanDialogOverlays();
        this.batchform = Object.assign({}, this.batchform, data || {});
        this.batchOpen = true;
        this.$nextTick(() => {
          this.$refs.batchBarcodeImg?.getBarcode?.();
        });
      };
      if (!batchId) {
        openWith(fromStockRow());
        return;
      }
      getBatch(batchId).then(response => {
        if (response?.data) openWith(response.data);
        else openWith(fromStockRow());
      }).catch(() => openWith(fromStockRow()));
    },
    closeBatch(){
      this.batchOpen = false;
    },
    onBatchDialogClosed() {
      this.batchOpen = false;
      this.clearOrphanDialogOverlays();
    },

    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.itemTypeId = null;
      this.queryParams.warehouseId = null;
      this.queryParams.nearExpiry = null;
      this.persistScope();
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.materialStockId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/wm/wmstock/export', {
        ...this.queryParams
      }, `wmstock_${new Date().getTime()}.xlsx`)
    },
    openBinMap() {
      this.$refs.binMapDrawer?.open();
    },
    applyBinLocate({ warehouseId, areaCode }) {
      if (warehouseId) {
        this.queryParams.warehouseId = warehouseId;
      }
      if (areaCode) {
        this.queryParams.areaCode = areaCode;
      }
      this.persistScope();
      this.handleQuery();
    },
    availableQty(row) {
      return Math.max(0, (Number(row.quantityOnhand) || 0) - (Number(row.quantityReserved) || 0));
    }
  }
};
</script>

<style scoped>
.wmstock-command-center {
  min-height: 100%;
  padding: 10px 12px 6px;
  background: var(--mes-watercolor-bg);
}
.wmstock-hub-row {
  display: flex;
  align-items: stretch;
  min-height: calc(100vh - 158px);
  height: calc(100vh - 158px);
  flex-wrap: nowrap;
}
.wmstock-hub-aside {
  display: flex;
  flex-direction: column;
  flex: 0 0 272px;
  max-width: 272px;
  /* 与右侧表格框底对齐：主区总高减去分页区占用 */
  height: calc(100% - 50px);
  align-self: flex-start;
  padding: 0 12px 0 0;
  border-right: 1px solid #dce5f0;
  overflow: hidden;
}
.hub-mode-switch {
  display: flex;
  width: 100%;
  margin-bottom: 10px;
}
.hub-mode-switch :deep(.el-radio-group) {
  display: flex;
  width: 100%;
}
.hub-mode-switch :deep(.el-radio-button) {
  flex: 1 1 0;
  min-width: 0;
}
.hub-mode-switch :deep(.el-radio-button__inner) {
  width: 100%;
  padding: 9px 0;
  color: #617189;
  border-color: #d8e2ee;
  border-radius: 0 !important;
  font-size: 13px;
  text-align: center;
}
.hub-mode-switch :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 0 !important;
}
.hub-mode-switch :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 0 !important;
}
.hub-mode-switch :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  color: #fff;
  background: #1769e0;
  border-color: #1769e0;
  box-shadow: none;
}
.stock-aside-summary,
.stock-aside-section {
  flex: 0 0 auto;
  margin-bottom: 8px;
  padding: 10px 12px;
  border: 1px solid #dce5f0;
  background: #fff;
  box-shadow: 0 5px 14px rgba(28, 49, 84, 0.035);
}
.wmstock-hub-aside > .stock-aside-section:last-child,
.wmstock-hub-aside > .stock-status-filter:last-child {
  margin-bottom: 0;
}
.stock-aside-section--grow {
  flex: 1 1 auto;
  min-height: 0;
}
.aside-summary-head,
.aside-section-title,
.warehouse-scope,
.stock-table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.aside-summary-head {
  color: #334865;
  font-size: 14px;
  font-weight: 700;
}
.aside-reset {
  padding: 0;
  color: #1769e0;
  border: 0;
  background: none;
  font-size: 12px;
  cursor: pointer;
}
.aside-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 10px;
  border-top: 1px solid #edf1f6;
}
.aside-stat-grid > div {
  padding: 9px 5px 0;
}
.aside-stat-grid > div:nth-child(odd) {
  border-right: 1px solid #edf1f6;
}
.aside-stat-grid b,
.stock-heading-metrics b {
  display: block;
  color: #183b67;
  font-size: 18px;
  font-weight: 700;
}
.aside-stat-grid span {
  display: block;
  margin-top: 2px;
  color: #7d8a9d;
  font-size: 11px;
}
.aside-section-title {
  margin-bottom: 9px;
  color: #3d506b;
  font-size: 13px;
  font-weight: 700;
}
.aside-section-title small {
  color: #91a0b3;
  font-size: 11px;
  font-weight: 400;
}
.stock-category-chips {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
}
.stock-category-chips button,
.status-filter-buttons button,
.warehouse-scope {
  color: #5b6c83;
  border: 1px solid #e0e7f0;
  background: #fbfcfe;
  cursor: pointer;
}
.stock-category-chips button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 31px;
  padding: 0 8px;
  text-align: left;
  font-size: 12px;
}
.stock-category-chips button:last-child {
  grid-column: span 2;
}
.stock-category-chips b {
  color: #7a8aa0;
  font-size: 12px;
}
.stock-category-chips button.active,
.status-filter-buttons button.active,
.warehouse-scope.active {
  color: #155dc2;
  border-color: #9dbde9;
  background: #edf5ff;
}
.stock-aside-tree :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #d9e2ee inset;
}
.stock-aside-tree {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 9px 10px;
}
.stock-aside-tree--fill {
  flex: 1 1 auto;
  min-height: 96px;
}
.stock-aside-tree .aside-section-title {
  flex: 0 0 auto;
  margin-bottom: 6px;
}
.stock-aside-tree :deep(.el-input) {
  flex: 0 0 auto;
}
.stock-aside-tree :deep(.el-tree) {
  margin-top: 0;
  color: #52647c;
  background: transparent;
  font-size: 12px;
}
.stock-aside-tree :deep(.el-tree-node__content) {
  height: 24px;
}
.hub-tree-wrap {
  flex: 1 1 auto;
  min-height: 72px;
  margin-top: 4px;
  overflow: auto;
}
.warehouse-scope {
  width: 100%;
  min-height: 32px;
  margin-bottom: 6px;
  padding: 0 8px;
  font-size: 12px;
}
.warehouse-scope:last-child {
  margin-bottom: 0;
}
.warehouse-scope b {
  color: #315985;
  font-size: 12px;
}
.status-filter-buttons {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}
.status-filter-buttons button {
  min-height: 28px;
  font-size: 12px;
}
.wmstock-hub-main {
  display: flex;
  flex-direction: column;
  flex: 1 1 0;
  width: auto;
  max-width: none;
  min-width: 0;
  height: 100%;
  min-height: 0;
}
.hub-panel--stock {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
}
.stock-panel-heading {
  display: none;
}
.stock-heading-metrics {
  display: flex;
  gap: 18px;
  color: #77869a;
  font-size: 12px;
}
.stock-heading-metrics b {
  display: inline;
  margin-left: 3px;
  font-size: 13px;
}
.toolbar-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  color: #77869a;
  font-size: 12px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: -2px;
}
.toolbar-page-hint {
  color: #1b2b43;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.02em;
  white-space: nowrap;
}
.toolbar-level {
  color: #3d506b;
  font-size: 13px;
  font-weight: 700;
}
.stock-table-toolbar {
  min-height: 36px;
  margin: 4px 0 7px;
  padding-left: 0;
}
.stock-search-form,
.stock-advanced-form {
  margin-bottom: 8px;
  padding: 10px 12px 0;
  border: 1px solid #dce5f0;
  background: #fff;
}
.stock-search-form :deep(.el-form-item) {
  margin: 0 12px 10px 0;
}
.stock-search-form :deep(.el-input) {
  width: 158px;
}
.stock-search-form :deep(.el-input__wrapper),
.stock-advanced-form :deep(.el-input__wrapper),
.stock-advanced-form :deep(.el-select__wrapper) {
  min-height: 33px;
  box-shadow: 0 0 0 1px #d9e2ee inset !important;
}
.stock-search-actions :deep(.el-button) {
  min-height: 33px;
}
.stock-advanced-form :deep(.el-form-item) {
  margin: 0 12px 10px 0;
}
.stock-advanced-form :deep(.el-input),
.stock-advanced-form :deep(.el-select) {
  width: 145px;
}
.stock-table-frame {
  position: relative;
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #c8d2e0;
  background:
    linear-gradient(180deg, rgba(23, 105, 224, 0.035), transparent 48px),
    #fff;
  box-shadow: 0 8px 22px rgba(28, 49, 84, 0.05);
}
.stock-main-table {
  flex: 1 1 auto;
  width: 100% !important;
  min-height: 0;
  --el-table-border-color: #e2e8f0;
  border: 0 !important;
  font-size: 13px;
}
.stock-main-table :deep(.el-table__inner-wrapper),
.stock-main-table :deep(.el-table__header-wrapper),
.stock-main-table :deep(.el-table__body-wrapper) {
  width: 100% !important;
}
.stock-main-table :deep(.el-table__body-wrapper) {
  overflow-x: hidden !important;
  overflow-y: auto !important;
}
.stock-main-table :deep(.el-scrollbar__wrap) {
  overflow-x: hidden !important;
}
.stock-main-table :deep(.el-table__header),
.stock-main-table :deep(.el-table__body) {
  width: 100% !important;
  table-layout: fixed !important;
}
.stock-main-table :deep(th.el-table__cell) {
  color: #3a4860;
  border-bottom: 1px solid #d5deea !important;
  background: linear-gradient(180deg, #f4f7fb 0%, #eef3f9 100%) !important;
  font-size: 12px;
}
.stock-main-table :deep(th.el-table__cell),
.stock-main-table :deep(td.el-table__cell) {
  padding-left: 0 !important;
  padding-right: 0 !important;
  text-align: center !important;
}
.stock-main-table :deep(.el-table__cell .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 0 4px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.stock-main-table :deep(.el-switch) {
  height: 22px;
}
.stock-main-table :deep(.el-button--text) {
  padding: 0 2px;
  font-size: 12px;
}
.stock-main-table :deep(.col-batch .cell),
.stock-main-table :deep(.col-area .cell) {
  overflow: visible;
}
.batch-link-btn {
  max-width: 100%;
  padding: 0;
  color: #1769e0;
  border: 0;
  background: transparent;
  font-size: 12px;
  line-height: 1.3;
  cursor: pointer;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.batch-link-btn:hover {
  color: #0f4fad;
  text-decoration: underline;
}
.hub-panel--stock > :deep(.pagination-container) {
  flex: 0 0 auto;
  height: 36px;
  margin-top: 4px !important;
  padding: 2px 0 0 !important;
  background: transparent !important;
}
.hub-panel--stock > :deep(.pagination-container .el-pagination) {
  height: 32px;
  padding: 0;
}
.batch-nav-summary {
  display: none;
}
.batch-nav-summary p {
  margin: 7px 0 0;
  color: #75859a;
  font-size: 12px;
  line-height: 1.5;
}
.batch-nav-section {
  padding: 10px;
}
.hub-aside-hint {
  margin: 0 0 10px;
  color: #7c899c;
  font-size: 12px;
  line-height: 1.5;
}
@media (max-width: 1200px) {
  .wmstock-hub-aside {
    flex-basis: 248px;
    max-width: 248px;
  }
  .stock-search-form :deep(.el-input) {
    width: 140px;
  }
}
@media (max-width: 820px) {
  .wmstock-hub-row {
    height: auto;
    min-height: 0;
    flex-wrap: wrap;
  }
  .wmstock-hub-aside {
    flex-basis: 100%;
    max-width: 100%;
    height: auto;
    border-right: 0;
    padding-right: 0;
    overflow: visible;
  }
  .stock-aside-tree--fill {
    flex: 0 0 auto;
    max-height: 220px;
  }
  .stock-panel-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
}
</style>
