# compare_test_010 / Step 8 价格与库存展示测试

任务：`商品对比页面测试用例与验收验证`  
步骤：8 - 编写价格与库存展示测试

## 修复结论

本步骤新增商品对比页价格与库存区域的真实页面测试契约，继续复用仓库现有页面发现器、步骤 2 fixtures 与既有测试适配模式。测试仅覆盖当前步骤要求的价格正常展示、价格缺失、库存为 0 与异常值容错，不在测试文件中实现页面、业务组件、接口或领域类型。

## 文件与运行方式

- 测试文件：`src/pages/compare/__tests__/compare-page.price-stock.spec.tsx`
- 页面发现：`src/test/compare-page/page-discovery.ts`
- 统一导出：`src/test/compare-page/index.tsx`
- 复用 fixtures：`tests/fixtures/compare/compare-data.ts`
- 复用样本：`tests/fixtures/compare/compare-api-samples.ts`
- 待确认规则：`tests/fixtures/compare/compare-types.ts`
- 执行命令：`npm test`

本步骤未新增 `.test.ts(x)` 文件，保持与当前 Vitest `src/**/*.spec.{ts,tsx}` 收集规则一致。

## 复用的步骤 2 fixtures

- 正常展示输入：`normalProducts[0]`
- 价格缺失输入：`missingPriceProduct`
- 库存为 0 输入：`outOfStockProduct`
- 对应接口样本：`missingPriceCompareResponse`、`outOfStockCompareResponse`
- 待确认文案规则：`unresolvedRules.missingPriceCopy`、`unresolvedRules.missingStockCopy`

测试中的异常值场景仅在 `missingPriceProduct` 基础上做 test-only 的原始字段整形，用于验证页面容错，不代表真实接口会返回这些值，也不把这些值写成生产规则。

## 真实页面适配契约

真实页面模块可提供测试专用导出 `comparePagePriceStockTestAdapter`：

- `renderPage()`
- `seedFixtures(fixtures)`
- `readPriceStock()`，返回每个已渲染商品的：
  - `fixtureId`
  - `priceText`
  - `stockText`
  - `priceState`
  - `stockState`
- `assertValuePresentation({ fixture, observation })`
- `assertMissingPricePresentation({ fixture, observation })`
- `assertOutOfStockPresentation({ fixture, observation })`
- `assertInvalidValueSanitized({ fixture, observation })`
- 可选的 `cleanup()`

适配器负责对接真实页面已有 DOM、格式化逻辑、样式状态和稳定选择器。测试本身不规定：

- 价格缺失时必须显示哪句文案
- 库存为 0 时必须显示哪句文案
- 价格格式化必须采用哪种货币样式
- 库存异常值必须降级为空、占位还是其他既有表现
- 局部状态由接口返回还是页面自身格式化逻辑决定

`priceState` 与 `stockState` 只是适配器向测试暴露的观察结果，用于避免把页面文案硬编码成测试规则。

## 验收覆盖

1. 正常价格与库存值可在页面当前实现下完整展示。
2. 价格缺失时，页面不渲染 `undefined`、`null`、`NaN` 等脏值，也不因缺字段报错。
3. 库存为 0 时，页面按现有实现暴露缺货状态；测试不擅自规定固定文案。
4. 异常值输入时，页面不会直接展示未处理原始异常值，也不会出现脚本报错或明显展示错位。
5. 缺省展示文案仍未在仓库中确认，因此继续通过 `unresolvedRules` 记录为待确认项，而不是伪造通过标准。

## 当前阻塞

仓库当前仍未发现真实商品对比页，因此页面集成组继续受以下 blocker 保护：

- `BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`

若真实页面存在但未提供本步骤适配器，则使用：

- `BLOCKED_COMPARE_PAGE_PRICE_STOCK_ADAPTER_NOT_FOUND`

若后续出现多个候选页面，则仍由页面发现器给出：

- `BLOCKED_MULTIPLE_COMPARE_PAGE_CANDIDATES`

缺少真实页面或适配器时，测试会明确跳过，不会构造临时页面让用例伪通过。

## 缺失项记录

当前仓库上下文仍未确认以下价格/库存展示口径，因此本步骤只记录，不外推为产品结论：

- 价格缺失时的默认展示文案
- 库存为 0 时的默认展示文案
- 库存缺失或非法值时的默认展示文案
- 局部异常时是否显示独立错误态或仅做静默降级

## 预期结果

在真实页面尚未加入时：

- fixture 来源、异常值 test-only 约束与待确认规则校验通过。
- `real page integration` 组按 blocker 跳过。
- 不会对价格/库存缺省文案、格式化样式或异常值来源作出未经确认的产品结论。
