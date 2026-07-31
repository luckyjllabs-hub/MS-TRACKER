// MS Tracker — Local-First IndexedDB Repository Layer

const DB_NAME = "MS_Tracker_DB";
const DB_VERSION = 1;

class MSDatabase {
  constructor() {
    this.db = null;
  }

  async init() {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION);

      request.onupgradeneeded = (evt) => {
        const db = evt.target.result;
        
        // 1. Accounts Store
        if (!db.objectStoreNames.contains("accounts")) {
          const accStore = db.createObjectStore("accounts", { keyPath: "id" });
          accStore.createIndex("order", "order", { unique: false });
        }

        // 2. Transactions Store
        if (!db.objectStoreNames.contains("transactions")) {
          const txStore = db.createObjectStore("transactions", { keyPath: "id" });
          txStore.createIndex("date", "date", { unique: false });
          txStore.createIndex("type", "type", { unique: false });
          txStore.createIndex("accountId", "accountId", { unique: false });
        }

        // 3. Categories Store
        if (!db.objectStoreNames.contains("categories")) {
          const catStore = db.createObjectStore("categories", { keyPath: "id" });
          catStore.createIndex("order", "order", { unique: false });
        }

        // 4. Tags Store
        if (!db.objectStoreNames.contains("tags")) {
          db.createObjectStore("tags", { keyPath: "id" });
        }

        // 5. Goals Store
        if (!db.objectStoreNames.contains("goals")) {
          db.createObjectStore("goals", { keyPath: "id" });
        }

        // 6. Recurring Store
        if (!db.objectStoreNames.contains("recurring")) {
          db.createObjectStore("recurring", { keyPath: "id" });
        }

        // 7. SMS Queue Store
        if (!db.objectStoreNames.contains("smsQueue")) {
          db.createObjectStore("smsQueue", { keyPath: "id" });
        }

        // 8. SMS Rules Store
        if (!db.objectStoreNames.contains("smsRules")) {
          db.createObjectStore("smsRules", { keyPath: "id" });
        }

        // 9. Settings Store
        if (!db.objectStoreNames.contains("settings")) {
          db.createObjectStore("settings", { keyPath: "key" });
        }
      };

      request.onsuccess = (evt) => {
        this.db = evt.target.result;
        resolve(this);
      };

