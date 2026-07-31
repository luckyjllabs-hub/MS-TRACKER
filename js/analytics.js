// MS Tracker — Zen Minimalist SVG Analytics Engine

import { Money } from './money.js';

export const Analytics = {
  // Render Category Donut Pie Breakdown
  renderCategoryDonut(svgElement, legendElement, categories, transactions) {
    if (!svgElement || !legendElement) return;

    const catTotals = {};
    let totalExpMinor = 0;

    categories.forEach(c => catTotals[c.id] = 0);
    transactions.forEach(t => {
      if (t.type === 'expense' && catTotals[t.categoryId] !== undefined) {
        catTotals[t.categoryId] += t.amountMinor;
        totalExpMinor += t.amountMinor;
      }
    });

    svgElement.innerHTML = '';
    legendElement.innerHTML = '';

    if (totalExpMinor === 0) {
      svgElement.innerHTML = `<text x="120" y="120" fill="#7C8079" text-anchor="middle" font-family="DM Sans" font-size="12">NO EXPENSES RECORDED</text>`;
      return;
    }

    let accumulatedAngle = 0;
    const radius = 80;
    const cx = 120, cy = 120;
    const circumference = 2 * Math.PI * radius;
    const colors = ['#8F9C8A', '#D8A47F', '#5C6757', '#A4A6A1', '#B8C2B3', '#E4D5C7'];

    categories.forEach((cat, idx) => {
      const spentMinor = catTotals[cat.id] || 0;
      if (spentMinor === 0) return;

      const percentage = spentMinor / totalExpMinor;
      const angle = percentage * 360;
      const strokeLength = percentage * circumference;
      const strokeOffset = circumference - (accumulatedAngle / 360) * circumference;
      const strokeColor = colors[idx % colors.length];

      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      circle.setAttribute('cx', cx);
      circle.setAttribute('cy', cy);
      circle.setAttribute('r', radius);
      circle.setAttribute('fill', 'transparent');
      circle.setAttribute('stroke', strokeColor);
      circle.setAttribute('stroke-width', '16');
      circle.setAttribute('stroke-dasharray', `${strokeLength} ${circumference - strokeLength}`);
      circle.setAttribute('stroke-dashoffset', strokeOffset);
      circle.setAttribute('transform', `rotate(-90 ${cx} ${cy})`);
      svgElement.appendChild(circle);

      accumulatedAngle += angle;

      // Legend item
      const row = document.createElement('div');
      row.className = 'flex items-center justify-between text-xs font-sans text-[#2D332A]';
      row.innerHTML = `
        <div class="flex items-center gap-2">
          <span class="w-2.5 h-2.5 rounded-full" style="background-color: ${strokeColor}"></span>
          <span>${cat.icon} ${cat.name}</span>
        </div>
        <span class="text-[#7C8079] num-tabular">${Money.format(spentMinor)} (${(percentage * 100).toFixed(0)}%)</span>
      `;
      legendElement.appendChild(row);
    });

    // Center Total Text
    const centerVal = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    centerVal.setAttribute('x', cx);
    centerVal.setAttribute('y', cy + 4);
    centerVal.setAttribute('fill', '#2D332A');
    centerVal.setAttribute('text-anchor', 'middle');
    centerVal.setAttribute('font-family', 'DM Sans');
    centerVal.setAttribute('font-size', '16');
    centerVal.setAttribute('font-weight', 'bold');
    centerVal.textContent = Money.format(totalExpMinor);
    svgElement.appendChild(centerVal);
  },

  // Render Daily Trend SVG Line
  renderTrendLine(svgElement, transactions) {
    if (!svgElement) return;

    // Group last 7 days spending
    const days = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      days.push(d.toISOString().split('T')[0]);
    }

    const dailyTotals = days.map(dStr => {
      const dayExp = transactions
        .filter(t => t.type === 'expense' && t.date === dStr)
        .reduce((sum, t) => sum + t.amountMinor, 0);
      return { date: dStr, amountMinor: dayExp };
    });

    const maxAmt = Math.max(...dailyTotals.map(d => d.amountMinor), 10000);

    svgElement.innerHTML = `
      <defs>
        <linearGradient id="grad-sage" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stop-color="#3B7A57" stop-opacity="0.4"/>
          <stop offset="100%" stop-color="#3B7A57" stop-opacity="0"/>
        </linearGradient>
      </defs>
    `;

    const points = dailyTotals.map((item, idx) => {
      const x = 30 + (idx * 42);
      const y = 130 - ((item.amountMinor / maxAmt) * 90);
      return { x, y: Math.max(Math.min(y, 130), 20), val: item.amountMinor };
    });

    let pathD = points.map((p, idx) => `${idx === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');
    let areaD = pathD + ` L ${points[points.length - 1].x} 140 L ${points[0].x} 140 Z`;

    const poly = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    poly.setAttribute('d', areaD);
    poly.setAttribute('fill', 'url(#grad-sage)');
    svgElement.appendChild(poly);

    const linePath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    linePath.setAttribute('d', pathD);
    linePath.setAttribute('fill', 'none');
    linePath.setAttribute('stroke', '#3B7A57');
    linePath.setAttribute('stroke-width', '2.5');
    svgElement.appendChild(linePath);

    // Add points & date labels
    points.forEach((p, idx) => {
      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      circle.setAttribute('cx', p.x);
      circle.setAttribute('cy', p.y);
      circle.setAttribute('r', '4');
      circle.setAttribute('fill', '#3B7A57');
      svgElement.appendChild(circle);

      const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      label.setAttribute('x', p.x);
      label.setAttribute('y', 155);
      label.setAttribute('fill', '#7C8079');
      label.setAttribute('text-anchor', 'middle');
      label.setAttribute('font-family', 'DM Sans');
      label.setAttribute('font-size', '9');
      const dObj = new Date(days[idx]);
      const dayName = dObj.toLocaleDateString('en-US', { weekday: 'short' });
      label.textContent = dayName;
      svgElement.appendChild(label);
    });
  },

  // Render Accounts Breakdown Subtab
  renderAccountsList(container, accounts, accountBalances) {
    if (!container) return;
    container.innerHTML = '';

    accounts.filter(a => !a.archived).forEach(a => {
      const bal = accountBalances[a.id] || 0;
      const isNegative = bal < 0;
      const div = document.createElement('div');
      div.className = 'bg-[#FFFFFF] p-3.5 rounded-2xl border border-[#E5E3DC] flex items-center justify-between font-sans text-xs';
      div.innerHTML = `
        <div class="flex items-center gap-3">
          <span class="text-2xl">${a.icon || '🏦'}</span>
          <div>
            <div class="font-bold text-[#2D332A]">${a.name}</div>
            <div class="text-[10px] text-[#7C8079]">${a.type}</div>
          </div>
        </div>
        <div class="font-bold num-tabular ${isNegative ? 'text-[#D87D56]' : 'text-[#3B7A57]'}">
          ${Money.format(bal)}
        </div>
      `;
      container.appendChild(div);
    });
  },

  // Render Tags Breakdown Subtab
  renderTagsList(container, tags, transactions) {
    if (!container) return;
    container.innerHTML = '';

    const tagTotals = {};
    tags.forEach(t => tagTotals[t.name] = 0);

    transactions.forEach(t => {
      if (t.type === 'expense' && Array.isArray(t.tags)) {
        t.tags.forEach(tagName => {
          tagTotals[tagName] = (tagTotals[tagName] || 0) + t.amountMinor;
        });
      }
    });

    const activeTags = Object.keys(tagTotals);
    if (activeTags.length === 0) {
      container.innerHTML = `<div class="py-8 text-center text-xs text-[#7C8079] font-sans">No tagged spending found</div>`;
      return;
    }

    activeTags.forEach(tagName => {
      const tagObj = tags.find(t => t.name === tagName);
      const spent = tagTotals[tagName];
      const div = document.createElement('div');
      div.className = 'bg-[#FFFFFF] p-3.5 rounded-2xl border border-[#E5E3DC] flex items-center justify-between font-sans text-xs';
      div.innerHTML = `
        <div class="flex items-center gap-2.5">
          <span class="w-3 h-3 rounded-full" style="background-color: ${tagObj?.color || '#3B7A57'}"></span>
          <span class="font-bold text-[#2D332A]">#${tagName}</span>
        </div>
        <div class="font-bold text-[#2D332A] num-tabular">${Money.format(spent)}</div>
      `;
      container.appendChild(div);
    });
  },

  // Render Cashflow Monthly Bar Subtab
  renderCashflowBars(container, transactions) {
    if (!container) return;
    container.innerHTML = '';

    let monthInc = 0, monthExp = 0;
    const currentMonthStr = new Date().toISOString().substring(0, 7);

    transactions.forEach(t => {
      if (t.date.startsWith(currentMonthStr)) {
        if (t.type === 'income') monthInc += t.amountMinor;
        else if (t.type === 'expense') monthExp += t.amountMinor;
      }
    });

    const maxVal = Math.max(monthInc, monthExp, 10000);
    const incPct = Math.round((monthInc / maxVal) * 100);
    const expPct = Math.round((monthExp / maxVal) * 100);

    container.innerHTML = `
      <div class="space-y-4 font-sans text-xs">
        <div>
          <div class="flex justify-between font-bold mb-1">
            <span class="text-[#3B7A57]">Total Income</span>
            <span class="num-tabular">+${Money.format(monthInc)}</span>
          </div>
          <div class="w-full bg-[#E5E3DC] h-3 rounded-full overflow-hidden">
            <div class="bg-[#3B7A57] h-full transition-all duration-300 rounded-full" style="width: ${incPct}%"></div>
          </div>
        </div>

        <div>
          <div class="flex justify-between font-bold mb-1">
            <span class="text-[#D87D56]">Total Expenses</span>
            <span class="num-tabular">-${Money.format(monthExp)}</span>
          </div>
          <div class="w-full bg-[#E5E3DC] h-3 rounded-full overflow-hidden">
            <div class="bg-[#D87D56] h-full transition-all duration-300 rounded-full" style="width: ${expPct}%"></div>
          </div>
        </div>

        <div class="pt-3 border-t border-[#F0EEE8] flex justify-between font-bold text-sm">
          <span class="text-[#2D332A]">Net Cashflow</span>
          <span class="num-tabular ${monthInc >= monthExp ? 'text-[#3B7A57]' : 'text-[#D87D56]'}">${Money.format(monthInc - monthExp)}</span>
        </div>
      </div>
    `;
  }
};
