// Global variables for Charts and User Map
let balanceChart = null;
let spendingChart = null;
let usersMap = {};
        
// Final Color Constants 
const ERROR_COLOR = '#ea4335';
const SUCCESS_COLOR = '#34a853';
const chartColors = ['#4285f4', '#34a853', '#fbbc05', '#ea4335', '#9c27b0', '#00bcd4', '#17a2b8', '#ffc107']; 

// --- INITIALIZATION & PAGE ROUTING ---
document.addEventListener('DOMContentLoaded', () => {
    fetchDataAndRender();
    document.getElementById('expenseForm').addEventListener('submit', handleExpenseSubmit);
    document.getElementById('userForm').addEventListener('submit', handleUserSubmit);
    document.querySelector('nav ul li a[href="#home"]').classList.add('active');
});

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
    
    // Manually trigger resize/render when switching pages
    if (pageId === 'balance-page') {
        fetchDataAndRender(); // Re-fetch to ensure data is current
        if (balanceChart) balanceChart.resize();
    }
    if (pageId === 'spending-page') {
        fetchDataAndRender(); // Re-fetch to ensure data is current
        if (spendingChart) spendingChart.resize();
    }
}

// Combined fetch function to update everything
async function fetchDataAndRender() {
    usersMap = await fetchUsersMap();
    const settlements = await fetchSettlementsData();
    const expenses = await fetchExpensesData(); 
    
    renderSettlements(settlements);
    renderChart(settlements);
    renderSpendingBreakdown(expenses); 
}

// =========================================================================
// 1. DATA FETCHING (API Calls)
// =========================================================================

async function fetchUsersMap() {
     try {
        const response = await fetch('http://localhost:8080/api/users');
        const users = await response.json();
        const map = {};
        users.forEach(user => { map[user.id] = user.name; });
        return map;
    } catch (error) {
        return {};
    }
}

async function fetchSettlementsData() {
     try {
        const response = await fetch('http://localhost:8080/api/settlements');
        if (!response.ok) { throw new Error(`Server status: ${response.status}`); }
        return await response.json();
    } catch (error) {
        return [];
    }
}

async function fetchExpensesData() {
     try {
        const response = await fetch('http://localhost:8080/api/expenses');
        if (!response.ok) { throw new Error(`Server status: ${response.status}`); }
        return await response.json();
    } catch (error) {
        return [];
    }
}

// =========================================================================
// 2. RENDER NET BALANCE VISUALIZATION (Dynamic Bar/Line Chart)
// =========================================================================

function renderChart(settlements) {
    // Get selected chart type (default to 'bar' if not set)
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
    
    // Bar Chart colors based on debt/receivable status
    const backgroundColors = data.map(val => val < 0 ? ERROR_COLOR : SUCCESS_COLOR);
    
    if (balanceChart) { balanceChart.destroy(); }
    
    // Determine the actual chart type and axis based on user selection
    let finalChartType = (chartType === 'horizontalBar') ? 'bar' : chartType;
    let indexAxis = (chartType === 'horizontalBar' || chartType === 'line') ? 'x' : 'y';
    
    // Ensure line charts are drawn as lines, not bars
    const datasetType = (finalChartType === 'line') ? 'line' : 'bar'; 

    balanceChart = new Chart(ctx, {
        type: finalChartType,
        data: {
            labels: labels,
            datasets: [{
                label: 'Net Balance (Owed/Receivable)',
                data: data,
                backgroundColor: backgroundColors,
                borderColor: (finalChartType === 'line') ? 'rgba(26, 115, 232, 0.7)' : undefined, // Blue border for line
                borderWidth: 2,
                type: datasetType, // Ensure line is drawn correctly
                tension: 0.3, // Curve the line
                fill: finalChartType !== 'line' // Fill bars, not lines
            }]
        },
        options: {
            indexAxis: indexAxis, // For horizontal bar
            responsive: true,
            scales: {
                y: {
                    beginAtZero: true,
                    title: { display: true, text: 'Amount (₹)' }
                },
                x: {
                    beginAtZero: true,
                    title: { display: true, text: 'User' },
                    ticks: { autoSkip: false, maxRotation: 45, minRotation: 45 }
                }
            },
            plugins: {
                legend: { display: false },
                title: { display: true, text: 'Final Net Debt/Receivable' }
            }
        }
    });
}


// =========================================================================
// 3. RENDER SPENDING CONTRIBUTION (Dynamic Pie/Donut/Polar Chart)
// =========================================================================

function renderSpendingBreakdown(expenses) {
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
        summaryDiv.innerHTML = '<p style="text-align: center; color: #666;">No expenses added yet.</p>';
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

    // Setup dynamic options based on chart type
    let options = {
        responsive: true,
        plugins: {
            legend: { position: 'right' },
            title: { display: true, text: `Total Trip Spend: ₹${totalSpent.toFixed(2)}` }
        },
        // PIE/DONUT/POLAR OPTIONS
        cutout: (chartType === 'doughnut') ? '40%' : '0%', // Adds donut hole
        rotation: (chartType === 'rotatingPie') ? 100 : 0 // Adds visual rotation/tilt
    };

    const finalChartType = (chartType === 'polarArea') ? 'polarArea' : 
                           (chartType === 'doughnut' || chartType === 'rotatingPie') ? 'doughnut' : 'pie';
                           
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

// --- SUBMISSION LOGIC (Unchanged) ---
async function handleUserSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const messageElement = document.getElementById('userMessage');
    messageElement.classList.remove('success', 'error');
    messageElement.textContent = 'Registering...';

    const userData = { name: form.userName.value, email: form.userEmail.value };
    try {
        const response = await fetch('http://localhost:8080/api/users', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(userData) });
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

    const expenseData = {
        description: form.description.value, amount: parseFloat(form.amount.value), paidBy: { id: parseInt(form.paidById.value, 10) }, splitType: "EQUAL"
    };

    try {
        const response = await fetch('http://localhost:8080/api/expenses', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(expenseData) });

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