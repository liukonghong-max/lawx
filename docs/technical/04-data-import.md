# 法规数据导入说明

## 1. 当前能力

MVP 提供一个 Python 导入工具，用于把 docx 法规文件解析成结构化条文并写入本地 PostgreSQL。

当前已验证样本：

```text
中华人民共和国民法典_20200528.docx
```

解析结果：

```text
法规名称：中华人民共和国民法典
发布日期：2020-05-28
生效日期：2021-01-01
条文数：1260
```

## 2. 解析规则

解析器会识别：

- 法规标题
- 发布日期
- 编
- 章
- 节
- 条
- 条文续段

条文续段会合并到上一条法条。例如《民法典》第一千二百五十四条包含多个自然段，导入后会合并为同一条 `law_articles.content`。

## 3. 运行测试

在项目根目录执行：

```bash
python3 -m unittest discover -s tests
```

当前测试覆盖：

- 民法典元数据解析
- 民法典 1260 条解析
- 第一条层级路径
- 第五百七十七条违约责任
- 多段条文合并
- 导入 SQL 生成

## 4. 导入民法典

确保数据库已经启动：

```bash
docker compose up -d
```

执行导入：

```bash
python3 scripts/import_law_docx.py 中华人民共和国民法典_20200528.docx --effective-date 2021-01-01
```

导入成功后会看到：

```text
Imported 中华人民共和国民法典: 1260 articles
```

## 5. 验证入库结果

查看法规文档：

```bash
docker exec law4x-postgres psql -U law4x -d law4x \
  -c "SELECT title, law_type, publish_date, effective_date, status FROM law_documents;"
```

查看条文数量：

```bash
docker exec law4x-postgres psql -U law4x -d law4x \
  -c "SELECT count(*) AS article_count FROM law_articles;"
```

查询指定条文：

```bash
docker exec law4x-postgres psql -U law4x -d law4x \
  -c "SELECT article_no, full_path, left(content, 120) AS preview FROM law_articles WHERE article_no = '第五百七十七条';"
```

## 6. 重复导入策略

导入脚本会先删除同名法规文件对应的 `law_documents` 记录，再重新插入。

由于 `law_articles` 通过外键 `ON DELETE CASCADE` 关联 `law_documents`，重复导入时旧条文会被自动删除。

## 7. 后续改进

- 支持 pdf/html/txt。
- 支持导入前解析预览。
- 支持人工修正条文。
- 支持前端上传文件。
- 支持生成 embedding 并写入 `law_article_embeddings`。
