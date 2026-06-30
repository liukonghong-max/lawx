# Spring Boot 后端说明

## 1. 定位

`backend/` 是 law4x 的正式后端主线，技术栈为：

- Spring Boot 3.x
- Java 17 target
- Apache POI
- PostgreSQL + pgvector
- 后续接入 AgentScope Java v2

根目录下的 Python 脚本仍然保留为数据验证和离线辅助工具，不作为线上后端主实现。

## 2. 当前能力

当前 Java 后端已实现：

- Spring Boot 项目骨架
- `LawDocxParser`
- `ParsedLawDocument`
- `LawArticle`
- 民法典 docx 解析测试

已验证：

- 识别标题：中华人民共和国民法典
- 识别发布日期：2020-05-28
- 解析条文数：1260
- 识别编/章/节/条层级
- 合并多段条文
- 命中《民法典》第五百七十七条

## 3. 运行测试

在 `backend/` 目录执行：

```bash
mvn test
```

当前测试：

```text
LawDocxParserTest
- parsesCivilCodeMetadata
- parsesAllCivilCodeArticles
- keepsHierarchyForFirstArticle
- parsesContractLiabilityArticle
- mergesContinuationParagraphsIntoArticle
```

## 4. Python 工具和 Java 后端的关系

Python 工具用途：

- 快速验证 docx 解析规则。
- 快速验证数据库 schema。
- 快速导入样本数据。
- 快速验证关键词检索。

Java 后端用途：

- 正式产品后端。
- 法规导入服务。
- 法条检索服务。
- RAG 测试服务。
- AgentScope Java Agent 编排。
- AG-UI 协议输出。

迁移策略：

```text
Python 原型验证
 -> Java 测试固化行为
 -> Java 服务实现
 -> Python 保留为离线工具或逐步删除
```

## 5. 后续计划

下一步迁移：

- `LawImportService`
- `LawArticleRepository`
- `LawArticleSearchService`
- keyword search Java 实现
- 将解析结果写入 PostgreSQL

再下一步：

- embedding 生成
- pgvector 查询
- AgentScope Java v2 RAG tool
- AG-UI `/ag-ui` 接口
