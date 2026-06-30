# 数据库 Schema 设计

## 1. 设计目标

数据库需要同时支撑三类能力：

- 法律法规条文级存储。
- RAG 检索和引用追溯。
- Agent 会话、引用、评测和反馈记录。

MVP 使用 PostgreSQL 16 + pgvector。

## 2. 扩展插件

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

说明：

- `vector`：用于存储和检索 embedding。
- `pg_trgm`：用于中文关键词模糊检索。
- `pgcrypto`：用于 `gen_random_uuid()`。

## 3. 核心表

### `knowledge_import_jobs`

记录法规文件导入任务。

核心用途：

- 追踪上传、解析、确认、发布状态。
- 记录解析出的条文数量和异常信息。
- 支持后续重新解析。

状态建议：

```text
uploaded
parsing
parsed
published
failed
```

### `law_documents`

记录一部法律法规的元数据。

一条记录对应一部法律，例如：

```text
中华人民共和国民法典
```

关键字段：

- `title`：法规名称。
- `law_type`：法律、行政法规、部门规章等。
- `issuer`：发布机关。
- `publish_date`：发布日期。
- `effective_date`：生效日期。
- `status`：现行有效、已修改、已废止等。
- `source_url`：来源链接。
- `source_file_name`：本地导入文件名。

### `law_articles`

记录条文级数据。

一条记录对应一条法条，例如：

```text
《民法典》第五百七十七条
```

关键字段：

- `document_id`：所属法规。
- `book_title`：编。
- `chapter_title`：章。
- `section_title`：节。
- `article_no`：条号。
- `article_order`：条文顺序。
- `content`：条文正文。
- `full_path`：完整章节路径。
- `effective_status`：条文生效状态。

### `law_article_embeddings`

记录条文 embedding。

MVP 默认使用 `vector(1536)`。如果后续 embedding 模型维度不是 1536，需要同步调整 migration。

关键字段：

- `article_id`：对应法条。
- `embedding_model`：embedding 模型名。
- `content_hash`：正文 hash，用于判断是否需要重新生成 embedding。
- `embedding`：向量。

### `chat_sessions`

记录一次用户会话。

### `chat_messages`

记录用户消息、Agent 消息和系统消息。

### `citations`

记录 Agent 回答引用了哪些法条。

每条 citation 必须能反查到 `law_articles.id`，这是引用可信机制的核心。

### `rag_test_runs`

记录检索测试页的一次测试。

用于观察：

- 用户问题。
- keyword search 命中。
- vector search 命中。
- rerank 后结果。
- 最终采用依据。

### `eval_cases` / `eval_runs` / `eval_run_items`

用于后续评测集。

MVP 可以先建表，不一定第一阶段做完整页面。

## 4. 检索索引

### 精确检索

用于法规名称、条号、状态过滤：

```sql
CREATE INDEX idx_law_documents_title ON law_documents(title);
CREATE INDEX idx_law_articles_article_no ON law_articles(article_no);
CREATE INDEX idx_law_articles_effective_status ON law_articles(effective_status);
```

### 中文关键词检索

MVP 使用 trigram：

```sql
CREATE INDEX idx_law_articles_content_trgm
ON law_articles USING gin (content gin_trgm_ops);
```

后续如果需要更强中文分词，可以接入 Elasticsearch、OpenSearch 或 PostgreSQL 中文分词插件。

### 向量检索

使用 ivfflat：

```sql
CREATE INDEX idx_law_article_embeddings_embedding
ON law_article_embeddings
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

注意：ivfflat 在数据量较小时优势不明显，且通常建议在导入一定数据后再建索引。MVP 数据量小也可以先建，后续根据数据规模调整。

## 5. 推荐查询链路

```text
用户问题
 -> 精确识别法律名称和条号
 -> keyword search
 -> vector search
 -> 合并候选条文
 -> rerank
 -> 返回 topK
 -> Agent 基于候选条文生成回答
 -> citations 记录引用
```

## 6. 初始 Migration

可执行 SQL 位于：

```text
db/migrations/001_initial_schema.sql
```

后续 Spring Boot 项目可以用 Flyway 或 Liquibase 管理这些 migration。
