# compare_test_010 / Step 6

## Scope

This step covers remove-and-refresh linkage on the product compare page.

## Target behavior

1. Removing one product triggers compare data refresh.
2. The refreshed response fully drives the rendered compare list.
3. Remaining product order matches the refreshed response order.
4. Placeholder slots update after removal.
5. The add-entry state becomes available again after the list shrinks below the max limit.
6. No stale product name, stale spec cell, or stale summary block remains on screen.
7. The page does not throw when refresh succeeds with fewer items.

## Required test scenarios

### Scenario A: remove item and refresh list
- Prepare initial compare payload with 4 products.
- Mock remove request success.
- Mock refresh request returning 3 products in a new explicit order.
- Assert the removed product disappears.
- Assert the remaining rendered order follows the refresh payload.
- Assert the compare main area also updates to the same 3 products.

### Scenario B: placeholder and add state recovery
- Start from full compare state.
- Remove one product.
- After refresh, assert one placeholder slot is visible.
- Assert add button or add entry is enabled again.
- Assert max-limit disabled state is cleared.

### Scenario C: no stale data after refresh
- Initial payload contains a unique title/spec/price block for the removed item.
- Refresh payload excludes that item.
- Assert those unique texts are absent after refresh.
- Assert there is no duplicated remaining product card.

## Execution notes

- Use the existing compare page test file and the same mock strategy already used by the repo.
- If the implementation uses re-fetch after remove, wait on the refreshed request completion instead of only checking optimistic UI.
- If ordering rules are undocumented, assert only the order returned by the mocked refresh payload.
- Do not add new business rules in the test.

## Acceptance mapping

- "对比列表能够刷新为最新数据": covered by Scenario A
- "剩余商品顺序、占位内容和主区域展示与刷新结果一致": covered by Scenario A and B
- "可继续添加商品的按钮或入口状态能够正确恢复": covered by Scenario B
- "未出现旧数据残留、顺序错乱或页面报错": covered by Scenario C
