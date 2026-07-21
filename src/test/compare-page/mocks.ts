export type CompareProduct = {
  id: string
  name: string
  image: string
  priceLabel: string
  stockLabel: string
  detailUrl: string
  attributes: Array<{
    label: string
    value: string
    differs: boolean
  }>
}

export type CompareApiState = {
  addProduct: {
    ok: boolean
    code: 'OK' | 'DUPLICATE' | 'LIMIT_EXCEEDED'
  }
  query: {
    ok: boolean
    status: number
  }
  removeProduct: {
    ok: boolean
    status: number
  }
}

export type CompareScenario = {
  name: string
  state: 'ready' | 'loading' | 'error'
  products: CompareProduct[]
  api: CompareApiState
  error: null | { message: string; status?: number }
}

const baseProducts: CompareProduct[] = [
  {
    id: 'sku-1001',
    name: 'Laptop Pro 14',
    image: '/mock/laptop-pro-14.png',
    priceLabel: '$1299',
    stockLabel: 'In stock',
    detailUrl: '/products/sku-1001',
    attributes: [
      { label: 'CPU', value: 'M3', differs: false },
      { label: 'Memory', value: '16GB', differs: true },
    ],
  },
  {
    id: 'sku-1002',
    name: 'Laptop Air 13',
    image: '/mock/laptop-air-13.png',
    priceLabel: '$999',
    stockLabel: 'Only 3 left',
    detailUrl: '/products/sku-1002',
    attributes: [
      { label: 'CPU', value: 'M3', differs: false },
      { label: 'Memory', value: '8GB', differs: true },
    ],
  },
]

export const compareScenarios = {
  happyPath: {
    name: 'happyPath',
    state: 'ready',
    products: baseProducts,
    api: {
      addProduct: { ok: true, code: 'OK' },
      query: { ok: true, status: 200 },
      removeProduct: { ok: true, status: 200 },
    },
    error: null,
  },
  duplicateBlocked: {
    name: 'duplicateBlocked',
    state: 'ready',
    products: baseProducts,
    api: {
      addProduct: { ok: false, code: 'DUPLICATE' },
      query: { ok: true, status: 200 },
      removeProduct: { ok: true, status: 200 },
    },
    error: null,
  },
  overLimitBlocked: {
    name: 'overLimitBlocked',
    state: 'ready',
    products: [
      ...baseProducts,
      {
        id: 'sku-1003',
        name: 'Laptop Studio 16',
        image: '/mock/laptop-studio-16.png',
        priceLabel: '$1499',
        stockLabel: 'In stock',
        detailUrl: '/products/sku-1003',
        attributes: [
          { label: 'CPU', value: 'M3 Max', differs: true },
          { label: 'Memory', value: '32GB', differs: true },
        ],
      },
      {
        id: 'sku-1004',
        name: 'Laptop Flex 15',
        image: '/mock/laptop-flex-15.png',
        priceLabel: '$899',
        stockLabel: 'Out of stock',
        detailUrl: '/products/sku-1004',
        attributes: [
          { label: 'CPU', value: 'R7', differs: true },
          { label: 'Memory', value: '16GB', differs: false },
        ],
      },
    ],
    api: {
      addProduct: { ok: false, code: 'LIMIT_EXCEEDED' },
      query: { ok: true, status: 200 },
      removeProduct: { ok: true, status: 200 },
    },
    error: null,
  },
  loading: {
    name: 'loading',
    state: 'loading',
    products: [],
    api: {
      addProduct: { ok: true, code: 'OK' },
      query: { ok: true, status: 200 },
      removeProduct: { ok: true, status: 200 },
    },
    error: null,
  },
  empty: {
    name: 'empty',
    state: 'ready',
    products: [],
    api: {
      addProduct: { ok: true, code: 'OK' },
      query: { ok: true, status: 200 },
      removeProduct: { ok: true, status: 200 },
    },
    error: null,
  },
  error: {
    name: 'error',
    state: 'error',
    products: [],
    api: {
      addProduct: { ok: true, code: 'OK' },
      query: { ok: false, status: 500 },
      removeProduct: { ok: false, status: 500 },
    },
    error: { message: 'compare query failed', status: 500 },
  },
} satisfies Record<string, CompareScenario>

export type CompareScenarioName = keyof typeof compareScenarios
