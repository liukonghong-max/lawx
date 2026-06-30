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
│   │   ├── LawArticleDetail
│   │   └── LawArticleSearchResult
│   └── repository
│       └── LawArticleRepository
├── application
│   ├── GetLawArticleDetailUseCase
│   └── SearchLawArticlesUseCase
├── infrastructure
│   └── persistence
│       └── JdbcLawArticleRepository
└── interfaces
    └── rest
        ├── LawArticleDetailController
        └── LawArticleSearchController
```

### `rag`

负责面向 Agent 和检索测试页的证据召回。

当前包结构：

```text
com.law4x.rag
├── domain
│   └── model
│       └── RagSearchResult
├── application
│   └── HybridSearchUseCase
└── interfaces
    └── rest
        └── RagSearchController
```

当前 `HybridSearchUseCase` 先复用 `LawArticleRepository` 的关键词检索能力，并将结果包装成 RAG 检索结果格式。若自然语言债务问题原句无结果，会临时扩展到 `借款合同`、`违约责任`、`诉讼时效` 等检索词。这样前端、AgentScope tool 和检索测试页可以先对齐接口，后续再替换为真正的 keyword + vector + rerank。

## 3. 分层职责

### `domain`

表达核心业务概念和端口。

当前包含：

- `LawArticleSearchResult`：法条检索结果。
- `LawArticleDetail`：完整法条、法规元数据和来源信息。
- `LawArticleRepository`：法规库检索端口。
- `RagSearchResult`：面向 RAG 的证据召回结果，包含命中方式、关键词分数、向量分数和最终分数。

后续可扩展：

- `LawDocument`
- `LawArticle`
- `Citation`
- `EffectiveStatus`
- `LegalSource`

### `application`

编排单个业务用例，不直接写 SQL，不依赖 HTTP。

当前包含：

- `GetLawArticleDetailUseCase`
- `SearchLawArticlesUseCase`
- `HybridSearchUseCase`

职责：

- 校验 query。
- 归一化 limit。
- 调用 `LawArticleRepository`。
- 校验 articleId。
- 按 articleId 获取完整法条详情。
- 将关键词检索结果包装为 RAG 检索结果。
- 限制 RAG 检索默认返回数量和最大返回数量。

后续可扩展：

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

- `LawArticleDetailController`
- `LawArticleSearchController`
- `RagSearchController`

职责：

- 提供 `GET /api/law/articles/search`。
- 提供 `GET /api/law/articles/{articleId}`。
- 提供 `GET /api/rag/search`。
- 将 HTTP 参数交给 use case。
- 将领域结果转换为 API response。

后续可扩展：

- `AguiController`
- `RagTestController`
- `CitationController`

## 4. 当前 API

REST API 统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

错误响应同样使用 `code/message/data`，其中 `data` 为 `null`。当前约定：

- `200`：成功。
- `-1`：失败。后续细分失败原因时，通过异常枚举扩展。

### 法条检索

```http
GET /api/law/articles/search?query=第五百七十七条&limit=3
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "articleId": "00000000-0000-0000-0000-000000000000",
        "documentTitle": "中华人民共和国民法典",
        "articleNo": "第五百七十七条",
        "fullPath": "中华人民共和国民法典 > 第三编 合同 > 第八章 违约责任 > 第五百七十七条",
        "preview": "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。",
        "score": 116.21
      }
    ]
  }
}
```

### 法条详情

```http
GET /api/law/articles/{articleId}
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "articleId": "00000000-0000-0000-0000-000000000000",
    "documentTitle": "中华人民共和国民法典",
    "lawType": "法律",
    "issuer": "全国人民代表大会",
    "publishDate": "2020-05-28",
    "effectiveDate": "2021-01-01",
    "documentStatus": "effective",
    "sourceUrl": null,
    "bookTitle": "第三编 合同",
    "chapterTitle": "第八章 违约责任",
    "sectionTitle": null,
    "articleNo": "第五百七十七条",
    "articleOrder": 577,
    "content": "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担继续履行、采取补救措施或者赔偿损失等违约责任。",
    "fullPath": "中华人民共和国民法典 > 第三编 合同 > 第八章 违约责任 > 第五百七十七条",
    "effectiveStatus": "effective"
  }
}
```

### RAG 检索

```http
GET /api/rag/search?query=别人欠钱不还怎么办&limit=5
```

当前为 RAG 骨架版本：先返回关键词召回结果，`vectorScore` 为 `0`。如果原句无结果且命中内置法律意图词，`matchType` 会返回 `keyword_expansion`。后续接入 embedding 和 pgvector 后，`matchType` 会扩展为 `vector` 或 `hybrid`。

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "articleId": "00000000-0000-0000-0000-000000000000",
        "documentTitle": "中华人民共和国民法典",
        "articleNo": "第六百七十五条",
        "fullPath": "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条",
        "preview": "借款人应当按照约定的期限返还借款。",
        "matchType": "keyword_expansion",
        "keywordScore": 42.50,
        "vectorScore": 0,
        "finalScore": 42.50,
        "reason": "当前通过法律意图词扩展命中，后续会叠加向量召回和 rerank。"
      }
    ]
  }
}
```

## 5. 后续建模顺序

建议按这个顺序推进：

1. 向量底座：embedding 字段或 `article_embeddings` 表
2. 引用校验：`ValidateCitationsUseCase`
3. Agent tool 适配：`SearchLawArticlesTool`
4. AG-UI 流式接口：`AguiController`

这样可以先把法规库能力做扎实，再把 Agent 接进来。
