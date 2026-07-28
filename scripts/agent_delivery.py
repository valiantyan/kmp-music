#!/usr/bin/env python3

import argparse
import difflib
import hashlib
import html
import json
import os
import shutil
import stat
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any


SNAPSHOT_VERSION = 2
MAX_SUMMARY_CHARACTERS = 500


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


def snapshot_command(arguments: argparse.Namespace) -> int:
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
    manifest_path = default_manifest_path()
    snapshot = {
        "version": SNAPSHOT_VERSION,
        "repository_root": str(root),
        "head": head,
        "baseline_directory": str(baseline_directory.resolve()),
        "baseline_overrides": baseline_overrides,
        "manifest_path": str(manifest_path.resolve()),
        "ignored_paths": sorted(ignored_paths),
        "files": files,
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(
        json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
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
    if snapshot.get("version") != SNAPSHOT_VERSION:
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
    return snapshot


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

    manifest_path = Path(snapshot["manifest_path"])
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
        raise DeliveryError(
            f"交付结论超过 {MAX_SUMMARY_CHARACTERS} 个字符，请压缩说明后重试"
        )
    write_manifest(
        snapshot=snapshot,
        file_changes=file_changes,
        changes=changes,
        verification=verification,
        risks=risks,
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
    output: Path,
) -> None:
    root = repository_root()
    head_entries = read_head_entries(root, snapshot["head"])
    inventory = render_manifest_inventory(file_changes)
    diffs = render_manifest_diffs(root, snapshot, file_changes, head_entries)
    content = "\n".join(
        (
            "# 子代理交付记录",
            "",
            "## 元数据",
            "",
            f"- 基线提交：`{snapshot['head']}`",
            f"- 修改文件数：{len(file_changes)}",
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
            "## 验证",
            "",
            verification,
            "",
            "## 剩余风险",
            "",
            risks,
            "",
        )
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(content, encoding="utf-8")


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
    snapshot.set_defaults(handler=snapshot_command)

    render = subparsers.add_parser("render", help="根据快照生成最终交付结论")
    render.add_argument("--snapshot", required=True, help="snapshot 命令输出的路径")
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
