# lawx

基于权威法律法规库的法律咨询与检索工作台。

## 项目目标

lawx 的 MVP 目标是打通法律法规 RAG Agent 的最小闭环：

```text
上传法规文件
 -> 解析条文
 -> 生成知识库
 -> 用户提问
 -> 检索法条
 -> Agent 回答
 -> 展示引用依据
```

## 当前内容

- `docs/product/01-product-blueprint.md`：产品蓝图
- `docs/product/02-mvp-prd.md`：MVP PRD
- `docs/technical/01-architecture.md`：技术架构设计
- `docs/technical/02-database-schema.md`：数据库 Schema 设计
- `docs/technical/03-local-database.md`：本地数据库启动说明
- `docs/technical/04-data-import.md`：法规数据导入说明
- `docs/delivery/01-implementation-plan.md`：MVP 实现计划
- `中华人民共和国民法典_20200528.docx`：第一条法规导入样本

## 技术方向

- 前端：React + AG-UI Protocol
- 后端：Spring Boot 3.x + AgentScope Java v2
- 数据库：PostgreSQL 16 + pgvector
- 检索：keyword search + vector search + rerank
- Agent：基于可追溯 citation 的法律法规问答

## 后端主线

正式后端位于：

```text
backend/
```

运行 Java 测试：

```bash
cd backend
mvn test
```

当前 Java 后端已实现民法典 docx 解析测试和解析器。根目录下的 Python 脚本用于数据验证和离线辅助，不作为线上后端主实现。

## 本地数据库

启动 PostgreSQL + pgvector：

```bash
docker compose up -d
```

连接信息：

```text
postgresql://law4x:law4x_dev@localhost:5432/law4x
```

## 导入样本法规

```bash
python3 scripts/import_law_docx.py 中华人民共和国民法典_20200528.docx --effective-date 2021-01-01
```

## 检索样本法规

```bash
python3 scripts/search_law_articles.py 违约责任 --limit 5
python3 scripts/search_law_articles.py 第五百七十七条 --limit 3
python3 scripts/search_law_articles.py 借款合同 --limit 5
```

## MVP 非目标

- 不做胜诉率预测
- 不做类案裁判倾向分析
- 不提供正式法律意见
- 不允许无来源回答
- 不编造法律名称、条号、案例或来源
