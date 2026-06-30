# Spring Boot 后端说明

## 1. 定位

`backend/` 是 law4x 的正式后端主线，技术栈为：

- Spring Boot 3.x
- Java 17 target
- PostgreSQL + pgvector
- 后续接入 AgentScope Java v2

法律文件解析不属于 Spring Boot 核心后端职责，统一放到 Parser Skill / 离线导入工具中实现。

## 2. 当前能力

当前 Java 后端已实现：

- Spring Boot 项目骨架
- Spring Boot 应用入口
- DDD 分层骨架
- 法条关键词检索 use case
- 法条详情 use case
- PostgreSQL 检索 repository
- REST 查询接口

## 3. 运行测试

在 `backend/` 目录执行：

```bash
mvn test
```

当前测试：

```text
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

下一步实现：

- `HybridSearchUseCase`
- `RagTestService`

再下一步：

- embedding 生成
- pgvector 查询
- AgentScope Java v2 RAG tool
- AG-UI `/ag-ui` 接口
