// MS Tracker — Core Application Controller & Event Router

import { dbEngine } from './db.js';
import { Money } from './money.js';
import { Components } from './components.js';
import { Analytics } from './analytics.js';

let appState = {
  accounts: [],
  transactions: [],
  categories: [],
  tags: [],
  goals: [],
  recurring: [],
  smsQueue: [],
  smsRules: [],
  settings: {},
  currentTab: 'dashboard',
  currentTxFilter: 'all',
  currentAnalyticsSubtab: 'overview'
};

// Native Android Experience Helper Utilities
const AndroidNative = {
  async triggerHaptic(style = 'LIGHT') {
    if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.Haptics) {
      try {
        await window.Capacitor.Plugins.Haptics.impact({ style });
      } catch (e) {}
    } else if (typeof navigator !== 'undefined' && navigator.vibrate) {
      navigator.vibrate(10);
    }
  },

  async showToast(text) {
    if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.Toast) {
      try {
        await window.Capacitor.Plugins.Toast.show({ text, duration: 'short' });
        return;
      } catch (e) {}
    }
    console.log('[Toast]', text);
  },

  async shareData(title, text, url) {
    if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.Share) {
      try {
        await window.Capacitor.Plugins.Share.share({ title, text, url, dialogTitle: title });
        return true;
      } catch (e) {}
    }
    if (typeof navigator !== 'undefined' && navigator.share) {
      try {
        await navigator.share({ title, text, url });
        return true;
      } catch (e) {}
    }
    return false;
  },

  async hideSplashScreen() {
    if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.SplashScreen) {
      try {
        await window.Capacitor.Plugins.SplashScreen.hide();
      } catch (e) {}
    }
  },

  initNetworkMonitoring() {
    if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.Network) {
      window.Capacitor.Plugins.Network.addListener('networkStatusChange', (status) => {
        if (!status.connected) {
          AndroidNative.showToast('Offline Mode: Data stored locally on device');
        }
      });
    } else {
      window.addEventListener('offline', () => {
        AndroidNative.showToast('Offline Mode: Data stored locally on device');
      });
    }
  }
};

function triggerCelebration() {
  const canvas = document.getElementById('celebration-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;
  canvas.style.position = 'fixed';
  canvas.style.top = '0';
  canvas.style.left = '0';
  canvas.style.pointerEvents = 'none';
  canvas.style.zIndex = '9999';

  const particles = [];
  const colors = ['#3B7A57', '#D87D56', '#8F9C8A', '#D8A47F', '#5C6757', '#FFD700'];
  for (let i = 0; i < 80; i++) {
    particles.push({
      x: canvas.width / 2,
      y: canvas.height / 2,
      vx: (Math.random() - 0.5) * 12,
      vy: (Math.random() - 0.7) * 14,
      size: Math.random() * 8 + 4,
      color: colors[Math.floor(Math.random() * colors.length)],
      alpha: 1
    });
  }

  function update() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    let active = false;
    particles.forEach(p => {
      p.x += p.vx;
      p.y += p.vy;
      p.vy += 0.3;
      p.alpha -= 0.015;
      if (p.alpha > 0) {
        active = true;
        ctx.globalAlpha = p.alpha;
        ctx.fillStyle = p.color;
        ctx.fillRect(p.x, p.y, p.size, p.size);
      }
    });
    if (active) requestAnimationFrame(update);
    else ctx.clearRect(0, 0, canvas.width, canvas.height);
  }
  update();
}

function setupPullToRefresh() {
  const container = document.getElementById('main-content-scroll');
  if (!container) return;

  let startY = 0;
  let isPulling = false;

  container.addEventListener('touchstart', (e) => {
    if (container.scrollTop === 0) {
      startY = e.touches[0].clientY;
      isPulling = true;
    }
  }, { passive: true });

  container.addEventListener('touchend', (e) => {
    if (!isPulling) return;
    isPulling = false;
    const endY = e.changedTouches[0]?.clientY || 0;
    if (endY - startY > 90 && container.scrollTop === 0) {
      AndroidNative.triggerHaptic('LIGHT');
      loadStateFromDB().then(() => {
        renderCurrentTab();
        AndroidNative.showToast('MS Tracker Refreshed');
      });
    }
  });
}

// Initialize Application Engine
async function initApp() {
  await dbEngine.init();
  await dbEngine.seedDefaultsIfEmpty();
  await loadStateFromDB();

  setupEventListeners();
  setupPullToRefresh();
  AndroidNative.initNetworkMonitoring();
  renderCurrentTab();

  // Hide Splash Screen cleanly after engine is ready
  await AndroidNative.hideSplashScreen();
}

async function loadStateFromDB() {
  appState.accounts = await dbEngine.getAll('accounts');
  appState.transactions = await dbEngine.getAll('transactions');
  appState.categories = await dbEngine.getAll('categories');
  appState.tags = await dbEngine.getAll('tags');
  appState.goals = await dbEngine.getAll('goals');
  appState.recurring = await dbEngine.getAll('recurring');
  appState.smsQueue = await dbEngine.getAll('smsQueue');
  appState.smsRules = await dbEngine.getAll('smsRules');
  const settingsArr = await dbEngine.getAll('settings');
  appState.settings = settingsArr.find(s => s.key === 'global') || {};
}

// Global Event Routing
function setupEventListeners() {
  // Bottom Nav Tabs
  document.querySelectorAll('.nav-tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const tab = btn.dataset.tab;
      switchTab(tab);
    });
  });

  // FAB Add Button
  document.getElementById('btn-fab-add')?.addEventListener('click', () => {
    openAddTransactionSheet('expense');
  });

  // Dashboard Shortcuts
  document.getElementById('btn-sms-queue-shortcut')?.addEventListener('click', openSMSQueueSheet);
  document.getElementById('btn-hero-inbox')?.addEventListener('click', openSMSQueueSheet);
  document.getElementById('btn-hero-add')?.addEventListener('click', () => openAddTransactionSheet('expense'));
  document.getElementById('btn-hero-transfer')?.addEventListener('click', () => openAddTransactionSheet('transfer'));
  document.getElementById('btn-budget-details')?.addEventListener('click', openBudgetDetailsSheet);
  document.getElementById('btn-manage-accounts')?.addEventListener('click', openManageAccountsSheet);
  document.getElementById('btn-view-full-ledger')?.addEventListener('click', () => switchTab('transactions'));
  document.getElementById('btn-create-goal')?.addEventListener('click', openCreateGoalSheet);

  // Filter Button Shortcut on Transactions Screen
  document.getElementById('btn-open-filters')?.addEventListener('click', openFiltersSheet);

  // Settings Shortcuts
  document.getElementById('btn-sett-install-app')?.addEventListener('click', promptPWAInstall);
  document.getElementById('btn-sett-accounts')?.addEventListener('click', openManageAccountsSheet);
  document.getElementById('btn-sett-categories')?.addEventListener('click', openManageCategoriesSheet);
  document.getElementById('btn-sett-export')?.addEventListener('click', exportCSVLedger);
  document.getElementById('btn-sett-backup')?.addEventListener('click', downloadJSONBackup);

  // Analytics Subtabs
  document.querySelectorAll('.analytics-subtab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const sub = btn.dataset.anab;
      switchAnalyticsSubtab(sub);
    });
  });

  // Search input live filtering
  document.getElementById('tx-search-input')?.addEventListener('input', (e) => {
    renderTransactionsView(e.target.value.trim().toLowerCase());
  });

  // Sheet backdrop dismissal
  document.getElementById('sheet-backdrop')?.addEventListener('click', (e) => {
    if (e.target.id === 'sheet-backdrop') closeSheet();
  });
}

function switchTab(tabId) {
  appState.currentTab = tabId;

  document.querySelectorAll('.app-screen').forEach(s => s.classList.add('hidden'));
  const target = document.getElementById(`screen-${tabId}`);
  if (target) target.classList.remove('hidden');

  document.querySelectorAll('.nav-tab-btn').forEach(btn => {
    if (btn.dataset.tab === tabId) {
      btn.className = 'nav-tab-btn flex-1 h-full flex flex-col items-center justify-center text-[#1B4332] font-bold';
    } else {
      btn.className = 'nav-tab-btn flex-1 h-full flex flex-col items-center justify-center text-[#7C8079] font-medium';
    }
  });

  renderCurrentTab();
}

function renderCurrentTab() {
  if (appState.currentTab === 'dashboard') renderDashboardView();
  else if (appState.currentTab === 'transactions') renderTransactionsView();
  else if (appState.currentTab === 'goals') renderGoalsView();
  else if (appState.currentTab === 'analytics') renderAnalyticsView();
  else if (appState.currentTab === 'settings') renderSettingsView();
}

