-- yunshu quality: qc_* master data + inspection docs (Sprint Q0+Q1)

CREATE TABLE IF NOT EXISTS qc_index (
  index_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '检测项ID',
  index_code VARCHAR(64) NOT NULL COMMENT '检测项编码',
  index_name VARCHAR(255) NOT NULL COMMENT '检测项名称',
  index_type VARCHAR(64) NOT NULL COMMENT '检测项类型',
  qc_tool VARCHAR(255) NULL COMMENT '检测工具',
  qc_result_type VARCHAR(64) NOT NULL COMMENT '质检值类型',
  qc_result_spc VARCHAR(255) NULL COMMENT '值属性',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (index_id),
  UNIQUE KEY uk_qc_index_code (index_code)
) ENGINE=InnoDB COMMENT='检测项表';

CREATE TABLE IF NOT EXISTS qc_template (
  template_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '检测模板ID',
  template_code VARCHAR(64) NOT NULL COMMENT '检测模板编号',
  template_name VARCHAR(255) NOT NULL COMMENT '检测模板名称',
  qc_types VARCHAR(255) NOT NULL COMMENT '检测种类',
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (template_id),
  UNIQUE KEY uk_qc_template_code (template_code)
) ENGINE=InnoDB COMMENT='检测模板表';

CREATE TABLE IF NOT EXISTS qc_template_index (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  template_id BIGINT UNSIGNED NOT NULL COMMENT '检测模板ID',
  index_id BIGINT UNSIGNED NOT NULL COMMENT '检测项ID',
  index_code VARCHAR(64) NOT NULL COMMENT '检测项编码',
  index_name VARCHAR(255) NOT NULL COMMENT '检测项名称',
  index_type VARCHAR(64) NOT NULL COMMENT '检测项类型',
  qc_tool VARCHAR(255) NULL COMMENT '检测工具',
  check_method VARCHAR(500) NULL COMMENT '检测要求',
  stander_val DOUBLE(12,4) NULL COMMENT '标准值',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  threshold_max DOUBLE(12,4) NULL COMMENT '误差上限',
  threshold_min DOUBLE(12,4) NULL COMMENT '误差下限',
  doc_url VARCHAR(255) NULL COMMENT '说明图',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (record_id),
  KEY idx_qc_template_index_template (template_id),
  KEY idx_qc_template_index_index (index_id)
) ENGINE=InnoDB COMMENT='检测模板-检测项表';

CREATE TABLE IF NOT EXISTS qc_template_product (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  template_id BIGINT UNSIGNED NOT NULL COMMENT '检测模板ID',
  item_id BIGINT UNSIGNED NOT NULL COMMENT '产品物料ID',
  item_code VARCHAR(64) NULL COMMENT '产品物料编码',
  item_name VARCHAR(255) NULL COMMENT '产品物料名称',
  specification VARCHAR(500) NULL COMMENT '规格型号',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  quantity_check INT NOT NULL DEFAULT 1 COMMENT '最低检测数',
  quantity_unqualified INT NOT NULL DEFAULT 0 COMMENT '最大不合格数',
  cr_rate DOUBLE(12,2) NOT NULL DEFAULT 0 COMMENT '最大致命缺陷率',
  maj_rate DOUBLE(12,2) NOT NULL DEFAULT 0 COMMENT '最大严重缺陷率',
  min_rate DOUBLE(12,2) NOT NULL DEFAULT 100 COMMENT '最大轻微缺陷率',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (record_id),
  KEY idx_qc_template_product_template (template_id),
  KEY idx_qc_template_product_item (item_id)
) ENGINE=InnoDB COMMENT='检测模板-产品表';

CREATE TABLE IF NOT EXISTS qc_defect (
  defect_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '缺陷ID',
  defect_code VARCHAR(64) NOT NULL COMMENT '缺陷编码',
  defect_name VARCHAR(500) NOT NULL COMMENT '缺陷描述',
  index_type VARCHAR(64) NOT NULL COMMENT '检测项类型',
  defect_level VARCHAR(64) NOT NULL COMMENT '缺陷等级',
  process_method VARCHAR(500) NULL COMMENT '处置方法',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (defect_id),
  UNIQUE KEY uk_qc_defect_code (defect_code)
) ENGINE=InnoDB COMMENT='常见缺陷表';

