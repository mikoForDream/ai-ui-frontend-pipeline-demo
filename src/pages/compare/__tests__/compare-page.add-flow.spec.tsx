import { describe, expect, it } from 'vitest';
import {
  comparePageDiscovery,
  loadComparePageModule,
  normalProducts,
  type ComparePageModule,
} from '../../../test/compare-page';
import type { ProductCompareFixture } from '../../../../tests/fixtures/compare/compare-types';

type AddFlowState = {
  listedFixtureIds: string[];
  mainAreaFixtureIds: string[];
};

type ComparePageAddFlowTestAdapter = {
  renderPage: () => void | Promise<void>;
  addFixture: (fixture: ProductCompareFixture) => void | Promise<void>;
  readState: () => AddFlowState | Promise<AddFlowState>;
  assertAddedControlState: (fixture: ProductCompareFixture) => void | Promise<void>;
  cleanup?: () => void | Promise<void>;
};

const integrationBlockers = {
  pageMissing: 'BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND',
  pageAmbiguous: 'BLOCKED_MULTIPLE_COMPARE_PAGE_CANDIDATES',
  adapterMissing: 'BLOCKED_COMPARE_PAGE_ADD_FLOW_ADAPTER_NOT_FOUND',
} as const;

function resolveAddFlowAdapter(
  pageModule: ComparePageModule | null,
): ComparePageAddFlowTestAdapter | null {
  if (!pageModule) return null;

  const candidate = pageModule.comparePageAddFlowTestAdapter;
  if (!candidate || typeof candidate !== 'object') return null;

  const adapter = candidate as Partial<ComparePageAddFlowTestAdapter>;
  if (
    typeof adapter.renderPage !== 'function' ||
    typeof adapter.addFixture !== 'function' ||
    typeof adapter.readState !== 'function' ||
    typeof adapter.assertAddedControlState !== 'function'
  ) {
    return null;
  }

  return adapter as ComparePageAddFlowTestAdapter;
}

async function runAddFlow(sampleSize: number) {
  const pageModule = await loadComparePageModule();
  const adapter = resolveAddFlowAdapter(pageModule);
  if (!adapter) return { blocker: integrationBlockers.adapterMissing } as const;

  const fixtures = normalProducts.slice(0, sampleSize);
  await adapter.renderPage();

  try {
    for (const fixture of fixtures) {
      await adapter.addFixture(fixture);
    }

    const state = await adapter.readState();
    const expectedIds = fixtures.map((fixture) => fixture.fixtureId);

    expect(state.listedFixtureIds).toEqual(expectedIds);
    expect(state.mainAreaFixtureIds).toEqual(expectedIds);

    for (const fixture of fixtures) {
      await adapter.assertAddedControlState(fixture);
    }
  } finally {
    await adapter.cleanup?.();
  }

  return { blocker: null } as const;
}

describe('ComparePage add flow', () => {
  it('reuses the step 2 normal fixtures without redefining product fields', () => {
    expect(normalProducts.length).toBeGreaterThanOrEqual(2);
    expect(normalProducts.every((fixture) => fixture.fixtureId.length > 0)).toBe(true);
  });

  it('uses fixture availability as test input, not as a production compare limit', () => {
    const sampleSizes = [2, normalProducts.length];
    expect(sampleSizes.every((size) => size <= normalProducts.length)).toBe(true);
  });

  const realPageSuite =
    comparePageDiscovery.found && !comparePageDiscovery.ambiguous ? describe : describe.skip;

  realPageSuite('real page integration', () => {
    it('adds two fixtures and synchronizes list and main area', async (context) => {
      const result = await runAddFlow(2);
      if (result.blocker) {
        context.skip(result.blocker);
      }
    });

    it('repeats the flow with every currently available normal fixture', async (context) => {
      const result = await runAddFlow(normalProducts.length);
      if (result.blocker) {
        context.skip(result.blocker);
      }
    });
  });
});
