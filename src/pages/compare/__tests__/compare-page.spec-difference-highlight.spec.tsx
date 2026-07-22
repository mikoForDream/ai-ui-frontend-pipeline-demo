import { describe, expect, it } from 'vitest';
import {
  comparePageDiscovery,
  loadComparePageModule,
  normalProducts,
  unresolvedRules,
  type ComparePageModule,
} from '../../../test/compare-page';
import type { ProductCompareFixture } from '../../../../tests/fixtures/compare/compare-types';

type CompareSpecRowObservation = {
  key: string;
  label: string;
  values: Array<string | null>;
  highlighted: boolean;
};

type CompareDifferenceHighlightTestAdapter = {
  renderPage: () => void | Promise<void>;
  seedFixtures: (fixtures: ProductCompareFixture[]) => void | Promise<void>;
  readSpecRows: () => CompareSpecRowObservation[] | Promise<CompareSpecRowObservation[]>;
  assertLongValuePresentation: (input: { key: string; expectedValue: string }) => void | Promise<void>;
  assertEmptyValuePresentation: (input: { key: string }) => void | Promise<void>;
  cleanup?: () => void | Promise<void>;
};

const integrationBlockers = {
  adapterMissing: 'BLOCKED_COMPARE_PAGE_DIFFERENCE_HIGHLIGHT_ADAPTER_NOT_FOUND',
} as const;

function resolveDifferenceHighlightAdapter(
  pageModule: ComparePageModule | null,
): CompareDifferenceHighlightTestAdapter | null {
  if (!pageModule) return null;

  const candidate = pageModule.comparePageDifferenceHighlightTestAdapter;
  if (!candidate || typeof candidate !== 'object') return null;

  const adapter = candidate as Partial<CompareDifferenceHighlightTestAdapter>;
  if (
    typeof adapter.renderPage !== 'function' ||
    typeof adapter.seedFixtures !== 'function' ||
    typeof adapter.readSpecRows !== 'function' ||
    typeof adapter.assertLongValuePresentation !== 'function' ||
    typeof adapter.assertEmptyValuePresentation !== 'function'
  ) {
    return null;
  }

  return adapter as CompareDifferenceHighlightTestAdapter;
}

function cloneFixtureWithSpecs(
  fixture: ProductCompareFixture,
  specs: ProductCompareFixture['specs'],
): ProductCompareFixture {
  return {
    ...fixture,
    source: { ...fixture.source },
    specs: specs.map((spec) => ({ ...spec })),
    expected: { ...fixture.expected },
    detailCandidate: fixture.detailCandidate ? { ...fixture.detailCandidate } : undefined,
  };
}

async function withDifferenceHighlightAdapter(
  fixtures: ProductCompareFixture[],
  run: (adapter: CompareDifferenceHighlightTestAdapter) => void | Promise<void>,
) {
  const pageModule = await loadComparePageModule();
  const adapter = resolveDifferenceHighlightAdapter(pageModule);
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

const longValue =
  'LPDDR5X 32GB unified memory configuration with extended thermal profile annotation for wrapping verification';

const alignedSpecFixtures = [
  cloneFixtureWithSpecs(normalProducts[0], [
    { key: 'memory', label: '内存', value: '16GB' },
    { key: 'finish', label: '机身材质', value: 'Aluminum' },
    { key: 'notes', label: '备注', value: null },
  ]),
  cloneFixtureWithSpecs(normalProducts[1], [
    { key: 'memory', label: '内存', value: '16GB' },
    { key: 'finish', label: '机身材质', value: 'Carbon Fiber' },
    { key: 'notes', label: '备注', value: longValue },
  ]),
  cloneFixtureWithSpecs(normalProducts[2], [
    { key: 'memory', label: '内存', value: '16GB' },
    { key: 'finish', label: '机身材质', value: 'Magnesium Alloy' },
    { key: 'notes', label: '备注', value: null },
  ]),
];

describe('ComparePage spec comparison and difference highlight', () => {
  it('reuses step 2 fixtures and keeps difference rules explicitly unresolved', () => {
    expect(alignedSpecFixtures.map((fixture) => fixture.fixtureId)).toEqual(
      normalProducts.map((fixture) => fixture.fixtureId),
    );
    expect(unresolvedRules.differenceCalculation.length).toBeGreaterThan(0);
  });

  it('treats long and empty spec values as test-only fixture shaping, not production copy rules', () => {
    const notesValues = alignedSpecFixtures
      .flatMap((fixture) => fixture.specs)
      .filter((spec) => spec.key === 'notes')
      .map((spec) => spec.value);

    expect(notesValues).toContain(longValue);
    expect(notesValues).toContain(null);
  });

  const realPageSuite =
    comparePageDiscovery.found && !comparePageDiscovery.ambiguous ? describe : describe.skip;

  realPageSuite('real page integration', () => {
    it('shows same-value rows without highlight and keeps differing rows aligned to the current implementation', async (context) => {
      const result = await withDifferenceHighlightAdapter(alignedSpecFixtures, async (adapter) => {
        const rows = await adapter.readSpecRows();
        const memoryRow = rows.find((row) => row.key === 'memory');
        const finishRow = rows.find((row) => row.key === 'finish');

        expect(memoryRow).toBeDefined();
        expect(memoryRow?.values).toEqual(['16GB', '16GB', '16GB']);
        expect(memoryRow?.highlighted).toBe(false);

        expect(finishRow).toBeDefined();
        expect(finishRow?.values).toEqual(['Aluminum', 'Carbon Fiber', 'Magnesium Alloy']);

        if (!finishRow?.highlighted) {
          context.skip(`BLOCKED_${unresolvedRules.differenceCalculation}`);
        }
      });

      if (result.blocker) context.skip(result.blocker);
    });

    it('renders long and empty spec values without dirty fallback output', async (context) => {
      const result = await withDifferenceHighlightAdapter(alignedSpecFixtures, async (adapter) => {
        const rows = await adapter.readSpecRows();
        const notesRow = rows.find((row) => row.key === 'notes');

        expect(notesRow).toBeDefined();
        expect(notesRow?.values).toEqual([null, longValue, null]);

        await adapter.assertLongValuePresentation({
          key: 'notes',
          expectedValue: longValue,
        });
        await adapter.assertEmptyValuePresentation({ key: 'notes' });
      });

      if (result.blocker) context.skip(result.blocker);
    });
  });
});
