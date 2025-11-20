// Global variables for Charts and User Map
let balanceChart = null;
let spendingChart = null;
let usersMap = {};
let categoryBreakdown = []; 

// Theme settings
const THEMES = {
    DEVICE: 'device',
    LIGHT: 'light',
    DARK: 'dark'
};
let currentTheme = THEMES.DEVICE;
        
// Final Color Constants (These remain static for charts/feedback)
const ERROR_COLOR = '#ea4335';
const SUCCESS_COLOR = '#34a853';
const chartColors = ['#4285f4', '#34a853', '#fbbc05', '#ea4335', '#9c27b0', '#00bcd4', '#17a2b8', '#ffc107']; 

// --- INITIALIZATION & PAGE ROUTING ---
document.addEventListener('DOMContentLoaded', () => {
    // 1. Initialize Theme
    initializeTheme();
    
    // 2. Fetch Data and Render
    fetchDataAndRender();
    
    // 3. Setup Event Listeners
    document.getElementById('expenseForm').addEventListener('submit', handleExpenseSubmit);
    document.getElementById('userForm').addEventListener('submit', handleUserSubmit);
    document.querySelector('nav ul li a[href="#home"]').classList.add('active');
    
    // Setup the default dropdown options for category type (Removed since now in HTML)
});

// =========================================================================
// THEME MANAGEMENT LOGIC
// =========================================================================

function initializeTheme() {
    const savedTheme = localStorage.getItem('expense_splitter_theme');
    if (savedTheme) {
        currentTheme = savedTheme;
    }
    applyTheme();
}

function applyTheme() {
    const body = document.body;
    const switcher = document.getElementById('themeSwitcher');
    
    body.classList.remove(THEMES.LIGHT + '-theme', THEMES.DARK + '-theme');

    if (currentTheme === THEMES.LIGHT) {
        body.classList.add(THEMES.LIGHT + '-theme');
        switcher.textContent = 'Light Theme';
    } else if (currentTheme === THEMES.DARK) {
        body.classList.add(THEMES.DARK + '-theme');
        switcher.textContent = 'Dark Theme';
    } else { // DEVICE
        // Apply no class, rely on CSS media query for device preference
        switcher.textContent = 'Device Theme';
    }
    
    localStorage.setItem('expense_splitter_theme', currentTheme);
    
    // Rerender charts to update text color/grid lines if needed (Chart.js doesn't auto-update)
    fetchDataAndRender();
}

// Function to cycle through themes (Device -> Light -> Dark -> Device)
function toggleTheme() {
    if (currentTheme === THEMES.DEVICE) {
        currentTheme = THEMES.LIGHT;
    } else if (currentTheme === THEMES.LIGHT) {
        currentTheme = THEMES.DARK;
    } else {
        currentTheme = THEMES.DEVICE;
    }
    applyTheme();
}

// Function to control page visibility
function showPage(pageId, element) {
    document.querySelectorAll('.content-section').forEach(section => {
        section.classList.remove('active');
    });
    document.getElementById(pageId).classList.add('active');
    
    document.querySelectorAll('nav ul li a').forEach(a => a.classList.remove('active'));
    if (element) {
         element.classList.add('active');
    }
    
    // Manually trigger resize/render when switching pages for Chart.js
    if (pageId === 'balance-page') {
        fetchDataAndRender(); 
        if (balanceChart) balanceChart.resize();
    }
    if (pageId === 'spending-page') {
        fetchDataAndRender(); 
        if (spendingChart) spendingChart.resize();
    }
}

// Combined fetch function to update everything
async function fetchDataAndRender() {
    usersMap = await fetchUsersMap();
    const settlements = await fetchSettlementsData();
    const expenses = await fetchExpensesData(); 
    categoryBreakdown = await fetchSpendingBreakdown();
    
    renderSettlements(settlements);
    renderChart(settlements);
    renderSpendingBreakdown(expenses); 
    renderCategoryBreakdown(categoryBreakdown);
}

