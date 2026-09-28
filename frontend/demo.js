/**
 * FeatureFlagLite — Live Client App Simulation Logic
 * Demonstrates real-time downstream consumer experience adaptation.
 */

const API_BASE = '/api/v1';

// DOM References
const demoEnvSelector = document.getElementById('demoEnvSelector');
const demoUserIdInput = document.getElementById('demoUserIdInput');
const navEnvBadge = document.getElementById('navEnvBadge');
const mockAppCanvas = document.getElementById('mockAppCanvas');
const mockAppSessionTag = document.getElementById('mockAppSessionTag');
const mockAppEnvIndicator = document.getElementById('mockAppEnvIndicator');

const searchFeatureArea = document.getElementById('searchFeatureArea');
const searchFlagStatusBadge = document.getElementById('searchFlagStatusBadge');

const dashboardFeatureArea = document.getElementById('dashboardFeatureArea');
const dashboardFlagStatusBadge = document.getElementById('dashboardFlagStatusBadge');

const checkoutFeatureArea = document.getElementById('checkoutFeatureArea');
const checkoutFlagStatusBadge = document.getElementById('checkoutFlagStatusBadge');

const themeFeatureArea = document.getElementById('themeFeatureArea');
const customFlagsArea = document.getElementById('customFlagsArea');
const inspectorDetails = document.getElementById('inspectorDetails');
const rawJsonResponse = document.getElementById('rawJsonResponse');
const inspectorEnvName = document.getElementById('inspectorEnvName');
const activeFlagCountPill = document.getElementById('activeFlagCountPill');

// Internal Demo State
let localThemeOverride = null; // null = use flag, true = dark, false = light
let lastEvaluations = {};

document.addEventListener('DOMContentLoaded', () => {
    // Check URL parameters for env or userId
    const params = new URLSearchParams(window.location.search);
    if (params.has('env')) {
        demoEnvSelector.value = params.get('env');
    }
    if (params.has('userId')) {
        demoUserIdInput.value = params.get('userId');
    }

    demoEnvSelector.addEventListener('change', () => {
        refreshDemoApp();
    });

    demoUserIdInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') refreshDemoApp();
    });

    refreshDemoApp();
});

function setDemoUser(userId, roleLabel) {
    demoUserIdInput.value = userId;
    document.querySelectorAll('.chip').forEach(c => {
        c.classList.toggle('active', c.textContent.includes(userId));
    });
    showToast(`Evaluating as: ${userId} (${roleLabel || 'Selected Persona'})`);
    refreshDemoApp();
}

async function apiFetch(path, options = {}) {
    const res = await fetch(API_BASE + path, {
        headers: { 'Content-Type': 'application/json', ...options.headers },
        ...options
    });
    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }
    if (res.status === 204) return null;
    return await res.json();
}

async function refreshDemoApp() {
    const env = demoEnvSelector.value;
    const userId = (demoUserIdInput.value || 'user-123').trim();

    navEnvBadge.textContent = env.toUpperCase();
    mockAppSessionTag.textContent = `USER: ${userId}`;
    mockAppEnvIndicator.textContent = `ENV: ${env.toUpperCase()}`;
    inspectorEnvName.textContent = env;

    try {
        // 1. Fetch raw environment flags
        const envFlagsResponse = await apiFetch(`/flags?environment=${env}`);
        rawJsonResponse.textContent = JSON.stringify(envFlagsResponse, null, 2);

        // 2. Fetch full flag catalog
        const allFlags = await apiFetch('/flags');

        // 3. Evaluate all flags for user using system endpoint
        let evaluationsList = [];
        try {
            evaluationsList = await apiFetch(`/flags/system/evaluate-all?environment=${env}&userId=${encodeURIComponent(userId)}`);
        } catch (e) {
            // Fallback to per-flag evaluation if needed
            const evalPromises = allFlags.map(f =>
                apiFetch(`/flags/${f.name}/evaluate?environment=${env}&userId=${encodeURIComponent(userId)}`)
                    .catch(() => ({
                        flagName: f.name,
                        environment: env,
                        enabled: envFlagsResponse.flags[f.name] ?? f.defaultState,
                        rolloutPercentage: 0,
                        bucket: 0,
                        explanation: 'Evaluated from baseline'
                    }))
            );
            evaluationsList = await Promise.all(evalPromises);
        }

        const evaluations = {};
        evaluationsList.forEach(item => {
            evaluations[item.flagName] = item;
        });
        lastEvaluations = evaluations;

        // Count active features
        const activeCount = evaluationsList.filter(e => e.enabled).length;
        activeFlagCountPill.textContent = `${activeCount} Active`;

        // 4. Render components
        renderSearch(evaluations['betaSearch']);
        renderDashboard(evaluations['newDashboard']);
        renderCheckout(evaluations['newCheckout'], userId, env);
        renderTheme(evaluations['darkMode'], env);
        renderCustomFlags(allFlags, evaluations);
        renderInspector(evaluations, userId, env);

    } catch (err) {
        rawJsonResponse.textContent = `// Error connecting to backend: ${err.message}`;
        showToast(`Could not refresh demo application: ${err.message}`, true);
    }
}

