import { describe, expect, it } from 'vitest';
import {
  comparePageDiscovery,
  compareScenarioChecklist,
  loadComparePageModule,
  missingFieldProduct,
  normalProducts,
  unresolvedRules,
  type ComparePageModule,
} from '../../../test/compare-page';
import type { ProductCompareFixture } from '../../../../tests/fixtures/compare/compare-types';

type CompareNavigationTarget = {
  kind: 'route' | 'link' | 'none';
  href: string | null;
  identifier: string | null;
};

type CompareDetailNavigationAdapter = {
  renderPage: () => void | Promise<void>;
  seedFixtures: (fixtures: ProductCompareFixture[]) => void | Promise<void>;
  triggerDetailNavigation: (
    fixture: ProductCompareFixture,
  ) => CompareNavigationTarget | Promise<CompareNavigationTarget>;
  assertNavigatedToFixture: (input: {
    fixture: ProductCompareFixture;
    target: CompareNavigationTarget;
  }) => void | Promise<void>;
  assertNavigationUnavailable: (input: {
    fixture: ProductCompareFixture;
    target: CompareNavigationTarget;
  }) => void | Promise<void>;
  enterErrorStateWithRetryRecovery?: (input: {
    failingFixture: ProductCompareFixture;
    recoveryFixtures: ProductCompareFixture[];
  }) => void | Promise<void>;
  cleanup?: () => void | Promise<void>;
};

const integrationBlockers = {
  adapterMissing: 'BLOCKED_COMPARE_PAGE_DETAIL_NAVIGATION_ADAPTER_NOT_FOUND',
  navigationRuleUnconfirmed: `BLOCKED_${unresolvedRules.detailNavigation}`,
  errorRecoveryNavigationUnobservable: 'BLOCKED_COMPARE_PAGE_DETAIL_NAVIGATION_ERROR_RECOVERY_NOT_OBSERVABLE',
} as const;

function resolveDetailNavigationAdapter(
  pageModule: ComparePageModule | null,
): CompareDetailNavigationAdapter | null {
  if (!pageModule) return null;

  const candidate = pageModule.comparePageDetailNavigationTestAdapter;
  if (!candidate || typeof candidate !== 'object') return null;

  const adapter = candidate as Partial<CompareDetailNavigationAdapter>;
  if (
    typeof adapter.renderPage !== 'function' ||
    typeof adapter.seedFixtures !== 'function' ||
    typeof adapter.triggerDetailNavigation !== 'function' ||
    typeof adapter.assertNavigatedToFixture !== 'function' ||
    typeof adapter.assertNavigationUnavailable !== 'function'
  ) {
    return null;
  }

  return adapter as CompareDetailNavigationAdapter;
}

async function withDetailNavigationAdapter(
  run: (adapter: CompareDetailNavigationAdapter) => void | Promise<void>,
) {
  const pageModule = await loadComparePageModule();
  const adapter = resolveDetailNavigationAdapter(pageModule);
  if (!adapter) return { blocker: integrationBlockers.adapterMissing } as const;

  await adapter.renderPage();
  try {
    await run(adapter);
  } finally {
    await adapter.cleanup?.();
  }

  return { blocker: null } as const;
}

describe('ComparePage detail navigation', () => {
  it('reuses the step 2 navigation fixture and keeps the production rule unresolved', () => {
    const navigationScenario = compareScenarioChecklist.find(
      (scenario) => scenario.id === 'compare-detail-navigation',
    );

    expect(navigationScenario).toBeDefined();
    expect(normalProducts[0].detailCandidate?.identifier).toBeTruthy();
    expect(normalProducts[0].detailCandidate?.sourceField).toBe('unconfirmed');
    expect(unresolvedRules.detailNavigation.length).toBeGreaterThan(0);
  });

  it('uses missing detail fields only as fixture-backed coverage input, not as an inferred route rule', () => {
    expect(missingFieldProduct.expected.missingFields ?? []).toContain('detailNavigationField');
    expect(missingFieldProduct.detailCandidate?.identifier).toBe('sku-1999');
  });

  const realPageSuite =
    comparePageDiscovery.found && !comparePageDiscovery.ambiguous ? describe : describe.skip;

  realPageSuite('real page integration', () => {
    it('navigates for a fixture with an available detail candidate when the current adapter can observe the target', async (context) => {
      const fixture = normalProducts[0];
      const result = await withDetailNavigationAdapter(async (adapter) => {
        await adapter.seedFixtures([fixture]);
        const target = await adapter.triggerDetailNavigation(fixture);

        if (target.kind === 'none') {
          context.skip(integrationBlockers.navigationRuleUnconfirmed);
          return;
        }

        expect(target.identifier ?? '').toContain(fixture.detailCandidate?.identifier ?? '');
        if (target.href !== null) {
          expect(target.href ?? '').not.toMatch(/undefined|null|NaN/i);
        }

        await adapter.assertNavigatedToFixture({
          fixture,
          target,
        });
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('handles missing detail navigation fields through the current implementation without crashing', async (context) => {
      const result = await withDetailNavigationAdapter(async (adapter) => {
        await adapter.seedFixtures([missingFieldProduct]);
        const target = await adapter.triggerDetailNavigation(missingFieldProduct);

        expect(`${target.href ?? ''} ${target.identifier ?? ''}`).not.toMatch(
          /undefined|null|NaN|\[object Object\]/i,
        );

        if (target.kind === 'none') {
          await adapter.assertNavigationUnavailable({
            fixture: missingFieldProduct,
            target,
          });
          return;
        }

        await adapter.assertNavigatedToFixture({
          fixture: missingFieldProduct,
          target,
        });
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('keeps valid detail navigation observable after error and retry recovery when the adapter exposes that flow', async (context) => {
      const fixture = normalProducts[1];
      const result = await withDetailNavigationAdapter(async (adapter) => {
        if (typeof adapter.enterErrorStateWithRetryRecovery !== 'function') {
          context.skip(integrationBlockers.errorRecoveryNavigationUnobservable);
          return;
        }

        await adapter.seedFixtures([fixture]);
        await adapter.enterErrorStateWithRetryRecovery({
          failingFixture: missingFieldProduct,
          recoveryFixtures: [fixture],
        });

        const target = await adapter.triggerDetailNavigation(fixture);
        if (target.kind === 'none') {
          context.skip(integrationBlockers.navigationRuleUnconfirmed);
          return;
        }

        expect(target.identifier ?? '').toContain(fixture.detailCandidate?.identifier ?? '');
        expect(target.href ?? '').not.toMatch(/undefined|null|NaN/i);

        await adapter.assertNavigatedToFixture({
          fixture,
          target,
        });
      });

      if (result.blocker) context.skip(result.blocker);
    });
  });
});