CREATE TABLE IF NOT EXISTS qc_iqc (
  iqc_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '来料检验单ID',
  iqc_code VARCHAR(64) NOT NULL COMMENT '来料检验单编号',
  iqc_name VARCHAR(500) NOT NULL COMMENT '来料检验单名称',
  template_id BIGINT UNSIGNED NOT NULL COMMENT '检验模板ID',
  source_doc_id BIGINT UNSIGNED NULL COMMENT '来源单据ID',
  source_doc_type VARCHAR(64) NULL COMMENT '来源单据类型',
  source_doc_code VARCHAR(64) NULL COMMENT '来源单据编号',
  source_line_id BIGINT UNSIGNED NULL COMMENT '来源单据行ID',
  vendor_id BIGINT UNSIGNED NOT NULL COMMENT '供应商ID',
  vendor_code VARCHAR(64) NOT NULL COMMENT '供应商编码',
  vendor_name VARCHAR(255) NOT NULL COMMENT '供应商名称',
  vendor_nick VARCHAR(255) NULL COMMENT '供应商简称',
  vendor_batch VARCHAR(64) NULL COMMENT '供应商批次号',
  item_id BIGINT UNSIGNED NOT NULL COMMENT '产品物料ID',
  item_code VARCHAR(64) NULL COMMENT '产品物料编码',
  item_name VARCHAR(255) NULL COMMENT '产品物料名称',
  specification VARCHAR(500) NULL COMMENT '规格型号',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  quantity_min_check INT NOT NULL DEFAULT 1 COMMENT '最低检测数',
  quantity_max_unqualified INT NOT NULL DEFAULT 0 COMMENT '最大不合格数',
  quantity_recived DOUBLE(12,2) NOT NULL COMMENT '本次接收数量',
  quantity_check INT NULL COMMENT '本次检测数量',
  quantity_qualified INT NOT NULL DEFAULT 0 COMMENT '合格数',
  quantity_unqualified INT NOT NULL DEFAULT 0 COMMENT '不合格数',
  cr_rate DOUBLE(12,2) NOT NULL DEFAULT 0 COMMENT '致命缺陷率',
  maj_rate DOUBLE(12,2) NOT NULL DEFAULT 0 COMMENT '严重缺陷率',
  min_rate DOUBLE(12,2) NOT NULL DEFAULT 0 COMMENT '轻微缺陷率',
  cr_quantity INT NOT NULL DEFAULT 0 COMMENT '致命缺陷数量',
  maj_quantity INT NOT NULL DEFAULT 0 COMMENT '严重缺陷数量',
  min_quantity INT NOT NULL DEFAULT 0 COMMENT '轻微缺陷数量',
  check_result VARCHAR(64) NULL COMMENT '检测结果',
  recive_date DATETIME(3) NULL COMMENT '来料日期',
  inspect_date DATETIME(3) NULL COMMENT '检测日期',
  inspector VARCHAR(64) NULL COMMENT '检测人员',
  status VARCHAR(64) NULL COMMENT '单据状态',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (iqc_id),
  UNIQUE KEY uk_qc_iqc_code (iqc_code),
  KEY idx_qc_iqc_template (template_id),
  KEY idx_qc_iqc_item (item_id)
) ENGINE=InnoDB COMMENT='来料检验单表';

CREATE TABLE IF NOT EXISTS qc_iqc_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  iqc_id BIGINT UNSIGNED NOT NULL COMMENT '检验单ID',
  index_id BIGINT UNSIGNED NOT NULL COMMENT '检测项ID',
  index_code VARCHAR(64) NULL COMMENT '检测项编码',
  index_name VARCHAR(255) NULL COMMENT '检测项名称',
  index_type VARCHAR(64) NULL COMMENT '检测项类型',
  qc_tool VARCHAR(255) NULL COMMENT '检测工具',
  check_method VARCHAR(500) NULL COMMENT '检测要求',
  stander_val DOUBLE(12,4) NULL COMMENT '标准值',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  threshold_max DOUBLE(12,4) NULL COMMENT '误差上限',
  threshold_min DOUBLE(12,4) NULL COMMENT '误差下限',
  cr_quantity INT NOT NULL DEFAULT 0 COMMENT '致命缺陷数量',
  maj_quantity INT NOT NULL DEFAULT 0 COMMENT '严重缺陷数量',
  min_quantity INT NOT NULL DEFAULT 0 COMMENT '轻微缺陷数量',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (line_id),
  KEY idx_qc_iqc_line_iqc (iqc_id)
) ENGINE=InnoDB COMMENT='来料检验单行表';

