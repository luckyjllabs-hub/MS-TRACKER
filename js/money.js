// MS Tracker — Financial Precision Money Utilities (Paise Integer Math)

export const Money = {
  // Convert standard rupee amount (e.g. 420.50) to minor unit integer (paise 42050)
  toMinor(rupees) {
    if (rupees === null || rupees === undefined || isNaN(rupees)) return 0;
    return Math.round(parseFloat(rupees) * 100);
  },

  // Convert minor unit integer (paise 42050) back to rupees (420.50)
  toMajor(minor) {
    if (!minor || isNaN(minor)) return 0;
    return minor / 100;
  },

  // Format minor units into clean Indian currency string (e.g. ₹4,20,500.00)
  format(minor, options = { showSymbol: true, absolute: false }) {
    const val = minor || 0;
    const absVal = Math.abs(val);
    const rupees = absVal / 100;
    
    const formatted = rupees.toLocaleString('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });

    const symbol = options.showSymbol ? '₹' : '';
    const prefix = !options.absolute && val < 0 ? '-' : '';

    return `${prefix}${symbol}${formatted}`;
  },

  // Safe addition in minor units
  add(aMinor, bMinor) {
    return (aMinor || 0) + (bMinor || 0);
  },

  // Safe subtraction in minor units
  subtract(aMinor, bMinor) {
    return (aMinor || 0) - (bMinor || 0);
  }
};