// 1. Render Dashboard (Emerald Net Worth Hero)
function renderDashboardView() {
  let netWorthMinor = 0;
  let monthIncMinor = 0;
  let monthExpMinor = 0;

  const accountBalances = {};
  appState.accounts.forEach(a => accountBalances[a.id] = a.startingBalanceMinor || 0);

  appState.transactions.forEach(tx => {
    if (tx.type === 'expense') {
      if (accountBalances[tx.accountId] !== undefined) accountBalances[tx.accountId] -= tx.amountMinor;
      monthExpMinor += tx.amountMinor;
    } else if (tx.type === 'income') {
      if (accountBalances[tx.accountId] !== undefined) accountBalances[tx.accountId] += tx.amountMinor;
      monthIncMinor += tx.amountMinor;
    } else if (tx.type === 'transfer') {
      if (accountBalances[tx.accountId] !== undefined) accountBalances[tx.accountId] -= tx.amountMinor;
      if (accountBalances[tx.toAccountId] !== undefined) accountBalances[tx.toAccountId] += tx.amountMinor;
    }
  });

  appState.accounts.forEach(a => {
    if (a.includeInNetWorth && !a.archived) {
      netWorthMinor += accountBalances[a.id];
    }
  });

  // Time-based greeting (placeholder name until onboarding exists)
  const hour = new Date().getHours();
  const period = hour < 12 ? 'Morning' : hour < 17 ? 'Afternoon' : 'Evening';
  const greetingPeriodEl = document.getElementById('dash-greeting-period');
  const greetingNameEl = document.getElementById('dash-greeting-name');
  if (greetingPeriodEl) greetingPeriodEl.textContent = `Good ${period},`;
  if (greetingNameEl) greetingNameEl.textContent = 'User';

  // Net worth amount (always white on emerald glass)
  const netWorthValEl = document.getElementById('dash-networth-val');
  if (netWorthValEl) {
    netWorthValEl.textContent = isPrivacyMasked ? '••••••••' : Money.format(netWorthMinor);
    netWorthValEl.className = 'font-sans text-[42px] font-bold tracking-tight text-white num-tabular leading-none mb-5';
  }

  // Month-over-month change pill vs end of previous calendar month
  renderNetWorthMomChange(netWorthMinor);

  // Populate Accounts Carousel (settings screen)
  populateAccountsCarousel(accountBalances);

  // Clean Category Spending
  renderCleanCategorySpending();

  // Financial Health gauge — current-month spend vs total category limits
  const currentMonthStr = new Date().toISOString().substring(0, 7);
  let monthSpentForHealth = 0;
  appState.transactions.forEach(t => {
    if (t.type === 'expense' && t.date.startsWith(currentMonthStr)) {
      monthSpentForHealth += t.amountMinor;
    }
  });
  const totalBudgetMinor = appState.categories.reduce((sum, c) => sum + (c.monthlyLimitMinor || 0), 0);
  renderFinancialHealthGauge(monthSpentForHealth, totalBudgetMinor);

  // Recent Transactions List
  const recentList = document.getElementById('dash-recent-tx-list');
  if (recentList) {
    recentList.innerHTML = '';

    const sortedTx = [...appState.transactions].sort((a, b) => {
      const dateComp = b.date.localeCompare(a.date);
      if (dateComp !== 0) return dateComp;
      const timeComp = (b.time || '').localeCompare(a.time || '');
      if (timeComp !== 0) return timeComp;
      return (b.createdAt || 0) - (a.createdAt || 0);
    });

    sortedTx.slice(0, 5).forEach(tx => {
      const acc = appState.accounts.find(a => a.id === tx.accountId);
      const cat = appState.categories.find(c => c.id === tx.categoryId);
      recentList.insertAdjacentHTML('beforeend', Components.TransactionRow(tx, acc ? acc.name : '', cat ? cat.icon : '', cat ? cat.name : 'Uncategorized'));
    });

    document.querySelectorAll('#dash-recent-tx-list [data-txid]').forEach(row => {
      row.addEventListener('click', () => openTransactionDetailSheet(row.dataset.txid));
    });
  }

  const badge = document.getElementById('badge-sms-count');
  if (badge) badge.innerText = appState.smsQueue.length;
}

/** Net worth as of end of previous calendar month (replay ledger through that date). */
function computeNetWorthAsOf(endDateStr) {
  const balances = {};
  appState.accounts.forEach(a => { balances[a.id] = a.startingBalanceMinor || 0; });

  appState.transactions.forEach(tx => {
    if (!tx.date || tx.date > endDateStr) return;
    if (tx.type === 'expense') {
      if (balances[tx.accountId] !== undefined) balances[tx.accountId] -= tx.amountMinor;
    } else if (tx.type === 'income') {
      if (balances[tx.accountId] !== undefined) balances[tx.accountId] += tx.amountMinor;
    } else if (tx.type === 'transfer') {
      if (balances[tx.accountId] !== undefined) balances[tx.accountId] -= tx.amountMinor;
      if (balances[tx.toAccountId] !== undefined) balances[tx.toAccountId] += tx.amountMinor;
    }
  });

  let total = 0;
  appState.accounts.forEach(a => {
    if (a.includeInNetWorth && !a.archived) total += balances[a.id] || 0;
  });
  return total;
}

function renderNetWorthMomChange(currentNetWorthMinor) {
  const el = document.getElementById('dash-networth-change');
  if (!el) return;

  const now = new Date();
  // Last day of previous calendar month
  const prevMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0);
  const y = prevMonthEnd.getFullYear();
  const m = String(prevMonthEnd.getMonth() + 1).padStart(2, '0');
  const d = String(prevMonthEnd.getDate()).padStart(2, '0');
  const baseline = computeNetWorthAsOf(`${y}-${m}-${d}`);

  let label = '0.0%';
  let positive = true;

  if (Math.abs(baseline) > 0) {
    const pct = ((currentNetWorthMinor - baseline) / Math.abs(baseline)) * 100;
    positive = pct >= 0;
    const abs = Math.abs(pct).toFixed(1);
    label = `${positive ? '+' : '-'}${abs}%`;
  } else if (currentNetWorthMinor !== 0) {
    // No prior baseline — treat any current value as full change vs zero
    positive = currentNetWorthMinor > 0;
    label = positive ? '+100.0%' : '-100.0%';
  }

  el.textContent = label;
  el.className = positive
    ? 'font-sans text-[12px] font-bold num-tabular px-2.5 py-1 rounded-full bg-black/40 text-[#34D399]'
    : 'font-sans text-[12px] font-bold num-tabular px-2.5 py-1 rounded-full bg-black/40 text-[#F87171]';
}

// Render Net Worth Trend Line Graph
function renderNetWorthTrendGraph(isNegative = false) {
  const svg = document.getElementById('dash-hero-trend-svg');
  if (!svg) return;

  const color = isNegative ? '#D87D56' : '#3B7A57';
  const gradId = isNegative ? 'netWorthGradNeg' : 'netWorthGrad';

  const fillPath = isNegative
    ? 'M 0,12 C 75,15 125,28 180,30 C 235,32 270,48 300,50 L 300,60 L 0,60 Z'
    : 'M 0,48 C 75,45 125,30 180,32 C 235,34 270,12 300,10 L 300,60 L 0,60 Z';

  const strokePath = isNegative
    ? 'M 0,12 C 75,15 125,28 180,30 C 235,32 270,48 300,50'
    : 'M 0,48 C 75,45 125,30 180,32 C 235,34 270,12 300,10';

  svg.innerHTML = `
    <defs>
      <linearGradient id="${gradId}" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="${color}" stop-opacity="0.25"/>
        <stop offset="100%" stop-color="${color}" stop-opacity="0.0"/>
      </linearGradient>
    </defs>
    <path d="${fillPath}" fill="url(#${gradId})" />
    <path d="${strokePath}" fill="none" stroke="${color}" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
  `;
}

// Financial Health — semicircular remaining-budget gauge
// Remaining = (1 − spent/limit) × 100. Over budget shows overage %.
function renderFinancialHealthGauge(spentMinor, totalBudgetMinor) {
  const svg = document.getElementById('dash-health-gauge-svg');
  if (!svg) return;

  const GREEN = '#1B4332';
  const TRACK = '#E9E7DF';
  const TICK = '#52B788';
  const cx = 140;
  const cy = 138;
  const r = 100;
  const strokeW = 18;
  const arcPath = `M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`;
  const arcLen = Math.PI * r;

  let displayPct = 100;
  let fillPct = 100;
  let arcColor = GREEN;
  let label = 'Budget Safe';
  let pctColor = '#1A1A1A';

  if (totalBudgetMinor <= 0) {
    if (spentMinor > 0) {
      displayPct = 0;
      fillPct = 0;
      arcColor = '#B5443C';
      label = 'No Limit Set';
      pctColor = '#B5443C';
    }
  } else if (spentMinor > totalBudgetMinor) {
    displayPct = Math.round(((spentMinor - totalBudgetMinor) / totalBudgetMinor) * 100);
    fillPct = 0;
    arcColor = '#B5443C';
    label = 'Over Budget';
    pctColor = '#B5443C';
  } else {
    const remaining = (1 - spentMinor / totalBudgetMinor) * 100;
    displayPct = Math.round(remaining);
    fillPct = remaining;
    if (displayPct >= 50) { arcColor = GREEN; label = 'Budget Safe'; }
    else if (displayPct >= 25) { arcColor = '#C4A574'; label = 'Budget Tight'; }
    else { arcColor = '#D87D56'; label = 'Budget Risky'; }
  }

  const filled = (fillPct / 100) * arcLen;

  let tick = '';
  if (fillPct > 1 && fillPct < 99) {
    const angle = Math.PI * (1 - fillPct / 100);
    const mx = cx + r * Math.cos(angle);
    const my = cy - r * Math.sin(angle);
    const nx = Math.cos(angle);
    const ny = -Math.sin(angle);
    tick = `<line x1="${mx - nx * 8}" y1="${my - ny * 8}" x2="${mx + nx * 8}" y2="${my + ny * 8}" stroke="${TICK}" stroke-width="3.5" stroke-linecap="round"/>`;
  }

  // Labels live inside the SVG so they sit in the bowl of the arc — never overlapping it.
  svg.innerHTML = `
    <path d="${arcPath}" fill="none" stroke="${TRACK}" stroke-width="${strokeW}" stroke-linecap="round"/>
    <path d="${arcPath}" fill="none" stroke="${arcColor}" stroke-width="${strokeW}" stroke-linecap="round"
      stroke-dasharray="${filled} ${arcLen}" style="transition: stroke-dasharray 400ms ease;"/>
    ${tick}
    <text x="${cx}" y="98" text-anchor="middle" fill="${pctColor}"
      font-family="Fraunces, Georgia, serif" font-size="36" font-weight="700"
      style="font-variant-numeric: tabular-nums;">${displayPct}%</text>
    <text x="${cx}" y="122" text-anchor="middle" fill="#8A8A84"
      font-family="DM Sans, system-ui, sans-serif" font-size="13" font-weight="500">${label}</text>
  `;
}

