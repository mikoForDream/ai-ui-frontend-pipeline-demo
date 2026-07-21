export type CompareSpec = {
  key: string;
  label: string;
  value: string;
  isDifferent: boolean;
};

export type ComparePrice = {
  amount: number | null;
  currency: string;
  display: string;
};

export type CompareStock = {
  available: number | null;
  status: 'in_stock' | 'out_of_stock' | 'unknown';
  display: string;
};

export type CompareDetailLink = {
  productId: string;
  href?: string;
  route?: {
    name: string;
    productId: string;
  };
};

export type CompareMeta = {
  category:
    | 'normal'
    | 'duplicate'
    | 'over_limit'
    | 'missing_fields'
    | 'out_of_stock';
  isDuplicateCandidate?: boolean;
  isOverLimitCandidate?: boolean;
  missingFields?: string[];
};

export type ProductCompareItem = {
  id: string;
  name?: string;
  image?: string;
  price?: ComparePrice | null;
  stock?: CompareStock | null;
  specs: CompareSpec[];
  detail: CompareDetailLink;
  meta: CompareMeta;
};

export type CompareApiResponse = {
  code: number;
  message: string;
  requestId: string;
  data: {
    compareLimit: number;
    items: ProductCompareItem[];
    summary: {
      total: number;
      canAddMore: boolean;
    };
  } | null;
};

export type CompareApiErrorResponse = {
  code: number;
  message: string;
  requestId: string;
  error: {
    type: 'server_error' | 'timeout' | 'validation_error';
    retryable: boolean;
    detail: string;
  };
};