CREATE TABLE IF NOT EXISTS qc_defect_record (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '缺陷ID',
  qc_type VARCHAR(64) NOT NULL COMMENT '检验单类型',
  qc_id BIGINT UNSIGNED NOT NULL COMMENT '检验单ID',
  line_id BIGINT UNSIGNED NOT NULL COMMENT '检验单行ID',
  defect_name VARCHAR(500) NOT NULL COMMENT '缺陷描述',
  defect_level VARCHAR(64) NOT NULL COMMENT '缺陷等级',
  defect_quantity INT NOT NULL DEFAULT 1 COMMENT '缺陷数量',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (record_id),
  KEY idx_qc_defect_record_qc (qc_type, qc_id)
) ENGINE=InnoDB COMMENT='检验单缺陷记录表';

CREATE TABLE IF NOT EXISTS qc_ipqc (
  ipqc_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '检验单ID',
  ipqc_code VARCHAR(64) NOT NULL COMMENT '检验单编号',
  ipqc_name VARCHAR(255) NULL COMMENT '检验单名称',
  ipqc_type VARCHAR(64) NOT NULL COMMENT '检验类型',
  template_id BIGINT UNSIGNED NOT NULL COMMENT '检验模板ID',
  source_doc_id BIGINT UNSIGNED NULL COMMENT '来源单据ID',
  source_doc_type VARCHAR(64) NULL COMMENT '来源单据类型',
  source_doc_code VARCHAR(64) NULL COMMENT '来源单据编号',
  source_line_id BIGINT UNSIGNED NULL COMMENT '来源单据行ID',
  workorder_id BIGINT UNSIGNED NOT NULL COMMENT '工单ID',
  workorder_code VARCHAR(64) NULL COMMENT '工单编码',
  workorder_name VARCHAR(255) NULL COMMENT '工单名称',
  task_id BIGINT UNSIGNED NULL COMMENT '任务ID',
  task_code VARCHAR(64) NULL COMMENT '任务编号',
  task_name VARCHAR(255) NULL COMMENT '任务名称',
  workstation_id BIGINT UNSIGNED NOT NULL COMMENT '工作站ID',
  workstation_code VARCHAR(64) NULL COMMENT '工作站编号',
  workstation_name VARCHAR(255) NULL COMMENT '工作站名称',
  process_id BIGINT UNSIGNED NULL COMMENT '工序ID',
  process_code VARCHAR(64) NULL COMMENT '工序编码',
  process_name VARCHAR(255) NULL COMMENT '工序名称',
  item_id BIGINT UNSIGNED NOT NULL COMMENT '产品物料ID',
  item_code VARCHAR(64) NULL COMMENT '产品物料编码',
  item_name VARCHAR(255) NULL COMMENT '产品物料名称',
  specification VARCHAR(500) NULL COMMENT '规格型号',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  quantity_check DOUBLE(12,4) NOT NULL DEFAULT 1 COMMENT '检测数量',
  quantity_unqualified DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '不合格数',
  quantity_qualified DOUBLE(12,4) NULL COMMENT '合格品数量',
  cr_rate DOUBLE(12,2) NOT NULL DEFAULT 0 COMMENT '致命缺陷率',
  maj_rate DOUBLE(12,2) NOT NULL DEFAULT 0 COMMENT '严重缺陷率',
  min_rate DOUBLE(12,2) NOT NULL DEFAULT 0 COMMENT '轻微缺陷率',
  cr_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '致命缺陷数量',
  maj_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '严重缺陷数量',
  min_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '轻微缺陷数量',
  check_result VARCHAR(64) NULL COMMENT '检测结果',
  inspect_date DATETIME(3) NULL COMMENT '检测日期',
  inspector VARCHAR(64) NULL COMMENT '检测人员',
  status VARCHAR(64) NULL COMMENT '单据状态',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (ipqc_id),
  UNIQUE KEY uk_qc_ipqc_code (ipqc_code),
  KEY idx_qc_ipqc_template (template_id),
  KEY idx_qc_ipqc_workorder (workorder_id)
) ENGINE=InnoDB COMMENT='过程检验单表';

