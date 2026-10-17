SIVANANTHA Android ERP Viewer v1.5.1 — Date Filters & Detailed Ledger PDFs

Upgrade from working v1.5 beta. Replace entire GitHub project, including app folder.

New:
- From and To date pickers on each of six business ledgers
- Clear filter action
- Brought-forward opening balance calculated from pre-period transactions
- Period inward/outward totals, closing balance and transaction count
- Multi-page detailed PDF with account, period, sync time, line items, running balances, totals
- Missing/invalid dates explicitly excluded with warning

Unchanged: login, auto-sync, Supabase RLS, offline cache, record PDFs, app icon.

LIMITATIONS: Date filter requires transaction dates beginning YYYY-MM-DD. No source records are modified.
Existing v1.5 beta ledger calculations are unchanged and remain subject to Windows ERP reconciliation.
Godown stock remains aggregate kilograms, not count/color-wise.

Not compiled on Android SDK here. Test against Windows ERP before using financial statements officially.
