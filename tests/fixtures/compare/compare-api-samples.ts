import {
  duplicateAttempt,
  missingFieldProduct,
  missingPriceProduct,
  normalProducts,
  outOfStockProduct,
  overLimitAttempt,
  postRemoveCompareSet,
} from './compare-data';
import type { CompareApiSample } from './compare-types';

// payload 中的字段仅为原始样本占位；真实接口明确后由适配器负责映射。
export const successCompareResponse: CompareApiSample = {
  transport: 'success',
  httpStatus: 200,
  payload: { candidateItems: normalProducts.map((item) => item.source) },
  fixtureItems: normalProducts,
  expected: { kind: 'success' },
};

export const emptyCompareResponse: CompareApiSample = {
  transport: 'success',
  httpStatus: 200,
  payload: { candidateItems: [] },
  fixtureItems: [],
  expected: { kind: 'empty' },
};

export const missingFieldCompareResponse: CompareApiSample = {
  transport: 'success',
  httpStatus: 200,
  payload: { candidateItems: [missingFieldProduct.source] },
  fixtureItems: [missingFieldProduct],
  expected: { kind: 'partial' },
};

export const missingPriceCompareResponse: CompareApiSample = {
  transport: 'success',
  httpStatus: 200,
  payload: { candidateItems: [missingPriceProduct.source] },
  fixtureItems: [missingPriceProduct],
  expected: { kind: 'partial' },
};

export const outOfStockCompareResponse: CompareApiSample = {
  transport: 'success',
  httpStatus: 200,
  payload: { candidateItems: [outOfStockProduct.source] },
  fixtureItems: [outOfStockProduct],
  expected: { kind: 'partial' },
};

export const duplicateAddResponse: CompareApiSample = {
  transport: 'success',
  payload: { candidateRejectedItem: duplicateAttempt.nextItem.source, rule: 'unconfirmed' },
  expected: { kind: 'duplicate_rejected' },
};

export const overLimitAddResponse: CompareApiSample = {
  transport: 'success',
  payload: { candidateRejectedItem: overLimitAttempt.nextItem.source, testOnlyLimit: overLimitAttempt.limit },
  expected: { kind: 'limit_rejected' },
};

export const postRemoveRefreshResponse: CompareApiSample = {
  transport: 'success',
  httpStatus: 200,
  payload: { candidateItems: postRemoveCompareSet.map((item) => item.source) },
  fixtureItems: postRemoveCompareSet,
  expected: { kind: 'success' },
};

export const serverErrorResponse: CompareApiSample = {
  transport: 'http_error',
  httpStatus: 500,
  payload: null,
  expected: { kind: 'server_error', retryable: true },
};

export const timeoutErrorResponse: CompareApiSample = {
  transport: 'timeout',
  expected: { kind: 'timeout', retryable: true },
};

export const compareApiSamples = {
  successCompareResponse,
  emptyCompareResponse,
  missingFieldCompareResponse,
  missingPriceCompareResponse,
  outOfStockCompareResponse,
  duplicateAddResponse,
  overLimitAddResponse,
  postRemoveRefreshResponse,
  serverErrorResponse,
  timeoutErrorResponse,
};
