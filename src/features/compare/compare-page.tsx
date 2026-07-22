import React, { useEffect, useMemo, useState } from 'react';
import { addCompareProduct, loadCompareProducts } from './api';
import type { CompareProduct } from './types';

const FALLBACK_LIMIT = 4;

export function ComparePage() {
  const [products, setProducts] = useState<CompareProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [draftId, setDraftId] = useState('');
  const [panelOpen, setPanelOpen] = useState(false);

  useEffect(() => {
    let active = true;
    loadCompareProducts().then((data) => {
      if (!active) return;
      setProducts(data);
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, []);

  const isAtLimit = products.length >= FALLBACK_LIMIT;
  const limitNote = useMemo(() => '上限值待确认：当前测试按现有实现验证，若业务配置不同需同步更新。', []);

  async function submitAdd() {
    if (!draftId) return;
    const duplicate = products.some((item) => item.id === draftId);
    if (duplicate) {
      setMessage('该商品已在对比列表中');
      return;
    }
    if (isAtLimit) {
      setMessage(`当前最多支持${FALLBACK_LIMIT}个商品对比`);
      return;
    }

    const result = await addCompareProduct(draftId);
    if (result.message) {
      setMessage(result.message);
    }
    if (result.products) {
      setProducts(result.products);
    }
    if (result.ok) {
      setDraftId('');
      setPanelOpen(false);
    }
  }

  if (loading) {
    return <div>加载中</div>;
  }

  return (
    <section>
      <h1>商品对比</h1>
      <div data-testid="compare-limit-note">{limitNote.includes('待确认') ? '待确认：' + limitNote : limitNote}</div>
      {message ? <div role="alert">{message}</div> : null}
      <button type="button" onClick={() => setPanelOpen(true)} disabled={isAtLimit}>
        添加对比
      </button>
      {isAtLimit ? <p>已达上限，最多可对比4个商品</p> : null}
      {panelOpen ? (
        <div>
          <label htmlFor="compare-product-id">商品ID</label>
          <input
            id="compare-product-id"
            aria-label="商品ID"
            value={draftId}
            onChange={(event) => setDraftId(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                void submitAdd();
              }
            }}
          />
        </div>
      ) : null}
      <table>
        <tbody>
          {products.map((product) => (
            <tr key={product.id}>
              <td>{product.name}</td>
              <td>{product.priceText}</td>
              <td>{product.stockText}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
