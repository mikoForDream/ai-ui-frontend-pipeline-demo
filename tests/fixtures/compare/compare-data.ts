import type { ProductCompareItem } from './compare-types';

export const compareLimit = 4;

export const normalProducts: ProductCompareItem[] = [
  {
    id: 'sku-1001',
    name: 'AeroBook 14 2024',
    image: 'https://cdn.example.com/products/aerobook-14.png',
    price: {
      amount: 5999,
      currency: 'CNY',
      display: '¥5999',
    },
    stock: {
      available: 32,
      status: 'in_stock',
      display: '现货 32 件',
    },
    specs: [
      { key: 'cpu', label: '处理器', value: 'Intel Core i5', isDifferent: true },
      { key: 'memory', label: '内存', value: '16GB', isDifferent: false },
      { key: 'storage', label: '存储', value: '512GB SSD', isDifferent: true },
      { key: 'weight', label: '重量', value: '1.37kg', isDifferent: true },
    ],
    detail: {
      productId: 'sku-1001',
      href: '/products/sku-1001',
      route: { name: 'product-detail', productId: 'sku-1001' },
    },
    meta: { category: 'normal' },
  },
  {
    id: 'sku-1002',
    name: 'AeroBook 14 Pro',
    image: 'https://cdn.example.com/products/aerobook-14-pro.png',
    price: {
      amount: 6999,
      currency: 'CNY',
      display: '¥6999',
    },
    stock: {
      available: 18,
      status: 'in_stock',
      display: '现货 18 件',
    },
    specs: [
      { key: 'cpu', label: '处理器', value: 'Intel Core i7', isDifferent: true },
      { key: 'memory', label: '内存', value: '16GB', isDifferent: false },
      { key: 'storage', label: '存储', value: '1TB SSD', isDifferent: true },
      { key: 'weight', label: '重量', value: '1.41kg', isDifferent: true },
    ],
    detail: {
      productId: 'sku-1002',
      href: '/products/sku-1002',
      route: { name: 'product-detail', productId: 'sku-1002' },
    },
    meta: { category: 'normal' },
  },
  {
    id: 'sku-1003',
    name: 'CloudPad Air 13',
    image: 'https://cdn.example.com/products/cloudpad-air-13.png',
    price: {
      amount: 5499,
      currency: 'CNY',
      display: '¥5499',
    },
    stock: {
      available: 9,
      status: 'in_stock',
      display: '现货 9 件',
    },
    specs: [
      { key: 'cpu', label: '处理器', value: 'Snapdragon X Elite', isDifferent: true },
      { key: 'memory', label: '内存', value: '16GB', isDifferent: false },
      { key: 'storage', label: '存储', value: '512GB SSD', isDifferent: true },
      { key: 'weight', label: '重量', value: '1.29kg', isDifferent: true },
    ],
    detail: {
      productId: 'sku-1003',
      href: '/products/sku-1003',
      route: { name: 'product-detail', productId: 'sku-1003' },
    },
    meta: { category: 'normal' },
  },
];

export const duplicateProduct: ProductCompareItem = {
  ...normalProducts[0],
  meta: {
    category: 'duplicate',
    isDuplicateCandidate: true,
  },
};

export const overLimitProduct: ProductCompareItem = {
  id: 'sku-1004',
  name: 'VisionNote Max 16',
  image: 'https://cdn.example.com/products/visionnote-max-16.png',
  price: {
    amount: 7999,
    currency: 'CNY',
    display: '¥7999',
  },
  stock: {
    available: 6,
    status: 'in_stock',
    display: '现货 6 件',
  },
  specs: [
    { key: 'cpu', label: '处理器', value: 'AMD Ryzen 9', isDifferent: true },
    { key: 'memory', label: '内存', value: '32GB', isDifferent: true },
    { key: 'storage', label: '存储', value: '1TB SSD', isDifferent: true },
    { key: 'weight', label: '重量', value: '1.86kg', isDifferent: true },
  ],
  detail: {
    productId: 'sku-1004',
    href: '/products/sku-1004',
    route: { name: 'product-detail', productId: 'sku-1004' },
  },
  meta: {
    category: 'over_limit',
    isOverLimitCandidate: true,
  },
};

export const missingFieldProduct: ProductCompareItem = {
  id: 'sku-1999',
  price: {
    amount: null,
    currency: 'CNY',
    display: '暂无报价',
  },
  stock: {
    available: null,
    status: 'unknown',
    display: '库存未知',
  },
  specs: [
    {
      key: 'screen',
      label: '屏幕',
      value: '15.6 英寸 超长说明文本用于校验长字段折行展示是否稳定',
      isDifferent: false,
    },
  ],
  detail: {
    productId: 'sku-1999',
    route: { name: 'product-detail', productId: 'sku-1999' },
  },
  meta: {
    category: 'missing_fields',
    missingFields: ['name', 'image', 'detail.href'],
  },
};

export const outOfStockProduct: ProductCompareItem = {
  id: 'sku-2001',
  name: 'PocketStation Mini',
  image: 'https://cdn.example.com/products/pocketstation-mini.png',
  price: {
    amount: 1999,
    currency: 'CNY',
    display: '¥1999',
  },
  stock: {
    available: 0,
    status: 'out_of_stock',
    display: '无库存',
  },
  specs: [
    { key: 'cpu', label: '处理器', value: 'Custom ARM', isDifferent: true },
    { key: 'memory', label: '内存', value: '8GB', isDifferent: true },
    { key: 'storage', label: '存储', value: '256GB', isDifferent: true },
  ],
  detail: {
    productId: 'sku-2001',
    href: '/products/sku-2001',
    route: { name: 'product-detail', productId: 'sku-2001' },
  },
  meta: { category: 'out_of_stock' },
};

export const normalCompareSet: ProductCompareItem[] = [...normalProducts, overLimitProduct];

export const duplicateAttempt = {
  existingItems: [normalProducts[0], normalProducts[1]],
  nextItem: duplicateProduct,
};

export const overLimitAttempt = {
  existingItems: normalCompareSet,
  nextItem: outOfStockProduct,
};

export const postRemoveCompareSet: ProductCompareItem[] = [
  normalProducts[1],
  normalProducts[2],
  overLimitProduct,
];

export const compareDataCatalog = {
  normalProducts,
  duplicateProduct,
  overLimitProduct,
  missingFieldProduct,
  outOfStockProduct,
  normalCompareSet,
  duplicateAttempt,
  overLimitAttempt,
  postRemoveCompareSet,
};
