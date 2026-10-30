(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const state = { csrf: null, profile: null, accounts: [], cards: [], transactions: [], cardRequests: [], membershipRequests: [], loans: [], support: [], selectedRecipient: null, recipientTimer: null, transferPayload: null, page: 'dashboard', toastTimer: null, chart: null, balanceVisible: true, transactionFilter: '', transactionType: 'ALL', transactionStatus: 'ALL', accountFilter: 'ALL', transactionSort: 'newest', transactionPage: 1, analyticsCurrency: null, analyticsMonths: 6 };
  const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const initials = (a = '', b = '') => `${a.trim().slice(0, 1)}${b.trim().slice(0, 1)}`.toUpperCase() || 'N';
  const fmtMoney = (amount, currency = 'EUR') => { try { return new Intl.NumberFormat('en-GB', { style: 'currency', currency, minimumFractionDigits: 2 }).format(Number(amount)); } catch { return `${Number(amount).toFixed(2)} ${currency}`; } };
  const fmtDate = (date, time = false) => date ? new Intl.DateTimeFormat('en-GB', time ? { dateStyle: 'medium', timeStyle: 'short' } : { dateStyle: 'medium' }).format(new Date(date)) : '—';
  const title = page => ({ dashboard: 'Overview', accounts: 'Accounts', cards: 'Cards', 'card-tiers': 'Membership', transfers: 'Send money', transactions: 'Transactions', analytics: 'Insights', loans: 'Loans', support: 'Support', profile: 'Profile' }[page] || 'Overview');
  const headers = new Headers({ Accept: 'application/json' });

  async function getCsrf() {
    const response = await fetch('/csrf', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('Your secure session could not be refreshed. Please sign in again.');
    state.csrf = await response.json();
  }
  async function api(url, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const requestHeaders = new Headers(options.headers || {});
    requestHeaders.set('Accept', 'application/json');
    if (options.body && !requestHeaders.has('Content-Type')) requestHeaders.set('Content-Type', 'application/json');
    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      if (!state.csrf) await getCsrf();
      requestHeaders.set(state.csrf.headerName, state.csrf.token);
    }
    const response = await fetch(url, { ...options, method, headers: requestHeaders, credentials: 'same-origin' });
    if (response.redirected && new URL(response.url).pathname.startsWith('/login')) { location.assign('/login/client'); throw new Error('Your session has expired.'); }
    const contentType = response.headers.get('content-type') || '';
    const payload = contentType.includes('json') ? await response.json() : null;
    if (response.status === 401) { location.assign('/login/client'); throw new Error('Please sign in again.'); }
    if (!response.ok) throw new Error(payload?.detail || payload?.message || `Request failed (${response.status})`);
    return payload;
  }
  function toast(message, type = 'success') {
    const node = $('#customerToast'); node.textContent = message; node.className = `customer-toast show ${type}`;
    clearTimeout(state.toastTimer); state.toastTimer = setTimeout(() => node.classList.remove('show'), 3400);
  }
  function errorHtml(message) { return `<div class="page-alert" role="alert">${esc(message)}</div>`; }
  function empty(titleText, description, symbol = '⌁') { return `<div class="empty-panel"><i aria-hidden="true">${symbol}</i><strong>${esc(titleText)}</strong><p>${esc(description)}</p></div>`; }
  function heading(kicker, pageTitle, copy, action = '') { return `<div class="page-heading-customer"><div><span class="overline">${esc(kicker)}</span><h1>${esc(pageTitle)}</h1><p>${esc(copy)}</p></div>${action}</div>`; }
  function statusTag(status) { const cls = ['PENDING', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER'].includes(status) ? 'pending' : ['REJECTED', 'BLOCKED'].includes(status) ? 'rejected' : ''; return `<span class="status-tag ${cls}">${esc(String(status || 'COMPLETED').replaceAll('_', ' '))}</span>`; }
  function cardMarkup(card, actions = true) {
    const tier = card.tier.toLowerCase();
    return `<article class="payment-card ${tier}"><div class="card-top"><span class="card-tier-label">NORDBANK · ${esc(card.tier)} CARD</span><span class="card-status ${card.status.toLowerCase()}">${esc(card.status)}</span></div><div><span class="card-chip" aria-hidden="true"></span><div class="card-number">•••• &nbsp;•••• &nbsp;•••• &nbsp;${esc(card.lastFour)}</div></div><div class="card-bottom"><span><small class="cardholder-label">CARDHOLDER</small><span class="cardholder-name">${esc(`${state.profile.firstName} ${state.profile.lastName}`)}</span></span><span class="card-expiry">${esc(new Date(card.expiresOn).toLocaleDateString('en-GB', { month: '2-digit', year: '2-digit' }))}</span></div>${actions ? `<div class="card-actions"><button class="button-customer small" data-card-action="${card.status === 'FROZEN' ? 'ACTIVE' : 'FROZEN'}" data-card-id="${esc(card.id)}">${card.status === 'FROZEN' ? 'Unfreeze' : 'Freeze'}</button><button class="button-customer small danger" data-card-action="BLOCKED" data-card-id="${esc(card.id)}">Block</button></div>` : ''}</article>`;
  }
  function transactionRows(transactions) {
    if (!transactions.length) return empty('No transactions found', 'Try adjusting the filters or make a transfer to get started.', '↗');
    return `<div class="table-wrap"><table class="customer-table"><thead><tr><th>TRANSACTION</th><th>DATE</th><th>REFERENCE</th><th>STATUS</th><th>AMOUNT</th></tr></thead><tbody>${transactions.map(tx => {
      const signed = tx.incoming ? `+${fmtMoney(tx.amount, tx.currency)}` : `−${fmtMoney(tx.amount, tx.currency)}`;
      const label = tx.type === 'TRANSFER' ? (tx.incoming ? `From ${tx.otherParty || 'NordBank customer'}` : `To ${tx.otherParty || 'NordBank customer'}`) : tx.type === 'DEPOSIT' ? 'Deposit' : tx.type === 'WITHDRAWAL' ? 'Withdrawal' : tx.type;
      return `<tr tabindex="0" role="button" data-transaction-id="${esc(tx.id)}"><td><span class="transaction-title">${esc(tx.description || label)}</span><small class="transaction-sub">${esc(label)}${tx.accountLastFour ? ` · ${esc(tx.accountLastFour)}` : ''}</small></td><td>${esc(fmtDate(tx.occurredAt, true))}</td><td>${esc(tx.reference)}</td><td>${statusTag(tx.status)}</td><td class="${tx.incoming ? 'amount-in' : 'amount-out'}">${esc(signed)}</td></tr>`;
    }).join('')}</tbody></table></div>`;
  }
  function balanceGroups() {
    return state.accounts.filter(a => a.status === 'OPEN').reduce((groups, account) => { groups[account.currency] = (groups[account.currency] || 0) + Number(account.balance); return groups; }, {});
  }
  function totalLabel() { const items = Object.entries(balanceGroups()); if (!items.length) return fmtMoney(0); return items.map(([currency, value]) => fmtMoney(value, currency)).join(' · '); }
  async function loadBase() {
    const [profile, accounts, cards] = await Promise.all([api('/api/customer/me'), api('/api/customer/accounts'), api('/api/customer/cards')]);
    state.profile = profile; state.accounts = accounts; state.cards = cards;
    $('#customerShortName').textContent = `${profile.firstName} ${profile.lastName}`;
    $('#customerAvatar').textContent = initials(profile.firstName, profile.lastName);
    $('#customerTierBadge').textContent = profile.membershipTier;
    $('#customerPageTitle').textContent = title(state.page);
  }
  async function loadTransactions() { state.transactions = await api('/api/customer/transactions'); return state.transactions; }
  function recentTransactions(items, limit = 5) { return transactionRows(items.slice(0, limit)); }
  function renderDashboard() {
    const openAccounts = state.accounts.filter(a => a.status === 'OPEN');
    const balance = state.balanceVisible ? esc(totalLabel()) : '••••••••';
    const recent = state.transactions.slice(0, 6);
    const cards = state.cards.slice(0, 2);
    return `<div class="dashboard-greeting"><div><span class="overline">YOUR FINANCIAL OVERVIEW</span><h1>Good ${new Date().getHours() < 12 ? 'morning' : new Date().getHours() < 18 ? 'afternoon' : 'evening'}, ${esc(state.profile.firstName)}.</h1><p>Here’s a clear view of your money today.</p></div><span class="membership-pill">✧ ${esc(state.profile.membershipTier)} MEMBER</span></div>
      <div class="dashboard-grid"><section class="balance-panel"><div class="balance-panel-top"><span class="balance-label">TOTAL AVAILABLE BALANCE</span><button class="visibility-button" id="balanceVisibility" type="button" aria-pressed="${state.balanceVisible}">${state.balanceVisible ? 'Hide balance' : 'Show balance'}</button></div><div><div class="balance-value">${balance}</div><div class="balance-subtitle">Across ${openAccounts.length} open ${openAccounts.length === 1 ? 'account' : 'accounts'} · balances grouped by currency</div></div><div class="balance-panel-bottom"><span>${esc(state.profile.customerNumber)}</span><button class="visibility-button" data-page="accounts" type="button">Manage accounts →</button></div></section><div class="dashboard-side-stats"><div class="dashboard-stat"><span class="stat-label">OPEN ACCOUNTS</span><strong>${openAccounts.length}</strong><small>Ready for everyday banking</small></div><div class="dashboard-stat"><span class="stat-label">PAYMENT CARDS</span><strong>${state.cards.length}</strong><small>${state.cards.filter(c => c.status === 'ACTIVE').length} active cards</small></div><div class="dashboard-stat"><span class="stat-label">MEMBERSHIP</span><strong style="font-size:17px">${esc(state.profile.membershipTier)}</strong><small>View your benefits</small></div><div class="dashboard-stat"><span class="stat-label">OPEN REQUESTS</span><strong>${state.support.filter(s => s.status !== 'RESOLVED').length + state.loans.filter(l => l.status === 'PENDING').length}</strong><small>Support and loan reviews</small></div></div></div>
      <div class="quick-actions"><button class="quick-action" data-page="transfers"><i>⇄</i><span>Send money</span></button><button class="quick-action" data-page="accounts"><i>▤</i><span>Account details</span></button><button class="quick-action" data-page="cards"><i>▣</i><span>Manage cards</span></button><button class="quick-action" data-page="analytics"><i>▥</i><span>Insights</span></button><button class="quick-action" data-page="support"><i>◇</i><span>Get support</span></button></div>
      <section class="section-block"><div class="section-title-row"><div><span class="overline">YOUR CARDS</span><h2>Everyday payments</h2></div><button class="text-button" data-page="cards">Manage cards →</button></div>${cards.length ? `<div class="card-list">${cards.map(c => cardMarkup(c, false)).join('')}</div>` : empty('Your first card is one step away', 'Request a standard card and link it to one of your accounts.', '▣')}</section>
      <section class="section-block"><div class="section-title-row"><div><span class="overline">ACCOUNT ACTIVITY</span><h2>Recent transactions</h2></div><button class="text-button" data-page="transactions">View all →</button></div>${recentTransactions(recent, 5)}</section>`;
  }
  async function renderAccounts() {
    await loadTransactions();
    return `${heading('YOUR MONEY', 'Your accounts', 'See balances and transaction activity for every account linked to your profile.', '')}
      ${state.accounts.length ? `<div class="account-grid">${state.accounts.map(account => `<article class="data-card account-tile"><div class="account-tile-top"><span class="account-kind">${account.type === 'CURRENT' ? 'Current account' : 'Savings account'}</span><span class="account-currency">${esc(account.currency)}</span></div><div class="account-number">${esc(account.accountNumber)}</div><div class="account-tile-bottom"><span class="account-balance">${esc(fmtMoney(account.balance, account.currency))}</span><span class="account-state ${String(account.status).toLowerCase()}">${esc(account.status)}</span></div><div class="inline-message">Opened ${esc(fmtDate(account.openedAt))}</div><div class="account-management-actions">${['OPEN', 'FROZEN'].includes(account.status) ? `<button class="button-customer small ${account.status === 'FROZEN' ? '' : 'primary'}" data-account-action="${account.status === 'FROZEN' ? 'OPEN' : 'FROZEN'}" data-account-id="${esc(account.id)}">${account.status === 'FROZEN' ? 'Unfreeze account' : 'Freeze account'}</button>` : ''}</div></article>`).join('')}</div>` : empty('No accounts yet', 'Your account list is empty. Contact customer support if you need help.')}
      <section class="section-block"><div class="section-title-row"><div><span class="overline">LATEST ACTIVITY</span><h2>Account transactions</h2></div><button class="text-button" data-page="transactions">Full history →</button></div>${recentTransactions(state.transactions, 8)}</section>`;
  }
  async function renderCards() {
    const [cards, requests, accounts] = await Promise.all([api('/api/customer/cards'), api('/api/customer/card-requests'), api('/api/customer/accounts')]);
    state.cards = cards; state.cardRequests = requests;
    const openAccounts = accounts.filter(a => a.status === 'OPEN');
    return `${heading('PAYMENT CARDS', 'Cards for your everyday', 'Manage simulated payment cards linked to your NordBank accounts.', '<button class="button-customer primary" data-action="new-card">Request a card <span>＋</span></button>')}
      ${cards.length ? `<div class="card-list">${cards.map(c => cardMarkup(c)).join('')}</div>` : empty('Your wallet is ready for its first card', 'Request a standard card to get started with this simulation.', '▣')}
      <section class="section-block"><div class="section-title-row"><div><span class="overline">CARD REQUESTS</span><h2>Request history</h2></div><button class="text-button" data-page="card-tiers">Explore membership →</button></div>${requests.length ? `<div class="table-wrap"><table class="customer-table"><thead><tr><th>REQUESTED CARD</th><th>DATE</th><th>STATUS</th><th>DECISION</th></tr></thead><tbody>${requests.map(r => `<tr><td class="transaction-title">${esc(r.tier)} card</td><td>${esc(fmtDate(r.requestedAt))}</td><td>${statusTag(r.status)}</td><td>${esc(r.decisionReason || '—')}</td></tr>`).join('')}</tbody></table></div>` : empty('No card requests', 'Higher tier cards require review by a NordBank employee.')}</section>
      <dialog class="customer-dialog" id="cardRequestDialog"><button class="dialog-close" type="button" data-close-dialog aria-label="Close">×</button><h2>Request a payment card</h2><p class="auth-subtitle">Cards are simulated. No real card credentials are issued or stored.</p><form id="cardRequestForm"><div class="form-grid-customer"><label class="field-stack full"><span>Link to account</span><select class="customer-select" name="accountId" required>${openAccounts.map(a => `<option value="${esc(a.id)}">${esc(a.type)} · ${esc(a.currency)} · ${esc(a.accountNumber.slice(-4))}</option>`).join('')}</select></label><label class="field-stack full"><span>Card tier</span><select class="customer-select" name="tier" required>${['STANDARD','PREMIUM','VIP'].filter(t => ['STANDARD','PREMIUM','VIP'].indexOf(t) <= ['STANDARD','PREMIUM','VIP'].indexOf(state.profile.membershipTier)).map(t => `<option value="${t}">${t}</option>`).join('')}</select><small class="inline-message">Your membership determines eligible card tiers. Premium and VIP card requests require review.</small></label></div><div class="form-error-customer" id="cardRequestError"></div><div class="form-actions-customer"><button class="button-customer primary" type="submit">Submit request</button></div></form></dialog>`;
  }
  async function renderTiers() {
    const [requests] = await Promise.all([api('/api/customer/membership-requests')]); state.membershipRequests = requests;
    const current = state.profile.membershipTier;
    const tiers = [
      { name: 'STANDARD', fee: 'Free', limit: '€1,000', features: ['Everyday digital banking', 'Standard card design', 'Basic financial overview'] },
      { name: 'PREMIUM', fee: '€5', limit: '€3,000', features: ['Priority customer support', 'Premium card design', 'Enhanced financial insights'] },
      { name: 'VIP', fee: '€15', limit: '€10,000', features: ['Priority service', 'Exclusive card design', 'Advanced financial insights'] }
    ];
    return `${heading('MEMBERSHIP', 'Choose how you bank', 'Compare simulated membership features. Premium and VIP upgrades require employee approval.', `<span class="membership-pill">CURRENT · ${esc(current)}</span>`)}
      <div class="tier-grid">${tiers.map((tier, index) => `<article class="tier-card ${tier.name === 'PREMIUM' ? 'featured' : ''}"><span class="overline">${index === 0 ? 'EVERYDAY' : index === 1 ? 'MORE FOR YOU' : 'EXCLUSIVE'}</span><h3>${tier.name}</h3><div class="tier-price">${tier.fee}<small> / month · simulated</small></div><ul><li>Daily card limit ${tier.limit}</li>${tier.features.map(f => `<li>${esc(f)}</li>`).join('')}</ul>${tier.name === current ? '<button class="button-customer small" disabled>Current membership</button>' : index < ['STANDARD','PREMIUM','VIP'].indexOf(current) ? '<button class="button-customer small" disabled>Previous tier</button>' : `<button class="button-customer ${index === 2 ? 'gold' : 'primary'} small" data-membership-tier="${tier.name}">Request upgrade</button>`}</article>`).join('')}</div>
      <p class="info-note" style="margin-top:12px">Membership fees and limits are fictional examples for this educational project. No fees are charged. A membership changes only after an authorized employee approves your request.</p>
      <section class="section-block"><div class="section-title-row"><div><span class="overline">REQUEST HISTORY</span><h2>Membership changes</h2></div></div>${requests.length ? `<div class="table-wrap"><table class="customer-table"><thead><tr><th>REQUESTED TIER</th><th>SUBMITTED</th><th>STATUS</th><th>DECISION</th></tr></thead><tbody>${requests.map(r => `<tr><td class="transaction-title">${esc(r.requestedTier)}</td><td>${esc(fmtDate(r.requestedAt))}</td><td>${statusTag(r.status)}</td><td>${esc(r.decisionReason || 'Pending review')}</td></tr>`).join('')}</tbody></table></div>` : empty('No membership requests', 'If you request an upgrade, its progress will appear here.')}</section>`;
  }
  async function renderTransfers() {
    const eligibleAccounts = state.accounts.filter(a => a.status === 'OPEN');
    return `${heading('INTERNAL TRANSFER', 'Send money to a NordBank customer', 'Transfers are simulated internally and settled immediately after you confirm.', '')}
      <div class="transfer-steps"><span class="transfer-step active" id="transferStep1"><i>1</i> Details</span><span class="transfer-step" id="transferStep2"><i>2</i> Review</span><span class="transfer-step" id="transferStep3"><i>3</i> Confirmation</span></div>
      <section class="data-card review-card" id="transferFormPanel"><form id="transferForm"><div class="form-grid-customer">
        <label class="field-stack full"><span>From account</span><select class="customer-select" name="sourceAccountId" required>${eligibleAccounts.map(a => `<option value="${esc(a.id)}">${esc(a.type)} · ${esc(a.currency)} · ${esc(fmtMoney(a.balance,a.currency))} · ${esc(a.accountNumber.slice(-4))}</option>`).join('')}</select></label>
        <label class="field-stack full"><span>Recipient</span><input class="customer-input" id="recipientSearch" autocomplete="off" placeholder="Search by name or email" minlength="2" required><div id="recipientResults" class="recipient-results" hidden></div><div id="selectedRecipient" class="selected-recipient" hidden></div></label>
        <label class="field-stack full"><span>Recipient account</span><select class="customer-select" name="targetAccountId" id="recipientAccount" required disabled><option value="">Choose a recipient first</option></select></label>
        <label class="field-stack"><span>Amount</span><input class="customer-input" type="number" name="amount" min="0.01" step="0.01" max="1000000" required placeholder="0.00"></label>
        <label class="field-stack"><span>Currency</span><input class="customer-input" id="transferCurrency" value="EUR" readonly></label>
        <label class="field-stack full"><span>Description <small>Optional</small></span><input class="customer-input" name="description" maxlength="140" placeholder="What is this transfer for?"></label>
      </div><div class="form-error-customer" id="transferError"></div><div class="form-actions-customer"><button class="button-customer primary" type="submit" ${eligibleAccounts.length ? '' : 'disabled'}>Review transfer →</button></div></form></section><section id="transferReviewPanel" class="data-card review-card" hidden></section><section id="transferSuccessPanel" class="data-card review-card" hidden></section>
      <p class="info-note" style="margin-top:12px">Transfers can only be sent to another registered NordBank customer. Fees: none in this educational simulation. Make sure the recipient and account are correct before confirming.</p>`;
  }
  function applyTransactionFilters() {
    const query = state.transactionFilter.toLowerCase();
    const from = $('#transactionFrom')?.value ? new Date(`${$('#transactionFrom').value}T00:00:00`) : null;
    const to = $('#transactionTo')?.value ? new Date(`${$('#transactionTo').value}T23:59:59`) : null;
    let filtered = state.transactions.filter(tx => {
      const text = `${tx.description || ''} ${tx.reference} ${tx.otherParty || ''} ${tx.type}`.toLowerCase();
      const date = new Date(tx.occurredAt);
      return (!query || text.includes(query)) && (state.transactionType === 'ALL' || tx.type === state.transactionType) && (state.transactionStatus === 'ALL' || tx.status === state.transactionStatus) && (state.accountFilter === 'ALL' || tx.accountId === state.accountFilter) && (!from || date >= from) && (!to || date <= to);
    });
    filtered.sort((a, b) => state.transactionSort === 'oldest' ? new Date(a.occurredAt) - new Date(b.occurredAt) : state.transactionSort === 'amount' ? Number(b.amount) - Number(a.amount) : new Date(b.occurredAt) - new Date(a.occurredAt));
    return filtered;
  }
  async function renderTransactions() {
    await loadTransactions();
    const accounts = state.accounts.map(a => `<option value="${esc(a.id)}" ${state.accountFilter===a.id?'selected':''}>${esc(a.type)} · ${esc(a.currency)} · ${esc(a.accountNumber.slice(-4))}</option>`).join('');
    return `${heading('YOUR ACTIVITY', 'Transaction history', 'Search and review incoming and outgoing activity across your accounts.', '')}<div class="filter-row"><input class="customer-input" id="transactionSearch" placeholder="Search description, name or reference" value="${esc(state.transactionFilter)}"><select class="customer-select" id="transactionType"><option value="ALL">All activity</option><option value="TRANSFER">Transfers</option><option value="DEPOSIT">Deposits</option><option value="WITHDRAWAL">Withdrawals</option></select><select class="customer-select" id="transactionStatus"><option value="ALL">All statuses</option><option value="COMPLETED">Completed</option><option value="PENDING">Pending</option><option value="REJECTED">Rejected</option></select><select class="customer-select" id="transactionAccount"><option value="ALL">All accounts</option>${accounts}</select><input class="customer-input" id="transactionFrom" type="date" aria-label="From date"><input class="customer-input" id="transactionTo" type="date" aria-label="To date"><select class="customer-select" id="transactionSort"><option value="newest">Newest first</option><option value="oldest">Oldest first</option><option value="amount">Largest amount</option></select></div><div id="transactionResults">${transactionRows(state.transactions)}</div><div class="transaction-pagination" id="transactionCount"></div>`;
  }
  function monthSeries(items, count = 6) {
    const now = new Date(); const months = [];
    for (let i = count - 1; i >= 0; i--) { const date = new Date(now.getFullYear(), now.getMonth() - i, 1); months.push({ key: `${date.getFullYear()}-${date.getMonth()}`, label: date.toLocaleDateString('en-GB', { month: 'short' }), income: 0, expenses: 0 }); }
    for (const tx of items) { const date = new Date(tx.occurredAt); const item = months.find(m => m.key === `${date.getFullYear()}-${date.getMonth()}`); if (item) item[tx.incoming ? 'income' : 'expenses'] += Number(tx.amount); }
    return months;
  }
  function drawChart(canvas, series, currency) {
    if (state.chart) { state.chart.destroy(); state.chart = null; }
    if (!window.Chart) { const parent = canvas.parentElement; parent.innerHTML = '<div class="empty-panel"><strong>Chart library unavailable</strong><p>Summary totals are available above. Reload when the chart service is reachable.</p></div>'; return; }
    state.chart = new Chart(canvas, { type: 'bar', data: { labels: series.map(m => m.label), datasets: [{ label: 'Income', data: series.map(m => m.income), backgroundColor: '#5b9b7d', borderRadius: 5 }, { label: 'Expenses', data: series.map(m => m.expenses), backgroundColor: '#778faf', borderRadius: 5 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 7, font: { size: 9 } } }, tooltip: { callbacks: { label: context => `${context.dataset.label}: ${fmtMoney(context.raw,currency)}` } } }, scales: { x: { grid: { display: false }, ticks: { font: { size: 8 } } }, y: { grid: { color: '#edf0f4' }, ticks: { font: { size: 8 }, callback: value => fmtMoney(value,currency) } } } } });
  }
  async function renderAnalytics() {
    await loadTransactions();
    const currencies = [...new Set([...state.transactions.map(t => t.currency), ...state.accounts.map(a => a.currency)])].sort();
    if (!state.analyticsCurrency || !currencies.includes(state.analyticsCurrency)) state.analyticsCurrency = currencies.includes('EUR') ? 'EUR' : currencies[0] || 'EUR';
    const items = state.transactions.filter(t => t.currency === state.analyticsCurrency);
    const income = items.filter(t => t.incoming).reduce((s,t) => s + Number(t.amount), 0);
    const expenses = items.filter(t => !t.incoming).reduce((s,t) => s + Number(t.amount), 0);
    const net = income - expenses; const series = monthSeries(items, state.analyticsMonths);
    const averageMonthlySpending = expenses / Math.max(1, state.analyticsMonths);
    setTimeout(() => { const canvas = $('#incomeExpenseChart'); if (canvas) drawChart(canvas, series, state.analyticsCurrency); }, 0);
    return `${heading('FINANCIAL INSIGHTS', 'Understand your activity', 'Monthly income and expenses based on your recorded account transactions.', '')}<div class="filter-row analytics-filters"><label class="field-stack"><span>Currency</span><select class="customer-select" id="analyticsCurrency">${currencies.map(c=>`<option value="${esc(c)}" ${c===state.analyticsCurrency?'selected':''}>${esc(c)}</option>`).join('')}</select></label><label class="field-stack"><span>Period</span><select class="customer-select" id="analyticsPeriod"><option value="6" ${state.analyticsMonths===6?'selected':''}>Last 6 months</option><option value="12" ${state.analyticsMonths===12?'selected':''}>Last 12 months</option></select></label></div><div class="analytics-summary"><div class="analytics-value"><span>TOTAL INCOME · ${esc(state.analyticsCurrency)}</span><strong>${esc(fmtMoney(income,state.analyticsCurrency))}</strong></div><div class="analytics-value"><span>TOTAL EXPENSES · ${esc(state.analyticsCurrency)}</span><strong>${esc(fmtMoney(expenses,state.analyticsCurrency))}</strong></div><div class="analytics-value"><span>NET MOVEMENT · ${esc(state.analyticsCurrency)}</span><strong class="${net >= 0 ? 'amount-in' : 'amount-out'}">${esc(fmtMoney(net,state.analyticsCurrency))}</strong></div><div class="analytics-value"><span>MONTHLY AVG SPENDING</span><strong>${esc(fmtMoney(averageMonthlySpending,state.analyticsCurrency))}</strong></div><div class="analytics-value"><span>TRANSACTIONS</span><strong>${items.length}</strong></div></div><div class="analytics-grid"><section class="data-card chart-card"><div class="section-title-row"><div><span class="overline">LAST ${state.analyticsMonths} MONTHS · ${esc(state.analyticsCurrency)}</span><h2>Income and expenses</h2></div></div><div class="chart-container"><canvas id="incomeExpenseChart" aria-label="Monthly income and expenses chart"></canvas></div></section><aside class="data-card chart-card"><div class="section-title-row"><div><span class="overline">ACCOUNTS</span><h2>Balances by account</h2></div></div>${state.accounts.length ? `<div class="account-grid">${state.accounts.map(a => `<div class="profile-value"><span>${esc(a.type)} · ${esc(a.currency)}</span><strong>${esc(fmtMoney(a.balance,a.currency))}</strong></div>`).join('')}</div>` : empty('No account data','Accounts will appear here.') }<p class="info-note" style="margin-top:12px">Card purchase analytics are unavailable because the simulation does not process card payments. Merchant categories are not inferred.</p></aside></div><section class="section-block"><div class="section-title-row"><div><span class="overline">RECENT ACTIVITY</span><h2>Recorded transactions</h2></div><button class="text-button" data-page="transactions">Open history →</button></div>${recentTransactions(state.transactions,8)}</section>`;
  }
  async function renderLoans() {
    const loans = await api('/api/customer/loans'); state.loans = loans;
    return `${heading('SIMULATED CREDIT', 'Loan applications', 'Request a simulated loan and follow its review status. Estimates are illustrative and are not a credit offer.', '<button class="button-customer primary" data-action="new-loan">Start application</button>')}<div class="loan-product"><div><h3>Personal loan · illustrative example</h3><p>Fixed example APR 8.9%. A real loan assessment is not performed.</p></div><strong>8.9% APR</strong></div>${loans.length ? `<section class="section-block"><div class="table-wrap"><table class="customer-table"><thead><tr><th>AMOUNT</th><th>TERM</th><th>MONTHLY ESTIMATE</th><th>REQUESTED</th><th>STATUS</th><th>DECISION</th></tr></thead><tbody>${loans.map(l => `<tr><td>${esc(fmtMoney(l.amount))}</td><td>${l.termMonths} months</td><td>${esc(fmtMoney(monthlyPayment(l.amount,l.termMonths)))}</td><td>${esc(fmtDate(l.requestedAt))}</td><td>${statusTag(l.status)}</td><td>${esc(l.decisionReason || 'Awaiting review')}</td></tr>`).join('')}</tbody></table></div></section>` : empty('No applications yet','If you submit an application, its status and decision will appear here.')}<dialog class="customer-dialog" id="loanDialog"><button class="dialog-close" type="button" data-close-dialog>×</button><h2>Personal loan application</h2><p class="auth-subtitle">Estimate uses illustrative 8.9% APR and monthly amortization.</p><form id="loanForm"><div class="form-grid-customer"><label class="field-stack"><span>Amount (€500–€100,000)</span><input class="customer-input" type="number" name="amount" min="500" max="100000" step="0.01" required></label><label class="field-stack"><span>Repayment term</span><select class="customer-select" name="termMonths">${[12,24,36,48,60,72,84].map(n=>`<option value="${n}">${n} months</option>`).join('')}</select></label><label class="field-stack full"><span>Purpose</span><textarea class="customer-textarea" name="purpose" maxlength="300" required></textarea></label><div class="field-stack full"><span>Estimated monthly payment</span><strong id="loanEstimate">—</strong></div></div><div class="form-error-customer" id="loanError"></div><div class="form-actions-customer"><button class="button-customer primary" type="submit">Submit application</button></div></form></dialog>`;
  }
  function monthlyPayment(principal, months) { const rate = 0.089 / 12; return Number(principal) * rate / (1 - Math.pow(1 + rate, -Number(months))); }
  async function renderSupport() {
    const requests = await api('/api/customer/support'); state.support = requests;
    return `${heading('HERE TO HELP', 'Customer support', 'Send a request to our service team and follow every update from your account.', '<button class="button-customer primary" data-action="new-support">New request</button>')}${requests.length ? `<div class="table-wrap"><table class="customer-table"><thead><tr><th>SUBJECT</th><th>CATEGORY</th><th>OPENED</th><th>UPDATED</th><th>STATUS</th><th></th></tr></thead><tbody>${requests.map(r => `<tr><td class="transaction-title">${esc(r.subject)}<small class="transaction-sub">${esc(r.id.slice(0,8).toUpperCase())}</small></td><td>${esc(r.category)}</td><td>${esc(fmtDate(r.createdAt))}</td><td>${esc(fmtDate(r.updatedAt))}</td><td>${statusTag(r.status)}</td><td><button class="text-button" data-support-id="${esc(r.id)}">Details</button></td></tr>`).join('')}</tbody></table></div>` : empty('No support requests', 'Our service team is here if you need help.')}
      <dialog class="customer-dialog" id="supportDialog"><button class="dialog-close" type="button" data-close-dialog>×</button><h2>Contact customer support</h2><p class="auth-subtitle">Share only the information needed to help with your request.</p><form id="supportForm"><div class="form-grid-customer"><label class="field-stack full"><span>Category</span><select class="customer-select" name="category"><option>Accounts</option><option>Cards</option><option>Transfers</option><option>Membership</option><option>Other</option></select></label><label class="field-stack full"><span>Subject</span><input class="customer-input" name="subject" maxlength="160" required></label><label class="field-stack full"><span>Message</span><textarea class="customer-textarea" name="message" maxlength="5000" required></textarea></label></div><div class="form-error-customer" id="supportError"></div><div class="form-actions-customer"><button class="button-customer primary" type="submit">Send request</button></div></form></dialog>`;
  }
  function renderProfile() {
    const p = state.profile;
    return `${heading('ACCOUNT SETTINGS', 'Your profile', 'Your personal information and account identifiers. Contact support if a protected detail needs updating.', '')}<div class="profile-summary"><div class="profile-value"><span>FULL NAME</span><strong>${esc(p.firstName)} ${esc(p.lastName)}</strong></div><div class="profile-value"><span>EMAIL ADDRESS</span><strong>${esc(p.email)}</strong></div><div class="profile-value"><span>CUSTOMER NUMBER</span><strong>${esc(p.customerNumber)}</strong></div><div class="profile-value"><span>MEMBERSHIP</span><strong>${esc(p.membershipTier)}</strong></div><div class="profile-value"><span>MEMBER SINCE</span><strong>${esc(fmtDate(p.joinedAt))}</strong></div><div class="profile-value"><span>PHONE</span><strong>${esc(p.phone || 'Not provided')}</strong></div></div><section class="section-block"><div class="section-title-row"><div><span class="overline">CONTACT DETAILS</span><h2>Update your phone number</h2></div></div><form class="data-card review-card" id="profileForm"><label class="field-stack"><span>Phone number</span><input class="customer-input" type="tel" name="phone" maxlength="32" value="${esc(p.phone || '')}" placeholder="+371 2000 0000"></label><div class="form-error-customer" id="profileError"></div><div class="form-actions-customer"><button class="button-customer primary" type="submit">Save phone number</button></div></form></section><p class="info-note" style="margin-top:14px">To change your email address or password, contact support so your identity can be verified.</p>`;
  }
  async function renderPage(page) {
    state.page = page; $('#customerPageTitle').textContent = title(page); $$('.customer-nav a').forEach(a => a.classList.toggle('selected', a.dataset.page === page));
    $('#customerPage').innerHTML = '<div class="page-loading"><span></span>Loading your information…</div>';
    try {
      await loadBase();
      let markup;
      if (page === 'dashboard') {
        const [transactions, loans, support] = await Promise.all([loadTransactions(), api('/api/customer/loans'), api('/api/customer/support')]);
        state.transactions = transactions; state.loans = loans; state.support = support; markup = renderDashboard();
      } else if (page === 'accounts') markup = await renderAccounts();
      else if (page === 'cards') markup = await renderCards();
      else if (page === 'card-tiers') markup = await renderTiers();
      else if (page === 'transfers') markup = await renderTransfers();
      else if (page === 'transactions') markup = await renderTransactions();
      else if (page === 'analytics') markup = await renderAnalytics();
      else if (page === 'loans') markup = await renderLoans();
      else if (page === 'support') markup = await renderSupport();
      else markup = renderProfile();
      if (state.page !== page) return;
      $('#customerPage').innerHTML = markup;
      if (page === 'transactions') {
        $('#transactionType').value = state.transactionType;
        $('#transactionStatus').value = state.transactionStatus;
        $('#transactionSort').value = state.transactionSort;
        refreshTransactionTable();
      }
    } catch (error) { $('#customerPage').innerHTML = `${heading('SECURE CUSTOMER PORTAL', title(page), 'We could not load this section right now.', '')}${errorHtml(error.message)}<button class="button-customer" style="margin-top:12px" data-retry-page="${esc(page)}">Try again</button>`; }
  }

  document.addEventListener('click', event => {
    const nav = event.target.closest('[data-page]');
    if (nav) { event.preventDefault(); $('#profilePopover').hidden = true; $('#customerSidebar').classList.remove('open'); renderPage(nav.dataset.page); return; }
    const close = event.target.closest('[data-close-dialog]'); if (close) { close.closest('dialog').close(); return; }
    const retry = event.target.closest('[data-retry-page]'); if (retry) { renderPage(retry.dataset.retryPage); return; }
    const action = event.target.closest('[data-action]')?.dataset.action;
    if (action === 'new-card') { if (!state.accounts.some(a => a.status === 'OPEN')) return toast('You need an open account before requesting a card.', 'error'); $('#cardRequestDialog').showModal(); }
    if (action === 'new-loan') $('#loanDialog').showModal();
    if (action === 'new-support') $('#supportDialog').showModal();
    const tier = event.target.closest('[data-membership-tier]')?.dataset.membershipTier;
    if (tier && confirm(`Submit a request to upgrade to ${tier}? No fee is charged in this simulation.`)) submitMembership(tier);
    const status = event.target.closest('[data-card-action]'); if (status) updateCard(status.dataset.cardId, status.dataset.cardAction);
    const accountStatus = event.target.closest('[data-account-action]'); if (accountStatus) updateAccountStatus(accountStatus);
    const tx = event.target.closest('[data-transaction-id]'); if (tx) showTransaction(tx.dataset.transactionId);
    const support = event.target.closest('[data-support-id]'); if (support) showSupport(support.dataset.supportId);
  });
  document.addEventListener('keydown', event => { if ((event.key === 'Enter' || event.key === ' ') && event.target.matches('[data-transaction-id]')) { event.preventDefault(); showTransaction(event.target.dataset.transactionId); } });
  $('#customerProfileButton').addEventListener('click', () => { $('#profilePopover').hidden = !$('#profilePopover').hidden; });
  document.addEventListener('click', event => { if (!event.target.closest('.customer-top-actions')) $('#profilePopover').hidden = true; });
  $('#mobileNavToggle').addEventListener('click', () => { const open = $('#customerSidebar').classList.toggle('open'); $('#mobileNavToggle').setAttribute('aria-expanded', String(open)); });
  $('#customerLogout').addEventListener('submit', async event => { event.preventDefault(); try { if (!state.csrf) await getCsrf(); const form = new URLSearchParams(); form.set(state.csrf.parameterName, state.csrf.token); const result = await fetch('/logout/client', { method: 'POST', body: form, credentials: 'same-origin', headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }); if (!result.ok) throw new Error('Could not sign out securely.'); location.assign('/login/client?logout'); } catch (error) { toast(error.message, 'error'); } });

  $('#customerPage').addEventListener('click', event => {
    if (event.target.id === 'balanceVisibility') { state.balanceVisible = !state.balanceVisible; renderPage('dashboard'); return; }
    if (event.target.id === 'transferCancel') { $('#transferReviewPanel').hidden = true; $('#transferFormPanel').hidden = false; step(1); }
    if (event.target.id === 'transferConfirm') submitTransfer(event.target);
    if (event.target.id === 'transactionSearch') { state.transactionFilter = event.target.value; state.transactionPage = 1; }
    if (event.target.id === 'transactionType') state.transactionType = event.target.value;
    if (event.target.id === 'transactionSort') state.transactionSort = event.target.value;
    if (event.target.id.startsWith('transaction') && ['transactionSearch','transactionType','transactionStatus','transactionSort','transactionFrom','transactionTo'].includes(event.target.id)) refreshTransactionTable();
  });
  $('#customerPage').addEventListener('input', event => {
    if (event.target.id === 'recipientSearch') searchRecipients(event.target.value);
    if (event.target.id === 'transactionSearch') { state.transactionFilter = event.target.value; state.transactionPage = 1; refreshTransactionTable(); }
    if (event.target.id === 'loanForm') return;
    if (event.target.name === 'amount' && event.target.closest('#loanForm')) updateLoanEstimate();
  });
  $('#customerPage').addEventListener('change', event => {
    if (event.target.name === 'sourceAccountId') updateTransferCurrency();
    if (event.target.id === 'transactionType') { state.transactionType = event.target.value; state.transactionPage = 1; refreshTransactionTable(); }
    if (event.target.id === 'transactionStatus') { state.transactionStatus = event.target.value; state.transactionPage = 1; refreshTransactionTable(); }
    if (event.target.id === 'transactionSort') { state.transactionSort = event.target.value; state.transactionPage = 1; refreshTransactionTable(); }
    if (event.target.id === 'transactionAccount') { state.accountFilter = event.target.value; state.transactionPage = 1; refreshTransactionTable(); }
    if (event.target.id === 'transactionFrom' || event.target.id === 'transactionTo') refreshTransactionTable();
    if (event.target.id === 'analyticsCurrency') { state.analyticsCurrency = event.target.value; renderPage('analytics'); }
    if (event.target.id === 'analyticsPeriod') { state.analyticsMonths = Number(event.target.value); renderPage('analytics'); }
    if (event.target.name === 'termMonths') updateLoanEstimate();
  });
  $('#customerPage').addEventListener('submit', event => {
    const form = event.target;
    if (form.id === 'transferForm') { event.preventDefault(); reviewTransfer(form); }
    if (form.id === 'cardRequestForm') { event.preventDefault(); submitCardRequest(form); }
    if (form.id === 'loanForm') { event.preventDefault(); submitLoan(form); }
    if (form.id === 'supportForm') { event.preventDefault(); submitSupport(form); }
    if (form.id === 'profileForm') { event.preventDefault(); updateProfile(form); }
  });

  function updateTransferCurrency() {
    const sourceId = $('#transferForm [name="sourceAccountId"]')?.value;
    const source = state.accounts.find(a => a.id === sourceId);
    const select = $('#recipientAccount');
    const target = select && [...select.options].find(o => o.value === select.value);
    $('#transferCurrency').value = target?.dataset.currency || source?.currency || 'EUR';
  }
  async function searchRecipients(value) {
    clearTimeout(state.recipientTimer); state.selectedRecipient = null; $('#selectedRecipient').hidden = true; $('#recipientAccount').disabled = true;
    if (value.trim().length < 2) { $('#recipientResults').hidden = true; return; }
    $('#recipientResults').hidden = false; $('#recipientResults').innerHTML = '<div class="inline-message">Searching registered customers…</div>';
    state.recipientTimer = setTimeout(async () => {
      try {
        const results = await api(`/api/customer/recipients?q=${encodeURIComponent(value.trim())}`);
        if (value.trim() !== $('#recipientSearch').value.trim()) return;
        $('#recipientResults').innerHTML = results.length ? results.map(r => `<button class="recipient-option" type="button" data-recipient-id="${esc(r.id)}"><span><strong>${esc(r.firstName)} ${esc(r.lastName)}</strong><small>${esc(r.maskedEmail)}</small></span><span>Choose →</span></button>`).join('') : '<div class="inline-message">No eligible registered customers found.</div>';
        $('#recipientResults').dataset.recipients = JSON.stringify(results);
      } catch (error) { $('#recipientResults').innerHTML = `<div class="inline-message">${esc(error.message)}</div>`; }
    }, 260);
  }
  $('#customerPage').addEventListener('click', event => {
    if (event.target.matches('[data-tx-page]')) { state.transactionPage = Number(event.target.dataset.txPage); refreshTransactionTable(); return; }
    const option = event.target.closest('[data-recipient-id]'); if (!option) return;
    const recipients = JSON.parse($('#recipientResults').dataset.recipients || '[]'); state.selectedRecipient = recipients.find(r => r.id === option.dataset.recipientId);
    if (!state.selectedRecipient) return;
    $('#recipientSearch').value = `${state.selectedRecipient.firstName} ${state.selectedRecipient.lastName}`;
    $('#selectedRecipient').innerHTML = `<span><strong>${esc(state.selectedRecipient.firstName)} ${esc(state.selectedRecipient.lastName)}</strong><br><small>${esc(state.selectedRecipient.maskedEmail)}</small></span><button class="text-button" id="changeRecipient" type="button">Change</button>`; $('#selectedRecipient').hidden = false;
    $('#recipientResults').hidden = true;
    const source = state.accounts.find(a => a.id === $('#transferForm [name="sourceAccountId"]').value);
    const options = state.selectedRecipient.accounts.filter(a => !source || a.currency === source.currency);
    $('#recipientAccount').innerHTML = options.length ? options.map(a => `<option data-currency="${esc(a.currency)}" value="${esc(a.id)}">${esc(a.currency)} · account ending ${esc(a.maskedAccount.slice(-4))}</option>`).join('') : '<option value="">No account accepts this currency</option>';
    $('#recipientAccount').disabled = !options.length;
    updateTransferCurrency();
  });
  function step(n) { [1,2,3].forEach(i => $(`#transferStep${i}`)?.classList.toggle('active', i === n)); }
  function reviewTransfer(form) {
    const data = new FormData(form); const source = state.accounts.find(a => a.id === data.get('sourceAccountId'));
    const target = state.selectedRecipient?.accounts.find(a => a.id === data.get('targetAccountId'));
    if (!source || !target || !state.selectedRecipient) return $('#transferError').textContent = 'Choose an eligible recipient and destination account.';
    const amount = Number(data.get('amount'));
    if (!Number.isFinite(amount) || amount <= 0) return $('#transferError').textContent = 'Enter a valid amount greater than zero.';
    if (amount > Number(source.balance)) return $('#transferError').textContent = 'The amount exceeds the available balance.';
    state.transferPayload = { sourceAccountId: source.id, targetAccountId: target.id, amount: data.get('amount'), description: String(data.get('description') || '').trim(), idempotencyKey: crypto.randomUUID() };
    $('#transferReviewPanel').innerHTML = `<div class="section-title-row"><div><span class="overline">CHECK BEFORE SENDING</span><h2>Review transfer</h2></div></div><div class="review-row"><span>From account</span><strong>${esc(source.type)} · ${esc(source.accountNumber.slice(-4))}</strong></div><div class="review-row"><span>To</span><strong>${esc(state.selectedRecipient.firstName)} ${esc(state.selectedRecipient.lastName)}</strong></div><div class="review-row"><span>Recipient account</span><strong>${esc(target.maskedAccount)}</strong></div><div class="review-row"><span>Amount</span><strong>${esc(fmtMoney(amount,source.currency))}</strong></div><div class="review-row"><span>Fee</span><strong>${esc(fmtMoney(0,source.currency))}</strong></div><div class="review-row"><span>Description</span><strong>${esc(state.transferPayload.description || '—')}</strong></div><div class="form-actions-customer"><button id="transferCancel" class="button-customer" type="button">Edit details</button><button id="transferConfirm" class="button-customer primary" type="button">Confirm transfer</button></div>`;
    $('#transferFormPanel').hidden = true; $('#transferReviewPanel').hidden = false; step(2);
  }
  async function submitTransfer(button) {
    if (!state.transferPayload || button.disabled) return;
    button.disabled = true; button.textContent = 'Sending securely…';
    try {
      const transfer = await api('/api/customer/transfers', { method: 'POST', body: JSON.stringify(state.transferPayload) });
      state.transferPayload = null; step(3);
      $('#transferReviewPanel').hidden = true; $('#transferSuccessPanel').hidden = false;
      $('#transferSuccessPanel').innerHTML = `<div class="success-mark">✓</div><span class="overline">TRANSFER COMPLETE</span><h2>Money sent successfully</h2><p class="auth-subtitle">Your simulated transfer has been recorded.</p><div class="review-row"><span>Amount</span><strong>${esc(fmtMoney(transfer.amount,transfer.currency))}</strong></div><div class="review-row"><span>Reference</span><strong>${esc(transfer.reference)}</strong></div><div class="review-row"><span>Recipient</span><strong>${esc(state.selectedRecipient.firstName)} ${esc(state.selectedRecipient.lastName)}</strong></div><div class="review-row"><span>Sent</span><strong>${esc(fmtDate(transfer.occurredAt,true))}</strong></div><div class="form-actions-customer"><button class="button-customer" data-page="transactions">View transactions</button><button class="button-customer primary" data-page="dashboard">Return to overview</button></div>`;
      await loadBase(); toast('Transfer completed successfully.');
    } catch (error) { button.disabled = false; button.textContent = 'Try again'; toast(error.message, 'error'); }
  }
  async function submitCardRequest(form) {
    const button = $('button[type="submit"]', form); button.disabled = true;
    try { const data = Object.fromEntries(new FormData(form)); await api('/api/customer/cards', { method: 'POST', body: JSON.stringify(data) }); $('#cardRequestDialog').close(); toast('Card request submitted.'); renderPage('cards'); }
    catch (error) { $('#cardRequestError').textContent = error.message; button.disabled = false; }
  }
  async function updateCard(id, status) {
    if (status === 'BLOCKED' && !confirm('Permanently block this card? This cannot be undone.')) return;
    try { await api(`/api/customer/cards/${encodeURIComponent(id)}`, { method: 'PATCH', body: JSON.stringify({ status }) }); toast(status === 'BLOCKED' ? 'Card blocked.' : status === 'FROZEN' ? 'Card frozen.' : 'Card is active.'); renderPage('cards'); }
    catch (error) { toast(error.message, 'error'); }
  }
  async function updateAccountStatus(button) {
    const { accountId: id, accountAction: status } = button.dataset;
    if (status === 'FROZEN' && !confirm('Freeze this account? Deposits, withdrawals, and incoming or outgoing transfers will be unavailable until you unfreeze it.')) return;
    button.disabled = true;
    try {
      await api(`/api/customer/accounts/${encodeURIComponent(id)}/status`, { method: 'PATCH', body: JSON.stringify({ status }) });
      toast(status === 'FROZEN' ? 'Account frozen.' : 'Account unfrozen.');
      renderPage('accounts');
    } catch (error) {
      button.disabled = false;
      toast(error.message, 'error');
    }
  }
  async function submitMembership(tier) { try { await api('/api/customer/membership-requests', { method: 'POST', body: JSON.stringify({ tier }) }); toast('Membership upgrade request sent for review.'); renderPage('card-tiers'); } catch (error) { toast(error.message, 'error'); } }
  function updateLoanEstimate() { const form = $('#loanForm'); if (!form) return; const amount = Number(form.elements.amount.value); const months = Number(form.elements.termMonths.value); $('#loanEstimate').textContent = amount > 0 ? `${fmtMoney(monthlyPayment(amount,months))} / month` : '—'; }
  async function submitLoan(form) { const button = $('button[type="submit"]', form); button.disabled = true; try { const data = Object.fromEntries(new FormData(form)); data.amount = Number(data.amount); data.termMonths = Number(data.termMonths); await api('/api/customer/loans', { method: 'POST', body: JSON.stringify(data) }); $('#loanDialog').close(); toast('Loan application submitted for review.'); renderPage('loans'); } catch (error) { $('#loanError').textContent = error.message; button.disabled = false; } }
  async function submitSupport(form) { const button = $('button[type="submit"]', form); button.disabled = true; try { const data = Object.fromEntries(new FormData(form)); await api('/api/customer/support', { method: 'POST', body: JSON.stringify(data) }); $('#supportDialog').close(); toast('Support request sent.'); renderPage('support'); } catch (error) { $('#supportError').textContent = error.message; button.disabled = false; } }
  async function updateProfile(form) { const button = $('button[type="submit"]', form); button.disabled = true; try { await api('/api/customer/me', { method: 'PATCH', body: JSON.stringify({ phone: form.elements.phone.value }) }); toast('Phone number updated.'); renderPage('profile'); } catch (error) { $('#profileError').textContent = error.message; button.disabled = false; } }
  function refreshTransactionTable() { const results = $('#transactionResults'); if (!results) return; const filtered = applyTransactionFilters(); const pageSize = 25; const pages = Math.max(1, Math.ceil(filtered.length / pageSize)); state.transactionPage = Math.min(state.transactionPage, pages); results.innerHTML = transactionRows(filtered.slice((state.transactionPage - 1) * pageSize, state.transactionPage * pageSize)); $('#transactionCount').innerHTML = `<span>${filtered.length} of ${state.transactions.length} transactions · Page ${state.transactionPage} of ${pages}</span><span><button class="button-customer small" data-tx-page="${Math.max(1,state.transactionPage-1)}" ${state.transactionPage===1?'disabled':''}>Previous</button><button class="button-customer small" data-tx-page="${Math.min(pages,state.transactionPage+1)}" ${state.transactionPage===pages?'disabled':''}>Next</button></span>`; }
  async function showTransaction(id) { try { const tx = await api(`/api/customer/transactions/${encodeURIComponent(id)}`); const amount = `${tx.incoming ? '+' : '−'}${fmtMoney(tx.amount,tx.currency)}`; $('#transactionDialogBody').innerHTML = `<span class="overline">TRANSACTION DETAILS</span><h2>${esc(tx.description || tx.type.replaceAll('_',' '))}</h2><div class="transaction-amount-large ${tx.incoming ? 'amount-in' : 'amount-out'}">${esc(amount)}</div><div class="review-row"><span>Reference</span><strong>${esc(tx.reference)}</strong></div><div class="review-row"><span>Date and time</span><strong>${esc(fmtDate(tx.occurredAt,true))}</strong></div><div class="review-row"><span>Type</span><strong>${esc(tx.type.replaceAll('_',' '))}</strong></div><div class="review-row"><span>Account</span><strong>${esc(tx.accountLastFour || '—')}</strong></div><div class="review-row"><span>Counterparty</span><strong>${esc(tx.otherParty || 'NordBank')}</strong></div><div class="review-row"><span>Status</span><strong>${statusTag(tx.status)}</strong></div>`; $('#transactionDialog').showModal(); } catch (error) { toast(error.message,'error'); } }
  async function showSupport(id) { try { const r = await api(`/api/customer/support/${encodeURIComponent(id)}`); $('#transactionDialogBody').innerHTML = `<span class="overline">CUSTOMER SUPPORT</span><h2>${esc(r.subject)}</h2><div class="review-row"><span>Request</span><strong>${esc(r.id.slice(0,8).toUpperCase())}</strong></div><div class="review-row"><span>Category</span><strong>${esc(r.category)}</strong></div><div class="review-row"><span>Status</span><strong>${statusTag(r.status)}</strong></div><section class="section-block"><span class="overline">YOUR MESSAGE</span><p>${esc(r.message)}</p></section><section class="section-block"><span class="overline">NORDBANK RESPONSE</span><p>${esc(r.employeeResponse || 'Our team has not responded yet.')}</p></section>`; $('#transactionDialog').showModal(); } catch (error) { toast(error.message,'error'); } }
  function start() { renderPage('dashboard'); }
  start();
})();
