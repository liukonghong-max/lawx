# Spring Boot 后端说明

## 1. 定位

`backend/` 是 law4x 的正式后端主线，技术栈为：

- Spring Boot 3.x
- Java 17 target
- PostgreSQL + pgvector
- AgentScope Java v2.0.0-RC3

法律文件解析不属于 Spring Boot 核心后端职责，统一放到 Parser Skill / 离线导入工具中实现。

## 2. 当前能力

当前 Java 后端已实现：

- Spring Boot 项目骨架
- Spring Boot 应用入口
- DDD 分层骨架
- 法条关键词检索 use case
- 法条详情 use case
- RAG hybrid 检索 use case
- pgvector 法条向量检索 repository
- 缺失 embedding 生成 use case
- AgentScope DashScope embedding 适配
- Ark OpenAI-compatible answer 适配
- AgentScope Java v2.0.0-RC3 依赖守卫
- PostgreSQL 检索 repository
- REST 查询接口
- `/api/rag/search` 支持 keyword + vector hybrid 合并
- `/api/rag/test-runs` 支持保存 RAG 检索测试快照到 `rag_test_runs`
- `/api/rag/answer` 支持基于检索证据生成回答和 citations，默认抽取式回答，可切换 DashScope LLM

## 3. 运行测试

在 `backend/` 目录执行：

```bash
mvn test
```

当前测试：

```text
AgentScopeDependencyTest
- usesAgentScopeV2VersionForSimpleRagExtension

Law4xBackendApplicationTest
- exposesSpringBootApplicationEntryPoint

SearchLawArticlesUseCaseTest
- rejectsBlankQuery
- normalizesLimitAndDelegatesToRepository

GetLawArticleDetailUseCaseTest
- rejectsInvalidArticleId
- returnsArticleDetail

JdbcLawArticleRepositoryTest
- findsArticleByExactArticleNumber
- findsArticlesByChapterKeyword
- findsArticleDetailById

LawArticleSearchControllerTest
- searchesLawArticles
- returnsBadRequestForBlankQuery

LawArticleDetailControllerTest
- returnsArticleDetail
- returnsNotFoundWhenArticleDoesNotExist

HybridSearchUseCaseTest
- rejectsBlankQuery
- rejectsBlankEmbeddingModel
- searchesSimilarArticlesWithQueryEmbedding
- mergesKeywordAndVectorResultsByArticle

CreateRagTestRunUseCaseTest
- rejectsBlankQuery
- searchesAndPersistsRagTestRunSnapshot

CreateRagAnswerUseCaseTest
- rejectsBlankQuery
- createsAnswerWithCitationsFromRagEvidence

GenerateMissingArticleEmbeddingsUseCaseTest
- rejectsBlankEmbeddingModel
- generatesAndStoresEmbeddingsForMissingArticles

AgentScopeEmbeddingClientTest
- embedsTextThroughAgentScopeModel
- rejectsDifferentModelName

DashScopeEmbeddingConfigurationTest
- usesUnsupportedClientWhenDashScopeIsDisabled
- failsFastWhenDashScopeEnabledWithoutApiKey

AgentScopeRagAnswerClientTest
- generatesAnswerThroughAgentScopeModelWithGroundedPrompt

DashScopeAnswerConfigurationTest
- usesExtractiveAnswerClientWhenDashScopeAnswerIsDisabled
- createsAgentScopeAnswerClientWhenDashScopeAnswerIsEnabled
- failsFastWhenDashScopeAnswerIsEnabledWithoutApiKey

OpenAiCompatibleRagAnswerClientTest
- sendsOpenAiChatCompletionRequestToArkEndpoint

OpenAiCompatibleAnswerConfigurationTest
- usesExtractiveAnswerClientWhenOpenAiAnswerIsDisabled
- createsOpenAiCompatibleAnswerClientWhenOpenAiAnswerIsEnabled
- failsFastWhenOpenAiAnswerIsEnabledWithoutApiKey

RagSearchControllerTest
- searchesRagEvidence
- returnsBadRequestForBlankQuery

RagTestRunControllerTest
- createsRagTestRun

RagAnswerControllerTest
- createsRagAnswer

RagSearchControllerIntegrationTest
- returnsVectorHitsWhenDebtQuestionHasNoKeywordHit

JdbcLawArticleEmbeddingRepositoryTest
- searchesSimilarArticlesByPgvectorDistance
- findsMissingEmbeddingsAndUpsertsGeneratedEmbedding

JdbcRagTestRunRepositoryTest
- savesRagTestRunSnapshot
```