CREATE TABLE IF NOT EXISTS qc_ipqc_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  ipqc_id BIGINT UNSIGNED NOT NULL COMMENT '检验单ID',
  index_id BIGINT UNSIGNED NOT NULL COMMENT '检测项ID',
  index_code VARCHAR(64) NULL COMMENT '检测项编码',
  index_name VARCHAR(255) NULL COMMENT '检测项名称',
  index_type VARCHAR(64) NULL COMMENT '检测项类型',
  qc_tool VARCHAR(255) NULL COMMENT '检测工具',
  check_method VARCHAR(500) NULL COMMENT '检测要求',
  stander_val DOUBLE(12,4) NULL COMMENT '标准值',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  threshold_max DOUBLE(12,4) NULL COMMENT '误差上限',
  threshold_min DOUBLE(12,4) NULL COMMENT '误差下限',
  cr_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '致命缺陷数量',
  maj_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '严重缺陷数量',
  min_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '轻微缺陷数量',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (line_id),
  KEY idx_qc_ipqc_line_ipqc (ipqc_id)
) ENGINE=InnoDB COMMENT='过程检验单行表';

CREATE TABLE IF NOT EXISTS qc_rqc (
  rqc_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '检验单ID',
  rqc_code VARCHAR(64) NOT NULL COMMENT '检验单编号',
  rqc_name VARCHAR(500) NULL COMMENT '检验单名称',
  template_id BIGINT UNSIGNED NOT NULL COMMENT '检验模板ID',
  source_doc_id BIGINT UNSIGNED NULL COMMENT '来源单据ID',
  source_doc_type VARCHAR(64) NULL COMMENT '来源单据类型',
  source_doc_code VARCHAR(64) NULL COMMENT '来源单据编号',
  source_line_id BIGINT UNSIGNED NULL COMMENT '来源单据行ID',
  rqc_type VARCHAR(64) NULL COMMENT '退料检验类型',
  item_id BIGINT UNSIGNED NOT NULL COMMENT '产品物料ID',
  item_code VARCHAR(64) NULL COMMENT '产品物料编码',
  item_name VARCHAR(255) NULL COMMENT '产品物料名称',
  specification VARCHAR(500) NULL COMMENT '规格型号',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  unit_name VARCHAR(128) NULL COMMENT '单位名称',
  batch_id BIGINT UNSIGNED NULL COMMENT '批次ID',
  batch_code VARCHAR(128) NULL COMMENT '批次号',
  quantity_check DOUBLE(12,4) NOT NULL DEFAULT 1 COMMENT '检测数量',
  quantity_unqualified DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '不合格数',
  quantity_qualified DOUBLE(12,4) NULL COMMENT '合格品数量',
  check_result VARCHAR(64) NULL COMMENT '检测结果',
  inspect_date DATETIME(3) NULL COMMENT '检测日期',
  user_id BIGINT UNSIGNED NULL COMMENT '检测人员ID',
  user_name VARCHAR(64) NULL COMMENT '检测人员名称',
  nick_name VARCHAR(64) NULL COMMENT '检测人员',
  status VARCHAR(64) NULL COMMENT '单据状态',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (rqc_id),
  UNIQUE KEY uk_qc_rqc_code (rqc_code),
  KEY idx_qc_rqc_template (template_id),
  KEY idx_qc_rqc_item (item_id)
) ENGINE=InnoDB COMMENT='退料检验单表';

CREATE TABLE IF NOT EXISTS qc_rqc_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  rqc_id BIGINT UNSIGNED NOT NULL COMMENT '检验单ID',
  index_id BIGINT UNSIGNED NOT NULL COMMENT '检测项ID',
  index_code VARCHAR(64) NULL COMMENT '检测项编码',
  index_name VARCHAR(255) NULL COMMENT '检测项名称',
  index_type VARCHAR(64) NULL COMMENT '检测项类型',
  qc_tool VARCHAR(255) NULL COMMENT '检测工具',
  check_method VARCHAR(500) NULL COMMENT '检测要求',
  stander_val DOUBLE(12,4) NULL COMMENT '标准值',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  threshold_max DOUBLE(12,4) NULL COMMENT '误差上限',
  threshold_min DOUBLE(12,4) NULL COMMENT '误差下限',
  cr_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '致命缺陷数量',
  maj_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '严重缺陷数量',
  min_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '轻微缺陷数量',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (line_id),
  KEY idx_qc_rqc_line_rqc (rqc_id)
) ENGINE=InnoDB COMMENT='退料检验单行表';

