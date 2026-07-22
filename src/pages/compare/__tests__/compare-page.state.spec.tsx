import { describe, expect, it } from 'vitest';
import {
  comparePageDiscovery,
  emptyCompareResponse,
  loadComparePageModule,
  missingFieldCompareResponse,
  missingFieldProduct,
  normalProducts,
  serverErrorResponse,
  timeoutErrorResponse,
  unresolvedRules,
  type ComparePageModule,
} from '../../../test/compare-page';
import type { CompareApiSample, ProductCompareFixture } from '../../../../tests/fixtures/compare/compare-types';

type ComparePageViewState = 'loading' | 'ready' | 'empty' | 'error' | 'partial' | 'unknown';

type ComparePageStateSnapshot = {
  viewState: ComparePageViewState;
  listedFixtureIds: string[];
  mainAreaFixtureIds: string[];
  visibleErrorText: string | null;
  visibleEmptyText: string | null;
  retryAvailable: boolean;
  addEntryAvailable: boolean | null;
  staleFixtureIds: string[];
};

type ComparePageStateTestAdapter = {
  renderPage: () => void | Promise<void>;
  mockQuerySample: (sample: CompareApiSample) => void | Promise<void>;
  startInitialLoad: () => void | Promise<void>;
  awaitSettled: () => void | Promise<void>;
  readState: () => ComparePageStateSnapshot | Promise<ComparePageStateSnapshot>;
  retryLoad?: () => void | Promise<void>;
  assertLoadingPresentation: () => void | Promise<void>;
  assertEmptyPresentation: (state: ComparePageStateSnapshot) => void | Promise<void>;
  assertErrorPresentation: (input: {
    sample: CompareApiSample;
    state: ComparePageStateSnapshot;
  }) => void | Promise<void>;
  assertPartialPresentation: (input: {
    fixture: ProductCompareFixture;
    state: ComparePageStateSnapshot;
  }) => void | Promise<void>;
  cleanup?: () => void | Promise<void>;
};

const integrationBlockers = {
  adapterMissing: 'BLOCKED_COMPARE_PAGE_STATE_ADAPTER_NOT_FOUND',
  retryUnavailable: 'BLOCKED_COMPARE_PAGE_STATE_RETRY_NOT_OBSERVABLE',
  partialFailureUnspecified: 'BLOCKED_PARTIAL_RENDER_FAILURE_ORACLE_UNCONFIRMED',
} as const;

function resolveStateAdapter(
  pageModule: ComparePageModule | null,
): ComparePageStateTestAdapter | null {
  if (!pageModule) return null;

  const candidate = pageModule.comparePageStateTestAdapter;
  if (!candidate || typeof candidate !== 'object') return null;

  const adapter = candidate as Partial<ComparePageStateTestAdapter>;
  if (
    typeof adapter.renderPage !== 'function' ||
    typeof adapter.mockQuerySample !== 'function' ||
    typeof adapter.startInitialLoad !== 'function' ||
    typeof adapter.awaitSettled !== 'function' ||
    typeof adapter.readState !== 'function' ||
    typeof adapter.assertLoadingPresentation !== 'function' ||
    typeof adapter.assertEmptyPresentation !== 'function' ||
    typeof adapter.assertErrorPresentation !== 'function' ||
    typeof adapter.assertPartialPresentation !== 'function'
  ) {
    return null;
  }

  return adapter as ComparePageStateTestAdapter;
}

async function withStateAdapter(
  run: (adapter: ComparePageStateTestAdapter) => void | Promise<void>,
) {
  const pageModule = await loadComparePageModule();
  const adapter = resolveStateAdapter(pageModule);
  if (!adapter) return { blocker: integrationBlockers.adapterMissing } as const;

  await adapter.renderPage();
  try {
    await run(adapter);
  } finally {
    await adapter.cleanup?.();
  }

  return { blocker: null } as const;
}

