# compare_test_010 / Step 11 接口联调与页面回归测试执行记录

任务：`商品对比页面测试用例与验收验证`  
步骤：11 - 执行接口联调与页面回归测试

## 执行结论

本步骤基于仓库现有商品对比页测试文件、页面发现器、步骤 2 fixtures 与既有 blocker/skip 契约，对“接口联调与页面回归测试”当前可执行范围进行了核对与记录。

当前目标分支仍未发现真实商品对比页面入口，且仓库上下文中也未提供联调环境域名、鉴权方式、真实接口定义或可访问的集成环境配置。因此本步骤不能把未执行的联调项记为通过，也不能伪造页面、接口或适配器制造通过结论。

当前可确认结果为：

- Mock/fixture 驱动的页面测试契约已在 `src/pages/compare/__tests__` 下覆盖添加、重复/上限、移除刷新、差异高亮、价格库存、状态流转与详情跳转。
- 真实页面集成测试统一受 `src/test/compare-page/page-discovery.ts` 保护；在真实页面缺失时按 blocker 跳过。
- 联调环境不可执行项已明确记录为未执行，不视为通过。
- Mock 结论与联调结论当前无法做一一对照，因为联调前提未满足；该差异本身已记录为环境阻塞，而不是功能通过。

## 本步骤涉及文件

- 测试基线：`src/pages/compare/__tests__/compare-page.spec.tsx`
- 添加流程：`src/pages/compare/__tests__/compare-page.add-flow.spec.tsx`
- 重复/上限：`src/pages/compare/__tests__/compare-page.duplicate-limit.spec.tsx`
- 移除刷新：`src/pages/compare/__tests__/compare-page.remove-refresh.spec.tsx`
- 差异高亮：`src/pages/compare/__tests__/compare-page.spec-difference-highlight.spec.tsx`
- 价格库存：`src/pages/compare/__tests__/compare-page.price-stock.spec.tsx`
- 状态流转：`src/pages/compare/__tests__/compare-page.state.spec.tsx`
- 详情跳转：`src/pages/compare/__tests__/compare-page.detail-navigation.spec.tsx`
- 页面发现器：`src/test/compare-page/page-discovery.ts`
- fixtures 与样本：`src/test/compare-page/index.tsx`、`tests/fixtures/compare/*`

## 联调执行前提核对

当前仓库上下文可验证的事实：

1. Vitest 仅收集 `src/**/*.spec.{ts,tsx}`，现有商品对比测试文件命名符合规则。
2. `src/test/compare-page/page-discovery.ts` 只会发现真实页面候选，不会创建测试页面替身。
3. `comparePageDiscovery.blocker` 在未发现页面时为 `BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`。
4. `tests/fixtures/compare/compare-types.ts` 明确保留 `apiContract`、`detailNavigation`、`compareLimit`、`duplicateIdentity`、`differenceCalculation` 等待确认项。
5. 仓库上下文未提供：
   - 真实商品对比页面文件
   - 真实商品对比接口定义
   - 联调环境地址
   - 鉴权或环境变量说明
   - 可观察真实接口行为的测试适配器实现

据此，本步骤只能输出执行记录与阻塞说明，不能新增“已通过的联调断言”。

## 已有 Mock/契约覆盖结论

以下结论来自现有测试文件与步骤 2 fixtures 的覆盖范围核对，不代表真实联调已通过：

1. 页面渲染与添加流程：
   - 已有测试验证添加 2 个及当前全部 normal fixtures 的页面契约。
   - 不把 fixture 数量写成生产上限。

2. 重复校验与数量上限：
   - 已有测试复用 `duplicateAttempt` 与 `overLimitAttempt`。
   - 判重依据与生产上限仍保持 unresolved，不从 fixture 推断业务规则。

3. 移除后的可观察结果：
   - 已有测试要求验证列表、主区域、陈旧数据清理与入口恢复状态。
   - 不强制规定必须重新请求，也不假设乐观更新。

4. 差异高亮：
   - 已有测试覆盖相同项、不同行、长值与空值。
   - 若真实实现无法稳定暴露高亮规则，则按 `unresolvedRules.differenceCalculation` 跳过，而不是放宽为 unknown。

5. 价格与库存：
   - 已有测试覆盖正常值、价格缺失、库存为 0、异常值清洗。
   - 未把缺失文案或样式硬编码为产品规则。

