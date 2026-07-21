export type UnknownRecord = Record<string, unknown>;

export type CompareSpecFixture = {
  key: string;
  label: string;
  value: string | null;
};

export type ProductCompareFixture = {
  fixtureId: string;
  source: UnknownRecord;
  specs: CompareSpecFixture[];
  expected: {
    category: 'normal' | 'duplicate_candidate' | 'over_limit_candidate' | 'missing_fields' | 'missing_price' | 'out_of_stock';
    missingFields?: string[];
    stockState?: 'available' | 'unavailable' | 'unknown';
  };
  detailCandidate?: {
    identifier: string | null;
    sourceField: 'unconfirmed';
  };
};

export type CompareApiSample = {
  transport: 'success' | 'http_error' | 'timeout';
  httpStatus?: number;
  payload?: UnknownRecord | null;
  fixtureItems?: ProductCompareFixture[];
  expected: {
    kind: 'success' | 'empty' | 'partial' | 'duplicate_rejected' | 'limit_rejected' | 'server_error' | 'timeout';
    retryable?: boolean;
  };
};

export type UnresolvedRuleKey =
  | 'compareLimit'
  | 'duplicateIdentity'
  | 'differenceCalculation'
  | 'missingPriceCopy'
  | 'missingStockCopy'
  | 'detailNavigation'
  | 'apiContract';

export const unresolvedRules: Record<UnresolvedRuleKey, string> = {
  compareLimit: '待产品或接口定义确认最大对比数量及其来源',
  duplicateIdentity: '待确认按商品 ID、SKU、SPU 或组合字段判重',
  differenceCalculation: '待确认差异由接口返回还是前端计算，以及高亮规则',
  missingPriceCopy: '待确认价格缺失时的展示文案与交互',
  missingStockCopy: '待确认库存缺失或为零时的展示文案与交互',
  detailNavigation: '待确认详情跳转字段、路由名称和参数来源',
  apiContract: '仓库暂无真实接口定义；当前样本不是生产接口契约',
};

export type CompareScenarioConfig = {
  testOnlyLimit: number;
  duplicateMatcher: 'fixture_id_for_test_only';
};
