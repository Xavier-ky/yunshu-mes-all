-- AI 对话会话与消息表（移植自 IntelligentDetection，供 Agent 中心聊天使用）
-- 与 Agent 平台的 agent_session/agent_message 解耦，聊天功能自包含。

USE fan_mes;

CREATE TABLE IF NOT EXISTS ai_chat_session (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_uuid CHAR(36) NOT NULL,
  title VARCHAR(255) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  last_message_at DATETIME(3) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'active',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_chat_session_uuid (session_uuid),
  KEY idx_ai_chat_last_message_at (last_message_at),
  KEY idx_ai_chat_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话会话';

CREATE TABLE IF NOT EXISTS ai_chat_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  message_uuid CHAR(36) NOT NULL,
  session_id BIGINT UNSIGNED NOT NULL,
  role VARCHAR(16) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  model_name VARCHAR(64) NULL,
  token_count INT UNSIGNED NULL,
  latency_ms INT UNSIGNED NULL,
  meta JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_chat_message_uuid (message_uuid),
  KEY idx_ai_chat_message_session_id (session_id),
  KEY idx_ai_chat_message_created_at (created_at),
  CONSTRAINT fk_ai_chat_message_session FOREIGN KEY (session_id)
    REFERENCES ai_chat_session (id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话消息';
