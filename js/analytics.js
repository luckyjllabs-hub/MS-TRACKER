// MS Tracker — Zen Minimalist SVG Analytics Engine

import { Money } from './money.js';

export const Analytics = {
  // Render Category Donut Pie Breakdown
  renderCategoryDonut(svgElement, legendElement, categories, transactions) {
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
  renderTrendLine(svgElement, dailyTotals) {
    svgElement.innerHTML = `
      <defs>
        <linearGradient id="grad-sage" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stop-color="#8F9C8A" stop-opacity="0.5"/>
          <stop offset="100%" stop-color="#8F9C8A" stop-opacity="0"/>
        </linearGradient>
      </defs>
    `;

    const points = dailyTotals.map((item, idx) => {
      const x = 30 + (idx * 45);
      const y = 140 - (item.amountMinor / 100000 * 100);
      return { x, y: Math.max(y, 20), val: item.amountMinor };
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
    linePath.setAttribute('stroke', '#8F9C8A');
    linePath.setAttribute('stroke-width', '2');
    svgElement.appendChild(linePath);
  }
};
