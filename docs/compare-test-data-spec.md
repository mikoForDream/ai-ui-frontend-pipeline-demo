# 商品对比测试数据规范

任务ID: compare_test_010
当前步骤: 2 - 整理商品对比测试数据与接口返回样本

## 目标

为商品对比页面后续页面级测试、接口联调用例与回归验证提供统一数据源，覆盖以下分类：

- 正常商品
- 重复商品
- 超上限商品
- 缺字段商品
- 无库存商品
- 接口成功
- 空数组
- 字段缺失
- 500 错误
- 超时

## 数据字段约束

测试数据按以下结构组织，字段命名保持稳定，可同时用于页面测试与接口 Mock：

### 商品对象 ProductCompareItem

- `id`: 商品ID，字符串，必填
- `name`: 商品名称，字符串，必填；缺字段场景允许省略
- `image`: 商品图片地址，字符串，必填；缺字段场景允许省略
- `price`: 价格信息对象，可空
  - `amount`: 数值或 `null`
  - `currency`: 货币代码，默认 `CNY`
  - `display`: 页面展示文案，允许为 `暂无报价`
- `stock`: 库存信息对象，可空
  - `available`: 数值或 `null`
  - `status`: `in_stock` | `out_of_stock` | `unknown`
  - `display`: 页面展示文案，允许为 `无库存` 或 `库存未知`
- `specs`: 规格属性数组
  - `key`: 属性键
  - `label`: 属性名称
  - `value`: 属性值
  - `isDifferent`: 是否差异项
- `detail`: 详情跳转信息对象
  - `productId`: 商品ID
  - `href`: 详情链接
  - `route`: 路由参数对象，可选
- `meta`: 辅助标记对象
  - `category`: 数据分类
  - `isDuplicateCandidate`: 是否用于重复校验
  - `isOverLimitCandidate`: 是否用于超上限场景
  - `missingFields`: 缺失字段列表

### 接口响应结构 CompareApiResponse

- `code`: 状态码，成功为 `0`
- `message`: 响应消息
- `requestId`: 请求ID
- `data`: 数据对象，可为 `null`
  - `compareLimit`: 最大对比数量
  - `items`: 商品数组
  - `summary`: 汇总对象
    - `total`: 当前对比数量
    - `canAddMore`: 是否可继续添加

### 错误响应结构 CompareApiErrorResponse

- `code`: 非 0 状态码
- `message`: 错误信息
- `requestId`: 请求ID
- `error`: 错误对象
  - `type`: `server_error` | `timeout` | `validation_error`
  - `retryable`: 是否可重试
  - `detail`: 详细错误说明

## 场景映射

### 页面级测试复用场景

- 添加 2 至 4 个不同商品：使用 `normalCompareSet`
- 重复添加同一商品：使用 `duplicateAttempt`
- 添加超过上限：使用 `overLimitAttempt`
- 移除后刷新：使用 `postRemoveCompareSet`
- 差异高亮：使用 `normalCompareSet` 中 `specs.isDifferent`
- 价格缺失与库存为 0：使用 `missingFieldProduct` 与 `outOfStockProduct`
- 空状态：使用 `emptyCompareResponse`
- 异常与超时：使用 `serverErrorResponse`、`timeoutErrorResponse`
- 详情跳转：使用每个商品的 `detail.productId`、`detail.href`、`detail.route`

### 联调用例复用场景

- 查询对比列表：`successCompareResponse`
- 重复商品拦截：`duplicateAddResponse`
- 超上限拦截：`overLimitAddResponse`
- 移除后重绘：`postRemoveRefreshResponse`
- 空数组：`emptyCompareResponse`
- 字段缺失：`missingFieldCompareResponse`
- 500 错误：`serverErrorResponse`
- 超时：`timeoutErrorResponse`

## 详情跳转字段检查

当前整理的数据中，正常商品、无库存商品、超上限候选商品均具备以下字段：

- `detail.productId`
- `detail.href`
- `detail.route.productId`

缺字段商品刻意缺失以下字段，用于容错验证：

- `name`
- `image`
- `detail.href`

该缺失已在 `meta.missingFields` 中显式记录，便于后续测试在断言中区分“预期缺失”与“脏数据”。

## 验收对应关系

- 已完成正常、重复、超上限、缺字段、无库存分类整理：见 `tests/fixtures/compare/compare-data.ts`
- 已覆盖成功、空数组、字段缺失、500错误、超时接口样本：见 `tests/fixtures/compare/compare-api-samples.ts`
- 字段与接口结构一致：见 `tests/fixtures/compare/compare-types.ts`
- 详情跳转字段具备或已记录缺失：见本说明与 `missingFields` 标记
