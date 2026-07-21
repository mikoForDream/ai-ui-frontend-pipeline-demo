import type {
  CompareScenarioConfig,
  ProductCompareFixture,
} from './compare-types';

// 仅用于构造边界测试，不代表生产规则。
export const scenarioConfig: CompareScenarioConfig = {
  testOnlyLimit: 4,
  duplicateMatcher: 'fixture_id_for_test_only',
};

const product = (
  fixtureId: string,
  source: Record<string, unknown>,
  specs: ProductCompareFixture['specs'],
): ProductCompareFixture => ({
  fixtureId,
  source,
  specs,
  expected: { category: 'normal' },
  detailCandidate: {
    identifier: fixtureId,
    sourceField: 'unconfirmed',
  },
});

export const normalProducts: ProductCompareFixture[] = [
  product(
    'fixture-product-1001',
    { candidateId: 'sku-1001', candidateName: 'AeroBook 14 2024', candidatePrice: 5999, candidateStock: 32 },
    [
      { key: 'cpu', label: '处理器', value: 'Intel Core i5' },
      { key: 'memory', label: '内存', value: '16GB' },
      { key: 'storage', label: '存储', value: '512GB SSD' },
    ],
  ),
  product(
    'fixture-product-1002',
    { candidateId: 'sku-1002', candidateName: 'AeroBook 14 Pro', candidatePrice: 6999, candidateStock: 18 },
    [
      { key: 'cpu', label: '处理器', value: 'Intel Core i7' },
      { key: 'memory', label: '内存', value: '16GB' },
      { key: 'storage', label: '存储', value: '1TB SSD' },
    ],
  ),
  product(
    'fixture-product-1003',
    { candidateId: 'sku-1003', candidateName: 'CloudPad Air 13', candidatePrice: 5499, candidateStock: 9 },
    [
      { key: 'cpu', label: '处理器', value: 'Snapdragon X Elite' },
      { key: 'memory', label: '内存', value: '16GB' },
      { key: 'storage', label: '存储', value: '512GB SSD' },
    ],
  ),
];

export const duplicateProduct: ProductCompareFixture = {
  ...normalProducts[0],
  expected: { category: 'duplicate_candidate' },
};

export const overLimitProduct: ProductCompareFixture = {
  ...product(
    'fixture-product-1004',
    { candidateId: 'sku-1004', candidateName: 'VisionNote Max 16', candidatePrice: 7999, candidateStock: 6 },
    [
      { key: 'cpu', label: '处理器', value: 'AMD Ryzen 9' },
      { key: 'memory', label: '内存', value: '32GB' },
    ],
  ),
  expected: { category: 'over_limit_candidate' },
};

export const missingFieldProduct: ProductCompareFixture = {
  fixtureId: 'fixture-product-missing-fields',
  source: { candidateId: 'sku-1999', candidatePrice: 3299, candidateStock: 7 },
  specs: [{ key: 'screen', label: '屏幕', value: null }],
  expected: {
    category: 'missing_fields',
    missingFields: ['candidateName', 'candidateImage', 'detailNavigationField'],
    stockState: 'available',
  },
  detailCandidate: { identifier: 'sku-1999', sourceField: 'unconfirmed' },
};

export const missingPriceProduct: ProductCompareFixture = {
  ...product(
    'fixture-product-missing-price',
    { candidateId: 'sku-2000', candidateName: 'LiteBook 13', candidatePrice: null, candidateStock: 7 },
    [{ key: 'memory', label: '内存', value: '16GB' }],
  ),
  expected: {
    category: 'missing_price',
    missingFields: ['candidatePrice'],
    stockState: 'available',
  },
};

export const outOfStockProduct: ProductCompareFixture = {
  ...product(
    'fixture-product-out-of-stock',
    { candidateId: 'sku-2001', candidateName: 'PocketStation Mini', candidatePrice: 1999, candidateStock: 0 },
    [{ key: 'storage', label: '存储', value: '256GB' }],
  ),
  expected: { category: 'out_of_stock', stockState: 'unavailable' },
};

export const normalCompareSet = [...normalProducts, overLimitProduct];

export const duplicateAttempt = {
  existingItems: [normalProducts[0], normalProducts[1]],
  nextItem: duplicateProduct,
  matcherStatus: 'unconfirmed' as const,
};

export const overLimitAttempt = {
  existingItems: normalCompareSet,
  nextItem: outOfStockProduct,
  limit: scenarioConfig.testOnlyLimit,
  limitSource: 'test_parameter_only' as const,
};

export const postRemoveCompareSet = [normalProducts[1], normalProducts[2], overLimitProduct];
