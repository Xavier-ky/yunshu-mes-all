-- Immutable, user-owned snapshots for Agent-generated quality reports.
-- Runtime initialization in agent-system mirrors this migration because the
-- current local profile has Flyway disabled.
CREATE TABLE IF NOT EXISTS agent_report_artifact (
  report_id VARCHAR(64) NOT NULL,
  session_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  agent_id BIGINT UNSIGNED NOT NULL,
  report_type VARCHAR(48) NOT NULL,
  status VARCHAR(24) NOT NULL,
  window_days INT NOT NULL,
  queried_at DATETIME(3) NULL,
  snapshot_json MEDIUMTEXT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  ready_at DATETIME(3) NULL,
  exported_at DATETIME(3) NULL,
  expires_at DATETIME(3) NULL,
  PRIMARY KEY (report_id),
  KEY idx_agent_report_session (session_id, created_at),
  KEY idx_agent_report_owner (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent quality report snapshots';