// ──────────────────────────────────────────────
// Component 1: Search Experience (betaSearch)
// ──────────────────────────────────────────────

function renderSearch(evalData) {
    const isAiOn = evalData ? evalData.enabled : false;

    if (isAiOn) {
        searchFlagStatusBadge.className = 'badge badge-live';
        searchFlagStatusBadge.textContent = 'AI Search Active';

        searchFeatureArea.innerHTML = `
            <div class="mock-search-bar ai-active">
                <span class="ai-pill">AI</span>
                <input type="text" id="mockAiInput" class="mock-search-input" placeholder="Ask AI: 'Filter active flags or summarize telemetry...'" onkeydown="if(event.key==='Enter') executeAiSearch()">
                <button class="btn btn-primary btn-sm" onclick="executeAiSearch()">Ask AI</button>
            </div>
            <div class="chip-row" style="margin-top:8px;">
                <span class="chip" onclick="applyAiSuggestion('Audit recent flag updates')">✨ Audit recent changes</span>
                <span class="chip" onclick="applyAiSuggestion('Show revenue growth trends')">✨ Show revenue trends</span>
                <span class="chip" onclick="applyAiSuggestion('Find 100% rollout flags')">✨ Find 100% rollouts</span>
            </div>
        `;
    } else {
        searchFlagStatusBadge.className = 'badge badge-paused';
        searchFlagStatusBadge.textContent = 'Standard Search (AI Flag Off)';

        searchFeatureArea.innerHTML = `
            <div class="mock-search-bar">
                <span style="color:var(--text-muted); font-size:0.8rem;">🔍</span>
                <input type="text" class="mock-search-input" placeholder="Standard search by exact keyword..." onkeydown="if(event.key==='Enter') showToast('Keyword search executed')">
                <button class="btn btn-secondary btn-sm" onclick="showToast('Keyword query submitted.')">Search</button>
            </div>
            <div style="font-size:0.71rem; color:var(--text-muted); margin-top:4px; font-family:var(--font-mono);">
                Standard keyword search active. Enable 'betaSearch' in the console to unlock AI semantic capabilities.
            </div>
        `;
    }
}

function applyAiSuggestion(text) {
    const input = document.getElementById('mockAiInput');
    if (input) input.value = text;
    showToast(`AI Semantic Analysis: "${text}"`);
}

function executeAiSearch() {
    const input = document.getElementById('mockAiInput');
    const q = input ? input.value.trim() : 'Active flags query';
    showToast(`AI Model generated instant answer for: "${q || 'System analysis'}"`);
}

// ──────────────────────────────────────────────
// Component 2: Analytics Dashboard (newDashboard)
// ──────────────────────────────────────────────

