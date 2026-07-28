#!/usr/bin/env python3

import argparse
import difflib
import fcntl
import hashlib
import html
import json
import os
import shutil
import stat
import subprocess
import sys
import tempfile
from contextlib import contextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


SNAPSHOT_VERSION = 3
LEGACY_SNAPSHOT_VERSION = 2
REVIEW_VERSION = 1
MAX_SUMMARY_CHARACTERS = 500
REQUIREMENT_KINDS = {"MUST", "FORBIDDEN", "UNCHANGED"}
REVIEW_EVIDENCE_SOURCES = {
    "raw_request",
    "original_contract",
    "rework_instruction",
    "task_diff",
    "test",
    "runtime",
    "build",
}
CLAIM_EVIDENCE_SOURCES = {"task_diff", "test", "runtime"}


class DeliveryError(RuntimeError):
    pass


def run_git(*arguments: str, cwd: Path | None = None) -> bytes:
    result = subprocess.run(
        ["git", *arguments],
        cwd=cwd,
        check=False,
        capture_output=True,
    )
    if result.returncode != 0:
        message = result.stderr.decode(errors="replace").strip()
        raise DeliveryError(message or "Git 命令执行失败")
    return result.stdout


def repository_root() -> Path:
    output = run_git("rev-parse", "--show-toplevel")
    return Path(os.fsdecode(output).strip()).resolve()


def repository_paths(root: Path) -> set[str]:
    output = run_git(
        "ls-files",
        "--cached",
        "--others",
        "--exclude-standard",
        "-z",
        cwd=root,
    )
    return {os.fsdecode(path) for path in output.split(b"\0") if path}


def file_state(path: Path) -> dict[str, Any]:
    try:
        metadata = path.lstat()
    except FileNotFoundError:
        return {"type": "missing"}

    mode = stat.S_IMODE(metadata.st_mode)
    if path.is_symlink():
        return {"type": "symlink", "mode": mode, "target": os.readlink(path)}
    if path.is_file():
        digest = hashlib.sha256()
        with path.open("rb") as source:
            for chunk in iter(lambda: source.read(1024 * 1024), b""):
                digest.update(chunk)
        return {"type": "file", "mode": mode, "sha256": digest.hexdigest()}
    return {"type": "other", "mode": mode}


def collect_file_states(root: Path, ignored_paths: set[str]) -> dict[str, dict[str, Any]]:
    return {
        relative_path: file_state(root / relative_path)
        for relative_path in sorted(repository_paths(root) - ignored_paths)
    }


def dirty_paths(root: Path, head: str) -> set[str]:
    tracked = run_git(
        "diff",
        "--name-only",
        "--no-renames",
        "-z",
        head,
        "--",
        cwd=root,
    )
    untracked = run_git(
        "ls-files",
        "--others",
        "--exclude-standard",
        "-z",
        cwd=root,
    )
    return {
        os.fsdecode(path)
        for output in (tracked, untracked)
        for path in output.split(b"\0")
        if path
    }


def relative_output_path(root: Path, output: Path) -> str | None:
    try:
        return output.resolve(strict=False).relative_to(root).as_posix()
    except ValueError:
        return None


def utc_now() -> str:
    return datetime.now(tz=timezone.utc).isoformat()


def read_required_text(path: Path, label: str) -> str:
    try:
        value = path.read_text(encoding="utf-8").strip()
    except OSError as error:
        raise DeliveryError(f"无法读取{label}：{error}") from error
    if not value:
        raise DeliveryError(f"{label}不能为空")
    return value


def read_json(path: Path, label: str) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise DeliveryError(f"无法读取{label}：{error}") from error


