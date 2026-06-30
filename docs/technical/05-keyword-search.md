# 关键词检索说明

## 1. 当前能力

MVP 已提供命令行关键词检索工具：

```text
scripts/search_law_articles.py
```

它基于 PostgreSQL 查询 `law_documents` 和 `law_articles`，用于验证法律条文数据是否可查。

当前支持：

- 条号精确匹配
- 法规名称匹配
- 章节路径匹配
- 条文正文关键词匹配
- `pg_trgm` 相似度排序
- 仅检索现行有效法规和条文

## 2. 使用方式

在项目根目录执行：

```bash
python3 scripts/search_law_articles.py 违约责任 --limit 5
```

条号检索：

```bash
python3 scripts/search_law_articles.py 第五百七十七条 --limit 3
```

章节检索：

```bash
python3 scripts/search_law_articles.py 借款合同 --limit 5
```

打印 SQL：

```bash
python3 scripts/search_law_articles.py 违约责任 --print-sql
```

## 3. 排序策略

当前排序使用一个简单 score：

```text
条号精确命中：+100
法规标题命中：+20
章节路径命中：+15
正文关键词命中：+10
正文 similarity：* 10
路径 similarity：* 5
```

然后按：

```text
score DESC, article_order ASC
```

排序。

## 4. 已验证查询

### 违约责任

命中：

- 《民法典》第五百七十七条
- 《民法典》第五百七十八条
- 《民法典》第五百九十三条

### 第五百七十七条

精确命中：

- 《民法典》第五百七十七条

### 借款合同

命中：

- 《民法典》第六百六十七条
- 《民法典》第六百六十八条
- 《民法典》第六百七十九条
- 《民法典》第六百八十条

## 5. 后续改进

关键词检索只是 RAG 的第一层。

后续会增加：

- embedding 向量检索
- keyword + vector hybrid search
- rerank
- 搜索结果 JSON API
- 检索测试页面
- AgentScope tool：`searchLawArticles`
