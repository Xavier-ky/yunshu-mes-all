import argparse
import json
from datetime import datetime
from pathlib import Path

import torch
from peft import PeftModel
from transformers import AutoModelForCausalLM, AutoTokenizer


DEFAULT_BASE_MODEL = Path("models") / "Qwen3-0.6B"
DEFAULT_ADAPTER = Path("outputs") / "mes_lora_qwen3_0_6b_clean360_balanced" / "final_adapter"
DEFAULT_OUTPUT = Path("models") / "Qwen3-0.6B-MES-clean360-balanced-merged"


def pick_device(requested_device: str) -> str:
    if requested_device != "auto":
        return requested_device
    return "cuda" if torch.cuda.is_available() else "cpu"


def pick_dtype(dtype_name: str):
    if dtype_name == "auto":
        return torch.float16 if torch.cuda.is_available() else torch.float32
    if dtype_name == "float16":
        return torch.float16
    if dtype_name == "bfloat16":
        return torch.bfloat16
    if dtype_name == "float32":
        return torch.float32
    raise ValueError(f"Unsupported dtype: {dtype_name}")


def validate_paths(base_model: Path, adapter: Path, output_dir: Path, overwrite: bool) -> None:
    if not base_model.exists():
        raise FileNotFoundError(f"Base model not found: {base_model}")
    if not adapter.exists():
        raise FileNotFoundError(f"LoRA adapter not found: {adapter}")
    if output_dir.exists() and any(output_dir.iterdir()) and not overwrite:
        raise FileExistsError(
            f"Output directory already exists and is not empty: {output_dir}\n"
            "Use --overwrite if you really want to replace it."
        )


def write_merge_info(output_dir: Path, args, device: str, dtype: torch.dtype) -> None:
    info = {
        "created_at": datetime.now().isoformat(timespec="seconds"),
        "base_model": str(Path(args.base_model)),
        "adapter": str(Path(args.adapter)),
        "output_dir": str(Path(args.output_dir)),
        "device": device,
        "dtype": str(dtype).replace("torch.", ""),
        "note": "LoRA adapter merged into base model with merge_and_unload. Original base model and adapter are not modified.",
    }
    with (output_dir / "merge_info.json").open("w", encoding="utf-8") as f:
        json.dump(info, f, ensure_ascii=False, indent=2)


def merge_lora(args) -> None:
    base_model = Path(args.base_model)
    adapter = Path(args.adapter)
    output_dir = Path(args.output_dir)
    validate_paths(base_model, adapter, output_dir, args.overwrite)

    device = pick_device(args.device)
    dtype = pick_dtype(args.dtype)

    print("=" * 80)
    print("Merging Qwen3 base model with MES LoRA adapter")
    print(f"Base model : {base_model}")
    print(f"Adapter    : {adapter}")
    print(f"Output     : {output_dir}")
    print(f"Device     : {device}")
    print(f"DType      : {dtype}")
    print("=" * 80)

    tokenizer = AutoTokenizer.from_pretrained(base_model, trust_remote_code=True)
    if tokenizer.pad_token_id is None:
        tokenizer.pad_token_id = tokenizer.eos_token_id

    model_kwargs = {
        "torch_dtype": dtype,
        "trust_remote_code": True,
        "low_cpu_mem_usage": True,
    }
    if device == "cuda":
        model_kwargs["device_map"] = "auto"

    print("Loading base model...")
    base = AutoModelForCausalLM.from_pretrained(base_model, **model_kwargs)
    if device == "cpu":
        base = base.to(device)

    print("Loading LoRA adapter...")
    peft_model = PeftModel.from_pretrained(base, adapter, is_trainable=False)
    peft_model.eval()

    print("Merging adapter into base model...")
    merged_model = peft_model.merge_and_unload()
    merged_model.eval()

    output_dir.mkdir(parents=True, exist_ok=True)
    print("Saving merged model...")
    merged_model.save_pretrained(output_dir, safe_serialization=True)
    tokenizer.save_pretrained(output_dir)
    write_merge_info(output_dir, args, device, dtype)

    print("=" * 80)
    print("Merge completed.")
    print(f"Merged model saved to: {output_dir}")
    print("Original base model and LoRA adapter were not modified.")
    print("=" * 80)


def parse_args():
    parser = argparse.ArgumentParser(
        description="Merge Qwen3-0.6B base model with MES LoRA adapter for faster standalone inference."
    )
    parser.add_argument("--base-model", default=str(DEFAULT_BASE_MODEL), help="Local base model directory")
    parser.add_argument("--adapter", default=str(DEFAULT_ADAPTER), help="LoRA adapter directory")
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT), help="Directory to save merged model")
    parser.add_argument(
        "--device",
        default="auto",
        choices=["auto", "cuda", "cpu"],
        help="Merge device. Use cpu if cuda memory is insufficient.",
    )
    parser.add_argument(
        "--dtype",
        default="auto",
        choices=["auto", "float16", "bfloat16", "float32"],
        help="Model dtype used during merge. auto uses float16 on CUDA and float32 on CPU.",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Allow writing into an existing non-empty output directory.",
    )
    return parser.parse_args()


if __name__ == "__main__":
    merge_lora(parse_args())
