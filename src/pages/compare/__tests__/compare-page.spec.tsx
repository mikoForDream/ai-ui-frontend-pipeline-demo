import { describe, expect, it } from 'vitest';
import {
  compareApiSamples,
  comparePageDiscovery,
  compareScenarioChecklist,
  loadComparePageModule,
  normalProducts,
  unresolvedRules,
} from '../../../test/compare-page';

describe('ComparePage test scaffold', () => {
  describe('fixture and environment baseline', () => {
    it('reuses the step 2 fixtures instead of redefining product data', () => {
      expect(normalProducts).toHaveLength(3);
      expect(compareApiSamples.successCompareResponse.fixtureItems).toEqual(normalProducts);
      expect(compareScenarioChecklist.length).toBeGreaterThan(0);
    });

    it('keeps unresolved business rules visible', () => {
      expect(Object.keys(unresolvedRules)).toEqual(
        expect.arrayContaining([
          'compareLimit',
          'duplicateIdentity',
          'differenceCalculation',
          'detailNavigation',
          'apiContract',
        ]),
      );
    });

    it('reports page discovery without substituting a test harness', () => {
      if (comparePageDiscovery.found) {
        expect(comparePageDiscovery.candidates.length).toBeGreaterThan(0);
      } else {
        expect(comparePageDiscovery.blocker).toBe('BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND');
        expect(comparePageDiscovery.candidates).toEqual([]);
      }
    });
  });

  const realPageSuite =
    comparePageDiscovery.found && !comparePageDiscovery.ambiguous ? describe : describe.skip;

  realPageSuite('real page integration', () => {
    it('loads exactly one real ComparePage module', async () => {
      const pageModule = await loadComparePageModule();
      expect(pageModule).not.toBeNull();
      expect(pageModule && Object.keys(pageModule).length).toBeGreaterThan(0);
    });
  });
});
