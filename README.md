# ai_demo
创建一个aiagent项目
create-order → check-inventory → ai-decide → human-review → ⏸ execute → complete
↓
(写入Redis + withHumanFeedback暂停)
↓
审核通过/拒绝 → resume() → execute/complete