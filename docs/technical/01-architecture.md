# 技术架构设计

## 1. 总体架构

```text
React 前端
 -> AG-UI Protocol / SSE
 -> Spring Boot 3.x
 -> AgentScope Java v2
 -> Postgres + pgvector
```

## 2. 前端

### 技术选型

- React
- AG-UI 事件流消费
- 三栏工作台 UI

### 核心模块

- `ModeSwitcher`：公众咨询 / 专业检索。
- `ChatPanel`：展示用户问题、Agent 回答、流式输出。
- `ToolActivity`：展示检索、引用校验、回答生成等中间状态。
- `CitationPanel`：展示引用法条、来源链接、生效状态、待确认事实和风险提示。
- `Composer`：输入问题、上传文件、粘贴合同或文书片段。
- `KnowledgeAdmin`：数据源管理、解析预览、检索测试。

## 3. 后端

### 技术选型

- Spring Boot 3.x
- AgentScope Java v2
- PostgreSQL 16
- pgvector

### 模块划分

```text
controller
- AguiController
- DataSourceController
- LawDocumentController
- RagTestController

agent
- LegalConsultationAgent
- ProfessionalSearchAgent
- AgentConfig

tools
- SearchLawArticlesTool
- GetArticleDetailTool
- ValidateCitationsTool
- CheckEffectiveStatusTool

rag
- HybridSearchService
- EmbeddingService
- RerankService

law
- LawDocumentService
- LawArticleService
- LawArticleSearchService

eval
- EvalCaseService
- EvalRunService
```

## 4. 数据库核心表

### `law_documents`

存储一部法律法规的元数据。

字段建议：

- `id`
- `title`
- `type`
- `issuer`
- `document_no`
- `publish_date`
- `effective_date`
- `status`
- `source_url`
- `source_file_name`
- `checksum`
- `created_at`
- `updated_at`

### `law_articles`

存储条文级数据。

字段建议：

- `id`
- `document_id`
- `book_title`
- `chapter_title`
- `section_title`
- `article_no`
- `article_order`
- `content`
- `full_path`
- `effective_status`
- `source_anchor`
- `created_at`
- `updated_at`

### `knowledge_import_jobs`

记录法规导入和解析任务。

字段建议：

- `id`
- `file_name`
- `source_url`
- `status`
- `total_articles`
- `error_message`
- `created_at`
- `updated_at`

### `chat_sessions`

记录会话。

### `chat_messages`

记录用户消息和 Agent 消息。

### `citations`

记录每条回答引用了哪些法条。

字段建议：

- `id`
- `message_id`
- `article_id`
- `document_title`
- `article_no`
- `quoted_text`
- `source_url`
- `confidence`

## 5. RAG 流程

```text
Parser Skill / 离线导入工具
 -> law_documents / law_articles
 -> embedding
 -> pgvector
 -> hybrid search
 -> rerank
 -> citation grounding
 -> Agent answer
```

## 6. 检索策略

MVP 使用 hybrid search：

```text
keyword score
+ vector score
+ law title boost
+ article number boost
+ status boost
= final score
```

优先返回：

- 现行有效法规
- 标题命中法规
- 条号命中条文
- 正文语义相关条文
- 来源可信条文

## 7. Agent Tools

### `searchLawArticles`

输入用户问题或检索词，返回候选法条。

### `getArticleDetail`

根据 `articleId` 返回完整法条、章节路径、来源链接和生效状态。

### `validateCitations`

检查回答中的引用是否来自检索结果，避免编造引用。

### `checkEffectiveStatus`

检查法规是否现行有效。

### `runRagTest`

用于检索测试页，返回关键词、向量和 rerank 结果。

## 8. Parser Skill 边界

法律文件解析由独立 Parser Skill / 离线导入工具完成，不放在 Spring Boot 核心后端中。

Parser Skill 负责：

- 读取 docx、pdf、html、txt。
- 识别法规元数据。
- 按编、章、节、条切分。
- 生成标准结构化 JSON / SQL。
- 写入 `law_documents` 和 `law_articles`。

Spring Boot 负责消费结构化后的法规数据，提供检索、RAG、Agent 和 AG-UI 能力。

## 9. AG-UI 集成

后端提供：

```text
POST /ag-ui
```

响应类型：

```text
text/event-stream
```

前端根据 AG-UI 事件展示：

- 流式文本
- tool call 开始
- tool call 参数
- tool call 结果
- 引用依据
- 错误状态

## 10. 引用可信机制

- Agent 回答前必须先检索知识库。
- 回答中的法律依据必须来自检索结果。
- `validateCitations` 校验通过后才能输出最终回答。
- 无依据时输出不确定提示。
- 右侧引用面板展示原文、来源、生效状态。
