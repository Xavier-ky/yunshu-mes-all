-- MySQL 8.0+
-- Agent platform schema. Agent data is separated by prefix but kept in the same DB
-- so that application code can join permissions/users while business links stay loose.

USE fan_mes;

CREATE TABLE agent_profile (
  agent_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_code VARCHAR(64) NOT NULL,
  agent_name VARCHAR(128) NOT NULL,
  agent_type VARCHAR(64) NOT NULL COMMENT 'PLANNING/KITTING/PRODUCTION/QUALITY/EQUIPMENT/ANDON/TRACE/REPORT',
  owner_dept_id BIGINT UNSIGNED NULL,
  description VARCHAR(1000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT UNSIGNED NULL,
  updated_by BIGINT UNSIGNED NULL,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  version INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (agent_id),
  UNIQUE KEY uk_agent_profile_code (agent_code),
  KEY idx_agent_profile_type_status (agent_type, status),
  CONSTRAINT fk_agent_profile_dept
    FOREIGN KEY (owner_dept_id) REFERENCES sys_department (dept_id)
) ENGINE=InnoDB COMMENT='Agent主表';

CREATE TABLE agent_version (
  agent_version_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_id BIGINT UNSIGNED NOT NULL,
  version_no VARCHAR(32) NOT NULL,
  release_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  release_note VARCHAR(1000) NULL,
  release_time DATETIME(3) NULL,
  PRIMARY KEY (agent_version_id),
  UNIQUE KEY uk_agent_version (agent_id, version_no),
  CONSTRAINT fk_agent_version_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id)
) ENGINE=InnoDB COMMENT='Agent版本';

CREATE TABLE agent_capability (
  capability_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  capability_code VARCHAR(64) NOT NULL,
  capability_name VARCHAR(128) NOT NULL,
  capability_type VARCHAR(64) NOT NULL COMMENT 'RAG/TOOL_CALL/PLANNING/ANALYSIS/WORKFLOW/MEMORY',
  description VARCHAR(500) NULL,
  PRIMARY KEY (capability_id),
  UNIQUE KEY uk_agent_capability_code (capability_code)
) ENGINE=InnoDB COMMENT='Agent能力定义';

CREATE TABLE agent_capability_map (
  map_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_id BIGINT UNSIGNED NOT NULL,
  capability_id BIGINT UNSIGNED NOT NULL,
  enable_status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  config_json JSON NULL,
  PRIMARY KEY (map_id),
  UNIQUE KEY uk_agent_capability_map (agent_id, capability_id),
  CONSTRAINT fk_agent_capability_map_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id),
  CONSTRAINT fk_agent_capability_map_capability
    FOREIGN KEY (capability_id) REFERENCES agent_capability (capability_id)
) ENGINE=InnoDB COMMENT='Agent能力关系';

CREATE TABLE llm_provider (
  provider_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  provider_code VARCHAR(64) NOT NULL,
  provider_name VARCHAR(128) NOT NULL,
  api_base_url VARCHAR(500) NULL,
  auth_type VARCHAR(32) NOT NULL DEFAULT 'API_KEY',
  secret_ref VARCHAR(255) NULL COMMENT '密钥引用，不保存明文密钥',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (provider_id),
  UNIQUE KEY uk_llm_provider_code (provider_code)
) ENGINE=InnoDB COMMENT='大模型服务商';

CREATE TABLE llm_model (
  model_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  provider_id BIGINT UNSIGNED NOT NULL,
  model_code VARCHAR(128) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  model_type VARCHAR(32) NOT NULL COMMENT 'CHAT/EMBEDDING/RERANK/VISION',
  context_window INT UNSIGNED NULL,
  input_price_per_1k DECIMAL(18,8) NULL,
  output_price_per_1k DECIMAL(18,8) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (model_id),
  UNIQUE KEY uk_llm_model_code (provider_id, model_code),
  CONSTRAINT fk_llm_model_provider
    FOREIGN KEY (provider_id) REFERENCES llm_provider (provider_id)
) ENGINE=InnoDB COMMENT='大模型';

CREATE TABLE agent_model_config (
  model_config_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_id BIGINT UNSIGNED NOT NULL,
  model_id BIGINT UNSIGNED NOT NULL,
  purpose VARCHAR(32) NOT NULL DEFAULT 'CHAT' COMMENT 'CHAT/EMBEDDING/SUMMARY/TOOL_REASONING',
  temperature DECIMAL(4,3) NULL,
  top_p DECIMAL(4,3) NULL,
  max_tokens INT UNSIGNED NULL,
  config_json JSON NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (model_config_id),
  UNIQUE KEY uk_agent_model_config (agent_id, purpose),
  CONSTRAINT fk_agent_model_config_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id),
  CONSTRAINT fk_agent_model_config_model
    FOREIGN KEY (model_id) REFERENCES llm_model (model_id)
) ENGINE=InnoDB COMMENT='Agent模型配置';