// Clean Category Spending — top categories this month (bars vs largest)
// Inline styles used so layout never depends on a stale Tailwind build.
function renderCleanCategorySpending() {
  const list = document.getElementById('dash-clean-category-list');
  if (!list) return;

  const month = new Date().toISOString().substring(0, 7);
  const totals = {};

  appState.transactions.forEach(t => {
    if (t.type === 'expense' && t.date.startsWith(month)) {
      totals[t.categoryId] = (totals[t.categoryId] || 0) + t.amountMinor;
    }
  });

  const items = Object.keys(totals).map(id => {
    const cat = appState.categories.find(c => c.id === id);
    return {
      name: cat?.name || 'Other',
      icon: cat?.icon || '📦',
      amountMinor: totals[id]
    };
  }).sort((a, b) => b.amountMinor - a.amountMinor);

  if (items.length === 0) {
    list.innerHTML = `<p style="color:#8A8A84;font-size:14px;font-family:DM Sans,system-ui,sans-serif;margin:0;">No spending recorded this month</p>`;
    return;
  }

  const palette = [
    { bar: '#1B4332', bg: '#D8F3DC' },
    { bar: '#7C3AED', bg: '#EDE4FF' },
    { bar: '#E07A3D', bg: '#FFE8D6' },
    { bar: '#2563EB', bg: '#DBEAFE' },
    { bar: '#B45309', bg: '#FEF3C7' },
  ];
  const max = items[0].amountMinor || 1;

  list.innerHTML = items.slice(0, 2).map((item, i) => {
    const c = palette[i % palette.length];
    const pct = Math.max(4, (item.amountMinor / max) * 100);
    const mb = i < Math.min(items.length, 2) - 1 ? '18px' : '0';
    return `
      <div style="display:flex;align-items:flex-start;gap:10px;margin-bottom:${mb};">
        <span style="width:32px;height:32px;border-radius:10px;background:${c.bg};display:inline-flex;align-items:center;justify-content:center;font-size:15px;flex-shrink:0;">${item.icon}</span>
        <div style="flex:1;min-width:0;">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:8px;">
            <span style="font-family:DM Sans,system-ui,sans-serif;font-weight:500;font-size:15px;color:#1A1A1A;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${item.name}</span>
            <span style="font-family:DM Sans,system-ui,sans-serif;font-weight:700;font-size:15px;color:#1A1A1A;font-variant-numeric:tabular-nums;flex-shrink:0;">${Money.format(item.amountMinor)}</span>
          </div>
          <div style="width:100%;height:10px;border-radius:999px;background:${c.bg};overflow:hidden;">
            <div style="height:100%;width:${pct}%;border-radius:999px;background:${c.bar};transition:width 400ms ease;"></div>
          </div>
        </div>
      </div>`;
  }).join('');
}

// Toggle Privacy Mask for Sensitive Net Worth Amounts
let isPrivacyMasked = false;
function togglePrivacyMask() {
  const netWorthEl = document.getElementById('dash-networth-val');
  const eyeBtn = document.getElementById('btn-toggle-eye');
  if (!netWorthEl) return;

  isPrivacyMasked = !isPrivacyMasked;
  if (isPrivacyMasked) {
    netWorthEl.innerText = "••••••••";
    if (eyeBtn) eyeBtn.innerText = "🙈";
  } else {
    if (eyeBtn) eyeBtn.innerText = "👁️";
    renderDashboardView();
  }
}

// Toggle Net Worth Chart View
let currentNetWorthChart = 'line';

function toggleNetWorthChartView() {
  const lineView = document.getElementById('networth-line-view');
  const pieView = document.getElementById('networth-pie-view');
  const iconEl = document.getElementById('networth-chart-toggle-icon');
  const lblEl = document.getElementById('networth-chart-toggle-lbl');

  if (!lineView || !pieView) return;

  if (currentNetWorthChart === 'line') {
    currentNetWorthChart = 'pie';
    lineView.classList.add('hidden');
    pieView.classList.remove('hidden');
    if (iconEl) iconEl.innerText = '📈';
    if (lblEl) lblEl.innerText = 'Line';
  } else {
    currentNetWorthChart = 'line';
    pieView.classList.add('hidden');
    lineView.classList.remove('hidden');
    if (iconEl) iconEl.innerText = '📊';
    if (lblEl) lblEl.innerText = 'Pie';
  }
}

// Render Net Worth Cashflow Breakdown Pie Chart (Income vs Spent)
function renderNetWorthPieChart(monthIncMinor = 0, monthExpMinor = 0) {
  const svg = document.getElementById('dash-networth-pie-svg');
  if (!svg) return;
  const legend = document.getElementById('dash-networth-pie-legend');

  const total = (monthIncMinor || 0) + (monthExpMinor || 0);

  if (total <= 0) {
    svg.innerHTML = `<circle cx="50" cy="50" r="35" stroke="#E5E3DC" stroke-width="18" fill="none"/>`;
    if (legend) legend.innerHTML = `<div class="text-[#7C8079] text-xs font-sans">No cashflow activity</div>`;
    return;
  }

  const incPct = monthIncMinor / total;
  const expPct = monthExpMinor / total;

  const radius = 35;
  const circumference = 2 * Math.PI * radius;

  const incLen = incPct * circumference;
  const expLen = expPct * circumference;

  const incColor = '#3B7A57'; // Green
  const expColor = '#D87D56'; // Terracotta Red

  let svgPaths = '';

  if (monthIncMinor > 0) {
    svgPaths += `<circle cx="50" cy="50" r="${radius}" stroke="${incColor}" stroke-width="18" fill="none" stroke-dasharray="${incLen} ${circumference - incLen}" stroke-dashoffset="0"/>`;
  }

  if (monthExpMinor > 0) {
    svgPaths += `<circle cx="50" cy="50" r="${radius}" stroke="${expColor}" stroke-width="18" fill="none" stroke-dasharray="${expLen} ${circumference - expLen}" stroke-dashoffset="${-incLen}"/>`;
  }

  const legendHtml = `
    <div class="space-y-1.5 font-sans text-xs">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-1.5">
          <span class="w-2.5 h-2.5 rounded-full bg-[#3B7A57]"></span>
          <span class="font-bold text-[#2D332A]">Income</span>
        </div>
        <span class="font-bold text-[#3B7A57] num-tabular">+${Money.format(monthIncMinor)}</span>
      </div>
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-1.5">
          <span class="w-2.5 h-2.5 rounded-full bg-[#D87D56]"></span>
          <span class="font-bold text-[#2D332A]">Spent</span>
        </div>
        <span class="font-bold text-[#D87D56] num-tabular">-${Money.format(monthExpMinor)}</span>
      </div>
    </div>
  `;

  svg.innerHTML = svgPaths;
  if (legend) legend.innerHTML = legendHtml;
}

// 2. Render Transactions Ledger
function renderTransactionsView(searchQuery = '') {
  const container = document.getElementById('tx-grouped-ledger-container');
  container.innerHTML = '';

  let filtered = appState.transactions.filter(tx => {
    if (appState.currentTxFilter !== 'all' && tx.type !== appState.currentTxFilter) return false;
    if (appState.selectedCategoryFilters && appState.selectedCategoryFilters.length > 0) {
      if (!appState.selectedCategoryFilters.includes(tx.categoryId)) return false;
    }
    if (searchQuery) {
      const cat = appState.categories.find(c => c.id === tx.categoryId);
      const acc = appState.accounts.find(a => a.id === tx.accountId);
      const searchStr = `${tx.merchant || ''} ${cat?.name || ''} ${acc?.name || ''} ${tx.note || ''}`.toLowerCase();
      if (!searchStr.includes(searchQuery)) return false;
    }
    return true;
  });

  if (filtered.length === 0) {
    container.innerHTML = `<div class="py-16 text-center text-[#7C8079] font-sans text-xs">No transactions match active filters</div>`;
    return;
  }

  const todayStr = new Date().toISOString().split('T')[0];
  const yesterdayObj = new Date();
  yesterdayObj.setDate(yesterdayObj.getDate() - 1);
  const yesterdayStr = yesterdayObj.toISOString().split('T')[0];

  const groups = {};
  filtered.forEach(tx => {
    const d = tx.date;
    if (!groups[d]) groups[d] = [];
    groups[d].push(tx);
  });

  const sortedDates = Object.keys(groups).sort((a, b) => b.localeCompare(a));
  sortedDates.forEach(dateStr => {
    let displayHeader = dateStr;
    if (dateStr === todayStr) displayHeader = "Today";
    else if (dateStr === yesterdayStr) displayHeader = "Yesterday";

    const groupSec = document.createElement('div');
    groupSec.className = 'space-y-2';
    groupSec.innerHTML = `<div class="text-[13px] font-headers font-bold text-[#2D332A] px-1">${displayHeader}</div>`;

    const card = document.createElement('div');
    card.className = 'bg-[#FFFFFF] rounded-3xl border border-[#E5E3DC] shadow-xs divide-y divide-[#F0EEE8] overflow-hidden';
    
    groups[dateStr].forEach(tx => {
      const acc = appState.accounts.find(a => a.id === tx.accountId);
      const cat = appState.categories.find(c => c.id === tx.categoryId);
      card.insertAdjacentHTML('beforeend', Components.TransactionRow(tx, acc ? acc.name : '', cat ? cat.icon : '', cat ? cat.name : 'Uncategorized'));
    });

    groupSec.appendChild(card);
    container.appendChild(groupSec);
  });

  document.querySelectorAll('#tx-grouped-ledger-container [data-txid]').forEach(row => {
    row.addEventListener('click', () => openTransactionDetailSheet(row.dataset.txid));
  });
}

// 3. Render Goals View
function renderGoalsView() {
  const container = document.getElementById('goals-main-grid');
  container.innerHTML = '';
  appState.goals.forEach(g => {
    container.insertAdjacentHTML('beforeend', Components.GoalCard(g));
  });

  document.querySelectorAll('#goals-main-grid [data-goalid]').forEach(card => {
    card.addEventListener('click', () => openGoalDetailSheet(card.dataset.goalid));
  });
}

