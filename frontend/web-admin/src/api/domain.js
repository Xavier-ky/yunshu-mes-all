import { request } from "./request";

// ==================== Factory ====================
export const fetchWorkshops    = (params) => request.get("/factory/workshops", { params });
export const getWorkshop       = (id) => request.get(`/factory/workshops/${id}`);
export const createWorkshop    = (data) => request.post("/factory/workshops", data);
export const updateWorkshop    = (id, data) => request.put(`/factory/workshops/${id}`, data);
export const deleteWorkshop    = (id) => request.delete(`/factory/workshops/${id}`);

export const fetchLines        = (params) => request.get("/factory/lines", { params });
export const getLine           = (id) => request.get(`/factory/lines/${id}`);
export const createLine        = (data) => request.post("/factory/lines", data);
export const updateLine        = (id, data) => request.put(`/factory/lines/${id}`, data);
export const deleteLine        = (id) => request.delete(`/factory/lines/${id}`);

export const fetchWorkstations = (params) => request.get("/factory/workstations", { params });
export const getWorkstation    = (id) => request.get(`/factory/workstations/${id}`);
export const createWorkstation = (data) => request.post("/factory/workstations", data);
export const updateWorkstation = (id, data) => request.put(`/factory/workstations/${id}`, data);
export const deleteWorkstation = (id) => request.delete(`/factory/workstations/${id}`);

export const fetchShifts       = (params) => request.get("/factory/shifts", { params });
export const getShift          = (id) => request.get(`/factory/shifts/${id}`);
export const createShift       = (data) => request.post("/factory/shifts", data);
export const updateShift       = (id, data) => request.put(`/factory/shifts/${id}`, data);
export const deleteShift       = (id) => request.delete(`/factory/shifts/${id}`);

// ==================== Master Data ====================
export const fetchProducts     = (params) => request.get("/master-data/products", { params });
export const getProduct        = (id) => request.get(`/master-data/products/${id}`);
export const createProduct     = (data) => request.post("/master-data/products", data);
export const updateProduct     = (id, data) => request.put(`/master-data/products/${id}`, data);
export const deleteProduct     = (id) => request.delete(`/master-data/products/${id}`);

export const fetchMaterials    = (params) => request.get("/master-data/materials", { params });
export const getMaterial       = (id) => request.get(`/master-data/materials/${id}`);
export const createMaterial    = (data) => request.post("/master-data/materials", data);
export const updateMaterial    = (id, data) => request.put(`/master-data/materials/${id}`, data);
export const deleteMaterial    = (id) => request.delete(`/master-data/materials/${id}`);

export const fetchBoms         = (params) => request.get("/master-data/boms", { params });
export const getBom            = (id) => request.get(`/master-data/boms/${id}`);
export const createBom         = (data) => request.post("/master-data/boms", data);
export const updateBom         = (id, data) => request.put(`/master-data/boms/${id}`, data);
export const deleteBom         = (id) => request.delete(`/master-data/boms/${id}`);

export const fetchUoms         = (params) => request.get("/master-data/uoms", { params });
export const getUom            = (id) => request.get(`/master-data/uoms/${id}`);
export const createUom         = (data) => request.post("/master-data/uoms", data);
export const updateUom         = (id, data) => request.put(`/master-data/uoms/${id}`, data);
export const deleteUom         = (id) => request.delete(`/master-data/uoms/${id}`);

// ==================== Process ====================
export const fetchSteps        = (params) => request.get("/process/steps", { params });
export const getStep           = (id) => request.get(`/process/steps/${id}`);
export const createStep        = (data) => request.post("/process/steps", data);
export const updateStep        = (id, data) => request.put(`/process/steps/${id}`, data);
export const deleteStep        = (id) => request.delete(`/process/steps/${id}`);

export const fetchRoutes       = (params) => request.get("/process/routes", { params });
export const getRoute          = (id) => request.get(`/process/routes/${id}`);
export const createRoute       = (data) => request.post("/process/routes", data);
export const updateRoute       = (id, data) => request.put(`/process/routes/${id}`, data);
export const deleteRoute       = (id) => request.delete(`/process/routes/${id}`);

// ==================== Barcode ====================
export const fetchBarcodeTypes    = (params) => request.get("/barcode/types", { params });
export const getBarcodeType       = (id) => request.get(`/barcode/types/${id}`);
export const createBarcodeType    = (data) => request.post("/barcode/types", data);
export const updateBarcodeType    = (id, data) => request.put(`/barcode/types/${id}`, data);
export const deleteBarcodeType    = (id) => request.delete(`/barcode/types/${id}`);

