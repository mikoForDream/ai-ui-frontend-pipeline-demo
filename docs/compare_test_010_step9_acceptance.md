# compare_test_010 / Step 9 加载中、空数据与接口异常测试

任务：`商品对比页面测试用例与验收验证`  
步骤：9 - 编写加载中、空数据与接口异常测试

## 修复结论

本步骤新增商品对比页状态处理的真实页面测试契约，继续复用仓库现有页面发现器、步骤 2 fixtures、接口样本与既有测试适配模式。测试只覆盖当前步骤要求的加载中、空数据、500、超时、缺失字段和重试恢复，不在测试文件中实现页面、业务组件、接口或领域类型。

局部渲染失败场景在当前仓库上下文中仍缺少稳定判定口径，因此本步骤明确记录为 blocker，而不是伪造一个页面或自定义失败规则让测试通过。

## 文件与运行方式

- 测试文件：`src/pages/compare/__tests__/compare-page.state.spec.tsx`
- 页面发现：`src/test/compare-page/page-discovery.ts`
- 统一导出：`src/test/compare-page/index.tsx`
- 复用 fixtures：`tests/fixtures/compare/compare-data.ts`
- 复用样本：`tests/fixtures/compare/compare-api-samples.ts`
- 待确认规则：`tests/fixtures/compare/compare-types.ts`
- 执行命令：`npm test`

本步骤未新增 `.test.ts(x)` 文件，保持与当前 Vitest `src/**/*.spec.{ts,tsx}` 收集规则一致。

## 复用的步骤 2 fixtures 与样本

- 空数据：`emptyCompareResponse`
- 缺字段：`missingFieldCompareResponse`
- 500 错误：`serverErrorResponse`
- 超时：`timeoutErrorResponse`
- 恢复后的正常数据：`normalProducts`
- 待确认接口口径：`unresolvedRules.apiContract`

这里的返回样本仅作为 test-only 输入来源，不代表真实接口字段名、请求次数、重试方式、错误文案或页面状态枚举。测试只要求页面通过真实适配器暴露当前实现可观察到的状态变化。

## 真实页面适配契约

真实页面模块可提供测试专用导出 `comparePageStateTestAdapter`：

- `renderPage()`
- `mockQuerySample(sample)`
- `startInitialLoad()`
- `awaitSettled()`
- `readState()`，返回：
  - `viewState`
  - `listedFixtureIds`
  - `mainAreaFixtureIds`
  - `visibleErrorText`
  - `visibleEmptyText`
  - `retryAvailable`
  - `addEntryAvailable`
  - `staleFixtureIds`
- 可选 `retryLoad()`
- `assertLoadingPresentation()`
- `assertEmptyPresentation(state)`
- `assertErrorPresentation({ sample, state })`
- `assertPartialPresentation({ fixture, state })`
- 可选的 `cleanup()`

适配器负责对接真实页面已有的 DOM、状态切换、接口 mock/联调方式和重试交互。测试本身不规定：

- 加载中必须使用骨架、spinner 还是其他占位
- 空态必须使用哪句文案
- 错误态必须使用哪句文案
- 必须重新请求几次
- 必须使用按钮点击、自动重试还是其他恢复方式
- 缺字段是否表现为局部占位、静默降级或其他现有实现
- 局部渲染失败一定要如何显示

`viewState`、`retryAvailable` 与其他字段只是适配器向测试暴露的观察结果，用于避免把页面文案和 DOM 结构硬编码成产品规则。

## 验收覆盖

1. 首次加载时可观察到加载中状态，结束后切换到正常展示。
2. 空数组返回时进入空态，列表与主区域无脏数据残留。
3. 500 错误时进入可验证的错误态，并在存在真实重试入口时验证恢复流程。
4. 超时场景与 500 场景分开覆盖，并在存在真实重试入口时验证恢复流程。
5. 缺失字段输入时页面不整页崩溃，不展示 `undefined`、`null`、`NaN` 或对象串值。
6. 局部渲染失败因当前实现缺少稳定判定口径，明确记录为 blocker，不伪造通过结论。

## 当前阻塞

仓库当前仍未发现真实商品对比页，因此页面集成组继续受以下 blocker 保护：

- `BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`

若真实页面存在但未提供本步骤适配器，则使用：

- `BLOCKED_COMPARE_PAGE_STATE_ADAPTER_NOT_FOUND`

若真实页面已能进入错误态，但仓库当前没有可稳定触发的重试入口观察方式，则使用：

- `BLOCKED_COMPARE_PAGE_STATE_RETRY_NOT_OBSERVABLE`

若局部渲染失败仍没有明确的页面可观察口径，则使用：

- `BLOCKED_PARTIAL_RENDER_FAILURE_ORACLE_UNCONFIRMED`

缺少真实页面或适配器时，测试会明确跳过，不会构造临时页面让用例伪通过。

## 预期结果

在真实页面尚未加入时：

- state fixtures、错误样本与待确认规则校验通过。
- `real page integration` 组按 blocker 跳过。
- 不会对错误文案、请求次数、重试方式或局部渲染失败口径作出未经确认的产品结论。