function populateAccountsCarousel(balances) {
  const accCarousel = document.getElementById('dash-accounts-carousel');
  if (!accCarousel) return;
  
  if (!balances) {
    balances = {};
    appState.accounts.forEach(a => balances[a.id] = a.startingBalanceMinor || 0);
    appState.transactions.forEach(tx => {
      if (tx.type === 'expense' && balances[tx.accountId] !== undefined) balances[tx.accountId] -= tx.amountMinor;
      else if (tx.type === 'income' && balances[tx.accountId] !== undefined) balances[tx.accountId] += tx.amountMinor;
      else if (tx.type === 'transfer') {
        if (balances[tx.accountId] !== undefined) balances[tx.accountId] -= tx.amountMinor;
        if (balances[tx.toAccountId] !== undefined) balances[tx.toAccountId] += tx.amountMinor;
      }
    });
  }

  accCarousel.innerHTML = '';
  appState.accounts.filter(a => !a.archived).forEach(a => {
    const cardHtml = Components.AccountCard(a, balances[a.id]);
    accCarousel.insertAdjacentHTML('beforeend', cardHtml);
  });
}

function renderSettingsView() {
  populateAccountsCarousel();
}

function switchAnalyticsSubtab(subTab) {
  appState.currentAnalyticsSubtab = subTab;

  document.querySelectorAll('.analytics-subtab-btn').forEach(btn => {
    if (btn.dataset.anab === subTab) {
      btn.className = 'analytics-subtab-btn w-1/5 text-center py-2 font-sans text-[10px] uppercase border-b-2 font-bold transition-colors border-[#3B7A57] text-[#3B7A57]';
    } else {
      btn.className = 'analytics-subtab-btn w-1/5 text-center py-2 font-sans text-[10px] uppercase border-b-2 font-bold transition-colors border-transparent text-[#7C8079]';
    }
  });

  document.getElementById('an-panel-overview').classList.add('hidden');
  document.getElementById('an-panel-categories').classList.add('hidden');
  document.getElementById('an-panel-accounts').classList.add('hidden');
  document.getElementById('an-panel-tags').classList.add('hidden');
  document.getElementById('an-panel-cashflow').classList.add('hidden');

  const targetPanel = document.getElementById(`an-panel-${subTab}`);
  if (targetPanel) targetPanel.classList.remove('hidden');

  renderAnalyticsView();
}

function renderAnalyticsView() {
  let incSumMinor = 0, expSumMinor = 0;
  appState.transactions.forEach(t => {
    if (t.type === 'income') incSumMinor += t.amountMinor;
    else if (t.type === 'expense') expSumMinor += t.amountMinor;
  });

  document.getElementById('an-kpi-inc').innerText = `+${Money.format(incSumMinor)}`;
  document.getElementById('an-kpi-exp').innerText = `-${Money.format(expSumMinor)}`;
  document.getElementById('an-kpi-saved').innerText = Money.format(Math.max(incSumMinor - expSumMinor, 0));
  document.getElementById('an-kpi-daily').innerText = Money.format(Math.round(expSumMinor / 30));

  // Render SVG Trend Line on Overview tab
  Analytics.renderTrendLine(document.getElementById('an-trend-svg'), appState.transactions);

  if (appState.currentAnalyticsSubtab === 'categories') {
    Analytics.renderCategoryDonut(
      document.getElementById('an-donut-svg'),
      document.getElementById('an-donut-legend'),
      appState.categories,
      appState.transactions
    );
  } else if (appState.currentAnalyticsSubtab === 'accounts') {
    const balances = {};
    appState.accounts.forEach(a => balances[a.id] = a.startingBalanceMinor || 0);
    appState.transactions.forEach(tx => {
      if (tx.type === 'expense' && balances[tx.accountId] !== undefined) balances[tx.accountId] -= tx.amountMinor;
      else if (tx.type === 'income' && balances[tx.accountId] !== undefined) balances[tx.accountId] += tx.amountMinor;
      else if (tx.type === 'transfer') {
        if (balances[tx.accountId] !== undefined) balances[tx.accountId] -= tx.amountMinor;
        if (balances[tx.toAccountId] !== undefined) balances[tx.toAccountId] += tx.amountMinor;
      }
    });
    Analytics.renderAccountsList(document.getElementById('an-accounts-list'), appState.accounts, balances);
  } else if (appState.currentAnalyticsSubtab === 'tags') {
    Analytics.renderTagsList(document.getElementById('an-tags-list'), appState.tags, appState.transactions);
  } else if (appState.currentAnalyticsSubtab === 'cashflow') {
    Analytics.renderCashflowBars(document.getElementById('an-cashflow-bars'), appState.transactions);
  }
}

// Sheets Utility Controls
function openSheet(htmlContent, isFloating = false) {
  const backdrop = document.getElementById('sheet-backdrop');
  const area = document.getElementById('sheet-content-area');
  area.innerHTML = htmlContent;

  if (isFloating) {
    backdrop.className = 'fixed inset-0 bg-[#2D332A]/50 backdrop-blur-md z-50 flex items-center justify-center p-4 transition-all duration-300 fade-in';
    area.className = 'w-full max-w-[370px] bg-[#FAF9F6]/95 backdrop-blur-xl border border-[#FFFFFF]/90 rounded-[32px] p-6 shadow-2xl max-h-[90vh] overflow-y-auto no-scrollbar space-y-4';
  } else {
    backdrop.className = 'fixed inset-0 bg-[#2D332A]/50 backdrop-blur-xs z-50 transition-opacity';
    area.className = 'absolute bottom-0 inset-x-0 bg-[#FFFFFF] border-t border-[#E5E3DC] rounded-t-3xl p-5 max-h-[85vh] overflow-y-auto no-scrollbar space-y-4 shadow-2xl';
  }

  backdrop.classList.remove('hidden');
}

function closeSheet() {
  const backdrop = document.getElementById('sheet-backdrop');
  backdrop.classList.add('hidden');
}

// SMS Smart Inbox Sheet (Floating Dialog with Frosted Glass Backdrop)
function openSMSQueueSheet() {
  const queue = appState.smsQueue || [];

  const headerRow = (countBadgeHtml = '') => `
    <div class="sms-emerald-header flex items-center justify-between px-5 py-4">
      <div class="flex items-center gap-2.5 min-w-0">
        <svg class="w-5 h-5 text-white flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/></svg>
        <h3 class="font-headers text-xl font-bold text-white tracking-tight">Smart Inbox</h3>
        ${countBadgeHtml}
      </div>
      <button id="btn-close-sms-sheet" class="text-white/70 hover:text-white p-1.5 rounded-full hover:bg-white/10 transition-colors flex-shrink-0" aria-label="Close">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
      </button>
    </div>
  `;

  const styleSmsSheet = () => {
    const area = document.getElementById('sheet-content-area');
    if (!area) return;
    area.className = 'sms-emerald-sheet w-full max-w-[370px] rounded-[32px] max-h-[90vh] overflow-y-auto no-scrollbar';
  };

  if (queue.length === 0) {
    const emptyHtml = `
      <div class="font-sans fade-in">
        ${headerRow()}
        <div class="px-6 py-8 text-center space-y-3">
          <div class="w-16 h-16 rounded-full bg-[#F0EEE8] border border-[#E5E3DC] flex items-center justify-center text-2xl mx-auto">
            📬
          </div>
          <h4 class="font-headers text-xl font-bold text-[#0B2A1F]">Smart Inbox Clear</h4>
          <p class="text-xs text-[#7C8079] max-w-xs mx-auto">No pending banking SMS notifications to parse right now.</p>
          <button id="btn-reseed-sms" class="mt-3 px-5 py-2.5 bg-[#34D399] hover:bg-[#2ECC71] text-white font-sans text-xs font-bold rounded-xl shadow-sm uppercase tracking-wider transition-colors">
            Load Example Stacked SMS
          </button>
        </div>
      </div>
    `;
    openSheet(emptyHtml, true);
    styleSmsSheet();
    document.getElementById('btn-close-sms-sheet')?.addEventListener('click', closeSheet);
    document.getElementById('btn-reseed-sms')?.addEventListener('click', async () => {
      const defaultSmsList = [
        { id: "sms-1", rawText: "Alert: Spend of INR 450.00 on Food at Starbucks card 1234", bank: "HDFC", amountMinor: 45000, merchant: "Starbucks", suggestedCategory: "Food & Drink", suggestedAccount: "acc-1", confidence: "High Confidence", timestamp: Date.now() },
        { id: "sms-2", rawText: "Txn: INR 280.00 debited for Uber ride on ICICI Card 5678", bank: "ICICI", amountMinor: 28000, merchant: "Uber", suggestedCategory: "Transport", suggestedAccount: "acc-3", confidence: "High Confidence", timestamp: Date.now() - 60000 },
        { id: "sms-3", rawText: "Alert: INR 620.00 spent at Swiggy on HDFC Card 1234", bank: "HDFC", amountMinor: 62000, merchant: "Swiggy", suggestedCategory: "Food & Drink", suggestedAccount: "acc-1", confidence: "High Confidence", timestamp: Date.now() - 120000 }
      ];
      for (const s of defaultSmsList) await dbEngine.put("smsQueue", s);
      await loadStateFromDB();
      openSMSQueueSheet();
      renderCurrentTab();
    });
    return;
  }

  const sms = queue[0];
  const countBadge = queue.length > 1
    ? `<span class="text-[11px] font-sans text-white/80 bg-white/10 px-2.5 py-0.5 rounded-full border border-white/15">1 of ${queue.length}</span>`
    : '';
  const matchingAcc = appState.accounts.find(a => a.id === sms.suggestedAccount) || appState.accounts[0];
  const accLabel = sms.bank || (matchingAcc ? matchingAcc.name : 'HDFC');

  let heroIcon = '☕';
  const mLower = (sms.merchant || '').toLowerCase();
  if (mLower.includes('uber') || mLower.includes('ola') || mLower.includes('cab')) heroIcon = '🚗';
  else if (mLower.includes('amazon') || mLower.includes('flipkart') || mLower.includes('shopping')) heroIcon = '🛍️';
  else if (mLower.includes('swiggy') || mLower.includes('zomato') || mLower.includes('food')) heroIcon = '🍔';
  else if (mLower.includes('starbucks') || mLower.includes('coffee') || mLower.includes('cafe')) heroIcon = '☕';
  else if (mLower.includes('netflix') || mLower.includes('spotify') || mLower.includes('movie')) heroIcon = '🎬';
  else heroIcon = '📦';

  const html = `
    <div class="font-sans fade-in">
      ${headerRow(countBadge)}

      <div class="px-6 pt-5 pb-6 space-y-4">
        <!-- Merchant -->
        <div class="text-center">
          <div class="w-16 h-16 rounded-full bg-[#F0EEE8] border border-[#E5E3DC] flex items-center justify-center text-2xl mx-auto mb-3">
            ${heroIcon}
          </div>

          <h2 class="font-headers text-2xl font-bold text-[#0B2A1F] tracking-tight">${sms.merchant || 'Activity'}</h2>

          <div class="flex justify-center my-2">
            <span class="bg-[#0B2A1F] text-white text-[11px] font-semibold px-3 py-1 rounded-full flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5 text-[#34D399]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>
              <span>${sms.confidence || 'High Confidence'}</span>
            </span>
          </div>

          <div class="my-3">
            <span class="font-sans text-4xl sm:text-5xl font-bold text-[#0B2A1F] tracking-tight num-tabular">${Money.format(sms.amountMinor)}</span>
          </div>

          <div class="flex justify-center mb-1">
            <span class="bg-[#F0EEE8] border border-[#E5E3DC] text-[#0B2A1F] text-xs font-bold px-3.5 py-1.5 rounded-full">${accLabel} - Card 1234</span>
          </div>
        </div>

        <!-- Raw SMS -->
        <div class="bg-[#F0EEE8]/80 border border-[#E5E3DC] rounded-2xl p-4 text-center text-xs text-[#5C6757] leading-relaxed font-sans">
          "${sms.rawText || ''}"
        </div>

        <!-- Actions -->
        <div class="pt-1 space-y-2">
          <button id="btn-approve-current-sms" class="w-full py-4 bg-[#34D399] hover:bg-[#2ECC71] active:scale-[0.98] text-white font-sans font-bold text-xs sm:text-sm rounded-2xl shadow-md flex items-center justify-center gap-2 tracking-wider uppercase transition-all">
            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            <span>APPROVE & ADD</span>
          </button>

          <button id="btn-dismiss-current-sms" class="w-full py-2.5 text-center text-xs font-bold text-[#7C8079] hover:text-[#0B2A1F] uppercase tracking-wider transition-colors font-sans block">
            DISMISS
          </button>
        </div>
      </div>
    </div>
  `;

  openSheet(html, true);
  styleSmsSheet();

  document.getElementById('btn-close-sms-sheet')?.addEventListener('click', closeSheet);

  document.getElementById('btn-approve-current-sms')?.addEventListener('click', async () => {
    const cat = appState.categories[0];
    const newTx = {
      id: `tx-${Date.now()}`,
      type: 'expense',
      amountMinor: sms.amountMinor,
      accountId: sms.suggestedAccount || appState.accounts[0]?.id || 'acc-1',
      toAccountId: null,
      categoryId: cat ? cat.id : 'cat-1',
      merchant: sms.merchant || 'Bank Transaction',
      tags: [],
      date: new Date().toISOString().split('T')[0],
      time: new Date().toTimeString().substring(0, 5),
      note: sms.rawText,
      receiptBlob: null,
      source: 'SMS Auto-Detected',
      splits: [],
      createdAt: Date.now()
    };
    await dbEngine.put('transactions', newTx);
    await dbEngine.delete('smsQueue', sms.id);
    await loadStateFromDB();
    openSMSQueueSheet();
    renderCurrentTab();
  });

  document.getElementById('btn-dismiss-current-sms')?.addEventListener('click', async () => {
    await dbEngine.delete('smsQueue', sms.id);
    await loadStateFromDB();
    openSMSQueueSheet();
    renderCurrentTab();
  });
}

