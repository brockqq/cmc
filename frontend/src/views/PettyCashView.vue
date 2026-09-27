<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCommunityStore } from '@/stores/communities'
import ExpenseForm from '@/components/ExpenseForm.vue'
import PettyCashLog from '@/components/PettyCashLog.vue'
import ReceiptPreview from '@/components/ReceiptPreview.vue'
import { usePettyCash } from '@/composables/usePettyCash'
import { formatDateTime } from '@/utils/datetime'
import { formatFileSize, formatMoney } from '@/utils/money'

const communities = useCommunityStore()
const route = useRoute()
const router = useRouter()

const options = computed(() => communities.pettyCashCommunities)
const communityId = computed(() => Number(route.query.community) || options.value[0]?.id || null)

const {
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
  replenish: replenishFund,
  saveExpense: persistExpense,
  deleteExpense,
} = usePettyCash(communityId)

/** Same rule as the server: no expenses and nothing to top up means there is nothing to settle. */
const nothingToSettle = computed(
  () => summary.value?.currentPeriod?.expenseCount === 0 && summary.value?.nextDeposit === 0,
)

const message = ref('')
const panel = ref(null) // null | 'record' | 'fund' | 'replenish'
const editingId = ref(null)
const fundInput = ref('')
const replenishNote = ref('')
const busy = ref(false)
const receipt = ref(null) // ReceiptPreview

function selectCommunity(id) {
  router.replace({ query: { community: String(id) } })
}

function showError(text) {
  error.value = text
}

function flash(text) {
  message.value = text
  error.value = ''
}

watch(communityId, () => {
  panel.value = null
  editingId.value = null
  message.value = ''
  receipt.value?.close()
})

function openPanel(name) {
  panel.value = panel.value === name ? null : name
  editingId.value = null
  if (name === 'fund') fundInput.value = summary.value?.fundAmount ?? ''
  if (name === 'replenish') replenishNote.value = ''
}