## 4. Parser Skill 和 Java 后端的关系

Parser Skill / 离线工具用途：

- 读取 docx、pdf、html、txt 等源文件。
- 抽取法规元数据。
- 按编、章、节、条切分。
- 输出标准结构化 JSON / SQL。
- 写入 `law_documents` 和 `law_articles`。

Java 后端用途：

- 正式产品后端。
- 法条检索服务。
- RAG 测试服务。
- AgentScope Java Agent 编排。
- AG-UI 协议输出。
- 会话、引用、反馈和评测。

职责边界：

```text
Parser Skill
 -> 结构化法规数据
 -> PostgreSQL
 -> Spring Boot 检索 / RAG / Agent
```

## 5. 后续计划

DashScope embedding 配置：

```yaml
law4x:
  embedding:
    dashscope:
      enabled: true
      api-key: ${DASHSCOPE_API_KEY}
      model-name: text-embedding-v4
      dimensions: 1536
```

Ark OpenAI-compatible answer 配置：

```yaml
law4x:
  answer:
    openai:
      enabled: true
      api-key: ${ARK_API_KEY}
      base-url: https://ark.cn-beijing.volces.com/api/coding/v3
      model-name: ark-code-latest
```

AgentScope Java 依赖：

```xml
<agentscope.version>2.0.0-RC3</agentscope.version>
```

当前后端使用 `agentscope-extensions-rag-simple`，版本统一跟随 `${agentscope.version}`，避免重新退回 1.x。

未开启 `ARK_ANSWER_ENABLED` 时，`/api/rag/answer` 使用本地抽取式 fallback，不调用外部模型。开启后，`OpenAiCompatibleRagAnswerClient` 会按 OpenAI chat completions 规范调用 Ark endpoint，并将问题和检索证据组织成 grounded prompt，要求模型只基于给定法条回答、无法判断时说明不确定、并避免承诺诉讼结果。

本地小批量入库：

```bash
export DASHSCOPE_EMBEDDING_ENABLED=true
export DASHSCOPE_API_KEY=你的DashScopeKey
curl --location --request POST 'localhost:8080/api/admin/rag/embeddings/generate?limit=20'
```

该接口会查找当前模型缺失 embedding 的法条，调用 `GenerateMissingArticleEmbeddingsUseCase` 写入 `law_article_embeddings`。先用较小 `limit` 试跑，确认费用和数据写入正常后再放大。

本地全量分批入库：

```bash
curl --location --request POST 'localhost:8080/api/admin/rag/embeddings/generate-all?batchSize=100&maxBatches=50'
```

`generate-all` 会循环执行单批生成，直到没有缺失 embedding 的法条，或达到 `maxBatches`。返回里的 `finished=true` 表示本轮已确认没有缺失数据；`finished=false` 表示达到批次数上限，可以继续调用。

本地 RAG hybrid 检索：

```bash
curl 'localhost:8080/api/rag/search?query=别人欠钱不还怎么办&limit=5'
```

`/api/rag/search` 会对 query 生成 embedding，执行 pgvector 向量召回，并与关键词检索结果按 `articleId` 合并。同一条法条同时命中 keyword 和 vector 时返回 `matchType=hybrid`；自然语言问题即使没有关键词命中，也可以返回 `matchType=vector` 的相似法条。

保存 RAG 检索测试快照：

```bash
curl --location --request POST 'localhost:8080/api/rag/test-runs' \
  --header 'Content-Type: application/json' \
  --data '{"query":"别人欠钱不还怎么办","limit":5}'
```

该接口会复用 hybrid 检索，保存 query、keyword/vector 结果、选中法条 ID 和 embedding 参数，返回 `runId` 与本次命中列表。后续人工评价、rerank 结果和 answer 评测可继续挂在同一条 `rag_test_runs` 记录上。

基于 RAG 证据生成回答：

```bash
curl --location --request POST 'localhost:8080/api/rag/answer' \
  --header 'Content-Type: application/json' \
  --data '{"query":"别人欠钱不还怎么办","limit":5}'
```

该接口会先创建一条 RAG test run，再基于命中的法条证据返回 `answer` 和 `citations`。默认实现是抽取式回答，不依赖外部 LLM；配置 `ARK_ANSWER_ENABLED=true` 和 `ARK_API_KEY` 后使用 Ark OpenAI-compatible chat completions 生成自然语言回答。

下一步实现：

- RAG 测试人工评价字段和更新接口
- AgentScope Java v2 RAG tool
- AG-UI `/ag-ui` 接口