CREATE TABLE IF NOT EXISTS qc_oqc (
  oqc_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '出货检验单ID',
  oqc_code VARCHAR(64) NOT NULL COMMENT '出货检验单编号',
  oqc_name VARCHAR(500) NULL COMMENT '出货检验单名称',
  template_id BIGINT UNSIGNED NOT NULL COMMENT '检验模板ID',
  source_doc_id BIGINT UNSIGNED NULL COMMENT '来源单据ID',
  source_doc_type VARCHAR(64) NULL COMMENT '来源单据类型',
  source_doc_code VARCHAR(64) NULL COMMENT '来源单据编号',
  source_line_id BIGINT UNSIGNED NULL COMMENT '来源单据行ID',
  client_id BIGINT UNSIGNED NOT NULL COMMENT '客户ID',
  client_code VARCHAR(64) NOT NULL COMMENT '客户编码',
  client_name VARCHAR(255) NOT NULL COMMENT '客户名称',
  batch_code VARCHAR(64) NULL COMMENT '批次号',
  item_id BIGINT UNSIGNED NOT NULL COMMENT '产品物料ID',
  item_code VARCHAR(64) NULL COMMENT '产品物料编码',
  item_name VARCHAR(255) NULL COMMENT '产品物料名称',
  specification VARCHAR(500) NULL COMMENT '规格型号',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  quantity_min_check DOUBLE(12,4) NOT NULL DEFAULT 1 COMMENT '最低检测数',
  quantity_max_unqualified DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '最大不合格数',
  quantity_out DOUBLE(12,4) NOT NULL COMMENT '发货数量',
  quantity_check DOUBLE(12,4) NOT NULL COMMENT '本次检测数量',
  quantity_unqualified DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '不合格数',
  quantity_quanlified DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '合格数量',
  cr_rate DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '致命缺陷率',
  maj_rate DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '严重缺陷率',
  min_rate DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '轻微缺陷率',
  cr_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '致命缺陷数量',
  maj_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '严重缺陷数量',
  min_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '轻微缺陷数量',
  check_result VARCHAR(64) NULL COMMENT '检测结果',
  out_date DATETIME(3) NULL COMMENT '出货日期',
  inspect_date DATETIME(3) NULL COMMENT '检测日期',
  inspector VARCHAR(64) NULL COMMENT '检测人员',
  status VARCHAR(64) NULL COMMENT '单据状态',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (oqc_id),
  UNIQUE KEY uk_qc_oqc_code (oqc_code),
  KEY idx_qc_oqc_template (template_id),
  KEY idx_qc_oqc_item (item_id)
) ENGINE=InnoDB COMMENT='出货检验单表';

CREATE TABLE IF NOT EXISTS qc_oqc_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  oqc_id BIGINT UNSIGNED NOT NULL COMMENT '检验单ID',
  index_id BIGINT UNSIGNED NOT NULL COMMENT '检测项ID',
  index_code VARCHAR(64) NULL COMMENT '检测项编码',
  index_name VARCHAR(255) NULL COMMENT '检测项名称',
  index_type VARCHAR(64) NULL COMMENT '检测项类型',
  qc_tool VARCHAR(255) NULL COMMENT '检测工具',
  check_method VARCHAR(500) NULL COMMENT '检测要求',
  stander_val DOUBLE(12,4) NULL COMMENT '标准值',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  threshold_max DOUBLE(12,4) NULL COMMENT '误差上限',
  threshold_min DOUBLE(12,4) NULL COMMENT '误差下限',
  cr_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '致命缺陷数量',
  maj_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '严重缺陷数量',
  min_quantity DOUBLE(12,4) NOT NULL DEFAULT 0 COMMENT '轻微缺陷数量',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (line_id),
  KEY idx_qc_oqc_line_oqc (oqc_id)
) ENGINE=InnoDB COMMENT='出货检验单行表';

