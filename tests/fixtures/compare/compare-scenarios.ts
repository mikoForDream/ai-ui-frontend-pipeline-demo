import {
  duplicateAttempt,
  normalCompareSet,
  outOfStockProduct,
  postRemoveCompareSet,
} from './compare-data';
import {
  duplicateAddResponse,
  emptyCompareResponse,
  missingFieldCompareResponse,
  overLimitAddResponse,
  postRemoveRefreshResponse,
  serverErrorResponse,
  successCompareResponse,
  timeoutErrorResponse,
} from './compare-api-samples';

export const compareScenarioChecklist = [
  {
    id: 'compare-add-normal',
    category: 'main-flow',
    input: normalCompareSet.slice(0, 3).map((item) => item.id),
    expectedApi: successCompareResponse.requestId,
    assertions: ['列表展示3个商品', '价格库存字段完整', '差异项高亮可识别'],
  },
  {
    id: 'compare-add-duplicate',
    category: 'validation',
    input: {
      existing: duplicateAttempt.existingItems.map((item) => item.id),
      next: duplicateAttempt.nextItem.id,
    },
    expectedApi: duplicateAddResponse.requestId,
    assertions: ['前端提示重复添加', '列表不新增重复商品'],
  },
  {
    id: 'compare-add-over-limit',
    category: 'validation',
    input: normalCompareSet.map((item) => item.id),
    expectedApi: overLimitAddResponse.requestId,
    assertions: ['提示达到上限', '按钮状态受限', '现有对比数据保持不变'],
  },
  {
    id: 'compare-remove-refresh',
    category: 'main-flow',
    input: postRemoveCompareSet.map((item) => item.id),
    expectedApi: postRemoveRefreshResponse.requestId,
    assertions: ['移除后列表刷新', '剩余顺序稳定', '可继续添加商品'],
  },
  {
    id: 'compare-price-stock-fallback',
    category: 'edge-case',
    input: outOfStockProduct.id,
    expectedApi: missingFieldCompareResponse.requestId,
    assertions: ['库存0显示无库存', '缺失价格显示暂无报价', '不渲染脏数据'],
  },
  {
    id: 'compare-empty-state',
    category: 'state',
    input: [],
    expectedApi: emptyCompareResponse.requestId,
    assertions: ['展示空状态', '保留添加入口', '页面无报错'],
  },
  {
    id: 'compare-server-error',
    category: 'state',
    input: null,
    expectedApi: serverErrorResponse.requestId,
    assertions: ['展示错误提示', '允许重试'],
  },
  {
    id: 'compare-timeout-retry',
    category: 'state',
    input: null,
    expectedApi: timeoutErrorResponse.requestId,
    assertions: ['超时后可重试', '恢复加载后不影响详情跳转'],
  },
];
