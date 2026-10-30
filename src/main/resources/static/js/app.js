(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const state = {
    csrf: null, clients: [], client: null, accounts: [], account: null, accountId: null, accountNeedsRefresh: false, employees: [],
    searchTimer: null, searchRequestId: 0, profileRequestId: 0, accountRequestId: 0,
    employeesRequestId: 0, overviewRequestId: 0, toastTimer: null
  };

  const escapeHtml = (value = '') => String(value).replace(/[&<>"']/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[char]);

  function initials(firstName = '', lastName = '') {
    return `${firstName.trim().charAt(0)}${lastName.trim().charAt(0)}`.toUpperCase() || 'CL';
  }

  function formatMoney(amount, currency) {
    try {
      return new Intl.NumberFormat('en-GB', { style: 'currency', currency, minimumFractionDigits: 2 }).format(Number(amount));
    } catch {
      return `${Number(amount).toFixed(2)} ${currency || ''}`.trim();
    }
  }

  function formatDate(value, withTime = false) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Date unavailable';
    return new Intl.DateTimeFormat('en-GB', withTime
      ? { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', timeZone: 'Europe/Kyiv' }
      : { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'Europe/Kyiv' }).format(date);
  }

  async function refreshCsrf() {
    const response = await fetch('/csrf', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('Could not establish a secure session. Please sign in again.');
    state.csrf = await response.json();
    const logoutInput = $('#logoutCsrf');
    if (logoutInput) {
      logoutInput.name = state.csrf.parameterName;
      logoutInput.value = state.csrf.token;
    }
  }

  async function api(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = new Headers(options.headers || {});
    headers.set('Accept', 'application/json');
    if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');

    if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
      if (!state.csrf) await refreshCsrf();
      headers.set(state.csrf.headerName, state.csrf.token);
    }

    const response = await fetch(path, { ...options, method, headers, credentials: 'same-origin' });
    if (response.redirected && new URL(response.url).pathname.startsWith('/login')) {
      window.location.assign('/login');
      throw new Error('Your session expired. Sign in again to continue.');
    }

    const contentType = response.headers.get('content-type') || '';
    const payload = contentType.includes('application/json') ? await response.json() : null;
    if (!response.ok) {
      if (response.status === 401) {
        window.location.assign('/login');
        throw new Error('Your session expired. Sign in again to continue.');
      }
      throw new Error(payload?.message || `Request failed (${response.status})`);
    }
    return payload;
  }

  function showToast(message, error = false) {
    const toast = $('#toast');
    $('#toastMessage').textContent = message;
    toast.classList.toggle('error', error);
    toast.classList.add('show');
    clearTimeout(state.toastTimer);
    state.toastTimer = setTimeout(() => toast.classList.remove('show'), 3400);
  }

  function setBusy(button, busy, label = 'Working…') {
    if (busy) {
      if (button.disabled) button.dataset.wasDisabled = 'true';
      else delete button.dataset.wasDisabled;
      button.dataset.originalText = button.innerHTML;
      button.disabled = true;
      button.setAttribute('aria-busy', 'true');
      button.innerHTML = `<span class="button-spinner"></span>${label}`;
    } else {
      button.disabled = button.dataset.wasDisabled === 'true';
      delete button.dataset.wasDisabled;
      button.removeAttribute('aria-busy');
      if (button.dataset.originalText) button.innerHTML = button.dataset.originalText;
    }
  }

  function updateClock() {
    $('#clock').textContent = new Intl.DateTimeFormat('en-GB', {
      hour: '2-digit', minute: '2-digit', timeZone: 'Europe/Kyiv', timeZoneName: 'short'
    }).format(new Date());
  }

  function statusClass(status) {
    if (status === 'BLOCKED') return 'blocked';
    if (status === 'REQUIRES_VERIFICATION') return 'review';
    return '';
  }

  function renderClients(clients, query) {
    state.clients = clients;
    $('#resultCount').textContent = clients.length ? String(clients.length).padStart(2, '0') : '';
    $('#navCount').textContent = clients.length ? String(clients.length).padStart(2, '0') : '—';
    $('#searchMeta').textContent = `Results for “${query}”`;
    if (!clients.length) {
      $('#clientResults').innerHTML = '<div class="empty-state"><span class="empty-mark"><i></i></span><strong>No matching clients</strong><p>Try another name, identification code, phone number, or email.</p></div>';
      return;
    }
    $('#clientResults').innerHTML = clients.map(client => `
      <button class="client-row" type="button" data-client-id="${escapeHtml(client.id)}" aria-label="Open ${escapeHtml(client.firstName)} ${escapeHtml(client.lastName)}">
        <span class="client-name-wrap"><span class="client-avatar">${escapeHtml(initials(client.firstName, client.lastName))}</span><span class="client-name-copy"><span class="client-name">${escapeHtml(client.firstName)} ${escapeHtml(client.lastName)}</span><small class="client-code">${escapeHtml(client.personalCode)}</small></span></span>
        <span class="contact-cell">${escapeHtml(client.phone || 'No phone')}<small>${escapeHtml(client.email || 'No email')}</small></span>
        <span class="status-badge ${statusClass(client.status)}">${escapeHtml((client.status || 'UNKNOWN').replaceAll('_', ' '))}</span>
      </button>`).join('');
  }

  function renderEmployees(employees) {
    state.employees = employees;
    $('#employeeCount').textContent = employees.length ? String(employees.length).padStart(2, '0') : '';
    if (!employees.length) {
      $('#employeeResults').innerHTML = '<div class="empty-state"><span class="empty-mark"></span><strong>No employees yet</strong><p>Add a staff account to give someone access to the workspace.</p></div>';
      return;
    }
    $('#employeeResults').innerHTML = employees.map(employee => {
      const status = employee.enabled ? 'Enabled' : 'Disabled';
      const action = employee.enabled ? 'Disable' : 'Enable';
      return `<div class="employee-row">
        <span class="employee-name-wrap"><span class="employee-avatar">${escapeHtml(employee.username.slice(0, 2).toUpperCase())}</span><span class="employee-name-copy"><strong>${escapeHtml(employee.username)}</strong><small class="employee-id">${escapeHtml(employee.id)}</small></span></span>
        <span class="employee-role">${escapeHtml(employee.role.charAt(0) + employee.role.slice(1).toLowerCase())}</span>
        <span class="status-badge ${employee.enabled ? '' : 'blocked'}">${status}</span>
        <span class="employee-created">${escapeHtml(formatDate(employee.createdAt))}</span>
        <span><button class="button button-quiet button-small employee-toggle ${employee.enabled ? 'employee-disable' : ''}" type="button" data-employee-toggle="${escapeHtml(employee.id)}">${action}</button></span>
      </div>`;
    }).join('');
  }

  async function loadEmployees() {
    const requestId = ++state.employeesRequestId;
    $('#employeeResults').innerHTML = '<div class="loading-shimmer"></div><div class="loading-shimmer"></div>';
    try {
      const employees = await api('/api/admin/employees');
      if (requestId !== state.employeesRequestId) return employees;
      renderEmployees(employees);
      return employees;
    } catch (error) {
      if (requestId !== state.employeesRequestId) return null;
      $('#employeeResults').innerHTML = `<div class="empty-state"><strong>Staff directory unavailable</strong><p>${escapeHtml(error.message)}</p></div>`;
      throw error;
    }
  }

  async function checkStaffAccess() {
    try {
      await loadEmployees();
      $('#staffNav').hidden = false;
    } catch {
      $('#staffNav').hidden = true;
    }
  }

  function showWorkspaceView(view) {
    const isStaff = view === 'staff';
    const isClients = view === 'clients';
    $('#overviewView').hidden = isStaff || isClients;
    $('#clientsView').hidden = isStaff || !isClients;
    $('#staffView').hidden = !isStaff;
    $('#breadcrumbPage').textContent = isStaff ? 'Team access' : isClients ? 'Clients' : 'Overview';
    $$('[data-nav]').forEach(item => item.classList.toggle('active', item.dataset.nav === view));
    if (isStaff) loadEmployees().catch(() => {});
    if (view === 'overview') loadOverview();
  }

  async function loadOverview() {
    const requestId = ++state.overviewRequestId;
    const metrics = $('#overviewMetrics');
    metrics.setAttribute('aria-busy', 'true');
    try {
      const clients = await api('/api/clients');
      const accountsByClient = await Promise.all(clients.map(client => api(`/api/clients/${encodeURIComponent(client.id)}/accounts`)));
      if (requestId !== state.overviewRequestId) return;
      const accounts = accountsByClient.flat();
      const openAccounts = accounts.filter(account => account.status === 'OPEN');
      const balances = openAccounts.reduce((totals, account) => {
        const currency = account.currency || '—';
        totals[currency] = (totals[currency] || 0) + Number(account.balance || 0);
        return totals;
      }, {});
      const balanceCopy = Object.entries(balances).length
        ? Object.entries(balances).sort(([a], [b]) => a.localeCompare(b)).map(([currency, amount]) => formatMoney(amount, currency)).join(' · ')
        : 'No open balances';
      metrics.innerHTML = `
        <article class="metric-card"><span class="metric-label">REGISTERED CLIENTS</span><strong class="metric-value">${clients.length.toLocaleString('en-GB')}</strong><span class="metric-note">${clients.filter(client => client.status === 'ACTIVE').length.toLocaleString('en-GB')} active relationships</span></article>
        <article class="metric-card"><span class="metric-label">OPEN ACCOUNTS</span><strong class="metric-value">${openAccounts.length.toLocaleString('en-GB')}</strong><span class="metric-note">${accounts.length.toLocaleString('en-GB')} accounts in total</span></article>
        <article class="metric-card metric-card-wide"><span class="metric-label">CLIENT BALANCES</span><strong class="metric-value metric-currency">${escapeHtml(balanceCopy)}</strong><span class="metric-note">Balances remain grouped by account currency</span></article>`;
      const latest = [...clients].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 5);
      $('#recentClients').innerHTML = latest.length ? latest.map(client => `
        <button class="client-row" type="button" data-client-id="${escapeHtml(client.id)}" aria-label="Open ${escapeHtml(client.firstName)} ${escapeHtml(client.lastName)}">
          <span class="client-name-wrap"><span class="client-avatar">${escapeHtml(initials(client.firstName, client.lastName))}</span><span class="client-name-copy"><span class="client-name">${escapeHtml(client.firstName)} ${escapeHtml(client.lastName)}</span><small class="client-code">${escapeHtml(formatDate(client.createdAt))}</small></span></span>
          <span class="contact-cell">${escapeHtml(client.phone || 'No phone')}<small>${escapeHtml(client.email || 'No email')}</small></span>
          <span class="status-badge ${statusClass(client.status)}">${escapeHtml((client.status || 'UNKNOWN').replaceAll('_', ' '))}</span>
        </button>`).join('') : '<div class="empty-state"><span class="empty-mark"></span><strong>No client records yet</strong><p>Add a client profile to begin using the workspace.</p></div>';
    } catch (error) {
      if (requestId !== state.overviewRequestId) return;
      metrics.innerHTML = `<div class="overview-error" role="status">Portfolio data could not be loaded. ${escapeHtml(error.message)}</div>`;
      $('#recentClients').innerHTML = `<div class="empty-state"><strong>Directory unavailable</strong><p>${escapeHtml(error.message)}</p></div>`;
    } finally {
      if (requestId === state.overviewRequestId) metrics.removeAttribute('aria-busy');
    }
  }

  async function searchClients() {
    clearTimeout(state.searchTimer);
    const query = $('#searchInput').value.trim();
    if (!query) {
      state.searchRequestId += 1;
      $('#searchInput').focus();
      $('#searchMeta').textContent = 'Enter a name or identifier to search';
      return;
    }
    const requestId = ++state.searchRequestId;
    $('#clientResults').innerHTML = '<div class="loading-shimmer"></div><div class="loading-shimmer"></div><div class="loading-shimmer"></div>';
    $('#searchMeta').textContent = 'Searching securely…';
    try {
      const clients = await api(`/api/clients?q=${encodeURIComponent(query)}`);
      if (requestId !== state.searchRequestId || query !== $('#searchInput').value.trim()) return;
      renderClients(clients, query);
    } catch (error) {
      if (requestId !== state.searchRequestId || query !== $('#searchInput').value.trim()) return;
      $('#clientResults').innerHTML = `<div class="empty-state"><strong>Search unavailable</strong><p>${escapeHtml(error.message)}</p></div>`;
      $('#searchMeta').textContent = 'Please try again';
    }
  }

  async function openProfile(clientId) {
    const requestId = ++state.profileRequestId;
    const dialog = $('#profileModal');
    state.client = null;
    state.accounts = [];
    $('#openAccountButton').disabled = true;
    $('#accountList').innerHTML = '<div class="loading-shimmer"></div><div class="loading-shimmer"></div>';
    if (!dialog.open) dialog.showModal();
    try {
      const [client, accounts] = await Promise.all([
        api(`/api/clients/${encodeURIComponent(clientId)}`),
        api(`/api/clients/${encodeURIComponent(clientId)}/accounts`)
      ]);
      if (requestId !== state.profileRequestId) return;
      state.client = client;
      state.accounts = accounts;
      $('#openAccountButton').disabled = false;
      $('#profileAvatar').textContent = initials(client.firstName, client.lastName);
      $('#profileName').textContent = `${client.firstName} ${client.lastName}`;
      $('#profileCode').textContent = `PERSONAL CODE  ·  ${client.personalCode}`;
      renderProfileMeta(client);
      renderAccounts(accounts);
    } catch (error) {
      if (requestId !== state.profileRequestId) return;
      $('#accountList').innerHTML = `<div class="no-accounts">${escapeHtml(error.message)}</div>`;
    }
  }

  function renderProfileMeta(client) {
    $('#profileMeta').innerHTML = `
      <div class="meta-item"><span>Phone</span><strong>${escapeHtml(client.phone || 'Not provided')}</strong></div>
      <div class="meta-item"><span>Email</span><strong>${escapeHtml(client.email || 'Not provided')}</strong></div>
      <div class="meta-item"><span>Client since</span><strong>${escapeHtml(formatDate(client.createdAt))}</strong></div>
      <div class="meta-item"><span>Profile status</span><strong>${escapeHtml((client.status || '').replaceAll('_', ' '))}</strong></div>
      ${client.verificationApprovedAt ? `<div class="meta-item"><span>Verified on</span><strong>${escapeHtml(formatDate(client.verificationApprovedAt, true))}</strong></div><div class="meta-item"><span>Verified by</span><strong>${escapeHtml(client.verificationApprovedBy || 'Staff')}</strong></div>` : ''}`;
    $('#verificationAction').hidden = client.status !== 'REQUIRES_VERIFICATION';
  }

  async function approveClientVerification() {
    const client = state.client;
    const button = $('#approveVerificationButton');
    if (!client || client.status !== 'REQUIRES_VERIFICATION' || button.disabled) return;
    button.disabled = true;
    button.textContent = 'Approving…';
    try {
      state.client = await api(`/api/clients/${encodeURIComponent(client.id)}/verification`, { method: 'PATCH' });
      renderProfileMeta(state.client);
      showToast('Client verification approved.');
      await loadOverview();
      if ($('#searchInput').value.trim()) await searchClients();
    } catch (error) {
      showToast(error.message, true);
    } finally {
      button.disabled = false;
      button.textContent = 'Approve verification';
    }
  }

  function renderAccounts(accounts) {
    $('#accountCount').textContent = accounts.length ? String(accounts.length).padStart(2, '0') : '';
    if (!accounts.length) {
      $('#accountList').innerHTML = '<div class="no-accounts">No accounts yet. Open the first account for this client.</div>';
      return;
    }
    $('#accountList').innerHTML = accounts.map(account => `
      <button class="account-card" type="button" data-account-id="${escapeHtml(account.id)}">
        <span class="account-card-top"><span class="account-type">${escapeHtml(account.type === 'CURRENT' ? 'Current account' : 'Savings account')}</span><span class="account-currency">${escapeHtml(account.currency)}</span></span>
        <span class="account-number">${escapeHtml(account.accountNumber)}</span>
        <span class="account-card-bottom"><span class="account-balance">${escapeHtml(formatMoney(account.balance, account.currency))}</span><span class="account-status ${account.status === 'OPEN' ? '' : 'closed'}">${escapeHtml(account.status === 'OPEN' ? 'Open' : 'Closed')}</span></span>
      </button>`).join('');
  }

  function refreshDestinationAccounts() {
    const field = $('#destinationField');
    const select = $('#targetAccountId');
    const isTransfer = $('#operationType').value === 'transfer';
    field.hidden = !isTransfer;
    select.required = isTransfer;
    $('#amountCurrency').textContent = state.account?.currency || '';
    const candidates = state.account
      ? state.accounts.filter(account => account.id !== state.account.id && account.currency === state.account.currency && account.status === 'OPEN')
      : [];
    if (isTransfer) {
      select.innerHTML = candidates.length
        ? candidates.map(account => `<option value="${escapeHtml(account.id)}">${escapeHtml(account.accountNumber)} · ${escapeHtml(account.type === 'CURRENT' ? 'Current account' : 'Savings account')} · ${escapeHtml(formatMoney(account.balance, account.currency))}</option>`).join('')
        : '<option value="">No eligible destination accounts</option>';
    }
    select.disabled = !isTransfer || candidates.length === 0;
    $('#operationForm button[type="submit"]').disabled = !state.account || state.accountNeedsRefresh || state.account.status !== 'OPEN' || (isTransfer && candidates.length === 0);
  }

  async function openAccountDetails(accountId) {
    const requestId = ++state.accountRequestId;
    const dialog = $('#accountModal');
    const accountChanged = state.accountId !== accountId;
    if (accountChanged) {
      $('#operationForm').reset();
      $('#operationType').value = 'deposit';
      $('#operationError').textContent = '';
    }
    state.accountId = accountId;
    state.account = null;
    state.accountNeedsRefresh = true;
    setBusy($('#refreshAccountButton'), true, 'Refreshing…');
    $('#operationForm').querySelectorAll('input, select, button[type="submit"]').forEach(control => { control.disabled = true; });
    $('#closeAccountButton').disabled = true;
    $('#accountHeading').textContent = 'Loading account…';
    $('#accountSummary').innerHTML = '<div class="loading-shimmer"></div>';
    $('#accountOpenedAt').textContent = '';
    $('#operationList').innerHTML = '<div class="loading-shimmer"></div><div class="loading-shimmer"></div>';
    if (!dialog.open) dialog.showModal();
    try {
      const [account, operations, accounts] = await Promise.all([
        api(`/api/accounts/${encodeURIComponent(accountId)}`),
        api(`/api/accounts/${encodeURIComponent(accountId)}/operations`),
        api(`/api/clients/${encodeURIComponent(state.client.id)}/accounts`)
      ]);
      if (requestId !== state.accountRequestId) return;
      state.account = account;
      state.accountId = account.id;
      state.accounts = accounts;
      state.accountNeedsRefresh = false;
      $('#accountHeading').textContent = account.type === 'CURRENT' ? 'Current account' : 'Savings account';
      $('#accountSummary').innerHTML = `
        <div><div class="summary-type">${escapeHtml(account.type === 'CURRENT' ? 'Current account' : 'Savings account')} · ${escapeHtml(account.status === 'OPEN' ? 'Open' : 'Closed')}</div><div class="summary-number">${escapeHtml(account.accountNumber)}</div></div>
        <div><div class="summary-balance-label">AVAILABLE BALANCE</div><div class="summary-balance">${escapeHtml(formatMoney(account.balance, account.currency))}</div></div>`;
      $('#accountOpenedAt').textContent = `OPENED ${formatDate(account.openedAt).toUpperCase()}`;
      renderOperations(operations, account);
      const canClose = account.status === 'OPEN' && Number(account.balance) === 0;
      $('#closeAccountButton').disabled = !canClose || state.accountNeedsRefresh;
      $('#closeAccountButton').title = canClose ? 'Close this zero-balance account' : 'Only open accounts with a zero balance can be closed';
      $('#operationForm').querySelectorAll('input, select, button[type="submit"]').forEach(control => { control.disabled = account.status !== 'OPEN'; });
      refreshDestinationAccounts();
      setBusy($('#refreshAccountButton'), false);
    } catch (error) {
      if (requestId !== state.accountRequestId) return;
      $('#accountHeading').textContent = 'Account unavailable';
      $('#accountSummary').innerHTML = `<div class="history-empty">${escapeHtml(error.message)}</div>`;
      $('#operationList').innerHTML = `<div class="history-empty">${escapeHtml(error.message)}</div>`;
      setBusy($('#refreshAccountButton'), false);
    }
  }

  function renderOperations(operations, account) {
    if (!operations.length) {
      $('#operationList').innerHTML = '<div class="history-empty">No activity recorded for this account.</div>';
      return;
    }
    $('#operationList').innerHTML = operations.slice(0, 12).map(operation => {
      const incoming = operation.type === 'DEPOSIT' || operation.targetAccountNumber === account.accountNumber;
      const title = operation.type === 'TRANSFER'
        ? (incoming ? `Transfer from ${operation.sourceAccountNumber}` : `Transfer to ${operation.targetAccountNumber}`)
        : operation.type === 'DEPOSIT' ? 'Deposit' : 'Withdrawal';
      const sign = incoming ? '+' : '−';
      const icon = operation.type === 'TRANSFER' ? '↗' : incoming ? '↓' : '↑';
      return `<div class="operation-item"><span class="operation-icon ${incoming ? 'in' : 'out'}">${icon}</span><span class="operation-copy"><strong>${escapeHtml(title)}</strong><small>${escapeHtml(formatDate(operation.occurredAt, true))}</small></span><span class="operation-amount ${incoming ? 'in' : ''}">${sign}${escapeHtml(formatMoney(operation.amount, operation.currency))}</span></div>`;
    }).join('');
  }

  function showDialog(id) {
    const dialog = document.getElementById(id);
    if (!dialog.open) dialog.showModal();
  }

  function closeDialog(id) {
    document.getElementById(id).close();
  }

  async function submitJsonForm(form, path, successMessage, onSuccess) {
    const button = $('button[type="submit"]', form);
    const errorNode = $('.form-error', form);
    errorNode.textContent = '';
    setBusy(button, true);
    try {
      const data = Object.fromEntries(new FormData(form).entries());
      const result = await api(path, { method: 'POST', body: JSON.stringify(data) });
      form.reset();
      onSuccess?.(result);
      showToast(successMessage);
    } catch (error) {
      errorNode.textContent = error.message;
    } finally {
      setBusy(button, false);
    }
  }

  $('#searchButton').addEventListener('click', searchClients);
  $('#searchInput').addEventListener('keydown', event => {
    if (event.key === 'Enter') { event.preventDefault(); searchClients(); }
  });
  $('#searchInput').addEventListener('input', () => {
    clearTimeout(state.searchTimer);
    state.searchRequestId += 1;
    const query = $('#searchInput').value.trim();
    if (query.length >= 2) {
      $('#clientResults').innerHTML = '<div class="empty-state search-empty"><strong>Updating results…</strong><p>Searching for the latest matches.</p></div>';
      $('#resultCount').textContent = '';
      $('#navCount').textContent = '—';
      $('#searchMeta').textContent = 'Preparing search';
      state.searchTimer = setTimeout(searchClients, 420);
    }
    else {
      $('#clientResults').innerHTML = query
        ? '<div class="empty-state search-empty"><strong>Keep typing to search</strong><p>Enter at least two characters, or press Enter to search now.</p></div>'
        : '<div class="empty-state search-empty"><span class="empty-mark"><i></i></span><strong>Start with a search</strong><p>Search by name, personal code, phone number, or email to open a client profile.</p></div>';
      $('#resultCount').textContent = '';
      $('#navCount').textContent = '—';
      $('#searchMeta').textContent = query ? 'Enter at least two characters' : '';
    }
  });
  $('#newClientButton').addEventListener('click', () => {
    $('#clientForm').reset();
    $('#clientFormError').textContent = '';
    showDialog('clientModal');
  });
  $('#clientResults').addEventListener('click', event => {
    const row = event.target.closest('[data-client-id]');
    if (row) openProfile(row.dataset.clientId);
  });
  $('#recentClients').addEventListener('click', event => {
    const row = event.target.closest('[data-client-id]');
    if (row) openProfile(row.dataset.clientId);
  });
  $('#accountList').addEventListener('click', event => {
    const card = event.target.closest('[data-account-id]');
    if (card) openAccountDetails(card.dataset.accountId);
  });
  $('#approveVerificationButton').addEventListener('click', approveClientVerification);
  $('#openAccountButton').addEventListener('click', () => {
    if (!state.client) return;
    $('#accountFormError').textContent = '';
    showDialog('openAccountModal');
  });
  $('#operationType').addEventListener('change', refreshDestinationAccounts);
  $('#refreshAccountButton').addEventListener('click', () => {
    if (state.accountId && state.client) openAccountDetails(state.accountId);
  });
  $$('[data-close]').forEach(button => button.addEventListener('click', () => closeDialog(button.dataset.close)));
  $$('.modal').forEach(dialog => dialog.addEventListener('click', event => {
    if (event.target === dialog) dialog.close();
  }));
  $('#accountModal').addEventListener('close', () => {
    $('#operationForm').reset();
    $('#operationType').value = 'deposit';
    $('#operationError').textContent = '';
  });
  $$('.nav-item').forEach(item => item.addEventListener('click', event => {
    if (!item.dataset.nav) return;
    event.preventDefault();
    const view = item.dataset.nav;
    if (view === 'staff' && $('#staffNav').hidden) return;
    showWorkspaceView(view);
  }));
  $$('[data-open-clients]').forEach(button => button.addEventListener('click', () => showWorkspaceView('clients')));
  $('#overviewNewClient').addEventListener('click', () => {
    showWorkspaceView('clients');
    $('#clientForm').reset();
    $('#clientFormError').textContent = '';
    showDialog('clientModal');
  });
  $('#refreshOverview').addEventListener('click', () => loadOverview());
  $('#profileMenuButton').addEventListener('click', () => {
    showDialog('profileMenuDialog');
    $('#profileMenuButton').setAttribute('aria-expanded', 'true');
  });
  $('#profileMenuDialog').addEventListener('close', () => $('#profileMenuButton').setAttribute('aria-expanded', 'false'));
  $('#profileSignOutButton').addEventListener('click', () => {
    const button = $('#profileSignOutButton');
    button.disabled = true;
    button.querySelector('strong').textContent = 'Signing out...';
    (async () => {
      try {
        if (!state.csrf) await refreshCsrf();
        closeDialog('profileMenuDialog');
        $('#logoutForm').requestSubmit();
      } catch (error) {
        button.disabled = false;
        button.querySelector('strong').textContent = 'Sign out';
        showToast(error.message || 'Could not sign out. Please try again.', true);
      }
    })();
  });

  $('#newEmployeeButton').addEventListener('click', () => {
    $('#employeeForm').reset();
    $('#employeeFormError').textContent = '';
    showDialog('employeeModal');
  });
  $('#refreshEmployeesButton').addEventListener('click', () => loadEmployees().catch(error => showToast(error.message, true)));
  $('#employeeForm').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const button = $('button[type="submit"]', form);
    $('#employeeFormError').textContent = '';
    setBusy(button, true, 'Creating…');
    try {
      const data = Object.fromEntries(new FormData(form).entries());
      data.username = data.username.trim();
      await api('/api/admin/employees', { method: 'POST', body: JSON.stringify(data) });
      closeDialog('employeeModal');
      form.reset();
      showToast('Employee account created');
      try {
        await loadEmployees();
      } catch {
        showToast('Employee created, but the staff list could not be refreshed.', true);
      }
    } catch (error) {
      $('#employeeFormError').textContent = error.message;
    } finally {
      setBusy(button, false);
    }
  });
  $('#employeeResults').addEventListener('click', async event => {
    const button = event.target.closest('[data-employee-toggle]');
    if (!button) return;
    const employee = state.employees.find(item => item.id === button.dataset.employeeToggle);
    if (!employee) return;
    const enabled = !employee.enabled;
    const verb = enabled ? 'enable' : 'disable';
    if (!confirm(`Are you sure you want to ${verb} access for ${employee.username}?`)) return;
    setBusy(button, true, enabled ? 'Enabling…' : 'Disabling…');
    try {
      await api(`/api/admin/employees/${encodeURIComponent(employee.id)}/enabled`, {
        method: 'PUT',
        body: JSON.stringify({ enabled })
      });
      showToast(`Employee access ${enabled ? 'enabled' : 'disabled'}`);
      try {
        await loadEmployees();
      } catch {
        showToast('Access updated, but the staff list could not be refreshed.', true);
      }
    } catch (error) {
      showToast(error.message, true);
      await loadEmployees().catch(() => {});
    } finally {
      setBusy(button, false);
    }
  });

  $('#clientForm').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const button = $('button[type="submit"]', form);
    $('#clientFormError').textContent = '';
    setBusy(button, true, 'Creating…');
    try {
      const body = Object.fromEntries(new FormData(form).entries());
      const client = await api('/api/clients', { method: 'POST', body: JSON.stringify(body) });
      closeDialog('clientModal');
      form.reset();
      showToast('Client profile created');
      $('#searchInput').value = client.personalCode;
      searchClients();
      openProfile(client.id);
    } catch (error) {
      $('#clientFormError').textContent = error.message;
    } finally {
      setBusy(button, false);
    }
  });

  $('#openAccountForm').addEventListener('submit', async event => {
    event.preventDefault();
    if (!state.client) return;
    const form = event.currentTarget;
    const button = $('button[type="submit"]', form);
    $('#accountFormError').textContent = '';
    setBusy(button, true, 'Opening…');
    try {
      const data = Object.fromEntries(new FormData(form).entries());
      const account = await api(`/api/clients/${encodeURIComponent(state.client.id)}/accounts`, { method: 'POST', body: JSON.stringify(data) });
      closeDialog('openAccountModal');
      form.reset();
      state.accounts = [account, ...state.accounts];
      renderAccounts(state.accounts);
      showToast('Account opened');
      try {
        const accounts = await api(`/api/clients/${encodeURIComponent(state.client.id)}/accounts`);
        state.accounts = accounts;
        renderAccounts(accounts);
      } catch {
        showToast('Account opened, but the account list could not be refreshed.', true);
      }
    } catch (error) {
      $('#accountFormError').textContent = error.message;
    } finally {
      setBusy(button, false);
    }
  });

  $('#operationForm').addEventListener('submit', async event => {
    event.preventDefault();
    if (!state.account) return;
    const form = event.currentTarget;
    const button = $('button[type="submit"]', form);
    const errorNode = $('#operationError');
    errorNode.textContent = '';
    setBusy(button, true, 'Processing…');
    try {
      const formData = new FormData(form);
      const type = formData.get('type');
      const amount = formData.get('amount');
      let path;
      let body;
      if (type === 'transfer') {
        path = `/api/accounts/${encodeURIComponent(state.account.id)}/transfers`;
        body = { targetAccountId: formData.get('targetAccountId'), amount };
      } else {
        path = `/api/accounts/${encodeURIComponent(state.account.id)}/${type === 'deposit' ? 'deposits' : 'withdrawals'}`;
        body = { amount };
      }
      await api(path, { method: 'POST', body: JSON.stringify(body) });
      form.reset();
      $('#operationType').value = 'deposit';
      $('#operationError').textContent = '';
      showToast(type === 'transfer' ? 'Transfer completed' : type === 'deposit' ? 'Deposit recorded' : 'Withdrawal completed');
      try {
        const [account, operations, accounts] = await Promise.all([
          api(`/api/accounts/${encodeURIComponent(state.account.id)}`),
          api(`/api/accounts/${encodeURIComponent(state.account.id)}/operations`),
          api(`/api/clients/${encodeURIComponent(state.client.id)}/accounts`)
        ]);
        state.account = account;
        state.accounts = accounts;
        state.accountNeedsRefresh = false;
        $('#accountSummary').innerHTML = `
          <div><div class="summary-type">${escapeHtml(account.type === 'CURRENT' ? 'Current account' : 'Savings account')} · ${escapeHtml(account.status === 'OPEN' ? 'Open' : 'Closed')}</div><div class="summary-number">${escapeHtml(account.accountNumber)}</div></div>
          <div><div class="summary-balance-label">AVAILABLE BALANCE</div><div class="summary-balance">${escapeHtml(formatMoney(account.balance, account.currency))}</div></div>`;
        renderOperations(operations, account);
        renderAccounts(accounts);
        const canClose = account.status === 'OPEN' && Number(account.balance) === 0;
        $('#closeAccountButton').disabled = !canClose || state.accountNeedsRefresh;
        form.querySelectorAll('input, select, button[type="submit"]').forEach(control => { control.disabled = account.status !== 'OPEN'; });
        refreshDestinationAccounts();
      } catch {
        state.accountNeedsRefresh = true;
        $('#closeAccountButton').disabled = true;
        showToast('Operation completed, but account details could not be refreshed.', true);
      }
    } catch (error) {
      errorNode.textContent = error.message;
    } finally {
      setBusy(button, false);
      refreshDestinationAccounts();
    }
  });

  $('#closeAccountButton').addEventListener('click', async () => {
    if (!state.account || !confirm(`Close account ${state.account.accountNumber}? This action cannot be undone.`)) return;
    const button = $('#closeAccountButton');
    setBusy(button, true, 'Closing…');
    try {
      const closedAccount = await api(`/api/accounts/${encodeURIComponent(state.account.id)}/close`, { method: 'POST' });
      state.account = closedAccount;
      state.accounts = state.accounts.map(account => account.id === closedAccount.id ? closedAccount : account);
      renderAccounts(state.accounts);
      showToast('Account closed');
      closeDialog('accountModal');
      try {
        const accounts = await api(`/api/clients/${encodeURIComponent(state.client.id)}/accounts`);
        state.accounts = accounts;
        renderAccounts(accounts);
      } catch {
        showToast('Account closed, but the account list could not be refreshed.', true);
      }
    } catch (error) {
      showToast(error.message, true);
    } finally {
      setBusy(button, false);
    }
  });

  refreshCsrf().catch(() => {});
  checkStaffAccess();
  showWorkspaceView('overview');
  updateClock();
  setInterval(updateClock, 30_000);
})();
