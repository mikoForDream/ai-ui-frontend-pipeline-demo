import { describe, expect, it } from 'vitest';
import {
  comparePageDiscovery,
  loadComparePageModule,
  missingPriceCompareResponse,
  missingPriceProduct,
  normalProducts,
  outOfStockCompareResponse,
  outOfStockProduct,
  unresolvedRules,
  type ComparePageModule,
} from '../../../test/compare-page';
import type { ProductCompareFixture } from '../../../../tests/fixtures/compare/compare-types';

type PriceStockObservation = {
  fixtureId: string;
  priceText: string | null;
  stockText: string | null;
  priceState: 'value' | 'missing' | 'invalid' | 'unknown';
  stockState: 'value' | 'out_of_stock' | 'missing' | 'invalid' | 'unknown';
};

type ComparePagePriceStockTestAdapter = {
  renderPage: () => void | Promise<void>;
  seedFixtures: (fixtures: ProductCompareFixture[]) => void | Promise<void>;
  readPriceStock: () => PriceStockObservation[] | Promise<PriceStockObservation[]>;
  assertValuePresentation: (input: {
    fixture: ProductCompareFixture;
    observation: PriceStockObservation;
  }) => void | Promise<void>;
  assertMissingPricePresentation: (input: {
    fixture: ProductCompareFixture;
    observation: PriceStockObservation;
  }) => void | Promise<void>;
  assertOutOfStockPresentation: (input: {
    fixture: ProductCompareFixture;
    observation: PriceStockObservation;
  }) => void | Promise<void>;
  assertInvalidValueSanitized: (input: {
    fixture: ProductCompareFixture;
    observation: PriceStockObservation;
  }) => void | Promise<void>;
  cleanup?: () => void | Promise<void>;
};

const integrationBlockers = {
  adapterMissing: 'BLOCKED_COMPARE_PAGE_PRICE_STOCK_ADAPTER_NOT_FOUND',
} as const;

function resolvePriceStockAdapter(
  pageModule: ComparePageModule | null,
): ComparePagePriceStockTestAdapter | null {
  if (!pageModule) return null;

  const candidate = pageModule.comparePagePriceStockTestAdapter;
  if (!candidate || typeof candidate !== 'object') return null;

  const adapter = candidate as Partial<ComparePagePriceStockTestAdapter>;
  if (
    typeof adapter.renderPage !== 'function' ||
    typeof adapter.seedFixtures !== 'function' ||
    typeof adapter.readPriceStock !== 'function' ||
    typeof adapter.assertValuePresentation !== 'function' ||
    typeof adapter.assertMissingPricePresentation !== 'function' ||
    typeof adapter.assertOutOfStockPresentation !== 'function' ||
    typeof adapter.assertInvalidValueSanitized !== 'function'
  ) {
    return null;
  }

  return adapter as ComparePagePriceStockTestAdapter;
}

function cloneFixture(
  fixture: ProductCompareFixture,
  source: Record<string, unknown>,
): ProductCompareFixture {
  return {
    ...fixture,
    source,
    specs: fixture.specs.map((spec) => ({ ...spec })),
    expected: { ...fixture.expected },
    detailCandidate: fixture.detailCandidate ? { ...fixture.detailCandidate } : undefined,
  };
}

async function withPriceStockAdapter(
  fixtures: ProductCompareFixture[],
  run: (adapter: ComparePagePriceStockTestAdapter) => void | Promise<void>,
) {
  const pageModule = await loadComparePageModule();
  const adapter = resolvePriceStockAdapter(pageModule);
  if (!adapter) return { blocker: integrationBlockers.adapterMissing } as const;

  await adapter.renderPage();
  try {
    await adapter.seedFixtures(fixtures);
    await run(adapter);
  } finally {
    await adapter.cleanup?.();
  }

  return { blocker: null } as const;
}

const invalidValueProduct = cloneFixture(missingPriceProduct, {
  ...missingPriceProduct.source,
  candidateId: 'sku-2000-invalid-values',
  candidateName: 'LiteBook 13 Invalid Input',
  candidatePrice: 'NaN',
  candidateStock: 'unknown',
});

const baselinePriceStockFixtures = [normalProducts[0], missingPriceProduct, outOfStockProduct];