CREATE TABLE agent_prompt_template (
  prompt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  prompt_code VARCHAR(64) NOT NULL,
  prompt_name VARCHAR(128) NOT NULL,
  prompt_type VARCHAR(32) NOT NULL COMMENT 'SYSTEM/TASK/TOOL/RAG/EVAL',
  description VARCHAR(500) NULL,
  PRIMARY KEY (prompt_id),
  UNIQUE KEY uk_agent_prompt_code (prompt_code)
) ENGINE=InnoDB COMMENT='提示词模板';

CREATE TABLE agent_prompt_version (
  prompt_version_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  prompt_id BIGINT UNSIGNED NOT NULL,
  agent_id BIGINT UNSIGNED NULL,
  version_no VARCHAR(32) NOT NULL,
  prompt_content MEDIUMTEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by BIGINT UNSIGNED NULL,
  PRIMARY KEY (prompt_version_id),
  UNIQUE KEY uk_prompt_version (prompt_id, agent_id, version_no),
  CONSTRAINT fk_prompt_version_template
    FOREIGN KEY (prompt_id) REFERENCES agent_prompt_template (prompt_id),
  CONSTRAINT fk_prompt_version_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id),
  CONSTRAINT fk_prompt_version_user
    FOREIGN KEY (created_by) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='提示词版本';

CREATE TABLE agent_connector (
  connector_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  connector_code VARCHAR(64) NOT NULL,
  connector_name VARCHAR(128) NOT NULL,
  connector_type VARCHAR(64) NOT NULL COMMENT 'MES_DB/MES_API/ERP_API/REPORT/IOT/VECTOR_STORE',
  auth_type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
  endpoint VARCHAR(500) NULL,
  secret_ref VARCHAR(255) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (connector_id),
  UNIQUE KEY uk_agent_connector_code (connector_code)
) ENGINE=InnoDB COMMENT='Agent连接器';

CREATE TABLE agent_tool (
  tool_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  connector_id BIGINT UNSIGNED NOT NULL,
  tool_code VARCHAR(64) NOT NULL,
  tool_name VARCHAR(128) NOT NULL,
  tool_type VARCHAR(64) NOT NULL COMMENT 'QUERY/COMMAND/REPORT/WORKFLOW',
  risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW' COMMENT 'LOW/MEDIUM/HIGH',
  input_schema_json JSON NULL,
  output_schema_json JSON NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (tool_id),
  UNIQUE KEY uk_agent_tool_code (tool_code),
  KEY idx_agent_tool_connector (connector_id),
  CONSTRAINT fk_agent_tool_connector
    FOREIGN KEY (connector_id) REFERENCES agent_connector (connector_id)
) ENGINE=InnoDB COMMENT='Agent工具定义';

CREATE TABLE agent_tool_permission (
  permission_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_id BIGINT UNSIGNED NOT NULL,
  tool_id BIGINT UNSIGNED NOT NULL,
  allow_scope VARCHAR(255) NOT NULL DEFAULT '*',
  require_approval TINYINT(1) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (permission_id),
  UNIQUE KEY uk_agent_tool_permission (agent_id, tool_id),
  CONSTRAINT fk_agent_tool_permission_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id),
  CONSTRAINT fk_agent_tool_permission_tool
    FOREIGN KEY (tool_id) REFERENCES agent_tool (tool_id)
) ENGINE=InnoDB COMMENT='Agent工具权限';

