/**
 * FeatureFlagLite — Developer & Admin Console Logic
 * Pure Monochrome • Human-Centered UX Writing • Swiss Typography
 */

const API_BASE = '/api/v1';

// Global Console State
let currentFlags = [];
let currentFlagStates = [];
let activeEnv = 'dev';
let activeFilter = 'all';
let activeAuditEnvFilter = 'all';
let currentOperator = localStorage.getItem('featureflag_operator') || 'admin';
let cachedAuditLogs = [];

// DOM References
const globalEnvSelect = document.getElementById('globalEnvSelect');
const currentEnvDisplay = document.getElementById('currentEnvDisplay');
const flagsTableBody = document.getElementById('flagsTableBody');
const flagSearchInput = document.getElementById('flagSearchInput');
const statTotalFlags = document.getElementById('statTotalFlags');
const statEnabled = document.getElementById('statEnabled');
const statRollouts = document.getElementById('statRollouts');
const statDisabled = document.getElementById('statDisabled');

// User Simulator DOM References
const simUserId = document.getElementById('simUserId');
const simUserTitle = document.getElementById('simUserTitle');
const simUserSubtitle = document.getElementById('simUserSubtitle');
const simActiveCountBadge = document.getElementById('simActiveCountBadge');
const userMatrixTableBody = document.getElementById('userMatrixTableBody');
const cohortTableHead = document.getElementById('cohortTableHead');
const cohortTableBody = document.getElementById('cohortTableBody');

// Operations DOM References
const syncSourceEnv = document.getElementById('syncSourceEnv');
const syncTargetEnv = document.getElementById('syncTargetEnv');
const killswitchEnv = document.getElementById('killswitchEnv');
const operatorIdInput = document.getElementById('operatorIdInput');

// Audit Feed DOM References
const auditActivityFeed = document.getElementById('auditActivityFeed');
const auditSearchInput = document.getElementById('auditSearchInput');

// Modal References
const editModal = document.getElementById('editModal');
const editForm = document.getElementById('editForm');
const editFlagName = document.getElementById('editFlagName');
const editFlagDisplay = document.getElementById('editFlagDisplay');
const editEnvDisplay = document.getElementById('editEnvDisplay');
const editEnabled = document.getElementById('editEnabled');
const editRolloutSlider = document.getElementById('editRolloutSlider');
const editRollout = document.getElementById('editRollout');
const editRolloutValue = document.getElementById('editRolloutValue');
const editLiveExplanation = document.getElementById('editLiveExplanation');

const createModal = document.getElementById('createModal');
const createFlagForm = document.getElementById('createFlagForm');
const historyModal = document.getElementById('historyModal');
const historyModalFlagName = document.getElementById('historyModalFlagName');
const flagHistoryTableBody = document.getElementById('flagHistoryTableBody');

// ──────────────────────────────────────────────
// Initialization
// ──────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

function initApp() {
    operatorIdInput.value = currentOperator;
    setupNavigationTabs();
    setupEventListeners();
    loadFlags();
}

function setupNavigationTabs() {
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const targetId = tab.dataset.tab;
            tabs.forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

            tab.classList.add('active');
            const targetContent = document.getElementById(targetId);
            if (targetContent) {
                targetContent.classList.add('active');
            }

            // Lazy load tab data
            if (targetId === 'tab-users') {
                simulateUser();
                loadCohortComparison();
            } else if (targetId === 'tab-audit') {
                loadAuditStream();
            }
        });
    });
}

function setupEventListeners() {
    // Environment selector
    globalEnvSelect.addEventListener('change', () => {
        activeEnv = globalEnvSelect.value;
        currentEnvDisplay.textContent = activeEnv.toUpperCase();
        loadFlags();

        // Refresh user diagnostic if on users tab
        const activeTab = document.querySelector('.tab-btn.active');
        if (activeTab && activeTab.dataset.tab === 'tab-users') {
            simulateUser();
            loadCohortComparison();
        }
    });

    // Slider sync and live human explanation
    editRolloutSlider.addEventListener('input', () => {
        editRollout.value = editRolloutSlider.value;
        editRolloutValue.textContent = editRolloutSlider.value + '%';
        updateRolloutExplanation();
    });

    editRollout.addEventListener('input', () => {
        const val = Math.min(100, Math.max(0, parseInt(editRollout.value) || 0));
        editRolloutSlider.value = val;
        editRolloutValue.textContent = val + '%';
        updateRolloutExplanation();
    });

    editForm.addEventListener('submit', handleUpdateStateSubmit);
    createFlagForm.addEventListener('submit', handleCreateFlagSubmit);

    // Close modals on escape key or backdrop click
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeEditModal();
            closeCreateModal();
            closeHistoryModal();
        }
    });

    [editModal, createModal, historyModal].forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        });
    });
}