// =========================================================================
// 1. DATA FETCHING (API Calls)
// (API URLs must be updated to the Heroku URL once deployed)
// =========================================================================

const API_BASE_URL = 'http://localhost:8080/api'; // CHANGE THIS TO YOUR HEROKU URL LATER

async function fetchUsersMap() {
     try {
        const response = await fetch(`${API_BASE_URL}/users`);
        const users = await response.json();
        const map = {};
        users.forEach(user => { map[user.id] = user.name; });
        return map;
    } catch (error) {
        console.error("Error fetching users:", error);
        return {};
    }
}

async function fetchSettlementsData() {
     try {
        const response = await fetch(`${API_BASE_URL}/settlements`);
        if (!response.ok) { throw new Error(`Server status: ${response.status}`); }
        return await response.json();
    } catch (error) {
        console.error("Error fetching settlements:", error);
        return [];
    }
}

async function fetchExpensesData() {
     try {
        const response = await fetch(`${API_BASE_URL}/expenses`);
        if (!response.ok) { throw new Error(`Server status: ${response.status}`); }
        return await response.json();
    } catch (error) {
        console.error("Error fetching expenses:", error);
        return [];
    }
}

// API Call to fetch Category Spending Breakdown
async function fetchSpendingBreakdown() {
    try {
        const response = await fetch(`${API_BASE_URL}/spending-breakdown`);
        if (!response.ok) { throw new Error(`Server status: ${response.status}`); }
        return await response.json();
    } catch (error) {
        console.error("Error fetching spending breakdown:", error);
        return [];
    }
}

// =========================================================================
// 2. RENDER NET BALANCE VISUALIZATION (Dynamic Bar/Line Chart)
// =========================================================================

function getChartTextColor() {
    // Determine the color of the text for Chart.js based on the active theme
    const bodyStyles = window.getComputedStyle(document.body);
    return bodyStyles.getPropertyValue('--text-color').trim();
}

function renderSettlements(settlements) {
    const settlementDiv = document.getElementById('settlements');
    
    if (settlements.length === 0) {
        settlementDiv.innerHTML = '<p class="settled-message">Everyone is settled up! Zero transactions needed.</p>';
        return;
    }
    let html = '<ul>';
    settlements.forEach(s => {
        const owesId = s.owesUserId || s.giverId;
        const receivesId = s.receivesUserId || s.receiverId;
        const owesName = usersMap[owesId] || `User ID ${owesId}`;
        const receivesName = usersMap[receivesId] || `User ID ${receivesId}`;
        html += `
            <li>
                <span class="user-owes">${owesName}</span> owes
                <span class="user-receives">${receivesName}</span>
                <span class="summary-info">₹${s.amount.toFixed(2)}</span>
            </li>
        `;
    });
    html += '</ul>';
    settlementDiv.innerHTML = html;
}

