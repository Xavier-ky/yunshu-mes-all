"""Thread-safe local streaming runtimes for the selectable Qwen companion models."""

from __future__ import annotations

import re
from dataclasses import dataclass
from threading import RLock, Thread
from typing import Any, Iterator

from app.core.config import settings


class LocalQwenError(RuntimeError):
    """Raised when a selected local model cannot be loaded or generate a response."""


@dataclass(frozen=True)
class LocalQwenResult:
    answer: str
    input_tokens: int
    output_tokens: int
    model: str


@dataclass(frozen=True)
class LocalQwenStreamEvent:
    delta: str = ""
    result: LocalQwenResult | None = None


@dataclass(frozen=True)
class LocalModelSpec:
    key: str
    model_name: str
    model_path: Any
    enabled: bool
    max_new_tokens: int
    repetition_penalty: float
    no_repeat_ngram_size: int = 0
    use_repeat_stopper: bool = False


class LocalQwenRuntime:
    """Loads one model lazily and serializes its inference for stable GPU streaming."""

    def __init__(self, spec: LocalModelSpec) -> None:
        self._spec = spec
        self._tokenizer: Any | None = None
        self._model: Any | None = None
        self._device = "not-loaded"
        self._lock = RLock()

    @property
    def model_name(self) -> str:
        return self._spec.model_name

    @property
    def key(self) -> str:
        return self._spec.key

    def status(self) -> dict[str, Any]:
        return {
            "key": self.key,
            "enabled": self._spec.enabled,
            "loaded": self._model is not None,
            "model": self.model_name,
            "model_path": str(self._spec.model_path),
            "device": self._device,
        }

    def warm_up(self) -> None:
        with self._lock:
            self._ensure_loaded()

    def _ensure_loaded(self) -> None:
        if self._model is not None and self._tokenizer is not None:
            return
        if not self._spec.enabled:
            raise LocalQwenError(f"{self.model_name} is disabled by configuration.")
        model_path = self._spec.model_path.expanduser().resolve()
        if not model_path.is_dir():
            raise LocalQwenError(f"{self.model_name} model directory does not exist: {model_path}")
        try:
            import torch
            from transformers import AutoModelForCausalLM, AutoTokenizer

            try:
                tokenizer = AutoTokenizer.from_pretrained(
                    str(model_path), trust_remote_code=True, fix_mistral_regex=True
                )
            except TypeError:
                tokenizer = AutoTokenizer.from_pretrained(str(model_path), trust_remote_code=True)
            if tokenizer.pad_token_id is None:
                tokenizer.pad_token_id = tokenizer.eos_token_id
            cuda_available = torch.cuda.is_available()
            load_kwargs: dict[str, Any] = {"trust_remote_code": True, "dtype": "auto"}
            if cuda_available:
                load_kwargs["device_map"] = "auto"
            model = AutoModelForCausalLM.from_pretrained(str(model_path), **load_kwargs)
            if not cuda_available:
                model = model.to("cpu")
            model.eval()
            model.generation_config.do_sample = False
            model.generation_config.temperature = None
            model.generation_config.top_p = None
            model.generation_config.top_k = None
        except Exception as exc:  # pragma: no cover - device-dependent failure path
            raise LocalQwenError(f"Unable to load {self.model_name}: {exc}") from exc

        self._tokenizer = tokenizer
        self._model = model
        self._device = "cuda" if cuda_available else "cpu"

    @staticmethod
    def _clean_answer(text: str) -> str:
        text = re.sub(r"<think>.*?</think>", "", text, flags=re.IGNORECASE | re.DOTALL)
        text = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", text)
        text = re.sub(r"(?m)^\s{0,3}#{1,6}\s*", "", text)
        text = re.sub(r"(?m)^\s*[-+]\s+", "", text)
        text = text.replace("**", "").replace("__", "").replace("`", "")
        text = text.replace("*", "").replace("_", "")
        text = re.sub(r"\n\s*\n+", "\n", text)
        return text.strip()

    @staticmethod
    def _chat_prompt(tokenizer: Any, messages: list[dict[str, str]]) -> str:
        try:
            return tokenizer.apply_chat_template(
                messages, tokenize=False, add_generation_prompt=True, enable_thinking=False
            )
        except TypeError:
            return tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)

    def _generation_kwargs(self, tokenizer: Any, input_tokens: int) -> dict[str, Any]:
        kwargs: dict[str, Any] = {
            "max_new_tokens": self._spec.max_new_tokens,
            "do_sample": False,
            "repetition_penalty": self._spec.repetition_penalty,
            "pad_token_id": tokenizer.pad_token_id,
            "eos_token_id": tokenizer.eos_token_id,
        }
        if self._spec.no_repeat_ngram_size:
            kwargs["no_repeat_ngram_size"] = self._spec.no_repeat_ngram_size
        if self._spec.use_repeat_stopper:
            from transformers import StoppingCriteria, StoppingCriteriaList

            class RepeatStopper(StoppingCriteria):
                def __init__(self, prompt_length: int) -> None:
                    self.prompt_length = prompt_length

                def __call__(self, input_ids, scores, **kwargs) -> bool:
                    generated = input_ids[0][self.prompt_length :].tolist()
                    if len(generated) < 96:
                        return False
                    for ngram_size in range(6, 25):
                        if len(generated) >= ngram_size * 3:
                            tail = generated[-ngram_size:]
                            if tail == generated[-2 * ngram_size : -ngram_size] == generated[-3 * ngram_size : -2 * ngram_size]:
                                return True
                    return False

            kwargs["stopping_criteria"] = StoppingCriteriaList([RepeatStopper(input_tokens)])
        return kwargs

    def generate(self, messages: list[dict[str, str]]) -> LocalQwenResult:
        with self._lock:
            self._ensure_loaded()
            assert self._tokenizer is not None and self._model is not None
            tokenizer, model = self._tokenizer, self._model
            try:
                prompt = self._chat_prompt(tokenizer, messages)
                inputs = tokenizer([prompt], return_tensors="pt").to(model.device)
                input_tokens = int(inputs.input_ids.shape[-1])
                generated = model.generate(
                    **inputs, **self._generation_kwargs(tokenizer, input_tokens)
                )
                new_tokens = generated[0][input_tokens:]
                answer = self._clean_answer(tokenizer.decode(new_tokens, skip_special_tokens=True))
                if not answer:
                    raise LocalQwenError(f"{self.model_name} returned an empty response.")
                return LocalQwenResult(answer, input_tokens, int(new_tokens.shape[-1]), self.model_name)
            except LocalQwenError:
                raise
            except Exception as exc:  # pragma: no cover - device-dependent failure path
                raise LocalQwenError(f"{self.model_name} generation failed: {exc}") from exc

    def stream_generate(self, messages: list[dict[str, str]]) -> Iterator[LocalQwenStreamEvent]:
        """Yield decoded text chunks immediately, then a cleaned completed answer."""
        with self._lock:
            self._ensure_loaded()
            assert self._tokenizer is not None and self._model is not None
            tokenizer, model = self._tokenizer, self._model
            try:
                from transformers import TextIteratorStreamer

                prompt = self._chat_prompt(tokenizer, messages)
                inputs = tokenizer([prompt], return_tensors="pt").to(model.device)
                input_tokens = int(inputs.input_ids.shape[-1])
                streamer = TextIteratorStreamer(
                    tokenizer, skip_prompt=True, skip_special_tokens=True, timeout=90.0
                )
                errors: list[Exception] = []

                def run_generation() -> None:
                    try:
                        model.generate(
                            **inputs,
                            streamer=streamer,
                            **self._generation_kwargs(tokenizer, input_tokens),
                        )
                    except Exception as exc:  # pragma: no cover - device-dependent failure path
                        errors.append(exc)

                thread = Thread(target=run_generation, daemon=True)
                thread.start()
                chunks: list[str] = []
                for chunk in streamer:
                    chunks.append(chunk)
                    yield LocalQwenStreamEvent(delta=chunk)
                thread.join()
                if errors:
                    raise errors[0]

                answer = self._clean_answer("".join(chunks))
                if not answer:
                    raise LocalQwenError(f"{self.model_name} returned an empty response.")
                yield LocalQwenStreamEvent(
                    result=LocalQwenResult(
                        answer=answer,
                        input_tokens=input_tokens,
                        output_tokens=len(tokenizer.encode(answer, add_special_tokens=False)),
                        model=self.model_name,
                    )
                )
            except LocalQwenError:
                raise
            except Exception as exc:  # pragma: no cover - device-dependent failure path
                raise LocalQwenError(f"{self.model_name} streaming failed: {exc}") from exc


local_qwen = LocalQwenRuntime(
    LocalModelSpec(
        key="qwen3",
        model_name="Qwen3-0.6B",
        model_path=settings.local_qwen_model_path,
        enabled=settings.local_qwen_enabled,
        max_new_tokens=settings.local_qwen_max_new_tokens,
        repetition_penalty=1.08,
    )
)
local_qwenl = LocalQwenRuntime(
    LocalModelSpec(
        key="qwen3l",
        model_name="Qwen3L (MES fine-tuned)",
        model_path=settings.local_qwenl_model_path,
        enabled=settings.local_qwenl_enabled,
        max_new_tokens=settings.local_qwenl_max_new_tokens,
        repetition_penalty=1.12,
        no_repeat_ngram_size=8,
        use_repeat_stopper=True,
    )
)


def get_local_model(model_key: str) -> LocalQwenRuntime:
    models = {"qwen3": local_qwen, "qwen3l": local_qwenl}
    try:
        return models[model_key.lower()]
    except KeyError as exc:
        raise LocalQwenError("Unsupported local model. Choose Qwen3 or Qwen3L.") from exc
