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

const resourceFeatureArea = document.getElementById('resourceFeatureArea');
const resourceFlagStatusBadge = document.getElementById('resourceFlagStatusBadge');

const securityActionsArea = document.getElementById('securityActionsArea');
const securityActionsStatusBadge = document.getElementById('securityActionsStatusBadge');

const themeFeatureArea = document.getElementById('themeFeatureArea');
const customFlagsArea = document.getElementById('customFlagsArea');
const inspectorDetails = document.getElementById('inspectorDetails');
const rawJsonResponse = document.getElementById('rawJsonResponse');
const inspectorEnvName = document.getElementById('inspectorEnvName');
const activeFlagCountPill = document.getElementById('activeFlagCountPill');
const floatingChatWidget = document.getElementById('floatingChatWidget');
const realtimeStatusDot = document.getElementById('realtimeStatusDot');
const realtimeStatusText = document.getElementById('realtimeStatusText');
const realtimeToggleBtn = document.getElementById('realtimeToggleBtn');
const realtimeEventBanner = document.getElementById('realtimeEventBanner');
const realtimeBannerText = document.getElementById('realtimeBannerText');
const realtimeBannerTime = document.getElementById('realtimeBannerTime');

// Internal Demo State
let localThemeOverride = null; // null = use flag, true = dark, false = light
let lastEvaluations = {};
let realtimeAutoSync = true;
let realtimeEventSource = null;
let realtimeBroadcastChannel = null;
let realtimeEventCount = 0;
let lastPolledFlagsHash = '';
let bannerDismissTimeout = null;

let demoClusterNodes = [
    { id: 'node-us-east-1a', region: 'us-east-1', cpu: '2.4 GHz', status: 'Online' },
    { id: 'node-us-east-1b', region: 'us-east-1', cpu: '2.4 GHz', status: 'Online' },
    { id: 'node-eu-central-1', region: 'eu-central-1', cpu: '3.1 GHz', status: 'Online' }
];
let nodeCounter = 4;
let chatDrawerOpen = false;
let chatMessages = [
    { sender: 'bot', text: 'Hello! Live support AI is online. How can we help you with your feature flag configuration or deployment today?' }
];

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
    initRealtimeStream();
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
        renderAddRemoveResources(evaluations['TestFlag'] || evaluations['TestF'], env);
        renderSecurityAndEnterprise(evaluations, env);
        renderTheme(evaluations['darkMode'], env);
        renderFloatingChat(evaluations['liveChatSupport'], env);
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

let lastSearchResult = null;

function renderSearch(evalData) {
    const isAiOn = evalData ? evalData.enabled : false;

    if (isAiOn) {
        searchFlagStatusBadge.className = 'badge badge-live';
        searchFlagStatusBadge.textContent = 'AI Search Active';

        searchFeatureArea.innerHTML = `
            <div class="mock-search-bar ai-active">
                <span class="ai-pill">AI</span>
                <input type="text" id="mockSearchInput" class="mock-search-input" placeholder="Ask AI: 'Filter active flags or summarize telemetry...'" onkeydown="if(event.key==='Enter') executeSearch('ai')">
                <button class="btn btn-primary btn-sm" onclick="executeSearch('ai')">🔍 Ask AI</button>
            </div>
            <div class="chip-row" style="margin-top:8px;">
                <span class="chip" onclick="applyAiSuggestion('Audit recent flag updates')">✨ Audit recent changes</span>
                <span class="chip" onclick="applyAiSuggestion('Show revenue growth trends')">✨ Show revenue trends</span>
                <span class="chip" onclick="applyAiSuggestion('Find 100% rollout flags')">✨ Find 100% rollouts</span>
            </div>
            <div id="searchResultArea" style="margin-top:8px;">
                ${lastSearchResult ? lastSearchResult : ''}
            </div>
        `;
    } else {
        searchFlagStatusBadge.className = 'badge badge-paused';
        searchFlagStatusBadge.textContent = 'Standard Search (AI Flag Off)';

        searchFeatureArea.innerHTML = `
            <div class="mock-search-bar">
                <span style="color:var(--text-muted); font-size:0.8rem;">🔍</span>
                <input type="text" id="mockSearchInput" class="mock-search-input" placeholder="Standard search by exact keyword..." onkeydown="if(event.key==='Enter') executeSearch('keyword')">
                <button class="btn btn-secondary btn-sm" onclick="executeSearch('keyword')">🔍 Search</button>
            </div>
            <div style="font-size:0.71rem; color:var(--text-muted); margin-top:4px; font-family:var(--font-mono);">
                Standard keyword search active. Enable 'betaSearch' in the console to unlock AI semantic capabilities.
            </div>
            <div id="searchResultArea" style="margin-top:8px;">
                ${lastSearchResult ? lastSearchResult : ''}
            </div>
        `;
    }
}

