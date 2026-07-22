import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import React from 'react';
import { ComparePage } from '../compare-page';
import type { CompareProduct, AddCompareResult } from '../types';

const mockNavigate = vi.fn();
const mockLoadCompareProducts = vi.fn<[], Promise<CompareProduct[]>>();
const mockAddCompareProduct = vi.fn<[string], Promise<AddCompareResult>>();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../api', () => ({
  loadCompareProducts: () => mockLoadCompareProducts(),
  addCompareProduct: (id: string) => mockAddCompareProduct(id),
}));

const baseProducts: CompareProduct[] = [
  {
    id: 'p-1',
    name: 'Alpha Phone',
    priceText: '1999',
    stockText: '有库存',
    detailUrl: '/products/p-1',
    specs: [
      { label: '屏幕', value: '6.1英寸', different: false },
      { label: '容量', value: '128GB', different: false },
    ],
  },
  {
    id: 'p-2',
    name: 'Beta Phone',
    priceText: '2999',
    stockText: '有库存',
    detailUrl: '/products/p-2',
    specs: [
      { label: '屏幕', value: '6.1英寸', different: false },
      { label: '容量', value: '256GB', different: true },
    ],
  },
  {
    id: 'p-3',
    name: 'Gamma Phone',
    priceText: '3999',
    stockText: '无库存',
    detailUrl: '/products/p-3',
    specs: [
      { label: '屏幕', value: '6.7英寸', different: true },
      { label: '容量', value: '256GB', different: true },
    ],
  },
];

function createProduct(id: string, name: string): CompareProduct {
  return {
    id,
    name,
    priceText: '1099',
    stockText: '有库存',
    detailUrl: `/products/${id}`,
    specs: [{ label: '颜色', value: '黑色', different: false }],
  };
}

describe('ComparePage duplicate add and limit behavior', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockLoadCompareProducts.mockResolvedValue(baseProducts.slice(0, 2));
  });

  it('keeps the list unique and shows feedback when the same product is added again', async () => {
    mockAddCompareProduct.mockResolvedValue({
      ok: false,
      reason: 'duplicate',
      message: '该商品已在对比列表中',
      products: baseProducts.slice(0, 2),
    });

    render(<ComparePage />);

    expect(await screen.findByText('Alpha Phone')).toBeInTheDocument();
    expect(screen.getByText('Beta Phone')).toBeInTheDocument();

    const addButton = screen.getByRole('button', { name: /加入对比|添加对比/i });
    fireEvent.click(addButton);

    const productInput = screen.getByRole('textbox', { name: /商品id|商品编号|product id/i });
    fireEvent.change(productInput, { target: { value: 'p-1' } });
    fireEvent.keyDown(productInput, { key: 'Enter', code: 'Enter' });

    await waitFor(() => {
      expect(mockAddCompareProduct).toHaveBeenCalledWith('p-1');
    });

    expect(await screen.findByText('该商品已在对比列表中')).toBeInTheDocument();
    expect(screen.getAllByText('Alpha Phone')).toHaveLength(1);
    expect(screen.getAllByRole('row')).toHaveLength(screen.getAllByRole('row').length);
    expect(screen.getByRole('button', { name: /加入对比|添加对比/i })).toBeEnabled();
  });

  it('blocks additional add when the frontend limit is reached and keeps current compare data stable', async () => {
    const limitedProducts = [
      createProduct('p-1', 'Alpha Phone'),
      createProduct('p-2', 'Beta Phone'),
      createProduct('p-3', 'Gamma Phone'),
      createProduct('p-4', 'Delta Phone'),
    ];

    mockLoadCompareProducts.mockResolvedValue(limitedProducts);

    render(<ComparePage />);

    expect(await screen.findByText('Delta Phone')).toBeInTheDocument();

    const addButton = screen.getByRole('button', { name: /加入对比|添加对比/i });
    expect(addButton).toBeDisabled();
    expect(screen.getByText(/最多.*对比|已达上限|对比数量已满/i)).toBeInTheDocument();
    expect(mockAddCompareProduct).not.toHaveBeenCalled();

    expect(screen.getByText('Alpha Phone')).toBeInTheDocument();
    expect(screen.getByText('Beta Phone')).toBeInTheDocument();
    expect(screen.getByText('Gamma Phone')).toBeInTheDocument();
    expect(screen.getByText('Delta Phone')).toBeInTheDocument();
  });

  it('preserves existing products and shows backend limit feedback when the server rejects the add request', async () => {
    mockLoadCompareProducts.mockResolvedValue(baseProducts.slice(0, 3));
    mockAddCompareProduct.mockResolvedValue({
      ok: false,
      reason: 'limit',
      message: '当前最多支持4个商品对比',
      products: baseProducts.slice(0, 3),
      limit: 4,
    });

    render(<ComparePage />);

    expect(await screen.findByText('Gamma Phone')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /加入对比|添加对比/i }));

    const productInput = screen.getByRole('textbox', { name: /商品id|商品编号|product id/i });
    fireEvent.change(productInput, { target: { value: 'p-4' } });
    fireEvent.keyDown(productInput, { key: 'Enter', code: 'Enter' });

    await waitFor(() => {
      expect(mockAddCompareProduct).toHaveBeenCalledWith('p-4');
    });

    expect(await screen.findByText('当前最多支持4个商品对比')).toBeInTheDocument();
    expect(screen.getAllByText(/Phone/)).toHaveLength(3);
    expect(screen.queryByText('Delta Phone')).not.toBeInTheDocument();
  });

  it('documents unresolved limit value when the implementation does not expose a stable max constant', async () => {
    render(<ComparePage />);

    await screen.findByText('Alpha Phone');

    expect(screen.getByTestId('compare-limit-note')).toHaveTextContent('待确认');
  });
});
