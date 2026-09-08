import argparse
from pathlib import Path
from threading import Thread
from typing import Optional

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer, TextIteratorStreamer


PACKAGE_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MODEL = PACKAGE_ROOT / "models" / "Qwen3-0.6B"


def pick_device() -> str:
    return "cuda" if torch.cuda.is_available() else "cpu"


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
        raise FileNotFoundError(f"Base model path not found: {path}")

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


def build_messages(history, prompt: str, system_prompt: Optional[str]):
    messages = []
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    messages.extend(history)
    messages.append({"role": "user", "content": prompt})
    return messages


@torch.inference_mode()
def stream_answer(tokenizer, model, messages, args) -> str:
    text = tokenizer.apply_chat_template(
        messages,
        tokenize=False,
        add_generation_prompt=True,
        enable_thinking=args.thinking,
    )
    inputs = tokenizer([text], return_tensors="pt").to(model.device)
    input_tokens = int(inputs.input_ids.shape[-1])
    streamer = TextIteratorStreamer(tokenizer, skip_prompt=True, skip_special_tokens=True)

    generation_kwargs = {
        **inputs,
        "streamer": streamer,
        "max_new_tokens": args.max_new_tokens,
        "do_sample": args.do_sample,
        "pad_token_id": tokenizer.pad_token_id,
        "eos_token_id": tokenizer.eos_token_id,
    }
    if args.do_sample:
        generation_kwargs.update(
            {
                "temperature": args.temperature,
                "top_p": args.top_p,
                "top_k": args.top_k,
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
    print("Qwen: ", end="", flush=True)
    for chunk in streamer:
        print(chunk, end="", flush=True)
        chunks.append(chunk)

    thread.join()
    if error_holder:
        raise error_holder[0]

    answer = "".join(chunks).strip()
    output_tokens = len(tokenizer.encode(answer, add_special_tokens=False)) if answer else 0
    print()
    print(f"[tokens] input={input_tokens}, output≈{output_tokens}")
    return answer


def interactive_chat(tokenizer, model, args):
    history = []
    print("Qwen3-0.6B base model chat started. 输入 exit / quit / 退出 结束；输入 /clear 清空上下文。")
    while True:
        prompt = input("\nYou: ").strip()
        if not prompt:
            continue
        if prompt.lower() in {"exit", "quit", "q", "退出"}:
            break
        if prompt == "/clear":
            history.clear()
            print("上下文已清空。")
            continue

        answer = stream_answer(tokenizer, model, build_messages(history, prompt, args.system), args)
        history.append({"role": "user", "content": prompt})
        history.append({"role": "assistant", "content": answer})
        history = history[-args.max_history_turns * 2 :]


def parse_args():
    parser = argparse.ArgumentParser(description="Chat with original Qwen3-0.6B base model.")
    parser.add_argument("--model", default=str(DEFAULT_MODEL), help="Base model directory")
    parser.add_argument("--prompt", help="Single question mode")
    parser.add_argument("--system", help="Optional system prompt")
    parser.add_argument("--max-new-tokens", type=int, default=1024)
    parser.add_argument("--max-history-turns", type=int, default=6)
    parser.add_argument("--do-sample", action="store_true", help="Use sampling decoding")
    parser.add_argument("--temperature", type=float, default=0.7)
    parser.add_argument("--top-p", type=float, default=0.8)
    parser.add_argument("--top-k", type=int, default=20)
    parser.add_argument("--thinking", action="store_true", help="Enable Qwen3 thinking mode")
    return parser.parse_args()


def main():
    args = parse_args()
    print(f"Loading base model: {args.model}")
    tokenizer, model, device = load_model(args.model)
    print(f"Model ready on {device}.")

    if args.prompt:
        stream_answer(tokenizer, model, build_messages([], args.prompt, args.system), args)
        return

    interactive_chat(tokenizer, model, args)


if __name__ == "__main__":
    main()
