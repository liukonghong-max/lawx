# AG-UI AgentScope Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 law4x 增加基于 AgentScope Java `HarnessAgent` 的 `/ag-ui` 流式接口，并让 React 前端切换到 Vercel AG-UI 驱动的公众咨询链路。

**Architecture:** 后端新增 `HarnessAgent` 与 `/ag-ui/runs` SSE 控制器，复用现有 law/rag use case 作为 AgentScope tools，并通过事件桥接层把 AgentScope `streamEvents()` 映射成前端 AG-UI 可消费事件。前端保留现有页面结构，替换数据层为 AG-UI client adapter，实现回答流式更新、引用依据增量展示和工具过程状态展示。

**Tech Stack:** Spring Boot 3.3, AgentScope Java 2.0.0-RC3, SSE, React 18, Vite, Vercel AG-UI

---

## 当前执行状态（2026-07-02）

- 总体进度：约 `75%`
- 当前判断：`前后端对话流式、AG-UI 协议、HarnessAgent、grounding 注入、正式 tools、服务端预检索与 hybrid grounding 已经打通；当前阶段已经从“Task 2: tools 装配”进入“Task 3: AG-UI 业务状态补齐”`
- 已落地文件：
  - `backend/src/main/java/com/law4x/agui/interfaces/rest/AgUiRunController.java`
  - `backend/src/main/java/com/law4x/agui/infrastructure/agent/config/AgUiAgentConfiguration.java`
  - `backend/src/main/java/com/law4x/agui/infrastructure/agent/config/AgUiProtocolConfiguration.java`
  - `backend/src/main/java/com/law4x/agui/infrastructure/agent/middleware/AgUiGroundingPromptMiddleware.java`
  - `backend/src/main/java/com/law4x/agui/infrastructure/agent/runtime/AgUiRuntimeContextHolder.java`
  - `backend/src/main/java/com/law4x/agui/infrastructure/agent/runtime/GroundedHarnessAgent.java`
  - `backend/src/main/java/com/law4x/agui/infrastructure/agent/tool/Law4xAgUiToolset.java`
  - `backend/src/main/java/com/law4x/agui/infrastructure/agent/tool/Law4xConsultationToolset.java`
  - `backend/src/main/java/com/law4x/agui/application/service/AgUiConsultationGroundingService.java`
  - `backend/src/main/java/com/law4x/agui/application/service/AgUiConversationStateService.java`
  - `backend/src/main/java/com/law4x/agui/application/service/CitationValidationService.java`
  - `backend/src/test/java/com/law4x/agui/interfaces/rest/AgUiRunControllerTest.java`
  - `backend/src/test/java/com/law4x/agui/infrastructure/agent/config/AgUiAgentConfigurationTest.java`
  - `backend/src/test/java/com/law4x/agui/infrastructure/agent/config/AgUiProtocolConfigurationTest.java`
  - `backend/src/test/java/com/law4x/agui/application/service/AgUiConsultationGroundingServiceTest.java`
  - `backend/src/test/java/com/law4x/agui/application/service/AgUiConversationStateServiceTest.java`
  - `backend/src/test/java/com/law4x/agui/infrastructure/agent/tool/Law4xAgUiToolsetTest.java`
  - `frontend/src/features/consultation/aguiClient.js`
  - `frontend/src/features/consultation/useAgUiConsultation.js`
  - `frontend/src/features/consultation/ConsultationPage.jsx`
  - `frontend/package.json`
- 主要缺口：
  - 当前虽然已有 `citations / answerSegments / state` 基础结构，但前端还没有完全按 AG-UI 原生状态模型消费
  - tool call / tool result 的前端标准可视化仍未补齐
  - 会话持久化、历史对话切换、恢复能力还没接上
  - 文档状态还停留在较早阶段，未反映当前真实进度

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

**状态：已基本完成**

- [x] 已有 `HarnessAgent` Bean 配置测试：`AgUiAgentConfigurationTest`
- [x] 已有 `HarnessAgent.builder()` 最小实现：`AgUiAgentConfiguration`
- [x] `RuntimeContext` 约定已落地：`AgUiRuntimeContextHolder + GroundedHarnessAgent + AgUiGroundingPromptMiddleware`
- [x] `getArticleDetail` 已落地为正式 tool
- [x] `validateCitations` 已落地为正式 tool
- [x] `Law4xAgUiToolset` 已创建
- [x] `CitationValidationService` 已创建
- [x] 本轮已重新执行 AG-UI 相关后端测试确认
- [ ] `searchLawArticles` 不再作为咨询 agent 必调 tool，而是由服务端预检索承担

