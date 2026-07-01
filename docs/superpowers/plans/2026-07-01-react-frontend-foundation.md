# React Frontend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 `frontend/` 正式 React 前端骨架，并迁入第一版公众咨询工作台。

**Architecture:** 新增独立 `frontend/` 应用，采用 React + Vite 作为正式产品前端；Spring Boot 继续提供现有 REST 接口与静态联调页。正式前端先落导航壳子、公众咨询页、引用面板与法条详情查看，再为后续 AG-UI 与专业检索保留模块边界。

**Tech Stack:** React, Vite, JavaScript, CSS, Spring Boot REST APIs

---

### Task 1: 建立前端工程骨架

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/src/main.jsx`
- Create: `frontend/src/App.jsx`
- Create: `frontend/src/styles.css`
- Create: `frontend/.gitignore`

- [ ] **Step 1: 创建最小 Vite + React 工程文件**
- [ ] **Step 2: 配置 `npm` scripts，保证可启动开发服务**
- [ ] **Step 3: 渲染最小应用壳子，确认 React 正常挂载**

### Task 2: 搭建三栏工作台与导航骨架

**Files:**
- Modify: `frontend/src/App.jsx`
- Modify: `frontend/src/styles.css`
- Create: `frontend/src/components/AppShell.jsx`
- Create: `frontend/src/components/Sidebar.jsx`

- [ ] **Step 1: 提取应用壳子组件与左侧导航**
- [ ] **Step 2: 预留 `法律咨询 / 专业检索 / 法规库` 三个导航入口**
- [ ] **Step 3: 中右区域预留页面内容与辅助面板容器**

### Task 3: 迁移公众咨询页

**Files:**
- Create: `frontend/src/features/consultation/ConsultationPage.jsx`
- Create: `frontend/src/features/consultation/useConsultation.js`
- Create: `frontend/src/features/consultation/api.js`
- Modify: `frontend/src/App.jsx`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: 接入 `/api/rag/answer` 请求**
- [ ] **Step 2: 渲染问题输入、回答区、引用区**
- [ ] **Step 3: 处理加载、错误、空状态**

### Task 4: 迁移 citation 完整条文查看

**Files:**
- Modify: `frontend/src/features/consultation/ConsultationPage.jsx`
- Modify: `frontend/src/features/consultation/api.js`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: 接入 `/api/law/articles/{articleId}` 详情请求**
- [ ] **Step 2: 在引用卡片中展示摘录与完整条文展开**
- [ ] **Step 3: 处理详情加载失败提示**

### Task 5: 文档与联调说明更新

**Files:**
- Modify: `README.md`
- Modify: `docs/delivery/02-product-progress.md`

- [ ] **Step 1: 更新正式前端入口说明**
- [ ] **Step 2: 标注当前静态页为联调页**
- [ ] **Step 3: 将“下一步”更新到 React 正式前端迁移**

### Task 6: 基础验证

**Files:**
- Verify: `frontend/package.json`
- Verify: `frontend/src/**/*.jsx`

- [ ] **Step 1: 运行前端语法或构建检查**
- [ ] **Step 2: 启动开发服务并确认页面可打开**
- [ ] **Step 3: 用浏览器检查公众咨询页基本流程**