CREATE TABLE IF NOT EXISTS qc_result (
  result_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  result_code VARCHAR(64) NOT NULL COMMENT '样品编号',
  source_doc_id BIGINT UNSIGNED NULL COMMENT '关联的质检单ID',
  source_doc_code VARCHAR(64) NULL COMMENT '关联的质检单编号',
  source_doc_name VARCHAR(255) NULL COMMENT '关联的质检单名称',
  source_doc_type VARCHAR(64) NULL COMMENT '关联的质检单类型',
  item_id BIGINT UNSIGNED NOT NULL COMMENT '产品物料ID',
  item_code VARCHAR(64) NULL COMMENT '产品物料编码',
  item_name VARCHAR(255) NULL COMMENT '产品物料名称',
  specification VARCHAR(500) NULL COMMENT '规格型号',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  sn_code VARCHAR(255) NULL COMMENT '对应的物资SN',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (result_id),
  UNIQUE KEY uk_qc_result_code (result_code),
  KEY idx_qc_result_item (item_id)
) ENGINE=InnoDB COMMENT='检测结果记录表';

CREATE TABLE IF NOT EXISTS qc_result_detail (
  detail_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水号',
  result_id BIGINT UNSIGNED NOT NULL COMMENT '结果记录ID',
  index_id BIGINT UNSIGNED NOT NULL COMMENT '检测项ID',
  index_code VARCHAR(64) NULL COMMENT '检测项编码',
  index_name VARCHAR(256) NULL COMMENT '检测项名称',
  index_type VARCHAR(64) NULL COMMENT '检测项类型',
  qc_tool VARCHAR(255) NULL COMMENT '检测工具',
  check_method VARCHAR(500) NULL COMMENT '检测要求',
  stander_val DOUBLE(12,4) NULL COMMENT '标准值',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位',
  threshold_max DOUBLE(12,4) NULL COMMENT '误差上限',
  threshold_min DOUBLE(12,4) NULL COMMENT '误差下限',
  qc_result_type VARCHAR(64) NOT NULL COMMENT '质检值类型',
  qc_result_spc VARCHAR(255) NULL COMMENT '值属性',
  qc_val_float FLOAT(14,4) NULL COMMENT '浮点值',
  qc_val_integer INT NULL COMMENT '整数',
  qc_val_text VARCHAR(500) NULL COMMENT '文字',
  qc_val_dict VARCHAR(64) NULL COMMENT '字典项',
  qc_val_file VARCHAR(255) NULL COMMENT '文件',
  defect_flag VARCHAR(64) NOT NULL DEFAULT 'normarl' COMMENT '判定',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (detail_id),
  KEY idx_qc_result_detail_result (result_id)
) ENGINE=InnoDB COMMENT='检测结果明细记录表';

-- seed: detection indexes
INSERT INTO qc_index (index_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, remark, create_by, create_time)
VALUES
  (1, 'QI-APPEAR', '外观检查', '外观', '目视', 'TEXT', NULL, '外观类检测项', 'system', NOW(3)),
  (2, 'QI-DIM', '尺寸检查', '尺寸', '卡尺', 'FLOAT', 'mm', '尺寸类检测项', 'system', NOW(3)),
  (3, 'QI-FUNC', '功能检查', '功能', '测试台', 'DICT', 'PASS_FAIL', '功能类检测项', 'system', NOW(3))
ON DUPLICATE KEY UPDATE index_name = VALUES(index_name);

-- seed: common defects
INSERT INTO qc_defect (defect_id, defect_code, defect_name, index_type, defect_level, process_method, remark, create_by, create_time)
VALUES
  (1, 'DF-SCRATCH', '表面划伤', '外观', 'MIN', '返工抛光', '轻微外观缺陷', 'system', NOW(3)),
  (2, 'DF-DIM-OUT', '尺寸超差', '尺寸', 'MAJ', '返工或报废', '严重尺寸缺陷', 'system', NOW(3))
ON DUPLICATE KEY UPDATE defect_name = VALUES(defect_name);

-- seed: template
INSERT INTO qc_template (template_id, template_code, template_name, qc_types, enable_flag, remark, create_by, create_time)
VALUES
  (1, 'QT-STD-001', '标准全检模板', 'IQC,PQC,OQC,RQC', 'Y', '通用标准检测模板', 'system', NOW(3))
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name);

-- seed: template indexes
INSERT INTO qc_template_index (record_id, template_id, index_id, index_code, index_name, index_type, qc_tool, check_method, stander_val, unit_of_measure, threshold_max, threshold_min, create_by, create_time)
VALUES
  (1, 1, 1, 'QI-APPEAR', '外观检查', '外观', '目视', '无可见划伤、污渍', NULL, NULL, NULL, NULL, 'system', NOW(3)),
  (2, 1, 2, 'QI-DIM', '尺寸检查', '尺寸', '卡尺', '按图纸公差', 10.0000, 'mm', 0.1000, -0.1000, 'system', NOW(3)),
  (3, 1, 3, 'QI-FUNC', '功能检查', '功能', '测试台', '通电功能正常', NULL, NULL, NULL, NULL, 'system', NOW(3))
