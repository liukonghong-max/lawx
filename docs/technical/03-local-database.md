# 本地数据库启动说明

## 1. 前置条件

本地需要安装并启动 Docker。

项目使用：

- PostgreSQL 16
- pgvector
- pg_trgm
- pgcrypto

## 2. 启动数据库

在项目根目录执行：

```bash
docker compose up -d
```

容器信息：

```text
container: law4x-postgres
database: law4x
user: law4x
password: law4x_dev
port: 5432
```

连接字符串：

```text
postgresql://law4x:law4x_dev@localhost:5432/law4x
```

## 3. 初始化 Schema

首次启动时，Docker 会自动执行：

```text
db/migrations/001_initial_schema.sql
```

该 SQL 会创建：

- `knowledge_import_jobs`
- `law_documents`
- `law_articles`
- `law_article_embeddings`
- `chat_sessions`
- `chat_messages`
- `citations`
- `rag_test_runs`
- `eval_cases`
- `eval_runs`
- `eval_run_items`
- `feedback`

并启用：

- `vector`
- `pg_trgm`
- `pgcrypto`

## 4. 验证数据库

进入容器：

```bash
docker exec -it law4x-postgres psql -U law4x -d law4x
```

查看扩展：

```sql
\dx
```

查看表：

```sql
\dt
```

验证核心表：

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

## 5. 重建数据库

如果需要清空本地数据并重新初始化：

```bash
docker compose down -v
docker compose up -d
```

注意：`down -v` 会删除本地数据库数据卷。