function renderChart(settlements) {
    const chartTextColor = getChartTextColor(); // Dynamic text color
    const chartGridColor = getChartTextColor() === '#e0e0e0' ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)';
    
    const chartType = document.getElementById('balanceChartType').value;
    const ctx = document.getElementById('balanceChart').getContext('2d');
    
    const netBalance = {}; 
    Object.keys(usersMap).forEach(id => { netBalance[id] = 0; });

    settlements.forEach(s => {
        const owesId = s.owesUserId || s.giverId;
        const receivesId = s.receivesUserId || s.receiverId;
        const amount = s.amount;
        netBalance[owesId] = (netBalance[owesId] || 0) - amount;
        netBalance[receivesId] = (netBalance[receivesId] || 0) + amount;
    });
    
    const labels = Object.keys(netBalance).map(id => usersMap[id] || `ID ${id}`);
    const data = Object.values(netBalance).map(val => val.toFixed(2));
    
    const backgroundColors = data.map(val => val < 0 ? ERROR_COLOR : SUCCESS_COLOR);
    
    if (balanceChart) { balanceChart.destroy(); }
    
    let finalChartType = (chartType === 'horizontalBar' || chartType === 'bar') ? 'bar' : chartType;
    let indexAxis = (chartType === 'horizontalBar') ? 'y' : 'x';
    const datasetType = (finalChartType === 'line') ? 'line' : 'bar'; 

    balanceChart = new Chart(ctx, {
        type: finalChartType,
        data: {
            labels: labels,
            datasets: [{
                label: 'Net Balance (Owed/Receivable)',
                data: data,
                backgroundColor: backgroundColors,
                borderColor: (finalChartType === 'line') ? 'rgba(26, 115, 232, 0.7)' : undefined,
                borderWidth: 2,
                type: datasetType, 
                tension: (finalChartType === 'line') ? 0.3 : 0, 
                fill: finalChartType !== 'line' 
            }]
        },
        options: {
            indexAxis: indexAxis, 
            responsive: true,
            plugins: {
                legend: { display: false },
                title: { 
                    display: true, 
                    text: 'Final Net Debt/Receivable',
                    color: chartTextColor
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    title: { display: true, text: 'Amount (₹)', color: chartTextColor },
                    ticks: { color: chartTextColor },
                    grid: { color: chartGridColor }
                },
                x: {
                    beginAtZero: true,
                    title: { display: true, text: 'User', color: chartTextColor },
                    ticks: { autoSkip: false, maxRotation: 45, minRotation: 45, color: chartTextColor },
                    grid: { color: chartGridColor }
                }
            }
        }
    });
}


// =========================================================================
// 3. RENDER SPENDING CONTRIBUTION (Dynamic Pie/Donut/Polar Chart)
// =========================================================================

function renderSpendingBreakdown(expenses) {
    const chartTextColor = getChartTextColor(); // Dynamic text color
    const chartType = document.getElementById('spendingChartType').value;
    const ctx = document.getElementById('spendingChart').getContext('2d');
    const summaryDiv = document.getElementById('spendingSummary');
    
    let totalSpent = 0;
    const userContribution = {}; 

    expenses.forEach(exp => {
        const paidById = exp.paidBy.id;
        const amount = exp.amount;
        totalSpent += amount;
        userContribution[paidById] = (userContribution[paidById] || 0) + amount;
    });

    if (totalSpent === 0) {
        summaryDiv.innerHTML = '<p style="text-align: center; color: var(--text-color);">No expenses added yet.</p>';
        if (spendingChart) spendingChart.destroy();
        return;
    }
    
    const labels = [];
    const data = [];

    Object.entries(userContribution).forEach(([id, amount]) => {
        const name = usersMap[id] || `ID ${id}`;
        labels.push(name);
        data.push(amount);
    });

    if (spendingChart) spendingChart.destroy();

    let options = {
        responsive: true,
        plugins: {
            legend: { 
                position: 'right',
                labels: { color: chartTextColor }
            },
            title: { 
                display: true, 
                text: `Total Trip Spend: ₹${totalSpent.toFixed(2)}`,
                color: chartTextColor
            }
        },
        cutout: (chartType === 'doughnut') ? '40%' : '0%', 
        rotation: (chartType === 'rotatingPie') ? 100 : 0 
    };

    const finalChartType = (chartType === 'polarArea') ? 'polarArea' : 
                           (chartType === 'doughnut') ? 'doughnut' : 'pie';
                           
    spendingChart = new Chart(ctx, {
        type: finalChartType,
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: chartColors.slice(0, labels.length),
                hoverOffset: 10
            }]
        },
        options: options
    });

    // Render Text Summary
    let summaryHtml = `<p style="font-weight: 700; color: var(--text-color);">Total Spend: ₹${totalSpent.toFixed(2)}</p><ul>`;
    Object.entries(userContribution).forEach(([id, amount]) => {
        const name = usersMap[id] || `ID ${id}`;
        const percentage = ((amount / totalSpent) * 100).toFixed(1);
        summaryHtml += `
            <li>
                <span>${name}:</span>
                <span style="font-weight: 700; color: var(--primary-color);">₹${amount.toFixed(2)} (${percentage}%)</span>
            </li>
        `;
    });
    summaryHtml += '</ul>';
    summaryDiv.innerHTML = summaryHtml;
}


