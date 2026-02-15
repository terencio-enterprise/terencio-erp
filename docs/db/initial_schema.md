# TERENCIO POS – ENTERPRISE CORE

## Technical & Functional Documentation (v1)

---

# 1. SYSTEM OVERVIEW

Terencio POS is a multi-tenant, enterprise-grade Retail ERP & Fiscal POS system designed for:

* Multi-company SaaS environments
* Multi-store retail chains
* Offline-capable POS devices
* Fiscal compliance (VeriFactu ready)
* Inventory & warehouse control
* Purchasing & supplier management
* CRM (B2C & B2B)
* Accounting integration
* Auditability & traceability

Architecture style:

* Domain-driven modular design
* Ledger-based inventory
* Immutable fiscal chain
* Snapshot-based fiscal data
* Optimistic locking where required

---

# 2. DOMAIN MODULES

The system is divided into the following logical domains:

1. Organization (Multi-tenant layer)
2. Security & Access (RBAC + Devices)
3. Catalog & Pricing
4. CRM & Commercial Logic
5. Inventory & Purchasing
6. Sales & POS Operations
7. Fiscal Compliance (VeriFactu)
8. Accounting & Audit
9. Domain Events & BI

Each module is described below.

---

# 3. ORGANIZATION LAYER

## 3.1 companies

Represents legal entities.

Key responsibilities:

* Fiscal configuration
* Currency
* Tax inclusion rules
* Rounding mode

Relationships:

* 1 company → many stores
* 1 company → many products
* 1 company → many users

---

## 3.2 stores

Represents physical branches.

* Belongs to a company
* Has timezone
* Has unique internal code per company

---

## 3.3 store_settings

Per-store configuration.

Use cases:

* Allow negative stock
* Default tariff
* Require customer for high value sales

---

## 3.4 warehouses

Represents stock locations.

* Can belong to a store
* Used by inventory system

---

# 4. SECURITY & ACCESS

## 4.1 users

RBAC users.

Roles:

* CASHIER
* MANAGER
* ADMIN
* SUPER_ADMIN

Supports:

* Password auth (admin)
* PIN auth (POS)
* Fine-grained permissions JSON

---

## 4.2 devices

POS terminals.

Supports:

* Offline operation
* Device-based fiscal chain
* HMAC secret authentication
* Version tracking

---

## 4.3 registration_codes

Temporary onboarding codes for POS activation.

---

# 5. MASTER DATA (CATALOG & PRICING)

## 5.1 taxes

Tax definitions per company.

Used for:

* Sales
* Purchases
* Fiscal reporting

---

## 5.2 categories

Hierarchical product classification.

---

## 5.3 products

Core catalog entity.

Key features:

* Multi-tenant isolation
* Soft delete
* Cost tracking
* Version for optimistic locking

---

## 5.4 product_barcodes

Multi-barcode support.
Supports packs via quantity_factor.

---

## 5.5 tariffs

Price lists.
Supports:

* Retail
* Wholesale
* Premium

---

## 5.6 product_prices

Current price snapshot per tariff.

---

## 5.7 product_price_history

Full price audit with validity ranges.

Enterprise rule:
Only one active price per product + tariff.

---

## 5.8 product_cost_history

Tracks cost evolution.
Used for margin & accounting.

---

# 6. CRM & COMMERCIAL

## 6.1 customers

Supports:

* B2C
* B2B
* Credit limits
* Custom tariff

---

## 6.2 customer_product_prices

Overrides tariff price per customer.

---

## 6.3 pricing_rules

Dynamic rule engine.

Supports:

* Volume discounts
* BOGO
* Category promos

Execution priority-based.

---

## 6.4 customer_account_movements

Customer ledger.

Used for:

* Invoice creation
* Payment registration
* Balance tracking

---

## 6.5 suppliers

Supplier registry.

---

# 7. INVENTORY & PURCHASING

## 7.1 inventory_stock

Snapshot table.
Represents current quantities.

---

## 7.2 stock_movements