function renderDashboard(evalData) {
    const isNewOn = evalData ? evalData.enabled : false;

    if (isNewOn) {
        dashboardFlagStatusBadge.className = 'badge badge-live';
        dashboardFlagStatusBadge.textContent = 'Modern Rich Dashboard';

        dashboardFeatureArea.innerHTML = `
            <div class="mock-dashboard-grid">
                <div class="mock-kpi-card">
                    <div class="mock-kpi-lbl">Monthly Recurring Revenue</div>
                    <div class="mock-kpi-val">$68,400</div>
                    <div style="font-size:0.7rem; color:#ffffff; font-family:var(--font-mono); margin-top:3px;">▲ +24.8% vs last month</div>
                </div>
                <div class="mock-kpi-card">
                    <div class="mock-kpi-lbl">Active Customer Sessions</div>
                    <div class="mock-kpi-val">2,410</div>
                    <div style="font-size:0.7rem; color:#ffffff; font-family:var(--font-mono); margin-top:3px;">▲ +120 today</div>
                </div>
                <div class="mock-kpi-card">
                    <div class="mock-kpi-lbl">System Reliability</div>
                    <div class="mock-kpi-val">99.98%</div>
                    <div style="font-size:0.7rem; color:var(--text-secondary); font-family:var(--font-mono); margin-top:3px;">0 incidents (30d)</div>
                </div>
            </div>

            <div class="mock-chart-container" style="margin-top:12px;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                    <div style="font-size:0.72rem; font-family:var(--font-mono); text-transform:uppercase; color:var(--text-secondary);">
                        Request Volume &bull; Last 7 Days (Hover for telemetry)
                    </div>
                    <span style="font-size:0.68rem; font-family:var(--font-mono); color:var(--text-muted);">PEAK: 14.2K REQ/S</span>
                </div>
                <div class="mock-chart-bars">
                    <div class="mock-chart-col" onclick="showToast('Monday: 8.2K req/s')">
                        <div class="mock-bar" style="height:45%;"></div>
                        <span class="mock-day-label">MON</span>
                    </div>
                    <div class="mock-chart-col" onclick="showToast('Tuesday: 9.8K req/s')">
                        <div class="mock-bar" style="height:60%;"></div>
                        <span class="mock-day-label">TUE</span>
                    </div>
                    <div class="mock-chart-col" onclick="showToast('Wednesday: 9.1K req/s')">
                        <div class="mock-bar" style="height:55%;"></div>
                        <span class="mock-day-label">WED</span>
                    </div>
                    <div class="mock-chart-col" onclick="showToast('Thursday: 11.4K req/s')">
                        <div class="mock-bar" style="height:75%;"></div>
                        <span class="mock-day-label">THU</span>
                    </div>
                    <div class="mock-chart-col" onclick="showToast('Friday: 13.9K req/s')">
                        <div class="mock-bar" style="height:92%;"></div>
                        <span class="mock-day-label">FRI</span>
                    </div>
                    <div class="mock-chart-col" onclick="showToast('Saturday: 10.6K req/s')">
                        <div class="mock-bar" style="height:68%;"></div>
                        <span class="mock-day-label">SAT</span>
                    </div>
                    <div class="mock-chart-col" onclick="showToast('Sunday: 14.2K req/s (Peak)')">
                        <div class="mock-bar" style="height:100%;"></div>
                        <span class="mock-day-label">SUN</span>
                    </div>
                </div>
            </div>
        `;
    } else {
        dashboardFlagStatusBadge.className = 'badge badge-paused';
        dashboardFlagStatusBadge.textContent = 'Legacy Fallback Table';

        dashboardFeatureArea.innerHTML = `
            <div class="legacy-table-container">
                <div style="font-weight:700; margin-bottom:4px; color:#ffffff;">[LEGACY TELEMETRY: PLAIN TEXT ASCII FORMAT]</div>
                <div>METRIC            | VALUE       | STATUS    | NOTE</div>
                <div>-------------------------------------------------------</div>
                <div>MRR               | $68,400     | NOMINAL   | Standard billing cycle</div>
                <div>ACTIVE_USERS      | 2,410       | NOMINAL   | Tracked via raw logs</div>
                <div>SYSTEM_UPTIME     | 99.98%      | NOMINAL   | SLA compliance verified</div>
                <div style="margin-top:6px; color:var(--text-muted);">
                    // Rich graphic charts and interactive KPI sparklines are disabled under the legacy dashboard flag.
                </div>
            </div>
        `;
    }
}

// ──────────────────────────────────────────────
// Component 3: Checkout System (newCheckout)
// ──────────────────────────────────────────────

