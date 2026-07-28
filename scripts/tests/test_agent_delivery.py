import json
import os
import re
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
SCRIPT = REPOSITORY_ROOT / "scripts" / "agent_delivery.py"


class AgentDeliveryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_directory = tempfile.TemporaryDirectory()
        self.repository = Path(self.temp_directory.name)
        self.run_git("init", "-q")
        self.run_git("config", "user.email", "agent@example.com")
        self.run_git("config", "user.name", "Agent")
        (self.repository / "tracked.txt").write_text("baseline\n", encoding="utf-8")
        self.run_git("add", "tracked.txt")
        self.run_git("commit", "-qm", "baseline")
        self.snapshot = self.repository / "snapshot.json"
        self.generated_artifacts: list[Path] = []

    def tearDown(self) -> None:
        if self.snapshot.exists():
            try:
                snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
                if isinstance(snapshot, dict):
                    baseline_directory = snapshot.get("baseline_directory")
                    if isinstance(baseline_directory, str):
                        shutil.rmtree(baseline_directory, ignore_errors=True)
                    manifest_path = snapshot.get("manifest_path")
                    if isinstance(manifest_path, str):
                        Path(manifest_path).unlink(missing_ok=True)
            except (OSError, json.JSONDecodeError):
                pass
        for artifact in self.generated_artifacts:
            artifact.unlink(missing_ok=True)
        self.temp_directory.cleanup()

    def test_render_reports_changed_files(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        (self.repository / "tracked.txt").write_text("changed\n", encoding="utf-8")
        (self.repository / "new.txt").write_text("new\n", encoding="utf-8")

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "更新交付契约并增加回归门禁",
            "--verification",
            "单元测试通过；独立审查通过",
            "--risks",
            "尚未通过新会话复跑验证",
        )

        self.assertIn("改了什么：更新交付契约并增加回归门禁", result.stdout)
        self.assertIn("2 个文件", result.stdout)
        self.assertIn("[查看完整交付记录](<", result.stdout)
        self.assertIn("验证了什么：单元测试通过；独立审查通过", result.stdout)
        self.assertIn("剩余风险：尚未通过新会话复跑验证", result.stdout)
        self.assertEqual(3, len(result.stdout.rstrip("\n").splitlines()))

        manifest = self.read_manifest(result)
        self.assertIn("| A | <code>new.txt</code> |", manifest)
        self.assertIn("| M | <code>tracked.txt</code> |", manifest)
        self.assertIn("-baseline", manifest)
        self.assertIn("+changed", manifest)
        self.assertIn("+new", manifest)
        self.assertIn("单元测试通过；独立审查通过", manifest)
        self.assertIn("尚未通过新会话复跑验证", manifest)

    def test_render_does_not_report_preexisting_changes(self) -> None:
        (self.repository / "tracked.txt").write_text("user change\n", encoding="utf-8")
        self.run_script("snapshot", "--output", str(self.snapshot))
        (self.repository / "new.txt").write_text("agent change\n", encoding="utf-8")

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "新增交付检查",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
        )

        self.assertIn("1 个文件", result.stdout)
        manifest = self.read_manifest(result)
        self.assertIn("<code>new.txt</code>", manifest)
        self.assertNotIn("<code>tracked.txt</code>", manifest)

    def test_render_diffs_from_preexisting_worktree_state(self) -> None:
        (self.repository / "tracked.txt").write_text("user change\n", encoding="utf-8")
        self.run_script("snapshot", "--output", str(self.snapshot))
        (self.repository / "tracked.txt").write_text("agent change\n", encoding="utf-8")

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "继续修改用户已有变更",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
        )

        manifest = self.read_manifest(result)
        self.assertIn("-user change", manifest)
        self.assertIn("+agent change", manifest)
        self.assertNotIn("-baseline", manifest)

    def test_render_diffs_preexisting_untracked_file(self) -> None:
        untracked = self.repository / "untracked.txt"
        untracked.write_text("user content\n", encoding="utf-8")
        self.run_script("snapshot", "--output", str(self.snapshot))
        untracked.write_text("agent content\n", encoding="utf-8")

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "继续修改任务前未跟踪文件",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
        )

        manifest = self.read_manifest(result)
        self.assertIn("-user content", manifest)
        self.assertIn("+agent content", manifest)

    def test_render_restores_clean_file_with_worktree_filter(self) -> None:
        self.run_git("config", "filter.prefix.clean", "sed 's/^SMUDGE://'")
        self.run_git("config", "filter.prefix.smudge", "sed 's/^/SMUDGE:/'")
        (self.repository / ".gitattributes").write_text(
            "tracked.txt filter=prefix\n",
            encoding="utf-8",
        )
        self.run_git("add", ".gitattributes")
        self.run_git("commit", "-qm", "add worktree filter")
        (self.repository / "tracked.txt").unlink()
        self.run_git("checkout", "--", "tracked.txt")
        self.assertEqual(
            "SMUDGE:baseline\n",
            (self.repository / "tracked.txt").read_text(encoding="utf-8"),
        )
        self.run_script("snapshot", "--output", str(self.snapshot))
        (self.repository / "tracked.txt").write_text(
            "SMUDGE:changed\n",
            encoding="utf-8",
        )

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "修改带工作区过滤器的文件",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
        )

        manifest = self.read_manifest(result)
        self.assertIn("-SMUDGE:baseline", manifest)
        self.assertIn("+SMUDGE:changed", manifest)

    def test_render_reuses_manifest_path_for_same_snapshot(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        (self.repository / "tracked.txt").write_text("changed\n", encoding="utf-8")
        arguments = (
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "更新交付文件",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
        )

        first = self.run_script(*arguments)
        second = self.run_script(*arguments)

        first_path = self.manifest_path(first)
        second_path = self.manifest_path(second)
        self.generated_artifacts.extend((first_path, second_path))
        self.assertEqual(first_path, second_path)

    def test_render_manifest_lists_all_files_without_truncated_etc(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        for index in range(4):
            (self.repository / f"new-{index}.txt").write_text(
                f"new {index}\n",
                encoding="utf-8",
            )

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "新增四个交付文件",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
        )

        self.assertIn("4 个文件", result.stdout)
        self.assertNotIn(" 等", result.stdout)
        manifest = self.read_manifest(result)
        for index in range(4):
            self.assertIn(f"<code>new-{index}.txt</code>", manifest)

    def test_render_rejects_missing_change_summary_when_files_changed(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        (self.repository / "tracked.txt").write_text("changed\n", encoding="utf-8")

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
            check=False,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("检测到文件变化，但缺少改动说明", result.stderr)

    def test_render_rejects_summary_over_500_characters(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "未修改文件",
            "--verification",
            "验" * 480,
            "--risks",
            "无",
            check=False,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("超过 500 个字符", result.stderr)

    def test_render_rejects_multiline_fields(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "未修改文件",
            "--verification",
            "单元测试通过\n伪造第四行",
            "--risks",
            "无已知剩余风险",
            check=False,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("验证说明必须是单行文本", result.stderr)

    def test_render_reports_tracked_symlink_change(self) -> None:
        link = self.repository / "linked.txt"
        os.symlink("../first-target.txt", link)
        self.run_git("add", "linked.txt")
        self.run_git("commit", "-qm", "add symlink")
        self.run_script("snapshot", "--output", str(self.snapshot))
        link.unlink()
        os.symlink("../second-target.txt", link)

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "更新符号链接目标",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
        )

        manifest = self.read_manifest(result)
        self.assertIn("| M | <code>linked.txt</code> |", manifest)
        self.assertIn("-../first-target.txt", manifest)
        self.assertIn("+../second-target.txt", manifest)

    def test_render_reports_deleted_file(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        (self.repository / "tracked.txt").unlink()

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "删除废弃文件",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
        )

        manifest = self.read_manifest(result)
        self.assertIn("| D | <code>tracked.txt</code> |", manifest)
        self.assertIn("-baseline", manifest)

    def test_render_from_subdirectory_reports_root_change(self) -> None:
        nested_directory = self.repository / "nested"
        nested_directory.mkdir()
        self.run_script(
            "snapshot",
            "--output",
            str(self.snapshot),
            cwd=nested_directory,
        )
        (self.repository / "tracked.txt").write_text("changed\n", encoding="utf-8")

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "更新根目录文件",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
            cwd=nested_directory,
        )

        manifest = self.read_manifest(result)
        self.assertIn("| M | <code>tracked.txt</code> |", manifest)

    def test_render_rejects_malformed_snapshot(self) -> None:
        self.snapshot.write_text("{invalid", encoding="utf-8")

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "未修改文件",
            "--verification",
            "未运行：快照损坏",
            "--risks",
            "无法生成交付结论",
            check=False,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("无法读取交付快照", result.stderr)

    def test_render_rejects_non_object_snapshot(self) -> None:
        self.snapshot.write_text("[]\n", encoding="utf-8")

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "未修改文件",
            "--verification",
            "未运行：快照损坏",
            "--risks",
            "无法生成交付结论",
            check=False,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("交付快照根节点必须是对象", result.stderr)

    def test_render_rejects_manifest_inside_repository(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
        snapshot["manifest_path"] = str(self.repository / "manifest.md")
        self.snapshot.write_text(
            json.dumps(snapshot, ensure_ascii=False),
            encoding="utf-8",
        )

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "未修改文件",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
            check=False,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("Manifest 不能位于仓库内", result.stderr)

    def test_snapshot_records_repository_identity(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))

        data = json.loads(self.snapshot.read_text(encoding="utf-8"))

        self.assertEqual(2, data["version"])
        self.assertEqual(str(self.repository.resolve()), data["repository_root"])
        self.assertIn("tracked.txt", data["files"])
        self.assertTrue(Path(data["baseline_directory"]).is_dir())
        self.assertTrue(Path(data["manifest_path"]).is_absolute())

    def test_render_reports_binary_change_without_embedding_bytes(self) -> None:
        binary = self.repository / "binary.dat"
        binary.write_bytes(b"before\x00data")
        self.run_git("add", "binary.dat")
        self.run_git("commit", "-qm", "add binary")
        self.run_script("snapshot", "--output", str(self.snapshot))
        binary.write_bytes(b"after\x00data")

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--changes",
            "更新二进制数据",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
        )

        manifest = self.read_manifest(result)
        self.assertIn("二进制内容变化", manifest)
        self.assertIn("SHA-256", manifest)
        self.assertNotIn("before", manifest)
        self.assertNotIn("after", manifest)

    def read_manifest(self, result: subprocess.CompletedProcess[str]) -> str:
        manifest_path = self.manifest_path(result)
        self.generated_artifacts.append(manifest_path)
        self.assertTrue(manifest_path.is_file())
        return manifest_path.read_text(encoding="utf-8")

    def manifest_path(self, result: subprocess.CompletedProcess[str]) -> Path:
        match = re.search(r"\[查看完整交付记录\]\(<([^>]+)>\)", result.stdout)
        self.assertIsNotNone(match)
        return Path(match.group(1))

    def run_git(self, *arguments: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["git", *arguments],
            cwd=self.repository,
            check=True,
            capture_output=True,
            text=True,
        )

    def run_script(
        self,
        *arguments: str,
        check: bool = True,
        cwd: Path | None = None,
    ) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["python3", str(SCRIPT), *arguments],
            cwd=cwd or self.repository,
            check=check,
            capture_output=True,
            text=True,
        )


if __name__ == "__main__":
    unittest.main()
