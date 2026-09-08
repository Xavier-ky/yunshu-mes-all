import argparse
import re
import warnings
from dataclasses import dataclass
from pathlib import Path
from threading import Thread
from typing import List, Optional

import torch
from transformers import (
    AutoModelForCausalLM,
    AutoTokenizer,
    StoppingCriteria,
    StoppingCriteriaList,
    TextIteratorStreamer,
)


PACKAGE_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MODEL = PACKAGE_ROOT / "models" / "Qwen3-0.6B-MES-clean360-balanced-merged"
DEFAULT_SYSTEM_FILE = PACKAGE_ROOT / "config" / "system_prompt.txt"
EXIT_WORDS = {"exit", "quit", "q", "退出", "结束"}

warnings.filterwarnings(
    "ignore",
    message=".*Torch was not compiled with flash attention.*",
    category=UserWarning,
)


@dataclass
class ChatConfig:
    max_new_tokens: int
    do_sample: bool
    temperature: float
    top_p: float
    top_k: int
    repetition_penalty: float
    no_repeat_ngram_size: int
    enable_thinking: bool
    clean_output: bool


class RepeatStopper(StoppingCriteria):
    def __init__(self, prompt_length: int, min_generated_tokens: int = 96):
        self.prompt_length = prompt_length
        self.min_generated_tokens = min_generated_tokens

    def __call__(self, input_ids, scores, **kwargs) -> bool:
        generated = input_ids[0][self.prompt_length :].tolist()
        if len(generated) < self.min_generated_tokens:
            return False

        for ngram_size in range(6, 25):
            if len(generated) < ngram_size * 3:
                continue
            tail = generated[-ngram_size:]
            prev = generated[-2 * ngram_size : -ngram_size]
            prev_prev = generated[-3 * ngram_size : -2 * ngram_size]
            if tail == prev == prev_prev:
                return True
        return False


def pick_device() -> str:
    return "cuda" if torch.cuda.is_available() else "cpu"


def load_system_prompt(system: Optional[str], system_file: Optional[str]) -> str:
    if system:
        return system.strip()

    path = Path(system_file) if system_file else DEFAULT_SYSTEM_FILE
    if path.exists():
        return path.read_text(encoding="utf-8").strip()

    return "你是云枢智造 MES 系统的智能助手，回答必须结合 MES 流程、现场角色、风险控制和处理闭环。"


def load_tokenizer(model_path: str):
    try:
        tokenizer = AutoTokenizer.from_pretrained(
            model_path,
            trust_remote_code=True,
            fix_mistral_regex=True,
        )
    except TypeError:
        tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)

    if tokenizer.pad_token_id is None:
        tokenizer.pad_token_id = tokenizer.eos_token_id
    return tokenizer


def load_model(model_path: str):
    path = Path(model_path)
    if not path.exists():
        raise FileNotFoundError(f"Merged model path not found: {path}")

    device = pick_device()
    tokenizer = load_tokenizer(str(path))
    model_kwargs = {"dtype": "auto", "trust_remote_code": True}
    if device == "cuda":
        model_kwargs["device_map"] = "auto"

    model = AutoModelForCausalLM.from_pretrained(str(path), **model_kwargs)
    if device == "cpu":
        model = model.to(device)
    model.eval()
    return tokenizer, model, device


def build_messages(history: List[dict], prompt: str, system_prompt: str) -> List[dict]:
    return [{"role": "system", "content": system_prompt}, *history, {"role": "user", "content": prompt}]


def trim_history(history: List[dict], max_history_turns: int) -> List[dict]:
    if max_history_turns <= 0:
        return []
    return history[-max_history_turns * 2 :]


