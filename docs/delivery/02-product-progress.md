# MVP 产品进度看板

最后更新：2026-07-02

## 1. 产品最终目标

law4x MVP 要打通一个可真实使用的法律法规 RAG 产品：

```text
法规数据入库
 -> 法条检索
 -> 用户提问
 -> 检索相关法条
 -> 生成带引用回答
 -> 展示引用依据
 -> 支持专业用户整理依据
```

MVP 不是 AI 律师，不输出无来源结论，不承诺诉讼结果。所有回答必须能回到具体法规条文。

## 2. 当前产品策略

先把产品做起来，再补评测和运营闭环。

说明：

- `backend/src/main/resources/static/` 当前作为联调测试页保留。
- 正式产品前端从本次开始迁移到 `React + AG-UI` 架构。

当前优先级：

1. 公众咨询页可用。
2. 引用依据面板可用。
3. 专业检索页可用。
4. 法规库浏览可用。
5. 补齐 AG-UI 对话状态模型与 tool 编排。
6. 最后补 RAG 评价、MVP 评测集和质量看板。

## 3. 已完成能力

### 数据和知识库

- [x] PostgreSQL + pgvector 本地数据库设计。
- [x] 民法典 docx 离线解析和入库脚本。
- [x] `law_documents`、`law_articles`、`law_article_embeddings` 等基础表。
- [x] 法条按法规、章节路径、条号、正文存储。

### 后端检索

- [x] Spring Boot 后端骨架。
- [x] DDD 分层骨架。
- [x] 法条关键词检索。
- [x] 法条详情查询。
- [x] DashScope embedding 适配。
- [x] 缺失 embedding 生成接口。
- [x] pgvector 相似法条检索。
- [x] `/api/rag/search` 支持 keyword + vector hybrid 合并。
- [x] 自然语言问题 `别人欠钱不还怎么办` 可返回相关法条。

### RAG 回答

- [x] `/api/rag/test-runs` 保存检索快照。
- [x] `/api/rag/answer` 基于检索证据生成回答和 citations。
- [x] 回答链路统一收敛到 AgentScope。
- [x] 旧的 fallback / 多 provider 适配代码已删除。
- [x] 当前基于 AgentScope `ReActAgent` 驱动问答。
- [x] 已为 workspace / memory / skill / sandbox / plan mode 预留宿主骨架。
- [x] 已接入 structured output 约束，当前采用 `json_object + middleware + 严格解析`。

### 工程质量

- [x] AgentScope Java v2.0.0-RC3 依赖校准。
- [x] 后端单元测试和集成测试覆盖主要 use case。
- [x] 当前问答主链路相关测试通过：
  - `AgentScopeRagAnswerClientTest`
  - `AgentScopeAnswerConfigurationTest`
  - `CreateRagAnswerUseCaseTest`

## 4. 未完成能力

### P0：产品可用闭环

- [x] Web 前端工程。
- [x] 公众咨询页。
- [x] 调用 `POST /api/rag/answer`。
- [x] 展示回答正文。
- [x] 展示 citations 引用依据。
- [x] 引用依据面板。
- [x] 引用条文详情展开。
- [x] 基础错误、加载、空结果状态。
- [x] 本地前后端联调说明。

### P1：专业用户功能

- [x] 专业检索页。
- [x] 调用 `GET /api/law/articles/search` 和 `/api/rag/search`。
- [x] 检索结果显示命中方式和相关度。
- [x] 已选依据清单。
- [x] 复制法律依据。
- [x] 导出 Markdown。
- [x] 生成法律依据段落。

### P1：法规库浏览

- [x] 法规库页。
- [x] 法条详情页或详情抽屉。
- [x] 按法规、章节、条号浏览。
- [x] 从 citation 跳转到原文条文。

### P2：流式和 Agent 编排

- [x] AG-UI `/ag-ui` 接口。
- [x] SSE 流式回答。
- [ ] tool call 过程展示。
- [ ] AgentScope Java v2 RAG tool：`searchLawArticles`。
- [ ] AgentScope Java v2 RAG tool：`getArticleDetail`。
- [ ] 引用校验 tool：`validateCitations`。
- [ ] AG-UI `citations / answerSegments / state` 完整同步。
- [ ] 会话持久化与历史对话恢复。

### P2：质量闭环

- [ ] RAG test run 人工评价字段。
- [ ] 评价更新接口。
- [ ] 10 个 MVP 评测问题。
- [ ] 批量评测运行。
- [ ] 引用真实性检查。
- [ ] 质量回归看板。

## 5. 当前下一步

当前应做：

```text
进入 P2
 -> 把现有 law/rag use case 提升为 AgentScope tools
 -> 把 citations / answerSegments 接入 AG-UI state
 -> 补齐会话持久化与历史对话
```

验收标准：

- `/ag-ui` 已稳定承接正式前端工作台接入。
- 回答过程已支持流式返回。
- AgentScope RAG tools 形成后续 Agent 编排基础能力。
- 前端能基于 AG-UI 直接消费 `citations / answerSegments / conversation state`。

## 6. 后续推进规则

每完成一个功能，必须更新本文件：

- 把对应 checklist 从 `[ ]` 改成 `[x]`。
- 更新“最后更新”日期。
- 如有新发现的缺口，加入对应优先级。
- 每次开始新任务前，先看“当前下一步”。

不在当前优先级里的功能先不做，避免偏离产品闭环。