export const fetchBarcodeRules    = (params) => request.get("/barcode/rules", { params });
export const getBarcodeRule       = (id) => request.get(`/barcode/rules/${id}`);
export const createBarcodeRule    = (data) => request.post("/barcode/rules", data);
export const updateBarcodeRule    = (id, data) => request.put(`/barcode/rules/${id}`, data);
export const deleteBarcodeRule    = (id) => request.delete(`/barcode/rules/${id}`);

export const fetchBarcodeTemplates = (params) => request.get("/barcode/templates", { params });
export const getBarcodeTemplate    = (id) => request.get(`/barcode/templates/${id}`);
export const createBarcodeTemplate = (data) => request.post("/barcode/templates", data);
export const updateBarcodeTemplate = (id, data) => request.put(`/barcode/templates/${id}`, data);
export const deleteBarcodeTemplate = (id) => request.delete(`/barcode/templates/${id}`);

// ==================== Planning ====================
export const fetchOrders          = (params) => request.get("/planning/orders", { params });
export const getOrder             = (id) => request.get(`/planning/orders/${id}`);
export const createOrder          = (data) => request.post("/planning/orders", data);
export const updateOrder          = (id, data) => request.put(`/planning/orders/${id}`, data);
export const deleteOrder          = (id) => request.delete(`/planning/orders/${id}`);

export const fetchWorkOrders      = (params) => request.get("/planning/work-orders", { params });
export const getWorkOrder         = (id) => request.get(`/planning/work-orders/${id}`);
export const createWorkOrder      = (data) => request.post("/planning/work-orders", data);
export const updateWorkOrder      = (id, data) => request.put(`/planning/work-orders/${id}`, data);
export const deleteWorkOrder      = (id) => request.delete(`/planning/work-orders/${id}`);

export const fetchProductionTasks = (params) => request.get("/planning/production-tasks", { params });
export const getProductionTask    = (id) => request.get(`/planning/production-tasks/${id}`);
export const createProductionTask = (data) => request.post("/planning/production-tasks", data);
export const updateProductionTask = (id, data) => request.put(`/planning/production-tasks/${id}`, data);
export const deleteProductionTask = (id) => request.delete(`/planning/production-tasks/${id}`);

export const fetchDispatchTasks   = (params) => request.get("/planning/dispatch-tasks", { params });
export const getDispatchTask      = (id) => request.get(`/planning/dispatch-tasks/${id}`);
export const createDispatchTask   = (data) => request.post("/planning/dispatch-tasks", data);
export const updateDispatchTask   = (id, data) => request.put(`/planning/dispatch-tasks/${id}`, data);
export const deleteDispatchTask   = (id) => request.delete(`/planning/dispatch-tasks/${id}`);

// ==================== Inventory ====================
export const fetchWarehouses      = (params) => request.get("/inventory/warehouses", { params });
export const getWarehouse         = (id) => request.get(`/inventory/warehouses/${id}`);
export const createWarehouse      = (data) => request.post("/inventory/warehouses", data);
export const updateWarehouse      = (id, data) => request.put(`/inventory/warehouses/${id}`, data);
export const deleteWarehouse      = (id) => request.delete(`/inventory/warehouses/${id}`);

export const fetchBatches         = (params) => request.get("/inventory/batches", { params });
export const getBatch             = (id) => request.get(`/inventory/batches/${id}`);
export const createBatch          = (data) => request.post("/inventory/batches", data);
export const updateBatch          = (id, data) => request.put(`/inventory/batches/${id}`, data);
export const deleteBatch          = (id) => request.delete(`/inventory/batches/${id}`);

export const fetchInventoryTransactions = (params) => request.get("/inventory/transactions", { params });

// ==================== Production ====================
export const fetchProductSns      = (params) => request.get("/production/product-sns", { params });
export const getProductSn         = (id) => request.get(`/production/product-sns/${id}`);
export const createProductSn      = (data) => request.post("/production/product-sns", data);
export const updateProductSn      = (id, data) => request.put(`/production/product-sns/${id}`, data);
export const deleteProductSn      = (id) => request.delete(`/production/product-sns/${id}`);

export const fetchProductionReports = (params) => request.get("/production/reports", { params });
export const getProductionReport    = (id) => request.get(`/production/reports/${id}`);
export const createProductionReport = (data) => request.post("/production/reports", data);
export const updateProductionReport = (id, data) => request.put(`/production/reports/${id}`, data);
export const deleteProductionReport = (id) => request.delete(`/production/reports/${id}`);

export const fetchCompletions     = (params) => request.get("/production/completions", { params });
export const getCompletion        = (id) => request.get(`/production/completions/${id}`);
export const createCompletion     = (data) => request.post("/production/completions", data);
export const updateCompletion     = (id, data) => request.put(`/production/completions/${id}`, data);
export const deleteCompletion     = (id) => request.delete(`/production/completions/${id}`);