// ──────────────────────────────────────────────
// API Helper
// ──────────────────────────────────────────────

async function apiFetch(path, options = {}) {
    try {
        const response = await fetch(API_BASE + path, {
            headers: { 'Content-Type': 'application/json', ...options.headers },
            ...options
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            const message = errorData.message || `Unable to complete request (HTTP ${response.status})`;
            throw new Error(message);
        }

        if (response.status === 204) return null;
        return await response.json();
    } catch (error) {
        showToast(error.message, true);
        throw error;
    }
}

// ──────────────────────────────────────────────
// Tab 1: Flag Directory Logic
// ──────────────────────────────────────────────

async function loadFlags() {
    try {
        const [flags, envData] = await Promise.all([
            apiFetch('/flags'),
            apiFetch(`/flags?environment=${activeEnv}`)
        ]);

        currentFlags = flags || [];
        const envFlagsMap = envData.flags || {};

        // Fetch states for per-flag rollout percentages
        const statePromises = currentFlags.map(flag =>
            apiFetch(`/flags/${flag.name}/states`).catch(() => [])
        );
        const allStatesNested = await Promise.all(statePromises);
        currentFlagStates = allStatesNested.flat();

        renderFlagsTable();
        updateMetricCards(envFlagsMap);
    } catch (error) {
        flagsTableBody.innerHTML = `
            <tr>
                <td colspan="5" class="table-loading" style="color: #ff9999;">
                    System connection unavailable. The server may be restarting or offline.
                    <div style="margin-top: 8px;">
                        <button class="btn btn-secondary btn-sm" onclick="loadFlags()">Retry Connection</button>
                    </div>
                </td>
            </tr>
        `;
    }
}

function updateMetricCards(envFlagsMap) {
    const total = currentFlags.length;
    let enabledCount = 0;
    let rolloutCount = 0;
    let disabledCount = 0;

    currentFlags.forEach(flag => {
        const isEnabled = envFlagsMap[flag.name] ?? flag.defaultState;
        const state = currentFlagStates.find(s => s.flagName === flag.name && s.environment === activeEnv);
        const rollout = state ? state.rolloutPercentage : (isEnabled ? 100 : 0);

        if (!isEnabled || rollout === 0) {
            disabledCount++;
        } else if (rollout === 100) {
            enabledCount++;
        } else {
            rolloutCount++;
        }
    });

    statTotalFlags.textContent = total;
    statEnabled.textContent = enabledCount;
    statRollouts.textContent = rolloutCount;
    statDisabled.textContent = disabledCount;
}

function renderFlagsTable() {
    let filtered = [...currentFlags];

    // Search query filter
    const query = (flagSearchInput.value || '').trim().toLowerCase();
    if (query) {
        filtered = filtered.filter(f =>
            f.name.toLowerCase().includes(query) ||
            (f.description && f.description.toLowerCase().includes(query))
        );
    }

    // Status chip filter
    if (activeFilter !== 'all') {
        filtered = filtered.filter(f => {
            const state = currentFlagStates.find(s => s.flagName === f.name && s.environment === activeEnv);
            const isEnabled = state ? state.enabled : f.defaultState;
            const rollout = state ? state.rolloutPercentage : (isEnabled ? 100 : 0);

            if (activeFilter === 'live') return isEnabled && rollout === 100;
            if (activeFilter === 'gradual') return isEnabled && rollout > 0 && rollout < 100;
            if (activeFilter === 'paused') return !isEnabled || rollout === 0;
            return true;
        });
    }

    if (filtered.length === 0) {
        flagsTableBody.innerHTML = `
            <tr>
                <td colspan="5" class="table-loading">
                    No feature flags match your criteria.
                    <div style="margin-top: 6px; font-size: 0.78rem; color: var(--text-muted);">
                        Try adjusting your search terms or filter selection.
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    flagsTableBody.innerHTML = filtered.map(flag => {
        const state = currentFlagStates.find(s => s.flagName === flag.name && s.environment === activeEnv);
        const isEnabled = state ? state.enabled : flag.defaultState;
        const rollout = state ? state.rolloutPercentage : (isEnabled ? 100 : 0);

        let statusBadge;
        let trafficText;
        let nextStepText;

        if (!isEnabled || rollout === 0) {
            statusBadge = '<span class="badge badge-paused">Paused (0%)</span>';
            trafficText = '<span style="color:var(--text-muted);">0% Traffic • Fallback Active</span>';
            nextStepText = '<span class="table-next-step">Safely held in reserve. Click <strong>Configure</strong> to enable.</span>';
        } else if (rollout === 100) {
            statusBadge = '<span class="badge badge-live">Live (100%)</span>';
            trafficText = '<strong>100% of Users</strong>';
            nextStepText = '<span class="table-next-step">Full release active. Ready for Staging/Prod promotion.</span>';
        } else {
            statusBadge = `<span class="badge badge-gradual">${rollout}% Rollout</span>`;
            trafficText = `<strong>${rollout}% of Hash Buckets</strong>`;
            nextStepText = `<span class="table-next-step">A/B Testing. Verify error metrics before expanding to 100%.</span>`;
        }

        return `
            <tr>
                <td>
                    <div class="table-flag-title">${escapeHtml(flag.name)}</div>
                    <div class="table-flag-desc">${escapeHtml(flag.description || 'No description provided.')}</div>
                </td>
                <td>${statusBadge}</td>
                <td>${trafficText}</td>
                <td>${nextStepText}</td>
                <td style="text-align: right;">
                    <div class="btn-group" style="justify-content: flex-end;">
                        <button class="btn btn-secondary btn-sm" onclick="openEditModal('${escapeAttr(flag.name)}', ${isEnabled}, ${rollout})">
                            Configure
                        </button>
                        <button class="btn btn-secondary btn-sm" onclick="jumpToUserTest('${escapeAttr(flag.name)}')">
                            Test User
                        </button>
                        <button class="btn btn-secondary btn-sm" onclick="openHistoryModal('${escapeAttr(flag.name)}')">
                            History
                        </button>
                        <button class="btn btn-danger btn-sm" onclick="confirmDeleteFlag(${flag.id}, '${escapeAttr(flag.name)}')">
                            Retire
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function handleSearch() {
    renderFlagsTable();
}

function setFilter(filter) {
    activeFilter = filter;
    document.querySelectorAll('.filter-chip').forEach(chip => {
        chip.classList.toggle('active', chip.dataset.filter === filter);
    });
    renderFlagsTable();
}

// ──────────────────────────────────────────────
// Tab 2: User Simulator & Targeting Logic
// ──────────────────────────────────────────────

function selectPersona(userId, label) {
    simUserId.value = userId;
    document.querySelectorAll('.chip').forEach(c => {
        c.classList.toggle('active', c.textContent.includes(userId));
    });
    simulateUser();
    showToast(`Persona loaded: "${label}" (${userId})`);
}

function jumpToUserTest(flagName) {
    const userTab = document.querySelector('[data-tab="tab-users"]');
    if (userTab) userTab.click();
    showToast(`Diagnosing feature availability for "${flagName}"`);
}

async function simulateUser() {
    const userId = (simUserId.value || 'user-123').trim();
    simUserTitle.textContent = `Diagnostic for "${userId}" in ${activeEnv.toUpperCase()}`;
    simUserSubtitle.textContent = `Deterministic hash slot calculated per flag. Zero random drift.`;

    try {
        const results = await apiFetch(`/flags/system/evaluate-all?environment=${activeEnv}&userId=${encodeURIComponent(userId)}`);

        let activeCount = 0;
        userMatrixTableBody.innerHTML = results.map(item => {
            if (item.enabled) activeCount++;

            const flag = currentFlags.find(f => f.name === item.flagName) || {};
            const experienceBadge = item.enabled
                ? '<span class="badge badge-granted">✓ Granted Access</span>'
                : '<span class="badge badge-fallback">✗ Standard Fallback</span>';

            const slotText = item.rolloutPercentage > 0 && item.rolloutPercentage < 100
                ? `<span class="text-mono" style="font-size:0.8rem;">Slot <strong>${item.bucket}</strong> / ${item.rolloutPercentage}%</span>`
                : `<span class="text-mono" style="font-size:0.8rem;">${item.rolloutPercentage}% Fixed</span>`;

            return `
                <tr>
                    <td>
                        <div class="table-flag-title">${escapeHtml(item.flagName)}</div>
                        <div class="table-flag-desc">${escapeHtml(flag.description || '')}</div>
                    </td>
                    <td>${experienceBadge}</td>
                    <td>${slotText}</td>
                    <td>
                        <div style="font-size:0.79rem; color:var(--text-secondary); line-height:1.4;">
                            ${escapeHtml(item.explanation)}
                        </div>
                    </td>
                    <td style="text-align: right;">
                        <button class="btn btn-secondary btn-sm" onclick="openEditModal('${escapeAttr(item.flagName)}', ${item.rolloutPercentage > 0}, ${item.rolloutPercentage})">
                            Adjust
                        </button>
                    </td>
                </tr>
            `;
        }).join('');

        simActiveCountBadge.textContent = `${activeCount} of ${results.length} Features Active`;
    } catch (error) {
        userMatrixTableBody.innerHTML = `
            <tr>
                <td colspan="5" class="table-loading" style="color: #ff9999;">
                    Diagnostic evaluation failed. Ensure the backend server is operational.
                </td>
            </tr>
        `;
    }
}

async function loadCohortComparison() {
    const sampleUsers = ['developer-alice', 'qa-bob', 'enterprise-vip', 'beta-tester', 'guest-99'];

    cohortTableHead.innerHTML = `
        <tr>
            <th style="width: 25%;">Feature Flag</th>
            ${sampleUsers.map(u => `<th style="text-align:center;">${u.split('-')[0]}<br><span style="font-size:0.65rem; color:var(--text-muted);">${u}</span></th>`).join('')}
        </tr>
    `;

    try {
        const userPromises = sampleUsers.map(u =>
            apiFetch(`/flags/system/evaluate-all?environment=${activeEnv}&userId=${encodeURIComponent(u)}`)
        );
        const resultsArray = await Promise.all(userPromises);

        cohortTableBody.innerHTML = currentFlags.map(flag => {
            const userCells = sampleUsers.map((u, idx) => {
                const userResult = resultsArray[idx].find(r => r.flagName === flag.name);
                const isGranted = userResult ? userResult.enabled : false;
                const slot = userResult ? userResult.bucket : 0;

                return `
                    <td style="text-align:center;">
                        <span class="badge ${isGranted ? 'badge-granted' : 'badge-fallback'}" style="font-size:0.65rem;">
                            ${isGranted ? 'ACTIVE' : 'OFF'}
                        </span>
                        <div style="font-size:0.65rem; color:var(--text-muted); font-family:var(--font-mono); margin-top:2px;">
                            Slot ${slot}
                        </div>
                    </td>
                `;
            }).join('');

            return `
                <tr>
                    <td>
                        <span class="table-flag-title">${escapeHtml(flag.name)}</span>
                    </td>
                    ${userCells}
                </tr>
            `;
        }).join('');
    } catch (error) {
        cohortTableBody.innerHTML = `<tr><td colspan="6" class="table-loading">Could not calculate cohort distribution.</td></tr>`;
    }
}

// ──────────────────────────────────────────────
// Tab 3: Environment Operations Logic
// ──────────────────────────────────────────────

async function executeEnvironmentSync() {
    const sourceEnv = syncSourceEnv.value;
    const targetEnv = syncTargetEnv.value;

    if (sourceEnv === targetEnv) {
        showToast('Source and Target environments cannot be the same.', true);
        return;
    }

    const message = `Are you sure you want to promote all flag settings from ${sourceEnv.toUpperCase()} to ${targetEnv.toUpperCase()}?\n\nThis will synchronize ${currentFlags.length} feature flag states and rollout percentages with full audit attribution.`;
    if (!confirm(message)) return;

    try {
        const result = await apiFetch(`/flags/system/sync?sourceEnv=${sourceEnv}&targetEnv=${targetEnv}&changedBy=${encodeURIComponent(currentOperator)}`, {
            method: 'POST'
        });

        showToast(result.message || `Promoted ${result.syncedCount} flags from ${sourceEnv.toUpperCase()} to ${targetEnv.toUpperCase()}`);
        loadFlags();
    } catch (error) {
        // Handled in apiFetch
    }
}

async function executeKillswitch() {
    const env = killswitchEnv.value;
    const message = `⚠️ EMERGENCY KILLSWITCH CONFIRMATION\n\nYou are about to pause all active feature flags in ${env.toUpperCase()}.\nAll users in ${env.toUpperCase()} will immediately revert to safe baseline behaviors.\n\nType OK to confirm incident mitigation:`;

    const userPrompt = prompt(message);
    if (userPrompt !== 'OK' && userPrompt !== 'ok') {
        showToast('Emergency pause cancelled by operator.');
        return;
    }

    try {
        const result = await apiFetch(`/flags/system/killswitch?environment=${env}&changedBy=${encodeURIComponent(currentOperator + '-killswitch')}`, {
            method: 'POST'
        });

        showToast(`Emergency Pause Executed: ${result.pausedCount} flags safely paused in ${env.toUpperCase()}`);
        loadFlags();
    } catch (error) {
        // Handled in apiFetch
    }
}

async function executeCachePurge() {
    try {
        const result = await apiFetch('/flags/system/cache/purge', { method: 'POST' });
        showToast(result.message || 'All in-memory caches have been purged.');
    } catch (error) {
        // Handled in apiFetch
    }
}

function saveOperatorId() {
    const val = operatorIdInput.value.trim() || 'admin';
    currentOperator = val;
    localStorage.setItem('featureflag_operator', val);
    showToast(`Operator identity updated to "${val}". Future changes will be attributed accordingly.`);
}

// ──────────────────────────────────────────────
// Tab 4: System Activity & Audit Stream Logic
// ──────────────────────────────────────────────

async function loadAuditStream() {
    try {
        const logs = await apiFetch('/flags/system/audit');
        cachedAuditLogs = logs || [];
        renderAuditStream();
    } catch (error) {
        auditActivityFeed.innerHTML = `
            <div class="activity-card" style="color:#ff9999; text-align:center;">
                Unable to retrieve system audit records.
            </div>
        `;
    }
}

function setAuditEnvFilter(env) {
    activeAuditEnvFilter = env;
    document.querySelectorAll('[data-audit-env]').forEach(chip => {
        chip.classList.toggle('active', chip.dataset.auditEnv === env);
    });
    renderAuditStream();
}

function filterAuditStream() {
    renderAuditStream();
}

function renderAuditStream() {
    let filtered = [...cachedAuditLogs];

    // Environment filter
    if (activeAuditEnvFilter !== 'all') {
        filtered = filtered.filter(l => l.environment.toLowerCase() === activeAuditEnvFilter.toLowerCase());
    }

    // Text search query
    const query = (auditSearchInput.value || '').trim().toLowerCase();
    if (query) {
        filtered = filtered.filter(l =>
            l.flagName.toLowerCase().includes(query) ||
            l.changedBy.toLowerCase().includes(query) ||
            l.environment.toLowerCase().includes(query)
        );
    }

    if (filtered.length === 0) {
        auditActivityFeed.innerHTML = `
            <div class="activity-card" style="text-align:center; color:var(--text-muted); padding:28px;">
                No audit records found matching your filters.
            </div>
        `;
        return;
    }

    auditActivityFeed.innerHTML = filtered.map(log => {
        const isTurnedOn = !log.oldEnabled && log.newEnabled;
        const isTurnedOff = log.oldEnabled && !log.newEnabled;
        const isRolloutShift = log.oldRolloutPercentage !== log.newRolloutPercentage;

        let humanActionTitle;
        let humanExplanation;

        if (log.oldEnabled === null) {
            humanActionTitle = `Initial registration of flag state`;
            humanExplanation = `Baseline created for ${log.flagName} in ${log.environment.toUpperCase()} with ${log.newRolloutPercentage}% rollout.`;
        } else if (isTurnedOff) {
            humanActionTitle = `${log.changedBy} safely paused "${log.flagName}"`;
            humanExplanation = `Feature disabled in ${log.environment.toUpperCase()}. Customer traffic reverted to standard fallback experience.`;
        } else if (isTurnedOn) {
            humanActionTitle = `${log.changedBy} enabled "${log.flagName}" to ${log.newRolloutPercentage}%`;
            humanExplanation = `Feature activated in ${log.environment.toUpperCase()} for customer evaluation.`;
        } else if (isRolloutShift) {
            humanActionTitle = `${log.changedBy} adjusted rollout reach (${log.oldRolloutPercentage}% → ${log.newRolloutPercentage}%)`;
            humanExplanation = `Traffic allocation modified for "${log.flagName}" in ${log.environment.toUpperCase()}.`;
        } else {
            humanActionTitle = `${log.changedBy} refreshed configuration for "${log.flagName}"`;
            humanExplanation = `Configuration verified with no net change in state (${log.newRolloutPercentage}%).`;
        }

        return `
            <div class="activity-card">
                <div class="activity-meta">
                    <span class="activity-env-pill text-mono">${log.environment.toUpperCase()}</span>
                    <span class="activity-time text-mono">${formatDateHuman(log.changedAt)}</span>
                </div>
                <div class="activity-title">${humanActionTitle}</div>
                <div class="activity-explanation">${humanExplanation}</div>
                <div class="activity-diff">
                    State: ${log.oldEnabled ? 'ON' : 'OFF'} ➔ ${log.newEnabled ? 'ON' : 'OFF'} &nbsp;•&nbsp;
                    Rollout: ${log.oldRolloutPercentage ?? 0}% ➔ ${log.newRolloutPercentage}% &nbsp;•&nbsp;
                    Operator: <span style="color:var(--text-primary);">${escapeHtml(log.changedBy)}</span>
                </div>
            </div>
        `;
    }).join('');
}

// ──────────────────────────────────────────────
// Modal 1: Edit Flag State & Rollout
// ──────────────────────────────────────────────

function openEditModal(flagName, isEnabled, rollout) {
    editFlagName.value = flagName;
    editFlagDisplay.textContent = flagName;
    editEnvDisplay.textContent = activeEnv.toUpperCase();
    editEnabled.value = String(isEnabled);
    editRollout.value = rollout;
    editRolloutSlider.value = rollout;
    editRolloutValue.textContent = rollout + '%';

    updateRolloutExplanation();
    editModal.style.display = 'flex';
}

function closeEditModal() {
    editModal.style.display = 'none';
}

function updateRolloutExplanation() {
    const isEnabled = editEnabled.value === 'true';
    const rollout = parseInt(editRollout.value) || 0;
    const envUpper = activeEnv.toUpperCase();

    if (!isEnabled || rollout === 0) {
        editLiveExplanation.innerHTML = `
            <strong>Feature is paused:</strong> 0% of users in ${envUpper} will see this feature. All traffic will safely receive the default baseline behavior.
        `;
    } else if (rollout === 100) {
        editLiveExplanation.innerHTML = `
            <strong>Full Release (100%):</strong> Every user in ${envUpper} will immediately experience this feature.
        `;
    } else {
        editLiveExplanation.innerHTML = `
            <strong>Gradual Canary Rollout (${rollout}%):</strong> Exactly ${rollout}% of hash-bucketed users will receive this feature. The remaining ${100 - rollout}% will remain on standard fallback. The same user will always receive the identical experience.
        `;
    }
}

async function handleUpdateStateSubmit(event) {
    event.preventDefault();

    const flagName = editFlagName.value;
    const enabled = editEnabled.value === 'true';
    const rolloutPercentage = parseInt(editRollout.value) || 0;

    try {
        await apiFetch(`/flags/${flagName}/states`, {
            method: 'PUT',
            body: JSON.stringify({
                environment: activeEnv,
                enabled,
                rolloutPercentage,
                changedBy: currentOperator
            })
        });

        showToast(`Configuration saved for "${flagName}" in ${activeEnv.toUpperCase()}`);
        closeEditModal();
        loadFlags();
    } catch (error) {
        // Handled in apiFetch
    }
}

// ──────────────────────────────────────────────
// Modal 2: Create Flag
// ──────────────────────────────────────────────

function openCreateModal() {
    createFlagForm.reset();
    createModal.style.display = 'flex';
    document.getElementById('newFlagName').focus();
}

function closeCreateModal() {
    createModal.style.display = 'none';
}

async function handleCreateFlagSubmit(event) {
    event.preventDefault();

    const name = document.getElementById('newFlagName').value.trim();
    const description = document.getElementById('newFlagDescription').value.trim();
    const defaultState = document.getElementById('newFlagDefault').value === 'true';

    try {
        await apiFetch('/flags', {
            method: 'POST',
            body: JSON.stringify({ name, description, defaultState })
        });

        showToast(`Feature flag "${name}" created and initialized across DEV, TEST, and PROD.`);
        closeCreateModal();
        loadFlags();
    } catch (error) {
        // Handled in apiFetch
    }
}

// ──────────────────────────────────────────────
// Modal 3: History Drawer
// ──────────────────────────────────────────────

async function openHistoryModal(flagName) {
    historyModalFlagName.textContent = flagName;
    flagHistoryTableBody.innerHTML = `<tr><td colspan="7" class="table-loading">Loading audit logs for "${flagName}"...</td></tr>`;
    historyModal.style.display = 'flex';

    try {
        const history = await apiFetch(`/flags/${flagName}/history`);

        if (history.length === 0) {
            flagHistoryTableBody.innerHTML = `<tr><td colspan="7" class="table-loading">No change records found for this flag.</td></tr>`;
            return;
        }

        flagHistoryTableBody.innerHTML = history.map(item => `
            <tr>
                <td><span class="badge badge-gradual">${item.environment.toUpperCase()}</span></td>
                <td>${item.oldEnabled === null ? '—' : (item.oldEnabled ? 'ON' : 'OFF')}</td>
                <td style="font-weight:700;">${item.newEnabled ? 'ON' : 'OFF'}</td>
                <td>${item.oldRolloutPercentage === null ? '—' : item.oldRolloutPercentage + '%'}</td>
                <td style="font-weight:700;">${item.newRolloutPercentage}%</td>
                <td class="text-mono" style="color:var(--text-secondary);">${escapeHtml(item.changedBy)}</td>
                <td class="text-mono" style="font-size:0.75rem; color:var(--text-muted);">${formatDateHuman(item.changedAt)}</td>
            </tr>
        `).join('');
    } catch (error) {
        flagHistoryTableBody.innerHTML = `<tr><td colspan="7" class="table-loading" style="color:#ff9999;">Could not load history.</td></tr>`;
    }
}

function closeHistoryModal() {
    historyModal.style.display = 'none';
}

// ──────────────────────────────────────────────
// Delete Flag
// ──────────────────────────────────────────────

async function confirmDeleteFlag(id, name) {
    const confirmation = confirm(
        `Are you sure you want to retire feature flag "${name}"?\n\nThis will permanently delete the flag and its environment rules across DEV, TEST, and PROD.`
    );
    if (!confirmation) return;

    try {
        await apiFetch(`/flags/${id}`, { method: 'DELETE' });
        showToast(`Feature flag "${name}" has been retired.`);
        loadFlags();
    } catch (error) {
        // Handled in apiFetch
    }
}

// ──────────────────────────────────────────────
// Utilities
// ──────────────────────────────────────────────

function showToast(message, isError = false) {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${isError ? 'toast-error' : ''}`;
    toast.textContent = message;
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

function formatDateHuman(dateStr) {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
    });
}
