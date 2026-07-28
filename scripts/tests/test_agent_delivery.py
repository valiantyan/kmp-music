import json
import os
import re
import shutil
import subprocess
import tempfile
import unittest
import uuid
from pathlib import Path
from unittest import mock

from scripts import agent_delivery


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
SCRIPT = REPOSITORY_ROOT / "scripts" / "agent_delivery.py"


class AgentDeliveryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_directory = tempfile.TemporaryDirectory()
        self.repository = Path(self.temp_directory.name)
        self.inputs_directory = tempfile.TemporaryDirectory()
        self.inputs = Path(self.inputs_directory.name)
        self.state_directory = tempfile.TemporaryDirectory()
        self.reviewer_id = f"reviewer-{uuid.uuid4()}"
        self.run_git("init", "-q")
        self.run_git("config", "user.email", "agent@example.com")
        self.run_git("config", "user.name", "Agent")
        (self.repository / "tracked.txt").write_text("baseline\n", encoding="utf-8")
        self.run_git("add", "tracked.txt")
        self.run_git("commit", "-qm", "baseline")
        self.snapshot = self.repository / "snapshot.json"
        self.snapshot_paths = [self.snapshot]
        self.generated_artifacts: list[Path] = []
        self.request_file = self.inputs / "request.md"
        self.requirements_file = self.inputs / "requirements.json"
        self.verification_evidence_file = self.inputs / "verification.md"
        self.request_file.write_text(
            "只迁移倍速设置，保留既有主题，不新增主题。\n",
            encoding="utf-8",
        )
        self.requirements_file.write_text(
            json.dumps(
                [
                    {"id": "M1", "kind": "MUST", "text": "只迁移倍速设置"},
                    {"id": "F1", "kind": "FORBIDDEN", "text": "不得新增主题"},
                    {"id": "U1", "kind": "UNCHANGED", "text": "保留既有主题"},
                ],
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )
        self.verification_evidence_file.write_text(
            "python3 -m unittest：通过\n",
            encoding="utf-8",
        )

    def tearDown(self) -> None:
        for snapshot_path in self.snapshot_paths:
            if not snapshot_path.exists():
                continue
            try:
                snapshot = json.loads(snapshot_path.read_text(encoding="utf-8"))
                if isinstance(snapshot, dict):
                    baseline_directory = snapshot.get("baseline_directory")
                    if isinstance(baseline_directory, str):
                        shutil.rmtree(baseline_directory, ignore_errors=True)
            except (OSError, json.JSONDecodeError):
                pass
        for artifact in self.generated_artifacts:
            artifact.unlink(missing_ok=True)
        self.inputs_directory.cleanup()
        self.state_directory.cleanup()
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
        self.assertIn("| 基线既有未变 | 未出现在任务级 diff，禁止归因本轮 | 0 个文件 |", manifest)
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
        self.assertIn("用户已有改动", manifest)
        self.assertNotIn("-baseline", manifest)
        self.assertNotIn("+user change", manifest)

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

    def test_render_creates_unique_manifest_for_same_snapshot(self) -> None:
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
        self.assertNotEqual(first_path, second_path)
        self.assertTrue(first_path.is_file())
        self.assertTrue(second_path.is_file())

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

    def test_review_rejects_report_inside_repository(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        verdicts_file = self.write_passing_verdicts()

        result = self.run_script(
            "review",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--verdicts-file",
            str(verdicts_file),
            "--verification-evidence-file",
            str(self.verification_evidence_file),
            "--output",
            str(self.repository / "review.json"),
            check=False,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("规格审查报告必须位于仓库外", result.stderr)

    def test_snapshot_records_repository_identity(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))

        data = json.loads(self.snapshot.read_text(encoding="utf-8"))

        self.assertEqual(4, data["version"])
        self.assertEqual(str(self.repository.resolve()), data["repository_root"])
        self.assertIn("tracked.txt", data["files"])
        self.assertTrue(Path(data["baseline_directory"]).is_dir())
        self.assertEqual("只迁移倍速设置，保留既有主题，不新增主题。", data["contract"]["raw_request"])
        self.assertEqual("writer-a", data["writer"]["current"])
        self.assertEqual("ACTIVE", data["task"]["lifecycle"])
        self.assertEqual(data["contract"]["digest"], data["task"]["contract_digest"])
        self.assertEqual(str(uuid.UUID(data["task"]["id"])), data["task"]["id"])

    def test_task_id_digest_rejects_silent_replacement(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
        snapshot["task"]["id"] = str(uuid.uuid4())
        self.snapshot.write_text(
            json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

        result = self.run_script(
            "status",
            "--snapshot",
            str(self.snapshot),
            check=False,
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("task_id 身份摘要不匹配", result.stderr)

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

    def test_rework_appends_version_without_overwriting_original_contract(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        before = json.loads(self.snapshot.read_text(encoding="utf-8"))
        instruction = self.inputs / "rework.md"
        instruction.write_text("删除既有主题。\n", encoding="utf-8")

        result = self.run_script(
            "append-rework",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--expected-version",
            "1",
            "--instruction-file",
            str(instruction),
        )

        self.assertEqual("v2", result.stdout.strip())
        after = json.loads(self.snapshot.read_text(encoding="utf-8"))
        self.assertEqual(before["contract"], after["contract"])
        self.assertEqual("删除既有主题。", after["rework_history"][0]["instruction"])
        self.assertEqual(2, after["rework_history"][0]["version"])

        stale_append = self.run_script(
            "append-rework",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--expected-version",
            "1",
            "--instruction-file",
            str(instruction),
            check=False,
        )
        self.assertNotEqual(0, stale_append.returncode)
        self.assertIn("当前版本是 2", stale_append.stderr)

    def test_contract_digest_rejects_silent_overwrite(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
        snapshot["contract"]["raw_request"] = "删除既有主题"
        self.snapshot.write_text(
            json.dumps(snapshot, ensure_ascii=False),
            encoding="utf-8",
        )

        result = self.run_script(
            "review",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--verdicts-file",
            str(self.inputs / "missing.json"),
            "--verification-evidence-file",
            str(self.verification_evidence_file),
            check=False,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("原始验收合同摘要不匹配", result.stderr)

    def test_concurrent_rework_only_appends_one_version(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        instruction = self.inputs / "concurrent-rework.md"
        instruction.write_text("只保留倍速迁移。\n", encoding="utf-8")
        command = [
            "python3",
            str(SCRIPT),
            "append-rework",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--expected-version",
            "1",
            "--instruction-file",
            str(instruction),
        ]
        environment = {
            **os.environ,
            "KMP_MUSIC_AGENT_DELIVERY_STATE_DIR": self.state_directory.name,
        }

        processes = [
            subprocess.Popen(
                command,
                cwd=self.repository,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                env=environment,
            )
            for _ in range(6)
        ]
        results = [process.communicate() + (process.returncode,) for process in processes]

        self.assertEqual(1, sum(returncode == 0 for _, _, returncode in results))
        snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
        self.assertEqual(1, len(snapshot["rework_history"]))
        self.assertEqual(2, snapshot["rework_history"][0]["version"])
        self.assertTrue(
            all(
                "当前版本是 2" in stderr
                for _, stderr, returncode in results
                if returncode != 0
            )
        )

    def test_takeover_requires_confirmed_writer_termination(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))

        rejected = self.run_script(
            "takeover",
            "--snapshot",
            str(self.snapshot),
            "--expected-writer",
            "writer-a",
            "--new-writer",
            "writer-b",
            check=False,
        )

        self.assertNotEqual(0, rejected.returncode)
        self.assertIn("尚未确认终止", rejected.stderr)

        self_confirmed = self.run_script(
            "confirm-terminated",
            "--snapshot",
            str(self.snapshot),
            "--expected-writer",
            "writer-a",
            "--confirmed-by",
            "writer-a",
            "--evidence",
            "writer-a 自称已经停止",
            check=False,
        )
        self.assertNotEqual(0, self_confirmed.returncode)
        self.assertIn("不能自行确认终止", self_confirmed.stderr)

        self.run_script(
            "confirm-terminated",
            "--snapshot",
            str(self.snapshot),
            "--expected-writer",
            "writer-a",
            "--confirmed-by",
            "coordinator",
            "--evidence",
            "interrupt_agent 返回 writer-a 已停止",
        )
        self.run_script(
            "takeover",
            "--snapshot",
            str(self.snapshot),
            "--expected-writer",
            "writer-a",
            "--new-writer",
            "writer-b",
        )

        snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
        self.assertEqual("writer-b", snapshot["writer"]["current"])
        self.assertEqual("active", snapshot["writer"]["status"])
        self.assertEqual(
            ["claimed", "termination_confirmed", "takeover"],
            [event["event"] for event in snapshot["writer"]["history"]],
        )

    def test_review_requires_all_ids_and_non_build_evidence(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        incomplete = self.inputs / "incomplete-verdicts.json"
        incomplete.write_text(
            json.dumps(
                [
                    {
                        "id": "M1",
                        "status": "PASS",
                        "evidence": [{"source": "test", "detail": "测试通过"}],
                    }
                ],
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

        missing = self.run_script(
            "review",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--verdicts-file",
            str(incomplete),
            "--verification-evidence-file",
            str(self.verification_evidence_file),
            check=False,
        )
        self.assertNotEqual(0, missing.returncode)
        self.assertIn("缺少 requirement ID：F1, U1", missing.stderr)

        build_only = self.inputs / "build-only-verdicts.json"
        build_only.write_text(
            json.dumps(
                [
                    {
                        "id": identifier,
                        "status": "PASS",
                        "evidence": [{"source": "build", "detail": "构建通过"}],
                    }
                    for identifier in ("M1", "F1", "U1")
                ],
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )
        proxy_only = self.run_script(
            "review",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--verdicts-file",
            str(build_only),
            "--verification-evidence-file",
            str(self.verification_evidence_file),
            check=False,
        )
        self.assertNotEqual(0, proxy_only.returncode)
        self.assertIn("不能只用构建或合同文本", proxy_only.stderr)

    def test_render_rejects_review_after_task_diff_changes(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        (self.repository / "tracked.txt").write_text("first change\n", encoding="utf-8")
        review_path = self.create_review(self.snapshot, "writer-a")
        (self.repository / "tracked.txt").write_text("second change\n", encoding="utf-8")

        result = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--review-report",
            str(review_path),
            "--changes",
            "更新文件",
            "--verification",
            "单元测试通过",
            "--risks",
            "无已知剩余风险",
            check=False,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("任务级 diff 已过期", result.stderr)

    def test_original_regression_preserves_theme_and_old_manifests(self) -> None:
        theme = self.repository / "theme.json"
        settings = self.repository / "settings.json"
        theme.write_text('{"theme":"existing"}\n', encoding="utf-8")
        settings.write_text('{"volume":80}\n', encoding="utf-8")
        self.run_git("add", "theme.json", "settings.json")
        self.run_git("commit", "-qm", "add existing settings")
        self.run_script("snapshot", "--output", str(self.snapshot))
        rework = self.inputs / "drifting-rework.md"
        rework.write_text("删除既有主题。\n", encoding="utf-8")
        self.run_script(
            "append-rework",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--expected-version",
            "1",
            "--instruction-file",
            str(rework),
        )
        settings.write_text('{"volume":80,"playbackSpeed":1.25}\n', encoding="utf-8")

        verdicts = self.inputs / "regression-verdicts.json"
        verdicts.write_text(
            json.dumps(
                [
                    {
                        "id": "M1",
                        "status": "PASS",
                        "evidence": [{"source": "task_diff", "detail": "只新增 playbackSpeed"}],
                    },
                    {
                        "id": "F1",
                        "status": "PASS",
                        "evidence": [{"source": "task_diff", "detail": "未新增主题 key"}],
                    },
                    {
                        "id": "U1",
                        "status": "PASS",
                        "evidence": [{"source": "task_diff", "detail": "theme.json 不在任务 diff"}],
                    },
                ],
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )
        review_result = self.run_script(
            "review",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--verdicts-file",
            str(verdicts),
            "--verification-evidence-file",
            str(self.verification_evidence_file),
        )
        review_path = Path(review_result.stdout.strip())
        self.generated_artifacts.append(review_path)
        review_report = json.loads(review_path.read_text(encoding="utf-8"))
        self.assertIn("删除既有主题", review_report["review_inputs"]["rework_instructions"][0])
        self.assertEqual(["M1", "F1", "U1"], [item["id"] for item in review_report["verdicts"]])

        render_arguments = (
            "render",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--review-report",
            str(review_path),
            "--changes",
            "只迁移倍速设置并保留既有主题",
            "--verification",
            "原问题回归通过",
            "--risks",
            "无已知剩余风险",
        )
        first = self.run_script(*render_arguments)
        second = self.run_script(*render_arguments)
        first_path = self.manifest_path(first)
        second_path = self.manifest_path(second)
        self.generated_artifacts.extend((first_path, second_path))
        self.assertNotEqual(first_path, second_path)
        self.assertTrue(first_path.is_file())
        self.assertTrue(second_path.is_file())
        first_manifest = first_path.read_text(encoding="utf-8")
        self.assertIn("<code>settings.json</code>", first_manifest)
        self.assertNotIn("<code>theme.json</code>", first_manifest)
        self.assertIn("playbackSpeed", first_manifest)
        self.assertIn("验证证据", first_manifest)
        self.assertIn("| 基线既有未变 | 未出现在任务级 diff，禁止归因本轮 | 2 个文件 |", first_manifest)
        snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
        self.assertEqual(
            ["claimed"],
            [event["event"] for event in snapshot["writer"]["history"]],
        )

    def test_same_task_clarification_and_review_fix_can_complete(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        instruction = self.inputs / "clarification.md"
        instruction.write_text("仍只迁移倍速设置，并重新验证。\n", encoding="utf-8")
        self.run_script(
            "append-rework",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--expected-version",
            "1",
            "--instruction-file",
            str(instruction),
        )
        (self.repository / "tracked.txt").write_text("first fix\n", encoding="utf-8")
        first_review = self.create_review(self.snapshot, "writer-a")
        _, first_receipt = self.render_candidate(self.snapshot, first_review)
        first_approval = self.approve_candidate(first_review, first_receipt)

        (self.repository / "tracked.txt").write_text("review fix\n", encoding="utf-8")
        stale = self.run_script(
            "complete",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--review-report",
            str(first_review),
            "--render-receipt",
            str(first_receipt),
            "--review-approval",
            str(first_approval),
            check=False,
        )
        self.assertNotEqual(0, stale.returncode)
        self.assertIn("任务级 diff 已过期", stale.stderr)

        second_review = self.create_review(self.snapshot, "writer-a")
        candidate, second_receipt = self.render_candidate(self.snapshot, second_review)
        second_approval = self.approve_candidate(second_review, second_receipt)
        completed = self.run_script(
            "complete",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--review-report",
            str(second_review),
            "--render-receipt",
            str(second_receipt),
            "--review-approval",
            str(second_approval),
        )
        self.assertEqual(candidate.stdout, completed.stdout)
        snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
        self.assertEqual("COMPLETED", snapshot["task"]["lifecycle"])
        self.assertEqual("closed", snapshot["writer"]["status"])
        self.assertEqual("CLOSED", snapshot["task"]["resources"]["contract"])
        self.assertEqual("CLOSED", snapshot["task"]["resources"]["writer"])

    def test_completed_task_rejects_all_delivery_reuse_commands(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        review_path = self.create_review(self.snapshot, "writer-a")
        _, receipt_path = self.render_candidate(self.snapshot, review_path)
        approval_path = self.approve_candidate(review_path, receipt_path)
        self.run_script(
            "complete",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--review-report",
            str(review_path),
            "--render-receipt",
            str(receipt_path),
            "--review-approval",
            str(approval_path),
        )
        instruction = self.inputs / "post-complete.md"
        instruction.write_text("开始另一个目标。\n", encoding="utf-8")
        verdicts = self.write_passing_verdicts()
        rejected_commands = [
            (
                "append-rework", "--snapshot", str(self.snapshot), "--writer-id", "writer-a",
                "--expected-version", "1", "--instruction-file", str(instruction),
            ),
            (
                "confirm-terminated", "--snapshot", str(self.snapshot), "--expected-writer",
                "writer-a", "--confirmed-by", "coordinator", "--evidence", "已终止",
            ),
            (
                "takeover", "--snapshot", str(self.snapshot), "--expected-writer", "writer-a",
                "--new-writer", "writer-b",
            ),
            (
                "review", "--snapshot", str(self.snapshot), "--writer-id", "writer-a",
                "--reviewer-id", self.reviewer_id, "--verdicts-file", str(verdicts),
                "--verification-evidence-file", str(self.verification_evidence_file),
            ),
            (
                "render", "--snapshot", str(self.snapshot), "--writer-id", "writer-a",
                "--review-report", str(review_path), "--changes", "未修改文件",
                "--verification", "单元测试通过", "--risks", "无已知剩余风险",
                "--receipt-output", str(self.inputs / "post-complete-receipt.json"),
            ),
            (
                "approve", "--snapshot", str(self.snapshot), "--reviewer-id",
                self.reviewer_id, "--review-report", str(review_path),
                "--render-receipt", str(receipt_path), "--output",
                str(self.inputs / "post-complete-approval.json"),
            ),
            (
                "complete", "--snapshot", str(self.snapshot), "--writer-id", "writer-a",
                "--review-report", str(review_path), "--render-receipt", str(receipt_path),
                "--review-approval", str(approval_path),
            ),
        ]
        for command in rejected_commands:
            with self.subTest(command=command[0]):
                result = self.run_script(*command, check=False)
                self.assertNotEqual(0, result.returncode)
                self.assertIn("已 COMPLETED", result.stderr)

        status = self.run_script("status", "--snapshot", str(self.snapshot))
        self.assertEqual("COMPLETED", json.loads(status.stdout)["lifecycle"])
        new_snapshot = self.run_script(
            "snapshot",
            "--output",
            str(self.inputs / "post-complete-new-task.json"),
            "--writer-id",
            "writer-a",
            check=False,
        )
        self.assertNotEqual(0, new_snapshot.returncode)
        self.assertIn("新任务必须创建新 agent", new_snapshot.stderr)

    def test_completed_registry_tombstone_rejects_copy_and_rollback(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        active_snapshot = self.snapshot.read_text(encoding="utf-8")
        copied_snapshot = self.inputs / "active-copy.json"
        copied_snapshot.write_text(active_snapshot, encoding="utf-8")
        self.snapshot_paths.append(copied_snapshot)
        instruction = self.inputs / "rollback-rework.md"
        instruction.write_text("复活旧任务。\n", encoding="utf-8")
        active_copy_reuse = self.run_script(
            "append-rework",
            "--snapshot",
            str(copied_snapshot),
            "--writer-id",
            "writer-a",
            "--expected-version",
            "1",
            "--instruction-file",
            str(instruction),
            check=False,
        )
        self.assertNotEqual(0, active_copy_reuse.returncode)
        self.assertIn("规范 snapshot 路径", active_copy_reuse.stderr)
        review_path = self.create_review(self.snapshot, "writer-a")
        _, receipt_path = self.render_candidate(self.snapshot, review_path)
        approval_path = self.approve_candidate(review_path, receipt_path)
        self.run_script(
            "complete",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--review-report",
            str(review_path),
            "--render-receipt",
            str(receipt_path),
            "--review-approval",
            str(approval_path),
        )
        copied_reuse = self.run_script(
            "append-rework",
            "--snapshot",
            str(copied_snapshot),
            "--writer-id",
            "writer-a",
            "--expected-version",
            "1",
            "--instruction-file",
            str(instruction),
            check=False,
        )
        self.assertNotEqual(0, copied_reuse.returncode)
        self.assertIn("已 COMPLETED", copied_reuse.stderr)

        self.snapshot.write_text(active_snapshot, encoding="utf-8")
        rollback_reuse = self.run_script(
            "append-rework",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--expected-version",
            "1",
            "--instruction-file",
            str(instruction),
            check=False,
        )
        self.assertNotEqual(0, rollback_reuse.returncode)
        self.assertIn("已 COMPLETED", rollback_reuse.stderr)
        status = json.loads(
            self.run_script("status", "--snapshot", str(self.snapshot)).stdout
        )
        self.assertEqual("COMPLETED", status["lifecycle"])
        self.assertEqual("ACTIVE", status["local_lifecycle"])
        self.assertTrue(status["snapshot_path_match"])

    def test_completed_registry_tombstone_survives_snapshot_write_failure(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        completed_snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
        completed_snapshot["task"]["completed_at"] = "2026-07-29T00:00:00+00:00"
        registry_path = Path(self.state_directory.name) / "identity-registry.json"
        written_paths: list[Path] = []
        real_atomic_write_json = agent_delivery.atomic_write_json

        def fail_after_first_write(path: Path, payload: object) -> None:
            real_atomic_write_json(path, payload)
            written_paths.append(path.resolve())
            raise OSError("simulated process failure after the first terminal write")

        with mock.patch.dict(
            os.environ,
            {"KMP_MUSIC_AGENT_DELIVERY_STATE_DIR": self.state_directory.name},
        ):
            with mock.patch.object(
                agent_delivery,
                "atomic_write_json",
                side_effect=fail_after_first_write,
            ):
                with self.assertRaises(agent_delivery.DeliveryError):
                    agent_delivery.mark_registered_task_completed(
                        self.snapshot,
                        completed_snapshot,
                    )

        self.assertEqual([registry_path.resolve()], written_paths)
        registry = json.loads(registry_path.read_text(encoding="utf-8"))
        task_id = completed_snapshot["task"]["id"]
        self.assertEqual("COMPLETED", registry["tasks"][task_id]["lifecycle"])
        local_snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
        self.assertEqual("ACTIVE", local_snapshot["task"]["lifecycle"])

        instruction = self.inputs / "interrupted-complete-rework.md"
        instruction.write_text("复活中断的旧任务。\n", encoding="utf-8")
        rejected = self.run_script(
            "append-rework",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--expected-version",
            "1",
            "--instruction-file",
            str(instruction),
            check=False,
        )
        self.assertNotEqual(0, rejected.returncode)
        self.assertIn("已 COMPLETED", rejected.stderr)

    def test_new_snapshot_gets_new_task_id(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        rejected_snapshot = self.inputs / "reused-writer-snapshot.json"
        rejected = self.run_script(
            "snapshot",
            "--output",
            str(rejected_snapshot),
            "--writer-id",
            "writer-a",
            check=False,
        )
        self.assertNotEqual(0, rejected.returncode)
        self.assertIn("新任务必须创建新 agent", rejected.stderr)

        other_snapshot = self.inputs / "other-snapshot.json"
        self.snapshot_paths.append(other_snapshot)
        self.run_script(
            "snapshot",
            "--output",
            str(other_snapshot),
            "--writer-id",
            "writer-b",
        )

        first = json.loads(self.snapshot.read_text(encoding="utf-8"))
        second = json.loads(other_snapshot.read_text(encoding="utf-8"))
        self.assertNotEqual(first["task"]["id"], second["task"]["id"])
        self.assertEqual("ACTIVE", first["task"]["lifecycle"])
        self.assertEqual("ACTIVE", second["task"]["lifecycle"])

    def test_reviewer_cannot_be_reused_for_different_task(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        self.create_review(self.snapshot, "writer-a")
        other_snapshot = self.inputs / "reviewer-other-snapshot.json"
        self.snapshot_paths.append(other_snapshot)
        self.run_script(
            "snapshot",
            "--output",
            str(other_snapshot),
            "--writer-id",
            "writer-b",
        )
        verdicts = self.write_passing_verdicts(other_snapshot)

        rejected = self.run_script(
            "review",
            "--snapshot",
            str(other_snapshot),
            "--writer-id",
            "writer-b",
            "--reviewer-id",
            self.reviewer_id,
            "--verdicts-file",
            str(verdicts),
            "--verification-evidence-file",
            str(self.verification_evidence_file),
            check=False,
        )
        self.assertNotEqual(0, rejected.returncode)
        self.assertIn("已绑定其他角色或 task_id", rejected.stderr)

    def test_writer_and_reviewer_roles_cannot_overlap(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        verdicts = self.write_passing_verdicts()
        self_review = self.run_script(
            "review",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--reviewer-id",
            "writer-a",
            "--verdicts-file",
            str(verdicts),
            "--verification-evidence-file",
            str(self.verification_evidence_file),
            check=False,
        )
        self.assertNotEqual(0, self_review.returncode)
        self.assertIn("不能与当前 task 的任一 writer 相同", self_review.stderr)

        self.create_review(self.snapshot, "writer-a")
        self.run_script(
            "confirm-terminated",
            "--snapshot",
            str(self.snapshot),
            "--expected-writer",
            "writer-a",
            "--confirmed-by",
            "coordinator",
            "--evidence",
            "writer-a 已终止",
        )
        reviewer_takeover = self.run_script(
            "takeover",
            "--snapshot",
            str(self.snapshot),
            "--expected-writer",
            "writer-a",
            "--new-writer",
            self.reviewer_id,
            check=False,
        )
        self.assertNotEqual(0, reviewer_takeover.returncode)
        self.assertIn("已绑定 task_id", reviewer_takeover.stderr)

    def test_fail_review_cannot_render_or_complete(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        verdicts_path = self.inputs / "failed-verdicts.json"
        verdicts = json.loads(self.write_passing_verdicts().read_text(encoding="utf-8"))
        verdicts[0]["status"] = "FAIL"
        verdicts_path.write_text(
            json.dumps(verdicts, ensure_ascii=False),
            encoding="utf-8",
        )
        review = self.run_script(
            "review",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--verdicts-file",
            str(verdicts_path),
            "--verification-evidence-file",
            str(self.verification_evidence_file),
        )
        review_path = Path(review.stdout.strip())
        self.generated_artifacts.append(review_path)
        rejected = self.run_script(
            "render",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--review-report",
            str(review_path),
            "--changes",
            "未修改文件",
            "--verification",
            "发现失败规格",
            "--risks",
            "任务未通过",
            "--receipt-output",
            str(self.inputs / "failed-receipt.json"),
            check=False,
        )
        self.assertNotEqual(0, rejected.returncode)
        self.assertIn("规格审查仍有 FAIL", rejected.stderr)

    def test_complete_requires_final_reviewer_approval(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        review_path = self.create_review(self.snapshot, "writer-a")
        _, receipt_path = self.render_candidate(self.snapshot, review_path)
        rejected = self.run_script(
            "complete",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--review-report",
            str(review_path),
            "--render-receipt",
            str(receipt_path),
            "--review-approval",
            str(self.inputs / "missing-approval.json"),
            check=False,
        )
        self.assertNotEqual(0, rejected.returncode)
        self.assertIn("无法读取reviewer approval", rejected.stderr)
        snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
        self.assertEqual("ACTIVE", snapshot["task"]["lifecycle"])

    def test_snapshot_output_and_task_id_cannot_be_reused(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        overwrite = self.run_script(
            "snapshot",
            "--output",
            str(self.snapshot),
            "--writer-id",
            "writer-b",
            check=False,
        )
        self.assertNotEqual(0, overwrite.returncode)
        self.assertIn("禁止覆盖", overwrite.stderr)

        other_snapshot = self.inputs / "collision-snapshot.json"
        self.snapshot_paths.append(other_snapshot)
        self.run_script(
            "snapshot",
            "--output",
            str(other_snapshot),
            "--writer-id",
            "writer-b",
        )
        snapshots = [self.snapshot, other_snapshot]
        for snapshot_path in snapshots:
            data = json.loads(snapshot_path.read_text(encoding="utf-8"))
            data["version"] = 3
            data.pop("task")
            data.pop("reviewer")
            snapshot_path.write_text(
                json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
        (Path(self.state_directory.name) / "identity-registry.json").unlink()

        collision_id = str(uuid.uuid4())
        self.run_script(
            "migrate-v3",
            "--snapshot",
            str(self.snapshot),
            "--expected-writer",
            "writer-a",
            "--task-id",
            collision_id,
        )
        collision = self.run_script(
            "migrate-v3",
            "--snapshot",
            str(other_snapshot),
            "--expected-writer",
            "writer-b",
            "--task-id",
            collision_id,
            check=False,
        )
        self.assertNotEqual(0, collision.returncode)
        self.assertIn("task_id", collision.stderr)
        self.assertIn("已注册", collision.stderr)

    def test_v3_snapshot_requires_explicit_migration(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        v4 = json.loads(self.snapshot.read_text(encoding="utf-8"))
        expected_baseline = v4["baseline_directory"]
        expected_contract = v4["contract"]
        expected_writer = v4["writer"]
        v4["version"] = 3
        v4.pop("task")
        v4.pop("reviewer")
        self.snapshot.write_text(
            json.dumps(v4, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        (Path(self.state_directory.name) / "identity-registry.json").unlink()

        status = self.run_script("status", "--snapshot", str(self.snapshot))
        self.assertEqual("migrate-v3", json.loads(status.stdout)["next_command"])
        rejected = self.run_script(
            "append-rework",
            "--snapshot",
            str(self.snapshot),
            "--writer-id",
            "writer-a",
            "--expected-version",
            "1",
            "--instruction-file",
            str(self.request_file),
            check=False,
        )
        self.assertIn("必须先运行 migrate-v3", rejected.stderr)

        migrated_task_id = str(uuid.uuid4())
        result = self.run_script(
            "migrate-v3",
            "--snapshot",
            str(self.snapshot),
            "--expected-writer",
            "writer-a",
            "--task-id",
            migrated_task_id,
        )
        self.assertEqual(migrated_task_id, result.stdout.strip())
        migrated = json.loads(self.snapshot.read_text(encoding="utf-8"))
        self.assertEqual(4, migrated["version"])
        self.assertEqual(expected_baseline, migrated["baseline_directory"])
        self.assertEqual(expected_contract, migrated["contract"])
        self.assertEqual(expected_writer, migrated["writer"])
        self.assertEqual(migrated_task_id, migrated["task"]["id"])

    def test_concurrent_complete_has_one_atomic_winner(self) -> None:
        self.run_script("snapshot", "--output", str(self.snapshot))
        review_path = self.create_review(self.snapshot, "writer-a")
        candidate, receipt_path = self.render_candidate(self.snapshot, review_path)
        approval_path = self.approve_candidate(review_path, receipt_path)
        command = [
            "python3", str(SCRIPT), "complete", "--snapshot", str(self.snapshot),
            "--writer-id", "writer-a", "--review-report", str(review_path),
            "--render-receipt", str(receipt_path), "--review-approval", str(approval_path),
        ]
        environment = {
            **os.environ,
            "KMP_MUSIC_AGENT_DELIVERY_STATE_DIR": self.state_directory.name,
        }
        processes = [
            subprocess.Popen(
                command,
                cwd=self.repository,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                env=environment,
            )
            for _ in range(6)
        ]
        results = [process.communicate() + (process.returncode,) for process in processes]

        winners = [result for result in results if result[2] == 0]
        self.assertEqual(1, len(winners))
        self.assertEqual(candidate.stdout, winners[0][0])
        self.assertTrue(
            all("已 COMPLETED" in stderr for _, stderr, code in results if code != 0)
        )
        snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
        self.assertEqual("COMPLETED", snapshot["task"]["lifecycle"])
        self.assertEqual(
            ["claimed", "completed"],
            [event["event"] for event in snapshot["writer"]["history"]],
        )

    def render_candidate(
        self,
        snapshot_path: Path,
        review_path: Path,
        writer: str = "writer-a",
    ) -> tuple[subprocess.CompletedProcess[str], Path]:
        receipt_path = self.inputs / f"receipt-{uuid.uuid4()}.json"
        result = self.run_script(
            "render",
            "--snapshot",
            str(snapshot_path),
            "--writer-id",
            writer,
            "--review-report",
            str(review_path),
            "--changes",
            "更新 harness 生命周期门禁",
            "--verification",
            "单元测试通过；独立审查候选待确认",
            "--risks",
            "无已知剩余风险",
            "--receipt-output",
            str(receipt_path),
        )
        manifest_path = self.manifest_path(result)
        self.generated_artifacts.append(manifest_path)
        self.assertTrue(receipt_path.is_file())
        return result, receipt_path

    def approve_candidate(
        self,
        review_path: Path,
        receipt_path: Path,
        snapshot_path: Path | None = None,
    ) -> Path:
        approval_path = self.inputs / f"approval-{uuid.uuid4()}.json"
        self.run_script(
            "approve",
            "--snapshot",
            str(snapshot_path or self.snapshot),
            "--reviewer-id",
            self.reviewer_id,
            "--review-report",
            str(review_path),
            "--render-receipt",
            str(receipt_path),
            "--output",
            str(approval_path),
        )
        self.assertTrue(approval_path.is_file())
        return approval_path

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
        prepared = list(arguments)
        if prepared and prepared[0] == "snapshot":
            if "--request-file" not in prepared:
                prepared.extend(("--request-file", str(self.request_file)))
            if "--requirements-file" not in prepared:
                prepared.extend(("--requirements-file", str(self.requirements_file)))
            if "--writer-id" not in prepared:
                prepared.extend(("--writer-id", "writer-a"))
        if prepared and prepared[0] == "review" and "--reviewer-id" not in prepared:
            prepared.extend(("--reviewer-id", self.reviewer_id))
        if prepared and prepared[0] == "render":
            prepared = self.prepare_render_arguments(prepared)
            if "--receipt-output" not in prepared:
                prepared.extend(
                    (
                        "--receipt-output",
                        str(self.inputs / f"render-{uuid.uuid4()}.json"),
                    )
                )
        environment = os.environ.copy()
        environment["KMP_MUSIC_AGENT_DELIVERY_STATE_DIR"] = self.state_directory.name
        return subprocess.run(
            ["python3", str(SCRIPT), *prepared],
            cwd=cwd or self.repository,
            check=check,
            capture_output=True,
            text=True,
            env=environment,
        )

    def prepare_render_arguments(self, arguments: list[str]) -> list[str]:
        snapshot_path = Path(arguments[arguments.index("--snapshot") + 1])
        try:
            snapshot = json.loads(snapshot_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return arguments
        if not isinstance(snapshot, dict) or snapshot.get("version") != 4:
            return arguments
        writer = snapshot["writer"]["current"]
        if "--writer-id" not in arguments:
            arguments.extend(("--writer-id", writer))
        if "--review-report" not in arguments:
            review_path = self.create_review(snapshot_path, writer)
            arguments.extend(("--review-report", str(review_path)))
        return arguments

    def create_review(self, snapshot_path: Path, writer: str) -> Path:
        verdicts_file = self.write_passing_verdicts(snapshot_path)
        result = subprocess.run(
            [
                "python3",
                str(SCRIPT),
                "review",
                "--snapshot",
                str(snapshot_path),
                "--writer-id",
                writer,
                "--reviewer-id",
                self.reviewer_id,
                "--verdicts-file",
                str(verdicts_file),
                "--verification-evidence-file",
                str(self.verification_evidence_file),
            ],
            cwd=self.repository,
            check=True,
            capture_output=True,
            text=True,
            env={
                **os.environ,
                "KMP_MUSIC_AGENT_DELIVERY_STATE_DIR": self.state_directory.name,
            },
        )
        review_path = Path(result.stdout.strip())
        self.generated_artifacts.append(review_path)
        return review_path

    def write_passing_verdicts(self, snapshot_path: Path | None = None) -> Path:
        snapshot = json.loads(
            (snapshot_path or self.snapshot).read_text(encoding="utf-8")
        )
        verdicts = [
            {
                "id": requirement["id"],
                "status": "PASS",
                "evidence": [
                    {
                        "source": "test",
                        "detail": f"回归测试覆盖 {requirement['id']}",
                    }
                ],
            }
            for requirement in snapshot["contract"]["requirements"]
        ]
        path = self.inputs / "verdicts.json"
        path.write_text(
            json.dumps(verdicts, ensure_ascii=False),
            encoding="utf-8",
        )
        return path


if __name__ == "__main__":
    unittest.main()
