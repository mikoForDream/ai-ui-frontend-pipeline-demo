import { describe, expect, it } from 'vitest';
import {
  comparePageDiscovery,
  duplicateAttempt,
  loadComparePageModule,
  overLimitAttempt,
  scenarioConfig,
  unresolvedRules,
  type ComparePageModule,
} from '../../../test/compare-page';
import type { ProductCompareFixture } from '../../../../tests/fixtures/compare/compare-types';

type BoundaryAttemptOutcome = {
  accepted: boolean;
  kind: 'duplicate' | 'limit' | 'other';
};

type ComparePageBoundaryTestAdapter = {
  renderPage: () => void | Promise<void>;
  seedFixtures: (fixtures: ProductCompareFixture[]) => void | Promise<void>;
  attemptAdd: (fixture: ProductCompareFixture) => BoundaryAttemptOutcome | Promise<BoundaryAttemptOutcome>;
  readFixtureIds: () => string[] | Promise<string[]>;
  assertDuplicateFeedback: (outcome: BoundaryAttemptOutcome) => void | Promise<void>;
  assertLimitFeedback: (
    outcome: BoundaryAttemptOutcome,
    input: { testOnlyLimit: number },
  ) => void | Promise<void>;
  cleanup?: () => void | Promise<void>;
};

const integrationBlockers = {
  adapterMissing: 'BLOCKED_COMPARE_PAGE_BOUNDARY_ADAPTER_NOT_FOUND',
} as const;

function resolveBoundaryAdapter(
  pageModule: ComparePageModule | null,
): ComparePageBoundaryTestAdapter | null {
  if (!pageModule) return null;

  const candidate = pageModule.comparePageBoundaryTestAdapter;
  if (!candidate || typeof candidate !== 'object') return null;

  const adapter = candidate as Partial<ComparePageBoundaryTestAdapter>;
  if (
    typeof adapter.renderPage !== 'function' ||
    typeof adapter.seedFixtures !== 'function' ||
    typeof adapter.attemptAdd !== 'function' ||
    typeof adapter.readFixtureIds !== 'function' ||
    typeof adapter.assertDuplicateFeedback !== 'function' ||
    typeof adapter.assertLimitFeedback !== 'function'
  ) {
    return null;
  }

  return adapter as ComparePageBoundaryTestAdapter;
}

async function withBoundaryAdapter(
  run: (adapter: ComparePageBoundaryTestAdapter) => void | Promise<void>,
) {
  const pageModule = await loadComparePageModule();
  const adapter = resolveBoundaryAdapter(pageModule);
  if (!adapter) return { blocker: integrationBlockers.adapterMissing } as const;

  await adapter.renderPage();
  try {
    await run(adapter);
  } finally {
    await adapter.cleanup?.();
  }

  return { blocker: null } as const;
}

describe('ComparePage duplicate and limit boundaries', () => {
  it('reuses the step 2 duplicate fixture instead of redefining products', () => {
    const existingIds = duplicateAttempt.existingItems.map((fixture) => fixture.fixtureId);
    expect(existingIds).toContain(duplicateAttempt.nextItem.fixtureId);
    expect(duplicateAttempt.matcherStatus).toBe('unconfirmed');
  });

  it('keeps duplicate identity as an unresolved production rule', () => {
    expect(scenarioConfig.duplicateMatcher).toBe('fixture_id_for_test_only');
    expect(unresolvedRules.duplicateIdentity.length).toBeGreaterThan(0);
  });

  it('uses the fixture limit only as a boundary-test parameter', () => {
    expect(overLimitAttempt.limitSource).toBe('test_parameter_only');
    expect(overLimitAttempt.limit).toBe(scenarioConfig.testOnlyLimit);
    expect(overLimitAttempt.existingItems).toHaveLength(overLimitAttempt.limit);
  });

  it('keeps the production compare limit unresolved', () => {
    expect(unresolvedRules.compareLimit.length).toBeGreaterThan(0);
  });

  const realPageSuite =
    comparePageDiscovery.found && !comparePageDiscovery.ambiguous ? describe : describe.skip;

  realPageSuite('real page integration', () => {
    it('rejects a duplicate attempt without duplicating the existing list', async (context) => {
      const result = await withBoundaryAdapter(async (adapter) => {
        await adapter.seedFixtures(duplicateAttempt.existingItems);
        const outcome = await adapter.attemptAdd(duplicateAttempt.nextItem);

        expect(outcome.accepted).toBe(false);
        expect(outcome.kind).toBe('duplicate');
        expect(await adapter.readFixtureIds()).toEqual(
          duplicateAttempt.existingItems.map((fixture) => fixture.fixtureId),
        );
        await adapter.assertDuplicateFeedback(outcome);
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('rejects the test-only over-limit attempt and preserves existing data', async (context) => {
      const result = await withBoundaryAdapter(async (adapter) => {
        await adapter.seedFixtures(overLimitAttempt.existingItems);
        const outcome = await adapter.attemptAdd(overLimitAttempt.nextItem);

        expect(outcome.accepted).toBe(false);
        expect(outcome.kind).toBe('limit');
        expect(await adapter.readFixtureIds()).toEqual(
          overLimitAttempt.existingItems.map((fixture) => fixture.fixtureId),
        );
        await adapter.assertLimitFeedback(outcome, {
          testOnlyLimit: overLimitAttempt.limit,
        });
      });

      if (result.blocker) context.skip(result.blocker);
    });
  });
});
