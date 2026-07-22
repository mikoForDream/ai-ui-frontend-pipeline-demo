import type { AddCompareResult, CompareProduct } from './types';

export async function loadCompareProducts(): Promise<CompareProduct[]> {
  return [];
}

export async function addCompareProduct(_id: string): Promise<AddCompareResult> {
  return { ok: false, reason: 'error', message: 'not implemented' };
}