// Budget Details Sheet
function openBudgetDetailsSheet() {
  const currentMonthStr = new Date().toISOString().substring(0, 7);
  const catTotals = {};
  appState.transactions.forEach(t => {
    if (t.type === 'expense' && t.date.startsWith(currentMonthStr)) {
      catTotals[t.categoryId] = (catTotals[t.categoryId] || 0) + t.amountMinor;
    }
  });

  const html = `
    <div class="flex items-center justify-between border-b border-[#E5E3DC] pb-3">
      <div>
        <h3 class="font-headers text-lg font-bold text-[#2D332A]">Monthly Budget Details</h3>
        <p class="text-[11px] text-[#7C8079]">Category limits vs actual spending</p>
      </div>
      <button id="btn-close-budget-sheet" class="text-[#7C8079] font-sans text-xs font-bold p-1">✕</button>
    </div>
    <div class="space-y-3 pt-2 font-sans text-xs max-h-[60vh] overflow-y-auto">
      ${appState.categories.map(c => {
        const spent = catTotals[c.id] || 0;
        const limit = c.monthlyLimitMinor || 0;
        const pct = limit > 0 ? Math.min(Math.round((spent / limit) * 100), 100) : 0;
        const isOver = limit > 0 && spent > limit;
        return `
          <div class="p-3 bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl space-y-1.5">
            <div class="flex items-center justify-between font-bold">
              <span>${c.icon} ${c.name}</span>
              <span class="num-tabular ${isOver ? 'text-[#D87D56]' : 'text-[#2D332A]'}">${Money.format(spent)} / ${limit > 0 ? Money.format(limit) : 'No Limit'}</span>
            </div>
            ${limit > 0 ? `
              <div class="w-full bg-[#E5E3DC] h-2 rounded-full overflow-hidden">
                <div class="${isOver ? 'bg-[#D87D56]' : 'bg-[#3B7A57]'} h-full transition-all duration-300 rounded-full" style="width: ${pct}%"></div>
              </div>
            ` : ''}
          </div>
        `;
      }).join('')}
    </div>
  `;
  openSheet(html);
  document.getElementById('btn-close-budget-sheet')?.addEventListener('click', closeSheet);
}

// Create Goal Sheet
function openCreateGoalSheet() {
  const html = `
    <div class="flex items-center justify-between border-b border-[#E5E3DC] pb-3">
      <h3 class="font-headers text-lg font-bold text-[#2D332A]">Create Savings Goal</h3>
      <button id="btn-close-goal-sheet" class="text-[#7C8079] font-sans text-xs font-bold p-1">✕</button>
    </div>
    <div class="space-y-3 pt-2 font-sans text-xs">
      <div>
        <label class="text-[#7C8079] font-bold block mb-1">Goal Name</label>
        <input type="text" id="goal-in-name" placeholder="e.g. Goa Trip, New Phone" class="w-full bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl p-2.5" />
      </div>
      <div>
        <label class="text-[#7C8079] font-bold block mb-1">Target Amount (₹)</label>
        <input type="number" id="goal-in-target" placeholder="50000" class="w-full bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl p-2.5" />
      </div>
      <div>
        <label class="text-[#7C8079] font-bold block mb-1">Target Deadline</label>
        <input type="date" id="goal-in-deadline" class="w-full bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl p-2.5" value="${new Date(Date.now() + 90*24*60*60*1000).toISOString().split('T')[0]}" />
      </div>
      <div>
        <label class="text-[#7C8079] font-bold block mb-1">Choose Icon</label>
        <select id="goal-in-icon" class="w-full bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl p-2.5">
          <option value="🏖️">🏖️ Vacation</option>
          <option value="📱">📱 Tech Gadget</option>
          <option value="🚗">🚗 Vehicle</option>
          <option value="🏠">🏠 House / Rent</option>
          <option value="🎓">🎓 Education</option>
          <option value="🎯">🎯 General Savings</option>
        </select>
      </div>
      <button id="btn-save-new-goal" class="w-full py-3 bg-[#3B7A57] text-[#FFFFFF] font-bold rounded-xl uppercase shadow-md mt-2">Create Goal</button>
    </div>
  `;
  openSheet(html);
  document.getElementById('btn-close-goal-sheet')?.addEventListener('click', closeSheet);

  document.getElementById('btn-save-new-goal').addEventListener('click', async () => {
    const name = document.getElementById('goal-in-name').value.trim();
    const targetRupees = parseFloat(document.getElementById('goal-in-target').value);
    const deadline = document.getElementById('goal-in-deadline').value;
    const icon = document.getElementById('goal-in-icon').value;

    if (!name || isNaN(targetRupees) || targetRupees <= 0) {
      alert('Please enter a valid goal name and target amount.');
      return;
    }

    const newGoal = {
      id: `goal-${Date.now()}`,
      name: name,
      targetAmountMinor: Money.toMinor(targetRupees),
      currentSavedMinor: 0,
      deadline: deadline,
      icon: icon,
      linkedAccountId: appState.accounts[0]?.id || 'acc-1',
      status: 'active',
      contributions: []
    };

    await dbEngine.put('goals', newGoal);
    await loadStateFromDB();
    closeSheet();
    renderCurrentTab();
  });
}

