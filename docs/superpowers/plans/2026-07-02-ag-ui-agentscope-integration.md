# AG-UI AgentScope Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 law4x 增加基于 AgentScope Java `HarnessAgent` 的 `/ag-ui` 流式接口，并让 React 前端切换到 Vercel AG-UI 驱动的公众咨询链路。

**Architecture:** 后端新增 `HarnessAgent` 与 `/ag-ui/runs` SSE 控制器，复用现有 law/rag use case 作为 AgentScope tools，并通过事件桥接层把 AgentScope `streamEvents()` 映射成前端 AG-UI 可消费事件。前端保留现有页面结构，替换数据层为 AG-UI client adapter，实现回答流式更新、引用依据增量展示和工具过程状态展示。

**Tech Stack:** Spring Boot 3.3, AgentScope Java 2.0.0-RC3, SSE, React 18, Vite, Vercel AG-UI

---

### Task 1: 后端 `/ag-ui` 最小闭环

**Files:**
- Create: `backend/src/main/java/com/law4x/agui/interfaces/rest/AgUiRunController.java`
- Create: `backend/src/main/java/com/law4x/agui/application/AgUiRunStreamService.java`
- Create: `backend/src/main/java/com/law4x/agui/application/AgUiEventMapper.java`
- Create: `backend/src/test/java/com/law4x/agui/interfaces/rest/AgUiRunControllerTest.java`
- Modify: `backend/pom.xml`

- [ ] 先为 `/ag-ui/runs` 写控制器测试，断言返回 `text/event-stream` 且输出 `run.started`、`message.delta`、`run.completed`
- [ ] 跑测试，确认因控制器/服务不存在而失败
- [ ] 实现最小控制器与伪流服务，让测试变绿
- [ ] 运行该测试确认通过

### Task 2: AgentScope `HarnessAgent` 与工具装配

**Files:**
- Create: `backend/src/main/java/com/law4x/agui/infrastructure/agent/AgUiAgentConfiguration.java`
- Create: `backend/src/main/java/com/law4x/agui/infrastructure/agent/Law4xAgUiToolset.java`
- Create: `backend/src/main/java/com/law4x/agui/application/CitationValidationService.java`
- Create: `backend/src/test/java/com/law4x/agui/infrastructure/agent/AgUiAgentConfigurationTest.java`
- Modify: `backend/src/main/resources/application.yml`

- [ ] 为 `HarnessAgent` 配置写测试，断言 Bean 可创建且依赖齐全
- [ ] 跑测试确认失败
- [ ] 实现 `HarnessAgent.builder()`、`RuntimeContext` 约定和 3 个 tools：`searchLawArticles`、`getArticleDetail`、`validateCitations`
- [ ] 跑测试确认通过

### Task 3: AgentScope 事件桥接到 AG-UI

**Files:**
- Modify: `backend/src/main/java/com/law4x/agui/application/AgUiRunStreamService.java`
- Modify: `backend/src/main/java/com/law4x/agui/application/AgUiEventMapper.java`
- Create: `backend/src/test/java/com/law4x/agui/application/AgUiEventMapperTest.java`

- [ ] 先写事件映射测试，覆盖 tool started/completed、文本增量、运行完成
- [ ] 跑测试确认失败
- [ ] 实现对 `streamEvents()` 的消费和 AG-UI 事件映射
- [ ] 跑测试确认通过

### Task 4: 前端接入 Vercel AG-UI

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/src/features/consultation/aguiClient.js`
- Create: `frontend/src/features/consultation/useAgUiConsultation.js`
- Modify: `frontend/src/features/consultation/ConsultationPage.jsx`
- Modify: `frontend/src/features/consultation/api.js`

- [ ] 为前端数据层写最小测试或在无法直接测试时先写可独立验证的 adapter 结构
- [ ] 安装 Vercel AG-UI 依赖并实现 SSE client adapter
- [ ] 把公众咨询页从同步 `createRagAnswer` 切到 AG-UI 流式 hook
- [ ] 本地构建前端，确认能通过编译

### Task 5: 验证与文档

**Files:**
- Modify: `docs/delivery/02-product-progress.md`
- Modify: `docs/delivery/01-implementation-plan.md`

- [ ] 运行后端相关测试
- [ ] 运行前端构建
- [ ] 根据实际完成情况更新进度文档
- [ ] 记录剩余缺口，例如生产级 state store、恢复、AG-UI 细粒度协议兼容项
