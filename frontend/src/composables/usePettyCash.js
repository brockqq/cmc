import { computed, ref, watch } from 'vue'
import { api } from '@/api/http'
import { createLatest } from '@/utils/latest'

/**
 * Petty cash data of the community `communityId` (a ref/computed of its id) and the actions on it.
 *
 * Responses are applied only if they are the latest request and still for the selected community, so switching
 * communities quickly can never show one community's figures while an action targets another. Actions target
 * `summary.communityId` — the community whose figures are on screen — rather than the selection.
 */
export function usePettyCash(communityId) {
  const summary = ref(null)
  const expenses = ref([])
  const log = ref([])
  const selectedPeriod = ref(null) // period number; null = open period
  const loading = ref(false)
  const error = ref('')

  const summaryLoads = createLatest()
  const periodLoads = createLatest()
  const allLoads = createLatest()

  const viewedPeriod = computed(() => {
    if (!summary.value?.periods.length) return null
    return selectedPeriod.value == null
      ? summary.value.currentPeriod
      : summary.value.periods.find((p) => p.number === selectedPeriod.value)
  })
  const viewingOpenPeriod = computed(() => viewedPeriod.value?.open ?? false)
  const expenseTotal = computed(() => expenses.value.reduce((sum, e) => sum + e.amount, 0))

  async function loadSummary() {
    const id = communityId.value
    if (!id) return
    const isCurrent = summaryLoads.begin()
    const data = await api.pettyCash.summary(id)
    if (isCurrent() && id === communityId.value) summary.value = data
  }

  /** Expenses and audit trail of the period being viewed. */
  async function loadPeriod() {
    const id = communityId.value
    const isCurrent = periodLoads.begin()
    if (!id || !summary.value?.fundAmount) {
      expenses.value = []
      log.value = []
      return
    }
    const period = selectedPeriod.value ?? undefined
    const [items, entries] = await Promise.all([api.pettyCash.expenses(id, period), api.pettyCash.log(id, period)])
    if (isCurrent() && id === communityId.value) {
      expenses.value = items
      log.value = entries
    }
  }

  const refresh = () => Promise.all([loadSummary(), loadPeriod()])

  async function loadAll() {
    const isCurrent = allLoads.begin()
    loading.value = true
    error.value = ''
    try {
      await loadSummary()
      await loadPeriod()
    } catch (e) {
      if (!isCurrent()) return
      error.value = e.message
      summary.value = null
    } finally {
      if (isCurrent()) loading.value = false
    }
  }

  watch(communityId, () => {
    // Hide the previous community's figures (and the buttons acting on them) until the new one has loaded
    summary.value = null
    expenses.value = []
    log.value = []
    summaryLoads.invalidate()
    periodLoads.invalidate()
    selectedPeriod.value = null
    loadAll()
  })
  watch(selectedPeriod, () => loadPeriod().catch((e) => (error.value = e.message)))

  /**
   * Runs an action against the community on screen; returns false (and changes nothing) if the user switched
   * community while it was in flight.
   */
  async function onShownCommunity(action) {
    const id = summary.value.communityId
    const result = await action(id)
    return id === communityId.value ? result : false
  }

  /** Sets the fund level; returns the new summary, or false if the community changed meanwhile. */
  async function setFund(amount) {
    const updated = await onShownCommunity((id) => api.pettyCash.setFund(id, amount))
    if (updated) {
      summary.value = updated
      await loadPeriod()
    }
    return updated
  }

  /** Closes the open period; returns the new summary, or false if the community changed meanwhile. */
  async function replenish(note) {
    const updated = await onShownCommunity((id) => api.pettyCash.replenish(id, note || null))
    if (updated) {
      summary.value = updated
      selectedPeriod.value = null
      await loadPeriod()
    }
    return updated
  }

  /**
   * Saves an expense's fields, then its receipt file. A failed upload keeps the saved expense:
   * returns { saved, uploadError }.
   */
  async function saveExpense(existing, { data, file, removeAttachment }) {
    const saved = existing
      ? await api.pettyCash.updateExpense(existing.id, data)
      : await api.pettyCash.createExpense(summary.value.communityId, data)
    let uploadError = null
    try {
      if (file) await api.pettyCash.uploadAttachment(saved.id, file)
      else if (removeAttachment) await api.pettyCash.removeAttachment(saved.id)
    } catch (e) {
      uploadError = e.message
    }
    await refresh()
    return { saved, uploadError }
  }

  async function deleteExpense(expense) {
    await api.pettyCash.deleteExpense(expense.id)
    await refresh()
  }

  return {
    summary,
    expenses,
    log,
    selectedPeriod,
    loading,
    error,
    viewedPeriod,
    viewingOpenPeriod,
    expenseTotal,
    loadAll,
    setFund,
    replenish,
    saveExpense,
    deleteExpense,
  }
}
