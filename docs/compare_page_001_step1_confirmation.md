# compare_page_001 / Step 1 商品对比页前期方案核对与缺失项确认

任务：`商品对比页面框架与基础交互开发`  
步骤：1 - 前期方案核对与缺失项确认

## 核对结论

已根据仓库当前真实文件核对商品对比页开发前置依赖。当前仓库具备 Vitest 测试基础设施、商品对比相关 fixtures、页面发现器与测试适配契约，但**尚未发现真实商品对比页面实现、真实路由配置、真实请求封装或接口类型定义**。因此本步骤只能输出已确认事实与缺失项清单，不能基于假设继续实现页面逻辑、路由跳转规则或接口字段映射。

## 已确认事实

### 1. 页面发现与页面注册方式

仓库当前通过 `src/test/compare-page/page-discovery.ts` 发现真实商品对比页，候选路径仅包括：

- `src/pages/**/ComparePage.{ts,tsx,js,jsx}`
- `src/pages/**/compare-page.{ts,tsx,js,jsx}`
- `src/pages/compare/index.{ts,tsx,js,jsx}`

这说明当前可验证的页面注册约束只有：

1. 商品对比页最终必须落在上述候选路径之一，才能被现有测试发现。
2. 当前仓库尚无真实页面文件被发现；`comparePageDiscovery` 在页面缺失时会给出 `BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND`。
3. 仓库未提供独立的页面路由总表、菜单注册表或页面元数据注册文件，至少在当前上下文中不可验证。

### 2. 测试与文件命名规则

已确认 `vitest.config.ts` 仅收集 `src/**/*.spec.{ts,tsx}`。

这对后续开发的直接影响是：

1. 若补充测试，只能使用 `.spec.ts` 或 `.spec.tsx`。
2. 现有对比页测试已经统一按 `src/pages/compare/__tests__/*.spec.tsx` 组织。
3. 不应新增 `.test.ts` 或 `.test.tsx` 文件。

### 3. 现有测试适配模式

当前仓库对商品对比页采用“真实页面模块 + 测试适配器导出”的模式。现有测试文件要求页面模块按场景导出以下适配器之一：

- `comparePageAddFlowTestAdapter`
- `comparePageBoundaryTestAdapter`
- `comparePageRemoveRefreshTestAdapter`
- `comparePageDifferenceHighlightTestAdapter`
- `comparePagePriceStockTestAdapter`
- `comparePageStateTestAdapter`
- `comparePageDetailNavigationTestAdapter`

这说明后续真实页面实现若要接入既有测试体系，应沿用页面模块导出测试适配器的模式，不能另起新的测试架构。

### 4. 已有本地状态形态只存在于任务说明与测试夹具，不存在真实生产定义

任务描述中建议页面维护 `compareSlots`，固定长度 4，元素结构建议为：

- `slotIndex`
- `status`
- `productId`
- `skuId`
- `image`
- `name`
- `price`
- `detailUrl`

但仓库当前**不存在可验证的生产状态定义文件**。当前能看到的只有测试 fixture 与测试场景：

- `tests/fixtures/compare/compare-data.ts`
- `tests/fixtures/compare/compare-api-samples.ts`
- `tests/fixtures/compare/compare-types.ts`

这些文件已明确声明：

1. `source` 字段只是原始样本容器，不是生产 DTO。
2. `payload` 只是样本占位，不是生产接口契约。
3. `scenarioConfig.testOnlyLimit = 4` 仅用于测试边界输入，不代表生产上限。
4. `detailCandidate.identifier` 与 `sourceField: 'unconfirmed'` 仅表示详情跳转字段仍未确认。

因此当前**不能把任务描述中的 `compareSlots` 结构直接认定为现有仓库生产状态模型**，只能作为待与真实页面容器对齐的目标草案。

### 5. 商品详情跳转字段当前未确认

`tests/fixtures/compare/compare-types.ts` 中明确存在：

- `unresolvedRules.detailNavigation = '待确认详情跳转字段、路由名称和参数来源'`

`tests/fixtures/compare/compare-data.ts` 中所有详情候选字段也都被标记为：

- `detailCandidate.identifier`
- `detailCandidate.sourceField: 'unconfirmed'`

因此当前只能确认：

1. 详情跳转需要某个商品标识字段。
2. 该字段可能是 `productId`、`skuId`、slug、链接或其他参数，但仓库未给出真实定义。
3. 不能在开发中自行决定“只传 productId”或“同时传 productId 与 skuId”。

### 6. 初始化商品 ID 来源与优先级当前未确认

任务说明提到初始化参数可能来自：

- 路由
- 缓存
- 外部页面回传

但当前仓库上下文中：

1. 没有真实 compare 页面容器。
2. 没有路由参数读取代码。
3. 没有缓存读写封装。
4. 没有跨页回传或弹层回传实现。

因此初始化商品 ID 来源以及优先级完全不可验证。

### 7. 添加商品入口形态当前未确认

任务说明允许两类入口：