// ==================== Quality ====================
export const fetchQualityTasks    = (params) => request.get("/quality/tasks", { params });
export const getQualityTask       = (id) => request.get(`/quality/tasks/${id}`);
export const createQualityTask    = (data) => request.post("/quality/tasks", data);
export const updateQualityTask    = (id, data) => request.put(`/quality/tasks/${id}`, data);
export const deleteQualityTask    = (id) => request.delete(`/quality/tasks/${id}`);

export const fetchDefects         = (params) => request.get("/quality/defects", { params });
export const getDefect            = (id) => request.get(`/quality/defects/${id}`);
export const createDefect         = (data) => request.post("/quality/defects", data);
export const updateDefect         = (id, data) => request.put(`/quality/defects/${id}`, data);
export const deleteDefect         = (id) => request.delete(`/quality/defects/${id}`);

export const fetchReworkOrders    = (params) => request.get("/quality/rework-orders", { params });
export const getReworkOrder       = (id) => request.get(`/quality/rework-orders/${id}`);
export const createReworkOrder    = (data) => request.post("/quality/rework-orders", data);
export const updateReworkOrder    = (id, data) => request.put(`/quality/rework-orders/${id}`, data);
export const deleteReworkOrder    = (id) => request.delete(`/quality/rework-orders/${id}`);

// ==================== Andon ====================
export const fetchAndonTypes      = (params) => request.get("/andon/types", { params });
export const getAndonType         = (id) => request.get(`/andon/types/${id}`);
export const createAndonType      = (data) => request.post("/andon/types", data);
export const updateAndonType      = (id, data) => request.put(`/andon/types/${id}`, data);
export const deleteAndonType      = (id) => request.delete(`/andon/types/${id}`);

export const fetchAndonReasons    = (params) => request.get("/andon/reasons", { params });
export const getAndonReason       = (id) => request.get(`/andon/reasons/${id}`);
export const createAndonReason    = (data) => request.post("/andon/reasons", data);
export const updateAndonReason    = (id, data) => request.put(`/andon/reasons/${id}`, data);
export const deleteAndonReason    = (id) => request.delete(`/andon/reasons/${id}`);

export const fetchAndonEvents     = (params) => request.get("/andon/events", { params });
export const createAndonEvent     = (data) => request.post("/andon/events", data);

// ==================== Equipment ====================
export const fetchDeviceCategories = (params) => request.get("/equipment/categories", { params });
export const getDeviceCategory     = (id) => request.get(`/equipment/categories/${id}`);
export const createDeviceCategory  = (data) => request.post("/equipment/categories", data);
export const updateDeviceCategory  = (id, data) => request.put(`/equipment/categories/${id}`, data);
export const deleteDeviceCategory  = (id) => request.delete(`/equipment/categories/${id}`);

export const fetchDevices          = (params) => request.get("/equipment/devices", { params });
export const getDevice             = (id) => request.get(`/equipment/devices/${id}`);
export const createDevice          = (data) => request.post("/equipment/devices", data);
export const updateDevice          = (id, data) => request.put(`/equipment/devices/${id}`, data);
export const deleteDevice          = (id) => request.delete(`/equipment/devices/${id}`);

export const fetchRepairOrders     = (params) => request.get("/equipment/repair-orders", { params });
export const getRepairOrder        = (id) => request.get(`/equipment/repair-orders/${id}`);
export const createRepairOrder     = (data) => request.post("/equipment/repair-orders", data);
export const updateRepairOrder     = (id, data) => request.put(`/equipment/repair-orders/${id}`, data);
export const deleteRepairOrder     = (id) => request.delete(`/equipment/repair-orders/${id}`);

// ==================== Traceability ====================
export const fetchTraceQueryLogs   = (params) => request.get("/traceability/query-logs", { params });
export const traceProduct          = (code) => request.get("/traceability/product", { params: { code } });

// ==================== Reporting ====================
export const fetchReportDefinitions = (params) => request.get("/reporting/definitions", { params });

// ==================== Integration ====================
export const fetchExternalSystems  = (params) => request.get("/integration/systems", { params });
export const getExternalSystem     = (id) => request.get(`/integration/systems/${id}`);
export const createExternalSystem  = (data) => request.post("/integration/systems", data);
export const updateExternalSystem  = (id, data) => request.put(`/integration/systems/${id}`, data);
export const deleteExternalSystem  = (id) => request.delete(`/integration/systems/${id}`);

export const fetchApiEndpoints     = (params) => request.get("/integration/endpoints", { params });
export const fetchSyncLogs         = (params) => request.get("/integration/sync-logs", { params });
