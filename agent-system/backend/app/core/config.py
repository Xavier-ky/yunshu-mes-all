from __future__ import annotations

import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

_BACKEND_ROOT = Path(__file__).resolve().parents[2]
_AGENT_SYSTEM_ROOT = _BACKEND_ROOT.parent


def _local_model_path() -> Path:
    configured = Path(os.getenv("LOCAL_QWEN_MODEL_PATH", ""))
    if not configured:
        return _AGENT_SYSTEM_ROOT / "mymodel" / "models" / "Qwen3-0.6B"
    return configured if configured.is_absolute() else _BACKEND_ROOT / configured


def _local_merged_model_path() -> Path:
    configured = Path(os.getenv("LOCAL_QWENL_MODEL_PATH", ""))
    if not configured:
        return _AGENT_SYSTEM_ROOT / "mymodel" / "models" / "Qwen3-0.6B-MES-clean360-balanced-merged"
    return configured if configured.is_absolute() else _BACKEND_ROOT / configured


class Settings:
    enable_mock_tools: bool = os.getenv("ENABLE_MOCK_TOOLS", "true").lower() == "true"
    mock_llm: bool = os.getenv("MOCK_LLM", "false").lower() == "true"
    default_provider: str = os.getenv("DEFAULT_LLM_PROVIDER", "deepseek")
    spring_boot_base_url: str = os.getenv("SPRING_BOOT_BASE_URL", "http://127.0.0.1:8080")
    companion_jwt_enforce: bool = os.getenv("COMPANION_JWT_ENFORCE", "true").lower() == "true"
    jwt_secret: str = os.getenv(
        "JWT_SECRET", "yunshu-mes-dev-jwt-secret-key-please-change-in-production-2026"
    )
    companion_request_timeout_seconds: float = float(
        os.getenv("COMPANION_REQUEST_TIMEOUT_SECONDS", "4")
    )
    local_qwen_enabled: bool = os.getenv("LOCAL_QWEN_ENABLED", "true").lower() == "true"
    local_qwen_model_path: Path = _local_model_path()
    local_qwen_max_new_tokens: int = int(os.getenv("LOCAL_QWEN_MAX_NEW_TOKENS", "384"))
    local_qwen_max_history_turns: int = int(os.getenv("LOCAL_QWEN_MAX_HISTORY_TURNS", "6"))
    local_qwen_temperature: float = float(os.getenv("LOCAL_QWEN_TEMPERATURE", "0.3"))
    local_qwen_eager_load: bool = os.getenv("LOCAL_QWEN_EAGER_LOAD", "false").lower() == "true"
    local_qwenl_enabled: bool = os.getenv("LOCAL_QWENL_ENABLED", "true").lower() == "true"
    local_qwenl_model_path: Path = _local_merged_model_path()
    local_qwenl_max_new_tokens: int = int(os.getenv("LOCAL_QWENL_MAX_NEW_TOKENS", "384"))
    local_qwenl_eager_load: bool = os.getenv("LOCAL_QWENL_EAGER_LOAD", "false").lower() == "true"
    agent_db_host: str = os.getenv("AGENT_DB_HOST", "127.0.0.1")
    agent_db_port: int = int(os.getenv("AGENT_DB_PORT", "3306"))
    agent_db_name: str = os.getenv("AGENT_DB_NAME", "fan_mes")
    agent_db_user: str = os.getenv("AGENT_DB_USER", "root")
    agent_db_password: str = os.getenv("AGENT_DB_PASSWORD", "")
    qdrant_url: str = os.getenv("QDRANT_URL", "http://127.0.0.1:6333")
    qdrant_api_key: str = os.getenv("QDRANT_API_KEY", "")
    qdrant_collection: str = os.getenv("QDRANT_COLLECTION", "mes_knowledge_v1")
    rag_embedding_dimensions: int = int(os.getenv("RAG_EMBEDDING_DIMENSIONS", "512"))
    rag_embedding_model: str = os.getenv("RAG_EMBEDDING_MODEL", "BAAI/bge-small-zh-v1.5")
    rag_embedding_model_path: Path = Path(
        os.getenv("RAG_EMBEDDING_MODEL_PATH", str(_AGENT_SYSTEM_ROOT / "mymodel" / "models" / "bge-small-zh-v1.5"))
    )
    rag_embedding_batch_size: int = int(os.getenv("RAG_EMBEDDING_BATCH_SIZE", "8"))
    cors_origins: list[str] = os.getenv(
        "AGENT_CORS_ORIGINS", "http://127.0.0.1:5173"
    ).split(",")

    @property
    def providers(self) -> dict[str, dict]:
        return {
            "deepseek": {
                "label": "DeepSeek V4-Pro",
                "short": "DeepSeek V4-Pro",
                "desc": "",
                "tier": "",
                "api_key": os.getenv("DEEPSEEK_API_KEY", ""),
                "base_url": os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"),
                "model": os.getenv("DEEPSEEK_MODEL", "deepseek-v4-pro"),
            },
            "qwen": {
                "label": "Qwen 3.7-Max",
                "short": "Qwen 3.7-Max",
                "desc": "",
                "tier": "",
                "api_key": os.getenv("QWEN_API_KEY", ""),
                "base_url": os.getenv(
                    "QWEN_BASE_URL",
                    "https://ws-9ket38qehg8899i8.cn-beijing.maas.aliyuncs.com/compatible-mode/v1",
                ),
                "model": os.getenv("QWEN_MODEL", "qwen3.7-max"),
            },
            "glm": {
                "label": "智谱 GLM 5.2",
                "short": "智谱 GLM 5.2",
                "desc": "",
                "tier": "",
                "api_key": os.getenv("GLM_API_KEY", ""),
                "base_url": os.getenv("GLM_BASE_URL", "https://open.bigmodel.cn/api/paas/v4"),
                "model": os.getenv("GLM_MODEL", "glm-5.2"),
            },
        }

    def get_provider(self, key: str | None) -> dict | None:
        if not key:
            key = self.default_provider
        return self.providers.get(key)

    @property
    def llm_provider(self) -> str:
        if self.mock_llm:
            return "mock"
        p = self.get_provider(self.default_provider)
        if p and p.get("api_key"):
            return self.default_provider
        return "mock"


settings = Settings()
