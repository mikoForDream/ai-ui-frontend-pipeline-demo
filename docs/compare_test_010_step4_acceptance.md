# compare_test_010 / Step 4 商品添加主流程测试

任务：`商品对比页面测试用例与验收验证`  
步骤：4 - 编写商品添加主流程测试

## 修复结论

本步骤已补充可执行的商品添加主流程测试契约，但仓库当前仍没有真实商品对比页面，因此页面级用例保持明确阻塞和跳过状态。测试不会创建临时页面冒充真实实现，也不会用重复 Mock 数据制造“通过”结果。

## 文件与运行方式

- 测试文件：`src/pages/compare/__tests__/compare-page.add-flow.spec.tsx`
- 复用数据：`tests/fixtures/compare/compare-data.ts` 中的 `normalProducts`
- 页面发现：`src/test/compare-page/page-discovery.ts`
- 执行命令：`npm test`

旧文件 `src/__tests__/compare-page.add-flow.test.tsx` 已删除。它不符合当前 Vitest 的 `*.spec.ts(x)` 收集规则，并且包含自造页面、重复商品数据和未确认业务规则。

## 测试覆盖

测试按两个场景运行：

1. 添加前两个可用的正常 fixture。
2. 添加当前 fixtures 中所有可用的正常商品。

每个场景通过真实页面测试适配器完成：

- 挂载真实商品对比页。
- 按 fixture 执行添加操作。
- 验证对比列表中的 fixture 顺序。
- 验证主区域中的 fixture 顺序。
- 由适配器依据页面现有实现检查添加控件状态。

这里的样本数量只代表当前测试数据规模，不代表生产环境最大对比数量。

## 真实页面适配契约

页面模块可提供测试专用导出 `comparePageAddFlowTestAdapter`，包含：

- `renderPage()`
- `addFixture(fixture)`
- `readState()`，返回 `listedFixtureIds` 与 `mainAreaFixtureIds`
- `assertAddedControlState(fixture)`
- 可选的 `cleanup()`

适配器负责将 fixture 候选字段映射到真实页面，并使用页面已有的可访问名称或稳定选择器。测试本身不规定按钮文案、禁用方式、商品 ID 字段、详情路由或页面 DOM 结构。

## 当前阻塞

当前仓库状态：

- `BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`

后续可能出现的保护性阻塞：

- `BLOCKED_MULTIPLE_COMPARE_PAGE_CANDIDATES`
- `BLOCKED_COMPARE_PAGE_ADD_FLOW_ADAPTER_NOT_FOUND`

真实页面和适配器就绪后，集成用例会自动执行；缺少任一条件时明确跳过，不用 Harness 代替。

## 边界约束

- 不预设最大对比数量。
- 不预设商品 ID、SKU 或 SPU 判重方式。
- 不预设“加入对比”“已添加”等按钮文案或禁用规则。
- 不预设价格、库存、属性字段与真实接口的映射。
- 不预设详情跳转字段或路由。
- 不新增独立商品 Mock；只复用步骤2 fixtures。

## 预期测试结果

在真实页面尚未加入时：

- fixtures 与参数边界校验通过。
- 页面集成用例以 blocker 原因跳过。
- 不会产生伪造的页面功能通过结论。