def clean_answer_text(text: str) -> str:
    leak_markers = [
        "The answer must include",
        "Please answer",
        "Evaluation question",
        "Answer in Chinese",
        "Use at most",
        "Do not invent",
        "Closed-loop handling:",
        "system prompt",
        "系统提示词",
        "评测提示词",
    ]
    for marker in leak_markers:
        index = text.find(marker)
        if index >= 0:
            text = text[:index]

    for old, new in {"`": "", "*": "", "#": "", ">": "", "Closed-loop": "闭环"}.items():
        text = text.replace(old, new)

    text = re.sub(r"(?m)^\s*[-+]\s+", "", text)
    text = re.sub(r"\n\s*\n+", "\n", text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\s+([，。！？；：、,.!?;:])", r"\1", text)
    return text.strip()


@torch.inference_mode()
def stream_answer(tokenizer, model, messages: List[dict], config: ChatConfig) -> str:
    text = tokenizer.apply_chat_template(
        messages,
        tokenize=False,
        add_generation_prompt=True,
        enable_thinking=config.enable_thinking,
    )
    inputs = tokenizer([text], return_tensors="pt").to(model.device)
    input_tokens = int(inputs.input_ids.shape[-1])

    streamer = TextIteratorStreamer(tokenizer, skip_prompt=True, skip_special_tokens=True)
    generation_kwargs = {
        **inputs,
        "streamer": streamer,
        "max_new_tokens": config.max_new_tokens,
        "do_sample": config.do_sample,
        "pad_token_id": tokenizer.pad_token_id,
        "eos_token_id": tokenizer.eos_token_id,
        "repetition_penalty": config.repetition_penalty,
        "no_repeat_ngram_size": config.no_repeat_ngram_size,
        "stopping_criteria": StoppingCriteriaList([RepeatStopper(input_tokens)]),
    }
    if config.do_sample:
        generation_kwargs.update(
            {
                "temperature": config.temperature,
                "top_p": config.top_p,
                "top_k": config.top_k,
            }
        )

    error_holder = []

    def generate():
        try:
            model.generate(**generation_kwargs)
        except Exception as exc:
            error_holder.append(exc)

    thread = Thread(target=generate, daemon=True)
    thread.start()

    chunks = []
    print("云枢MES: ", end="", flush=True)
    for chunk in streamer:
        print(chunk, end="", flush=True)
        chunks.append(chunk)

    thread.join()
    if error_holder:
        raise error_holder[0]

    raw_answer = "".join(chunks).strip()
    answer = clean_answer_text(raw_answer) if config.clean_output else raw_answer
    if config.clean_output and answer != raw_answer:
        print("\r" + " " * 120 + "\r", end="")
        print(f"云枢MES: {answer}", end="")

    output_tokens = len(tokenizer.encode(answer, add_special_tokens=False)) if answer else 0
    print()
    print(f"[tokens] input={input_tokens}, output≈{output_tokens}")
    return answer


def interactive_chat(tokenizer, model, system_prompt: str, config: ChatConfig, max_history_turns: int):
    history: List[dict] = []
    print("云枢智造 MES merged model chat started. 输入 exit / quit / 退出 结束；输入 /clear 清空上下文。")
    while True:
        prompt = input("\nYou: ").strip()
        if not prompt:
            continue
        if prompt.lower() in EXIT_WORDS:
            break
        if prompt == "/clear":
            history.clear()
            print("上下文已清空。")
            continue

        messages = build_messages(trim_history(history, max_history_turns), prompt, system_prompt)
        answer = stream_answer(tokenizer, model, messages, config)
        history.append({"role": "user", "content": prompt})
        history.append({"role": "assistant", "content": answer})


def parse_args():
    parser = argparse.ArgumentParser(description="Chat with Yunshu MES merged Qwen3 model.")
    parser.add_argument("--model", default=str(DEFAULT_MODEL), help="Merged model directory")
    parser.add_argument("--prompt", help="Single question mode")
    parser.add_argument("--system", help="Override system prompt")
    parser.add_argument("--system-file", default=str(DEFAULT_SYSTEM_FILE), help="System prompt text file")
    parser.add_argument("--max-new-tokens", type=int, default=1024)
    parser.add_argument("--max-history-turns", type=int, default=6)
    parser.add_argument("--do-sample", action="store_true", help="Use sampling decoding")
    parser.add_argument("--temperature", type=float, default=0.3)
    parser.add_argument("--top-p", type=float, default=0.85)
    parser.add_argument("--top-k", type=int, default=20)
    parser.add_argument("--repetition-penalty", type=float, default=1.12)
    parser.add_argument("--no-repeat-ngram-size", type=int, default=8)
    parser.add_argument("--thinking", action="store_true", help="Enable Qwen3 thinking mode")
    parser.add_argument("--raw", action="store_true", help="Keep raw answer without cleanup")
    return parser.parse_args()


def main():
    args = parse_args()
    config = ChatConfig(
        max_new_tokens=args.max_new_tokens,
        do_sample=args.do_sample,
        temperature=args.temperature,
        top_p=args.top_p,
        top_k=args.top_k,
        repetition_penalty=args.repetition_penalty,
        no_repeat_ngram_size=args.no_repeat_ngram_size,
        enable_thinking=args.thinking,
        clean_output=not args.raw,
    )

    print(f"Loading merged model: {args.model}")
    tokenizer, model, device = load_model(args.model)
    print(f"Model ready on {device}.")
    system_prompt = load_system_prompt(args.system, args.system_file)

    if args.prompt:
        stream_answer(tokenizer, model, build_messages([], args.prompt, system_prompt), config)
        return

    interactive_chat(tokenizer, model, system_prompt, config, args.max_history_turns)


if __name__ == "__main__":
    main()
