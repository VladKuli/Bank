(() => {
    const $ = (s, root = document) => root.querySelector(s);
    const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[c]));
    let csrf;
    let rows = [];
    let toastTimer;
    const section = location.pathname.split('/').filter(Boolean).at(-1) || 'cards';
    const labels = {
        cards: 'Card requests',
        memberships: 'Membership reviews',
        loans: 'Loan applications',
        requests: 'Customer support',
        transfers: 'Internal transfers'
    };
    const paths = {
        cards: '/api/employee/card-requests',
        memberships: '/api/employee/membership-requests',
        loans: '/api/employee/loan-applications',
        requests: '/api/employee/support-requests',
        transfers: '/api/employee/transfers'
    };
    const fmtDate = v => v ? new Intl.DateTimeFormat('en-GB', {
        dateStyle: 'medium',
        timeStyle: 'short'
    }).format(new Date(v)) : '—';

    async function getCsrf() {
        const r = await fetch('/csrf', {credentials: 'same-origin'});
        if (!r.ok) throw new Error('Could not refresh secure session.');
        csrf = await r.json();
    }

    async function api(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = new Headers({Accept: 'application/json', ...(options.headers || {})});
        if (options.body) headers.set('Content-Type', 'application/json');
        if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
            if (!csrf) await getCsrf();
            headers.set(csrf.headerName, csrf.token)
        }
        const r = await fetch(url, {...options, method, headers, credentials: 'same-origin'});
        const type = r.headers.get('content-type') || '';
        const p = type.includes('json') ? await r.json() : null;
        if (r.status === 401) {
            location.assign('/login/employee');
            throw new Error('Sign in again.')
        }
        if (!r.ok) throw new Error(p?.detail || p?.message || `Request failed (${r.status})`);
        return p;
    }

    function toast(message, error = false) {
        const n = $('#workflowToast');
        n.textContent = message;
        n.className = `customer-toast show ${error ? 'error' : 'success'}`;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => n.classList.remove('show'), 3300)
    }

    function heading(copy, count) {
        return `<div class="page-heading-customer"><div><span class="overline">NORDBANK OPERATIONS</span><h1>${esc(labels[section])}</h1><p>${esc(copy)}</p></div><span class="workflow-count">${count} ${count === 1 ? 'ITEM' : 'ITEMS'}</span></div>`
    }

    function empty() {
        return '<div class="workflow-empty">There are no items in this queue.</div>'
    }

    async function load() {
        $('#workflowTitle').textContent = labels[section] || 'Operations';
        document.querySelectorAll('[data-workflow]').forEach(a => a.classList.toggle('selected', a.dataset.workflow === section));
        $('#workflowPage').innerHTML = '<div class="page-loading"><span></span>Loading queue…</div>';
        try {
            rows = await api(paths[section]);
            $('#workflowPage').innerHTML = section === 'transfers' ? renderTransfers(rows) : section === 'requests' ? renderSupport(rows) : renderReviews(rows);
        } catch (error) {
            $('#workflowPage').innerHTML = `${heading('', 0)}<div class="workflow-error">${esc(error.message)}</div>`;
        }
    }

    function renderReviews(items) {
        const copy = {
            cards: 'Review simulated card issuance requests. Premium and VIP cards are issued only after approval.',
            memberships: 'Review membership upgrade requests. Approved changes update the customer profile immediately.',
            loans: 'Review illustrative loan applications. Approval records a decision and does not disburse funds.'
        }[section];
        return `${heading(copy, items.length)}${items.length ? `<div class="review-queue">${items.map(item => `<article class="review-item"><div><strong>${esc(item.customerName)}</strong><small>${esc(item.customerEmail || 'Customer')} · ${esc(fmtDate(item.requestedAt))}</small></div><div><strong>${esc(item.requestedValue || item.tier || 'Request')}</strong><small>${esc(item.detail || '')}</small></div><p>${esc(item.status)}</p><div class="review-actions"><button class="button-customer small primary" data-decision="APPROVED" data-id="${esc(item.id)}">Approve</button><button class="button-customer small danger" data-decision="REJECTED" data-id="${esc(item.id)}">Reject</button></div></article>`).join('')}</div>` : empty()}`;
    }

    function renderSupport(items) {
        return `${heading('Respond to customer requests and keep the case status up to date.', items.length)}${items.length ? items.map(item => `<article class="support-item"><div class="review-item" style="padding:0;border:0;background:transparent"><div><strong>${esc(item.subject)}</strong><small>${esc(item.category)} · ${esc(fmtDate(item.createdAt))} · ${esc(item.id.slice(0, 8).toUpperCase())}</small></div><div><strong>${esc(item.status.replaceAll('_', ' '))}</strong><small>Customer: ${esc(item.ownerName || 'Customer')}</small></div><p>${esc(item.message)}</p></div>${item.employeeResponse ? `<p class="inline-message"><strong>Previous response:</strong> ${esc(item.employeeResponse)}</p>` : ''}<form class="support-update" data-support-form="${esc(item.id)}"><textarea name="response" maxlength="5000" required placeholder="Write a response visible to the customer">${esc(item.employeeResponse || '')}</textarea><select name="status"><option value="IN_PROGRESS" ${item.status === 'IN_PROGRESS' ? 'selected' : ''}>In progress</option><option value="WAITING_FOR_CUSTOMER" ${item.status === 'WAITING_FOR_CUSTOMER' ? 'selected' : ''}>Waiting for customer</option><option value="RESOLVED" ${item.status === 'RESOLVED' ? 'selected' : ''}>Resolved</option></select><span></span><button class="button-customer primary small" type="submit">Save response</button></form></article>`).join('') : empty()}`;
    }

    function renderTransfers(items) {
        return `${heading('Search the shared ledger of simulated customer-to-customer transfers.', items.length)}<div class="filter-row"><input class="workflow-search" id="transferSearch" placeholder="Search by reference, sender or recipient"></div><div class="table-wrap"><div class="transfer-row" style="background:#f9fafc;font-size:7px;font-weight:700;color:#8491a3"><span>REFERENCE / DATE</span><span>SENDER → RECIPIENT</span><span>ACCOUNTS</span><span>AMOUNT</span></div><div id="employeeTransferRows">${transferRows(items)}</div></div>`;
    }

    function transferRows(items) {
        if (!items.length) return empty();
        return items.map(t => `<div class="transfer-row"><span><strong>${esc(t.reference)}</strong><small>${esc(fmtDate(t.occurredAt))}</small></span><span><strong>${esc(t.sender)} → ${esc(t.recipient)}</strong><small>${esc(t.status)}</small></span><span><strong>${esc(t.sourceAccount)} → ${esc(t.destinationAccount)}</strong><small>${esc(t.description || 'No description')}</small></span><strong>${esc(new Intl.NumberFormat('en-GB', {
            style: 'currency',
            currency: t.currency
        }).format(Number(t.amount)))}</strong></div>`).join('')
    }

    async function decide(button) {
        const status = button.dataset.decision;
        let reason = null;
        if (status === 'REJECTED') {
            reason = prompt('Enter a reason the customer can see:');
            if (reason === null) return;
            if (!reason.trim()) return toast('A reason is required to reject.', true)
        } else if (!confirm('Approve this request?')) return;
        const apiPath = paths[section];
        const id = button.dataset.id;
        button.disabled = true;
        try {
            await api(`${apiPath}/${encodeURIComponent(id)}`, {
                method: 'PATCH',
                body: JSON.stringify({status, reason})
            });
            toast(status === 'APPROVED' ? 'Request approved.' : 'Request rejected.');
            await load()
        } catch (e) {
            button.disabled = false;
            toast(e.message, true)
        }
    }

    $('#workflowPage').addEventListener('click', event => {
        const button = event.target.closest('[data-decision]');
        if (button) decide(button)
    });
    $('#workflowPage').addEventListener('input', event => {
        if (event.target.id === 'transferSearch') {
            const q = event.target.value.toLowerCase();
            $('#employeeTransferRows').innerHTML = transferRows(rows.filter(r => `${r.reference} ${r.sender} ${r.recipient}`.toLowerCase().includes(q)))
        }
    });
    $('#workflowPage').addEventListener('submit', async event => {
        const form = event.target.closest('[data-support-form]');
        if (!form) return;
        event.preventDefault();
        const button = form.querySelector('[type=submit]');
        button.disabled = true;
        try {
            const data = Object.fromEntries(new FormData(form));
            await api(`/api/employee/support-requests/${encodeURIComponent(form.dataset.supportForm)}`, {
                method: 'PATCH',
                body: JSON.stringify(data)
            });
            toast('Customer response saved.');
            await load()
        } catch (error) {
            button.disabled = false;
            toast(error.message, true)
        }
    });
    $('#workflowLogout').addEventListener('submit', async event => {
        event.preventDefault();
        try {
            if (!csrf) await getCsrf();
            const data = new URLSearchParams();
            data.set(csrf.parameterName, csrf.token);
            const r = await fetch('/logout', {
                method: 'POST',
                body: data,
                credentials: 'same-origin',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            });
            if (!r.ok) throw new Error('Could not sign out.');
            location.assign('/login/employee?logout')
        } catch (error) {
            toast(error.message, true)
        }
    });
    load();
})();
