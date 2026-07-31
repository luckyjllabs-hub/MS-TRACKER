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
  document.getElementById('btn-manage-accounts')?.addEventListener('click', openManageAccountsSheet);
  document.getElementById('btn-view-full-ledger')?.addEventListener('click', () => switchTab('transactions'));
  document.getElementById('btn-create-goal')?.addEventListener('click', () => { if (typeof openCreateGoalSheet === 'function') openCreateGoalSheet(); });
  // Net Worth Chart Toggle & Eye Privacy Mask
  document.getElementById('btn-toggle-networth-chart')?.addEventListener('click', toggleNetWorthChartView);
  document.getElementById('btn-toggle-eye')?.addEventListener('click', togglePrivacyMask);

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
      btn.className = 'nav-tab-btn flex-1 h-full flex flex-col items-center justify-center text-[#3B7A57] font-bold';
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

// 1. Render Dashboard (Visual Overview Overhaul)
function renderDashboardView() {
  let netWorthMinor = 0;
  let monthIncMinor = 0;
  let monthExpMinor = 0;
  const currentMonthStr = new Date().toISOString().substring(0, 7);

  const accountBalances = {};
  appState.accounts.forEach(a => accountBalances[a.id] = a.startingBalanceMinor || 0);

  appState.transactions.forEach(tx => {
    if (tx.type === 'expense') {
      if (accountBalances[tx.accountId] !== undefined) accountBalances[tx.accountId] -= tx.amountMinor;
      if (tx.date.startsWith(currentMonthStr)) monthExpMinor += tx.amountMinor;
    } else if (tx.type === 'income') {
      if (accountBalances[tx.accountId] !== undefined) accountBalances[tx.accountId] += tx.amountMinor;
      if (tx.date.startsWith(currentMonthStr)) monthIncMinor += tx.amountMinor;
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

  const isNetWorthNegative = netWorthMinor < 0;

  // Render Net Worth Value & Growth styling
  const netWorthValEl = document.getElementById('dash-networth-val');
  netWorthValEl.innerText = Money.format(netWorthMinor);
  if (isNetWorthNegative) {
    netWorthValEl.className = 'font-headers text-3xl sm:text-4xl font-bold tracking-tight text-[#D87D56] num-tabular';
  } else {
    netWorthValEl.className = 'font-headers text-3xl sm:text-4xl font-bold tracking-tight text-[#2D332A] num-tabular';
  }

  // Mirrored Trend Line Graph for Negative Net Worth
  renderNetWorthTrendGraph(isNetWorthNegative);

  document.getElementById('dash-income-val').innerText = `+${Money.format(monthIncMinor)}`;
  document.getElementById('dash-expense-val').innerText = `-${Money.format(monthExpMinor)}`;

  // Saved & Budget Left metrics for 4-column grid
  const savedMinor = Math.max(monthIncMinor - monthExpMinor, 0);
  const totalBudgetMinor = appState.categories.reduce((sum, c) => sum + (c.monthlyLimitMinor || 0), 0);
  const budgetLeftMinor = Math.max(totalBudgetMinor - monthExpMinor, 0);
  const budgetLeftPct = totalBudgetMinor > 0 ? Math.round((budgetLeftMinor / totalBudgetMinor) * 100) : 0;

  const savedEl = document.getElementById('dash-saved-val');
  if (savedEl) savedEl.innerText = Money.format(savedMinor);

  const budgetLeftEl = document.getElementById('dash-budget-left-val');
  if (budgetLeftEl) budgetLeftEl.innerText = Money.format(budgetLeftMinor);

  const budgetPctEl = document.getElementById('dash-budget-left-pct');
  if (budgetPctEl) budgetPctEl.innerText = `${budgetLeftPct}% left`;

  // Populate Accounts Carousel
  populateAccountsCarousel(accountBalances);

  // Render Pictorial Spending Donut Breakdown & Net Worth Pie Chart
  renderOverviewDonutChart(monthExpMinor);
  renderNetWorthPieChart(accountBalances, isNetWorthNegative);

  // Monthly Budget Gauge
  const spentMinor = monthExpMinor;
  const pct = totalBudgetMinor > 0 ? Math.min((spentMinor / totalBudgetMinor) * 100, 100) : 0;

  document.getElementById('dash-spent-val').innerText = Money.format(spentMinor);
  document.getElementById('dash-limit-val').innerText = `of ${Money.format(totalBudgetMinor)} Limit`;
  document.getElementById('dash-budget-progress-bar').style.width = `${pct}%`;

  // Recent Transactions List (Sorted Chronologically Descending: Most Recent Payment at Top)
  const recentList = document.getElementById('dash-recent-tx-list');
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

  // Attach click listeners to transaction rows
  document.querySelectorAll('#dash-recent-tx-list [data-txid]').forEach(row => {
    row.addEventListener('click', () => openTransactionDetailSheet(row.dataset.txid));
  });

  document.getElementById('badge-sms-count').innerText = appState.smsQueue.length;
}

// Render Net Worth Trend Line Graph (Supports Positive Growth vs Negative Deficit Mirroring)
function renderNetWorthTrendGraph(isNegative = false) {
  const svg = document.getElementById('dash-hero-trend-svg');
  if (!svg) return;

  const color = isNegative ? '#D87D56' : '#3B7A57';
  const gradId = isNegative ? 'netWorthGradNeg' : 'netWorthGrad';

  // Upward growth curve for positive vs downward slope curve for negative (viewBox 0 0 300 60)
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

// Pictorial Donut Generator for Overview
function renderOverviewDonutChart(totalExpenseMinor) {
  const svg = document.getElementById('dash-overview-donut-svg');
  const legend = document.getElementById('dash-overview-legend-pills');
  if (!svg || !legend) return;

  const currentMonthStr = new Date().toISOString().substring(0, 7);
  const catTotals = {};

  appState.transactions.forEach(t => {
    if (t.type === 'expense' && t.date.startsWith(currentMonthStr)) {
      catTotals[t.categoryId] = (catTotals[t.categoryId] || 0) + t.amountMinor;
    }
  });

  const catItems = Object.keys(catTotals).map(catId => {
    const cat = appState.categories.find(c => c.id === catId);
    return {
      id: catId,
      name: cat ? cat.name : 'Other',
      icon: cat ? cat.icon : '📦',
      amountMinor: catTotals[catId]
    };
  }).sort((a, b) => b.amountMinor - a.amountMinor);

  if (catItems.length === 0) {
    svg.innerHTML = `<circle cx="50" cy="50" r="40" stroke="#E5E3DC" stroke-width="12" fill="none"/>`;
    legend.innerHTML = `<div class="text-[#7C8079] text-xs font-sans">No spending recorded this month</div>`;
    return;
  }

  const colors = ['#3B7A57', '#D87D56', '#8F9C8A', '#D8A47F', '#5C6757', '#94A3B8'];
  let strokeDashoffset = 0;
  const radius = 40;
  const circumference = 2 * Math.PI * radius;

  let svgPaths = '';
  let legendHtml = '';

  catItems.slice(0, 3).forEach((item, idx) => {
    const pct = item.amountMinor / totalExpenseMinor;
    const strokeDasharray = `${pct * circumference} ${circumference}`;
    const strokeColor = colors[idx % colors.length];

    svgPaths += `<circle cx="50" cy="50" r="${radius}" stroke="${strokeColor}" stroke-width="12" fill="none" stroke-dasharray="${strokeDasharray}" stroke-dashoffset="${-strokeDashoffset}"/>`;
    strokeDashoffset += pct * circumference;

    legendHtml += `
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="w-2.5 h-2.5 rounded-full" style="background-color: ${strokeColor}"></span>
          <span class="font-semibold text-[#2D332A] truncate max-w-[90px]">${item.icon} ${item.name}</span>
        </div>
        <span class="font-bold text-[#2D332A] num-tabular">${Money.format(item.amountMinor)}</span>
      </div>
    `;
  });

  svg.innerHTML = svgPaths;
  legend.innerHTML = legendHtml;
}

// Toggle Privacy Mask for Sensitive Net Worth Amounts
let isPrivacyMasked = false;
function togglePrivacyMask() {
  const netWorthEl = document.getElementById('dash-networth-val');
  const eyeBtn = document.getElementById('btn-toggle-eye');
  if (!netWorthEl) return;

  isPrivacyMasked = !isPrivacyMasked;
  if (isPrivacyMasked) {
    netWorthEl.dataset.realVal = netWorthEl.innerText;
    netWorthEl.innerText = "••••••••";
    if (eyeBtn) eyeBtn.innerText = "🙈";
  } else {
    if (netWorthEl.dataset.realVal) netWorthEl.innerText = netWorthEl.dataset.realVal;
    if (eyeBtn) eyeBtn.innerText = "👁️";
  }
}

// Toggle Net Worth Chart View (Line Graph <-> Pie Chart)
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

// Render Net Worth Account Distribution Pie Chart (Mirrors Positive vs Negative Net Worth)
function renderNetWorthPieChart(accountBalances, isNegativeNetWorth = false) {
  const svg = document.getElementById('dash-networth-pie-svg');
  if (!svg) return;
  const legend = document.getElementById('dash-networth-pie-legend');

  // Filter relevant accounts: positive assets when net worth is positive, or negative liabilities when negative
  let validAccs = [];
  if (isNegativeNetWorth) {
    validAccs = appState.accounts.filter(a => !a.archived && (accountBalances[a.id] || 0) < 0);
  } else {
    validAccs = appState.accounts.filter(a => !a.archived && (accountBalances[a.id] || 0) > 0);
  }

  // Fallback to all non-zero accounts if filtered list is empty
  if (validAccs.length === 0) {
    validAccs = appState.accounts.filter(a => !a.archived && (accountBalances[a.id] || 0) !== 0);
  }

  const totalVal = validAccs.reduce((sum, a) => sum + Math.abs(accountBalances[a.id]), 0);

  if (validAccs.length === 0 || totalVal <= 0) {
    svg.innerHTML = `<circle cx="50" cy="50" r="35" stroke="#E5E3DC" stroke-width="18" fill="none"/>`;
    if (legend) legend.innerHTML = `<div class="text-[#7C8079] text-xs font-sans">No balance data</div>`;
    return;
  }

  const colors = isNegativeNetWorth
    ? ['#D87D56', '#D8A47F', '#C2593F', '#E89D75', '#B84328']
    : ['#3B7A57', '#D87D56', '#8F9C8A', '#D8A47F', '#5C6757', '#94A3B8'];

  let strokeDashoffset = 0;
  const radius = 35;
  const circumference = 2 * Math.PI * radius;

  let svgPaths = '';
  let legendHtml = '';

  validAccs.forEach((acc, idx) => {
    const bal = Math.abs(accountBalances[acc.id]);
    const pct = bal / totalVal;
    const strokeDasharray = `${pct * circumference} ${circumference}`;
    const strokeColor = colors[idx % colors.length];

    svgPaths += `<circle cx="50" cy="50" r="${radius}" stroke="${strokeColor}" stroke-width="18" fill="none" stroke-dasharray="${strokeDasharray}" stroke-dashoffset="${-strokeDashoffset}"/>`;
    strokeDashoffset += pct * circumference;

    legendHtml += `
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-1.5">
          <span class="w-2.5 h-2.5 rounded-full" style="background-color: ${strokeColor}"></span>
          <span class="font-semibold text-[#2D332A] truncate max-w-[90px]">${acc.icon || '🏦'} ${acc.name}</span>
        </div>
        <span class="font-bold text-[#2D332A] num-tabular">${Money.format(accountBalances[acc.id])}</span>
      </div>
    `;
  });

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

  if (appState.currentAnalyticsSubtab === 'categories') {
    Analytics.renderCategoryDonut(
      document.getElementById('an-donut-svg'),
      document.getElementById('an-donut-legend'),
      appState.categories,
      appState.transactions
    );
  }
}

// Sheets Utility Controls
function openSheet(htmlContent) {
  const backdrop = document.getElementById('sheet-backdrop');
  const area = document.getElementById('sheet-content-area');
  area.innerHTML = htmlContent;
  backdrop.classList.remove('hidden');
}

function closeSheet() {
  document.getElementById('sheet-backdrop').classList.add('hidden');
}

// Filter Bottom Sheet (Category-First Filtering)
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

  // Type filter toggle buttons
  document.querySelectorAll('.btn-type-filter').forEach(btn => {
    btn.addEventListener('click', () => {
      appState.currentTxFilter = btn.dataset.typefilter;
      document.querySelectorAll('.btn-type-filter').forEach(b => {
        b.className = `btn-type-filter flex-1 py-2 text-center rounded-full font-bold text-xs transition-all ${b.dataset.typefilter === appState.currentTxFilter ? 'bg-[#FFFFFF] text-[#2D332A] shadow-xs' : 'text-[#7C8079]'}`;
      });
    });
  });

  // Category pill selection toggle
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

  const html = `
    <div class="bg-[#F4F3EF] min-h-[620px] flex flex-col justify-between -m-5 p-6 rounded-t-3xl text-[#2D332A] font-sans fade-in">
      <div class="flex items-center justify-between border-b border-[#E5E3DC]/60 pb-3">
        <button id="btn-close-entry" class="text-[#7C8079] hover:text-[#2D332A] text-lg font-bold p-1 w-8 h-8 rounded-full flex items-center justify-center hover:bg-[#E4E8E3] transition-colors">✕</button>
        <h2 class="font-headers text-xl text-[#2D332A] font-bold tracking-tight">New Entry</h2>
        <div class="w-8"></div>
      </div>

      <div class="bg-[#E5E3DC]/60 p-1.5 rounded-full flex items-center shadow-inner my-2">
        <button id="btn-type-exp" class="flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all bg-[#FFFFFF] text-[#2D332A] shadow-sm">Expense</button>
        <button id="btn-type-inc" class="flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all text-[#7C8079] hover:text-[#2D332A]">Income</button>
        <button id="btn-type-trf" class="flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all text-[#7C8079] hover:text-[#2D332A]">Transfer</button>
      </div>

      <div class="flex justify-center items-center gap-2 my-1">
        <div class="bg-[#FFFFFF] border border-[#E5E3DC] rounded-full px-3.5 py-1.5 flex items-center gap-1.5 shadow-xs hover:border-[#3B7A57] transition-colors">
          <span class="text-xs text-[#7C8079]">Account:</span>
          <select id="entry-acc-select" class="bg-transparent font-sans text-xs font-bold text-[#3B7A57] focus:outline-none cursor-pointer">
            ${appState.accounts.map(a => `<option value="${a.id}">${a.icon} ${a.name}</option>`).join('')}
          </select>
        </div>

        <div class="bg-[#FFFFFF] border border-[#E5E3DC] rounded-full px-3 py-1.5 flex items-center gap-1.5 shadow-xs hover:border-[#3B7A57] transition-colors">
          <span class="text-xs text-[#3B7A57]">📅</span>
          <input type="date" id="entry-date-select" class="bg-transparent font-sans text-xs font-bold text-[#2D332A] focus:outline-none cursor-pointer" value="${new Date().toISOString().split('T')[0]}" />
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

      <div class="my-1">
        <div class="text-[11px] font-bold text-[#3B7A57] mb-2 px-1 uppercase tracking-wider">Category</div>
        <div id="category-pills-row" class="flex gap-2 overflow-x-auto pb-1.5 no-scrollbar">
          ${appState.categories.map((c, idx) => `
            <button data-catid="${c.id}" class="btn-cat-pill flex-shrink-0 px-4 py-2 rounded-full border text-xs font-semibold flex items-center gap-2 transition-all ${idx === 0 ? 'bg-[#3B7A57] text-[#FFFFFF] border-[#3B7A57] shadow-sm' : 'bg-[#FFFFFF] text-[#2D332A] border-[#E5E3DC] hover:border-[#3B7A57]'}">
              <span class="text-sm">${c.icon}</span>
              <span>${c.name}</span>
            </button>
          `).join('')}
        </div>
      </div>

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

  const btnExp = document.getElementById('btn-type-exp');
  const btnInc = document.getElementById('btn-type-inc');
  const btnTrf = document.getElementById('btn-type-trf');

  btnExp.addEventListener('click', () => {
    activeType = 'expense';
    btnExp.className = 'flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all bg-[#FFFFFF] text-[#2D332A] shadow-sm';
    btnInc.className = 'flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all text-[#7C8079] hover:text-[#2D332A]';
    btnTrf.className = 'flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all text-[#7C8079] hover:text-[#2D332A]';
  });

  btnInc.addEventListener('click', () => {
    activeType = 'income';
    btnInc.className = 'flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all bg-[#FFFFFF] text-[#2D332A] shadow-sm';
    btnExp.className = 'flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all text-[#7C8079] hover:text-[#2D332A]';
    btnTrf.className = 'flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all text-[#7C8079] hover:text-[#2D332A]';
  });

  btnTrf.addEventListener('click', () => {
    activeType = 'transfer';
    btnTrf.className = 'flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all bg-[#FFFFFF] text-[#2D332A] shadow-sm';
    btnExp.className = 'flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all text-[#7C8079] hover:text-[#2D332A]';
    btnInc.className = 'flex-1 py-2 text-center rounded-full font-semibold text-xs transition-all text-[#7C8079] hover:text-[#2D332A]';
  });

  document.getElementById('entry-acc-select').addEventListener('change', (e) => {
    selectedAccId = e.target.value;
  });

  document.querySelectorAll('.btn-cat-pill').forEach(pill => {
    pill.addEventListener('click', () => {
      selectedCatId = pill.dataset.catid;
      document.querySelectorAll('.btn-cat-pill').forEach(p => {
        p.className = 'btn-cat-pill flex-shrink-0 px-4 py-2 rounded-full border text-xs font-semibold flex items-center gap-2 transition-all bg-[#FFFFFF] text-[#2D332A] border-[#E5E3DC] hover:border-[#3B7A57]';
      });
      pill.className = 'btn-cat-pill flex-shrink-0 px-4 py-2 rounded-full border text-xs font-semibold flex items-center gap-2 transition-all bg-[#3B7A57] text-[#FFFFFF] border-[#3B7A57] shadow-sm';
    });
  });

  const displayEl = document.getElementById('numpad-display-val');
  document.querySelectorAll('.btn-num-key').forEach(btnKey => {
    btnKey.addEventListener('click', () => {
      const key = btnKey.dataset.key;
      if (key === 'del') {
        if (keypadVal.length > 1) {
          keypadVal = keypadVal.slice(0, -1);
        } else {
          keypadVal = "0.00";
        }
      } else if (key === '.') {
        if (!keypadVal.includes('.')) {
          keypadVal += '.';
        }
      } else {
        if (keypadVal === "0.00" || keypadVal === "0") {
          keypadVal = key;
        } else {
          if (keypadVal.replace('.', '').length < 8) {
            keypadVal += key;
          }
        }
      }
      displayEl.innerText = keypadVal;
    });
  });

  document.getElementById('btn-close-entry').addEventListener('click', closeSheet);

  document.getElementById('btn-submit-entry').addEventListener('click', async () => {
    const amtRupees = parseFloat(keypadVal);
    if (isNaN(amtRupees) || amtRupees <= 0) return;

    const noteVal = document.getElementById('entry-note-input').value.trim();
    const customDateVal = document.getElementById('entry-date-select')?.value || new Date().toISOString().split('T')[0];
    const catObj = appState.categories.find(c => c.id === selectedCatId);

    const newTx = {
      id: `tx-${Date.now()}`,
      type: activeType,
      amountMinor: Money.toMinor(amtRupees),
      accountId: selectedAccId,
      toAccountId: null,
      categoryId: selectedCatId,
      merchant: noteVal || (catObj ? catObj.name : 'Activity'),
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

function openTransactionDetailSheet(txId) {
  const tx = appState.transactions.find(t => t.id === txId);
  if (!tx) return;

  const acc = appState.accounts.find(a => a.id === tx.accountId);
  const cat = appState.categories.find(c => c.id === tx.categoryId);

  const html = `
    <div class="flex items-center justify-between border-b border-[#E5E3DC] pb-3">
      <h3 class="font-headers text-lg font-bold text-[#2D332A]">Transaction Details</h3>
      <button onclick="document.getElementById('sheet-backdrop').classList.add('hidden')" class="text-[#7C8079] font-sans text-xs">[ESC]</button>
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

  const pct = Math.min(Math.round((g.currentSavedMinor / g.targetAmountMinor) * 100), 100);

  const html = `
    <div class="flex items-center justify-between border-b border-[#E5E3DC] pb-3">
      <h3 class="font-headers text-lg font-bold text-[#2D332A]">${g.name}</h3>
      <button onclick="document.getElementById('sheet-backdrop').classList.add('hidden')" class="text-[#7C8079] font-sans text-xs">[ESC]</button>
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
    </div>
    <div class="space-y-2 pt-2">
      ${appState.accounts.map(a => `<div class="p-3 bg-[#FAF9F6] rounded-xl border border-[#E5E3DC] flex justify-between font-sans text-xs"><span>${a.icon} ${a.name} (${a.type})</span></div>`).join('')}
    </div>
    <button id="btn-add-new-acc" class="w-full py-2.5 bg-[#3B7A57] text-[#FFFFFF] font-sans text-xs font-bold rounded-xl uppercase mt-2">+ Add Account</button>
  `;
  openSheet(html);

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
    </div>
    <div class="space-y-2 pt-2">
      ${appState.categories.map(c => `<div class="p-3 bg-[#FAF9F6] rounded-xl border border-[#E5E3DC] flex justify-between font-sans text-xs"><span>${c.icon} ${c.name}</span><span class="text-[#7C8079] num-tabular">Limit: ${Money.format(c.monthlyLimitMinor)}</span></div>`).join('')}
    </div>
  `;
  openSheet(html);
}

function exportCSVLedger() {
  let csv = "Date,Time,Type,Amount (INR),Account,Category,Merchant,Notes,Source\n";
  appState.transactions.forEach(t => {
    const acc = appState.accounts.find(a => a.id === t.accountId);
    const cat = appState.categories.find(c => c.id === t.categoryId);
    const amtStr = Money.toMajor(t.amountMinor).toFixed(2);
    csv += `"${t.date}","${t.time||''}","${t.type}",${amtStr},"${acc?acc.name:''}","${cat?cat.name:''}","${t.merchant||''}","${t.note||''}","${t.source||''}"\n`;
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

// Register Service Worker for PWA (Bypass if running inside Capacitor Native App)
if ('serviceWorker' in navigator && !window.Capacitor) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js').catch(err => console.log('SW registration error:', err));
  });
}

// Capacitor Native Hardware Back Button & Status Bar Handler
if (typeof window !== 'undefined') {
  window.addEventListener('DOMContentLoaded', () => {
    if (window.Capacitor && window.Capacitor.Plugins) {
      // Configure Status Bar
      if (window.Capacitor.Plugins.StatusBar) {
        window.Capacitor.Plugins.StatusBar.setBackgroundColor({ color: '#F4F3EF' }).catch(() => {});
        window.Capacitor.Plugins.StatusBar.setStyle({ style: 'DARK' }).catch(() => {});
      }

      // Configure Back Button Handler
      if (window.Capacitor.Plugins.App) {
        window.Capacitor.Plugins.App.addListener('backButton', () => {
          const sheetBackdrop = document.getElementById('sheet-backdrop');
          if (sheetBackdrop && !sheetBackdrop.classList.contains('hidden')) {
            Components.closeBottomSheet();
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
