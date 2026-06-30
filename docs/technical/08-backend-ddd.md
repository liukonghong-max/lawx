# 后端 DDD 建模

## 1. 目标

Spring Boot 后端采用 DDD 分层，核心原则是：

- 业务规则放在 `domain` 和 `application`。
- 数据库、HTTP、Agent 协议放在外层适配器。
- Parser Skill 只负责离线解析和入库，不进入后端核心模型。

## 2. 当前限界上下文

### `law`

负责法规库、法条检索、条文详情、后续 RAG grounding。

当前包结构：

```text
com.law4x.law
├── domain
│   ├── model
│   │   └── LawArticleSearchResult
│   └── repository
│       └── LawArticleRepository
├── application
│   └── SearchLawArticlesUseCase
├── infrastructure
│   └── persistence
│       └── JdbcLawArticleRepository
└── interfaces
    └── rest
        └── LawArticleSearchController
```

## 3. 分层职责

### `domain`

表达核心业务概念和端口。

当前包含：

- `LawArticleSearchResult`：法条检索结果。
- `LawArticleRepository`：法规库检索端口。

后续可扩展：

- `LawDocument`
- `LawArticle`
- `Citation`
- `EffectiveStatus`
- `LegalSource`

### `application`

编排单个业务用例，不直接写 SQL，不依赖 HTTP。

当前包含：

- `SearchLawArticlesUseCase`

职责：

- 校验 query。
- 归一化 limit。
- 调用 `LawArticleRepository`。

后续可扩展：

- `GetLawArticleDetailUseCase`
- `HybridSearchUseCase`
- `ValidateCitationsUseCase`
- `RunRagTestUseCase`

### `infrastructure`

实现外部资源适配。

当前包含：

- `JdbcLawArticleRepository`

职责：

- 使用 `JdbcClient` 查询 PostgreSQL。
- 实现条号、标题、章节路径、正文、`pg_trgm` 相似度综合排序。

后续可扩展：

- `PgVectorLawArticleRepository`
- `OpenAiEmbeddingClient`
- `AgentScopeToolAdapter`

### `interfaces`

负责协议输入输出。

当前包含：

- `LawArticleSearchController`

职责：

- 提供 `GET /api/law/articles/search`。
- 将 HTTP 参数交给 use case。
- 将领域结果转换为 API response。

后续可扩展：

- `AguiController`
- `RagTestController`
- `CitationController`

## 4. 当前 API

```http
GET /api/law/articles/search?query=第五百七十七条&limit=3
```

返回：

```json
{
  "items": [
    {
      "documentTitle": "中华人民共和国民法典",
      "articleNo": "第五百七十七条",
      "fullPath": "中华人民共和国民法典 > 第三编 合同 > 第八章 违约责任 > 第五百七十七条",
      "preview": "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。",
      "score": 116.21
    }
  ]
}
```

## 5. 后续建模顺序

建议按这个顺序推进：

1. 法条详情：`GetLawArticleDetailUseCase`
2. RAG 检索：`HybridSearchUseCase`
3. 引用校验：`ValidateCitationsUseCase`
4. Agent tool 适配：`SearchLawArticlesTool`
5. AG-UI 流式接口：`AguiController`

这样可以先把法规库能力做扎实，再把 Agent 接进来。