- 打开选品弹层
- 跳转商品选择页

仓库当前未提供：

1. 商品选择页路由配置。
2. 选品弹层组件或入口契约。
3. 统一弹层管理方式。

因此只能确认“页面需要预留添加入口”，但**不能确认当前应该接弹层还是跳转页面**。

### 8. 真实接口请求封装规范与字段映射当前未确认

当前仓库中没有可验证的 compare 业务请求层文件，也没有 API type 定义文件。现有 fixtures 反复强调：

- `compare-api-samples.ts` 中 `payload` 字段仅为原始样本占位；真实接口明确后由适配器负责映射。
- `unresolvedRules.apiContract` 明确说明：`仓库暂无真实接口定义；当前样本不是生产接口契约`。

因此当前无法确认以下任何一项：

1. 商品基础信息批量查询接口的路径、方法、请求参数。
2. 商品选择接口的路径、方法、筛选参数。
3. 返回字段是否是 `productId`、`skuId`、`name`、`image`、`price` 或其他命名。
4. 错误码语义。
5. 空数据返回结构。
6. 请求封装是基于 `fetch`、`axios` 还是仓库自定义 request client。

## compareSlots 字段映射核对

当前只能得到“测试夹具候选字段”与“页面建议字段”的低置信度对应关系，不能视为生产映射结论：

| compareSlots 建议字段 | 当前仓库可见候选来源 | 结论 |
| --- | --- | --- |
| `slotIndex` | 无真实接口来源 | 仅页面本地 UI 字段，可后续页面实现自管 |
| `status` | 无真实接口来源 | 仅页面本地 UI 状态，可后续页面实现自管 |
| `productId` | fixtures 中无确认字段 | 未确认 |
| `skuId` | `source.candidateId` 看起来像候选，但未确认 | 待确认，不能直接定稿 |
| `image` | `missingFields` 中出现 `candidateImage`，但 normal fixture 未提供 | 待确认 |
| `name` | `source.candidateName` | 仅测试候选，不是生产确认字段 |
| `price` | `source.candidatePrice` | 仅测试候选，不是生产确认字段 |
| `detailUrl` | fixtures 未提供真实 URL；仅有 `detailNavigationField` 缺失提示 | 未确认 |

结论：`compareSlots` 所需字段目前**无法与现有真实类型定义或真实接口字段完成确认映射**；只能确认测试样本里存在若干候选字段名，且这些字段被明确标记为未确认。

## 缺失项确认清单

以下项目在进入页面开发前必须补齐，否则后续实现只能建立在假设上：

1. **商品对比页真实路由入口**
   - 页面文件实际路径
   - 路由注册文件位置
   - 页面访问 URL

2. **商品详情页真实路由规则**
   - 路由名称或 URL 模式
   - 跳转依赖字段是 `productId`、`skuId`、slug 还是其他参数
   - 缺字段时的降级方式

3. **初始化商品 ID 的真实来源与优先级**
   - 是否支持路由 query/path
   - 是否支持缓存
   - 是否支持外部页面或弹层回传
   - 多来源同时存在时的优先级

4. **添加商品入口形态**
   - 当前应接商品选择页还是选品弹层
   - 若为弹层，复用哪套弹层管理模式
   - 若为跳转，回传链路如何定义

5. **商品基础信息批量查询接口契约**
   - 请求路径、方法、参数结构
   - 批量入参字段名
   - 返回列表字段定义
   - 空数据结构
   - 错误码与可重试语义

6. **商品选择接口契约**
   - 请求路径、方法、筛选条件
   - 列表字段定义
   - 分页结构
   - 去重或禁选标识字段

7. **请求封装规范**
   - compare 相关 API 应放在哪个目录
   - 使用哪个 request client / hook / service pattern
   - 类型定义文件放置位置

8. **页面本地状态模式**
   - 是页面内 `useState`、`useReducer`，还是仓库已有 store 模式
   - compareSlots 最终生产字段结构
   - filled/empty/error 等槽位状态是否已有统一定义

## 当前步骤验收判断

对照本步骤验收标准，当前可成立的结论如下：

1. 页面开发依赖的测试发现方式、测试适配模式和 fixtures 约束已核对完成。
2. 真实路由、真实接口、回传方式和生产字段映射**未核对完成**，因为仓库当前不存在相应实现或定义文件。
3. `compareSlots` 字段与现有真实类型定义/真实接口字段**尚不能对应**；只能列出测试候选字段。
4. 缺失项已形成明确清单，且未用假设替代实现。

## 建议的后续前置动作

1. 先补充真实商品对比页文件，使其落入 `src/test/compare-page/page-discovery.ts` 可发现的路径。
2. 明确 compare 页面与商品详情页路由配置文件。
3. 提供 compare 相关真实 API 封装或 OpenAPI/接口文档。
4. 明确添加入口是弹层还是跳转页，并给出回传契约。
5. 在以上信息明确后，再进入页面骨架开发与测试适配器接入。