// Filter Bottom Sheet
function openFiltersSheet() {
  if (!appState.selectedCategoryFilters) appState.selectedCategoryFilters = [];

  const html = `
    <div class="bg-[#FFFFFF] p-3 text-[#2D332A] font-sans space-y-5 fade-in">
      <div class="flex items-center justify-between border-b border-[#E5E3DC] pb-3">
        <div>
          <h3 class="font-headers text-xl font-bold text-[#2D332A]">Filter Transactions</h3>
          <p class="text-[11px] text-[#7C8079]">Select categories to view specific spending</p>
        </div>
        <button id="btn-reset-filters" class="text-xs font-sans text-[#3B7A57] font-bold uppercase tracking-wider hover:underline">Reset All</button>
      </div>

      <!-- Transaction Type Segmented Toggle -->
      <div>
        <label class="text-xs text-[#7C8079] font-bold uppercase tracking-wider block mb-2">Transaction Type</label>
        <div class="bg-[#F4F3EF] p-1 rounded-full flex items-center shadow-inner">
          <button data-typefilter="all" class="btn-type-filter flex-1 py-2 text-center rounded-full font-bold text-xs transition-all ${appState.currentTxFilter === 'all' ? 'bg-[#FFFFFF] text-[#2D332A] shadow-xs' : 'text-[#7C8079]'}">All</button>
          <button data-typefilter="expense" class="btn-type-filter flex-1 py-2 text-center rounded-full font-bold text-xs transition-all ${appState.currentTxFilter === 'expense' ? 'bg-[#FFFFFF] text-[#2D332A] shadow-xs' : 'text-[#7C8079]'}">Expense</button>
          <button data-typefilter="income" class="btn-type-filter flex-1 py-2 text-center rounded-full font-bold text-xs transition-all ${appState.currentTxFilter === 'income' ? 'bg-[#FFFFFF] text-[#2D332A] shadow-xs' : 'text-[#7C8079]'}">Income</button>
        </div>
      </div>

      <!-- Category Filter Pills -->
      <div>
        <div class="flex items-center justify-between mb-2">
          <label class="text-xs text-[#7C8079] font-bold uppercase tracking-wider">Categories</label>
          <span class="text-[10px] text-[#7C8079] font-medium">Tap to select multiple</span>
        </div>
        <div class="flex flex-wrap gap-2.5" id="filter-cat-options">
          ${appState.categories.map(c => {
            const isSelected = appState.selectedCategoryFilters.includes(c.id);
            return `
              <button data-catid="${c.id}" class="btn-filter-cat-pill px-4 py-2 rounded-full border text-xs font-bold flex items-center gap-2 transition-all ${isSelected ? 'bg-[#3B7A57] text-[#FFFFFF] border-[#3B7A57] shadow-xs' : 'bg-[#FAF9F6] text-[#2D332A] border-[#E5E3DC] hover:border-[#3B7A57]'}">
                <span class="text-sm">${c.icon}</span>
                <span>${c.name}</span>
              </button>
            `;
          }).join('')}
        </div>
      </div>

      <button id="btn-apply-filters" class="w-full py-4 bg-[#3B7A57] text-[#FFFFFF] font-sans font-bold text-sm rounded-full shadow-md hover:bg-[#2D332A] active:scale-[0.98] transition-all">
        Apply Category Filters
      </button>
    </div>
  `;
  openSheet(html);

  document.querySelectorAll('.btn-type-filter').forEach(btn => {
    btn.addEventListener('click', () => {
      appState.currentTxFilter = btn.dataset.typefilter;
      document.querySelectorAll('.btn-type-filter').forEach(b => {
        b.className = `btn-type-filter flex-1 py-2 text-center rounded-full font-bold text-xs transition-all ${b.dataset.typefilter === appState.currentTxFilter ? 'bg-[#FFFFFF] text-[#2D332A] shadow-xs' : 'text-[#7C8079]'}`;
      });
    });
  });

  document.querySelectorAll('.btn-filter-cat-pill').forEach(pill => {
    pill.addEventListener('click', () => {
      const catId = pill.dataset.catid;
      const idx = appState.selectedCategoryFilters.indexOf(catId);
      if (idx > -1) {
        appState.selectedCategoryFilters.splice(idx, 1);
        pill.className = 'btn-filter-cat-pill px-4 py-2 rounded-full border text-xs font-bold flex items-center gap-2 transition-all bg-[#FAF9F6] text-[#2D332A] border-[#E5E3DC] hover:border-[#3B7A57]';
      } else {
        appState.selectedCategoryFilters.push(catId);
        pill.className = 'btn-filter-cat-pill px-4 py-2 rounded-full border text-xs font-bold flex items-center gap-2 transition-all bg-[#3B7A57] text-[#FFFFFF] border-[#3B7A57] shadow-xs';
      }
    });
  });

  document.getElementById('btn-apply-filters').addEventListener('click', () => {
    renderTransactionsView();
    closeSheet();
  });

  document.getElementById('btn-reset-filters').addEventListener('click', () => {
    appState.currentTxFilter = 'all';
    appState.selectedCategoryFilters = [];
    renderTransactionsView();
    closeSheet();
  });
}

// Transaction Modal Handlers
function openAddTransactionSheet(type = 'expense') {
  let activeType = type;
  let keypadVal = "0.00";
  let selectedCatId = appState.categories[0]?.id || 'cat-1';
  let selectedAccId = appState.accounts[0]?.id || 'acc-1';
  let selectedToAccId = appState.accounts[1]?.id || appState.accounts[0]?.id || 'acc-1';
  let selectedDateVal = new Date().toISOString().split('T')[0];

  function renderModalContent() {
    const html = `
      <div class="bg-[#F4F3EF] min-h-[620px] flex flex-col justify-between -m-5 p-6 rounded-t-3xl text-[#2D332A] font-sans fade-in">
        <div class="flex items-center justify-between border-b border-[#E5E3DC]/60 pb-3">
          <button id="btn-close-entry" class="text-[#7C8079] hover:text-[#2D332A] text-lg font-bold p-1 w-8 h-8 rounded-full flex items-center justify-center hover:bg-[#E4E8E3] transition-colors">✕</button>
          <h2 class="font-headers text-xl text-[#2D332A] font-bold tracking-tight">New Entry</h2>
          <div class="w-8"></div>
        </div>

        <div class="bg-[#E5E3DC]/60 p-1.5 rounded-full flex items-center shadow-inner my-2">
          <button id="btn-type-exp" class="flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all ${activeType === 'expense' ? 'bg-[#FFFFFF] text-[#2D332A] shadow-sm' : 'text-[#7C8079]'}">Expense</button>
          <button id="btn-type-inc" class="flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all ${activeType === 'income' ? 'bg-[#FFFFFF] text-[#2D332A] shadow-sm' : 'text-[#7C8079]'}">Income</button>
          <button id="btn-type-trf" class="flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all ${activeType === 'transfer' ? 'bg-[#FFFFFF] text-[#2D332A] shadow-sm' : 'text-[#7C8079]'}">Transfer</button>
        </div>

        <div class="flex flex-wrap justify-center items-center gap-2 my-1">
          <div class="bg-[#FFFFFF] border border-[#E5E3DC] rounded-full px-3.5 py-1.5 flex items-center gap-1.5 shadow-xs hover:border-[#3B7A57] transition-colors">
            <span class="text-xs text-[#7C8079]">${activeType === 'transfer' ? 'From:' : 'Account:'}</span>
            <select id="entry-acc-select" class="bg-transparent font-sans text-xs font-bold text-[#3B7A57] focus:outline-none cursor-pointer">
              ${appState.accounts.map(a => `<option value="${a.id}" ${a.id === selectedAccId ? 'selected' : ''}>${a.icon} ${a.name}</option>`).join('')}
            </select>
          </div>

          ${activeType === 'transfer' ? `
            <div class="bg-[#FFFFFF] border border-[#E5E3DC] rounded-full px-3.5 py-1.5 flex items-center gap-1.5 shadow-xs hover:border-[#3B7A57] transition-colors">
              <span class="text-xs text-[#7C8079]">To:</span>
              <select id="entry-toacc-select" class="bg-transparent font-sans text-xs font-bold text-[#3B7A57] focus:outline-none cursor-pointer">
                ${appState.accounts.map(a => `<option value="${a.id}" ${a.id === selectedToAccId ? 'selected' : ''}>${a.icon} ${a.name}</option>`).join('')}
              </select>
            </div>
          ` : ''}

          <div class="bg-[#FFFFFF] border border-[#E5E3DC] rounded-full px-3 py-1.5 flex items-center gap-1.5 shadow-xs hover:border-[#3B7A57] transition-colors">
            <span class="text-xs text-[#3B7A57]">📅</span>
            <input type="date" id="entry-date-select" class="bg-transparent font-sans text-xs font-bold text-[#2D332A] focus:outline-none cursor-pointer" value="${selectedDateVal}" />
          </div>
        </div>

        <div class="text-center my-2 py-1">
          <div class="text-[11px] text-[#7C8079] font-medium uppercase tracking-wider mb-1">Enter amount</div>
          <div class="text-3xl font-headers text-[#2D332A] font-bold tracking-tight flex items-center justify-center">
            <span class="text-xl text-[#3B7A57] mr-1 font-sans">₹</span>
            <span id="numpad-display-val" class="num-tabular">${keypadVal}</span>
            <span class="currency-cursor text-[#3B7A57] font-light ml-0.5">|</span>
          </div>
        </div>

        ${activeType !== 'transfer' ? `
          <div class="my-1">
            <div class="text-[11px] font-bold text-[#3B7A57] mb-2 px-1 uppercase tracking-wider">Category</div>
            <div id="category-pills-row" class="flex gap-2 overflow-x-auto pb-1.5 no-scrollbar">
              ${appState.categories.map(c => `
                <button data-catid="${c.id}" class="btn-cat-pill flex-shrink-0 px-4 py-2 rounded-full border text-xs font-semibold flex items-center gap-2 transition-all ${c.id === selectedCatId ? 'bg-[#3B7A57] text-[#FFFFFF] border-[#3B7A57] shadow-sm' : 'bg-[#FFFFFF] text-[#2D332A] border-[#E5E3DC] hover:border-[#3B7A57]'}">
                  <span class="text-sm">${c.icon}</span>
                  <span>${c.name}</span>
                </button>
              `).join('')}
            </div>
          </div>
        ` : ''}

        <div class="bg-[#FFFFFF] rounded-3xl p-5 shadow-sm border border-[#E5E3DC]/80 my-2">
          <div class="grid grid-cols-3 gap-y-4 gap-x-3 text-center font-sans text-2xl font-medium text-[#2D332A]">
            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors" data-key="1">1</button>
            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors" data-key="2">2</button>
            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors" data-key="3">3</button>

            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors" data-key="4">4</button>
            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors" data-key="5">5</button>
            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors" data-key="6">6</button>

            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors" data-key="7">7</button>
            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors" data-key="8">8</button>
            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors" data-key="9">9</button>

            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors" data-key=".">.</button>
            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors" data-key="0">0</button>
            <button class="btn-num-key py-2.5 rounded-2xl hover:bg-[#F4F3EF] transition-colors text-xl flex items-center justify-center text-[#7C8079]" data-key="del">⌫</button>
          </div>
        </div>

        <div class="bg-[#FFFFFF] rounded-2xl p-3.5 flex items-center gap-2.5 my-1 border border-[#E5E3DC] focus-within:border-[#3B7A57] transition-colors shadow-xs">
          <span class="text-[#3B7A57] text-sm">✏️</span>
          <input type="text" id="entry-note-input" placeholder="Add a note (optional)" class="bg-transparent w-full text-xs font-sans text-[#2D332A] focus:outline-none placeholder:text-[#A4A6A1]" />
        </div>

        <button id="btn-submit-entry" class="w-full py-4 bg-[#3B7A57] text-[#FFFFFF] rounded-full font-sans font-bold text-sm flex items-center justify-center gap-2 shadow-md hover:bg-[#2D332A] active:scale-[0.98] transition-all mt-2">
          <span>Save Transaction</span>
          <span class="text-base font-bold">✓</span>
        </button>
      </div>
    `;

    openSheet(html);

    document.getElementById('btn-type-exp')?.addEventListener('click', () => { activeType = 'expense'; renderModalContent(); });
    document.getElementById('btn-type-inc')?.addEventListener('click', () => { activeType = 'income'; renderModalContent(); });
    document.getElementById('btn-type-trf')?.addEventListener('click', () => { activeType = 'transfer'; renderModalContent(); });

    document.getElementById('entry-acc-select')?.addEventListener('change', (e) => { selectedAccId = e.target.value; });
    document.getElementById('entry-toacc-select')?.addEventListener('change', (e) => { selectedToAccId = e.target.value; });
    document.getElementById('entry-date-select')?.addEventListener('change', (e) => { selectedDateVal = e.target.value; });

    document.querySelectorAll('.btn-cat-pill').forEach(pill => {
      pill.addEventListener('click', () => {
        selectedCatId = pill.dataset.catid;
        renderModalContent();
      });
    });

    const displayEl = document.getElementById('numpad-display-val');
    document.querySelectorAll('.btn-num-key').forEach(btnKey => {
      btnKey.addEventListener('click', () => {
        const key = btnKey.dataset.key;
        if (key === 'del') {
          if (keypadVal.length > 1) {
            keypadVal = keypadVal.slice(0, -1);
            if (keypadVal === '' || keypadVal === '.') keypadVal = '0';
          } else {
            keypadVal = "0";
          }
        } else if (key === '.') {
          if (!keypadVal.includes('.')) keypadVal += '.';
        } else {
          if (keypadVal === "0.00" || keypadVal === "0") {
            keypadVal = key;
          } else {
            if (keypadVal.replace('.', '').length < 8) keypadVal += key;
          }
        }
        if (displayEl) displayEl.innerText = keypadVal;
      });
    });

    document.getElementById('btn-close-entry')?.addEventListener('click', closeSheet);

    document.getElementById('btn-submit-entry')?.addEventListener('click', async () => {
      const amtRupees = parseFloat(keypadVal);
      if (isNaN(amtRupees) || amtRupees <= 0) return;

      const noteVal = document.getElementById('entry-note-input')?.value.trim();
      const customDateVal = selectedDateVal || new Date().toISOString().split('T')[0];
      const catObj = appState.categories.find(c => c.id === selectedCatId);

      const newTx = {
        id: `tx-${Date.now()}`,
        type: activeType,
        amountMinor: Money.toMinor(amtRupees),
        accountId: selectedAccId,
        toAccountId: activeType === 'transfer' ? selectedToAccId : null,
        categoryId: activeType === 'transfer' ? (appState.categories[0]?.id || 'cat-8') : selectedCatId,
        merchant: noteVal || (activeType === 'transfer' ? 'Transfer' : (catObj ? catObj.name : 'Activity')),
        tags: [],
        date: customDateVal,
        time: new Date().toTimeString().substring(0, 5),
        note: noteVal,
        receiptBlob: null,
        source: 'Manual',
        splits: [],
        createdAt: Date.now()
      };

      await dbEngine.put('transactions', newTx);
      await loadStateFromDB();
      closeSheet();
      renderCurrentTab();
    });
  }

  renderModalContent();
}

