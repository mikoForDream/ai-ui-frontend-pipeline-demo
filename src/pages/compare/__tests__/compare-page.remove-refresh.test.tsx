import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';

type CompareItem = {
  id: string;
  name: string;
  price: string;
  stock: number;
  detailUrl: string;
  specs: Record<string, string>;
};

type ComparePageProps = {
  maxCount?: number;
  fetchCompareList: () => Promise<CompareItem[]>;
  removeCompareItem: (id: string) => Promise<void>;
};

function ComparePage({
  maxCount = 4,
  fetchCompareList,
  removeCompareItem,
}: ComparePageProps) {
  const [items, setItems] = React.useState<CompareItem[]>([]);
  const [loading, setLoading] = React.useState(true);

  const load = React.useCallback(async () => {
    setLoading(true);
    const next = await fetchCompareList();
    setItems(next);
    setLoading(false);
  }, [fetchCompareList]);

  React.useEffect(() => {
    void load();
  }, [load]);

  const handleRemove = async (id: string) => {
    await removeCompareItem(id);
    await load();
  };

  if (loading) {
    return <div aria-label="compare-loading">loading</div>;
  }

  const placeholders = Math.max(maxCount - items.length, 0);
  const canAddMore = items.length < maxCount;

  return (
    <div>
      <section aria-label="compare-list">
        {items.map((item) => (
          <article key={item.id} data-testid="compare-card">
            <h2>{item.name}</h2>
            <p>{item.price}</p>
            <button onClick={() => void handleRemove(item.id)} type="button">
              移除{item.name}
            </button>
          </article>
        ))}
        {Array.from({ length: placeholders }).map((_, index) => (
          <div data-testid="compare-placeholder" key={`placeholder-${index}`}>
            待添加商品
          </div>
        ))}
      </section>

      <button disabled={!canAddMore} type="button">
        添加商品
      </button>

      <section aria-label="compare-main-area">
        {items.map((item) => (
          <div data-testid="compare-main-column" key={`main-${item.id}`}>
            <h3>{item.name}</h3>
            <span>{item.specs.color}</span>
            <span>{item.specs.memory}</span>
          </div>
        ))}
      </section>
    </div>
  );
}

const initialList: CompareItem[] = [
  {
    id: 'sku-1',
    name: 'Alpha Phone',
    price: '3999',
    stock: 20,
    detailUrl: '/detail/sku-1',
    specs: { color: 'black', memory: '128G' },
  },
  {
    id: 'sku-2',
    name: 'Bravo Phone',
    price: '4299',
    stock: 18,
    detailUrl: '/detail/sku-2',
    specs: { color: 'silver', memory: '256G' },
  },
  {
    id: 'sku-3',
    name: 'Comet Phone',
    price: '4599',
    stock: 9,
    detailUrl: '/detail/sku-3',
    specs: { color: 'blue', memory: '512G' },
  },
  {
    id: 'sku-4',
    name: 'Delta Phone',
    price: '4999',
    stock: 4,
    detailUrl: '/detail/sku-4',
    specs: { color: 'gold', memory: '1T' },
  },
];

const refreshedList: CompareItem[] = [
  {
    id: 'sku-3',
    name: 'Comet Phone',
    price: '4599',
    stock: 9,
    detailUrl: '/detail/sku-3',
    specs: { color: 'blue', memory: '512G' },
  },
  {
    id: 'sku-1',
    name: 'Alpha Phone',
    price: '3999',
    stock: 20,
    detailUrl: '/detail/sku-1',
    specs: { color: 'black', memory: '128G' },
  },
  {
    id: 'sku-4',
    name: 'Delta Phone',
    price: '4999',
    stock: 4,
    detailUrl: '/detail/sku-4',
    specs: { color: 'gold', memory: '1T' },
  },
];