function renderCheckout(evalData, userId, env) {
    const isExpressOn = evalData ? evalData.enabled : false;
    const rollout = evalData ? evalData.rolloutPercentage : 0;
    const slot = evalData ? evalData.bucket : 0;

    if (isExpressOn) {
        checkoutFlagStatusBadge.className = 'badge badge-live';
        checkoutFlagStatusBadge.textContent = `1-Click Active (${rollout}%)`;

        checkoutFeatureArea.innerHTML = `
            <div class="mock-checkout-box">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <div style="font-size:0.88rem; font-weight:700; font-family:var(--font-mono);">
                            EXPRESS CHECKOUT GATEWAY
                        </div>
                        <div style="font-size:0.74rem; color:var(--text-secondary);">
                            Instant authorization with zero form friction.
                        </div>
                    </div>
                    <span class="badge badge-granted">✓ 1-CLICK ENABLED</span>
                </div>

                <button class="express-checkout-btn" onclick="executeMockCheckout()">
                    ⚡ Instant 1-Click Buy ($49.00)
                </button>

                <div style="font-size:0.73rem; font-family:var(--font-mono); color:var(--text-secondary); line-height:1.4;">
                    ✓ User <code>${escapeHtml(userId)}</code> evaluated into hash slot <strong>${slot}</strong> (within active <strong>0&ndash;${rollout - 1}</strong> tier). Granted express access.
                </div>
            </div>
        `;
    } else {
        checkoutFlagStatusBadge.className = 'badge badge-paused';
        checkoutFlagStatusBadge.textContent = rollout > 0 ? `${rollout}% Rollout (Excluded)` : 'Standard Fallback';

        checkoutFeatureArea.innerHTML = `
            <div class="mock-checkout-box">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <div style="font-size:0.88rem; font-weight:700; font-family:var(--font-mono); color:#aaaaaa;">
                            STANDARD 3-STEP CHECKOUT
                        </div>
                        <div style="font-size:0.74rem; color:var(--text-muted);">
                            Traditional fallback checkout form.
                        </div>
                    </div>
                    <span class="badge badge-fallback">STANDARD FORM</span>
                </div>

                <div class="legacy-checkout-steps">
                    <div class="legacy-step-row">
                        <input type="text" class="input" placeholder="Full Legal Name" value="Jane Doe" readonly>
                        <input type="text" class="input" placeholder="Shipping Address" value="742 Evergreen Terrace" readonly>
                    </div>
                    <div class="legacy-step-row">
                        <input type="text" class="input input-mono" placeholder="Card Number" value="•••• •••• •••• 4242" readonly>
                        <input type="text" class="input input-mono" placeholder="MM/YY" value="09/29" style="max-width:90px;" readonly>
                    </div>
                    <button class="btn btn-secondary" onclick="executeStandardCheckout()">
                        Review &amp; Place Order ($49.00)
                    </button>
                </div>

                <div style="font-size:0.73rem; font-family:var(--font-mono); color:var(--text-muted); line-height:1.4;">
                    User <code>${escapeHtml(userId)}</code> evaluated into hash slot <strong>${slot}</strong> (outside active <strong>${rollout}%</strong> tier). Retained on standard safe form.
                </div>
            </div>
        `;
    }
}

function executeMockCheckout() {
    showToast('Order confirmed via 1-Click Express Checkout! Payment token authorized in 24ms.');
}

function executeStandardCheckout() {
    showToast('Standard 3-step checkout validated and processed successfully.');
}

// ──────────────────────────────────────────────
// Component 4: Theme Engine (darkMode)
// ──────────────────────────────────────────────

function renderTheme(evalData, env) {
    const isDarkFlag = evalData ? evalData.enabled : true;
    const effectiveDark = localThemeOverride !== null ? localThemeOverride : isDarkFlag;

    // Apply or remove light mode class from mockAppCanvas
    if (effectiveDark) {
        mockAppCanvas.classList.remove('mock-app-light');
    } else {
        mockAppCanvas.classList.add('mock-app-light');
    }

    themeFeatureArea.innerHTML = `
        <div style="background:var(--bg-surface); border:1px solid var(--border); border-radius:var(--radius); padding:14px 16px; display:flex; justify-content:space-between; align-items:center;">
            <div>
                <div style="font-family:var(--font-mono); font-size:0.82rem; font-weight:700;">
                    THEME ENGINE: <span style="font-weight:400;">${effectiveDark ? 'OLED MONOCHROME DARK' : 'CRISP WHITE LIGHT'}</span>
                </div>
                <div style="font-size:0.74rem; color:var(--text-secondary); margin-top:2px;">
                    Controlled by flag <code>darkMode</code> in ${env.toUpperCase()}. ${localThemeOverride !== null ? '(Client local preview active)' : ''}
                </div>
            </div>
            <span class="badge ${effectiveDark ? 'badge-live' : 'badge-paused'}">
                ${effectiveDark ? 'DARK MODE' : 'LIGHT MODE'}
            </span>
        </div>
    `;
}

function toggleLocalTheme() {
    if (localThemeOverride === null) {
        const flagVal = lastEvaluations['darkMode'] ? lastEvaluations['darkMode'].enabled : true;
        localThemeOverride = !flagVal;
    } else {
        localThemeOverride = !localThemeOverride;
    }
    renderTheme(lastEvaluations['darkMode'], demoEnvSelector.value);
    showToast(`Client theme switched to: ${localThemeOverride ? 'Dark Monochrome' : 'Clean White'}`);
}

// ──────────────────────────────────────────────
// Component 5: Additional / Custom Flags
// ──────────────────────────────────────────────