The source of truth ledger.

Types:

* SALE
* RETURN
* PURCHASE
* ADJUSTMENT
* TRANSFER_IN
* TRANSFER_OUT

Every stock change must generate a movement.

---

## 7.3 inventory_lots

Optional lot traceability.

---

## 7.4 stock_transfers

Warehouse transfers.
Protected when COMPLETED.

---

## 7.5 stock_counts

Physical inventory adjustment.

---

## 7.6 purchase_orders

Supplier order workflow.

States:

* DRAFT
* SENT
* PARTIAL
* RECEIVED
* CANCELLED

---

## 7.7 purchase_order_lines

Line-level purchase detail.

---

# 8. SALES & POS OPERATIONS

## 8.1 sales

Central transactional entity.

Features:

* Immutable once fiscalized
* Device sequence control
* Snapshot customer & store data
* Refund linkage
* Rectification support

---

## 8.2 sale_lines

Contains:

* Unit price (after pricing engine)
* Discounts
* Tax snapshot
* Pricing context JSON

---

## 8.3 sale_taxes

Aggregated tax breakdown per sale.

---

## 8.4 payments

Multiple payments per sale.
Supports multi-currency.

---

## 8.5 shifts

Cash session control per device.

---

## 8.6 cash_movements

Manual cash adjustments.

---

# 9. FISCAL COMPLIANCE

## fiscal_audit_log

Immutable fiscal chain per device.

Guarantees:

* Strict sequential chain
* No update/delete
* Hash validation
* AEAT status tracking

Triggers enforce:

* Sequence continuity
* Immutability

---

# 10. ACCOUNTING

## accounting_entries

Represents accounting journal entry.

---

## accounting_entry_lines

Debit / Credit lines.

Constraint:

* Either debit OR credit.

(Recommended future improvement: balance validation trigger)

---

## audit_user_actions

Tracks sensitive operations.

---

# 11. DOMAIN EVENTS

Asynchronous event log for:

* BI
* Integrations
* External systems

---

# 12. CORE USE CASES

## 12.1 Create Sale (POS Online)

1. Load product
2. Resolve pricing (tariff → overrides → rules)
3. Create sale (DRAFT)
4. Add lines
5. Add payments
6. Mark ISSUED
7. Insert fiscal_audit_log record
8. Generate stock_movements
9. Sync

---

## 12.2 Offline Sale

1. Device sequence increments locally
2. Sale stored offline
3. Sync batch pushes to server
4. Server validates uniqueness & fiscal chain

---

## 12.3 Refund / Credit Note

1. Create sale type CREDIT_NOTE
2. Link original_sale_uuid
3. Insert negative stock movements
4. Insert fiscal ANNUL record

---

## 12.4 Purchase Reception

1. Change PO to RECEIVED
2. Insert stock_movements (PURCHASE)
3. Update inventory_stock
4. Insert product_cost_history

---

## 12.5 Stock Transfer

1. Create transfer
2. COMPLETE
3. Generate TRANSFER_OUT + TRANSFER_IN movements

---

## 12.6 Price Change

1. Close previous product_price_history
2. Insert new price
3. Update product_prices snapshot

---

## 12.7 Accounting Entry

1. Create accounting_entries
2. Insert lines
3. (Recommended) Validate debit = credit

---

# 13. DATA INTEGRITY RULES

* Fiscal logs immutable
* Fiscal chain strictly sequential
* No modification of fiscalized sales
* Completed stock docs protected
* Unique active price per tariff
* Sale totals mathematically validated
* Sale lines mathematically validated

---

# 14. SCALABILITY STRATEGY

Designed for:

* 200+ stores
* Millions of sales
* Horizontal scaling at backend level
* Partition-ready structure

---

# 15. FUTURE ENTERPRISE IMPROVEMENTS

* Accounting balance trigger
* EXCLUDE date overlap constraint
* Table partitioning by year/company
* Advanced pricing engine service
* Event streaming (Kafka)

---

END OF DOCUMENTATION v1