describe('ComparePage price and stock presentation', () => {
  it('reuses step 2 fixtures and samples for missing price and out-of-stock scenarios', () => {
    expect(missingPriceCompareResponse.fixtureItems).toEqual([missingPriceProduct]);
    expect(outOfStockCompareResponse.fixtureItems).toEqual([outOfStockProduct]);
    expect(normalProducts[0].fixtureId.length).toBeGreaterThan(0);
  });

  it('keeps fallback copy unresolved instead of turning fixture values into product rules', () => {
    expect(unresolvedRules.missingPriceCopy.length).toBeGreaterThan(0);
    expect(unresolvedRules.missingStockCopy.length).toBeGreaterThan(0);
  });

  it('uses malformed price and stock only as test-only input shaping for tolerance checks', () => {
    expect(typeof invalidValueProduct.source.candidatePrice).toBe('string');
    expect(typeof invalidValueProduct.source.candidateStock).toBe('string');
    expect(invalidValueProduct.fixtureId).toBe(missingPriceProduct.fixtureId);
  });

  const realPageSuite =
    comparePageDiscovery.found && !comparePageDiscovery.ambiguous ? describe : describe.skip;

  realPageSuite('real page integration', () => {
    it('shows normal price and stock values using the current page implementation', async (context) => {
      const result = await withPriceStockAdapter(baselinePriceStockFixtures, async (adapter) => {
        const observations = await adapter.readPriceStock();
        const normalObservation = observations.find(
          (entry) => entry.fixtureId === normalProducts[0].fixtureId,
        );

        expect(normalObservation).toBeDefined();
        expect(normalObservation?.priceText).not.toBeNull();
        expect(normalObservation?.stockText).not.toBeNull();
        expect(normalObservation?.priceState).toBe('value');
        expect(['value', 'unknown']).toContain(normalObservation?.stockState ?? 'unknown');

        await adapter.assertValuePresentation({
          fixture: normalProducts[0],
          observation: normalObservation!,
        });
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('degrades missing price without exposing dirty raw values or crashing the page', async (context) => {
      const result = await withPriceStockAdapter([missingPriceProduct], async (adapter) => {
        const observations = await adapter.readPriceStock();
        const observation = observations.find(
          (entry) => entry.fixtureId === missingPriceProduct.fixtureId,
        );

        expect(observation).toBeDefined();
        expect(observation?.priceText ?? '').not.toMatch(/undefined|null|NaN/i);
        expect(['missing', 'unknown']).toContain(observation?.priceState ?? 'unknown');
        expect(['value', 'unknown']).toContain(observation?.stockState ?? 'unknown');

        await adapter.assertMissingPricePresentation({
          fixture: missingPriceProduct,
          observation: observation!,
        });
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('shows stock-zero state through the current implementation without inferring fallback copy', async (context) => {
      const result = await withPriceStockAdapter([outOfStockProduct], async (adapter) => {
        const observations = await adapter.readPriceStock();
        const observation = observations.find(
          (entry) => entry.fixtureId === outOfStockProduct.fixtureId,
        );

        expect(observation).toBeDefined();
        expect(observation?.priceState).toBe('value');
        expect(['out_of_stock', 'unknown']).toContain(observation?.stockState ?? 'unknown');
        expect(observation?.stockText ?? '').not.toMatch(/undefined|null|NaN/i);

        await adapter.assertOutOfStockPresentation({
          fixture: outOfStockProduct,
          observation: observation!,
        });
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('sanitizes malformed price and stock inputs instead of rendering raw invalid values', async (context) => {
      const result = await withPriceStockAdapter([invalidValueProduct], async (adapter) => {
        const observations = await adapter.readPriceStock();
        const observation = observations.find(
          (entry) => entry.fixtureId === invalidValueProduct.fixtureId,
        );

        expect(observation).toBeDefined();
        expect(`${observation?.priceText ?? ''} ${observation?.stockText ?? ''}`).not.toMatch(
          /undefined|null|NaN|\[object Object\]/i,
        );
        expect(['invalid', 'missing', 'unknown']).toContain(observation?.priceState ?? 'unknown');
        expect(['invalid', 'missing', 'unknown']).toContain(observation?.stockState ?? 'unknown');

        await adapter.assertInvalidValueSanitized({
          fixture: invalidValueProduct,
          observation: observation!,
        });
      });

      if (result.blocker) context.skip(result.blocker);
    });
  });
});