import { describe, expect, it } from 'vitest';
import {
  comparePageDiscovery,
  loadComparePageModule,
  postRemoveRefreshResponse,
  unresolvedRules,
  type ComparePageModule,
} from '../../../test/compare-page';
import { normalCompareSet, postRemoveCompareSet } from '../../../../tests/fixtures/compare/compare-data';
import type { ProductCompareFixture } from '../../../../tests/fixtures/compare/compare-types';

type RemoveRefreshState = {
  listedFixtureIds: string[];
  mainAreaFixtureIds: string[];
  removedFixtureId: string;
  staleFixtureIds: string[];
  addEntryAvailable: boolean;
  placeholderCount: number | null;
};

type ComparePageRemoveRefreshTestAdapter = {
  renderPage: () => void | Promise<void>;
  seedFixtures: (fixtures: ProductCompareFixture[]) => void | Promise<void>;
  removeFixture: (fixture: ProductCompareFixture) => void | Promise<void>;
  awaitRefreshSettled: () => void | Promise<void>;
  readState: () => RemoveRefreshState | Promise<RemoveRefreshState>;
  assertRefreshDrivenView: (input: {
    before: ProductCompareFixture[];
    removed: ProductCompareFixture;
    after: ProductCompareFixture[];
  }) => void | Promise<void>;
  assertAddEntryRecovered: (state: RemoveRefreshState) => void | Promise<void>;
  cleanup?: () => void | Promise<void>;
};

const integrationBlockers = {
  adapterMissing: 'BLOCKED_COMPARE_PAGE_REMOVE_REFRESH_ADAPTER_NOT_FOUND',
} as const;

function resolveRemoveRefreshAdapter(
  pageModule: ComparePageModule | null,
): ComparePageRemoveRefreshTestAdapter | null {
  if (!pageModule) return null;

  const candidate = pageModule.comparePageRemoveRefreshTestAdapter;
  if (!candidate || typeof candidate !== 'object') return null;

  const adapter = candidate as Partial<ComparePageRemoveRefreshTestAdapter>;
  if (
    typeof adapter.renderPage !== 'function' ||
    typeof adapter.seedFixtures !== 'function' ||
    typeof adapter.removeFixture !== 'function' ||
    typeof adapter.awaitRefreshSettled !== 'function' ||
    typeof adapter.readState !== 'function' ||
    typeof adapter.assertRefreshDrivenView !== 'function' ||
    typeof adapter.assertAddEntryRecovered !== 'function'
  ) {
    return null;
  }

  return adapter as ComparePageRemoveRefreshTestAdapter;
}

async function withRemoveRefreshAdapter(
  run: (adapter: ComparePageRemoveRefreshTestAdapter) => void | Promise<void>,
) {
  const pageModule = await loadComparePageModule();
  const adapter = resolveRemoveRefreshAdapter(pageModule);
  if (!adapter) return { blocker: integrationBlockers.adapterMissing } as const;

  await adapter.renderPage();
  try {
    await run(adapter);
  } finally {
    await adapter.cleanup?.();
  }

  return { blocker: null } as const;
}

const initialFixtures = normalCompareSet;
const refreshedFixtures = postRemoveCompareSet;
const removedFixture = initialFixtures.find(
  (fixture) => !refreshedFixtures.some((next) => next.fixtureId === fixture.fixtureId),
);

describe('ComparePage remove and refresh linkage', () => {
  it('reuses the step 2 refresh fixture instead of redefining compare products', () => {
    expect(postRemoveRefreshResponse.fixtureItems).toEqual(postRemoveCompareSet);
    expect(postRemoveCompareSet.length).toBeGreaterThan(0);
  });

  it('treats refresh ordering as adapter-observed output, not a production sorting rule', () => {
    expect(refreshedFixtures.map((fixture) => fixture.fixtureId)).toEqual(
      postRemoveRefreshResponse.fixtureItems?.map((fixture) => fixture.fixtureId),
    );
  });

  it('keeps unresolved production rules visible for copy and API details', () => {
    expect(unresolvedRules.compareLimit.length).toBeGreaterThan(0);
    expect(unresolvedRules.apiContract.length).toBeGreaterThan(0);
  });

  const realPageSuite =
    comparePageDiscovery.found && !comparePageDiscovery.ambiguous ? describe : describe.skip;

  realPageSuite('real page integration', () => {
    it('removes one fixture, waits for refreshed compare data, and syncs list with main area', async (context) => {
      if (!removedFixture) {
        context.skip('BLOCKED_REMOVE_REFRESH_FIXTURE_NOT_RESOLVABLE');
        return;
      }

      const result = await withRemoveRefreshAdapter(async (adapter) => {
        await adapter.seedFixtures(initialFixtures);
        await adapter.removeFixture(removedFixture);
        await adapter.awaitRefreshSettled();

        const state = await adapter.readState();
        const expectedIds = refreshedFixtures.map((fixture) => fixture.fixtureId);

        expect(state.removedFixtureId).toBe(removedFixture.fixtureId);
        expect(state.listedFixtureIds).toEqual(expectedIds);
        expect(state.mainAreaFixtureIds).toEqual(expectedIds);
        expect(state.staleFixtureIds).not.toContain(removedFixture.fixtureId);

        await adapter.assertRefreshDrivenView({
          before: initialFixtures,
          removed: removedFixture,
          after: refreshedFixtures,
        });
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('exposes recovered add-entry state and no stale removed fixture after refresh', async (context) => {
      if (!removedFixture) {
        context.skip('BLOCKED_REMOVE_REFRESH_FIXTURE_NOT_RESOLVABLE');
        return;
      }

      const result = await withRemoveRefreshAdapter(async (adapter) => {
        await adapter.seedFixtures(initialFixtures);
        await adapter.removeFixture(removedFixture);
        await adapter.awaitRefreshSettled();

        const state = await adapter.readState();
        const expectedIds = refreshedFixtures.map((fixture) => fixture.fixtureId);

        expect(state.listedFixtureIds).toEqual(expectedIds);
        expect(state.mainAreaFixtureIds).toEqual(expectedIds);
        expect(state.staleFixtureIds).toEqual([]);
        expect(state.addEntryAvailable).toBe(true);
        expect(state.placeholderCount === null || state.placeholderCount >= 0).toBe(true);

        await adapter.assertAddEntryRecovered(state);
      });

      if (result.blocker) context.skip(result.blocker);
    });
  });
});
