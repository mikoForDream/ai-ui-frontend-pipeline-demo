import {
  compareLimit,
  duplicateAttempt,
  missingFieldProduct,
  normalCompareSet,
  outOfStockProduct,
  postRemoveCompareSet,
} from './compare-data';
import type { CompareApiErrorResponse, CompareApiResponse } from './compare-types';

export const successCompareResponse: CompareApiResponse = {
  code: 0,
  message: 'ok',
  requestId: 'req-compare-success-001',
  data: {
    compareLimit,
    items: normalCompareSet,
    summary: {
      total: normalCompareSet.length,
      canAddMore: false,
    },
  },
};

export const emptyCompareResponse: CompareApiResponse = {
  code: 0,
  message: 'ok',
  requestId: 'req-compare-empty-001',
  data: {
    compareLimit,
    items: [],
    summary: {
      total: 0,
      canAddMore: true,
    },
  },
};

export const missingFieldCompareResponse: CompareApiResponse = {
  code: 0,
  message: 'partial data',
  requestId: 'req-compare-partial-001',
  data: {
    compareLimit,
    items: [missingFieldProduct, outOfStockProduct],
    summary: {
      total: 2,
      canAddMore: true,
    },
  },
};

export const duplicateAddResponse: CompareApiErrorResponse = {
  code: 40901,
  message: '商品已在对比列表中',
  requestId: 'req-compare-duplicate-001',
  error: {
    type: 'validation_error',
    retryable: false,
    detail: `duplicate product: ${duplicateAttempt.nextItem.id}`,
  },
};

export const overLimitAddResponse: CompareApiErrorResponse = {
  code: 40902,
  message: '对比商品数量已达上限',
  requestId: 'req-compare-limit-001',
  error: {
    type: 'validation_error',
    retryable: false,
    detail: `compare limit is ${compareLimit}`,
  },
};

export const postRemoveRefreshResponse: CompareApiResponse = {
  code: 0,
  message: 'ok',
  requestId: 'req-compare-remove-refresh-001',
  data: {
    compareLimit,
    items: postRemoveCompareSet,
    summary: {
      total: postRemoveCompareSet.length,
      canAddMore: true,
    },
  },
};

export const serverErrorResponse: CompareApiErrorResponse = {
  code: 50000,
  message: '服务器异常',
  requestId: 'req-compare-server-error-001',
  error: {
    type: 'server_error',
    retryable: true,
    detail: 'compare service internal error',
  },
};

export const timeoutErrorResponse: CompareApiErrorResponse = {
  code: 50408,
  message: '请求超时',
  requestId: 'req-compare-timeout-001',
  error: {
    type: 'timeout',
    retryable: true,
    detail: 'compare request timeout after 8000ms',
  },
};

export const compareApiSamples = {
  successCompareResponse,
  emptyCompareResponse,
  missingFieldCompareResponse,
  duplicateAddResponse,
  overLimitAddResponse,
  postRemoveRefreshResponse,
  serverErrorResponse,
  timeoutErrorResponse,
};
