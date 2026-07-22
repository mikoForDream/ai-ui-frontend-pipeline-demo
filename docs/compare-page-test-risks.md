# 商品对比页面测试骨架与阻塞记录

任务：`compare_test_010`  
当前步骤：3 - 搭建商品对比页面测试骨架与公共依赖

## 本次修正结论

测试环境与骨架已经建立，但仓库当前不存在可挂载的真实商品对比页面。因此本步骤只确认测试基础设施可运行，并将页面级验证明确标记为阻塞；不会使用临时 Harness 冒充真实页面，也不会声称页面测试已通过。

## 已建立内容

- `package.json`：提供 Vitest、jsdom、React Testing Library 等最小测试依赖和命令。
- `vitest.config.ts`、`tsconfig.json`：提供测试环境与 TypeScript 配置。
- `src/test/setup.ts`：统一加载 DOM 断言和测试清理。
- `src/test/compare-page/page-discovery.ts`：仅发现并加载真实页面候选模块。
- `src/test/compare-page/index.tsx`：统一复用步骤 2 的 fixtures 与场景清单。
- `src/pages/compare/__tests__/compare-page.spec.tsx`：运行基础校验；真实页面缺失时跳过页面集成组并记录阻塞。

## 当前阻塞

`BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`

仓库中未发现以下真实页面入口之一：

- `ComparePage.tsx` / `ComparePage.ts`
- `compare-page.tsx` / `compare-page.ts`
- `src/pages/compare/index.tsx` / `index.ts`

因此暂时无法验证页面渲染、交互、稳定选择器或详情跳转。真实页面加入后，发现器会启用页面集成测试组；若匹配到多个候选，则以 `BLOCKED_MULTIPLE_COMPARE_PAGE_CANDIDATES` 停止自动选择，避免挂载错误页面。

## 边界约束

- 不在本步骤定义最大对比数量、判重依据、差异高亮、兜底文案或详情路由。
- 不新建重复 Mock 数据；所有数据复用 `tests/fixtures/compare`。
- `scenarioConfig` 中的数值仅为测试参数，不代表生产规则。
- 接口字段继续作为候选样本，真实接口明确后再增加适配器和契约测试。

## 本地验证

```bash
npm install
npm test
```

预期结果：基础设施与 fixtures 校验通过；在真实页面尚未加入时，`real page integration` 测试组显示为 skipped，并由阻塞测试确认原因。