function applyAiSuggestion(text) {
    const input = document.getElementById('mockSearchInput');
    if (input) input.value = text;
    executeSearch('ai');
}

function executeSearch(mode) {
    const input = document.getElementById('mockSearchInput');
    const q = input ? input.value.trim() : '';
    const query = q || (mode === 'ai' ? 'Audit recent flag updates' : 'production');

    if (mode === 'ai') {
        lastSearchResult = `
            <div style="background:var(--bg-card); border:1px solid var(--border-strong); border-radius:var(--radius); padding:10px 12px; font-size:0.78rem; animation:toastSlideIn 0.15s ease;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
                    <span style="font-weight:700; font-family:var(--font-mono); font-size:0.75rem;">✨ AI Semantic Result for: "${escapeHtml(query)}"</span>
                    <button class="btn btn-secondary btn-sm" style="padding:1px 6px; font-size:0.65rem;" onclick="clearSearchResult()">✕</button>
                </div>
                <div style="color:var(--text-secondary); line-height:1.4;">
                    Identified 15 registered flags across DEV, TEST, PROD. <strong>100% Rollouts</strong>: darkMode, newDashboard. <strong>Gradual Rollouts</strong>: newCheckout (50%), liveChatSupport (50% in TEST). Deterministic hash buckets active for user.
                </div>
            </div>
        `;
        showToast(`AI Model generated answer for: "${query}"`);
    } else {
        lastSearchResult = `
            <div style="background:var(--bg-card); border:1px solid var(--border); border-radius:var(--radius); padding:10px 12px; font-size:0.78rem; animation:toastSlideIn 0.15s ease;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
                    <span style="font-weight:700; font-family:var(--font-mono); font-size:0.75rem;">Exact Keyword Matches for: "${escapeHtml(query)}"</span>
                    <button class="btn btn-secondary btn-sm" style="padding:1px 6px; font-size:0.65rem;" onclick="clearSearchResult()">✕</button>
                </div>
                <div style="color:var(--text-muted); font-family:var(--font-mono); font-size:0.72rem;">
                    [MATCH 1] flag: production_safeguards &bull; [MATCH 2] env: PROD &bull; status: 200 OK
                </div>
            </div>
        `;
        showToast(`Keyword query executed for: "${query}"`);
    }

    const container = document.getElementById('searchResultArea');
    if (container) container.innerHTML = lastSearchResult;
}

