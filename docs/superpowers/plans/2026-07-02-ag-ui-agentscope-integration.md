# AG-UI AgentScope Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 law4x 增加基于 AgentScope Java `HarnessAgent` 的 `/ag-ui` 流式接口，并让 React 前端切换到 Vercel AG-UI 驱动的公众咨询链路。

**Architecture:** 后端新增 `HarnessAgent` 与 `/ag-ui/runs` SSE 控制器，复用现有 law/rag use case 作为 AgentScope tools，并通过事件桥接层把 AgentScope `streamEvents()` 映射成前端 AG-UI 可消费事件。前端保留现有页面结构，替换数据层为 AG-UI client adapter，实现回答流式更新、引用依据增量展示和工具过程状态展示。

**Tech Stack:** Spring Boot 3.3, AgentScope Java 2.0.0-RC3, SSE, React 18, Vite, Vercel AG-UI

---

## 当前执行状态（2026-07-02）

- 总体进度：约 `60%`
- 当前判断：`前后端对话流式已经打通，并且已经正式跑在 AG-UI 协议上；当前计划的重点不再是“打通协议”，而是“补齐 tool 化、state/citations 同步和完整产品闭环”`
- 已落地文件：
  - `backend/src/main/java/com/law4x/agui/interfaces/rest/AgUiRunController.java`
  - `backend/src/main/java/com/law4x/agui/infrastructure/agent/AgUiAgentConfiguration.java`
  - `backend/src/main/java/com/law4x/agui/infrastructure/agent/AgUiProtocolConfiguration.java`
  - `backend/src/test/java/com/law4x/agui/interfaces/rest/AgUiRunControllerTest.java`
  - `backend/src/test/java/com/law4x/agui/infrastructure/agent/AgUiAgentConfigurationTest.java`
  - `frontend/src/features/consultation/aguiClient.js`
  - `frontend/src/features/consultation/useAgUiConsultation.js`
  - `frontend/src/features/consultation/ConsultationPage.jsx`
  - `frontend/package.json`
- 主要缺口：
  - `Law4xAgUiToolset`、`CitationValidationService` 还不存在
  - 当前仍主要是 `text stream + statusText`，还没有把 `citations / answerSegments / tool result state` 做成 AG-UI 驱动的完整状态流
  - 会话持久化、历史对话切换、恢复能力还没接上
  - 进度文档还未同步到“协议已通”的新状态

---

### Task 1: 后端 `/ag-ui` 最小闭环

**Files:**
- Create: `backend/src/main/java/com/law4x/agui/interfaces/rest/AgUiRunController.java`
- Create: `backend/src/test/java/com/law4x/agui/interfaces/rest/AgUiRunControllerTest.java`
- Modify: `backend/pom.xml`

**状态：已完成**

- [x] 已有 `/ag-ui/runs` 控制器测试，断言 `text/event-stream`，文件为 `AgUiRunControllerTest`
- [x] 已有 `/ag-ui/runs` 控制器实现，当前由 `AguiAgentAdapter` 直接驱动
- [x] 已有 `AgUiProtocolConfiguration`，完成 `AguiAdapterConfig + AguiAgentAdapter` 装配
- [x] 当前协议链路已经具备可联调能力

**备注：**

- 原计划中单独增加 `AgUiRunStreamService / AgUiEventMapper` 的做法可以不再作为前置必做项。
- 当前更合理的方向是：在现有 `agentscope-extensions-agui` 直连基础上补业务状态，而不是重复包一层协议桥接。

### Task 2: AgentScope `HarnessAgent` 与工具装配

**Files:**
- Create: `backend/src/main/java/com/law4x/agui/infrastructure/agent/AgUiAgentConfiguration.java`
- Create: `backend/src/main/java/com/law4x/agui/infrastructure/agent/Law4xAgUiToolset.java`
- Create: `backend/src/main/java/com/law4x/agui/application/CitationValidationService.java`
- Create: `backend/src/test/java/com/law4x/agui/infrastructure/agent/AgUiAgentConfigurationTest.java`
- Modify: `backend/src/main/resources/application.yml`