CREATE TABLE agent_session (
  session_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  session_title VARCHAR(255) NULL,
  session_status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  start_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  end_time DATETIME(3) NULL,
  PRIMARY KEY (session_id),
  KEY idx_agent_session_user_time (user_id, start_time),
  KEY idx_agent_session_agent_time (agent_id, start_time),
  CONSTRAINT fk_agent_session_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id),
  CONSTRAINT fk_agent_session_user
    FOREIGN KEY (user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='Agent会话';

CREATE TABLE agent_message (
  message_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NOT NULL,
  sender_type VARCHAR(32) NOT NULL COMMENT 'USER/AGENT/SYSTEM/TOOL',
  sender_id BIGINT UNSIGNED NULL,
  message_content MEDIUMTEXT NOT NULL,
  token_count INT UNSIGNED NULL,
  send_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (message_id),
  KEY idx_agent_message_session_time (session_id, send_time),
  CONSTRAINT fk_agent_message_session
    FOREIGN KEY (session_id) REFERENCES agent_session (session_id)
) ENGINE=InnoDB COMMENT='Agent消息';

CREATE TABLE agent_attachment (
  attachment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  message_id BIGINT UNSIGNED NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_type VARCHAR(64) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_size BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (attachment_id),
  KEY idx_agent_attachment_message (message_id),
  CONSTRAINT fk_agent_attachment_message
    FOREIGN KEY (message_id) REFERENCES agent_message (message_id)
) ENGINE=InnoDB COMMENT='Agent消息附件';

CREATE TABLE agent_context_binding (
  binding_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NOT NULL,
  biz_object_type VARCHAR(64) NOT NULL COMMENT 'work_order/product_sn/device/andon_event/etc',
  biz_object_id BIGINT UNSIGNED NOT NULL,
  binding_reason VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (binding_id),
  KEY idx_agent_context_session (session_id),
  KEY idx_agent_context_biz (biz_object_type, biz_object_id),
  CONSTRAINT fk_agent_context_session
    FOREIGN KEY (session_id) REFERENCES agent_session (session_id)
) ENGINE=InnoDB COMMENT='Agent会话业务上下文绑定';

CREATE TABLE agent_task (
  task_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NULL,
  agent_id BIGINT UNSIGNED NOT NULL,
  task_type VARCHAR(64) NOT NULL COMMENT 'CHAT/ANALYSIS/TRACE/PLAN/DIAGNOSE/REPORT/AUTO_EVENT',
  task_title VARCHAR(255) NOT NULL,
  task_status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  biz_object_type VARCHAR(64) NULL,
  biz_object_id BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  finished_at DATETIME(3) NULL,
  PRIMARY KEY (task_id),
  KEY idx_agent_task_agent_status (agent_id, task_status),
  KEY idx_agent_task_biz (biz_object_type, biz_object_id),
  CONSTRAINT fk_agent_task_session
    FOREIGN KEY (session_id) REFERENCES agent_session (session_id),
  CONSTRAINT fk_agent_task_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id)
) ENGINE=InnoDB COMMENT='Agent任务';

CREATE TABLE agent_plan (
  plan_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  task_id BIGINT UNSIGNED NOT NULL,
  plan_status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  plan_summary VARCHAR(1000) NULL,
  generated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (plan_id),
  KEY idx_agent_plan_task (task_id),
  CONSTRAINT fk_agent_plan_task
    FOREIGN KEY (task_id) REFERENCES agent_task (task_id)
) ENGINE=InnoDB COMMENT='Agent执行计划';

CREATE TABLE agent_plan_step (
  step_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  plan_id BIGINT UNSIGNED NOT NULL,
  step_seq INT UNSIGNED NOT NULL,
  step_type VARCHAR(64) NOT NULL COMMENT 'THINK/RETRIEVE/TOOL/ASK_APPROVAL/SUMMARIZE',
  step_desc VARCHAR(1000) NOT NULL,
  step_status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  PRIMARY KEY (step_id),
  UNIQUE KEY uk_agent_plan_step_seq (plan_id, step_seq),
  CONSTRAINT fk_agent_plan_step_plan
    FOREIGN KEY (plan_id) REFERENCES agent_plan (plan_id)
) ENGINE=InnoDB COMMENT='Agent计划步骤';

CREATE TABLE agent_action (
  action_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  step_id BIGINT UNSIGNED NOT NULL,
  action_type VARCHAR(64) NOT NULL COMMENT 'QUERY/CALL_TOOL/GENERATE_RECOMMENDATION/CREATE_TICKET/UPDATE_STATUS',
  biz_object_type VARCHAR(64) NULL,
  biz_object_id BIGINT UNSIGNED NULL,
  action_status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  finished_at DATETIME(3) NULL,
  PRIMARY KEY (action_id),
  KEY idx_agent_action_step (step_id),
  KEY idx_agent_action_biz (biz_object_type, biz_object_id),
  CONSTRAINT fk_agent_action_step
    FOREIGN KEY (step_id) REFERENCES agent_plan_step (step_id)
) ENGINE=InnoDB COMMENT='Agent动作';

