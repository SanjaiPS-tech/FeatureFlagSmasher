/**
 * FeatureFlagLite — Client App Demo Logic
 * Simulates a real client app consuming the FeatureFlagLite API.
 */

const API_BASE = '/api/v1';

const demoEnvSelector = document.getElementById('demoEnvSelector');
const demoUserIdInput = document.getElementById('demoUserIdInput');
const navEnvBadge = document.getElementById('navEnvBadge');
const mockAppEnvIndicator = document.getElementById('mockAppEnvIndicator');
const searchFeatureArea = document.getElementById('searchFeatureArea');
const dashboardFeatureArea = document.getElementById('dashboardFeatureArea');
const checkoutFeatureArea = document.getElementById('checkoutFeatureArea');
const themeFeatureArea = document.getElementById('themeFeatureArea');
const customFlagsArea = document.getElementById('customFlagsArea');
const inspectorDetails = document.getElementById('inspectorDetails');
const rawJsonResponse = document.getElementById('rawJsonResponse');
const inspectorEnvName = document.getElementById('inspectorEnvName');

document.addEventListener('DOMContentLoaded', () => {
    // If URL has ?env=, pick it up
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

function setDemoUser(userId) {
    demoUserIdInput.value = userId;
    document.querySelectorAll('.chip').forEach(c => {
        c.classList.toggle('active', c.textContent.trim() === userId);
    });
    showToast(`Evaluating as user: ${userId}`);
    refreshDemoApp();
}

async function apiFetch(path) {
    const res = await fetch(API_BASE + path);
    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }
    return await res.json();
}

async function refreshDemoApp() {
    const env = demoEnvSelector.value;
    const userId = demoUserIdInput.value.trim() || 'user-123';

    navEnvBadge.textContent = env.toUpperCase();
    mockAppEnvIndicator.textContent = `ENVIRONMENT: ${env.toUpperCase()} | USER: ${userId}`;
    inspectorEnvName.textContent = env;

    try {
        // 1. Fetch raw environment flags
        const envFlagsResponse = await apiFetch(`/flags?environment=${env}`);
        rawJsonResponse.textContent = JSON.stringify(envFlagsResponse, null, 2);

        // 2. Fetch full flag catalog for evaluations & metadata
        const allFlags = await apiFetch('/flags');

        // 3. Evaluate each flag for current user & environment
        const evaluations = {};
        for (const flag of allFlags) {
            try {
                const evalData = await apiFetch(`/flags/${flag.name}/evaluate?environment=${env}&userId=${encodeURIComponent(userId)}`);
                evaluations[flag.name] = evalData;
            } catch (e) {
                evaluations[flag.name] = {
                    flagName: flag.name,
                    environment: env,
                    enabled: envFlagsResponse.flags[flag.name] ?? flag.defaultState,
                    rolloutPercentage: 0
                };
            }
        }

        // 4. Render components
        renderSearch(evaluations['betaSearch']);
        renderDashboard(evaluations['newDashboard']);
        renderCheckout(evaluations['newCheckout'], userId, env);
        renderTheme(evaluations['darkMode'], env);
        renderCustomFlags(allFlags, evaluations);
        renderInspector(evaluations, userId, env);

    } catch (err) {
        rawJsonResponse.textContent = `// Error connecting to backend: ${err.message}`;
        showToast(`Failed to fetch flag data: ${err.message}`, true);
    }
}

function renderSearch(evalData) {
    const isOn = evalData ? evalData.enabled : false;
    if (isOn) {
        searchFeatureArea.innerHTML = `
            <div class="mock-search-bar ai-active">
                <span class="ai-pill">AI</span>
                <input type="text" class="mock-search-input" placeholder="Ask AI: 'Filter active flags or summarize metrics...'">
                <button class="btn btn-primary btn-sm" style="padding:4px 8px;" onclick="showToast('AI Semantic Query executed')">Search</button>
            </div>
            <div class="chip-row" style="margin-top:6px;">
                <span class="chip" onclick="showToast('Suggestion applied')">✨ Show revenue insights</span>
                <span class="chip" onclick="showToast('Suggestion applied')">✨ Audit flag changes</span>
                <span class="chip" onclick="showToast('Suggestion applied')">✨ Filter 100% rollouts</span>
            </div>
        `;
    } else {
        searchFeatureArea.innerHTML = `
            <div class="mock-search-bar">
                <span style="color:#666; font-size:0.8rem;">🔍</span>
                <input type="text" class="mock-search-input" placeholder="Standard search by keyword...">
                <button class="btn btn-secondary btn-sm" style="padding:4px 8px;" onclick="showToast('Keyword Search executed')">Go</button>
            </div>
        `;
    }
}

function renderDashboard(evalData) {
    const isOn = evalData ? evalData.enabled : false;
    if (isOn) {
        dashboardFeatureArea.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
                <div style="font-size:0.82rem; font-weight:700; font-family:var(--font-mono);">
                    ANALYTICS DASHBOARD <span class="badge badge-on" style="font-size:0.65rem;">NEW UI</span>
                </div>
                <div style="font-size:0.72rem; color:var(--text-secondary); font-family:var(--font-mono);">REAL-TIME TELEMETRY</div>
            </div>
            <div class="mock-dashboard-grid">
                <div class="mock-kpi-card">
                    <div class="mock-kpi-lbl">Monthly Recurring Revenue</div>
                    <div class="mock-kpi-val">$68,400</div>
                    <div style="font-size:0.7rem; color:#aaa; margin-top:2px;">▲ +24.8% vs last month</div>
                </div>
                <div class="mock-kpi-card">
                    <div class="mock-kpi-lbl">Active Users</div>
                    <div class="mock-kpi-val">2,410</div>
                    <div style="font-size:0.7rem; color:#aaa; margin-top:2px;">▲ +120 new today</div>
                </div>
                <div class="mock-kpi-card">
                    <div class="mock-kpi-lbl">System Uptime</div>
                    <div class="mock-kpi-val">99.98%</div>
                    <div style="font-size:0.7rem; color:#aaa; margin-top:2px;">0 incidents in 30d</div>
                </div>
            </div>
            <div class="mock-chart-container" style="margin-top:12px;">
                <div style="font-size:0.72rem; font-family:var(--font-mono); color:var(--text-secondary); margin-bottom:8px;">REQUEST VOLUME (LAST 7 DAYS)</div>
                <div class="mock-chart-bars">
                    <div class="mock-bar" style="height:35%;"></div>
                    <div class="mock-bar" style="height:55%;"></div>
                    <div class="mock-bar" style="height:48%;"></div>
                    <div class="mock-bar" style="height:70%;"></div>
                    <div class="mock-bar" style="height:90%;"></div>
                    <div class="mock-bar" style="height:65%;"></div>
                    <div class="mock-bar" style="height:100%;"></div>
                </div>
            </div>
        `;
    } else {
        dashboardFeatureArea.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
                <div style="font-size:0.82rem; font-weight:700; font-family:var(--font-mono); color:#888;">
                    LEGACY METRICS <span class="badge badge-off" style="font-size:0.65rem;">V1.0</span>
                </div>
            </div>
            <div class="legacy-table-container">
                <div>[TABLE REPORT: PLAIN TEXT ONLY]</div>
                <div>METRIC            | VALUE       | STATUS</div>
                <div>---------------------------------------</div>
                <div>MRR               | $68,400     | OK</div>
                <div>ACTIVE_USERS      | 2,410       | OK</div>
                <div>SYSTEM_UPTIME     | 99.98%      | OK</div>
                <div>(Rich graph view is disabled under legacy dashboard flag)</div>
            </div>
        `;
    }
}

function renderCheckout(evalData, userId, env) {
    const isOn = evalData ? evalData.enabled : false;
    const rollout = evalData ? evalData.rolloutPercentage : 0;

    if (isOn) {
        checkoutFeatureArea.innerHTML = `
            <div class="mock-checkout-box">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <div style="font-size:0.85rem; font-weight:700; font-family:var(--font-mono);">
                        EXPRESS CHECKOUT
                    </div>
                    <span class="badge badge-on">1-CLICK ENABLED (${rollout}%)</span>
                </div>
                <button class="express-checkout-btn" onclick="showToast('Order placed instantly via 1-Click Checkout!')">
                    ⚡ Instant 1-Click Buy ($49.00)
                </button>
                <div style="font-size:0.72rem; font-family:var(--font-mono); color:var(--text-secondary);">
                    ✓ User <code>${escapeHtml(userId)}</code> was evaluated into the active <strong>${rollout}%</strong> rollout tier.
                </div>
            </div>
        `;
    } else {
        checkoutFeatureArea.innerHTML = `
            <div class="mock-checkout-box">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <div style="font-size:0.85rem; font-weight:700; font-family:var(--font-mono); color:#888;">
                        STANDARD CHECKOUT
                    </div>
                    <span class="badge badge-off">${rollout > 0 ? rollout + '% ROLLOUT' : 'DISABLED'}</span>
                </div>
                <div class="legacy-checkout-steps">
                    <div class="legacy-step-row">
                        <input type="text" placeholder="Full Name" readonly value="Jane Doe">
                        <input type="text" placeholder="Shipping Address" readonly value="123 Market St">
                    </div>
                    <div class="legacy-step-row">
                        <input type="text" placeholder="Card Number" readonly value="•••• •••• •••• 4242">
                        <input type="text" placeholder="MM/YY" readonly value="12/28" style="max-width:80px;">
                    </div>
                    <button class="btn btn-secondary" onclick="showToast('Submitting standard 3-step checkout...')">
                        Complete Standard Checkout ($49.00)
                    </button>
                </div>
                <div style="font-size:0.72rem; font-family:var(--font-mono); color:var(--text-muted);">
                    User <code>${escapeHtml(userId)}</code> did not meet the rollout threshold for ${env.toUpperCase()}.
                </div>
            </div>
        `;
    }
}

function renderTheme(evalData, env) {
    const isOn = evalData ? evalData.enabled : false;
    themeFeatureArea.innerHTML = `
        <div class="panel" style="padding:16px; border:1px solid var(--border);">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                <div style="font-size:0.82rem; font-weight:700; font-family:var(--font-mono);">
                    THEME ENGINE: <span style="font-weight:400;">${isOn ? 'OLED MONOCHROME DARK' : 'STANDARD LIGHT'}</span>
                </div>
                <span class="badge ${isOn ? 'badge-on' : 'badge-off'}">${isOn ? 'DARK ACTIVE' : 'LIGHT ACTIVE'}</span>
            </div>
            <div style="font-size:0.75rem; color:var(--text-secondary);">
                Flag <code>darkMode</code> is currently <strong>${isOn ? 'ON' : 'OFF'}</strong> in ${env.toUpperCase()}.
            </div>
        </div>
    `;
}

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
            <div style="background:var(--bg-surface); border:1px solid var(--border); padding:12px 16px; border-radius:var(--radius); display:flex; justify-content:space-between; align-items:center;">
                <div>
                    <div style="font-family:var(--font-mono); font-size:0.82rem; font-weight:600;">${escapeHtml(flag.name)}</div>
                    <div style="font-size:0.74rem; color:var(--text-secondary);">${escapeHtml(flag.description || '')}</div>
                </div>
                <span class="badge ${ev.enabled ? 'badge-on' : 'badge-off'}">
                    ${ev.enabled ? 'ENABLED (' + ev.rolloutPercentage + '%)' : 'DISABLED'}
                </span>
            </div>
        `;
    }).join('');

    customFlagsArea.innerHTML = `
        <div style="display:flex; flex-direction:column; gap:8px;">
            <div style="font-size:0.78rem; font-weight:700; font-family:var(--font-mono); color:var(--text-secondary);">
                ADDITIONAL CUSTOM FLAGS
            </div>
            ${itemsHtml}
        </div>
    `;
}

function renderInspector(evaluations, userId, env) {
    const keys = Object.keys(evaluations);
    if (keys.length === 0) {
        inspectorDetails.innerHTML = '<div>No flags evaluated.</div>';
        return;
    }

    inspectorDetails.innerHTML = keys.map(flagName => {
        const ev = evaluations[flagName];
        return `
            <div style="padding:6px 0; border-bottom:1px solid var(--border); display:flex; justify-content:space-between; align-items:center;">
                <span style="font-weight:600;">${escapeHtml(flagName)}</span>
                <span style="color:${ev.enabled ? '#fff' : '#666'}; font-weight:700;">
                    ${ev.enabled ? 'TRUE' : 'FALSE'} (${ev.rolloutPercentage}%)
                </span>
            </div>
        `;
    }).join('');
}

function showToast(msg, isError = false) {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${isError ? 'toast-error' : ''}`;
    toast.textContent = msg;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 2500);
}

function escapeHtml(str) {
    if (!str) return '';
    const d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
}