/** Runs a form action with the busy flag set, reporting failures in the error banner. */
async function withBusy(action) {
  busy.value = true
  try {
    await action()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}

const saveFund = () =>
  withBusy(async () => {
    const first = !summary.value.fundAmount
    const updated = await setFund(Number(fundInput.value))
    if (!updated) return // the user switched community meanwhile
    panel.value = null
    flash(first ? `已設定額度 ${formatMoney(updated.fundAmount)}，第 1 期開始` : '已更新額度，將於下次撥補時生效')
  })

const replenish = () =>
  withBusy(async () => {
    const deposit = summary.value.nextDeposit
    const updated = await replenishFund(replenishNote.value)
    if (!updated) return
    panel.value = null
    flash(`已結算並撥補 ${formatMoney(deposit)}，第 ${updated.currentPeriod.number} 期開始`)
  })

async function saveExpense(existing, payload) {
  const { saved, uploadError } = await persistExpense(existing, payload)
  panel.value = null
  editingId.value = null
  if (uploadError) error.value = `支出已儲存，但單據檔案處理失敗：${uploadError}`
  else flash(existing ? '已更新支出' : `已登錄支出 ${formatMoney(saved.amount)}`)
}

async function removeExpense(e) {
  if (!confirm(`確定要刪除「${e.purpose}」${formatMoney(e.amount)} 這筆支出嗎？`)) return
  try {
    await deleteExpense(e)
    flash('已刪除支出')
  } catch (err) {
    error.value = err.message
  }
}

onMounted(async () => {
  await communities.fetchManageable().catch(() => {})
  // Put the default community in the URL; the id itself doesn't change, so the watcher won't load it
  if (!route.query.community && communityId.value) selectCommunity(communityId.value)
  loadAll()
})
</script>

<template>
  <section class="card">
    <div class="head">
      <h1>零用金</h1>
      <select
        v-if="options.length > 1"
        :value="communityId"
        aria-label="選擇社區"
        @change="selectCommunity($event.target.value)"
      >
        <option v-for="c in options" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
    </div>

    <p v-if="!options.length" class="hint">只有社區管理員與行政委員可以查看零用金。</p>

    <div v-if="error" class="alert error">{{ error }}</div>
    <div v-if="message" class="alert success">{{ message }}</div>

    <template v-if="summary">
      <h2 v-if="options.length <= 1" class="community-name">{{ summary.communityName }}</h2>

      <!-- Not set up yet -->
      <div v-if="summary.fundAmount == null" class="empty">
        <p>此社區尚未設定零用金額度。</p>
        <p v-if="!summary.canManage" class="muted">請社區管理員設定額度後，行政委員即可開始登錄支出。</p>
        <form v-else class="inline-form" @submit.prevent="saveFund">
          <label for="fund-init">每期額度（元）</label>
          <input id="fund-init" v-model="fundInput" type="number" min="1" step="1" required />
          <button class="btn small" :disabled="busy">設定並開始第 1 期</button>
        </form>
      </div>

      <template v-else>
        <div class="stats">
          <div class="stat">
            <span class="label">每期額度</span>
            <strong>{{ formatMoney(summary.fundAmount) }}</strong>
          </div>
          <div class="stat" :class="{ negative: summary.balance < 0 }">
            <span class="label">目前餘額</span>
            <strong>{{ formatMoney(summary.balance) }}</strong>
            <span v-if="summary.balance < 0" class="sub">已超支，下次撥補一併補回</span>
          </div>
          <div class="stat">
            <span class="label">第 {{ summary.currentPeriod.number }} 期支出</span>
            <strong>{{ formatMoney(summary.currentPeriod.expenseTotal) }}</strong>
            <span class="sub">{{ summary.currentPeriod.expenseCount }} 筆</span>
          </div>
        </div>

        <div class="toolbar">
          <button v-if="summary.canRecord" class="btn small" @click="openPanel('record')">登錄支出</button>
          <template v-if="summary.canManage">
            <button
              class="btn small"
              :disabled="nothingToSettle"
              :title="nothingToSettle ? '本期沒有支出，餘額也等於額度，不需要結算' : ''"
              @click="openPanel('replenish')"
            >
              結算並撥補
            </button>
            <button class="btn small secondary" @click="openPanel('fund')">調整額度</button>
          </template>
        </div>

        <ExpenseForm v-if="panel === 'record'" :save="(p) => saveExpense(null, p)" @cancel="panel = null" />

        <form v-if="panel === 'fund'" class="editor" @submit.prevent="saveFund">
          <div class="field">
            <label for="fund-amount">每期額度（元）</label>
            <input id="fund-amount" v-model="fundInput" type="number" min="1" step="1" required />
          </div>
          <p class="muted small">新額度會在下次「結算並撥補」時生效，本期餘額不變。</p>
          <div class="actions">
            <button class="btn" :disabled="busy">儲存</button>
            <button class="btn secondary" type="button" @click="panel = null">取消</button>
          </div>
        </form>

        <form v-if="panel === 'replenish'" class="editor" @submit.prevent="replenish">
          <dl class="calc">
            <dt>每期額度</dt>
            <dd>{{ formatMoney(summary.fundAmount) }}</dd>
            <dt>目前餘額</dt>
            <dd :class="{ neg: summary.balance < 0 }">{{ formatMoney(summary.balance) }}</dd>
            <dt class="total">本次撥補</dt>
            <dd class="total">{{ formatMoney(summary.nextDeposit) }}</dd>
          </dl>
          <p class="muted small">
            結算後第 {{ summary.currentPeriod.number }} 期將無法再修改，並開始第 {{ summary.currentPeriod.number + 1 }} 期，
            餘額回到 {{ formatMoney(summary.fundAmount) }}。
            <template v-if="summary.nextDeposit < 0">（餘額高於額度，差額為繳回。）</template>
          </p>
          <div class="field">
            <label for="rep-note">備註（選填）</label>
            <input id="rep-note" v-model="replenishNote" maxlength="500" placeholder="例如：10 月撥補" />
          </div>
          <div class="actions">
            <button class="btn" :disabled="busy">確認結算並撥補</button>
            <button class="btn secondary" type="button" @click="panel = null">取消</button>
          </div>
        </form>
      </template>
    </template>
    <p v-else-if="loading" class="hint">載入中…</p>
  </section>

  <section v-if="summary?.fundAmount != null" class="card">
    <div class="head">
      <h2 class="section-title">支出明細</h2>
      <select v-model="selectedPeriod" aria-label="選擇期別">
        <option v-for="p in summary.periods" :key="p.number" :value="p.open ? null : p.number">
          第 {{ p.number }} 期{{ p.open ? '（本期）' : '' }}
        </option>
      </select>
    </div>

    <p v-if="viewedPeriod" class="period-info">
      期初撥補 {{ formatMoney(viewedPeriod.deposit) }} · {{ formatDateTime(viewedPeriod.startedAt) }} 由
      {{ viewedPeriod.startedBy }} 開始
      <template v-if="viewedPeriod.note">（{{ viewedPeriod.note }}）</template>
      <template v-if="!viewedPeriod.open">
        · {{ formatDateTime(viewedPeriod.closedAt) }} 由 {{ viewedPeriod.closedBy }} 結算，期末餘額
        <strong :class="{ neg: viewedPeriod.endingBalance < 0 }">{{ formatMoney(viewedPeriod.endingBalance) }}</strong>
      </template>
    </p>

    <p v-if="!expenses.length" class="hint">這一期尚無支出。</p>
    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>日期</th>
            <th>用途</th>
            <th>單據號碼</th>
            <th class="num">金額</th>
            <th>單據</th>
            <th>登錄人</th>
            <th v-if="viewingOpenPeriod && summary.canRecord"></th>
          </tr>
        </thead>
        <tbody>
          <template v-for="e in expenses" :key="e.id">
            <tr v-if="editingId === e.id">
              <td colspan="7" class="edit-cell">
                <ExpenseForm :initial="e" :save="(p) => saveExpense(e, p)" @cancel="editingId = null" />
              </td>
            </tr>
            <tr v-else>
              <td>{{ e.spentOn.replaceAll('-', '/') }}</td>
              <td class="wrap">
                {{ e.purpose }}
                <span v-if="e.payee" class="muted">· {{ e.payee }}</span>
                <div v-if="e.note" class="muted small">{{ e.note }}</div>
              </td>
              <td>{{ e.receiptNo ?? '—' }}</td>
              <td class="num">{{ formatMoney(e.amount) }}</td>
              <td>
                <button v-if="e.attachment" class="link" :title="formatFileSize(e.attachment.size)" @click="receipt.open(e)">
                  查看
                </button>
                <span v-else class="muted">—</span>
              </td>
              <td>
                {{ e.recordedBy }}
                <div v-if="e.updatedBy" class="muted small" :title="formatDateTime(e.updatedAt)">{{ e.updatedBy }} 修改</div>
              </td>
              <td v-if="viewingOpenPeriod && summary.canRecord" class="ops">
                <template v-if="e.canEdit">
                  <button class="link" @click="editingId = e.id; panel = null">編輯</button>
                  <button class="link danger" @click="removeExpense(e)">刪除</button>
                </template>
              </td>
            </tr>
          </template>
        </tbody>
        <tfoot>
          <tr>
            <td colspan="3">合計 {{ expenses.length }} 筆</td>
            <td class="num">{{ formatMoney(expenseTotal) }}</td>
            <td :colspan="viewingOpenPeriod && summary.canRecord ? 3 : 2"></td>
          </tr>
        </tfoot>
      </table>
    </div>
  </section>

  <section v-if="summary?.fundAmount != null" class="card">
    <h2 class="section-title">異動紀錄</h2>
    <p class="muted small intro">這一期所有登錄、修改、刪除與撥補的紀錄，無法更改或刪除。</p>
    <PettyCashLog :entries="log" />
  </section>

  <ReceiptPreview ref="receipt" @error="showError" />
</template>

<style scoped>
.card + .card {
  margin-top: 1.25rem;
}
.community-name {
  margin: -0.5rem 0 1rem;
  font-size: 1rem;
  color: var(--muted);
  font-weight: 400;
}
.section-title {
  margin: 0;
  font-size: 1.1rem;
}
.empty p {
  margin: 0 0 0.75rem;
}
.inline-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}
.inline-form input {
  width: 9rem;
  padding: 0.4rem 0.6rem;
  font: inherit;
  color: var(--text);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}