CREATE TABLE agent_tool_call (
  tool_call_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  action_id BIGINT UNSIGNED NOT NULL,
  tool_id BIGINT UNSIGNED NOT NULL,
  request_payload JSON NULL,
  response_payload JSON NULL,
  call_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  error_message VARCHAR(1000) NULL,
  latency_ms INT UNSIGNED NULL,
  called_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (tool_call_id),
  KEY idx_agent_tool_call_action (action_id),
  KEY idx_agent_tool_call_tool_time (tool_id, called_at),
  CONSTRAINT fk_agent_tool_call_action
    FOREIGN KEY (action_id) REFERENCES agent_action (action_id),
  CONSTRAINT fk_agent_tool_call_tool
    FOREIGN KEY (tool_id) REFERENCES agent_tool (tool_id)
) ENGINE=InnoDB COMMENT='Agent工具调用记录';

CREATE TABLE agent_approval_request (
  approval_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  action_id BIGINT UNSIGNED NOT NULL,
  approver_id BIGINT UNSIGNED NOT NULL,
  approval_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  approval_opinion VARCHAR(1000) NULL,
  requested_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  approval_time DATETIME(3) NULL,
  PRIMARY KEY (approval_id),
  KEY idx_agent_approval_action (action_id),
  KEY idx_agent_approval_approver_status (approver_id, approval_status),
  CONSTRAINT fk_agent_approval_action
    FOREIGN KEY (action_id) REFERENCES agent_action (action_id),
  CONSTRAINT fk_agent_approval_user
    FOREIGN KEY (approver_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='Agent高风险动作审批';

CREATE TABLE agent_action_result (
  result_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  action_id BIGINT UNSIGNED NOT NULL,
  result_status VARCHAR(32) NOT NULL,
  result_summary VARCHAR(1000) NULL,
  result_payload JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (result_id),
  KEY idx_agent_action_result_action (action_id),
  CONSTRAINT fk_agent_action_result_action
    FOREIGN KEY (action_id) REFERENCES agent_action (action_id)
) ENGINE=InnoDB COMMENT='Agent动作结果';

CREATE TABLE knowledge_space (
  knowledge_space_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  space_code VARCHAR(64) NOT NULL,
  space_name VARCHAR(128) NOT NULL,
  biz_domain VARCHAR(64) NOT NULL COMMENT 'PROCESS/QUALITY/EQUIPMENT/REPORT/MES_DOC',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (knowledge_space_id),
  UNIQUE KEY uk_knowledge_space_code (space_code)
) ENGINE=InnoDB COMMENT='知识空间';

CREATE TABLE knowledge_source (
  source_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  knowledge_space_id BIGINT UNSIGNED NOT NULL,
  source_type VARCHAR(64) NOT NULL COMMENT 'FILE/URL/DB/API',
  source_name VARCHAR(255) NOT NULL,
  source_uri VARCHAR(1000) NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (source_id),
  KEY idx_knowledge_source_space (knowledge_space_id),
  CONSTRAINT fk_knowledge_source_space
    FOREIGN KEY (knowledge_space_id) REFERENCES knowledge_space (knowledge_space_id)
) ENGINE=InnoDB COMMENT='知识来源';

CREATE TABLE knowledge_document (
  document_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_id BIGINT UNSIGNED NOT NULL,
  document_title VARCHAR(255) NOT NULL,
  document_type VARCHAR(64) NOT NULL,
  document_hash VARCHAR(128) NULL,
  parser_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  version_no VARCHAR(32) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (document_id),
  KEY idx_knowledge_document_source (source_id),
  CONSTRAINT fk_knowledge_document_source
    FOREIGN KEY (source_id) REFERENCES knowledge_source (source_id)
) ENGINE=InnoDB COMMENT='知识文档';

CREATE TABLE knowledge_chunk (
  chunk_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  document_id BIGINT UNSIGNED NOT NULL,
  chunk_seq INT UNSIGNED NOT NULL,
  chunk_text MEDIUMTEXT NOT NULL,
  chunk_hash VARCHAR(128) NOT NULL,
  token_count INT UNSIGNED NULL,
  metadata_json JSON NULL,
  PRIMARY KEY (chunk_id),
  UNIQUE KEY uk_knowledge_chunk_hash (chunk_hash),
  UNIQUE KEY uk_knowledge_chunk_seq (document_id, chunk_seq),
  CONSTRAINT fk_knowledge_chunk_document
    FOREIGN KEY (document_id) REFERENCES knowledge_document (document_id)
) ENGINE=InnoDB COMMENT='知识切片';

CREATE TABLE knowledge_embedding (
  embedding_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  chunk_id BIGINT UNSIGNED NOT NULL,
  model_id BIGINT UNSIGNED NOT NULL,
  vector_store_id VARCHAR(128) NOT NULL COMMENT '外部向量库集合ID',
  vector_id VARCHAR(128) NOT NULL COMMENT '外部向量ID',
  embedding_status VARCHAR(32) NOT NULL DEFAULT 'READY',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (embedding_id),
  UNIQUE KEY uk_knowledge_embedding_vector (vector_store_id, vector_id),
  KEY idx_knowledge_embedding_chunk (chunk_id),
  CONSTRAINT fk_knowledge_embedding_chunk
    FOREIGN KEY (chunk_id) REFERENCES knowledge_chunk (chunk_id),
  CONSTRAINT fk_knowledge_embedding_model
    FOREIGN KEY (model_id) REFERENCES llm_model (model_id)
) ENGINE=InnoDB COMMENT='知识向量索引映射';

CREATE TABLE agent_retrieval_query (
  retrieval_query_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NULL,
  task_id BIGINT UNSIGNED NULL,
  agent_id BIGINT UNSIGNED NOT NULL,
  query_text TEXT NOT NULL,
  query_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (retrieval_query_id),
  KEY idx_agent_retrieval_agent_time (agent_id, query_time),
  CONSTRAINT fk_agent_retrieval_session
    FOREIGN KEY (session_id) REFERENCES agent_session (session_id),
  CONSTRAINT fk_agent_retrieval_task
    FOREIGN KEY (task_id) REFERENCES agent_task (task_id),
  CONSTRAINT fk_agent_retrieval_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id)
) ENGINE=InnoDB COMMENT='Agent检索请求';

CREATE TABLE agent_retrieval_result (
  retrieval_result_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  retrieval_query_id BIGINT UNSIGNED NOT NULL,
  chunk_id BIGINT UNSIGNED NOT NULL,
  score DECIMAL(9,6) NOT NULL,
  rank_no INT UNSIGNED NOT NULL,
  PRIMARY KEY (retrieval_result_id),
  KEY idx_retrieval_result_query (retrieval_query_id),
  CONSTRAINT fk_retrieval_result_query
    FOREIGN KEY (retrieval_query_id) REFERENCES agent_retrieval_query (retrieval_query_id),
  CONSTRAINT fk_retrieval_result_chunk
    FOREIGN KEY (chunk_id) REFERENCES knowledge_chunk (chunk_id)
) ENGINE=InnoDB COMMENT='Agent检索结果';

CREATE TABLE agent_memory (
  memory_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_id BIGINT UNSIGNED NOT NULL,
  memory_type VARCHAR(64) NOT NULL COMMENT 'USER_PREF/BIZ_RULE/CASE_PATTERN/FACT',
  memory_content MEDIUMTEXT NOT NULL,
  confidence DECIMAL(9,6) NOT NULL DEFAULT 1,
  source_type VARCHAR(64) NULL,
  expire_time DATETIME(3) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (memory_id),
  KEY idx_agent_memory_agent_type (agent_id, memory_type),
  CONSTRAINT fk_agent_memory_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id)
) ENGINE=InnoDB COMMENT='Agent长期记忆';

