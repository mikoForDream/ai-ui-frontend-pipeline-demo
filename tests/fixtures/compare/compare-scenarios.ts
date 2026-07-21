import {
  duplicateAttempt,
  missingPriceProduct,
  normalCompareSet,
  outOfStockProduct,
  overLimitAttempt,
  postRemoveCompareSet,
  scenarioConfig,
} from './compare-data';
import {
  emptyCompareResponse,
  missingPriceCompareResponse,
  outOfStockCompareResponse,
  serverErrorResponse,
  successCompareResponse,
  timeoutErrorResponse,
} from './compare-api-samples';
import { unresolvedRules } from './compare-types';

export const compareScenarioChecklist = [
  {
    id: 'compare-add-normal',
    category: 'main-flow',
    input: normalCompareSet.slice(0, 3).map((item) => item.fixtureId),
    sample: successCompareResponse,
    assertions: ['列表展示输入商品', '字段缺失时页面不崩溃', '差异规则确认后再断言高亮'],
    blockedBy: [unresolvedRules.differenceCalculation],
  },
  {
    id: 'compare-add-duplicate',
    category: 'validation',
    input: duplicateAttempt,
    assertions: ['按已确认的判重规则拦截重复项', '列表不新增重复商品'],
    blockedBy: [unresolvedRules.duplicateIdentity],
  },
  {
    id: 'compare-add-over-limit',
    category: 'validation',
    input: overLimitAttempt,
    testParameters: { compareLimit: scenarioConfig.testOnlyLimit },
    assertions: ['达到场景参数上限时拒绝新增', '现有对比数据保持不变'],
    blockedBy: [unresolvedRules.compareLimit],
  },
  {
    id: 'compare-remove-refresh',
    category: 'main-flow',
    input: postRemoveCompareSet.map((item) => item.fixtureId),
    assertions: ['移除后列表刷新', '剩余数据稳定'],
  },
  {
    id: 'compare-missing-price',
    category: 'edge-case',
    input: missingPriceProduct.fixtureId,
    sample: missingPriceCompareResponse,
    assertions: ['价格缺失时按确认后的规则降级', '页面不渲染 undefined 或抛错'],
    blockedBy: [unresolvedRules.missingPriceCopy],
  },
  {
    id: 'compare-out-of-stock',
    category: 'edge-case',
    input: outOfStockProduct.fixtureId,
    sample: outOfStockCompareResponse,
    assertions: ['库存为 0 时按确认后的规则展示', '该状态不与价格缺失混为同一输入'],
    blockedBy: [unresolvedRules.missingStockCopy],
  },
  {
    id: 'compare-detail-navigation',
    category: 'navigation',
    input: normalCompareSet[0].detailCandidate,
    assertions: ['真实跳转字段确认后可到达对应商品详情'],
    blockedBy: [unresolvedRules.detailNavigation],
  },
  {
    id: 'compare-empty-state',
    category: 'state',
    input: [],
    sample: emptyCompareResponse,
    assertions: ['展示空状态', '页面无报错'],
  },
  {
    id: 'compare-server-error',
    category: 'state',
    input: null,
    sample: serverErrorResponse,
    assertions: ['展示错误状态', '按确认后的策略决定是否允许重试'],
  },
  {
    id: 'compare-timeout-retry',
    category: 'state',
    input: null,
    sample: timeoutErrorResponse,
    assertions: ['超时状态可被识别', '重试策略确认后补充交互断言'],
  },
];
