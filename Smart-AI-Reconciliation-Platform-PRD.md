# Smart AI Reconciliation Platform — Product Requirements Document (PRD)

> **Version:** v1.3 | **Date:** 2026-07-07 | **Author:** AI Reconciliation Product Team
>
> **v1.3 Update:** Added predictive model vs LLM layered architecture design
> **v1.2 Update:** Added industry pain point data (10 global research studies)
>
> Based on AI/LLM technical capabilities, design a smart reconciliation platform suitable for most enterprises.

---

## Table of Contents

1. [Product Overview & Positioning](#1-product-overview--positioning)
2. [User Roles & Core Scenarios](#2-user-roles--core-scenarios)
3. [Functional Requirements](#3-functional-requirements)
4. [AI Capability Matrix](#4-ai-capability-matrix)
5. [Technical Architecture Requirements](#5-technical-architecture-requirements)
6. [Data Model Design](#6-data-model-design)
7. [Non-Functional Requirements](#7-non-functional-requirements)
8. [Implementation Roadmap](#8-implementation-roadmap)
9. [Appendix: Core Algorithm Descriptions](#9-appendix-core-algorithm-descriptions)

---

## 1. Product Overview & Positioning

### 1.1 Product Vision

Build a **"thinking reconciliation engine"** — one that not only automates reconciliation tasks but also understands data semantics, identifies anomaly patterns, explains discrepancy causes, and recommends resolution actions like a seasoned financial controller.

### 1.2 Product Positioning

| Dimension | Positioning |
|-----------|-------------|
| **Target Market** | Mid-to-large enterprises (annual revenue > 50M RMB), finance teams of 5+ people |
| **Core Value** | Improve reconciliation efficiency by 80%+, increase discrepancy discovery rate by 50%+, shorten month-end close cycle by 60%+ |
| **Technical Moat** | LLM-driven fuzzy matching + Multi-dimensional anomaly detection + Explainable discrepancy analysis |
| **Business Model** | SaaS subscription (billed by connected accounts/data volume) + On-premises deployment |

### 1.3 Differences from Traditional Reconciliation

| Traditional Reconciliation | Smart AI Reconciliation |
|----------------------------|------------------------|
| Rule-based exact matching (amounts must be equal) | LLM semantic understanding + fuzzy matching (tolerates decimals, exchange rates, time differences) |
| Manual line-by-line discrepancy investigation | AI auto-classifies discrepancies + provides root cause analysis |
| Fixed templates, development required for new scenarios | Natural language rule descriptions, AI auto-generates matching logic |
| Reactive, issues discovered after the fact | Proactive alerts, identifies trending anomalies |
| Binary "match/no-match" results only | Confidence scoring + multi-candidate recommendations + action suggestions |

### 1.4 Industry Pain Points: Why the Market Needs Smart Reconciliation

The following data is derived from research studies covering 300+ finance teams globally (SixthFin/Odoxa 2026, AutoRek 2024, BlackLine/FSN, Deloitte, LiveFlow 2025, Gartner 2023, and others):

#### Spreadsheet Dependency: Excel Remains the Dominant Tool

| Data Point | Source |
|------------|--------|
| **67%** of UK businesses with 250+ employees still use Excel for account reconciliation | SixthFin/Odoxa (2026) |
| **84%** of payment companies rely heavily on manual tasks and spreadsheets | AutoRek (2024) |
| **57%** of financial companies manage reconciliation via spreadsheets or hybrid approaches | AutoRek |
| **40%** of businesses process up to half of their financial data manually | bluQube (2025) |

#### Time Black Hole: Month-End Close Cycles Are Too Long

| Data Point | Source |
|------------|--------|
| **50%** of finance teams need 6+ business days to close the books | CFO.com / Ledge (2025) |
| Only **16%** of teams can complete close within 3 days | LiveFlow / CFO Dive (2025) |
| A 1,000-employee enterprise consumes ~**100,000 person-hours** annually on reconciliation, with direct labor costs of **$3-5M** | aurum.solutions |
| Finance teams spend **41%** of their time collecting and processing data rather than analyzing it | Deloitte |
| Average cash reconciliation takes **20-50 hours** per month | Multiple sources |

#### Alarming Error Rates: The Hidden Cost of Manual Processes

| Data Point | Source |
|------------|--------|
| **59%** of accountants make multiple financial errors per month; **18%** make errors daily | Gartner (2023) |
| Manual reconciliation error rates can reach **45%** in complex operations | NetSuite |
| Manual data entry has a **1.6%** error rate per transaction (160 errors per 10,000 entries) | Institute of Finance & Management |
| **~94%** of business spreadsheets contain errors | Multiple research studies |
| **1/3** of finance leaders do not trust the reliability of their own close data | SixthFin/Odoxa (2026) |
| **~70%** of finance leaders have made material decisions based on inaccurate financial data | BlackLine/Censuswide |

#### Data Silos: System Fragmentation

| Data Point | Source |
|------------|--------|
| **~80%** of finance professionals attribute month-end delays to waiting for data from other systems/departments | LiveFlow / CFO Dive (2025) |
| UK businesses manage an average of **8-25** disconnected systems | Accountancy Age |
| **61%** of organizations believe too many GL systems cause confusion | FSN/BlackLine |
| **83%** of organizations are held up by the last business unit's data submission | FSN/BlackLine |

#### Talent Drain: The Hidden Human Cost of Reconciliation Work

| Data Point | Source |
|------------|--------|
| **74%** of accountants experience work-related stress | CABA (2026) |
| **77%** of finance professionals have experienced burnout | Deloitte |
| Approximately **300,000** accountants have left the profession in the US over the past 2 years (17% workforce decline) | Multiple sources |
| **97%** of finance leaders acknowledge close negatively impacts workload; **93%** say it affects employee morale | SixthFin (2026) |
| The cost of replacing a skilled accountant is up to **30%** of their annual salary | US Department of Labor |

#### Compliance Risk & Fraud Exposure

| Data Point | Source |
|------------|--------|
| Organizations with material internal control deficiencies have **80-90%** higher fraud incidence, often correlated with manual reconciliation | ACFE |
| **80%** of payment companies expect compliance costs to continue rising | AutoRek |
| **~80%** of companies frequently miss reporting deadlines | AutoRek |
| **79%** of companies say their reconciliation processes cannot handle current data volumes | AutoRek |
| Macy's discovered **$151M** in fraudulent entries in December 2024 due to reconciliation gaps (real case) | SEC Filing |

#### Pain Points in One Sentence

> **Manual reconciliation wastes millions of dollars annually per enterprise, consumes 40%+ of finance team time, produces 45% error rates, and drove 300,000 US accountants to leave the profession — while mainstream reconciliation software remains almost entirely devoid of AI capabilities.**

---

## 2. User Roles & Core Scenarios

### 2.1 User Roles

| Role | Responsibilities | Core Concerns |
|------|-----------------|---------------|
| **Cashier/Accountant** | Day-to-day reconciliation executor | Efficiency, accuracy, reducing manual work |
| **Finance Manager** | Review, discrepancy resolution decisions | Discrepancy visualization, risk alerts, approval efficiency |
| **CFO/Finance Director** | Overall risk oversight | Reconciliation health dashboard, compliance, close progress |
| **IT/System Administrator** | System integration & maintenance | Data source configuration, rule configuration, access management |
| **Auditor (External)** | Audit verification | Audit trail, data completeness, traceability |

### 2.2 Core Business Scenarios

```
Scenario 1: Bank Reconciliation (Monthly/Daily)
  Enterprise ERP bank journal ↔ Bank statement
  Channels: Host-to-host bank API / Online banking download / SWIFT MT940

Scenario 2: Third-Party Payment Reconciliation (Daily)
  Business system transaction records ↔ WeChat Pay/Alipay/PayPal settlement reports
  Channels: Payment platform API / Settlement files

Scenario 3: AR/AP Reconciliation (Monthly/Quarterly)
  Our ledger data ↔ Customer/Supplier confirmation data
  Channels: Email/EDI/Supplier portal

Scenario 4: Cross-System Internal Reconciliation (Daily/Real-time)
  ERP General Ledger ↔ Business System (OMS/WMS/CRM) Sub-ledgers
  Channels: Direct database connection / API

Scenario 5: Month-End Multi-Dimensional Close Reconciliation (Monthly)
  GL ↔ Sub-ledger ↔ Auxiliary Ledger ↔ Business Ledger
  Channels: ERP data export / Data warehouse

Scenario 6: Intercompany Reconciliation (Monthly)
  Parent Company ↔ Subsidiary A ↔ Subsidiary B related-party transaction reconciliation
  Channels: Each entity's ERP system
```

### 2.3 User Journey

```
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Connect  │───▶│Configure │───▶│  Auto    │───▶│  Review  │───▶│  Resolve │
│Data Source│   │  Rules   │    │ Execute  │    │ Results  │    │Discrepancy│
└─────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
   1 hour          30 min           5 min          15 min          10 min
  (Trad: 2h)     (Trad: 2h)      (Trad: 4h)     (Trad: 4h)     (Trad: 2h)

                    Close Cycle: Traditional 14 hours → AI Reconciliation 2 hours
```

---

## 3. Functional Requirements

### 3.1 Functional Overview Map

```
Smart AI Reconciliation Platform
├── 1. Data Source Management
│   ├── 1.1 Multi-Source Connectors
│   ├── 1.2 Smart Field Mapping
│   ├── 1.3 Data Preview & Cleansing
│   └── 1.4 Scheduled Sync Policies
├── 2. Reconciliation Rule Engine
│   ├── 2.1 Pre-built Rule Templates
│   ├── 2.2 Visual Rule Orchestration
│   ├── 2.3 Natural Language Rule Generation (AI)
│   └── 2.4 Rule Recommendation & Optimization (AI)
├── 3. Smart Matching Engine
│   ├── 3.1 Exact Matching (Amount + Date + Ref No.)
│   ├── 3.2 Fuzzy Matching (AI Semantic Matching)
│   ├── 3.3 One-to-Many / Many-to-One Split Matching
│   └── 3.4 Match Confidence Scoring
├── 4. Discrepancy Management Center
│   ├── 4.1 Auto Discrepancy Classification (AI)
│   ├── 4.2 Root Cause Analysis (AI)
│   ├── 4.3 Discrepancy Resolution Suggestions (AI)
│   └── 4.4 Discrepancy Resolution Workflow
├── 5. Reconciliation Workbench
│   ├── 5.1 Task Scheduling
│   ├── 5.2 Real-Time Progress
│   ├── 5.3 Batch Operations
│   └── 5.4 Manual Adjustments & Write-offs
├── 6. Smart Analytics
│   ├── 6.1 Reconciliation Health Dashboard
│   ├── 6.2 Anomaly Trend Alerts (AI)
│   ├── 6.3 Natural Language Query (AI)
│   └── 6.4 Auto Report Generation (AI)
├── 7. Approval & Workflow
│   ├── 7.1 Configurable Approval Flows
│   ├── 7.2 Task Assignment & Reminders
│   ├── 7.3 Approval Comment Records
│   └── 7.4 SLA Monitoring
├── 8. System Administration
│   ├── 8.1 Multi-Org / Multi-Ledger
│   ├── 8.2 Role-Based Access Control (RBAC)
│   ├── 8.3 Audit Log (Immutable)
│   └── 8.4 Internationalization (Multi-Currency / Multi-Language / Multi-Timezone)
└── 9. Open Platform
    ├── 9.1 RESTful API
    ├── 9.2 Webhook Event Push
    ├── 9.3 Custom Plugin Mechanism
    └── 9.4 Standard Data Exchange Formats
```

### 3.2 Detailed Functional Specifications

---

#### Module 1: Data Source Management

##### 1.1 Multi-Source Connectors

**Supported Data Source Types:**

| Category | Specific Sources | Connection Method |
|----------|-----------------|-------------------|
| **Banks** | ICBC, CCB, ABC, BOC, CMB and 50+ domestic Chinese banks | Host-to-host API / SFTP files / SWIFT MT940/MT942 |
| **International Banks** | HSBC, Citi, JPMorgan, Deutsche Bank, DBS, etc. | SWIFT / ISO 20022 / API |
| **Third-Party Payments** | WeChat Pay, Alipay, PayPal, Stripe, Square, Adyen | REST API / Settlement files (CSV/Excel) |
| **ERP Systems** | SAP S/4HANA, Oracle EBS/Netsuite, Yonyou NCC/U8/T+, Kingdee Cloud Starlink/Easylink, Microsoft Dynamics 365 | Direct DB connection / API / Excel export |
| **Business Systems** | OMS, WMS, POS, E-commerce platforms (Taobao/JD/Amazon/Shopify) | API / Staging tables |
| **File Import** | Excel, CSV, XML, JSON, OFX/QFX, PDF (scanned bank statements) | Upload / SFTP / Email attachment auto-collection |

**Key Features:**
- Hot-plug connector architecture: new data sources without main system upgrades
- Connection testing & health monitoring: automatic alerts on connection failure
- Encrypted credential storage (AES-256)
- Configurable incremental/full sync strategies

##### 1.2 Smart Field Mapping (AI-Powered)

This is one of the product's core differentiators.

**Traditional Pain Point:** Field naming varies wildly across sources — "Transaction Amount" vs "TXN_AMT" vs "Amount (RMB)" — manual mapping is time-consuming and error-prone.

**AI Solution:**

```
Input: Source field list + Target field list

┌─────────────────────────────────────────────────┐
│ AI Field Semantic Mapping Engine                │
│                                                 │
│ Source field: "TXN_AMT"                         │
│   ↓ Semantic Understanding (LLM)                │
│   → "This is a transaction amount field"         │
│   → Field type inference: Decimal(18,2)          │
│   → Suggested mapping: "Transaction Amount"     │
│     (Confidence: 98%)                           │
│   → Data format recognition: "1000.00"          │
│     → Has decimal point, 2 decimal places       │
│                                                 │
│ Source field: "VALUE_DATE"                      │
│   ↓ Semantic Understanding (LLM)                │
│   → "This is a value date/posting date field"    │
│   → Suggested mapping: "Value Date"             │
│     (Confidence: 95%)                           │
│   → Date format recognition: "20240115"         │
│     → yyyyMMdd                                  │
└─────────────────────────────────────────────────┘
```

**Key Features:**
- LLM semantic analysis for automatic field meaning identification
- Support for 20+ languages in field name mapping
- Automatic field format recognition & conversion rule recommendations
- Human confirmation/correction interface (one-click accept all recommendations)
- Mapping scheme saved as templates for reuse with similar data sources

##### 1.3 Data Preview & Cleansing

- Instant preview upon data source connection (first 100 rows)
- Automatic data quality checks:
  - Required field missing rate
  - Amount field format anomalies
  - Date range anomalies
  - Duplicate row detection
  - Amount total vs source system verification
- One-click data cleansing:
  - Remove blank rows / special characters
  - Amount formatting ("1,000.00" → 1000.00)
  - Date standardization
  - Negative sign / debit-credit direction unification

##### 1.4 Scheduled Sync Policies

- Cron expression scheduling
- Event-driven sync (new file received / Webhook trigger)
- Auto-retry on sync failure + alerts
- Sync history & latency monitoring

---

#### Module 2: Reconciliation Rule Engine

##### 2.1 Pre-Built Rule Templates

| Rule Template | Applicable Scenario | Matching Logic |
|---------------|-------------------|----------------|
| Bank Recon - Standard | Bank transactions vs ERP journal | Amount + Date (±1 day) + Description fuzzy match |
| Bank Recon - Strict | High-risk accounts | Amount + Date + Reference No. exact match |
| Third-Party Payment Recon | Payment transactions vs Settlement report | Amount + Fee breakdown + Order No. match |
| AR Recon | Our AR vs Customer confirmation | Invoice No. + Amount + Customer Name |
| AP Recon | Supplier invoice vs Our Purchase | PO No. + Invoice No. + Amount |
| Cross-System Check | GL vs Sub-ledger | Account + Period + Amount |
| Intercompany | Related-party transactions | Transaction ID + Amount + Direction |

##### 2.2 Visual Rule Orchestration

- Drag-and-drop rule builder (similar to decision table editor)
- Match condition combinations: AND/OR/NOT logic
- Supported condition types:
  - Exact match / Range match / Contains match / Regex match
  - Amount tolerance (absolute value / percentage)
  - Date offset (±N days / business days)
  - Amount aggregation match (one-to-many)
- Rule priority ordering
- Rule test sandbox: validate rule effectiveness using historical data

##### 2.3 Natural Language Rule Generation (AI-Powered)

**Traditional Pain Point:** Finance personnel cannot directly configure rules and must rely on IT translation.

**AI Solution:**

```
User Input (Natural Language):
"Auto-match records where the amount difference doesn't exceed 5 RMB,
dates are within 2 business days before or after, and summaries contain
the same supplier name. If there's a fee difference, tag it separately
as 'Fee Difference'."

  ↓ LLM Parsing + Rule Generation

Generated Rule (Structured):
{
  "name": "Supplier Payment Recon - with Fee Tolerance",
  "conditions": [
    { "field": "amount_diff_abs", "operator": "lte", "value": 5 },
    { "field": "date_diff_business_days", "operator": "lte", "value": 2 },
    { "field": "supplier_name_similarity", "operator": "gte", "value": 0.8 }
  ],
  "exceptions": [
    { "condition": "fee_identified", "tag": "Fee Difference" }
  ]
}
```

**Key Features:**
- Natural language input → Structured rule output
- LLM translation process visualization (user can review each step)
- Rule conflict detection (logical conflicts between new and existing rules)
- Rule effectiveness simulation (AI predicts match rate and false-positive rate of new rules)

##### 2.4 Rule Recommendation & Optimization (AI-Powered)

- Based on historical reconciliation results, AI recommends:
  - "Relaxing date tolerance from 1 to 2 days would auto-match 87% of these unmatched records"
  - "Records with brand names in descriptions — suggest adding supplier name matching rule"
- Continuous rule performance monitoring → suggest optimization/deprecation of low-efficiency rules

---

#### Module 3: Smart Matching Engine

##### 3.1 Layered Matching Architecture

```
                    Layer 1: Exact Matching
        Amount = Amount AND RefNo = RefNo AND Date = Date
                    Match Rate Target: 60-70%
                         ↓ Unmatched
                    Layer 2: Rule Matching
        Amount Tolerance ≤ Threshold AND Date Offset ≤ N Days
                    AND Description Similarity ≥ 80%
                    Match Rate Target: 15-25%
                         ↓ Unmatched
                    Layer 3: AI Semantic Matching
        LLM Transaction Semantic Understanding + Vector Similarity
                    + Contextual Reasoning
                    Match Rate Target: 5-10%
                         ↓ Unmatched
                    Layer 4: Discrepancy Management
                Auto-Classification + Root Cause Analysis
                    + Resolution Suggestions
```

##### 3.2 AI Semantic Matching in Detail

This is the product's core technical moat.

**Reasons Traditional Matching Fails:**
- Completely different description text for the same transaction: ERP records "Purchase Payment - Company A 2024Q1" while bank shows "Interbank Transfer A Technology Co., Ltd. CN20240115-005"
- One-to-many split scenarios: ERP single payment of 1M RMB, bank processes 3 separate debits (500K+300K+200K)
- Fee splitting: Transaction amount = Principal + Fee, amounts differ on both sides
- Exchange rate differences: Foreign currency transaction booked at different rate than settlement rate
- Timing differences: ERP books by transaction date, bank settles by clearing date (T+1/T+2)

**AI Semantic Matching Flow:**

```
┌─────────────────────────────────────────────────────┐
│              AI Semantic Matching Engine             │
│                                                     │
│  Input: Unmatched Record A (ERP) + Candidate        │
│         Records B1..Bn (Bank)                       │
│                                                     │
│  Step 1: Semantic Vectorization                     │
│    Vectorize transaction description, counterparty  │
│    name, purpose, etc. using Embedding model →      │
│    calculate cosine_similarity for semantic distance │
│                                                     │
│  Step 2: Multi-Dimensional Scoring (LLM)            │
│    ┌───────────────────────────────────────┐        │
│    │ Dimension         │ Wt   │ A↔B1 │ A↔B2 │        │
│    ├────────────────────┼──────┼──────┼──────┤        │
│    │ Amount Match       │ 30%  │ 0.95 │ 0.70 │        │
│    │ Date Match         │ 20%  │ 0.90 │ 0.90 │        │
│    │ Counterparty Sim.  │ 25%  │ 0.88 │ 0.40 │        │
│    │ Purpose Sem. Sim.  │ 15%  │ 0.92 │ 0.30 │        │
│    │ Ref No. Fuzzy Match│ 10%  │ 0.50 │ 0.60 │        │
│    │ Weighted Total     │      │ 0.89 │ 0.60 │        │
│    └───────────────────────────────────────┘        │
│                                                     │
│  Step 3: Best Match Decision                        │
│    Score ≥ 0.85 → Auto-match                        │
│    Score 0.70-0.85 → Recommended match (human confirm)│
│    Score < 0.70 → Flag as discrepancy                │
│                                                     │
│  Step 4: Match Explanation (Explainable AI)          │
│    "A matched with B1 score 0.89: amounts equal     │
│     (0.95), dates 1 day apart (0.90), counterparty  │
│     names highly semantically related (0.88)..."     │
└─────────────────────────────────────────────────────┘
```

##### 3.3 One-to-Many / Many-to-One Split Matching

**Scenarios:**
- ERP payment of 10,000 RMB → Bank processes as 6,000 + 4,000 across two debits (across days)
- E-commerce daily aggregated receipt of 50,000 RMB → Corresponding 300 order line items

**Algorithm:**
- Subset Sum optimization: search for amount combinations among candidate records
- AI-assisted: LLM analyzes split reasons (fees involved, cross-border tranches) to narrow search space
- Performance: within 100 candidate records, complete combinatorial search in under 30 seconds

##### 3.4 Match Confidence Scoring

- Each matched record pair carries a confidence score (0-100%)
- Match quality dashboard: distribution display by confidence interval
- Low-confidence matches automatically routed for human review
- Data sources with consistently low confidence trigger rule optimization suggestions

---

#### Module 4: Discrepancy Management Center

##### 4.1 Auto Discrepancy Classification (AI-Powered)

AI automatically classifies discrepancies into the following categories without manual line-by-line review:

| Discrepancy Category | Typical Manifestation | AI Identification Basis |
|---------------------|----------------------|------------------------|
| **Timing Difference** | Same amount but dates cross months | Exact amount match + Date difference + Month crossing |
| **Fee Difference** | Amount differs by a few/dozens of RMB | Difference within common fee range (0.1%-1%) |
| **FX Rate Difference** | Foreign currency transaction amounts differ | Currency field + Difference ratio near FX fluctuation range |
| **Manual Entry Error** | Amount digits transposed/shifted | Digit similarity analysis (e.g., "1234" vs "1243") |
| **Outstanding Items** | One side has record, other side doesn't | Unilateral presence + Near period end |
| **Duplicate Entry** | Same amount, same date appears twice | Exact duplicate detection + High probability of duplication |
| **Counterparty Unposted** | Only our side has a record | Unilateral + Non-period-end timing |
| **Unknown Discrepancy** | Cannot auto-classify | Submit for human judgment and learning |

##### 4.2 Root Cause Analysis (AI-Powered)

**AI not only tells you "there's a discrepancy" — it tells you "why":**

```
Discrepancy ID: #D20240715-0032
Amount Difference: ¥1,200.00

AI Root Cause Analysis Results:
┌─────────────────────────────────────────────┐
│ 🔍 Root Cause: FX Settlement Difference      │
│                                             │
│ 📋 Analysis Process:                        │
│ 1. This transaction is USD-settled          │
│    (ERP recorded rate 7.1000)               │
│ 2. Bank actual settlement rate 7.0824       │
│ 3. Conversion difference:                   │
│    $10,000 × (7.1000-7.0824) = ¥176.00     │
│    (FX difference)                          │
│ 4. Additional ¥24.00 cross-border fee       │
│ 5. Remaining ¥1,000.00 unmatched →          │
│    possibly counterparty bank fee            │
│                                             │
│ 💡 Suggestions:                              │
│ • Book ¥176.00 to "FX Gain/Loss" account    │
│ • Book ¥24.00 to "Bank Fee" account         │
│ • Confirm ¥1,000.00 with counterparty bank   │
│                                             │
│ ⚠️ Risk Level: 🟡 Medium                     │
│ (Involves foreign currency,                  │
│  requires finance manager review)            │
└─────────────────────────────────────────────┘
```

##### 4.3 Discrepancy Resolution Suggestions (AI-Powered)

- AI provides resolution actions based on discrepancy category + historical resolution records
- One-click adjustment entry generation for common discrepancies
- Adjustments previewable → confirm → auto-write to ERP
- AI learns user resolution patterns, suggestions become increasingly accurate

##### 4.4 Discrepancy Resolution Workflow

- Discrepancy assignment: designate handler / auto-assign by rules
- Resolution status: Pending → In Progress → Awaiting Review → Completed → Closed
- Resolution deadline: SLA configuration + auto-escalation on timeout
- Batch processing: one-click batch operations for same-category discrepancies
- Complete resolution audit trail (operator, timestamp, resolution method, adjustment entries)

---

#### Module 5: Reconciliation Workbench

##### 5.1 Task Scheduling

- Task types: Daily (bank/payment), Weekly (inventory), Monthly (GL/intercompany)
- Dependencies: Sub-ledger recon → GL recon (subsequent tasks auto-wait if prerequisites incomplete)
- Reconciliation calendar view: display all monthly reconciliation tasks and statuses
- Month-end close countdown + completion progress bar

##### 5.2 Real-Time Progress

- Real-time progress display for large reconciliation tasks (millions of records)
- Real-time match rate statistics
- Throughput monitoring (records/second)
- Estimated completion time

##### 5.3 Batch Operations

- Batch confirm matches (one-click confirm all matches with ≥95% confidence)
- Batch tag discrepancy categories
- Batch export discrepancy list (Excel/PDF)
- Batch generate adjustment vouchers

##### 5.4 Manual Adjustments & Write-offs

- Manually create match relationships (drag-and-drop)
- Manual discrepancy adjustment entry
- Write-off voucher generation & linking
- Adjustment records automatically incorporated into AI training data

---

#### Module 6: Smart Analytics

##### 6.1 Reconciliation Health Dashboard

```
┌────────────────────────────────────────────────┐
│  📊 Reconciliation Health Dashboard   Jul 2024 │
│                                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │Auto-Match│ │Resolution│ │ Avg Close│       │
│  │  92.5%   │ │  97.3%   │ │  1.8 Days│       │
│  │  ↑ 3.2%  │ │  ↑ 1.1%  │ │  ↓ 0.5d │       │
│  └──────────┘ └──────────┘ └──────────┘       │
│                                                │
│  Discrepancy Trend (12 months) ▁▂▃▂▁▅▃▂▁▂▁▂   │
│  Discrepancy Category Pie Chart                │
│  Per-Account/Entity Health Radar Chart         │
│  Top 10 Discrepancy Amount Ranking              │
│  Month-End Close Progress Gantt Chart           │
└────────────────────────────────────────────────┘
```

##### 6.2 Anomaly Trend Alerts (AI-Powered)

- Time-series analysis based anomaly pattern detection:
  - Sudden increase in account discrepancy rate (MoM/YoY)
  - Repeated discrepancies in a business line
  - Persistent deterioration of supplier reconciliation timeliness
  - Month-end discrepancy spike alerts
- Alert channels: In-system notifications / Email / WeCom/DingTalk/Slack

##### 6.3 Natural Language Query (AI-Powered)

Users can converse with data using natural language:

```
User: "What caused the 5 largest CCB reconciliation
      discrepancies last month?"

AI: "There were 23 discrepancies in June 2024 CCB
    reconciliation. The 5 largest by amount:
    1. ¥128,500 - Supplier 'Huadong Tech' payment delay
       (Bank side 6/3, ERP side 6/5)
    2. ¥86,200 - Cross-border FX rate difference
       (Contract rate vs Settlement rate)
    3. ¥52,000 - Fee allocation difference
    4. ¥31,500 - Counterparty account info change
       causing refund and re-payment
    5. ¥18,300 - Outstanding item (settled 7/2,
       timing difference)
    Would you like me to analyze any of these in detail?"

User: "Compare Q1 and Q2 reconciliation efficiency this year"

AI: "Q1 vs Q2 Reconciliation Efficiency Comparison:
    • Auto-match rate: 89.3% → 92.5% (↑3.2pp)
    • Avg close days: 2.3 → 1.8 days (↓0.5 days)
    • Avg discrepancy resolution: 1.5 → 0.8 days (↓0.7 days)
    • Manual intervention ratio: 18% → 11% (↓7pp)
    Bank reconciliation improved most, reaching 95%+ auto-match.
    Note: Alipay reconciliation auto-match is low (78%),
    recommend optimizing match rules."
```

##### 6.4 Auto Report Generation (AI-Powered)

- Periodic reports:
  - Daily reconciliation brief (auto-pushed)
  - Monthly reconciliation report (with discrepancy details and trend analysis)
  - Quarterly reconciliation analysis report (with efficiency statistics and improvement suggestions)
- Report content AI auto-authored (text analysis + data charts)
- Customizable report templates (corporate branding/format)
- Multi-format export: PDF, Excel, online link

---

#### Module 7: Approval & Workflow

##### 7.1 Configurable Approval Flows

- Visual flow designer (BPMN-style)
- Approval node types: Countersign, Or-Sign, Conditional Branch, Auto-Approve
- Approval conditions: dynamic routing by amount threshold, discrepancy type, account type
- Auto-escalation/reassignment on approval timeout

##### 7.2 Task Assignment & Reminders

- Auto-assignment rules (by account/entity/business line)
- To-do list (supports filtering/sorting/batch operations)
- Multi-channel reminders: System notification + Email + IM (WeCom/DingTalk/Feishu)
- Reminder strategy: Daily digest / Real-time notification / Pre-deadline nudges

##### 7.3 Approval Comment Records

- Structured approval comments (Approve/Reject/Return for Modification + Notes)
- Adjustment entry attachments
- Approval history timeline

##### 7.4 SLA Monitoring

- Three-tier monitoring: Recon Completion SLA, Discrepancy Resolution SLA, Approval SLA
- Timeout alerts (Yellow/Orange/Red three levels)
- SLA achievement rate statistical reports

---

#### Module 8: System Administration

##### 8.1 Multi-Org / Multi-Ledger

- Three-tier org structure: Group - Subsidiary - Department
- Multi-ledger management (different accounting standards/currencies)
- Inter-org data isolation + Group-level consolidated view

##### 8.2 Role-Based Access Control (RBAC)

- Pre-built roles: Super Admin, CFO, Finance Manager, Accountant, Auditor (Read-only)
- Custom roles: Function-level + Data-level permissions
- Sensitive operation double confirmation

##### 8.3 Audit Log

- All operation records immutable (blockchain notarization optional)
- Log scope: Login, Data modification, Reconciliation operations, Approval operations, Configuration changes
- Audit query: Search by time/operator/operation type/target
- Audit report export (SOX/CSA compliance ready)

##### 8.4 Internationalization

- Multi-currency: 160+ currencies, automatic FX rate conversion (real-time rate feed integration)
- Multi-language: Chinese / English / Japanese / Traditional Chinese (UI + AI analysis results)
- Multi-timezone: Unified timezone display for global branches
- Multi-GAAP: CAS/IFRS/GAAP difference handling

---

#### Module 9: Open Platform

##### 9.1 RESTful API

- Comprehensive REST API (benchmarked against BlackLine/Stripe API design standards)
- API versioning (backward compatible)
- API Key + OAuth 2.0 authentication
- Rate limiting + usage analytics

##### 9.2 Webhook Event Push

- Event types: Reconciliation complete, Discrepancy found, Approval complete, SLA timeout
- Retry mechanism + delivery status tracking
- Support push to third-party systems (ERP/CRM/OA)

##### 9.3 Custom Plugin Mechanism

- Plugin types: Data source connectors / Match algorithms / Discrepancy handlers / Notification channels
- Plugin marketplace (similar to VS Code Extensions)
- SDK & developer documentation

##### 9.4 Standard Data Exchange Formats

- Support Accounting Data Standard (ADS)
- Support ISO 20022 financial messaging standard
- Support XBRL GL (Global Ledger Taxonomy)

---

## 4. AI Capability Matrix

### 4.0 Architecture Principle: Layered Design — Predictive Models vs LLMs

Before diving into the AI capability matrix, a key architectural decision must be clarified. Based on deep analysis of industry best practices (predictive models + LLM separation, fusion architectures, and traditional rule/ML approaches), **the industry best practice has converged on the following paradigm:**

```
┌─────────────────────────────────────────────────────┐
│              AI Capability Layered Architecture       │
│                                                     │
│  Layer 3: LLM Layer (Communication & Reasoning)     │
│  ┌─────────────────────────────────────────────┐    │
│  │ • Natural language query / rule generation   │    │
│  │ • Semantic matching (multi-dimension scoring │    │
│  │   + explanation)                             │    │
│  │ • Root cause analysis (Chain-of-Thought)     │    │
│  │ • Report generation / suggestion explanation │    │
│  │ ⚠️ LLM accuracy ceiling ~70% on financial    │    │
│  │    classification                            │    │
│  │    → Use ONLY for understanding, explaining, │    │
│  │      reasoning — NOT for computation         │    │
│  └─────────────────────────────────────────────┘    │
│                        ↑                            │
│  Layer 2: Predictive Model Layer                    │
│          (Computation & Classification)             │
│  ┌─────────────────────────────────────────────┐    │
│  │ • Amount matching (numerical comparison)      │    │
│  │ • Discrepancy classification (fine-tuned     │    │
│  │   models, mid-90% accuracy)                  │    │
│  │ • Anomaly detection (Isolation Forest /      │    │
│  │   Prophet)                                   │    │
│  │ • Confidence scoring (rules + statistics)    │    │
│  │ ✅ Math → must not hallucinate → predictive  │    │
│  │    models / statistical algorithms           │    │
│  └─────────────────────────────────────────────┘    │
│                        ↑                            │
│  Layer 1: Rule Engine Layer (Deterministic Match)   │
│  ┌─────────────────────────────────────────────┐    │
│  │ • Exact match (Amount + Date + Reference No.)│    │
│  │ • Rule match (Amount tolerance + Date offset │    │
│  │   + Regex)                                   │    │
│  │ • Split combination search (Subset Sum)      │    │
│  │ ✅ Deterministic logic → zero hallucination  │    │
│  │    → traditional code implementation         │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

> **Core Principle:** Math and computation belong to predictive models / rule engines (must not hallucinate). Semantic understanding and reasoning belong to LLMs (must not compute). This architecture has been validated in real-world deployments — predictive models have achieved 54% higher accuracy than GPT-4o on financial classification tasks.

### 4.1 AI Capability Overview

| AI Capability | Technical Approach | Application Module | Maturity | Differentiation Level |
|---------------|-------------------|--------------------|----------|----------------------|
| **Smart Field Mapping** | LLM Semantic Understanding + Embedding | Data Source Management | ⭐⭐⭐ | 🟢 High |
| **NL Rule Generation** | LLM + Constrained Decoding | Rule Engine | ⭐⭐⭐ | 🟢 Very High |
| **LLM Semantic Matching** | Embedding + LLM Multi-Dimension Scoring | Match Engine | ⭐⭐⭐⭐ | 🟢 Very High |
| **Split Combination Match** | Subset Sum Algorithm + LLM Assistance | Match Engine | ⭐⭐⭐ | 🟡 Medium-High |
| **Auto Discrepancy Classification** | Fine-tuned Small Models + Few-shot LLM | Discrepancy Management | ⭐⭐⭐⭐ | 🟢 High |
| **Root Cause Analysis (CoT)** | LLM Reasoning Chain | Discrepancy Management | ⭐⭐⭐ | 🟢 Very High |
| **Resolution Suggestion** | LLM + Historical Pattern Learning | Discrepancy Management | ⭐⭐⭐ | 🟢 High |
| **Anomaly Trend Detection** | Time-series Models + LLM Explanation | Smart Analytics | ⭐⭐⭐ | 🟡 Medium |
| **Natural Language Query** | LLM + Text-to-SQL + RAG | Smart Analytics | ⭐⭐⭐⭐ | 🟢 High |
| **Auto Report Generation** | LLM Text Generation + Templates | Smart Analytics | ⭐⭐⭐⭐ | 🟡 Medium |
| **Rule Optimization Suggestions** | Historical Data Analysis + LLM | Rule Engine | ⭐⭐ | 🟢 High |
| **Continuous Learning** | Human Feedback → Model Fine-tuning | Global | ⭐⭐ | 🟡 Medium-High |

### 4.2 AI Model Selection Recommendations

| Scenario | Recommended Model | Rationale |
|----------|------------------|-----------|
| **Field Semantic Mapping** | Claude Sonnet / GPT-4o mini | High-precision text understanding, manageable cost |
| **Semantic Match Scoring** | Local Embedding (BGE-M3) + Claude Sonnet | Embedding for coarse ranking, LLM for fine ranking |
| **NL Rule Parsing** | Claude Sonnet / GPT-4o | Requires strict structured output |
| **Root Cause Analysis (CoT)** | Claude Opus / GPT-4o | Requires deep reasoning chains |
| **Natural Language Query** | Claude Sonnet + Text-to-SQL | Balance accuracy and latency |
| **Discrepancy Classification** | Fine-tuned small models (Qwen/DeepSeek) | High-frequency calls, need low cost & low latency |
| **Anomaly Detection** | Statistical models (Prophet/Isolation Forest) | Numerical analysis outperforms LLMs |
| **Report Generation** | Claude Haiku / GPT-4o mini | Structured text generation |

### 4.3 AI Reliability & Safety

- **Explainability:** All AI decisions carry readable explanation text
- **Human-in-the-Loop:** AI makes recommendations, humans make decisions
- **Confidence Thresholds:** Auto-route to human below threshold
- **Hallucination Prevention:** Amount-related outputs double-verified; structured constrained outputs
- **Data Privacy:** Support localized LLM deployment, sensitive data never leaves corporate network
- **Audit Traceability:** Complete logging of all AI recommendations/decisions

---

## 5. Technical Architecture Requirements

### 5.1 Overall Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend Layer                        │
│  React/Next.js • Responsive Web • Mobile Adapted         │
│  Dashboards • Workbench • Rule Editor • Approval Center  │
└───────────────────────┬─────────────────────────────────┘
                        │ REST API / WebSocket
┌───────────────────────┴─────────────────────────────────┐
│                    API Gateway Layer                     │
│  Kong/APISIX • Auth • Rate Limiting • Routing • Logging  │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────┴─────────────────────────────────┐
│                  Business Services Layer                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │Data Src  │ │Recon     │ │Disc Mgmt │ │Workflow  │  │
│  │Service   │ │Engine    │ │Service   │ │Engine    │  │
│  │• Conn Mgmt│ │• Rule Exec│ │• Classify│ │• Flow Def│  │
│  │• Field Map│ │• Scoring │ │• Root Cause│ • Task Sched│  │
│  │• Cleansing│ │• Split   │ │• Suggest │ │• Approval│  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │Smart     │ │Open      │ │System    │ │Notif     │  │
│  │Analytics │ │Platform  │ │Admin     │ │Service   │  │
│  │• NL Query│ │• REST API│ │• Multi-Tenant│ • Email │  │
│  │• Alerts  │ │• Webhook │ │• RBAC    │ │• IM      │  │
│  │• Reports │ │• Plugins │ │• Audit   │ │• SMS     │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────┴─────────────────────────────────┐
│                     AI Service Layer                     │
│  ┌──────────────────────────────────────────────────┐  │
│  │          AI Orchestration Engine (LangGraph)       │  │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌──────────┐  │  │
│  │  │  LLM   │ │Embedding│ │ Time-  │ │  Rule    │  │  │
│  │  │Reasoning│ │Vectorize│ │ Series │ │  Engine  │  │  │
│  │  └────────┘ └────────┘ └────────┘ └──────────┘  │  │
│  └──────────────────────────────────────────────────┘  │
│  • Model Routing • Prompt Mgmt • Output Validation       │
│  • Cache Optimization                                   │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────┴─────────────────────────────────┐
│                      Data Layer                          │
│  PostgreSQL (Business) • Redis (Cache) • ES (Search)     │
│  Milvus/Qdrant (Vector DB) • MinIO (Files)               │
│  ClickHouse (Analytics) • Kafka (Message Queue)          │
│  • Blockchain Notarization (Optional)                    │
└─────────────────────────────────────────────────────────┘
```

### 5.2 Key Technical Decisions

| Decision Point | Selection | Rationale |
|---------------|-----------|-----------|
| **Backend Language** | Java (Spring Boot) + Python (AI Services) | Java for business core (enterprise-grade maturity), Python for AI (best ecosystem) |
| **Reconciliation Engine** | Custom built (Java streaming + Python AI) | No off-the-shelf engine meets requirements |
| **Workflow Engine** | Camunda / Temporal | Proven open-source workflow engines at scale |
| **AI Orchestration** | LangGraph / Dify | Flexible multi-step AI workflow orchestration |
| **Vector Database** | Milvus (production) / Qdrant (lightweight deployment) | Core infrastructure for semantic matching |
| **Message Queue** | Kafka | High-throughput data sync + event-driven architecture |
| **Deployment** | Kubernetes + Helm | Supports public cloud/private cloud/hybrid deployment |

### 5.3 Performance Metrics

| Metric | Target | Description |
|--------|--------|-------------|
| **Reconciliation Throughput** | ≥ 1M records/min | Single instance processing capacity |
| **AI Match Latency** | ≤ 500ms/record | Layer 3 semantic match per-record latency |
| **API Response Time** | P95 ≤ 200ms | Non-AI APIs |
| **Natural Language Query** | ≤ 5 seconds | End-to-end query response |
| **System Availability** | ≥ 99.9% (SaaS) | Annual downtime ≤ 8.76 hours |
| **Data Consistency** | Eventual consistency ≤ 5 sec | Cross-system data sync latency |

---

## 6. Data Model Design

### 6.1 Core Entity Relationships

```
┌──────────┐    1:N    ┌──────────┐    1:N    ┌──────────┐
│Organization│◄────────│  Ledger   │◄────────│  Account  │
│ (Company)  │         │ (Ledger)  │         │ (Account) │
└──────────┘           └──────────┘          └──────────┘
                                                   │
                                                   │ 1:N
                        ┌──────────────────────────┤
                        │                          │
                        ▼                          ▼
                  ┌──────────┐              ┌──────────┐
                  │DataSource│              │ReconTask │
                  │(Data Src)│              │(ReconTask)│
                  └────┬─────┘              └────┬─────┘
                       │                         │
                       │ 1:N                     │ 1:N
                       ▼                         ▼
                  ┌──────────┐              ┌──────────┐
                  │RawRecord │              │ReconMatch│
                  │(Raw Rec) │              │(Match)   │
                  └──────────┘              └────┬─────┘
                                                 │
                        ┌─────────────┬──────────┤
                        │             │          │
                        ▼             ▼          ▼
                  ┌──────────┐ ┌──────────┐ ┌──────────┐
                  │ Matched  │ │Unmatched │ │Adjustment│
                  │(Matched) │ │(Disc)    │ │(Adj)     │
                  └──────────┘ └────┬─────┘ └──────────┘
                                    │
                                    ▼
                              ┌──────────┐
                              │ Approval │
                              │(Approval)│
                              └──────────┘
```

### 6.2 Core Table Schema Overview

```sql
-- Reconciliation Task
CREATE TABLE recon_task (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    name VARCHAR(200),
    source_a_id UUID NOT NULL,       -- Data source A
    source_b_id UUID NOT NULL,       -- Data source B
    rule_config JSONB,               -- Rule configuration
    period VARCHAR(20),              -- Recon period (2024-07)
    status VARCHAR(20),              -- pending/running/completed/error
    match_summary JSONB,             -- Match summary statistics
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Match Result
CREATE TABLE recon_match (
    id UUID PRIMARY KEY,
    task_id UUID REFERENCES recon_task(id),
    record_a_id UUID,                -- Record from data source A
    record_b_id UUID,                -- Record from data source B
    match_type VARCHAR(30),          -- exact/rule/ai_semantic/ai_split/manual
    confidence DECIMAL(5,2),         -- Confidence 0.00-100.00
    match_dimensions JSONB,          -- Detailed scoring by dimension
    ai_explanation TEXT,             -- AI match explanation
    status VARCHAR(20),              -- auto_confirmed/pending_review/manual_confirmed
    created_at TIMESTAMP DEFAULT NOW()
);

-- Discrepancy Record
CREATE TABLE recon_discrepancy (
    id UUID PRIMARY KEY,
    task_id UUID REFERENCES recon_task(id),
    record_id UUID,                  -- Unmatched record
    side VARCHAR(10),                -- source_a / source_b
    category VARCHAR(50),            -- Discrepancy category (AI-classified)
    ai_root_cause TEXT,              -- AI root cause analysis
    ai_suggestion JSONB,             -- AI resolution suggestion
    amount_diff DECIMAL(18,2),
    risk_level VARCHAR(10),          -- low/medium/high/critical
    handler_id UUID,                 -- Assigned handler
    status VARCHAR(20),              -- pending/processing/resolved/closed
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Raw Data Source Record
CREATE TABLE raw_record (
    id UUID PRIMARY KEY,
    source_id UUID REFERENCES data_source(id),
    batch_id VARCHAR(100),
    raw_data JSONB,                  -- Original data
    normalized_data JSONB,           -- Normalized data
    amount DECIMAL(18,2),
    currency VARCHAR(3),
    transaction_date DATE,
    transaction_ref VARCHAR(500),    -- Transaction reference / serial number
    description TEXT,                -- Summary / description
    counter_party VARCHAR(300),      -- Counterparty name
    direction VARCHAR(10),           -- debit/credit
    hash_value VARCHAR(64),          -- Data fingerprint (for dedup)
    created_at TIMESTAMP DEFAULT NOW()
);

-- AI Inference Log (for audit)
CREATE TABLE ai_inference_log (
    id UUID PRIMARY KEY,
    module VARCHAR(50),              -- field_mapping/semantic_match/root_cause/...
    input_data JSONB,
    output_data JSONB,
    model_used VARCHAR(100),
    tokens_used INT,
    latency_ms INT,
    confidence DECIMAL(5,2),
    user_feedback VARCHAR(20),       -- accepted/rejected/modified
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## 7. Non-Functional Requirements

### 7.1 Security

| Requirement | Implementation |
|-------------|---------------|
| Transport Encryption | TLS 1.3 mandatory |
| Data Encryption | AES-256 at rest + Field-level encryption (bank account numbers, etc.) |
| Authentication | OAuth 2.0 + MFA (Multi-Factor Authentication) |
| Authorization | RBAC + ABAC (Attribute-Based Access Control) |
| Data Masking | Sensitive field display masking on statements |
| Penetration Testing | Quarterly third-party security audits |
| Compliance Certifications | SOC 2 Type II / ISO 27001 / China Classified Protection Level 3 |

### 7.2 Scalability

- Horizontal scaling: All services stateless, support K8s HPA
- Multi-tenancy: Database-level isolation (large customers) / Schema-level isolation (SMBs)
- Plugin architecture: Data source connectors, match algorithms, discrepancy handlers all hot-pluggable
- Internationalization: i18n framework + configuration-driven

### 7.3 Reliability

- Service resilience: Circuit breaker + Retry + Degradation
- Data backup: Daily full + Real-time incremental backup (RPO ≤ 1 minute)
- Disaster recovery: Cross-AZ deployment, RTO ≤ 30 minutes
- Message idempotency: All data sync operations idempotent by design

### 7.4 Observability

- Logging: Structured logs (JSON) + ELK centralized storage
- Metrics: Prometheus + Grafana (business metrics + technical metrics)
- Tracing: Distributed tracing (OpenTelemetry)
- Alerting: Tiered alerts + on-call rotation

---

## 8. Implementation Roadmap

### Phase 1: MVP (3 months)

**Goal:** Validate core value — AI matching outperforms traditional rules

```
Scope:
✅ Data Source Management: Excel/CSV import + Host-to-host banking (2 banks)
✅ Smart Field Mapping (AI-powered)
✅ Reconciliation Rule Engine: Pre-built templates + Visual orchestration
✅ Smart Matching Engine: Exact match + Rule match + AI semantic match
✅ Discrepancy Management: AI auto-classification + Root cause analysis
  + Resolution suggestions
✅ Basic Workbench: Manual adjustments + Batch operations
✅ Result Export: Excel/PDF

Not Included:
❌ Approval workflow
❌ Natural language query
❌ Open API
❌ Multi-org architecture
❌ Third-party payment integration
```

### Phase 2: Enhanced (3 months)

**Goal:** Complete enterprise-grade capabilities, support paying customer go-live

```
New:
✅ Approval Workflow (Camunda)
✅ Natural Language Query (Text-to-SQL + RAG)
✅ Third-Party Payment Integration (WeChat Pay/Alipay/PayPal)
✅ Multi-Bank Connectivity (10+ banks)
✅ REST API v1
✅ Reconciliation Health Dashboard
✅ Scheduled Execution + Auto-run
✅ Multi-Org / Multi-Ledger Support
```

### Phase 3: Full Release (3 months)

**Goal:** Build ecosystem moat, comprehensively surpass traditional vendors

```
New:
✅ Natural Language Rule Generation
✅ Anomaly Trend Alerts
✅ Auto Report Generation
✅ Plugin Marketplace + SDK
✅ Webhook Event Push
✅ Blockchain Audit Notarization
✅ Mobile Adaptation
✅ Internationalization (English/Japanese)
✅ 30+ Banks + 5+ Payment Channels
```

### Phase 4: Intelligent Evolution (Ongoing)

**Goal:** Evolve from tool to intelligent finance assistant

```
✅ Continuous Learning: Auto-optimize matching models from user feedback
✅ Predictive Reconciliation: Predict risks before discrepancies occur
✅ Cross-Enterprise Benchmarking: Industry reconciliation efficiency analysis
✅ Finance LLM Fine-tuning: Domain-specific small models replace general LLMs
✅ Multi-Agent Collaboration: Recon Agent + Tax Agent + Analysis Agent
```

---

## 9. Appendix: Core Algorithm Descriptions

### 9.1 AI Semantic Matching Algorithm

```
Algorithm: Multi-Stage Semantic Matching

Input:
  - Unmatched record set U = {u₁, u₂, ..., uₙ} (from Data Source A)
  - Candidate record set C = {c₁, c₂, ..., cₘ} (from Data Source B)
  - Date window W (default ±7 days)
  - Amount tolerance T (default ±1%)

Output:
  - Match pair list M = {(uᵢ, cⱼ, score, explanation)}

Flow:
  Stage 1: Candidate Filtering (Inverted Index)
    For each uᵢ:
      1. Filter by date window W → Candidate set C'
      2. Filter by amount tolerance T → Candidate set C''
      3. Coarse ranking by counterparty hash index → Top K candidates

  Stage 2: Vector Similarity Coarse Ranking (Embedding)
    For each uᵢ's Top K candidates:
      1. Concatenate text: "[Counterparty] [Description] [Amount] [Date]"
      2. BGE-M3 → vectors v(uᵢ), v(cⱼ)
      3. cosine_similarity(v(uᵢ), v(cⱼ)) → ranking
      4. Keep Top 3 for fine ranking

  Stage 3: LLM Multi-Dimension Fine Ranking
    For each uᵢ's Top 3 candidates:
      Prompt:
        "You are a financial reconciliation expert. Please assess whether
        the following two records represent the same transaction, scoring
        on the following dimensions (0-1):
        - Amount match quality
        - Date match quality
        - Counterparty name semantic similarity
        - Transaction description semantic similarity
        - Transaction reference number match quality

        Record A: {uᵢ as JSON}
        Record B: {cⱼ as JSON}

        Return scores and reasoning in JSON format."

      Parse LLM-returned scores → Weighted composite score
      Weight = [0.30, 0.20, 0.25, 0.15, 0.10]

  Stage 4: Decision
    Composite score ≥ 0.85 → Auto-match
    Composite score 0.70-0.85 → Recommended match
    Composite score < 0.70 → Flag as discrepancy

  Stage 5: Split Supplement (Optional)
    For still-unmatched uᵢ, attempt subset sum search in C:
      subset_sum(target=uᵢ.amount, candidates=C.amounts, tolerance=T)
      If solution found → Flag as "one-to-many" match
```

### 9.2 Discrepancy Root Cause Analysis Algorithm

```
Algorithm: LLM Chain-of-Thought Discrepancy Root Cause Analysis

Input:
  - Discrepancy record d (with source record and target candidates)
  - Context: Account type, transaction history, industry type

Flow:
  1. Feature Extraction:
     - Amount difference numerical features (absolute, percentage, integer)
     - Date difference features (calendar days/business days, month/qtr/year crossing)
     - Text similarity features (edit distance, semantic similarity)
     - Structural features (currency, account type, transaction type)

  2. LLM Reasoning Chain (Chain-of-Thought):
     Prompt:
     "You are a senior financial auditor. Please analyze the root cause
     of the following reconciliation discrepancy:

     Discrepancy Information:
     - ERP Record: {json}
     - Bank Record: {json}
     - Account Type: {account_type}

     Please reason through the following steps:
     Step 1: What are the numerical characteristics of the amount difference?
     Step 2: What type of discrepancy might these characteristics correspond to?
     Step 3: Combining transaction context, what is the most likely root cause?
     Step 4: What additional information is needed to confirm?
     Step 5: Provide resolution recommendations.

     Output the complete analysis in JSON format."

  3. Result Validation:
     - Is the discrepancy category in the known taxonomy?
     - Is the recommendation actionable?
     - Is the confidence reasonable?

  4. Feedback Loop:
     - User confirms/corrects → Stored as labeled data
     - Periodic classification model fine-tuning
```

### 9.3 Natural Language Query Execution Flow

```
User: "How many unresolved discrepancies over 10,000 RMB were there
      with ICBC last month?"

Step 1: LLM Intent Understanding + Entity Recognition
  → intent: "aggregate_query"
  → entities: { bank: "ICBC", period: "last_month", amount: ">10000",
               status: "unresolved" }

Step 2: Text-to-SQL Generation
  → SELECT COUNT(*) FROM recon_discrepancy d
    JOIN recon_task t ON d.task_id = t.id
    JOIN account a ON t.account_id = a.id
    WHERE a.bank_name = 'ICBC'
      AND t.period = '2024-06'
      AND ABS(d.amount_diff) > 10000
      AND d.status IN ('pending', 'processing')

Step 3: SQL Validation & Execution
  → Syntax validation → Permission check → Execute → Result: 8 records

Step 4: LLM Result Polishing
  → "In June 2024 ICBC reconciliation, there are 8 discrepancies over
     10,000 RMB that remain unresolved, totaling ¥156,200.00.
     The largest single item is ¥52,000.00 (supplier payment delay).
     Would you like me to list the details of all 8 items?"
```

---

> 📌 **This document is v1.3. Planned future iterations:**
> 1. Adjust AI feature priorities based on actual customer validation
> 2. Refine UI/UX interaction design for each module
> 3. Supplement performance benchmark and data security whitepaper
> 4. Develop pricing strategy and SaaS plan design
>
> 🤖 Generated with [Claude Code](https://claude.com/claude-code)