def atomic_write_json(path: Path, value: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}-",
        suffix=".tmp",
        dir=path.parent,
    )
    os.close(descriptor)
    temporary = Path(temporary_name)
    try:
        temporary.write_text(
            json.dumps(value, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)


@contextmanager
def snapshot_lock(path: Path) -> Any:
    identity = hashlib.sha256(str(path.resolve()).encode("utf-8")).hexdigest()
    lock_path = Path(tempfile.gettempdir()) / f"kmp-music-agent-delivery-{identity}.lock"
    try:
        with lock_path.open("a+b") as lock:
            fcntl.flock(lock.fileno(), fcntl.LOCK_EX)
            try:
                yield
            finally:
                fcntl.flock(lock.fileno(), fcntl.LOCK_UN)
    except OSError as error:
        raise DeliveryError(f"无法锁定交付快照：{error}") from error


def normalized_requirements(value: Any) -> list[dict[str, str]]:
    if not isinstance(value, list) or not value:
        raise DeliveryError("验收合同必须是非空数组")
    requirements: list[dict[str, str]] = []
    identifiers: set[str] = set()
    for item in value:
        if not isinstance(item, dict):
            raise DeliveryError("验收合同条目必须是对象")
        identifier = item.get("id")
        kind = item.get("kind")
        text = item.get("text")
        if not isinstance(identifier, str) or not identifier.strip():
            raise DeliveryError("验收合同条目缺少 requirement ID")
        normalized_identifier = identifier.strip()
        if normalized_identifier in identifiers:
            raise DeliveryError(f"验收合同包含重复 requirement ID：{normalized_identifier}")
        if kind not in REQUIREMENT_KINDS:
            raise DeliveryError(
                f"{normalized_identifier} 的 kind 必须是 MUST/FORBIDDEN/UNCHANGED"
            )
        if not isinstance(text, str) or not text.strip():
            raise DeliveryError(f"{normalized_identifier} 缺少验收条目正文")
        identifiers.add(normalized_identifier)
        requirements.append(
            {
                "id": normalized_identifier,
                "kind": kind,
                "text": text.strip(),
            }
        )
    return requirements


def contract_digest(raw_request: str, requirements: list[dict[str, str]]) -> str:
    encoded = json.dumps(
        {"raw_request": raw_request, "requirements": requirements},
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def build_contract(request_file: Path, requirements_file: Path) -> dict[str, Any]:
    raw_request = read_required_text(request_file, "用户原话")
    requirements = normalized_requirements(
        read_json(requirements_file, "结构化验收合同")
    )
    return {
        "version": 1,
        "raw_request": raw_request,
        "requirements": requirements,
        "digest": contract_digest(raw_request, requirements),
    }


def build_writer(writer_id: str) -> dict[str, Any]:
    writer = single_line_field(writer_id, "写入者 ID")
    if not writer:
        raise DeliveryError("写入者 ID 不能为空")
    return {
        "current": writer,
        "status": "active",
        "history": [
            {
                "event": "claimed",
                "writer": writer,
                "at": utc_now(),
            }
        ],
    }


def snapshot_command(arguments: argparse.Namespace) -> int:
    contract = build_contract(
        Path(arguments.request_file),
        Path(arguments.requirements_file),
    )
    writer = build_writer(arguments.writer_id)
    root = repository_root()
    head = run_git("rev-parse", "HEAD", cwd=root).decode().strip()
    output = Path(arguments.output) if arguments.output else default_snapshot_path()
    ignored_paths: set[str] = set()
    relative_output = relative_output_path(root, output)
    if relative_output is not None:
        ignored_paths.add(relative_output)

    files = collect_file_states(root, ignored_paths)
    baseline_overrides = sorted(dirty_paths(root, head) - ignored_paths)
    baseline_directory = Path(
        tempfile.mkdtemp(prefix="kmp-music-agent-delivery-baseline-")
    )
    copy_baseline_overrides(root, baseline_directory, baseline_overrides, files)
    confirmed_head = run_git("rev-parse", "HEAD", cwd=root).decode().strip()
    confirmed_files = collect_file_states(root, ignored_paths)
    if head != confirmed_head or files != confirmed_files:
        shutil.rmtree(baseline_directory, ignore_errors=True)
        raise DeliveryError("快照期间工作区发生变化，请重试 snapshot")
    snapshot = {
        "version": SNAPSHOT_VERSION,
        "repository_root": str(root),
        "head": head,
        "baseline_directory": str(baseline_directory.resolve()),
        "baseline_overrides": baseline_overrides,
        "ignored_paths": sorted(ignored_paths),
        "files": files,
        "contract": contract,
        "rework_history": [],
        "writer": writer,
    }
    atomic_write_json(output, snapshot)
    print(output.resolve())
    return 0


def default_snapshot_path() -> Path:
    descriptor, name = tempfile.mkstemp(prefix="kmp-music-agent-delivery-", suffix=".json")
    os.close(descriptor)
    return Path(name)


def default_manifest_path() -> Path:
    descriptor, name = tempfile.mkstemp(
        prefix="kmp-music-agent-delivery-",
        suffix="-manifest.md",
    )
    os.close(descriptor)
    return Path(name)


def copy_baseline_overrides(
    root: Path,
    baseline_directory: Path,
    paths: list[str],
    files: dict[str, dict[str, Any]],
) -> None:
    for relative_path in paths:
        state = files.get(relative_path, {"type": "missing"})
        source = root / relative_path
        destination = baseline_directory / relative_path
        if state.get("type") == "file":
            destination.parent.mkdir(parents=True, exist_ok=True)
            try:
                shutil.copy2(source, destination)
            except OSError as error:
                raise DeliveryError(
                    f"无法保存任务前基线：{display_path(relative_path)}: {error}"
                ) from error
        elif state.get("type") == "symlink":
            destination.parent.mkdir(parents=True, exist_ok=True)
            try:
                os.symlink(os.readlink(source), destination)
            except OSError as error:
                raise DeliveryError(
                    f"无法保存任务前符号链接：{display_path(relative_path)}: {error}"
                ) from error


def load_snapshot(path: Path) -> dict[str, Any]:
    try:
        snapshot = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise DeliveryError(f"无法读取交付快照：{error}") from error
    if not isinstance(snapshot, dict):
        raise DeliveryError("交付快照根节点必须是对象")
    version = snapshot.get("version")
    if version not in {LEGACY_SNAPSHOT_VERSION, SNAPSHOT_VERSION}:
        raise DeliveryError("交付快照版本不受支持")
    if not isinstance(snapshot.get("repository_root"), str):
        raise DeliveryError("交付快照缺少仓库路径")
    root = Path(snapshot["repository_root"]).resolve()
    if not isinstance(snapshot.get("head"), str):
        raise DeliveryError("交付快照缺少基线提交")
    baseline_directory = snapshot.get("baseline_directory")
    if not isinstance(baseline_directory, str) or not Path(baseline_directory).is_dir():
        raise DeliveryError("交付快照缺少可用的基线目录")
    if relative_output_path(root, Path(baseline_directory)) is not None:
        raise DeliveryError("交付基线目录不能位于仓库内")
    baseline_overrides = snapshot.get("baseline_overrides")
    if not isinstance(baseline_overrides, list) or not all(
        isinstance(path, str) and is_repository_relative_path(path)
        for path in baseline_overrides
    ):
        raise DeliveryError("交付快照的基线覆盖路径格式无效")
    if version == LEGACY_SNAPSHOT_VERSION:
        manifest_path = snapshot.get("manifest_path")
        if not isinstance(manifest_path, str) or not Path(manifest_path).is_absolute():
            raise DeliveryError("交付快照缺少绝对 Manifest 路径")
        if relative_output_path(root, Path(manifest_path)) is not None:
            raise DeliveryError("Manifest 不能位于仓库内")
    ignored_paths = snapshot.get("ignored_paths", [])
    if not isinstance(ignored_paths, list) or not all(
        isinstance(path, str) for path in ignored_paths
    ):
        raise DeliveryError("交付快照的忽略路径格式无效")
    files = snapshot.get("files")
    if not isinstance(files, dict) or not all(
        isinstance(path, str) and isinstance(state, dict)
        for path, state in files.items()
    ):
        raise DeliveryError("交付快照缺少有效的文件状态")
    if version == SNAPSHOT_VERSION:
        validate_contract(snapshot.get("contract"))
        validate_rework_history(snapshot.get("rework_history"))
        validate_writer(snapshot.get("writer"))
    return snapshot


def validate_contract(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict) or value.get("version") != 1:
        raise DeliveryError("交付快照缺少原始验收合同")
    raw_request = value.get("raw_request")
    if not isinstance(raw_request, str) or not raw_request:
        raise DeliveryError("原始验收合同缺少用户原话")
    requirements = normalized_requirements(value.get("requirements"))
    digest = value.get("digest")
    if digest != contract_digest(raw_request, requirements):
        raise DeliveryError("原始验收合同摘要不匹配，禁止静默覆盖")
    return value


def validate_rework_history(value: Any) -> list[dict[str, Any]]:
    if not isinstance(value, list):
        raise DeliveryError("返工记录必须是数组")
    for index, item in enumerate(value, start=2):
        if not isinstance(item, dict) or item.get("version") != index:
            raise DeliveryError("返工记录版本必须连续递增")
        instruction = item.get("instruction")
        if not isinstance(instruction, str) or not instruction:
            raise DeliveryError(f"返工记录 v{index} 缺少用户指令")
    return value


def validate_writer(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise DeliveryError("交付快照缺少写入者状态")
    current = value.get("current")
    status = value.get("status")
    history = value.get("history")
    if not isinstance(current, str) or not current:
        raise DeliveryError("写入者状态缺少当前 writer")
    if status not in {"active", "terminated_confirmed"}:
        raise DeliveryError("写入者状态无效")
    if not isinstance(history, list) or not history:
        raise DeliveryError("写入者状态缺少审计历史")
    return value


def require_writer(snapshot: dict[str, Any], writer_id: str | None) -> None:
    writer = validate_writer(snapshot.get("writer"))
    requested = single_line_field(writer_id or "", "写入者 ID")
    if writer["status"] != "active":
        raise DeliveryError("旧写入者已确认终止，必须完成 takeover 后才能继续写入")
    if requested != writer["current"]:
        raise DeliveryError(
            f"当前任务写入者是 {writer['current']}，拒绝 {requested or '未知写入者'} 写入"
        )


def initialize_contract_command(arguments: argparse.Namespace) -> int:
    path = Path(arguments.snapshot)
    with snapshot_lock(path):
        snapshot = load_snapshot(path)
        if snapshot["version"] != LEGACY_SNAPSHOT_VERSION:
            raise DeliveryError("只有未绑定合同的 v2 快照可以初始化合同")
        snapshot["version"] = SNAPSHOT_VERSION
        snapshot["contract"] = build_contract(
            Path(arguments.request_file),
            Path(arguments.requirements_file),
        )
        snapshot["rework_history"] = []
        snapshot["writer"] = build_writer(arguments.writer_id)
        snapshot.pop("manifest_path", None)
        atomic_write_json(path, snapshot)
    print(path.resolve())
    return 0


def append_rework_command(arguments: argparse.Namespace) -> int:
    path = Path(arguments.snapshot)
    with snapshot_lock(path):
        snapshot = load_snapshot(path)
        require_current_snapshot(snapshot)
        require_writer(snapshot, arguments.writer_id)
        history = validate_rework_history(snapshot["rework_history"])
        current_version = 1 + len(history)
        if arguments.expected_version != current_version:
            raise DeliveryError(
                f"验收合同当前版本是 {current_version}，拒绝基于 v{arguments.expected_version} 追加返工"
            )
        instruction = read_required_text(Path(arguments.instruction_file), "返工指令")
        history.append(
            {
                "version": current_version + 1,
                "instruction": instruction,
                "at": utc_now(),
            }
        )
        atomic_write_json(path, snapshot)
    print(f"v{current_version + 1}")
    return 0


def confirm_terminated_command(arguments: argparse.Namespace) -> int:
    path = Path(arguments.snapshot)
    with snapshot_lock(path):
        snapshot = load_snapshot(path)
        require_current_snapshot(snapshot)
        writer = validate_writer(snapshot["writer"])
        expected_writer = single_line_field(arguments.expected_writer, "旧写入者 ID")
        confirmed_by = single_line_field(arguments.confirmed_by, "确认者 ID")
        evidence = single_line_field(arguments.evidence, "终止证据")
        if writer["status"] != "active" or writer["current"] != expected_writer:
            raise DeliveryError("待确认终止的 writer 与当前活动 writer 不一致")
        if not confirmed_by or not evidence:
            raise DeliveryError("确认旧写入者终止必须记录确认者和证据")
        if confirmed_by == expected_writer:
            raise DeliveryError("旧写入者不能自行确认终止")
        writer["status"] = "terminated_confirmed"
        writer["history"].append(
            {
                "event": "termination_confirmed",
                "writer": expected_writer,
                "confirmed_by": confirmed_by,
                "evidence": evidence,
                "at": utc_now(),
            }
        )
        atomic_write_json(path, snapshot)
    print(f"已确认终止：{expected_writer}")
    return 0


def takeover_command(arguments: argparse.Namespace) -> int:
    path = Path(arguments.snapshot)
    with snapshot_lock(path):
        snapshot = load_snapshot(path)
        require_current_snapshot(snapshot)
        writer = validate_writer(snapshot["writer"])
        expected_writer = single_line_field(arguments.expected_writer, "旧写入者 ID")
        new_writer = single_line_field(arguments.new_writer, "新写入者 ID")
        if writer["current"] != expected_writer:
            raise DeliveryError("接管目标与当前 writer 不一致")
        if writer["status"] != "terminated_confirmed":
            raise DeliveryError("旧写入者尚未确认终止，禁止并发接管")
        if not new_writer or new_writer == expected_writer:
            raise DeliveryError("新写入者 ID 必须与旧写入者不同")
        writer["current"] = new_writer
        writer["status"] = "active"
        writer["history"].append(
            {
                "event": "takeover",
                "previous_writer": expected_writer,
                "writer": new_writer,
                "at": utc_now(),
            }
        )
        atomic_write_json(path, snapshot)
    print(f"当前写入者：{new_writer}")
    return 0


def require_current_snapshot(snapshot: dict[str, Any]) -> None:
    if snapshot.get("version") != SNAPSHOT_VERSION:
        raise DeliveryError("该操作要求已绑定原始验收合同的 v3 快照")


def changed_file_states(
    snapshot: dict[str, Any],
) -> list[tuple[str, dict[str, Any], dict[str, Any]]]:
    root = repository_root()
    if str(root) != snapshot.get("repository_root"):
        raise DeliveryError("交付快照不属于当前仓库")

    ignored_paths = set(snapshot.get("ignored_paths", []))
    before = snapshot.get("files")
    if not isinstance(before, dict):
        raise DeliveryError("交付快照缺少文件状态")
    after = collect_file_states(root, ignored_paths)
    paths = set(before) | set(after)
    missing = {"type": "missing"}
    return [
        (path, before.get(path, missing), after.get(path, missing))
        for path in sorted(paths)
        if before.get(path) != after.get(path)
    ]


def structured_digest(value: Any) -> str:
    encoded = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def text_digest(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def task_diff(
    snapshot: dict[str, Any],
    file_changes: list[tuple[str, dict[str, Any], dict[str, Any]]],
) -> str:
    root = repository_root()
    head_entries = read_head_entries(root, snapshot["head"])
    return render_manifest_diffs(root, snapshot, file_changes, head_entries)


def normalized_verdicts(
    value: Any,
    requirements: list[dict[str, str]],
) -> list[dict[str, Any]]:
    if not isinstance(value, list):
        raise DeliveryError("规格审查结论必须是数组")
    requirement_ids = [requirement["id"] for requirement in requirements]
    verdicts_by_id: dict[str, dict[str, Any]] = {}
    for item in value:
        if not isinstance(item, dict):
            raise DeliveryError("规格审查条目必须是对象")
        identifier = item.get("id")
        status = item.get("status")
        evidence = item.get("evidence")
        if not isinstance(identifier, str) or identifier not in requirement_ids:
            raise DeliveryError(f"规格审查包含未知 requirement ID：{identifier}")
        if identifier in verdicts_by_id:
            raise DeliveryError(f"规格审查重复 requirement ID：{identifier}")
        if status not in {"PASS", "FAIL"}:
            raise DeliveryError(f"{identifier} 的审查结果必须是 PASS 或 FAIL")
        if not isinstance(evidence, list) or not evidence:
            raise DeliveryError(f"{identifier} 缺少规格证据")
        normalized_evidence: list[dict[str, str]] = []
        for claim in evidence:
            if not isinstance(claim, dict):
                raise DeliveryError(f"{identifier} 的规格证据必须是对象")
            source = claim.get("source")
            detail = claim.get("detail")
            if source not in REVIEW_EVIDENCE_SOURCES:
                raise DeliveryError(f"{identifier} 包含未知证据来源：{source}")
            if not isinstance(detail, str) or not detail.strip():
                raise DeliveryError(f"{identifier} 包含空证据说明")
            normalized_evidence.append(
                {"source": source, "detail": detail.strip()}
            )
        if not any(
            claim["source"] in CLAIM_EVIDENCE_SOURCES
            for claim in normalized_evidence
        ):
            raise DeliveryError(f"{identifier} 不能只用构建或合同文本作为规格证据")
        verdicts_by_id[identifier] = {
            "id": identifier,
            "status": status,
            "evidence": normalized_evidence,
        }
    missing = [identifier for identifier in requirement_ids if identifier not in verdicts_by_id]
    if missing:
        raise DeliveryError(f"规格审查缺少 requirement ID：{', '.join(missing)}")
    return [verdicts_by_id[identifier] for identifier in requirement_ids]


def default_review_path() -> Path:
    descriptor, name = tempfile.mkstemp(
        prefix="kmp-music-agent-delivery-",
        suffix="-review.json",
    )
    os.close(descriptor)
    return Path(name)


def review_command(arguments: argparse.Namespace) -> int:
    snapshot = load_snapshot(Path(arguments.snapshot))
    require_current_snapshot(snapshot)
    require_writer(snapshot, arguments.writer_id)
    contract = validate_contract(snapshot["contract"])
    requirements = normalized_requirements(contract["requirements"])
    verdicts = normalized_verdicts(
        read_json(Path(arguments.verdicts_file), "规格审查结论"),
        requirements,
    )
    verification_evidence = read_required_text(
        Path(arguments.verification_evidence_file),
        "验证证据",
    )
    file_changes = changed_file_states(snapshot)
    diff = task_diff(snapshot, file_changes)
    report = {
        "version": REVIEW_VERSION,
        "snapshot_digest": structured_digest(snapshot),
        "contract_digest": contract["digest"],
        "task_diff_digest": text_digest(diff),
        "review_inputs": {
            "raw_request": contract["raw_request"],
            "original_requirements": requirements,
            "rework_instructions": [
                item["instruction"] for item in snapshot["rework_history"]
            ],
            "task_diff": diff,
        },
        "verdicts": verdicts,
        "verification_evidence": verification_evidence,
        "created_at": utc_now(),
    }
    output = Path(arguments.output) if arguments.output else default_review_path()
    root = repository_root()
    if relative_output_path(root, output) is not None:
        raise DeliveryError("规格审查报告必须位于仓库外，避免污染任务级 diff")
    atomic_write_json(output, report)
    print(output.resolve())
    return 0


def load_review_report(
    path: Path,
    snapshot: dict[str, Any],
    file_changes: list[tuple[str, dict[str, Any], dict[str, Any]]],
) -> dict[str, Any]:
    report = read_json(path, "规格审查报告")
    if not isinstance(report, dict) or report.get("version") != REVIEW_VERSION:
        raise DeliveryError("规格审查报告版本不受支持")
    contract = validate_contract(snapshot["contract"])
    diff = task_diff(snapshot, file_changes)
    if report.get("snapshot_digest") != structured_digest(snapshot):
        raise DeliveryError("规格审查报告不属于当前合同或 writer 状态")
    if report.get("contract_digest") != contract["digest"]:
        raise DeliveryError("规格审查报告未绑定原始验收合同")
    if report.get("task_diff_digest") != text_digest(diff):
        raise DeliveryError("规格审查报告对应的任务级 diff 已过期")
    inputs = report.get("review_inputs")
    if not isinstance(inputs, dict):
        raise DeliveryError("规格审查报告缺少审查输入")
    expected_inputs = {
        "raw_request": contract["raw_request"],
        "original_requirements": contract["requirements"],
        "rework_instructions": [
            item["instruction"] for item in snapshot["rework_history"]
        ],
        "task_diff": diff,
    }
    if inputs != expected_inputs:
        raise DeliveryError("规格审查报告未完整读取原话、合同、返工指令和任务级 diff")
    normalized_verdicts(report.get("verdicts"), contract["requirements"])
    verification_evidence = report.get("verification_evidence")
    if not isinstance(verification_evidence, str) or not verification_evidence.strip():
        raise DeliveryError("规格审查报告缺少验证证据")
    return report


def render_command(arguments: argparse.Namespace) -> int:
    snapshot = load_snapshot(Path(arguments.snapshot))
    file_changes = changed_file_states(snapshot)
    changes = single_line_field(arguments.changes, "改动说明")
    verification = single_line_field(arguments.verification, "验证说明")
    risks = single_line_field(arguments.risks, "剩余风险说明")

    if file_changes and not changes:
        raise DeliveryError("检测到文件变化，但缺少改动说明")
    if file_changes and "未修改文件" in changes:
        raise DeliveryError("检测到文件变化，改动说明不能声称未修改文件")
    if not verification:
        raise DeliveryError("缺少验证说明；未运行验证时必须写明原因")
    if not risks:
        raise DeliveryError("缺少剩余风险说明；无已知风险时请明确写出")

    review_report: dict[str, Any] | None = None
    if snapshot["version"] == SNAPSHOT_VERSION:
        require_writer(snapshot, arguments.writer_id)
        if not arguments.review_report:
            raise DeliveryError("v3 快照 render 必须提供逐条规格审查报告")
        review_report = load_review_report(
            Path(arguments.review_report),
            snapshot,
            file_changes,
        )
    manifest_path = default_manifest_path()
    file_inventory = render_file_inventory(len(file_changes), manifest_path)
    changes_line = f"改了什么：{changes or '未修改文件'}；{file_inventory}"
    summary = "\n".join(
        (
            changes_line,
            f"验证了什么：{verification}",
            f"剩余风险：{risks}",
        )
    )
    if len(summary) > MAX_SUMMARY_CHARACTERS:
        manifest_path.unlink(missing_ok=True)
        raise DeliveryError(
            f"交付结论超过 {MAX_SUMMARY_CHARACTERS} 个字符，请压缩说明后重试"
        )
    write_manifest(
        snapshot=snapshot,
        file_changes=file_changes,
        changes=changes,
        verification=verification,
        risks=risks,
        review_report=review_report,
        output=manifest_path,
    )
    print(summary)
    return 0


def render_file_inventory(file_count: int, manifest_path: Path) -> str:
    link = f"[查看完整交付记录](<{manifest_path.resolve().as_posix()}>)"
    if file_count == 0:
        return f"未检测到任务期间的仓库文件变化；{link}"
    return f"{file_count} 个文件；{link}"


def write_manifest(
    snapshot: dict[str, Any],
    file_changes: list[tuple[str, dict[str, Any], dict[str, Any]]],
    changes: str,
    verification: str,
    risks: str,
    review_report: dict[str, Any] | None,
    output: Path,
) -> None:
    root = repository_root()
    head_entries = read_head_entries(root, snapshot["head"])
    inventory = render_manifest_inventory(file_changes)
    diffs = render_manifest_diffs(root, snapshot, file_changes, head_entries)
    attribution = render_baseline_attribution(snapshot, file_changes)
    review = render_review_report(review_report)
    verification_evidence = (
        review_report["verification_evidence"]
        if review_report is not None
        else "旧版快照未绑定结构化验证证据。"
    )
    content = "\n".join(
        (
            "# 子代理交付记录",
            "",
            "## 元数据",
            "",
            f"- 基线提交：`{snapshot['head']}`",
            f"- 修改文件数：{len(file_changes)}",
            "",
            "## 基线归因",
            "",
            attribution,
            "",
            "## 修改摘要",
            "",
            changes or "未修改文件",
            "",
            "## 文件清单",
            "",
            inventory,
            "",
            "## 详细 Diff",
            "",
            diffs,
            "",
            "## 规格审查",
            "",
            review,
            "",
            "## 验证",
            "",
            verification,
            "",
            "### 验证证据",
            "",
            verification_evidence,
            "",
            "## 剩余风险",
            "",
            risks,
            "",
        )
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(content, encoding="utf-8")


def render_baseline_attribution(
    snapshot: dict[str, Any],
    file_changes: list[tuple[str, dict[str, Any], dict[str, Any]]],
) -> str:
    baseline_overrides = snapshot.get("baseline_overrides", [])
    baseline_files = snapshot.get("files", {})
    task_paths = [path for path, _, _ in file_changes]
    unchanged_baseline_paths = (
        set(baseline_files) - set(baseline_overrides) - set(task_paths)
    )
    unchanged_baseline_count = len(unchanged_baseline_paths)
    user_paths = [display_path(path) for path in baseline_overrides]
    return "\n".join(
        (
            "| 类别 | 归因规则 | 文件 |",
            "| --- | --- | --- |",
            "| 本轮新增或修改 | 仅计算 snapshot 后变化，可归因本轮 | "
            + render_path_list(task_paths)
            + " |",
            "| 用户已有改动 | snapshot 前已脏，只把 snapshot 后增量计入本轮 | "
            + render_path_list(user_paths)
            + " |",
            f"| 基线既有未变 | 未出现在任务级 diff，禁止归因本轮 | {unchanged_baseline_count} 个文件 |",
        )
    )


def render_path_list(paths: list[str]) -> str:
    if not paths:
        return "无"
    return "<br>".join(f"<code>{html.escape(path)}</code>" for path in paths)


def render_review_report(report: dict[str, Any] | None) -> str:
    if report is None:
        return "旧版快照未绑定结构化规格审查。"
    rows = [
        "| Requirement ID | 结论 | 证据 |",
        "| --- | --- | --- |",
    ]
    for verdict in report["verdicts"]:
        evidence = "<br>".join(
            f"<code>{html.escape(item['source'])}</code>：{html.escape(item['detail'])}"
            for item in verdict["evidence"]
        )
        rows.append(
            f"| {html.escape(verdict['id'])} | {verdict['status']} | {evidence} |"
        )
    return "\n".join(rows)


def render_manifest_inventory(
    file_changes: list[tuple[str, dict[str, Any], dict[str, Any]]],
) -> str:
    if not file_changes:
        return "未检测到任务期间的仓库文件变化。"
    rows = ["| 状态 | 文件 |", "| --- | --- |"]
    rows.extend(
        f"| {change_status(before, after)} | <code>{html.escape(display_path(path))}</code> |"
        for path, before, after in file_changes
    )
    return "\n".join(rows)


def render_manifest_diffs(
    root: Path,
    snapshot: dict[str, Any],
    file_changes: list[tuple[str, dict[str, Any], dict[str, Any]]],
    head_entries: dict[str, str],
) -> str:
    if not file_changes:
        return "未检测到任务期间的仓库文件变化。"
    sections: list[str] = []
    for path, before, after in file_changes:
        diff = render_file_diff(root, snapshot, path, before, after, head_entries)
        sections.extend(
            (
                f"### <code>{html.escape(display_path(path))}</code>",
                "",
                fenced_diff(diff),
                "",
            )
        )
    return "\n".join(sections).rstrip()


def read_head_entries(root: Path, head: str) -> dict[str, str]:
    output = run_git("ls-tree", "-rz", "--full-tree", head, cwd=root)
    entries: dict[str, str] = {}
    for record in output.split(b"\0"):
        if not record:
            continue
        metadata, path = record.split(b"\t", 1)
        _, object_type, object_id = metadata.split(b" ", 2)
        if object_type == b"blob":
            entries[os.fsdecode(path)] = object_id.decode()
    return entries


def render_file_diff(
    root: Path,
    snapshot: dict[str, Any],
    path: str,
    before: dict[str, Any],
    after: dict[str, Any],
    head_entries: dict[str, str],
) -> str:
    before_content = baseline_content(root, snapshot, path, before, head_entries)
    after_content = state_content(root / path, after)
    metadata = render_mode_change(before, after)
    if is_binary(before_content) or is_binary(after_content):
        binary = "\n".join(
            (
                f"二进制内容变化：a/{display_path(path)} 与 b/{display_path(path)} 不同",
                f"修改前 SHA-256：{content_digest(before_content)}",
                f"修改后 SHA-256：{content_digest(after_content)}",
            )
        )
        return "\n".join(part for part in (metadata, binary) if part)

    before_text = "" if before_content is None else before_content.decode("utf-8")
    after_text = "" if after_content is None else after_content.decode("utf-8")
    from_file = "/dev/null" if before_content is None else f"a/{display_path(path)}"
    to_file = "/dev/null" if after_content is None else f"b/{display_path(path)}"
    unified = "\n".join(
        difflib.unified_diff(
            before_text.splitlines(),
            after_text.splitlines(),
            fromfile=from_file,
            tofile=to_file,
            lineterm="",
        )
    )
    return "\n".join(part for part in (metadata, unified) if part) or "文件元数据发生变化"


def baseline_content(
    root: Path,
    snapshot: dict[str, Any],
    path: str,
    state: dict[str, Any],
    head_entries: dict[str, str],
) -> bytes | None:
    if state.get("type") == "missing":
        return None
    if path in set(snapshot["baseline_overrides"]):
        return state_content(Path(snapshot["baseline_directory"]) / path, state)
    object_id = head_entries.get(path)
    if object_id is None:
        raise DeliveryError(f"无法从基线提交还原文件：{display_path(path)}")
    return run_git(
        "cat-file",
        "--filters",
        f"--path={path}",
        object_id,
        cwd=root,
    )


def state_content(path: Path, state: dict[str, Any]) -> bytes | None:
    state_type = state.get("type")
    if state_type == "missing":
        return None
    if state_type == "symlink":
        return os.fsencode(os.readlink(path))
    if state_type == "file":
        try:
            return path.read_bytes()
        except OSError as error:
            raise DeliveryError(f"无法读取文件内容：{path}: {error}") from error
    return None


def change_status(before: dict[str, Any], after: dict[str, Any]) -> str:
    if before.get("type") == "missing":
        return "A"
    if after.get("type") == "missing":
        return "D"
    return "M"


def render_mode_change(before: dict[str, Any], after: dict[str, Any]) -> str:
    before_mode = git_mode(before)
    after_mode = git_mode(after)
    if before_mode is None and after_mode is not None:
        return f"new file mode {after_mode}"
    if before_mode is not None and after_mode is None:
        return f"deleted file mode {before_mode}"
    if before_mode != after_mode:
        return f"old mode {before_mode}\nnew mode {after_mode}"
    return ""


def git_mode(state: dict[str, Any]) -> str | None:
    state_type = state.get("type")
    if state_type == "missing":
        return None
    if state_type == "symlink":
        return "120000"
    mode = state.get("mode")
    if state_type == "file" and isinstance(mode, int):
        return format(0o100000 | mode, "06o")
    return "160000"


def is_binary(content: bytes | None) -> bool:
    if content is None:
        return False
    if b"\0" in content:
        return True
    try:
        content.decode("utf-8")
    except UnicodeDecodeError:
        return True
    return False


def content_digest(content: bytes | None) -> str:
    if content is None:
        return "不存在"
    return hashlib.sha256(content).hexdigest()


def fenced_diff(diff: str) -> str:
    longest_run = longest_backtick_run(diff)
    fence = "`" * max(3, longest_run + 1)
    return f"{fence}diff\n{diff}\n{fence}"


def longest_backtick_run(value: str) -> int:
    longest = 0
    current = 0
    for character in value:
        if character == "`":
            current += 1
            longest = max(longest, current)
        else:
            current = 0
    return longest


def is_repository_relative_path(path: str) -> bool:
    candidate = Path(path)
    return not candidate.is_absolute() and ".." not in candidate.parts


def single_line_field(value: str, label: str) -> str:
    if "\n" in value or "\r" in value:
        raise DeliveryError(f"{label}必须是单行文本")
    return value.strip()


def display_path(path: str) -> str:
    return "".join(
        character
        if character >= " " and character != "\x7f"
        else f"\\x{ord(character):02x}"
        for character in path
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="记录任务前文件状态，并生成可核查的子代理交付结论。"
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    snapshot = subparsers.add_parser("snapshot", help="在任务首次修改前记录仓库文件状态")
    snapshot.add_argument("--output", help="快照输出路径；省略时写入系统临时目录")
    snapshot.add_argument("--request-file", required=True, help="包含用户原话的 UTF-8 文件")
    snapshot.add_argument(
        "--requirements-file",
        required=True,
        help="包含 MUST/FORBIDDEN/UNCHANGED 条目的 JSON 文件",
    )
    snapshot.add_argument("--writer-id", required=True, help="当前唯一写入者 ID")
    snapshot.set_defaults(handler=snapshot_command)

    initialize = subparsers.add_parser(
        "initialize-contract",
        help="为执行中的 v2 快照绑定原始验收合同",
    )
    initialize.add_argument("--snapshot", required=True, help="v2 快照路径")
    initialize.add_argument("--request-file", required=True, help="包含用户原话的 UTF-8 文件")
    initialize.add_argument(
        "--requirements-file",
        required=True,
        help="包含 MUST/FORBIDDEN/UNCHANGED 条目的 JSON 文件",
    )
    initialize.add_argument("--writer-id", required=True, help="当前唯一写入者 ID")
    initialize.set_defaults(handler=initialize_contract_command)

    rework = subparsers.add_parser("append-rework", help="向验收合同追加版本化返工指令")
    rework.add_argument("--snapshot", required=True, help="v3 快照路径")
    rework.add_argument("--writer-id", required=True, help="当前唯一写入者 ID")
    rework.add_argument(
        "--expected-version",
        required=True,
        type=int,
        help="追加前已读取的合同版本",
    )
    rework.add_argument("--instruction-file", required=True, help="返工用户原话文件")
    rework.set_defaults(handler=append_rework_command)

    confirm = subparsers.add_parser(
        "confirm-terminated",
        help="记录旧写入者已经终止的接管前证据",
    )
    confirm.add_argument("--snapshot", required=True, help="v3 快照路径")
    confirm.add_argument("--expected-writer", required=True, help="待确认终止的 writer ID")
    confirm.add_argument("--confirmed-by", required=True, help="确认者 ID")
    confirm.add_argument("--evidence", required=True, help="单行终止确认依据")
    confirm.set_defaults(handler=confirm_terminated_command)

    takeover = subparsers.add_parser("takeover", help="在旧 writer 终止确认后接管任务")
    takeover.add_argument("--snapshot", required=True, help="v3 快照路径")
    takeover.add_argument("--expected-writer", required=True, help="已终止的 writer ID")
    takeover.add_argument("--new-writer", required=True, help="新 writer ID")
    takeover.set_defaults(handler=takeover_command)

    review = subparsers.add_parser("review", help="生成绑定合同与任务 diff 的逐条规格审查")
    review.add_argument("--snapshot", required=True, help="v3 快照路径")
    review.add_argument("--writer-id", required=True, help="当前唯一写入者 ID")
    review.add_argument("--verdicts-file", required=True, help="逐 requirement 结论 JSON")
    review.add_argument(
        "--verification-evidence-file",
        required=True,
        help="本轮验证命令与结果证据文件",
    )
    review.add_argument("--output", help="仓库外的审查报告输出路径")
    review.set_defaults(handler=review_command)

    render = subparsers.add_parser("render", help="根据快照生成最终交付结论")
    render.add_argument("--snapshot", required=True, help="snapshot 命令输出的路径")
    render.add_argument("--writer-id", help="v3 快照的当前唯一写入者 ID")
    render.add_argument("--review-report", help="review 命令输出的结构化规格审查报告")
    render.add_argument("--changes", required=True, help="行为或产物改动摘要")
    render.add_argument("--verification", required=True, help="验证命令、结果与独立审查结论")
    render.add_argument("--risks", required=True, help="剩余风险或阻塞原因")
    render.set_defaults(handler=render_command)
    return parser


def main() -> int:
    parser = build_parser()
    arguments = parser.parse_args()
    try:
        return arguments.handler(arguments)
    except DeliveryError as error:
        print(f"交付检查失败：{error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
