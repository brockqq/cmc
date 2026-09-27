import { beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick, ref } from 'vue'
import { api } from '@/api/http'
import { usePettyCash } from './usePettyCash'

vi.mock('@/api/http', () => ({
  api: {
    pettyCash: {
      summary: vi.fn(),
      expenses: vi.fn(),
      log: vi.fn(),
      setFund: vi.fn(),
      replenish: vi.fn(),
      createExpense: vi.fn(),
      updateExpense: vi.fn(),
      deleteExpense: vi.fn(),
      uploadAttachment: vi.fn(),
      removeAttachment: vi.fn(),
    },
  },
}))

/** A promise the test resolves by hand, to control the order responses arrive in. */
function deferred() {
  let resolve
  const promise = new Promise((r) => (resolve = r))
  return { promise, resolve }
}

const summaryOf = (communityId, fundAmount) => ({
  communityId,
  fundAmount,
  balance: fundAmount,
  nextDeposit: 0,
  currentPeriod: { number: 1, open: true },
  periods: [{ number: 1, open: true }],
})

const flush = async () => {
  for (let i = 0; i < 5; i++) await nextTick()
}

function setup(initialId) {
  const communityId = ref(initialId)
  const scope = effectScope()
  const cash = scope.run(() => usePettyCash(communityId))
  return { communityId, cash, scope }
}

beforeEach(() => {
  vi.resetAllMocks()
  api.pettyCash.expenses.mockResolvedValue([])
  api.pettyCash.log.mockResolvedValue([])
})

describe('usePettyCash', () => {
  it('ignores a slow response for a community the user already switched away from', async () => {
    const slow = deferred()
    api.pettyCash.summary.mockImplementation((id) => (id === 1 ? slow.promise : Promise.resolve(summaryOf(2, 3333))))
    const { communityId, cash } = setup(1)

    cash.loadAll()
    communityId.value = 2 // switch while community 1 is still loading
    await flush()
    expect(cash.summary.value.communityId).toBe(2)

    slow.resolve(summaryOf(1, 1111)) // community 1's answer finally arrives
    await flush()
    expect(cash.summary.value.communityId).toBe(2)
    expect(cash.summary.value.fundAmount).toBe(3333)
  })

  it('clears the figures as soon as the community changes', async () => {
    api.pettyCash.summary.mockImplementation((id) =>
      id === 1 ? Promise.resolve(summaryOf(1, 1111)) : new Promise(() => {}),
    )
    const { communityId, cash } = setup(1)
    await cash.loadAll()
    expect(cash.summary.value.fundAmount).toBe(1111)

    communityId.value = 2
    await nextTick()
    expect(cash.summary.value).toBeNull() // nothing (and no buttons) until community 2 has loaded
  })

  it('replenishes the community on screen and drops the result if the user moved on', async () => {
    api.pettyCash.summary.mockImplementation((id) => Promise.resolve(summaryOf(id, 5000)))
    const pending = deferred()
    api.pettyCash.replenish.mockReturnValue(pending.promise)
    const { communityId, cash } = setup(1)
    await cash.loadAll()

    const result = cash.replenish('十月')
    expect(api.pettyCash.replenish).toHaveBeenCalledWith(1, '十月')
    communityId.value = 2
    await flush()
    pending.resolve(summaryOf(1, 9999))

    expect(await result).toBe(false)
    expect(cash.summary.value.communityId).toBe(2)
  })

  it('loads expenses and the audit trail of the selected period', async () => {
    api.pettyCash.summary.mockResolvedValue(summaryOf(1, 5000))
    const { cash } = setup(1)
    await cash.loadAll()
    expect(api.pettyCash.expenses).toHaveBeenLastCalledWith(1, undefined)

    api.pettyCash.log.mockResolvedValue([{ id: 9, action: 'REPLENISHED' }])
    cash.selectedPeriod.value = 3
    await flush()
    expect(api.pettyCash.expenses).toHaveBeenLastCalledWith(1, 3)
    expect(api.pettyCash.log).toHaveBeenLastCalledWith(1, 3)
    expect(cash.log.value).toEqual([{ id: 9, action: 'REPLENISHED' }])
  })

  it('keeps a saved expense when the receipt upload fails', async () => {
    api.pettyCash.summary.mockResolvedValue(summaryOf(1, 5000))
    api.pettyCash.createExpense.mockResolvedValue({ id: 5, amount: 100 })
    api.pettyCash.uploadAttachment.mockRejectedValue(new Error('檔案不可超過 5MB'))
    const { cash } = setup(1)
    await cash.loadAll()

    const { saved, uploadError } = await cash.saveExpense(null, { data: { amount: 100 }, file: new Blob(['x']) })
    expect(api.pettyCash.createExpense).toHaveBeenCalledWith(1, { amount: 100 })
    expect(saved.id).toBe(5)
    expect(uploadError).toBe('檔案不可超過 5MB')
  })
})
