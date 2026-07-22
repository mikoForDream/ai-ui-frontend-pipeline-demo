export type ComparePageModule = Record<string, unknown>;

const candidateModules = import.meta.glob<ComparePageModule>([
  '../../pages/**/ComparePage.{ts,tsx,js,jsx}',
  '../../pages/**/compare-page.{ts,tsx,js,jsx}',
  '../../pages/compare/index.{ts,tsx,js,jsx}',
]);

const candidatePaths = Object.keys(candidateModules).sort();

export const comparePageDiscovery = {
  found: candidatePaths.length > 0,
  ambiguous: candidatePaths.length > 1,
  candidates: candidatePaths,
  blocker:
    candidatePaths.length === 0
      ? 'BLOCKED_REAL_COMPARE_PAGE_NOT_FOUND'
      : candidatePaths.length > 1
        ? 'BLOCKED_MULTIPLE_COMPARE_PAGE_CANDIDATES'
        : null,
} as const;

export async function loadComparePageModule(): Promise<ComparePageModule | null> {
  if (candidatePaths.length !== 1) return null;
  return candidateModules[candidatePaths[0]]();
}
