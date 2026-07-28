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
        self.inputs_directory = tempfile.TemporaryDirectory()
        self.inputs = Path(self.inputs_directory.name)
        self.run_git("init", "-q")
        self.run_git("config", "user.email", "agent@example.com")
        self.run_git("config", "user.name", "Agent")
        (self.repository / "tracked.txt").write_text("baseline\n", encoding="utf-8")
        self.run_git("add", "tracked.txt")
        self.run_git("commit", "-qm", "baseline")
        self.snapshot = self.repository / "snapshot.json"
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
        if self.snapshot.exists():
            try:
                snapshot = json.loads(self.snapshot.read_text(encoding="utf-8"))
                if isinstance(snapshot, dict):
                    baseline_directory = snapshot.get("baseline_directory")
                    if isinstance(baseline_directory, str):
                        shutil.rmtree(baseline_directory, ignore_errors=True)
            except (OSError, json.JSONDecodeError):
                pass
        for artifact in self.generated_artifacts:
            artifact.unlink(missing_ok=True)
        self.inputs_directory.cleanup()
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

        self.assertEqual(3, data["version"])
        self.assertEqual(str(self.repository.resolve()), data["repository_root"])
        self.assertIn("tracked.txt", data["files"])
        self.assertTrue(Path(data["baseline_directory"]).is_dir())
        self.assertEqual("只迁移倍速设置，保留既有主题，不新增主题。", data["contract"]["raw_request"])
        self.assertEqual("writer-a", data["writer"]["current"])

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

        processes = [
            subprocess.Popen(
                command,
                cwd=self.repository,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
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
            prepared.extend(
                (
                    "--request-file",
                    str(self.request_file),
                    "--requirements-file",
                    str(self.requirements_file),
                    "--writer-id",
                    "writer-a",
                )
            )
        if prepared and prepared[0] == "render":
            prepared = self.prepare_render_arguments(prepared)
        return subprocess.run(
            ["python3", str(SCRIPT), *prepared],
            cwd=cwd or self.repository,
            check=check,
            capture_output=True,
            text=True,
        )

    def prepare_render_arguments(self, arguments: list[str]) -> list[str]:
        snapshot_path = Path(arguments[arguments.index("--snapshot") + 1])
        try:
            snapshot = json.loads(snapshot_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return arguments
        if not isinstance(snapshot, dict) or snapshot.get("version") != 3:
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
                "--verdicts-file",
                str(verdicts_file),
                "--verification-evidence-file",
                str(self.verification_evidence_file),
            ],
            cwd=self.repository,
            check=True,
            capture_output=True,
            text=True,
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