6. 关键状态：
   - 已有测试明确要求 `loading`、`ready`、`empty`、`error`、`ready` 的状态链路。
   - 缺字段场景接受 `partial` 或 `ready`，没有把 `unknown` 加入通过分支。

7. 详情跳转：
   - 已有测试复用 `detailCandidate` 作为 test-only 候选输入。
   - 跳转字段来源、路由名与参数规则保持 unresolved。

## 联调执行结果记录

### 1. 真实/集成环境页面验证

结果：未执行  
原因：仓库当前未发现真实商品对比页面，页面发现器给出 blocker：

- `BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`

影响：

- 无法挂载真实页面执行商品对比数据查询联调。
- 无法验证添加商品、移除商品、差异展示、价格库存展示与详情跳转在联调环境中的真实表现。
- 无法对比 Mock 结论与真实页面渲染结果是否一致。

### 2. 商品对比数据查询接口联调

结果：未执行  
原因：未提供真实接口定义、联调地址、请求方式、鉴权或环境配置。

当前仅能确认：

- `tests/fixtures/compare/compare-api-samples.ts` 中的 payload 是样本占位，不是生产接口契约。
- `unresolvedRules.apiContract` 仍成立。

### 3. 添加商品接口联调

结果：未执行  
原因：无真实页面、无真实接口配置、无联调适配器。

当前仅能确认：

- 重复拦截与上限拒绝在测试中已按 fixture 驱动建模。
- 这些结论不能外推为真实接口已验证通过。

### 4. 移除商品后刷新联调

结果：未执行  
原因：无真实页面与真实接口可观察口径。

当前仅能确认：

- 测试契约要求验证移除后的列表同步、主区域同步、旧数据清理与入口恢复。
- 测试没有把“必须重新请求”写成通过条件，符合当前仓库约束。

### 5. 详情跳转联调

结果：未执行  
原因：无真实页面、无真实路由/链接规则、无联调环境。

当前仅能确认：

- 详情跳转字段来源仍是待确认项。
- 缺字段场景只作为 test-only 覆盖输入，不推断生产路由规则。

## Mock 与联调差异归档

当前无法形成“功能行为差异”对照表，因为联调未执行。已识别并归档的差异来源如下：

1. 页面存在性差异：
   - Mock/fixture 契约测试存在。
   - 真实页面模块不存在。

2. 接口契约差异：
   - Mock 样本存在，但已在 fixtures 中明确为占位 payload。
   - 真实接口字段、状态码语义、判重规则、上限来源、详情跳转字段均未确认。

3. 环境可达性差异：
   - 本地 Vitest 环境已定义。
   - 联调环境入口、凭证与稳定数据源未提供。

这些差异当前属于“执行前置条件缺失”，不是“联调通过”。

## 回归测试结论

在当前仓库上下文下，可以成立的回归结论只有：

1. 商品对比页面相关测试文件已覆盖本任务要求的核心流程与关键状态，并复用既有 fixtures、页面发现器和 blocker 契约。
2. 现有测试没有把 fixture 边界数据、按钮文案、占位文案、详情字段、刷新方式、请求次数或判重依据写成生产规则。
3. 真实页面/接口联调回归尚未执行，不能声明“页面渲染、添加移除、差异展示和详情跳转在联调环境下已验证通过”。

## 验收状态

按本步骤验收标准逐项记录：

1. 核心接口在联调环境下已完成至少一轮页面验证：未完成
2. 页面渲染、添加移除、差异展示和详情跳转在联调环境下结果已记录：受阻，仅记录未执行原因
3. Mock 结果与联调结果不一致的项已被识别并归档：已归档为“联调前提缺失导致无法对照”
4. 未执行或受环境限制的场景已被明确说明：已说明

## 阻塞项

- `BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`
- `unresolvedRules.apiContract`
- `unresolvedRules.detailNavigation`
- 缺少联调环境地址、鉴权信息与稳定数据准备方式

## 后续执行入口

当以下条件补齐后，才能把本步骤从“执行记录”推进到“真实联调结果”：

1. 提供真实商品对比页面文件，并能被 `page-discovery.ts` 唯一发现。
2. 页面导出与当前测试文件匹配的联调/页面测试适配器。
3. 提供真实或集成环境接口地址、鉴权方式与可复现数据。
4. 明确真实接口字段与 `tests/fixtures/compare` 的映射关系。

在这些条件满足前，本步骤正确结论应保持为：已完成联调执行准备与回归范围核对，但真实联调未执行，不能视为通过。
