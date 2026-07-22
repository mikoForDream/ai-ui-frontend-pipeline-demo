export {
  comparePageDiscovery,
  loadComparePageModule,
  type ComparePageModule,
} from './page-discovery';

export {
  normalProducts,
  normalCompareSet,
  postRemoveCompareSet,
  missingFieldProduct,
  missingPriceProduct,
  outOfStockProduct,
  duplicateAttempt,
  overLimitAttempt,
  scenarioConfig,
} from '../../../tests/fixtures/compare/compare-data';

export {
  compareApiSamples,
  emptyCompareResponse,
  missingFieldCompareResponse,
  missingPriceCompareResponse,
  outOfStockCompareResponse,
  postRemoveRefreshResponse,
  serverErrorResponse,
  timeoutErrorResponse,
} from '../../../tests/fixtures/compare/compare-api-samples';
export { compareScenarioChecklist } from '../../../tests/fixtures/compare/compare-scenarios';
export { unresolvedRules } from '../../../tests/fixtures/compare/compare-types';
