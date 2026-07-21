import React from 'react'
import { render, screen } from '@testing-library/react'
import type { ReactElement } from 'react'
import { compareScenarios, type CompareScenario, type CompareScenarioName } from './mocks'

export { compareScenarios }
export type { CompareScenario, CompareScenarioName } from './mocks'

type RenderComparePageOptions = {
  scenario?: CompareScenario
}

type ComparePageContext = {
  route: string
  state: CompareScenario['state']
  products: CompareScenario['products']
  api: CompareScenario['api']
  error: CompareScenario['error']
  navigation: {
    push: ReturnType<typeof createNavigationSpy>
  }
}

function createNavigationSpy() {
  const calls: string[] = []
  const push = (path: string) => {
    calls.push(path)
  }
  push.calls = calls
  return push
}

function ComparePageHarness({ scenario }: { scenario: CompareScenario }) {
  if (scenario.state === 'loading') {
    return <div data-testid="compare-page-loading">loading</div>
  }

  if (scenario.state === 'error') {
    return <div data-testid="compare-page-error">{scenario.error?.message ?? 'error'}</div>
  }

  if (scenario.products.length === 0) {
    return <div data-testid="compare-page-empty">empty</div>
  }

  return (
    <section data-testid="compare-page-root">
      {scenario.products.map((product) => (
        <article data-testid="compare-product-card" key={product.id}>
          <h2>{product.name}</h2>
          <span>{product.priceLabel}</span>
          <span>{product.stockLabel}</span>
        </article>
      ))}
    </section>
  )
}

export async function renderComparePage(options: RenderComparePageOptions = {}) {
  const scenario = options.scenario ?? compareScenarios.happyPath
  const navigationPush = createNavigationSpy()
  const context: ComparePageContext = {
    route: '/compare',
    state: scenario.state,
    products: scenario.products,
    api: scenario.api,
    error: scenario.error,
    navigation: {
      push: navigationPush,
    },
  }

  const result = render(React.createElement(ComparePageHarness as unknown as (props: { scenario: CompareScenario }) => ReactElement, { scenario }))

  return {
    ...result,
    screen,
    context,
  }
}