**备注：**

- 当前设计已从“是否由模型自行决定检索”切换为“服务端先 grounding，再由 agent 基于证据回答并校验引用”。
- `searchLawArticles` 仍保留在 `Law4xAgUiToolset` 中作为可复用能力，但不再暴露给咨询 agent 的 toolkit。

### Task 3: AG-UI 业务状态补齐

**Files:**
- Modify: `frontend/src/features/consultation/useAgUiConsultation.js`
- Modify: `frontend/src/features/consultation/ConsultationPage.jsx`
- Modify: `backend/src/main/java/com/law4x/agui/interfaces/rest/AgUiRunController.java` 或新增业务状态组装层
- Optional: Create `backend/src/main/java/com/law4x/agui/application/AgUiConversationStateService.java`

**状态：进行中**

- [x] 已有 `AgUiConversationStateService` 生成 `citations / answerSegments / answer`
- [x] 已有 `AgUiConsultationGroundingService` 在服务端预检索并写入 `allowedCitationIds / citations`
- [ ] 把 `citations` 变成前端稳定消费的 AG-UI 运行结果，而不是仅依赖最终 state 兜底
- [ ] 把 `answerSegments` 从后端基础结构升级成前端可直接消费的真实引用分段
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
- [ ] `citations` 仍未以 ai-elements / AG-UI 原生方式稳定增量填充
- [ ] `answerSegments` 尚未与真实 citation 段落和可点击依据联动
- [ ] `frontend/src/features/consultation/api.js` 仍保留旧的 `createRagAnswer` 同步接口

**备注：**

- 这部分不该再被视为“接入中”，而应该视为“已接入，正在补完咨询产品状态模型”。

### Task 5: 验证与文档

**Files:**
- Modify: `docs/delivery/02-product-progress.md`
- Modify: `docs/delivery/01-implementation-plan.md`

**状态：需要按当前真实进度重写**

- [x] 后端 AG-UI 关键测试本轮已回归：
  - `AgUiConsultationGroundingServiceTest`
  - `AgUiConversationStateServiceTest`
  - `AgUiRunControllerTest`
  - `AgUiAgentConfigurationTest`
  - `AgUiProtocolConfigurationTest`
- [x] 前端构建已通过
- [ ] `docs/delivery/02-product-progress.md` 需要同步到当前状态
- [ ] `docs/delivery/01-implementation-plan.md` 如继续作为总计划，也需要同步
- [x] 当前缺口已明确：
  - 缺生产级 state store / 会话恢复
  - 缺 AG-UI 业务状态建模
  - 缺 citations 增量同步
  - 缺 tool call 真正可视化过程

## 下一阶段开发计划（P2 下半程）

### Phase 3A: AG-UI 状态模型收口

- [ ] 明确后端单一输出模型：`answer / citations / answerSegments / allowedCitationIds / toolState`
- [ ] 统一 `AgUiRunController` 在流式结束和中间态的 state 写入策略
- [ ] 统一前端 `useAgUiConsultation` 对 `messages / state / tool events` 的消费入口

### Phase 3B: citations 与 answerSegments 前端闭环

- [ ] 把 `citations` 改成前端直接可渲染的依据数据源
- [ ] 让 `answerSegments` 与 citationIds 建立稳定映射
- [ ] 在会话区支持点击段落高亮对应依据

### Phase 3C: tool 可视化

- [ ] 把 `getArticleDetail`、`validateCitations` 的调用过程同步到前端
- [ ] 使用 ai-elements 标准 tool/reasoning 能力展示调用状态和结果
- [ ] 明确哪些 tool 结果进入 message，哪些进入 state

### Phase 3D: 会话持久化与恢复

- [ ] 定义会话存储结构：线程、消息、state snapshot
- [ ] 支持历史会话列表、切换、恢复继续对话
- [ ] 明确刷新页面后的恢复策略

### 建议执行顺序

1. 先做 `Phase 3A`
2. 再做 `Phase 3B`
3. 然后做 `Phase 3C`
4. 最后做 `Phase 3D`