ON DUPLICATE KEY UPDATE index_name = VALUES(index_name);

-- seed: template product (prefer existing md_item, fallback item_id=7)
INSERT INTO qc_template_product (record_id, template_id, item_id, item_code, item_name, specification, unit_of_measure, quantity_check, quantity_unqualified, cr_rate, maj_rate, min_rate, create_by, create_time)
SELECT 1, 1,
  COALESCE((SELECT item_id FROM md_item ORDER BY item_id LIMIT 1), 7),
  COALESCE((SELECT item_code FROM md_item ORDER BY item_id LIMIT 1), 'ITEM-7'),
  COALESCE((SELECT item_name FROM md_item ORDER BY item_id LIMIT 1), '演示物料'),
  COALESCE((SELECT specification FROM md_item ORDER BY item_id LIMIT 1), ''),
  COALESCE((SELECT unit_of_measure FROM md_item ORDER BY item_id LIMIT 1), 'PCS'),
  5, 0, 0, 0, 100, 'system', NOW(3)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM qc_template_product WHERE record_id = 1);

-- optional sample PREPARE docs
INSERT INTO qc_iqc (iqc_id, iqc_code, iqc_name, template_id, vendor_id, vendor_code, vendor_name, item_id, item_code, item_name, unit_of_measure, quantity_recived, quantity_check, status, create_by, create_time)
SELECT 1, 'IQC-DEMO-001', '演示来料检验单', 1, 1, 'V-DEMO', '演示供应商',
  COALESCE((SELECT item_id FROM md_item ORDER BY item_id LIMIT 1), 7),
  COALESCE((SELECT item_code FROM md_item ORDER BY item_id LIMIT 1), 'ITEM-7'),
  COALESCE((SELECT item_name FROM md_item ORDER BY item_id LIMIT 1), '演示物料'),
  'PCS', 100, 5, 'PREPARE', 'system', NOW(3)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM qc_iqc WHERE iqc_id = 1);

INSERT INTO qc_iqc_line (line_id, iqc_id, index_id, index_code, index_name, index_type, qc_tool, check_method, stander_val, unit_of_measure, threshold_max, threshold_min, create_by, create_time)
SELECT record_id, 1, index_id, index_code, index_name, index_type, qc_tool, check_method, stander_val, unit_of_measure, threshold_max, threshold_min, 'system', NOW(3)
FROM qc_template_index
WHERE template_id = 1
  AND NOT EXISTS (SELECT 1 FROM qc_iqc_line WHERE iqc_id = 1);

INSERT INTO qc_ipqc (ipqc_id, ipqc_code, ipqc_name, ipqc_type, template_id, workorder_id, workorder_code, workorder_name, workstation_id, workstation_code, workstation_name, item_id, item_code, item_name, unit_of_measure, quantity_check, status, create_by, create_time)
SELECT 1, 'IPQC-DEMO-001', '演示过程检验单', 'FIRST', 1, 1, 'WO-DEMO', '演示工单', 1, 'WS-DEMO', '演示工位',
  COALESCE((SELECT item_id FROM md_item ORDER BY item_id LIMIT 1), 7),
  COALESCE((SELECT item_code FROM md_item ORDER BY item_id LIMIT 1), 'ITEM-7'),
  COALESCE((SELECT item_name FROM md_item ORDER BY item_id LIMIT 1), '演示物料'),
  'PCS', 1, 'PREPARE', 'system', NOW(3)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM qc_ipqc WHERE ipqc_id = 1);

INSERT INTO qc_ipqc_line (line_id, ipqc_id, index_id, index_code, index_name, index_type, qc_tool, check_method, stander_val, unit_of_measure, threshold_max, threshold_min, create_by, create_time)
SELECT record_id + 100, 1, index_id, index_code, index_name, index_type, qc_tool, check_method, stander_val, unit_of_measure, threshold_max, threshold_min, 'system', NOW(3)
FROM qc_template_index
WHERE template_id = 1
  AND NOT EXISTS (SELECT 1 FROM qc_ipqc_line WHERE ipqc_id = 1);