describe('compare page remove and refresh linkage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('removes one item, refreshes compare data, and renders the refreshed order consistently', async () => {
    const fetchCompareList = vi
      .fn<[], Promise<CompareItem[]>>()
      .mockResolvedValueOnce(initialList)
      .mockResolvedValueOnce(refreshedList);
    const removeCompareItem = vi.fn<[string], Promise<void>>().mockResolvedValue();

    render(
      <ComparePage
        fetchCompareList={fetchCompareList}
        removeCompareItem={removeCompareItem}
      />,
    );

    expect(await screen.findByText('Alpha Phone')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '添加商品' })).toBeDisabled();

    await userEvent.click(screen.getByRole('button', { name: '移除Bravo Phone' }));

    await waitFor(() => {
      expect(removeCompareItem).toHaveBeenCalledWith('sku-2');
      expect(fetchCompareList).toHaveBeenCalledTimes(2);
    });

    await waitFor(() => {
      expect(screen.queryByText('Bravo Phone')).not.toBeInTheDocument();
    });

    const cards = screen.getAllByTestId('compare-card');
    expect(cards).toHaveLength(3);
    expect(within(cards[0]).getByText('Comet Phone')).toBeInTheDocument();
    expect(within(cards[1]).getByText('Alpha Phone')).toBeInTheDocument();
    expect(within(cards[2]).getByText('Delta Phone')).toBeInTheDocument();

    const mainColumns = screen.getAllByTestId('compare-main-column');
    expect(mainColumns).toHaveLength(3);
    expect(within(mainColumns[0]).getByText('Comet Phone')).toBeInTheDocument();
    expect(within(mainColumns[1]).getByText('Alpha Phone')).toBeInTheDocument();
    expect(within(mainColumns[2]).getByText('Delta Phone')).toBeInTheDocument();
  });

  it('restores placeholder and add-entry state after removing one item from a full compare list', async () => {
    const fetchCompareList = vi
      .fn<[], Promise<CompareItem[]>>()
      .mockResolvedValueOnce(initialList)
      .mockResolvedValueOnce(refreshedList);
    const removeCompareItem = vi.fn<[string], Promise<void>>().mockResolvedValue();

    render(
      <ComparePage
        fetchCompareList={fetchCompareList}
        removeCompareItem={removeCompareItem}
      />,
    );

    expect(await screen.findByText('Delta Phone')).toBeInTheDocument();
    const addButton = screen.getByRole('button', { name: '添加商品' });
    expect(addButton).toBeDisabled();
    expect(screen.queryAllByTestId('compare-placeholder')).toHaveLength(0);

    await userEvent.click(screen.getByRole('button', { name: '移除Bravo Phone' }));

    await waitFor(() => {
      expect(screen.getAllByTestId('compare-placeholder')).toHaveLength(1);
    });

    expect(addButton).toBeEnabled();
    expect(screen.getByText('待添加商品')).toBeInTheDocument();
  });

  it('does not leave stale removed product data after refresh', async () => {
    const fetchCompareList = vi
      .fn<[], Promise<CompareItem[]>>()
      .mockResolvedValueOnce(initialList)
      .mockResolvedValueOnce(refreshedList);
    const removeCompareItem = vi.fn<[string], Promise<void>>().mockResolvedValue();

    render(
      <ComparePage
        fetchCompareList={fetchCompareList}
        removeCompareItem={removeCompareItem}
      />,
    );

    expect(await screen.findByText('silver')).toBeInTheDocument();
    expect(screen.getByText('256G')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '移除Bravo Phone' }));

    await waitFor(() => {
      expect(screen.queryByText('Bravo Phone')).not.toBeInTheDocument();
    });

    expect(screen.queryByText('silver')).not.toBeInTheDocument();
    expect(screen.queryByText('256G')).not.toBeInTheDocument();
    expect(screen.getAllByTestId('compare-card').map((node) => node.textContent)).toEqual([
      expect.stringContaining('Comet Phone'),
      expect.stringContaining('Alpha Phone'),
      expect.stringContaining('Delta Phone'),
    ]);
  });
});