describe('ComparePage state handling', () => {
  it('reuses step 2 state samples instead of redefining transport payloads', () => {
    expect(emptyCompareResponse.fixtureItems).toEqual([]);
    expect(missingFieldCompareResponse.fixtureItems).toEqual([missingFieldProduct]);
    expect(serverErrorResponse.expected.retryable).toBe(true);
    expect(timeoutErrorResponse.expected.retryable).toBe(true);
  });

  it('keeps API contract and partial-render rules unresolved instead of inferring production behavior', () => {
    expect(unresolvedRules.apiContract.length).toBeGreaterThan(0);
    expect(missingFieldProduct.expected.missingFields?.length ?? 0).toBeGreaterThan(0);
  });

  it('uses fixture-backed state inputs as test-only coverage, not as UI copy or request-count rules', () => {
    expect(normalProducts[0].fixtureId.length).toBeGreaterThan(0);
    expect(timeoutErrorResponse.transport).toBe('timeout');
    expect(serverErrorResponse.httpStatus).toBe(500);
  });

  const realPageSuite =
    comparePageDiscovery.found && !comparePageDiscovery.ambiguous ? describe : describe.skip;

  realPageSuite('real page integration', () => {
    it('shows a loading state before the initial query settles, then switches to ready data', async (context) => {
      const result = await withStateAdapter(async (adapter) => {
        await adapter.mockQuerySample({
          transport: 'success',
          httpStatus: 200,
          payload: { candidateItems: normalProducts.map((item) => item.source) },
          fixtureItems: normalProducts,
          expected: { kind: 'success' },
        });
        await adapter.startInitialLoad();

        const loadingState = await adapter.readState();
        expect(['loading', 'unknown']).toContain(loadingState.viewState);
        expect(loadingState.listedFixtureIds).toEqual([]);
        expect(loadingState.mainAreaFixtureIds).toEqual([]);
        await adapter.assertLoadingPresentation();

        await adapter.awaitSettled();
        const settledState = await adapter.readState();
        const expectedIds = normalProducts.map((fixture) => fixture.fixtureId);

        expect(['ready', 'unknown']).toContain(settledState.viewState);
        expect(settledState.listedFixtureIds).toEqual(expectedIds);
        expect(settledState.mainAreaFixtureIds).toEqual(expectedIds);
        expect(settledState.staleFixtureIds).toEqual([]);
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('shows empty state on an empty response without crashing unrelated entry state', async (context) => {
      const result = await withStateAdapter(async (adapter) => {
        await adapter.mockQuerySample(emptyCompareResponse);
        await adapter.startInitialLoad();
        await adapter.awaitSettled();

        const state = await adapter.readState();
        expect(['empty', 'unknown']).toContain(state.viewState);
        expect(state.listedFixtureIds).toEqual([]);
        expect(state.mainAreaFixtureIds).toEqual([]);
        expect(state.visibleErrorText ?? '').not.toMatch(/undefined|null|NaN/i);
        expect(state.staleFixtureIds).toEqual([]);
        expect(state.addEntryAvailable === null || typeof state.addEntryAvailable === 'boolean').toBe(true);

        await adapter.assertEmptyPresentation(state);
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('shows retryable error state for a 500 response and can recover through the current retry flow', async (context) => {
      const result = await withStateAdapter(async (adapter) => {
        await adapter.mockQuerySample(serverErrorResponse);
        await adapter.startInitialLoad();
        await adapter.awaitSettled();

        const errorState = await adapter.readState();
        expect(['error', 'unknown']).toContain(errorState.viewState);
        expect(errorState.listedFixtureIds).toEqual([]);
        expect(errorState.mainAreaFixtureIds).toEqual([]);
        expect(errorState.retryAvailable).toBe(true);
        expect(errorState.visibleErrorText ?? '').not.toMatch(/undefined|null|NaN/i);

        await adapter.assertErrorPresentation({
          sample: serverErrorResponse,
          state: errorState,
        });

        if (typeof adapter.retryLoad !== 'function') {
          context.skip(integrationBlockers.retryUnavailable);
          return;
        }

        await adapter.mockQuerySample({
          transport: 'success',
          httpStatus: 200,
          payload: { candidateItems: normalProducts.map((item) => item.source) },
          fixtureItems: normalProducts,
          expected: { kind: 'success' },
        });
        await adapter.retryLoad();
        await adapter.awaitSettled();

        const recoveredState = await adapter.readState();
        const expectedIds = normalProducts.map((fixture) => fixture.fixtureId);

        expect(['ready', 'unknown']).toContain(recoveredState.viewState);
        expect(recoveredState.listedFixtureIds).toEqual(expectedIds);
        expect(recoveredState.mainAreaFixtureIds).toEqual(expectedIds);
        expect(recoveredState.visibleErrorText ?? '').not.toMatch(/undefined|null|NaN/i);
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('shows retryable timeout handling and can return to normal flow when retried', async (context) => {
      const result = await withStateAdapter(async (adapter) => {
        await adapter.mockQuerySample(timeoutErrorResponse);
        await adapter.startInitialLoad();
        await adapter.awaitSettled();

        const timeoutState = await adapter.readState();
        expect(['error', 'unknown']).toContain(timeoutState.viewState);
        expect(timeoutState.retryAvailable).toBe(true);
        expect(timeoutState.visibleErrorText ?? '').not.toMatch(/undefined|null|NaN/i);

        await adapter.assertErrorPresentation({
          sample: timeoutErrorResponse,
          state: timeoutState,
        });

        if (typeof adapter.retryLoad !== 'function') {
          context.skip(integrationBlockers.retryUnavailable);
          return;
        }

        await adapter.mockQuerySample({
          transport: 'success',
          httpStatus: 200,
          payload: { candidateItems: normalProducts.slice(0, 2).map((item) => item.source) },
          fixtureItems: normalProducts.slice(0, 2),
          expected: { kind: 'success' },
        });
        await adapter.retryLoad();
        await adapter.awaitSettled();

        const recoveredState = await adapter.readState();
        const expectedIds = normalProducts.slice(0, 2).map((fixture) => fixture.fixtureId);

        expect(['ready', 'unknown']).toContain(recoveredState.viewState);
        expect(recoveredState.listedFixtureIds).toEqual(expectedIds);
        expect(recoveredState.mainAreaFixtureIds).toEqual(expectedIds);
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('tolerates missing fields without rendering dirty values or crashing the whole page', async (context) => {
      const result = await withStateAdapter(async (adapter) => {
        await adapter.mockQuerySample(missingFieldCompareResponse);
        await adapter.startInitialLoad();
        await adapter.awaitSettled();

        const state = await adapter.readState();
        expect(['partial', 'ready', 'unknown']).toContain(state.viewState);
        expect(state.listedFixtureIds).toEqual([missingFieldProduct.fixtureId]);
        expect(state.mainAreaFixtureIds).toEqual([missingFieldProduct.fixtureId]);
        expect(`${state.visibleErrorText ?? ''} ${state.visibleEmptyText ?? ''}`).not.toMatch(
          /undefined|null|NaN|\[object Object\]/i,
        );
        expect(state.staleFixtureIds).toEqual([]);

        await adapter.assertPartialPresentation({
          fixture: missingFieldProduct,
          state,
        });
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('records partial-render failure coverage as blocked when the current implementation exposes no stable oracle', async (context) => {
      context.skip(integrationBlockers.partialFailureUnspecified);
    });
  });
});