function clearSearchResult() {
    lastSearchResult = null;
    const container = document.getElementById('searchResultArea');
    if (container) container.innerHTML = '';
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
// Component 4: Resource Manager [TestFlag] (Add / Remove Buttons)
// ──────────────────────────────────────────────

function renderAddRemoveResources(evalData, env) {
    const isUnlocked = evalData ? evalData.enabled : false;

    if (isUnlocked) {
        resourceFlagStatusBadge.className = 'badge badge-live';
        resourceFlagStatusBadge.textContent = 'Active (Add/Remove Unlocked)';

        const nodeCardsHtml = demoClusterNodes.map(node => `
            <div class="resource-card" id="card-${node.id}">
                <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                    <span class="resource-card-name">${escapeHtml(node.id)}</span>
                    <button class="btn btn-secondary btn-sm" style="padding:1px 5px; font-size:0.65rem;" onclick="removeSpecificDemoNode('${escapeAttr(node.id)}')" title="Remove this node">✕</button>
                </div>
                <div class="resource-card-type">${escapeHtml(node.region)} &bull; ${escapeHtml(node.cpu)}</div>
                <div style="font-size:0.65rem; color:#ffffff; font-family:var(--font-mono); margin-top:3px;">
                    ● ${escapeHtml(node.status)}
                </div>
            </div>
        `).join('');

        resourceFeatureArea.innerHTML = `
            <div class="resource-manager-box">
                <div class="resource-toolbar">
                    <div style="display:flex; align-items:center; gap:8px;">
                        <button class="btn btn-primary btn-sm" onclick="addDemoNode()">
                            + Add Node
                        </button>
                        <button class="btn btn-secondary btn-sm" onclick="removeLatestDemoNode()" ${demoClusterNodes.length === 0 ? 'disabled' : ''}>
                            &minus; Remove Node
                        </button>
                        <button class="btn btn-secondary btn-sm" onclick="resetDemoNodes()" title="Reset nodes to default cluster baseline">
                            ↻ Reset
                        </button>
                    </div>
                    <div style="display:flex; align-items:center; gap:8px;">
                        <span class="badge badge-granted" id="nodeCountBadge">${demoClusterNodes.length} Online Nodes</span>
                        <span style="font-size:0.72rem; color:var(--text-muted); font-family:var(--font-mono);">FLAG: TestFlag ON</span>
                    </div>
                </div>

                <div class="resource-cards-grid" id="clusterNodesGrid">
                    ${demoClusterNodes.length > 0 ? nodeCardsHtml : '<div style="grid-column: 1 / -1; font-size:0.76rem; color:var(--text-muted); padding:12px; text-align:center;">No nodes in cluster. Click "+ Add Node" to scale up resources.</div>'}
                </div>

                <div style="font-size:0.71rem; font-family:var(--font-mono); color:var(--text-secondary); line-height:1.4;">
                    ✓ Dynamic cluster scaling controls unlocked by feature flag <code>TestFlag</code>. Developers can safely provision and deprovision nodes in real-time.
                </div>
            </div>
        `;
    } else {
        resourceFlagStatusBadge.className = 'badge badge-paused';
        resourceFlagStatusBadge.textContent = 'Locked (TestFlag OFF)';

        resourceFeatureArea.innerHTML = `
            <div class="resource-manager-box" style="opacity: 0.85;">
                <div class="resource-toolbar">
                    <div style="display:flex; align-items:center; gap:8px;">
                        <button class="btn btn-secondary btn-sm" disabled style="opacity:0.4; cursor:not-allowed;">
                            + Add Node (Locked)
                        </button>
                        <button class="btn btn-secondary btn-sm" disabled style="opacity:0.4; cursor:not-allowed;">
                            &minus; Remove Node (Locked)
                        </button>
                    </div>
                    <span class="badge badge-fallback">READ-ONLY CLUSTER</span>
                </div>

                <div style="background:var(--bg-card); border:1px dashed var(--border); padding:12px 14px; border-radius:var(--radius); font-size:0.75rem; color:var(--text-secondary); line-height:1.4;">
                    🔒 <strong>Add / Remove Resource buttons are currently disabled.</strong><br>
                    The feature flag <code>TestFlag</code> is turned <strong>OFF</strong> in <code>${env.toUpperCase()}</code>. Cluster scaling permissions are restricted to prevent accidental deprovisioning. Enable <code>TestFlag</code> in the console or inspector to unlock dynamic resource buttons.
                </div>
            </div>
        `;
    }
}

function addDemoNode() {
    const regions = ['us-east-1', 'us-west-2', 'eu-west-1', 'ap-southeast-1'];
    const randomRegion = regions[Math.floor(Math.random() * regions.length)];
    const newNode = {
        id: `node-${randomRegion.substring(0, 7)}-${nodeCounter++}`,
        region: randomRegion,
        cpu: (2.0 + Math.random() * 2.0).toFixed(1) + ' GHz',
        status: 'Online'
    };
    demoClusterNodes.push(newNode);
    showToast(`✓ Provisioned resource node: ${newNode.id} (${newNode.region})`);
    renderAddRemoveResources(lastEvaluations['TestFlag'] || lastEvaluations['TestF'], demoEnvSelector.value);
}

function removeLatestDemoNode() {
    if (demoClusterNodes.length === 0) {
        showToast('No resource nodes available to remove', true);
        return;
    }
    const removed = demoClusterNodes.pop();
    showToast(`&minus; Terminated resource node: ${removed.id}`);
    renderAddRemoveResources(lastEvaluations['TestFlag'] || lastEvaluations['TestF'], demoEnvSelector.value);
}

function removeSpecificDemoNode(nodeId) {
    demoClusterNodes = demoClusterNodes.filter(n => n.id !== nodeId);
    showToast(`&minus; Removed specific node: ${nodeId}`);
    renderAddRemoveResources(lastEvaluations['TestFlag'] || lastEvaluations['TestF'], demoEnvSelector.value);
}

function resetDemoNodes() {
    demoClusterNodes = [
        { id: 'node-us-east-1a', region: 'us-east-1', cpu: '2.4 GHz', status: 'Online' },
        { id: 'node-us-east-1b', region: 'us-east-1', cpu: '2.4 GHz', status: 'Online' },
        { id: 'node-eu-central-1', region: 'eu-central-1', cpu: '3.1 GHz', status: 'Online' }
    ];
    showToast('Reset cluster nodes to standard baseline.');
    renderAddRemoveResources(lastEvaluations['TestFlag'] || lastEvaluations['TestF'], demoEnvSelector.value);
}

// ──────────────────────────────────────────────
// Component 5: Enterprise Actions & Security Controls
// ──────────────────────────────────────────────

let aiSummaryOpen = false;

function renderSecurityAndEnterprise(evaluations, env) {
    const isPdfOn = evaluations['exportToPdf'] ? evaluations['exportToPdf'].enabled : false;
    const isAiSummOn = evaluations['aiTelemetrySummarizer'] ? evaluations['aiTelemetrySummarizer'].enabled : false;
    const is2faOn = evaluations['twoFactorAuth'] ? evaluations['twoFactorAuth'].enabled : false;
    const isBiometricOn = evaluations['biometricFaceUnlock'] ? evaluations['biometricFaceUnlock'].enabled : false;
    const isSsoOn = evaluations['ssoEnterpriseOkta'] ? evaluations['ssoEnterpriseOkta'].enabled : false;
    const isCollabOn = evaluations['realtimeCollaboration'] ? evaluations['realtimeCollaboration'].enabled : false;
    const isChatOn = evaluations['liveChatSupport'] ? evaluations['liveChatSupport'].enabled : false;

    const activeEnterpriseFlags = [isPdfOn, isAiSummOn, is2faOn, isBiometricOn, isSsoOn, isCollabOn, isChatOn].filter(Boolean).length;
    securityActionsStatusBadge.className = activeEnterpriseFlags > 0 ? 'badge badge-live' : 'badge badge-paused';
    securityActionsStatusBadge.textContent = `${activeEnterpriseFlags} Active Policies`;

    securityActionsArea.innerHTML = `
        <div class="feature-actions-grid">
            <!-- Action 1: Export PDF (exportToPdf) -->
            <div class="feature-action-card">
                <div class="feature-action-title">
                    <span>📄 Audit PDF Export</span>
                    <span class="badge ${isPdfOn ? 'badge-live' : 'badge-paused'}" style="font-size:0.62rem;">${isPdfOn ? 'READY' : 'OFF'}</span>
                </div>
                <div class="feature-action-desc">
                    Generates encrypted board-ready compliance reports and flag audit logs.
                </div>
                <button class="btn btn-secondary btn-sm" onclick="exportPdfReport()" ${isPdfOn ? '' : 'disabled style="opacity:0.4; cursor:not-allowed;"'}>
                    ${isPdfOn ? 'Export PDF Report' : 'Export Disabled (Flag Off)'}
                </button>
            </div>

            <!-- Action 2: AI Telemetry Summarizer (aiTelemetrySummarizer) -->
            <div class="feature-action-card">
                <div class="feature-action-title">
                    <span>✨ AI Telemetry Summary</span>
                    <span class="badge ${isAiSummOn ? 'badge-live' : 'badge-paused'}" style="font-size:0.62rem;">${isAiSummOn ? 'GENAI' : 'OFF'}</span>
                </div>
                <div class="feature-action-desc">
                    Synthesizes cluster metrics, traffic spikes, and rollout anomalies.
                </div>
                <button class="btn ${isAiSummOn ? 'btn-primary' : 'btn-secondary'} btn-sm" onclick="triggerAiTelemetrySummary()" ${isAiSummOn ? '' : 'disabled style="opacity:0.4; cursor:not-allowed;"'}>
                    ${isAiSummOn ? '✨ Generate AI Summary' : 'AI Summary (Flag Off)'}
                </button>
            </div>

            <!-- Action 3: Live Chat Support Button (liveChatSupport) -->
            <div class="feature-action-card">
                <div class="feature-action-title">
                    <span>💬 Live Chat Support</span>
                    <span class="badge ${isChatOn ? 'badge-live' : 'badge-paused'}" style="font-size:0.62rem;">${isChatOn ? 'ONLINE' : 'OFF'}</span>
                </div>
                <div class="feature-action-desc">
                    Instant 24/7 technical assistance and intelligent query resolution.
                </div>
                <button class="btn btn-secondary btn-sm" onclick="openChatFromButton()" ${isChatOn ? '' : 'disabled style="opacity:0.4; cursor:not-allowed;"'}>
                    ${isChatOn ? '💬 Open Live Chat' : 'Chat Offline (Flag Off)'}
                </button>
            </div>

            <!-- Action 4: Security Identity & Access (twoFactorAuth / biometricFaceUnlock / ssoEnterpriseOkta) -->
            <div class="feature-action-card">
                <div class="feature-action-title">
                    <span>🔐 Enterprise Auth</span>
                    <span class="badge ${is2faOn || isBiometricOn || isSsoOn ? 'badge-live' : 'badge-paused'}" style="font-size:0.62rem;">
                        ${is2faOn ? '2FA' : (isSsoOn ? 'SSO' : (isBiometricOn ? 'BIO' : 'BASIC'))}
                    </span>
                </div>
                <div class="feature-action-desc">
                    Multi-factor authentication, hardware WebAuthn passkeys, and Okta SAML.
                </div>
                <div style="display:flex; gap:6px; flex-wrap:wrap;">
                    <button class="btn btn-secondary btn-sm" onclick="verifyTwoFactor()" ${is2faOn ? '' : 'disabled style="opacity:0.4; cursor:not-allowed;"'} title="twoFactorAuth flag">
                        ${is2faOn ? 'Verify 2FA' : '2FA (Off)'}
                    </button>
                    <button class="btn btn-secondary btn-sm" onclick="authenticateBiometric()" ${isBiometricOn ? '' : 'disabled style="opacity:0.4; cursor:not-allowed;"'} title="biometricFaceUnlock flag">
                        ${isBiometricOn ? 'Passkey' : 'Passkey (Off)'}
                    </button>
                    <button class="btn btn-secondary btn-sm" onclick="launchSsoOkta()" ${isSsoOn ? '' : 'disabled style="opacity:0.4; cursor:not-allowed;"'} title="ssoEnterpriseOkta flag">
                        ${isSsoOn ? 'Okta SSO' : 'SSO (Off)'}
                    </button>
                </div>
            </div>
        </div>

        ${aiSummaryOpen ? `
            <div style="margin-top:10px; background:var(--bg-surface); border:1px solid var(--border-strong); border-radius:var(--radius); padding:14px; animation:toastSlideIn 0.2s ease;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
                    <span style="font-family:var(--font-mono); font-size:0.8rem; font-weight:700;">✨ AI Telemetry Intelligence Briefing</span>
                    <button class="btn btn-secondary btn-sm" style="padding:1px 6px; font-size:0.68rem;" onclick="toggleAiSummaryBox()">✕ Close</button>
                </div>
                <p style="font-size:0.77rem; color:var(--text-secondary); line-height:1.45; margin:0;">
                    <strong>Executive Synthesis:</strong> System uptime over the past 30 days remains exceptional at <strong>99.98%</strong> with peak throughput reaching <strong>14.2K requests/second</strong> on Sunday. Express checkout rollout has driven a <strong>+24.8% increase in conversion velocity</strong>. No critical anomalies or rate-limit violations observed.
                </p>
            </div>
        ` : ''}
    `;
}

function exportPdfReport() {
    showToast('📄 Audit Report PDF generated! Download initiated (42 KB).');
}

function triggerAiTelemetrySummary() {
    aiSummaryOpen = !aiSummaryOpen;
    renderSecurityAndEnterprise(lastEvaluations, demoEnvSelector.value);
    showToast(aiSummaryOpen ? '✨ AI Telemetry Summary synthesized in 12ms.' : 'AI Summary hidden.');
}

function toggleAiSummaryBox() {
    aiSummaryOpen = false;
    renderSecurityAndEnterprise(lastEvaluations, demoEnvSelector.value);
}

function verifyTwoFactor() {
    showToast('🔐 2FA TOTP token verified. Hardware session certified.');
}

function authenticateBiometric() {
    showToast('👁 WebAuthn biometric passkey validated in 6ms.');
}

function launchSsoOkta() {
    showToast('🏢 Authenticated via Okta Enterprise SSO (SAML 2.0).');
}

function openChatFromButton() {
    chatDrawerOpen = true;
    renderFloatingChat(lastEvaluations['liveChatSupport'], demoEnvSelector.value);
    setTimeout(() => {
        const inp = document.getElementById('chatInputBox');
        if (inp) inp.focus();
    }, 100);
}

// ──────────────────────────────────────────────
// Component 6: Floating Support Chat Widget (liveChatSupport)
// ──────────────────────────────────────────────

function renderFloatingChat(evalData, env) {
    const isChatOn = evalData ? evalData.enabled : false;

    if (!isChatOn) {
        floatingChatWidget.innerHTML = `
            <div style="position:fixed; bottom:20px; right:20px; font-size:0.72rem; color:var(--text-muted); background:var(--bg-surface); border:1px solid var(--border); padding:6px 12px; border-radius:999px; z-index:1500; font-family:var(--font-mono); opacity:0.75;">
                Live Chat Offline (liveChatSupport OFF)
            </div>
        `;
        return;
    }

    const messagesHtml = chatMessages.map(m => `
        <div class="chat-msg ${m.sender === 'user' ? 'chat-msg-user' : 'chat-msg-bot'}">
            ${escapeHtml(m.text)}
        </div>
    `).join('');

    floatingChatWidget.innerHTML = `
        <!-- Floating Chat Button -->
        <button class="floating-chat-btn" id="floatingChatBtn" onclick="toggleChatDrawer()" title="Click to toggle live support chat">
            <span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:#000000; box-shadow:0 0 6px #000000;"></span>
            <span>💬 Live Support Chat</span>
        </button>

        <!-- Floating Chat Drawer -->
        ${chatDrawerOpen ? `
            <div class="chat-widget-drawer" id="chatDrawer">
                <div class="chat-widget-header">
                    <div class="chat-widget-title">
                        <span>💬</span>
                        <span>Apex Instant Support AI</span>
                    </div>
                    <button class="btn btn-secondary btn-sm" style="padding:2px 8px; font-size:0.7rem;" onclick="toggleChatDrawer()">✕</button>
                </div>

                <div class="chat-widget-body" id="chatBody">
                    ${messagesHtml}
                </div>

                <div class="chat-widget-input-row">
                    <input type="text" id="chatInputBox" class="chat-widget-input" placeholder="Type a message..." onkeydown="if(event.key==='Enter') sendChatMessage()">
                    <button class="btn btn-primary btn-sm" onclick="sendChatMessage()">Send</button>
                </div>
            </div>
        ` : ''}
    `;

    if (chatDrawerOpen) {
        setTimeout(() => {
            const body = document.getElementById('chatBody');
            if (body) body.scrollTop = body.scrollHeight;
        }, 50);
    }
}

function toggleChatDrawer() {
    chatDrawerOpen = !chatDrawerOpen;
    renderFloatingChat(lastEvaluations['liveChatSupport'], demoEnvSelector.value);
    if (chatDrawerOpen) {
        setTimeout(() => {
            const inp = document.getElementById('chatInputBox');
            if (inp) inp.focus();
        }, 80);
    }
}

function sendChatMessage() {
    const input = document.getElementById('chatInputBox');
    if (!input) return;
    const text = input.value.trim();
    if (!text) return;

    chatMessages.push({ sender: 'user', text });
    input.value = '';
    renderFloatingChat(lastEvaluations['liveChatSupport'], demoEnvSelector.value);

    // Automated smart bot reply
    setTimeout(() => {
        let reply = "I understand your query! All feature flags for this session are currently synchronized with the FeatureFlagLite API.";
        const lower = text.toLowerCase();
        if (lower.includes('flag') || lower.includes('toggle')) {
            reply = "You can toggle any flag in real time using the Decision Engine Inspector on the right, or via the Management Console.";
        } else if (lower.includes('search') || lower.includes('find')) {
            reply = "Search behavior is managed by the 'betaSearch' flag. When enabled, it provides AI semantic query capabilities.";
        } else if (lower.includes('node') || lower.includes('add') || lower.includes('remove') || lower.includes('resource')) {
            reply = "Cluster scaling controls are managed by 'TestFlag'. Turn it ON to unlock the dynamic '+ Add Node' and '− Remove Node' toolbar.";
        } else if (lower.includes('checkout') || lower.includes('buy') || lower.includes('pay')) {
            reply = "Checkout flow adapts based on 'newCheckout' percentage rollout. Users in the active rollout tier receive 1-click buy.";
        }

        chatMessages.push({ sender: 'bot', text: reply });
        renderFloatingChat(lastEvaluations['liveChatSupport'], demoEnvSelector.value);
    }, 350);
}

// ──────────────────────────────────────────────
// Component 7: Theme Engine (darkMode)
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
    const standardFlags = new Set([
        'newDashboard', 'darkMode', 'newCheckout', 'betaSearch',
        'TestFlag', 'liveChatSupport', 'exportToPdf', 'aiTelemetrySummarizer',
        'twoFactorAuth', 'biometricFaceUnlock', 'ssoEnterpriseOkta', 'realtimeCollaboration'
    ]);
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

// ──────────────────────────────────────────────
// Real-Time Event Streaming & Auto-Sync Engine
// ──────────────────────────────────────────────

function initRealtimeStream() {
    // 1. Establish Server-Sent Events (SSE) connection
    if (window.EventSource) {
        setupSseConnection();
    } else {
        updateRealtimeIndicator(false, 'SSE Unsupported (Polling Active)');
    }

    // 2. Setup BroadcastChannel for zero-latency local tab synchronization
    try {
        if ('BroadcastChannel' in window) {
            realtimeBroadcastChannel = new BroadcastChannel('featureflaglite_channel');
            realtimeBroadcastChannel.onmessage = (event) => {
                if (event.data) {
                    handleRealtimeFlagEvent(event.data, 'Local Bus');
                }
            };
        }
    } catch (e) {
        console.warn('BroadcastChannel not initialized:', e);
    }

    // 3. Robust polling fallback every 2.5 seconds to guarantee synchronization
    setInterval(pollForChanges, 2500);
}

function setupSseConnection() {
    if (realtimeEventSource) {
        try { realtimeEventSource.close(); } catch (e) {}
    }

    const sseUrl = `${API_BASE}/flags/stream`;
    try {
        realtimeEventSource = new EventSource(sseUrl);

        realtimeEventSource.addEventListener('connected', (e) => {
            updateRealtimeIndicator(true, 'Live SSE Stream • Connected');
        });

        realtimeEventSource.addEventListener('flag-update', (e) => {
            try {
                const data = JSON.parse(e.data);
                handleRealtimeFlagEvent(data, 'SSE Stream');
            } catch (err) {
                console.error('Error parsing SSE event:', err);
            }
        });

        realtimeEventSource.onerror = () => {
            updateRealtimeIndicator(false, 'Stream Reconnecting...');
        };
    } catch (e) {
        console.warn('Could not establish EventSource:', e);
        updateRealtimeIndicator(false, 'Polling Fallback Active');
    }
}

function handleRealtimeFlagEvent(eventData, source) {
    if (!realtimeAutoSync) return;

    realtimeEventCount++;
    const currentEnv = demoEnvSelector.value.toLowerCase();
    const eventEnv = (eventData.environment || '*').toLowerCase();
    const flagName = eventData.flagName || 'System Flag';
    const action = eventData.action || 'UPDATED';

    updateRealtimeIndicator(true, `Live Stream • ${realtimeEventCount} event${realtimeEventCount > 1 ? 's' : ''}`);

    // Check if event targets current environment or all environments
    if (eventEnv === '*' || eventEnv === currentEnv) {
        showRealtimeBanner(flagName, action, eventEnv === '*' ? currentEnv : eventEnv, eventData);
        flashAffectedComponent(flagName);
        refreshDemoApp();
    }
}

function showRealtimeBanner(flagName, action, env, data) {
    if (!realtimeEventBanner) return;

    if (bannerDismissTimeout) {
        clearTimeout(bannerDismissTimeout);
    }

    let actionLabel = 'updated';
    if (action === 'STATE_CHANGED') {
        const isEnabled = data.details && data.details.enabled !== undefined ? data.details.enabled : null;
        actionLabel = isEnabled !== null ? (isEnabled ? 'switched ON' : 'switched OFF') : 'state changed';
    } else if (action === 'FLAG_CREATED') {
        actionLabel = 'created';
    } else if (action === 'FLAG_DELETED') {
        actionLabel = 'deleted';
    }

    if (realtimeBannerText) {
        realtimeBannerText.innerHTML = `<strong>Live Update:</strong> Flag <code>${escapeHtml(flagName)}</code> ${escapeHtml(actionLabel)} in <strong>${escapeHtml(env.toUpperCase())}</strong> &bull; Client simulation adapted in real-time.`;
    }
    if (realtimeBannerTime) {
        realtimeBannerTime.textContent = new Date().toLocaleTimeString();
    }

    realtimeEventBanner.style.display = 'flex';

    bannerDismissTimeout = setTimeout(() => {
        if (realtimeEventBanner) {
            realtimeEventBanner.style.display = 'none';
        }
    }, 4500);
}

function flashAffectedComponent(flagName) {
    let targetElement = null;

    if (flagName === 'betaSearch') {
        targetElement = searchFeatureArea;
    } else if (flagName === 'newDashboard') {
        targetElement = dashboardFeatureArea;
    } else if (flagName === 'newCheckout') {
        targetElement = checkoutFeatureArea;
    } else if (flagName === 'TestFlag' || flagName === 'TestF') {
        targetElement = resourceFeatureArea;
    } else if (flagName === 'liveChatSupport') {
        targetElement = floatingChatWidget || securityActionsArea;
    } else if (flagName === 'darkMode') {
        targetElement = themeFeatureArea;
    } else if (['twoFactorAuth', 'biometricFaceUnlock', 'ssoEnterpriseOkta', 'exportToPdf', 'aiTelemetrySummarizer'].includes(flagName)) {
        targetElement = securityActionsArea;
    } else {
        targetElement = customFlagsArea;
    }

    if (targetElement) {
        targetElement.classList.remove('realtime-highlight-flash');
        void targetElement.offsetWidth; // trigger reflow
        targetElement.classList.add('realtime-highlight-flash');
    }
}

function updateRealtimeIndicator(isConnected, label) {
    if (realtimeStatusDot) {
        realtimeStatusDot.className = isConnected ? 'status-dot' : 'status-dot status-dot-offline';
    }
    if (realtimeStatusText) {
        realtimeStatusText.textContent = label;
    }
}

function toggleRealtimeSync() {
    realtimeAutoSync = !realtimeAutoSync;
    if (realtimeToggleBtn) {
        if (realtimeAutoSync) {
            realtimeToggleBtn.className = 'btn btn-primary btn-sm';
            realtimeToggleBtn.textContent = '⚡ Realtime: ON';
            showToast('Real-time auto-sync activated. UI will adapt instantly on flag changes.');
            refreshDemoApp();
        } else {
            realtimeToggleBtn.className = 'btn btn-secondary btn-sm';
            realtimeToggleBtn.textContent = '⚡ Realtime: PAUSED';
            showToast('Real-time auto-sync paused. Manual refresh required.');
        }
    }
}

async function pollForChanges() {
    if (!realtimeAutoSync) return;
    const env = demoEnvSelector ? demoEnvSelector.value : 'dev';

    try {
        const flagsData = await apiFetch(`/flags?environment=${env}`);
        const currentHash = JSON.stringify(flagsData.flags);

        if (lastPolledFlagsHash && lastPolledFlagsHash !== currentHash) {
            // State changed since last poll
            lastPolledFlagsHash = currentHash;
            handleRealtimeFlagEvent({
                action: 'STATE_CHANGED',
                flagName: 'Environment Flags',
                environment: env
            }, 'Heartbeat Poll');
        } else {
            lastPolledFlagsHash = currentHash;
        }
    } catch (e) {
        // Silently swallow background poll errors
    }
}

