# 云枢智造 MES 本地模型包

这个文件夹是可搬迁的本地模型推理包，包含两类模型：

1. 原始基础模型：`models/Qwen3-0.6B`
2. 最终融合模型：`models/Qwen3-0.6B-MES-clean360-balanced-merged`

推荐在 MES 系统中优先使用最终融合模型。

## 目录结构

```text
mymodel/
  README.md
  models/
    Qwen3-0.6B/
    Qwen3-0.6B-MES-clean360-balanced-merged/
  adapters/
    mes_lora_clean360_balanced/
  scripts/
    chat_base.py
    chat_mes_merged.py
    merge_lora_adapter.py
  config/
    system_prompt.txt
    model_info.json
```

## 环境要求

建议使用已有的 `pytorch` conda 环境，至少需要：

```powershell
pip install torch transformers accelerate peft
```

如果只运行融合后的模型，`peft` 不是必须；如果要重新合并 adapter，则需要 `peft`。

## 运行最终 MES 融合模型

在 `mymodel` 根目录执行：

```powershell
python scripts/chat_mes_merged.py
```

如果当前 `python` 不是 pytorch 环境：

```powershell
D:\Anaconda2024.10\envs\pytorch\python.exe scripts\chat_mes_merged.py
```

单轮提问：

```powershell
python scripts/chat_mes_merged.py --prompt "风扇总装线发现电机异响，MES应该如何处理？"
```

## 运行原始基础模型

```powershell
python scripts/chat_base.py
```

## 说明

- `models/Qwen3-0.6B-MES-clean360-balanced-merged` 已经融合 LoRA adapter，推理时不需要再挂载 adapter。
- `adapters/mes_lora_clean360_balanced` 是最终 adapter 备份，主要用于复现合并或后续研究。
- `config/system_prompt.txt` 是 MES Agent 默认系统提示词，可以按真实 MES 项目需要微调。
