"""End-to-end verification for persisted 云枢小智 main-chat conversations.

The script signs in through MES, sends a deterministic system-overview request
to the Agent API, reloads the history through the two history endpoints, and
then deletes only its own verification session and trace.
"""

from __future__ import annotations

import argparse
import uuid

import httpx


def cleanup(session_id: str | None, trace_id: str | None) -> None:
    if not session_id and not trace_id:
        return
    from pathlib import Path
    import sys

    root = Path(__file__).resolve().parents[1]
    sys.path.insert(0, str(root))
    from app.storage.mysql_store import conversation_store

    with conversation_store._connection() as connection:
        with connection.cursor() as cursor:
            if session_id and session_id.isdigit():
                cursor.execute("DELETE FROM agent_message WHERE session_id = %s", (session_id,))
                cursor.execute("DELETE FROM agent_session WHERE session_id = %s", (session_id,))
            if trace_id:
                cursor.execute(
                    "DELETE FROM agent_runtime_state WHERE state_type = 'TRACE' AND state_key = %s",
                    (trace_id,),
                )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mes-url", default="http://127.0.0.1:8080")
    parser.add_argument("--agent-url", default="http://127.0.0.1:8090")
    parser.add_argument("--username", default="supervisor")
    parser.add_argument("--password", required=True)
    args = parser.parse_args()

    marker = f"[MAIN-CHAT-PERSISTENCE-VERIFY-{uuid.uuid4().hex[:10]}] 查询今天系统总览"
    session_id: str | None = None
    trace_id: str | None = None
    try:
        with httpx.Client(timeout=30.0) as client:
            login = client.post(
                f"{args.mes_url}/api/auth/login",
                json={"username": args.username, "password": args.password},
            )
            login.raise_for_status()
            token = login.json()["data"]["token"]
            headers = {"Authorization": f"Bearer {token}"}

            denied = client.post(f"{args.agent_url}/api/agent/chat/stream", json={"message": marker})
            assert denied.status_code == 401, denied.text

            stream_events: list[dict] = []
            pending = ""
            with client.stream(
                "POST",
                f"{args.agent_url}/api/agent/chat/stream",
                headers=headers,
                json={"message": marker, "model": "deepseek", "approval_mode": "risk_only"},
            ) as reply:
                reply.raise_for_status()
                for chunk in reply.iter_text():
                    pending += chunk
                    blocks = pending.split("\n\n")
                    pending = blocks.pop()
                    for block in blocks:
                        line = next((item for item in block.splitlines() if item.startswith("data:")), "")
                        if line:
                            import json

                            stream_events.append(json.loads(line[5:].strip()))
                if pending.strip().startswith("data:"):
                    import json

                    stream_events.append(json.loads(pending[5:].strip()))

            metadata = next(item for item in stream_events if item.get("type") == "meta")
            done = next(item for item in stream_events if item.get("type") == "done")
            answer = "".join(item.get("content", "") for item in stream_events if item.get("type") == "delta")
            assert answer, stream_events
            session_id = str(done["session_id"])
            trace_id = done["trace_id"]

            sessions = client.get(f"{args.agent_url}/api/agent/sessions", headers=headers)
            sessions.raise_for_status()
            assert any(str(item["id"]) == session_id for item in sessions.json()["sessions"])

            history = client.get(f"{args.agent_url}/api/agent/sessions/{session_id}", headers=headers)
            history.raise_for_status()
            messages = history.json()["messages"]
            assert len(messages) >= 2, history.text
            assert messages[-2]["role"] == "user" and messages[-2]["content"] == marker, history.text
            assert messages[-1]["role"] == "assistant" and messages[-1]["content"] == answer, history.text
            print(
                {
                    "anonymous_chat": denied.status_code,
                    "persisted_session_id": session_id,
                    "persisted_messages": len(messages),
                    "history_reload": "passed",
                    "stream_delta_count": sum(1 for item in stream_events if item.get("type") == "delta"),
                    "stream_status": metadata["status"],
                }
            )
    finally:
        cleanup(session_id, trace_id)


if __name__ == "__main__":
    main()
