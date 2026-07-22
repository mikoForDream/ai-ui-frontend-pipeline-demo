# compare_test_010 / Step 5 重复添加与数量上限测试

任务：`商品对比页面测试用例与验收验证`  
步骤：5 - 编写重复添加与数量上限测试

## 修复结论

本步骤只补充真实商品对比页的边界测试契约，不新增商品对比页面、接口、类型或生产规则。原 PR 中自造的页面、空 API、重复类型和重复 Mock 测试已删除。

## 文件与运行方式

- 测试文件：`src/pages/compare/__tests__/compare-page.duplicate-limit.spec.tsx`
- 复用数据：`tests/fixtures/compare/compare-data.ts`
- 页面发现：`src/test/compare-page/page-discovery.ts`
- 执行命令：`npm test`

## 复用的步骤 2 fixtures

- `duplicateAttempt`：构造重复添加场景。
- `overLimitAttempt`：构造超出测试参数边界的场景。
- `scenarioConfig.testOnlyLimit`：仅用于边界测试数据，不代表生产环境最大对比数量。
- `unresolvedRules`：保留真实上限和判重依据的待确认状态。

测试不再定义 `Alpha Phone`、`detailUrl`、固定按钮文案或另一套商品类型。

## 真实页面适配契约

真实页面模块可提供测试专用导出 `comparePageBoundaryTestAdapter`：

- `renderPage()`
- `seedFixtures(fixtures)`
- `attemptAdd(fixture)`，返回 `accepted` 与行为类型
- `readFixtureIds()`
- `assertDuplicateFeedback(outcome)`
- `assertLimitFeedback(outcome, { testOnlyLimit })`
- 可选的 `cleanup()`

适配器负责连接真实页面已有的状态、接口调用和反馈方式。测试不规定提示文案、按钮是否禁用、前端或后端由哪一层拦截，也不规定商品 ID/SKU/SPU 的判重方式。

## 验收覆盖

1. 重复添加后，已有列表不出现第二个相同 fixture。
2. 重复尝试被真实实现识别为重复，并由适配器核验实际反馈。
3. 超过测试参数边界后，已有数据保持不变。
4. 超限尝试被真实实现识别为超限，并由适配器核验实际反馈。
5. 生产上限与生产判重依据仍保持待确认，不写入业务代码。

## 当前阻塞

仓库当前没有真实商品对比页：

- `BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`

后续保护性阻塞：

- `BLOCKED_MULTIPLE_COMPARE_PAGE_CANDIDATES`
- `BLOCKED_COMPARE_PAGE_BOUNDARY_ADAPTER_NOT_FOUND`

缺少真实页面或边界适配器时，页面集成用例明确跳过；fixture 来源、测试参数边界和未确认规则校验仍正常执行。

## 删除的错误实现

- `src/features/compare/__tests__/compare-page.spec.tsx`
- `src/features/compare/api.ts`
- `src/features/compare/compare-page.tsx`
- `src/features/compare/types.ts`

这些文件超出了“编写测试”的步骤范围，并写死了数量上限、提示文案、按钮状态、详情路由和重复商品结构。

## 预期结果

真实页面尚未实现时：

- 4 项 fixture/规则边界校验通过。
- 2 项真实页面集成用例按 blocker 跳过。
- 不产生伪造的页面功能通过结论。
