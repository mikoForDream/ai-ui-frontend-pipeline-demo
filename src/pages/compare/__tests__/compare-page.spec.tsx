import { describe, expect, it } from 'vitest'
import { renderComparePage, compareScenarios } from '../../../test/compare-page'

describe('ComparePage test scaffold', () => {
  describe('main flow', () => {
    it('boots with compare page context and seed data', async () => {
      const view = await renderComparePage({
        scenario: compareScenarios.happyPath,
      })

      expect(view.context.route).toBe('/compare')
      expect(view.context.products).toHaveLength(2)
      expect(view.screen.getByTestId('compare-page-root')).toBeTruthy()
    })
  })

  describe('component interaction', () => {
    it('supports mock switching for duplicate and max-limit cases', async () => {
      const duplicateView = await renderComparePage({
        scenario: compareScenarios.duplicateBlocked,
      })
      expect(duplicateView.context.api.addProduct.code).toBe('DUPLICATE')

      const limitView = await renderComparePage({
        scenario: compareScenarios.overLimitBlocked,
      })
      expect(limitView.context.api.addProduct.code).toBe('LIMIT_EXCEEDED')
    })
  })

  describe('state scenarios', () => {
    it('supports loading, empty and error state initialization', async () => {
      const loadingView = await renderComparePage({
        scenario: compareScenarios.loading,
      })
      expect(loadingView.context.state).toBe('loading')

      const emptyView = await renderComparePage({
        scenario: compareScenarios.empty,
      })
      expect(emptyView.context.products).toHaveLength(0)

      const errorView = await renderComparePage({
        scenario: compareScenarios.error,
      })
      expect(errorView.context.error?.message).toContain('compare')
    })
  })

  describe('risk notes', () => {
    it('records missing selectors or page object dependencies for follow-up', () => {
      expect([
        'Page test uses provisional test ids: compare-page-root and compare-page-loading.',
        'If the production page does not expose stable selectors, follow-up work must add non-visual test hooks.',
        'Route jump assertions currently depend on injected navigation spy instead of a real page-level detail link selector.',
      ]).toMatchInlineSnapshot(`
        [
          "Page test uses provisional test ids: compare-page-root and compare-page-loading.",
          "If the production page does not expose stable selectors, follow-up work must add non-visual test hooks.",
          "Route jump assertions currently depend on injected navigation spy instead of a real page-level detail link selector.",
        ]
      `)
    })
  })
})