      request.onerror = (evt) => {
        reject(evt.target.error);
      };
    });
  }

  // Generic Store Methods
  async getAll(storeName) {
    return new Promise((resolve, reject) => {
      const tx = this.db.transaction(storeName, "readonly");
      const store = tx.objectStore(storeName);
      const req = store.getAll();
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }

  async put(storeName, item) {
    return new Promise((resolve, reject) => {
      const tx = this.db.transaction(storeName, "readwrite");
      const store = tx.objectStore(storeName);
      const req = store.put(item);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }

  async delete(storeName, id) {
    return new Promise((resolve, reject) => {
      const tx = this.db.transaction(storeName, "readwrite");
      const store = tx.objectStore(storeName);
      const req = store.delete(id);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }

  async clearStore(storeName) {
    return new Promise((resolve, reject) => {
      const tx = this.db.transaction(storeName, "readwrite");
      const store = tx.objectStore(storeName);
      const req = store.clear();
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }

  // Seed Initial Baseline Dataset if Store is Empty
  async seedDefaultsIfEmpty() {
    const accounts = await this.getAll("accounts");
    if (accounts.length === 0) {
      // Seed Accounts
      const defaultAccounts = [
        { id: "acc-1", name: "HDFC Bank", type: "Bank Account", institution: "HDFC", startingBalanceMinor: 245000, icon: "🏦", includeInNetWorth: true, archived: false, order: 1 },
        { id: "acc-2", name: "Cash Wallet", type: "Cash", institution: "Cash", startingBalanceMinor: 12000, icon: "💵", includeInNetWorth: true, archived: false, order: 2 },
        { id: "acc-3", name: "Credit Card", type: "Credit Card", institution: "ICICI", startingBalanceMinor: -35000, icon: "💳", includeInNetWorth: true, archived: false, order: 3 }
      ];
      for (const a of defaultAccounts) await this.put("accounts", a);

      // Seed Categories
      const defaultCategories = [
        { id: "cat-1", name: "Food & Drink", icon: "🍔", monthlyLimitMinor: 500000, order: 1, archived: false },
        { id: "cat-2", name: "Transport", icon: "🚗", monthlyLimitMinor: 300000, order: 2, archived: false },
        { id: "cat-3", name: "Shopping", icon: "🛍️", monthlyLimitMinor: 400000, order: 3, archived: false },
        { id: "cat-4", name: "Entertainment", icon: "🎬", monthlyLimitMinor: 200000, order: 4, archived: false },
        { id: "cat-5", name: "College", icon: "🎓", monthlyLimitMinor: 250000, order: 5, archived: false },
        { id: "cat-6", name: "Subscriptions", icon: "📺", monthlyLimitMinor: 100000, order: 6, archived: false },
        { id: "cat-7", name: "Health", icon: "🏥", monthlyLimitMinor: 150000, order: 7, archived: false },
        { id: "cat-8", name: "Other", icon: "📦", monthlyLimitMinor: 0, order: 8, archived: false }
      ];
      for (const c of defaultCategories) await this.put("categories", c);

      // Seed Tags
      const defaultTags = [
        { id: "tag-1", name: "Goa Trip", color: "#8F9C8A" },
        { id: "tag-2", name: "College", color: "#D8A47F" },
        { id: "tag-3", name: "Date Night", color: "#7C8079" }
      ];
      for (const t of defaultTags) await this.put("tags", t);

      // Seed Transactions
      const todayStr = new Date().toISOString().split("T")[0];
      const defaultTransactions = [
        { id: "tx-1", type: "expense", amountMinor: 450, accountId: "acc-1", toAccountId: null, categoryId: "cat-1", merchant: "Starbucks", tags: ["Date Night"], date: todayStr, time: "14:30", note: "Iced Latte", receiptBlob: null, source: "SMS Detected", splits: [], createdAt: Date.now() },
        { id: "tx-2", type: "expense", amountMinor: 1230, accountId: "acc-3", toAccountId: null, categoryId: "cat-2", merchant: "Uber", tags: [], date: todayStr, time: "11:15", note: "Ride downtown", receiptBlob: null, source: "Manual", splits: [], createdAt: Date.now() - 1000 },
        { id: "tx-3", type: "expense", amountMinor: 8500, accountId: "acc-1", toAccountId: null, categoryId: "cat-3", merchant: "Amazon", tags: ["Goa Trip"], date: todayStr, time: "18:20", note: "Beach gear", receiptBlob: null, source: "Manual", splits: [], createdAt: Date.now() - 2000 },
        { id: "tx-4", type: "income", amountMinor: 300000, accountId: "acc-1", toAccountId: null, categoryId: "cat-8", merchant: "Payroll Inc", tags: [], date: todayStr.substring(0, 8) + "01", time: "09:00", note: "Monthly Salary", receiptBlob: null, source: "Manual", splits: [], createdAt: Date.now() - 3000 }
      ];
      for (const tx of defaultTransactions) await this.put("transactions", tx);

      // Seed Goals
      const defaultGoals = [
        { id: "goal-1", name: "Goa Trip", targetAmountMinor: 100000, currentSavedMinor: 45000, deadline: "2026-10-15", icon: "🏖️", linkedAccountId: "acc-1", status: "active", contributions: [{ id: "c-1", amountMinor: 20000, date: todayStr, note: "Deposit" }] },
        { id: "goal-2", name: "New Phone", targetAmountMinor: 80000, currentSavedMinor: 32000, deadline: "2026-11-30", icon: "📱", linkedAccountId: "acc-1", status: "active", contributions: [] }
      ];
      for (const g of defaultGoals) await this.put("goals", g);

      // Seed Recurring
      const defaultRecurring = [
        { id: "rec-1", name: "Netflix", amountMinor: 1599, type: "expense", categoryId: "cat-6", accountId: "acc-3", frequency: "Monthly", nextDueDate: todayStr, reminder: true, status: "active" },
        { id: "rec-2", name: "Apartment Rent", amountMinor: 90000, type: "expense", categoryId: "cat-8", accountId: "acc-1", frequency: "Monthly", nextDueDate: todayStr, reminder: true, status: "active" }
      ];
      for (const r of defaultRecurring) await this.put("recurring", r);

      // Seed SMS Queue & Rules
      const defaultSmsList = [
        { id: "sms-1", rawText: "Alert: Spend of INR 450.00 on Food at Starbucks card 1234", bank: "HDFC", amountMinor: 45000, merchant: "Starbucks", suggestedCategory: "Food & Drink", suggestedAccount: "acc-1", confidence: "High Confidence", timestamp: Date.now() },
        { id: "sms-2", rawText: "Txn: INR 280.00 debited for Uber ride on ICICI Card 5678", bank: "ICICI", amountMinor: 28000, merchant: "Uber", suggestedCategory: "Transport", suggestedAccount: "acc-3", confidence: "High Confidence", timestamp: Date.now() - 60000 },
        { id: "sms-3", rawText: "Alert: INR 620.00 spent at Swiggy on HDFC Card 1234", bank: "HDFC", amountMinor: 62000, merchant: "Swiggy", suggestedCategory: "Food & Drink", suggestedAccount: "acc-1", confidence: "High Confidence", timestamp: Date.now() - 120000 }
      ];
      for (const s of defaultSmsList) await this.put("smsQueue", s);

      await this.put("smsRules", { id: "rule-1", bank: "HDFC Bank", enabled: true, pattern: "debited by|spent on" });
      await this.put("smsRules", { id: "rule-2", bank: "SBI", enabled: true, pattern: "debited for|spent at" });

      // Seed Settings
      await this.put("settings", { key: "global", schemaVersion: 1, pinEnabled: false, pin: "1234", onboardingComplete: false });
    }
  }
}

export const dbEngine = new MSDatabase();