// =========================================================================
// 4. CATEGORY SPENDING BREAKDOWN
// =========================================================================

function renderCategoryBreakdown(categoryData) {
    const categoryDiv = document.getElementById('categoryBreakdownSummary'); 
    
    if (categoryData.length === 0) {
        categoryDiv.innerHTML = '<p style="text-align: center; color: var(--text-color);">No categorized expenses found.</p>';
        return;
    }
    
    let html = '<h3>Category-wise Top Spenders 🏆</h3>';
    
    categoryData.forEach(item => {
        const topSpenderName = usersMap[item.topSpenderId] || `ID ${item.topSpenderId}`;
        
        let breakdownList = '';
        Object.entries(item.userSpendingBreakdown)
            .sort(([, a], [, b]) => b - a) 
            .forEach(([userId, amount]) => {
                const userName = usersMap[userId] || `ID ${userId}`;
                const isTopSpender = (parseInt(userId) === item.topSpenderId);
                breakdownList += `
                    <li class="${isTopSpender ? 'top-spender' : ''}">
                        <span>${userName}:</span>
                        <span style="font-weight: 700; color: ${isTopSpender ? 'var(--success-color)' : 'var(--primary-color)'};">
                            ₹${parseFloat(amount).toFixed(2)}
                            ${isTopSpender ? ' (Highest)' : ''}
                        </span>
                    </li>
                `;
            });
            
        html += `
            <div class="category-card">
                <span class="category-title">${item.category}</span>
                <p>
                    <span style="font-weight: 700;">Total Spent: ₹${parseFloat(item.totalCategorySpend).toFixed(2)}</span>
                    <br>
                    Highest Contributor: <span class="top-spender-name">${topSpenderName}</span>
                </p>
                <ul class="spending-list">${breakdownList}</ul>
            </div>
        `;
    });
    
    categoryDiv.innerHTML = html;
}


// =========================================================================
// 5. SUBMISSION LOGIC
// =========================================================================

async function handleUserSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const messageElement = document.getElementById('userMessage');
    messageElement.classList.remove('success', 'error');
    messageElement.textContent = 'Registering...';

    const userData = { name: form.userName.value, email: form.userEmail.value };
    try {
        const response = await fetch(`${API_BASE_URL}/users`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(userData) });
        const result = await response.json();
        if (response.ok) {
            messageElement.textContent = `User ${result.name} registered successfully with ID: ${result.id}`;
            messageElement.classList.add('success');
            form.reset();
            await fetchDataAndRender(); 
        } else {
            messageElement.textContent = `Failed to register user. Status: ${response.status}. Email might already exist.`;
            messageElement.classList.add('error');
        }
    } catch (error) { messageElement.textContent = 'Network Error: Could not connect to API.'; messageElement.classList.add('error'); }
}


async function handleExpenseSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const messageElement = document.getElementById('expenseMessage');
    messageElement.classList.remove('success', 'error');
    messageElement.textContent = 'Saving...';

    const descriptionValue = form.description.value ? form.description.value : "";
    
    const expenseData = {
        description: descriptionValue, 
        amount: parseFloat(form.amount.value), 
        paidBy: { id: parseInt(form.paidById.value, 10) },
        expenseType: form.expenseType.value, 
        category: form.expenseCategory.value 
    };

    try {
        const response = await fetch(`${API_BASE_URL}/expenses`, { 
            method: 'POST', 
            headers: { 'Content-Type': 'application/json' }, 
            body: JSON.stringify(expenseData) 
        });

        if (response.ok) {
            messageElement.textContent = 'Expense saved successfully! Recalculating debt...';
            messageElement.classList.add('success');
            form.reset();
            await fetchDataAndRender(); 
        } else {
            messageElement.textContent = `Failed to save expense. Status: ${response.status}. Check if User ID exists.`;
            messageElement.classList.add('error');
        }
    } catch (error) {
        messageElement.textContent = 'Network Error: Could not connect to API.';
        messageElement.classList.add('error');
    }
}