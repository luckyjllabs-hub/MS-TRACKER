// MS Tracker — Reusable Zen Minimalist Component Generators

import { Money } from './money.js';

export const Components = {
  // 1. Account Card for Carousel
  AccountCard(account, balanceMinor) {
    const isNegative = balanceMinor < 0;
    const formatted = Money.format(balanceMinor);

    return `
      <div class="flex-shrink-0 w-36 bg-[#FAF9F6] border border-[#E5E3DC] p-3.5 rounded-2xl flex flex-col justify-between select-none hover:border-[#8F9C8A] transition-colors">
        <div class="flex items-center justify-between">
          <span class="text-xl">${account.icon || '🏦'}</span>
          <span class="text-[9px] font-sans text-[#7C8079] uppercase font-medium">${account.type.split(' ')[0]}</span>
        </div>
        <div class="mt-3">
          <div class="text-[11px] font-sans text-[#7C8079] font-medium truncate">${account.name}</div>
          <div class="font-sans text-xs font-bold ${isNegative ? 'text-[#D8A47F]' : 'text-[#2D332A]'} mt-0.5 num-tabular">${formatted}</div>
        </div>
      </div>
    `;
  },

  // 2. Transaction Row Element (Matching Zen Pill Aesthetics with Category/Account subtext)
  TransactionRow(tx, accountName, categoryIcon, categoryName) {
    const isExpense = tx.type === 'expense';
    const isIncome = tx.type === 'income';
    const sign = isExpense ? '-' : (isIncome ? '+' : '⇆ ');
    const amtColor = isExpense ? 'text-[#2D332A]' : (isIncome ? 'text-[#5C6757]' : 'text-[#5C6757]');
    const formattedAmt = Money.format(tx.amountMinor, { absolute: true });

    return `
      <div class="p-3.5 flex items-center justify-between hover:bg-[#F4F3EF]/60 transition-colors cursor-pointer select-none border-b border-[#F0EEE8] last:border-0" data-txid="${tx.id}">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-[#E4E8E3] border border-[#8F9C8A]/20 flex items-center justify-center text-base flex-shrink-0">
            ${categoryIcon || '📦'}
          </div>
          <div>
            <div class="text-xs font-sans font-bold text-[#2D332A]">
              ${tx.merchant || categoryName}
            </div>
            <div class="text-[10px] font-sans text-[#7C8079] mt-0.5">
              ${categoryName} • ${accountName || 'Primary'}
            </div>
          </div>
        </div>
        <div class="text-right font-sans">
          <span class="${amtColor} font-bold text-xs num-tabular">${sign}${formattedAmt}</span>
        </div>
      </div>
    `;
  },

  // 3. Goal Card for 2-Column Grid
  GoalCard(goal) {
    const pct = Math.min(Math.round((goal.currentSavedMinor / goal.targetAmountMinor) * 100), 100);
    const deadlineDiff = goal.deadline ? Math.ceil((new Date(goal.deadline) - new Date()) / (1000 * 60 * 60 * 24)) : null;

    return `
      <div class="bg-[#FAF9F6] p-4 rounded-2xl border border-[#E5E3DC] space-y-3 select-none hover:border-[#8F9C8A] transition-colors cursor-pointer" data-goalid="${goal.id}">
        <div class="flex items-center justify-between">
          <span class="text-2xl">${goal.icon || '🎯'}</span>
          <span class="text-xs font-sans font-bold text-[#8F9C8A] num-tabular">${pct}%</span>
        </div>
        <div>
          <h4 class="font-sans text-xs font-bold text-[#2D332A] truncate">${goal.name}</h4>
          <p class="text-[10px] font-sans text-[#7C8079] mt-0.5 num-tabular">${Money.format(goal.currentSavedMinor)} / ${Money.format(goal.targetAmountMinor)}</p>
        </div>
        <div class="w-full bg-[#E5E3DC] h-1.5 rounded-full overflow-hidden">
          <div class="bg-[#8F9C8A] h-full transition-all duration-300" style="width: ${pct}%"></div>
        </div>
        <div class="text-[9px] font-sans text-[#7C8079] flex items-center justify-between pt-1 border-t border-[#F0EEE8]">
          <span>${deadlineDiff !== null ? (deadlineDiff > 0 ? `${deadlineDiff} days left` : 'Completed/Due') : 'Goal'}</span>
          <span class="text-[#8F9C8A] font-semibold">Deposit +</span>
        </div>
      </div>
    `;
  },

  // 4. Upcoming Bill Row Item
  UpcomingBillRow(rec, categoryName) {
    return `
      <div class="p-3 bg-[#FAF9F6] border border-[#E5E3DC] rounded-xl flex items-center justify-between select-none">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-lg bg-[#E4E8E3] text-[#5C6757] flex items-center justify-center text-xs">📅</div>
          <div>
            <div class="text-xs font-sans font-semibold text-[#2D332A]">${rec.name}</div>
            <div class="text-[9px] font-sans text-[#7C8079]">Due ${rec.nextDueDate} • ${rec.frequency}</div>
          </div>
        </div>
        <div class="text-[#2D332A] font-sans text-xs font-bold num-tabular">-${Money.format(rec.amountMinor)}</div>
      </div>
    `;
  }
};