CREATE TABLE agent_memory_tag (
  tag_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  memory_id BIGINT UNSIGNED NOT NULL,
  tag_name VARCHAR(64) NOT NULL,
  PRIMARY KEY (tag_id),
  UNIQUE KEY uk_agent_memory_tag (memory_id, tag_name),
  CONSTRAINT fk_agent_memory_tag_memory
    FOREIGN KEY (memory_id) REFERENCES agent_memory (memory_id)
) ENGINE=InnoDB COMMENT='Agent记忆标签';

CREATE TABLE agent_memory_link (
  memory_link_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  memory_id BIGINT UNSIGNED NOT NULL,
  biz_object_type VARCHAR(64) NOT NULL,
  biz_object_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (memory_link_id),
  KEY idx_memory_link_biz (biz_object_type, biz_object_id),
  CONSTRAINT fk_memory_link_memory
    FOREIGN KEY (memory_id) REFERENCES agent_memory (memory_id)
) ENGINE=InnoDB COMMENT='Agent记忆业务关联';

CREATE TABLE agent_case (
  case_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_id BIGINT UNSIGNED NOT NULL,
  case_type VARCHAR(64) NOT NULL,
  case_title VARCHAR(255) NOT NULL,
  problem_desc TEXT NOT NULL,
  solution_summary TEXT NULL,
  outcome VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (case_id),
  KEY idx_agent_case_agent_type (agent_id, case_type),
  CONSTRAINT fk_agent_case_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id)
) ENGINE=InnoDB COMMENT='Agent案例库';