**状态：部分完成**

- [x] 已有 `HarnessAgent` Bean 配置测试：`AgUiAgentConfigurationTest`
- [x] 已有 `HarnessAgent.builder()` 最小实现：`AgUiAgentConfiguration`
- [ ] `RuntimeContext` 约定未见落地
- [ ] `searchLawArticles` 未落地为 AG-UI tool
- [ ] `getArticleDetail` 未落地为 AG-UI tool
- [ ] `validateCitations` 未落地为 AG-UI tool
- [ ] `Law4xAgUiToolset` 未创建
- [ ] `CitationValidationService` 未创建
- [ ] 本轮未重新执行后端测试确认

**备注：**

- 当前 Agent 已经能被 AG-UI 协议驱动，但 law/rag 现有 use case 还没有正式提升成 tool 集。

### Task 3: AG-UI 业务状态补齐

**Files:**
- Modify: `frontend/src/features/consultation/useAgUiConsultation.js`
- Modify: `frontend/src/features/consultation/ConsultationPage.jsx`
- Modify: `backend/src/main/java/com/law4x/agui/interfaces/rest/AgUiRunController.java` 或新增业务状态组装层
- Optional: Create `backend/src/main/java/com/law4x/agui/application/AgUiConversationStateService.java`

**状态：未开始**

- [ ] 把 `citations` 变成 AG-UI 运行结果的一部分
- [ ] 把 `answerSegments` 从纯文本占位升级成真实结构化片段
- [ ] 明确 tool call 结果如何同步到前端 state
- [ ] 统一前端对 `messages / state / tool events` 的消费方式

**备注：**

- 这里的重点已经不是“协议事件怎么转”，而是“业务结果怎么附着到 AG-UI state/message 上”。

### Task 4: 前端接入 Vercel AG-UI

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/src/features/consultation/aguiClient.js`
- Create: `frontend/src/features/consultation/useAgUiConsultation.js`
- Modify: `frontend/src/features/consultation/ConsultationPage.jsx`
- Modify: `frontend/src/features/consultation/api.js`

**状态：主链路已完成，业务能力待补齐**

- [x] 已安装 `@ag-ui/client`
- [x] 已实现最小 SSE client adapter：`frontend/src/features/consultation/aguiClient.js`
- [x] 已实现 `useAgUiConsultation` hook
- [x] 已把公众咨询页切到 AG-UI hook
- [x] 前端构建已通过
- [ ] 未看到前端数据层测试
- [ ] `citations` 仍未从 AG-UI 结果中增量填充
- [ ] `answerSegments` 目前只有基础文本流，未与真实 citation 段落关联
- [ ] `frontend/src/features/consultation/api.js` 仍保留旧的 `createRagAnswer` 同步接口

**备注：**

- 这部分不该再被视为“接入中”，而应该视为“已接入，正在补完咨询产品状态模型”。

### Task 5: 验证与文档

**Files:**
- Modify: `docs/delivery/02-product-progress.md`
- Modify: `docs/delivery/01-implementation-plan.md`

**状态：需要重写为新阶段目标**

- [ ] 后端相关测试本轮未统一回归
- [x] 前端构建已通过
- [ ] `docs/delivery/02-product-progress.md` 尚未更新
- [ ] `docs/delivery/01-implementation-plan.md` 尚未更新
- [x] 当前缺口已明确：
  - 缺生产级 state store / 会话恢复
  - 缺 AG-UI 业务状态建模
  - 缺 citations 增量同步
  - 缺 tool call 真正可视化过程

**建议下一步：**

1. 先补 `Task 2`，把现有 law/rag 能力包装成正式 tools。
2. 再补 `Task 3`，统一 `messages + state + citations + answerSegments` 的消费模型。
3. 最后补会话持久化、历史对话和文档状态。