.stat {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  padding: 0.75rem 1rem;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 8px;
}
.stat .label {
  font-size: 0.85rem;
  color: var(--muted);
}
.stat strong {
  font-size: 1.4rem;
  font-variant-numeric: tabular-nums;
}
.stat .sub {
  font-size: 0.8rem;
  color: var(--muted);
}
.stat.negative {
  border-color: var(--danger);
}
.stat.negative strong,
.stat.negative .sub,
.neg {
  color: var(--danger);
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.calc {
  display: grid;
  grid-template-columns: max-content max-content;
  gap: 0.3rem 1.5rem;
  margin: 0 0 0.75rem;
  font-variant-numeric: tabular-nums;
}
.calc dt {
  color: var(--muted);
}
.calc dd {
  margin: 0;
  text-align: right;
}
.calc .total {
  padding-top: 0.3rem;
  border-top: 1px solid var(--border);
  font-weight: 700;
  color: var(--text);
}
.period-info {
  margin: 0 0 1rem;
  font-size: 0.85rem;
  color: var(--muted);
}
.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
td.wrap {
  white-space: normal;
  min-width: 10rem;
}
tfoot td {
  font-weight: 600;
  border-bottom: none;
}
/* not display:flex — that would take the cell out of the table layout */
.ops {
  white-space: nowrap;
}
.ops button + button {
  margin-left: 0.6rem;
}
.edit-cell {
  white-space: normal;
  padding: 0.75rem 0;
}
.muted {
  color: var(--muted);
}
.intro {
  margin: 0.25rem 0 0.75rem;
}
.small {
  font-size: 0.85rem;
}
</style>