CREATE TABLE agent_case_step (
  case_step_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  case_id BIGINT UNSIGNED NOT NULL,
  step_seq INT UNSIGNED NOT NULL,
  step_content TEXT NOT NULL,
  PRIMARY KEY (case_step_id),
  UNIQUE KEY uk_agent_case_step_seq (case_id, step_seq),
  CONSTRAINT fk_agent_case_step_case
    FOREIGN KEY (case_id) REFERENCES agent_case (case_id)
) ENGINE=InnoDB COMMENT='Agent案例步骤';

CREATE TABLE agent_event_subscription (
  subscription_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_id BIGINT UNSIGNED NOT NULL,
  event_type VARCHAR(64) NOT NULL COMMENT 'WORK_ORDER_CREATED/ANDON_OPENED/QUALITY_FAILED/DEVICE_FAULT',
  filter_expr VARCHAR(1000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (subscription_id),
  KEY idx_agent_event_subscription_agent (agent_id),
  CONSTRAINT fk_agent_event_subscription_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id)
) ENGINE=InnoDB COMMENT='Agent事件订阅';

CREATE TABLE agent_event_inbox (
  event_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  subscription_id BIGINT UNSIGNED NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  biz_object_type VARCHAR(64) NOT NULL,
  biz_object_id BIGINT UNSIGNED NOT NULL,
  event_payload JSON NULL,
  consume_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  consumed_at DATETIME(3) NULL,
  PRIMARY KEY (event_id),
  KEY idx_agent_event_inbox_status (consume_status, created_at),
  KEY idx_agent_event_inbox_biz (biz_object_type, biz_object_id),
  CONSTRAINT fk_agent_event_inbox_subscription
    FOREIGN KEY (subscription_id) REFERENCES agent_event_subscription (subscription_id)
) ENGINE=InnoDB COMMENT='Agent事件收件箱';

CREATE TABLE agent_workflow (
  workflow_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workflow_code VARCHAR(64) NOT NULL,
  workflow_name VARCHAR(128) NOT NULL,
  workflow_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  PRIMARY KEY (workflow_id),
  UNIQUE KEY uk_agent_workflow_code (workflow_code)
) ENGINE=InnoDB COMMENT='Agent工作流';

CREATE TABLE agent_workflow_node (
  node_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workflow_id BIGINT UNSIGNED NOT NULL,
  node_code VARCHAR(64) NOT NULL,
  node_type VARCHAR(64) NOT NULL COMMENT 'AGENT/TOOL/CONDITION/APPROVAL',
  agent_id BIGINT UNSIGNED NULL,
  tool_id BIGINT UNSIGNED NULL,
  config_json JSON NULL,
  PRIMARY KEY (node_id),
  UNIQUE KEY uk_workflow_node_code (workflow_id, node_code),
  CONSTRAINT fk_workflow_node_workflow
    FOREIGN KEY (workflow_id) REFERENCES agent_workflow (workflow_id),
  CONSTRAINT fk_workflow_node_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id),
  CONSTRAINT fk_workflow_node_tool
    FOREIGN KEY (tool_id) REFERENCES agent_tool (tool_id)
) ENGINE=InnoDB COMMENT='Agent工作流节点';

CREATE TABLE agent_workflow_edge (
  edge_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workflow_id BIGINT UNSIGNED NOT NULL,
  from_node_id BIGINT UNSIGNED NOT NULL,
  to_node_id BIGINT UNSIGNED NOT NULL,
  condition_expr VARCHAR(1000) NULL,
  PRIMARY KEY (edge_id),
  KEY idx_workflow_edge_workflow (workflow_id),
  CONSTRAINT fk_workflow_edge_workflow
    FOREIGN KEY (workflow_id) REFERENCES agent_workflow (workflow_id),
  CONSTRAINT fk_workflow_edge_from
    FOREIGN KEY (from_node_id) REFERENCES agent_workflow_node (node_id),
  CONSTRAINT fk_workflow_edge_to
    FOREIGN KEY (to_node_id) REFERENCES agent_workflow_node (node_id)
) ENGINE=InnoDB COMMENT='Agent工作流连线';

CREATE TABLE agent_workflow_run (
  workflow_run_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workflow_id BIGINT UNSIGNED NOT NULL,
  trigger_task_id BIGINT UNSIGNED NULL,
  run_status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
  start_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  end_time DATETIME(3) NULL,
  PRIMARY KEY (workflow_run_id),
  KEY idx_workflow_run_workflow_time (workflow_id, start_time),
  CONSTRAINT fk_workflow_run_workflow
    FOREIGN KEY (workflow_id) REFERENCES agent_workflow (workflow_id),
  CONSTRAINT fk_workflow_run_task
    FOREIGN KEY (trigger_task_id) REFERENCES agent_task (task_id)
) ENGINE=InnoDB COMMENT='Agent工作流执行';

