export type CompareSpec = {
  label: string;
  value: string;
  different: boolean;
};

export type CompareProduct = {
  id: string;
  name: string;
  priceText: string;
  stockText: string;
  detailUrl: string;
  specs: CompareSpec[];
};

export type AddCompareResult = {
  ok: boolean;
  reason?: 'duplicate' | 'limit' | 'error';
  message?: string;
  products?: CompareProduct[];
  limit?: number;
};