function openTransactionDetailSheet(txId) {
  const tx = appState.transactions.find(t => t.id === txId);
  if (!tx) return;

  const acc = appState.accounts.find(a => a.id === tx.accountId);
  const cat = appState.categories.find(c => c.id === tx.categoryId);

  const html = `
    <div class="flex items-center justify-between border-b border-[#E5E3DC] pb-3">
      <h3 class="font-headers text-lg font-bold text-[#2D332A]">Transaction Details</h3>
      <button id="btn-close-detail-sheet" class="text-[#7C8079] font-sans text-xs font-bold p-1">✕</button>
    </div>

    <div class="text-center py-2">
      <div class="text-3xl font-sans font-bold text-[#2D332A] num-tabular">${Money.format(tx.amountMinor)}</div>
      <div class="text-xs font-sans text-[#7C8079] mt-0.5">${tx.merchant || 'Activity'}</div>
    </div>

    <div class="bg-[#FAF9F6] p-3.5 rounded-2xl border border-[#E5E3DC] space-y-2 font-sans text-xs">
      <div class="flex justify-between py-1 border-b border-[#F0EEE8]">
        <span class="text-[#7C8079]">Type</span>
        <span class="font-semibold text-[#2D332A] uppercase">${tx.type}</span>
      </div>
      <div class="flex justify-between py-1 border-b border-[#F0EEE8]">
        <span class="text-[#7C8079]">Account</span>
        <span class="text-[#2D332A]">${acc ? acc.name : 'N/A'}</span>
      </div>
      <div class="flex justify-between py-1 border-b border-[#F0EEE8]">
        <span class="text-[#7C8079]">Category</span>
        <span class="text-[#2D332A]">${cat ? cat.name : 'N/A'}</span>
      </div>
      <div class="flex justify-between py-1">
        <span class="text-[#7C8079]">Date</span>
        <span class="text-[#2D332A]">${tx.date} ${tx.time || ''}</span>
      </div>
    </div>

    <button id="btn-delete-tx" class="w-full py-2.5 border border-[#D87D56] text-[#D87D56] rounded-xl font-sans text-xs uppercase font-bold hover:bg-[#F7EBE3]">
      Delete Transaction
    </button>
  `;
  openSheet(html);
  document.getElementById('btn-close-detail-sheet')?.addEventListener('click', closeSheet);

  document.getElementById('btn-delete-tx').addEventListener('click', async () => {
    if (confirm("Delete this transaction?")) {
      await dbEngine.delete('transactions', txId);
      await loadStateFromDB();
      closeSheet();
      renderCurrentTab();
    }
  });
}

function openGoalDetailSheet(goalId) {
  const g = appState.goals.find(goal => goal.id === goalId);
  if (!g) return;

  const pct = g.targetAmountMinor > 0 ? Math.min(Math.round((g.currentSavedMinor / g.targetAmountMinor) * 100), 100) : 0;

  const html = `
    <div class="flex items-center justify-between border-b border-[#E5E3DC] pb-3">
      <h3 class="font-headers text-lg font-bold text-[#2D332A]">${g.name}</h3>
      <button id="btn-close-goaldetail-sheet" class="text-[#7C8079] font-sans text-xs font-bold p-1">✕</button>
    </div>

    <div class="text-center py-2">
      <div class="text-3xl font-sans font-bold text-[#3B7A57] num-tabular">${pct}%</div>
      <div class="text-xs font-sans text-[#7C8079] num-tabular">${Money.format(g.currentSavedMinor)} / ${Money.format(g.targetAmountMinor)}</div>
    </div>

    <div class="space-y-3 font-sans text-xs">
      <div class="p-3 bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl space-y-2">
        <label class="text-[9px] text-[#7C8079] uppercase block font-semibold">Deposit Funds</label>
        <div class="flex gap-2">
          <input type="number" id="in-deposit-amt" placeholder="Amount ₹" class="flex-1 bg-[#FFFFFF] border border-[#E5E3DC] rounded-xl p-2" />
          <button id="btn-deposit-goal" class="px-4 py-2 bg-[#3B7A57] text-[#FFFFFF] font-bold rounded-xl uppercase">Deposit</button>
        </div>
      </div>

      <button id="btn-delete-goal" class="w-full py-2 border border-[#D87D56] text-[#D87D56] rounded-xl font-sans text-xs uppercase font-bold">
        Delete Goal
      </button>
    </div>
  `;
  openSheet(html);
  document.getElementById('btn-close-goaldetail-sheet')?.addEventListener('click', closeSheet);

  document.getElementById('btn-deposit-goal').addEventListener('click', async () => {
    const amtRupees = parseFloat(document.getElementById('in-deposit-amt').value);
    if (isNaN(amtRupees) || amtRupees <= 0) return;

    g.currentSavedMinor += Money.toMinor(amtRupees);
    if (!g.contributions) g.contributions = [];
    g.contributions.unshift({ id: `c-${Date.now()}`, amountMinor: Money.toMinor(amtRupees), date: new Date().toISOString().split('T')[0], note: "Deposit" });

    await dbEngine.put('goals', g);
    await loadStateFromDB();

    if (g.currentSavedMinor >= g.targetAmountMinor) triggerCelebration();

    openGoalDetailSheet(goalId);
    renderCurrentTab();
  });

  document.getElementById('btn-delete-goal').addEventListener('click', async () => {
    if (confirm("Delete this goal?")) {
      await dbEngine.delete('goals', goalId);
      await loadStateFromDB();
      closeSheet();
      renderCurrentTab();
    }
  });
}

