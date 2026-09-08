-- P0 Agent durable runtime state.
--
-- The existing agent_* tables remain the audit model for conversations, tasks,
-- tools and approvals.  These two tables add the state snapshot that a
-- LangGraph run needs in order to resume safely after the Python service
-- restarts.  They intentionally do not reference MES business tables: all
-- MES reads/writes must continue to pass through authenticated application
-- APIs and domain services.

USE fan_mes;

CREATE TABLE IF NOT EXISTS agent_graph_checkpoint (
  checkpoint_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  thread_id VARCHAR(128) NOT NULL COMMENT 'LangGraph thread/session identity',
  checkpoint_token VARCHAR(128) NOT NULL COMMENT 'stable checkpoint identity',
  parent_checkpoint_token VARCHAR(128) NULL,
  graph_code VARCHAR(64) NOT NULL COMMENT 'e.g. MES_P0_ORCHESTRATOR',
  graph_version VARCHAR(32) NOT NULL,
  session_id BIGINT UNSIGNED NULL,
  task_id BIGINT UNSIGNED NULL,
  user_id BIGINT UNSIGNED NULL,
  biz_object_type VARCHAR(64) NULL,
  biz_object_id BIGINT UNSIGNED NULL,
  checkpoint_status VARCHAR(32) NOT NULL DEFAULT 'RUNNING'
    COMMENT 'RUNNING/INTERRUPTED/COMPLETED/FAILED/CANCELLED',
  state_json JSON NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (checkpoint_id),
  UNIQUE KEY uk_agent_graph_checkpoint_token (thread_id, checkpoint_token),
  KEY idx_agent_graph_checkpoint_thread_time (thread_id, created_at),
  KEY idx_agent_graph_checkpoint_status_time (checkpoint_status, updated_at),
  KEY idx_agent_graph_checkpoint_task (task_id),
  CONSTRAINT fk_agent_graph_checkpoint_session
    FOREIGN KEY (session_id) REFERENCES agent_session (session_id),
  CONSTRAINT fk_agent_graph_checkpoint_task
    FOREIGN KEY (task_id) REFERENCES agent_task (task_id),
  CONSTRAINT fk_agent_graph_checkpoint_user
    FOREIGN KEY (user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='LangGraph durable checkpoints';

CREATE TABLE IF NOT EXISTS agent_runtime_state (
  runtime_state_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  state_type VARCHAR(32) NOT NULL COMMENT 'TRACE/CONFIRMATION/STREAM',
  state_key VARCHAR(128) NOT NULL,
  owner_user_id BIGINT UNSIGNED NULL,
  task_id BIGINT UNSIGNED NULL,
  state_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  payload_json JSON NOT NULL,
  expires_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (runtime_state_id),
  UNIQUE KEY uk_agent_runtime_state_key (state_type, state_key),
  KEY idx_agent_runtime_state_owner_time (owner_user_id, updated_at),
  KEY idx_agent_runtime_state_status_expiry (state_status, expires_at),
  CONSTRAINT fk_agent_runtime_state_user
    FOREIGN KEY (owner_user_id) REFERENCES sys_user (user_id),
  CONSTRAINT fk_agent_runtime_state_task
    FOREIGN KEY (task_id) REFERENCES agent_task (task_id)
) ENGINE=InnoDB COMMENT='Agent transient state persisted across service restarts';