CREATE TABLE agent_workflow_node_run (
  node_run_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workflow_run_id BIGINT UNSIGNED NOT NULL,
  node_id BIGINT UNSIGNED NOT NULL,
  run_status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
  run_output JSON NULL,
  start_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  end_time DATETIME(3) NULL,
  PRIMARY KEY (node_run_id),
  KEY idx_node_run_workflow_run (workflow_run_id),
  CONSTRAINT fk_node_run_workflow_run
    FOREIGN KEY (workflow_run_id) REFERENCES agent_workflow_run (workflow_run_id),
  CONSTRAINT fk_node_run_node
    FOREIGN KEY (node_id) REFERENCES agent_workflow_node (node_id)
) ENGINE=InnoDB COMMENT='Agent工作流节点执行';

CREATE TABLE agent_recommendation (
  recommendation_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  task_id BIGINT UNSIGNED NOT NULL,
  recommendation_type VARCHAR(64) NOT NULL COMMENT 'SCHEDULE/KITTING/QUALITY/EQUIPMENT/ANDON/TRACE',
  biz_object_type VARCHAR(64) NULL,
  biz_object_id BIGINT UNSIGNED NULL,
  recommendation_text TEXT NOT NULL,
  confidence DECIMAL(9,6) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PROPOSED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (recommendation_id),
  KEY idx_recommendation_task (task_id),
  KEY idx_recommendation_biz (biz_object_type, biz_object_id),
  CONSTRAINT fk_recommendation_task
    FOREIGN KEY (task_id) REFERENCES agent_task (task_id)
) ENGINE=InnoDB COMMENT='Agent建议';

CREATE TABLE agent_recommendation_evidence (
  evidence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  recommendation_id BIGINT UNSIGNED NOT NULL,
  evidence_type VARCHAR(64) NOT NULL COMMENT 'DB_RECORD/DOCUMENT/TOOL_RESULT/METRIC',
  evidence_ref VARCHAR(255) NOT NULL,
  evidence_summary TEXT NULL,
  PRIMARY KEY (evidence_id),
  KEY idx_recommendation_evidence_rec (recommendation_id),
  CONSTRAINT fk_recommendation_evidence_rec
    FOREIGN KEY (recommendation_id) REFERENCES agent_recommendation (recommendation_id)
) ENGINE=InnoDB COMMENT='Agent建议依据';

CREATE TABLE agent_risk_alert (
  alert_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  task_id BIGINT UNSIGNED NULL,
  alert_type VARCHAR(64) NOT NULL,
  severity VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  biz_object_type VARCHAR(64) NULL,
  biz_object_id BIGINT UNSIGNED NULL,
  alert_content TEXT NOT NULL,
  alert_status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (alert_id),
  KEY idx_agent_alert_status (alert_status, severity),
  KEY idx_agent_alert_biz (biz_object_type, biz_object_id),
  CONSTRAINT fk_agent_alert_task
    FOREIGN KEY (task_id) REFERENCES agent_task (task_id)
) ENGINE=InnoDB COMMENT='Agent风险预警';

