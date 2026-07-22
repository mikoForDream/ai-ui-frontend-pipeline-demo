import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import React from 'react'

const products = [
  {
    id: 'p-101',
    name: 'Alpha Phone',
    image: 'https://img.example.com/a.png',
    price: '3999',
    stock: 12,
    detailUrl: '/products/p-101',
    attributes: [
      { label: '屏幕', value: '6.1 英寸' },
      { label: '存储', value: '128GB' },
    ],
  },
  {
    id: 'p-102',
    name: 'Beta Phone',
    image: 'https://img.example.com/b.png',
    price: '4299',
    stock: 8,
    detailUrl: '/products/p-102',
    attributes: [
      { label: '屏幕', value: '6.5 英寸' },
      { label: '存储', value: '256GB' },
    ],
  },
  {
    id: 'p-103',
    name: 'Gamma Phone',
    image: 'https://img.example.com/c.png',
    price: '4699',
    stock: 0,
    detailUrl: '/products/p-103',
    attributes: [
      { label: '屏幕', value: '6.7 英寸' },
      { label: '存储', value: '256GB' },
    ],
  },
  {
    id: 'p-104',
    name: 'Delta Phone',
    image: 'https://img.example.com/d.png',
    price: '4999',
    stock: 18,
    detailUrl: '/products/p-104',
    attributes: [
      { label: '屏幕', value: '6.8 英寸' },
      { label: '存储', value: '512GB' },
    ],
  },
]

type Product = (typeof products)[number]

function ProductCompareTestPage() {
  const [compareIds, setCompareIds] = React.useState<string[]>([])
  const compareProducts = compareIds
    .map((id) => products.find((item) => item.id === id))
    .filter(Boolean) as Product[]

  const addToCompare = (product: Product) => {
    setCompareIds((current) => {
      if (current.includes(product.id)) return current
      return [...current, product.id]
    })
  }

  return (
    <div>
      <section aria-label="商品卡片列表">
        {products.map((product) => {
          const added = compareIds.includes(product.id)
          return (
            <article key={product.id} data-testid={`product-card-${product.id}`}>
              <img alt={product.name} src={product.image} />
              <h3>{product.name}</h3>
              <button
                type="button"
                onClick={() => addToCompare(product)}
                disabled={added}
                aria-label={`${product.name}${added ? '已加入对比' : '加入对比'}`}
              >
                {added ? '已加入对比' : '加入对比'}
              </button>
            </article>
          )
        })}
      </section>

      <aside aria-label="对比列表" data-testid="compare-list">
        {compareProducts.map((product) => (
          <div key={product.id} data-testid="compare-list-item">
            <img alt={`${product.name} 对比图`} src={product.image} />
            <span>{product.name}</span>
          </div>
        ))}
      </aside>

      <main aria-label="商品对比主区域" data-testid="compare-main">
        <section aria-label="基础信息区">
          {compareProducts.map((product) => (
            <article key={product.id} data-testid="compare-main-product">
              <img alt={`${product.name} 主图`} src={product.image} />
              <h4>{product.name}</h4>
              <p>价格: {product.price}</p>
              <p>库存: {product.stock}</p>
              <a href={product.detailUrl}>查看详情</a>
            </article>
          ))}
        </section>

        <section aria-label="属性对比区">
          {compareProducts.map((product) => (
            <div key={product.id} data-testid={`attribute-column-${product.id}`}>
              {product.attributes.map((attribute) => (
                <p key={attribute.label}>
                  {attribute.label}: {attribute.value}
                </p>
              ))}
            </div>
          ))}
        </section>
      </main>
    </div>
  )
}

describe('商品对比页面 - 商品添加主流程', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('添加 2 个不同商品后，对比列表和主区域同步更新，按钮状态切换正确', async () => {
    render(<ProductCompareTestPage />)

    fireEvent.click(screen.getByRole('button', { name: 'Alpha Phone加入对比' }))
    fireEvent.click(screen.getByRole('button', { name: 'Beta Phone加入对比' }))

    await waitFor(() => {
      expect(screen.getAllByTestId('compare-list-item')).toHaveLength(2)
      expect(screen.getAllByTestId('compare-main-product')).toHaveLength(2)
    })

    const compareList = screen.getByTestId('compare-list')
    const listItems = within(compareList).getAllByTestId('compare-list-item')
    expect(listItems[0]).toHaveTextContent('Alpha Phone')
    expect(listItems[1]).toHaveTextContent('Beta Phone')

    expect(screen.getByAltText('Alpha Phone 对比图')).toBeInTheDocument()
    expect(screen.getByAltText('Beta Phone 对比图')).toBeInTheDocument()

    const mainProducts = screen.getAllByTestId('compare-main-product')
    expect(mainProducts[0]).toHaveTextContent('Alpha Phone')
    expect(mainProducts[0]).toHaveTextContent('价格: 3999')
    expect(mainProducts[0]).toHaveTextContent('库存: 12')
    expect(mainProducts[1]).toHaveTextContent('Beta Phone')
    expect(mainProducts[1]).toHaveTextContent('价格: 4299')
    expect(mainProducts[1]).toHaveTextContent('库存: 8')

    expect(screen.getByTestId('attribute-column-p-101')).toHaveTextContent('屏幕: 6.1 英寸')
    expect(screen.getByTestId('attribute-column-p-102')).toHaveTextContent('存储: 256GB')

    expect(screen.getByRole('button', { name: 'Alpha Phone已加入对比' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Beta Phone已加入对比' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Gamma Phone加入对比' })).toBeEnabled()
  })

  it('添加 4 个不同商品后，列表顺序、主区域列数和按钮状态保持一致', async () => {
    render(<ProductCompareTestPage />)

    for (const product of products) {
      fireEvent.click(screen.getByRole('button', { name: `${product.name}加入对比` }))
    }

    await waitFor(() => {
      expect(screen.getAllByTestId('compare-list-item')).toHaveLength(4)
      expect(screen.getAllByTestId('compare-main-product')).toHaveLength(4)
    })

    const compareList = screen.getByTestId('compare-list')
    const listItems = within(compareList).getAllByTestId('compare-list-item')
    expect(listItems.map((node) => node.textContent)).toEqual([
      expect.stringContaining('Alpha Phone'),
      expect.stringContaining('Beta Phone'),
      expect.stringContaining('Gamma Phone'),
      expect.stringContaining('Delta Phone'),
    ])

    expect(screen.getByText('价格: 4699')).toBeInTheDocument()
    expect(screen.getByText('库存: 0')).toBeInTheDocument()
    expect(screen.getByText('价格: 4999')).toBeInTheDocument()
    expect(screen.getByText('库存: 18')).toBeInTheDocument()

    expect(screen.getByRole('link', { name: '查看详情' })).toBeInTheDocument()

    for (const product of products) {
      expect(screen.getByRole('button', { name: `${product.name}已加入对比` })).toBeDisabled()
    }
  })
})