function renderCustomFlags(allFlags, evaluations) {
    const standardFlags = new Set(['newDashboard', 'darkMode', 'newCheckout', 'betaSearch']);
    const customFlags = allFlags.filter(f => !standardFlags.has(f.name));

    if (customFlags.length === 0) {
        customFlagsArea.innerHTML = '';
        return;
    }

    const itemsHtml = customFlags.map(flag => {
        const ev = evaluations[flag.name] || { enabled: false, rolloutPercentage: 0 };
        return `
            <div style="background:var(--bg-surface); border:1px solid var(--border); padding:12px 14px; border-radius:var(--radius); display:flex; justify-content:space-between; align-items:center;">
                <div>
                    <div style="font-family:var(--font-mono); font-size:0.82rem; font-weight:700;">${escapeHtml(flag.name)}</div>
                    <div style="font-size:0.74rem; color:var(--text-secondary);">${escapeHtml(flag.description || 'Custom registered flag')}</div>
                </div>
                <span class="badge ${ev.enabled ? 'badge-live' : 'badge-paused'}">
                    ${ev.enabled ? 'ACTIVE (' + ev.rolloutPercentage + '%)' : 'PAUSED'}
                </span>
            </div>
        `;
    }).join('');

    customFlagsArea.innerHTML = `
        <div>
            <div style="font-size:0.7rem; font-family:var(--font-mono); text-transform:uppercase; color:var(--text-muted); margin-bottom:6px;">
                Additional Custom Registered Flags
            </div>
            <div style="display:flex; flex-direction:column; gap:8px;">
                ${itemsHtml}
            </div>
        </div>
    `;
}

// ──────────────────────────────────────────────
// Sidebar Inspector
// ──────────────────────────────────────────────

function renderInspector(evaluations, userId, env) {
    const keys = Object.keys(evaluations);
    if (keys.length === 0) {
        inspectorDetails.innerHTML = '<div class="table-loading">No flags evaluated.</div>';
        return;
    }

    inspectorDetails.innerHTML = keys.map(flagName => {
        const ev = evaluations[flagName];
        const isGranted = ev.enabled;
        const rollout = ev.rolloutPercentage;
        const slot = ev.bucket !== undefined ? ev.bucket : 0;

        return `
            <div class="inspector-card">
                <div class="inspector-card-header">
                    <span class="inspector-flag-name">${escapeHtml(flagName)}</span>
                    <span class="badge ${isGranted ? 'badge-granted' : 'badge-fallback'}" style="font-size:0.65rem;">
                        ${isGranted ? '✓ GRANTED' : '✗ FALLBACK'}
                    </span>
                </div>
                <div class="inspector-explanation">
                    ${escapeHtml(ev.explanation || (isGranted ? 'Feature active' : 'Fallback active'))}
                </div>
                <div class="inspector-slot-info" style="display:flex; justify-content:space-between; align-items:center;">
                    <span>Slot: <strong>${slot}</strong> / Reach: <strong>${rollout}%</strong></span>
                    <button class="btn btn-secondary btn-sm" style="font-size:0.68rem; padding:2px 8px;" onclick="quickToggleFlag('${escapeAttr(flagName)}', ${!isGranted}, ${rollout === 0 ? 100 : rollout})">
                        ${isGranted ? 'Turn Off' : 'Turn On'}
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

async function quickToggleFlag(flagName, newEnabled, rollout) {
    const env = demoEnvSelector.value;
    try {
        await apiFetch(`/flags/${flagName}/states`, {
            method: 'PUT',
            body: JSON.stringify({
                environment: env,
                enabled: newEnabled,
                rolloutPercentage: newEnabled ? (rollout > 0 ? rollout : 100) : 0,
                changedBy: 'demo-quick-toggle'
            })
        });

        showToast(`"${flagName}" switched to ${newEnabled ? 'ON' : 'OFF'} in ${env.toUpperCase()}`);
        refreshDemoApp();
    } catch (err) {
        showToast(`Could not toggle flag: ${err.message}`, true);
    }
}

function copyRawJson() {
    const text = rawJsonResponse.textContent;
    navigator.clipboard.writeText(text).then(() => {
        showToast('Raw API payload copied to clipboard');
    }).catch(() => {
        showToast('Copied payload');
    });
}

// ──────────────────────────────────────────────
// Utilities
// ──────────────────────────────────────────────

function showToast(msg, isError = false) {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${isError ? 'toast-error' : ''}`;
    toast.textContent = msg;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(12px)';
        toast.style.transition = 'all 0.2s ease';
        setTimeout(() => toast.remove(), 200);
    }, 2800);
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function escapeAttr(str) {
    return str.replace(/'/g, "\\'").replace(/"/g, '\\"');
}