CREATE TABLE agent_decision_record (
  decision_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  task_id BIGINT UNSIGNED NOT NULL,
  decision_user_id BIGINT UNSIGNED NOT NULL,
  decision_content TEXT NOT NULL,
  decision_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (decision_id),
  KEY idx_decision_task (task_id),
  CONSTRAINT fk_decision_task
    FOREIGN KEY (task_id) REFERENCES agent_task (task_id),
  CONSTRAINT fk_decision_user
    FOREIGN KEY (decision_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='Agent建议人工决策记录';

CREATE TABLE agent_policy (
  policy_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  policy_code VARCHAR(64) NOT NULL,
  policy_name VARCHAR(128) NOT NULL,
  policy_type VARCHAR(64) NOT NULL COMMENT 'SECURITY/APPROVAL/DATA_SCOPE/TOOL_LIMIT',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (policy_id),
  UNIQUE KEY uk_agent_policy_code (policy_code)
) ENGINE=InnoDB COMMENT='Agent策略';

CREATE TABLE agent_policy_binding (
  binding_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  policy_id BIGINT UNSIGNED NOT NULL,
  agent_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (binding_id),
  UNIQUE KEY uk_agent_policy_binding (policy_id, agent_id),
  CONSTRAINT fk_policy_binding_policy
    FOREIGN KEY (policy_id) REFERENCES agent_policy (policy_id),
  CONSTRAINT fk_policy_binding_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id)
) ENGINE=InnoDB COMMENT='Agent策略绑定';

CREATE TABLE agent_guardrail_rule (
  rule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  policy_id BIGINT UNSIGNED NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_type VARCHAR(64) NOT NULL COMMENT 'DENY_TOOL/REQUIRE_APPROVAL/DATA_MASK/OUTPUT_CHECK',
  rule_content TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (rule_id),
  UNIQUE KEY uk_guardrail_rule_code (rule_code),
  CONSTRAINT fk_guardrail_rule_policy
    FOREIGN KEY (policy_id) REFERENCES agent_policy (policy_id)
) ENGINE=InnoDB COMMENT='Agent安全规则';

CREATE TABLE agent_evaluation_case (
  eval_case_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  case_code VARCHAR(64) NOT NULL,
  scenario_type VARCHAR(64) NOT NULL,
  input_content TEXT NOT NULL,
  expected_output TEXT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (eval_case_id),
  UNIQUE KEY uk_eval_case_code (case_code)
) ENGINE=InnoDB COMMENT='Agent评估用例';

CREATE TABLE agent_evaluation_run (
  eval_run_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  eval_case_id BIGINT UNSIGNED NOT NULL,
  agent_id BIGINT UNSIGNED NOT NULL,
  score DECIMAL(9,6) NULL,
  result_status VARCHAR(32) NOT NULL,
  result_detail JSON NULL,
  run_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (eval_run_id),
  KEY idx_eval_run_agent_time (agent_id, run_time),
  CONSTRAINT fk_eval_run_case
    FOREIGN KEY (eval_case_id) REFERENCES agent_evaluation_case (eval_case_id),
  CONSTRAINT fk_eval_run_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id)
) ENGINE=InnoDB COMMENT='Agent评估结果';

CREATE TABLE agent_feedback (
  feedback_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  rating INT UNSIGNED NOT NULL,
  feedback_content TEXT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (feedback_id),
  KEY idx_agent_feedback_session (session_id),
  CONSTRAINT fk_agent_feedback_session
    FOREIGN KEY (session_id) REFERENCES agent_session (session_id),
  CONSTRAINT fk_agent_feedback_user
    FOREIGN KEY (user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='Agent用户反馈';

CREATE TABLE agent_cost_usage (
  cost_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_id BIGINT UNSIGNED NOT NULL,
  session_id BIGINT UNSIGNED NULL,
  task_id BIGINT UNSIGNED NULL,
  model_id BIGINT UNSIGNED NOT NULL,
  input_tokens BIGINT UNSIGNED NOT NULL DEFAULT 0,
  output_tokens BIGINT UNSIGNED NOT NULL DEFAULT 0,
  cost_amount DECIMAL(18,8) NULL,
  currency VARCHAR(16) NOT NULL DEFAULT 'USD',
  usage_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (cost_id),
  KEY idx_agent_cost_agent_time (agent_id, usage_time),
  CONSTRAINT fk_agent_cost_agent
    FOREIGN KEY (agent_id) REFERENCES agent_profile (agent_id),
  CONSTRAINT fk_agent_cost_session
    FOREIGN KEY (session_id) REFERENCES agent_session (session_id),
  CONSTRAINT fk_agent_cost_task
    FOREIGN KEY (task_id) REFERENCES agent_task (task_id),
  CONSTRAINT fk_agent_cost_model
    FOREIGN KEY (model_id) REFERENCES llm_model (model_id)
) ENGINE=InnoDB COMMENT='Agent模型用量成本';

CREATE TABLE agent_audit_log (
  audit_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  action_id BIGINT UNSIGNED NULL,
  session_id BIGINT UNSIGNED NULL,
  audit_type VARCHAR(64) NOT NULL,
  risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
  audit_content TEXT NOT NULL,
  audit_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (audit_id),
  KEY idx_agent_audit_action (action_id),
  KEY idx_agent_audit_session_time (session_id, audit_time),
  CONSTRAINT fk_agent_audit_action
    FOREIGN KEY (action_id) REFERENCES agent_action (action_id),
  CONSTRAINT fk_agent_audit_session
    FOREIGN KEY (session_id) REFERENCES agent_session (session_id)
) ENGINE=InnoDB COMMENT='Agent审计日志';
