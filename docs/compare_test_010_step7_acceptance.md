# compare_test_010 / Step 7 参数对比与差异高亮测试

任务：`商品对比页面测试用例与验收验证`  
步骤：7 - 编写参数对比与差异高亮测试

## 修复结论

本步骤补充了商品对比页参数区域的真实页面测试契约，继续复用仓库现有页面发现器、步骤 2 fixtures 和测试适配模式。测试只覆盖当前步骤要求的参数展示、差异高亮、长字段和空值场景，不在测试文件中实现页面、业务组件、接口或领域类型。

## 文件与运行方式

- 测试文件：`src/pages/compare/__tests__/compare-page.spec-difference-highlight.spec.tsx`
- 页面发现：`src/test/compare-page/page-discovery.ts`
- 统一导出：`src/test/compare-page/index.tsx`
- 复用 fixtures：`tests/fixtures/compare/compare-data.ts`
- 待确认规则：`tests/fixtures/compare/compare-types.ts`
- 执行命令：`npm test`

本步骤未新增 `.test.ts(x)` 文件，保持与当前 Vitest `src/**/*.spec.{ts,tsx}` 收集规则一致。

## 复用的步骤 2 fixtures

- 基础商品来源：`normalProducts`
- 差异规则占位：`unresolvedRules.differenceCalculation`

测试在 `normalProducts` 基础上仅做 test-only 的规格字段整形，以便构造：

1. 全部相同的参数行
2. 明显不同的参数行
3. 含长文本的参数值
4. 含空值的参数值

这些整形后的 `specs` 仅用于当前测试输入，不代表真实接口字段、真实差异算法、真实空态文案或真实折行策略。

## 真实页面适配契约

真实页面模块可提供测试专用导出 `comparePageDifferenceHighlightTestAdapter`：

- `renderPage()`
- `seedFixtures(fixtures)`
- `readSpecRows()`，返回每一行：
  - `key`
  - `label`
  - `values`
  - `highlighted`
- `assertLongValuePresentation({ key, expectedValue })`
- `assertEmptyValuePresentation({ key })`
- 可选的 `cleanup()`

适配器负责连接真实页面已有 DOM、样式状态和稳定选择器。测试本身不规定：

- 差异高亮一定由前端计算还是接口返回
- 高亮具体 className、颜色或文案
- 空值必须显示哪种占位文案
- 长字段必须折行、截断还是其他现有展示策略

## 验收覆盖

1. 相同属性行可以正常展示，且不会误报为高亮。
2. 差异属性行按页面当前实现暴露为高亮状态；若仓库尚未确认该规则，测试明确以 blocker/待确认方式记录，而不是伪造通过。
3. 长字段内容可被页面完整承载，并由适配器校验当前实现没有产生脏展示。
4. 空值场景通过适配器验证页面使用现有容错展示，而不是渲染 `undefined`、`null` 等脏值。
5. 差异高亮依赖来源继续记录在 `unresolvedRules.differenceCalculation`，测试已落实该依赖未确认时的保护性跳过。

## 当前阻塞

仓库当前仍未发现真实商品对比页，因此页面集成组继续受以下 blocker 保护：

- `BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`

若真实页面存在但未提供本步骤适配器，则使用：

- `BLOCKED_COMPARE_PAGE_DIFFERENCE_HIGHLIGHT_ADAPTER_NOT_FOUND`

若差异高亮规则本身仍未在真实实现中可稳定识别，则测试会以 `unresolvedRules.differenceCalculation` 对应的 blocker 说明跳过该断言，不会擅自把 fixture 对比结果写成生产规则。

## 预期结果

在真实页面尚未加入时：

- fixture 来源与待确认规则校验通过。
- `real page integration` 组按 blocker 跳过。
- 不会对差异高亮的计算来源、空值文案或长字段样式作出未经确认的产品结论。
