# compare_test_010 / Step 6 移除商品与刷新联动测试

任务：`商品对比页面测试用例与验收验证`  
步骤：6 - 编写移除商品与刷新联动测试

## 修复结论

本步骤已将当前 PR 中不符合仓库架构的自造页面测试替换为真实页面集成契约测试，并删除未被 Vitest 收集的旧文件。新测试只复用现有页面发现器、步骤 2 fixtures 和仓库已有测试适配模式；不会在测试文件内重新实现页面、领域类型、商品数组或生产规则。

## 文件与运行方式

- 测试文件：`src/pages/compare/__tests__/compare-page.remove-refresh.spec.tsx`
- 页面发现：`src/test/compare-page/page-discovery.ts`
- 统一导出：`src/test/compare-page/index.tsx`
- 复用 fixtures：`tests/fixtures/compare/compare-data.ts`
- 复用刷新样本：`tests/fixtures/compare/compare-api-samples.ts`
- 执行命令：`npm test`

已删除旧文件：

- `src/pages/compare/__tests__/compare-page.remove-refresh.test.tsx`
- `docs/compare_test_010-step6-acceptance.md`

删除原因：旧测试文件名不符合当前 `*.spec.ts(x)` 收集规则，并在测试中自造页面、重复类型、重复商品数据、测试外推的数量上限与 UI 文案。

## 复用的步骤 2 fixtures

- `normalCompareSet`：作为移除前的种子输入。
- `postRemoveCompareSet`：作为移除并刷新后的期望结果。
- `postRemoveRefreshResponse`：作为刷新场景样本来源。

这里的 fixture 顺序仅表示测试输入与刷新后的观察结果，不代表生产排序规则；测试只要求页面最终表现与刷新结果一致。

## 真实页面适配契约

真实页面模块可提供测试专用导出 `comparePageRemoveRefreshTestAdapter`：

- `renderPage()`
- `seedFixtures(fixtures)`
- `removeFixture(fixture)`
- `awaitRefreshSettled()`
- `readState()`，返回：
  - `listedFixtureIds`
  - `mainAreaFixtureIds`
  - `removedFixtureId`
  - `staleFixtureIds`
  - `addEntryAvailable`
  - `placeholderCount`
- `assertRefreshDrivenView({ before, removed, after })`
- `assertAddEntryRecovered(state)`
- 可选的 `cleanup()`

适配器负责对接真实页面已有的 DOM、接口 mock/联调方式和刷新完成信号。测试本身不规定：

- 必须重新请求几次
- 必须采用乐观更新还是重新拉取
- 入口按钮具体文案
- 占位文案
- 详情字段
- 判重依据
- 生产最大数量

`placeholderCount` 允许返回 `null`，用于兼容“页面不使用显式占位槽，但仍可观察到入口状态恢复”的实现。

## 验收覆盖

1. 移除一个商品后，页面进入真实实现定义的刷新完成状态。
2. 刷新完成后，对比列表中的 fixture 顺序与 `postRemoveCompareSet` 一致。
3. 主区域中的 fixture 顺序与对比列表保持一致。
4. 被移除 fixture 不再出现在已渲染结果中，且不存在陈旧 fixture 残留。
5. 可继续添加商品的入口状态恢复。
6. 若实现存在显式占位态，可通过 `placeholderCount` 观察其变化；若无显式占位态，不强行规定 UI 结构。

## 当前阻塞

仓库当前仍未发现真实商品对比页，因此页面集成组继续受以下 blocker 保护：

- `BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`

若真实页面存在但未提供本步骤适配器，则使用：

- `BLOCKED_COMPARE_PAGE_REMOVE_REFRESH_ADAPTER_NOT_FOUND`

若后续出现多个候选页面，则仍由页面发现器给出：

- `BLOCKED_MULTIPLE_COMPARE_PAGE_CANDIDATES`

缺少真实页面或适配器时，测试会明确跳过，不会构造临时页面让用例伪通过。

## 预期结果

在真实页面尚未加入时：

- fixture 与契约边界校验通过。
- `real page integration` 组按 blocker 跳过。
- 不会对“移除后必然重新请求”或“生产上限/按钮文案已确认”作出错误结论。