function openManageAccountsSheet() {
  const html = `
    <div class="flex items-center justify-between border-b border-[#E5E3DC] pb-3">
      <h3 class="font-headers text-lg font-bold text-[#2D332A]">Manage Accounts</h3>
      <button id="btn-close-accounts-sheet" class="text-[#7C8079] font-sans text-xs font-bold p-1">✕</button>
    </div>
    <div class="space-y-2 pt-2 max-h-[50vh] overflow-y-auto">
      ${appState.accounts.map(a => `
        <div class="p-3 bg-[#FAF9F6] rounded-xl border border-[#E5E3DC] flex justify-between items-center font-sans text-xs">
          <span>${a.icon} ${a.name} (${a.type})</span>
          ${appState.accounts.length > 1 ? `<button data-deleteacc="${a.id}" class="btn-del-acc text-[#D87D56] font-bold text-[10px] uppercase hover:underline">Delete</button>` : ''}
        </div>
      `).join('')}
    </div>
    <button id="btn-add-new-acc" class="w-full py-2.5 bg-[#3B7A57] text-[#FFFFFF] font-sans text-xs font-bold rounded-xl uppercase mt-3">+ Add Account</button>
  `;
  openSheet(html);
  document.getElementById('btn-close-accounts-sheet')?.addEventListener('click', closeSheet);

  document.querySelectorAll('.btn-del-acc').forEach(btn => {
    btn.addEventListener('click', async () => {
      const accId = btn.dataset.deleteacc;
      if (confirm("Delete this account?")) {
        await dbEngine.delete('accounts', accId);
        await loadStateFromDB();
        openManageAccountsSheet();
        renderCurrentTab();
      }
    });
  });

  document.getElementById('btn-add-new-acc')?.addEventListener('click', () => {
    const addHtml = `
      <div class="flex items-center justify-between border-b border-[#E5E3DC] pb-3">
        <h3 class="font-headers text-lg font-bold text-[#2D332A]">New Account</h3>
      </div>
      <div class="space-y-3 pt-2 font-sans text-xs">
        <input type="text" id="acc-in-name" placeholder="Account Name (e.g. HDFC)" class="w-full bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl p-2.5" />
        <select id="acc-in-type" class="w-full bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl p-2.5">
          <option>Bank Account</option><option>Cash</option><option>Credit Card</option><option>Wallet</option><option>Savings</option>
        </select>
        <input type="number" id="acc-in-bal" placeholder="Starting Balance ₹" class="w-full bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl p-2.5" />
        <button id="btn-save-acc" class="w-full py-3 bg-[#3B7A57] text-[#FFFFFF] font-bold rounded-xl uppercase">Create Account</button>
      </div>
    `;
    openSheet(addHtml);

    document.getElementById('btn-save-acc').addEventListener('click', async () => {
      const name = document.getElementById('acc-in-name').value.trim();
      const type = document.getElementById('acc-in-type').value;
      const balRupees = parseFloat(document.getElementById('acc-in-bal').value) || 0;
      if (!name) return;

      const newAcc = {
        id: `acc-${Date.now()}`,
        name: name,
        type: type,
        institution: name,
        startingBalanceMinor: Money.toMinor(balRupees),
        icon: "🏦",
        includeInNetWorth: true,
        archived: false,
        order: appState.accounts.length + 1
      };

      await dbEngine.put('accounts', newAcc);
      await loadStateFromDB();
      closeSheet();
      renderCurrentTab();
    });
  });
}

function openManageCategoriesSheet() {
  const html = `
    <div class="flex items-center justify-between border-b border-[#E5E3DC] pb-3">
      <h3 class="font-headers text-lg font-bold text-[#2D332A]">Manage Categories</h3>
      <button id="btn-close-cat-sheet" class="text-[#7C8079] font-sans text-xs font-bold p-1">✕</button>
    </div>
    <div class="space-y-2 pt-2 max-h-[50vh] overflow-y-auto">
      ${appState.categories.map(c => `
        <div class="p-3 bg-[#FAF9F6] rounded-xl border border-[#E5E3DC] flex justify-between items-center font-sans text-xs">
          <div class="flex items-center gap-2">
            <span class="text-base">${c.icon}</span>
            <span class="font-bold text-[#2D332A]">${c.name}</span>
          </div>
          <div class="text-[#7C8079] num-tabular">
            Limit: ${Money.format(c.monthlyLimitMinor)}
          </div>
        </div>
      `).join('')}
    </div>
    <button id="btn-add-new-cat" class="w-full py-2.5 bg-[#3B7A57] text-[#FFFFFF] font-sans text-xs font-bold rounded-xl uppercase mt-3">+ Add Category</button>
  `;
  openSheet(html);
  document.getElementById('btn-close-cat-sheet')?.addEventListener('click', closeSheet);

  document.getElementById('btn-add-new-cat')?.addEventListener('click', () => {
    const addHtml = `
      <div class="flex items-center justify-between border-b border-[#E5E3DC] pb-3">
        <h3 class="font-headers text-lg font-bold text-[#2D332A]">New Category</h3>
      </div>
      <div class="space-y-3 pt-2 font-sans text-xs">
        <input type="text" id="cat-in-name" placeholder="Category Name (e.g. Travel, Fitness)" class="w-full bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl p-2.5" />
        <input type="text" id="cat-in-icon" placeholder="Emoji Icon (e.g. ✈️)" class="w-full bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl p-2.5" value="📦" />
        <input type="number" id="cat-in-limit" placeholder="Monthly Limit ₹ (0 for unlimited)" class="w-full bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl p-2.5" />
        <button id="btn-save-cat" class="w-full py-3 bg-[#3B7A57] text-[#FFFFFF] font-bold rounded-xl uppercase">Create Category</button>
      </div>
    `;
    openSheet(addHtml);

    document.getElementById('btn-save-cat').addEventListener('click', async () => {
      const name = document.getElementById('cat-in-name').value.trim();
      const icon = document.getElementById('cat-in-icon').value.trim() || "📦";
      const limitRupees = parseFloat(document.getElementById('cat-in-limit').value) || 0;
      if (!name) return;

      const newCat = {
        id: `cat-${Date.now()}`,
        name: name,
        icon: icon,
        monthlyLimitMinor: Money.toMinor(limitRupees),
        order: appState.categories.length + 1,
        archived: false
      };

      await dbEngine.put('categories', newCat);
      await loadStateFromDB();
      closeSheet();
      renderCurrentTab();
    });
  });
}

function exportCSVLedger() {
  const escapeCSV = (str) => `"${(str || '').toString().replace(/"/g, '""')}"`;
  let csv = "Date,Time,Type,Amount (INR),Account,Category,Merchant,Notes,Source\n";
  appState.transactions.forEach(t => {
    const acc = appState.accounts.find(a => a.id === t.accountId);
    const cat = appState.categories.find(c => c.id === t.categoryId);
    const amtStr = Money.toMajor(t.amountMinor).toFixed(2);
    csv += `${escapeCSV(t.date)},${escapeCSV(t.time||'')},${escapeCSV(t.type)},${amtStr},${escapeCSV(acc?acc.name:'')},${escapeCSV(cat?cat.name:'')},${escapeCSV(t.merchant||'')},${escapeCSV(t.note||'')},${escapeCSV(t.source||'')}\n`;
  });

  const blob = new Blob([csv], { type: 'text/csv' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `MS_Tracker_Ledger_${new Date().toISOString().split('T')[0]}.csv`;
  a.click();
}

function downloadJSONBackup() {
  const jsonStr = JSON.stringify(appState, null, 2);
  const blob = new Blob([jsonStr], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `MS_Tracker_Backup_${new Date().toISOString().split('T')[0]}.json`;
  a.click();
}

// Service Worker & PWA Direct Installation
let deferredPWAInstallPrompt = null;

window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredPWAInstallPrompt = e;
  const btn = document.getElementById('btn-sett-install-app');
  if (btn) {
    btn.classList.add('ring-2', 'ring-[#3B7A57]');
  }
});

function promptPWAInstall() {
  if (window.Capacitor) {
    alert("You are using the native MS Tracker Android Application!");
    return;
  }
  if (deferredPWAInstallPrompt) {
    deferredPWAInstallPrompt.prompt();
    deferredPWAInstallPrompt.userChoice.then((choiceResult) => {
      if (choiceResult.outcome === 'accepted') {
        alert('MS Tracker is installing to your home screen!');
      }
      deferredPWAInstallPrompt = null;
    });
  } else {
    alert("MS Tracker Personal Finance Application");
  }
}

// Register Service Worker for PWA
if ('serviceWorker' in navigator && !window.Capacitor) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js').catch(err => console.log('SW registration error:', err));
  });
}

// Capacitor Native Hardware Back Button & Status Bar Handler
if (typeof window !== 'undefined') {
  window.addEventListener('DOMContentLoaded', () => {
    if (window.Capacitor && window.Capacitor.Plugins) {
      if (window.Capacitor.Plugins.StatusBar) {
        window.Capacitor.Plugins.StatusBar.setBackgroundColor({ color: '#F4F3EF' }).catch(() => {});
        window.Capacitor.Plugins.StatusBar.setStyle({ style: 'DARK' }).catch(() => {});
      }

      if (window.Capacitor.Plugins.App) {
        window.Capacitor.Plugins.App.addListener('backButton', () => {
          const sheetBackdrop = document.getElementById('sheet-backdrop');
          if (sheetBackdrop && !sheetBackdrop.classList.contains('hidden')) {
            closeSheet();
            return;
          }
          if (appState.currentTab !== 'dashboard') {
            const dashBtn = document.querySelector('[data-tab="dashboard"]');
            if (dashBtn) dashBtn.click();
            return;
          }
          window.Capacitor.Plugins.App.exitApp();
        });
      }
    }
  });
}

// Start Engine
window.addEventListener('DOMContentLoaded', initApp);
