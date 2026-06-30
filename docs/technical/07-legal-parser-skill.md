# 法律文件解析 Skill 设计

## 1. 定位

法律文件解析不放在 Spring Boot 核心后端中实现，而是作为独立 Skill / 离线导入工具存在。

原因：

- 法规文件解析是低频后台任务。
- docx、pdf、html、txt 的格式差异大。
- 解析规则需要频繁试错和人工校验。
- 后端主链路应聚焦检索、RAG、Agent、引用和权限。

## 2. 职责边界

### Parser Skill 负责

- 读取 docx、pdf、html、txt 等法规文件。
- 抽取原始文本。
- 识别法规元数据。
- 按编、章、节、条切分。
- 合并条文续段。
- 生成标准结构化输出。
- 写入数据库或输出 SQL/JSON。
- 生成解析质量报告。

### Spring Boot 后端负责

- 读取 `law_documents` 和 `law_articles`。
- 提供法条检索 API。
- 提供 RAG 测试 API。
- 封装 AgentScope Java tools。
- 输出 AG-UI 事件流。
- 管理会话、引用、反馈和评测。

## 3. 标准输出格式

Parser Skill 的核心产物是结构化法规 JSON：

```json
{
  "title": "中华人民共和国民法典",
  "lawType": "法律",
  "publishDate": "2020-05-28",
  "effectiveDate": "2021-01-01",
  "sourceFileName": "中华人民共和国民法典_20200528.docx",
  "sourceUrl": null,
  "articles": [
    {
      "articleNo": "第一条",
      "articleOrder": 1,
      "bookTitle": "第一编 总则",
      "chapterTitle": "第一章 基本规定",
      "sectionTitle": null,
      "content": "为了保护民事主体的合法权益，调整民事关系...",
      "fullPath": "中华人民共和国民法典 > 第一编 总则 > 第一章 基本规定 > 第一条",
      "contentHash": "..."
    }
  ]
}
```

## 4. 当前实现

当前 Skill 原型位于：

```text
scripts/law_docx_parser.py
scripts/import_law_docx.py
```

已验证：

- 《中华人民共和国民法典》解析出 1260 条。
- 第一条层级路径正确。
- 第五百七十七条可命中违约责任内容。
- 第一千二百五十四条等多段条文可合并。

测试位于：

```text
tests/test_law_docx_parser.py
tests/test_import_law_docx.py
```

## 5. 导入方式

短期继续使用离线命令导入：

```bash
python3 scripts/import_law_docx.py 中华人民共和国民法典_20200528.docx --effective-date 2021-01-01
```

后续可以扩展为：

- Codex Skill
- 管理后台上传任务
- 独立批处理服务
- CI/定时法规更新任务

无论实现形式如何，都必须输出同一份标准结构化格式。

## 6. 后续演进

### P0

- 固化 docx 解析。
- 输出标准 JSON。
- 支持写入 PostgreSQL。
- 提供解析质量报告。

### P1

- 支持 html/txt。
- 支持解析预览。
- 支持人工修正后再发布。

### P2

- 支持 pdf。
- 支持法规版本对比。
- 支持增量更新。
- 支持解析异常标注和回放。
