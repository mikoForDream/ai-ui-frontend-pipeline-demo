# compare_test_010 / Step 10 商品详情跳转测试

任务：`商品对比页面测试用例与验收验证`  
步骤：10 - 编写商品详情跳转测试

## 修复结论

本步骤补充了商品对比页详情跳转行为的真实页面测试契约，继续复用仓库现有页面发现器、步骤 2 fixtures 与既有测试适配模式。测试只覆盖当前步骤要求的详情入口触发、缺失跳转字段容错，以及异常恢复后跳转不受无关状态影响；不会在测试文件内实现页面、业务组件、API、路由规则或领域类型。

## 文件与运行方式

- 测试文件：`src/pages/compare/__tests__/compare-page.detail-navigation.spec.tsx`
- 页面发现：`src/test/compare-page/page-discovery.ts`
- 统一导出：`src/test/compare-page/index.tsx`
- 复用 fixtures：`tests/fixtures/compare/compare-data.ts`
- 场景清单：`tests/fixtures/compare/compare-scenarios.ts`
- 待确认规则：`tests/fixtures/compare/compare-types.ts`
- 执行命令：`npm test`

本步骤未新增 `.test.ts(x)` 文件，保持与当前 Vitest `src/**/*.spec.{ts,tsx}` 收集规则一致。

## 复用的步骤 2 fixtures

- 正常跳转输入：`normalProducts`
- 缺少跳转依赖字段场景：`missingFieldProduct`
- 导航场景登记：`compareScenarioChecklist` 中的 `compare-detail-navigation`
- 待确认跳转规则：`unresolvedRules.detailNavigation`

这里的 `detailCandidate.identifier` 仅作为 test-only 候选输入来源，用于核对适配器是否能把当前实现暴露出的可观察跳转目标关联回 fixture。它不代表生产一定使用商品 ID、slug、链接地址或某个固定路由参数。

## 真实页面适配契约

真实页面模块可提供测试专用导出 `comparePageDetailNavigationTestAdapter`：

- `renderPage()`
- `seedFixtures(fixtures)`
- `triggerDetailNavigation(fixture)`，返回：
  - `kind`：`route` / `link` / `none`
  - `href`
  - `identifier`
- `assertNavigatedToFixture({ fixture, target })`
- `assertNavigationUnavailable({ fixture, target })`
- 可选 `enterErrorStateWithRetryRecovery({ failingFixture, recoveryFixtures })`
- 可选的 `cleanup()`

适配器负责连接真实页面已有 DOM、路由、链接、window 打开行为或其他稳定跳转观察方式。测试本身不规定：

- 详情一定通过前端路由还是外链打开
- 一定使用商品 ID、SKU、slug 还是参数对象跳转
- 缺失跳转字段时必须禁用、隐藏还是无动作
- 错误恢复必须通过哪种重试入口完成
- 跳转行为必须依赖哪一个具体接口字段

当真实页面或适配器当前无法稳定暴露跳转目标时，测试使用 blocker/skip 明确记录，不伪造页面或路由让用例通过。

## 验收覆盖

1. 具备可观察跳转目标的商品，在当前实现下能够触发详情跳转。
2. 缺失详情跳转依赖字段时，页面表现符合现有实现且不会抛错或渲染脏值。
3. 错误与重试恢复流程若可被适配器稳定观察，不应无故破坏本来可用的详情跳转。
4. 详情跳转字段、路由名称与参数来源仍通过 `unresolvedRules.detailNavigation` 明确保留为待确认项。

## 当前阻塞

仓库当前仍未发现真实商品对比页，因此页面集成组继续受以下 blocker 保护：

- `BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`

若真实页面存在但未提供本步骤适配器，则使用：

- `BLOCKED_COMPARE_PAGE_DETAIL_NAVIGATION_ADAPTER_NOT_FOUND`

若真实页面当前无法稳定暴露详情跳转目标或字段来源仍不可观察，则测试会使用：

- `BLOCKED_待确认详情跳转字段、路由名称和参数来源`

若真实页面无法稳定进入“错误后恢复再校验跳转”的观察流程，则测试会使用：

- `BLOCKED_COMPARE_PAGE_DETAIL_NAVIGATION_ERROR_RECOVERY_NOT_OBSERVABLE`

缺少真实页面、适配器或稳定观察口径时，测试会明确跳过，不会构造临时页面、伪路由或假链接制造通过结论。

## 预期结果

在真实页面尚未加入时：

- fixture 来源、待确认跳转规则与缺字段覆盖校验通过。
- `real page integration` 组按 blocker 跳过。
- 不会对详情页地址、路由参数结构、点击后打开方式或异常恢复实现作出未经确认的产品结论。
