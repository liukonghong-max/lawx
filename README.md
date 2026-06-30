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
- `docs/delivery/01-implementation-plan.md`：MVP 实现计划
- `中华人民共和国民法典_20200528.docx`：第一条法规导入样本

## 技术方向

- 前端：React + AG-UI Protocol
- 后端：Spring Boot 3.x + AgentScope Java v2
- 数据库：PostgreSQL 16 + pgvector
- 检索：keyword search + vector search + rerank
- Agent：基于可追溯 citation 的法律法规问答

## MVP 非目标

- 不做胜诉率预测
- 不做类案裁判倾向分析
- 不提供正式法律意见
- 不允许无来源回答
- 不编造法律名称、条号、案例或来源
