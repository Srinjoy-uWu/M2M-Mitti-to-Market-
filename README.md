# 🌾 Mitti2Market — Direct Agri Marketplace

> **SIH 26033:** "Multiple intermediaries reduce farmers' earnings and increase consumer prices."

Mitti2Market is a direct agricultural marketplace connecting **Farmers / FPOs** with **Businesses / Bulk Buyers** — eliminating middlemen, optimizing supply chain routes, and ensuring fair trade with transparent dispute resolution.

---

## 🚀 How to Easily Start the Project

### Prerequisites
- **Java 17+** (JDK installed and available in PATH)
- **Node.js 18+** & **npm**

---

### Option 1: One-Click Launch (Recommended for Windows)

Simply double-click:
```bash
start.bat
```
*(Or right-click `start.ps1` → **Run with PowerShell**)*

**What this does automatically:**
1. Checks that Java and Node.js are installed.
2. Automatically installs frontend dependencies (`npm install`) if `node_modules` is missing.
3. Launches the Spring Boot backend on **port 8080** in its own console window.
4. Launches the Vite React frontend dev server on **port 5173** in its own console window.
5. Automatically opens your default web browser to `http://localhost:5173`.

---

### Option 2: Manual Terminal Launch

If you prefer starting services manually in separate terminals:

#### 1. Start the Backend
```bash
cd backend
.\mvnw.cmd spring-boot:run     # On Windows
# ./mvnw spring-boot:run       # On Linux / macOS
```
> The backend boots up on **`http://localhost:8080`** using an embedded H2/MySQL-compatible database and automatically seeds sample farmers, buyers, deals, produce, and warehouse hubs.

#### 2. Start the Frontend
In a new terminal window:
```bash
cd frontend
npm install                    # Only needed on first run
npm run dev
```
> The frontend runs on **`http://localhost:5173`**.

---

## 🔑 Ready-to-Use Demo Credentials

The platform includes pre-seeded accounts configured for each portal:

| Role | Portal URL | Email / Phone | Password |
| :--- | :--- | :--- | :--- |
| **Farmer** | `http://localhost:5173/farmer/login` | `ramesh@farmer.com` *(or `ramesh@example.com` or `9876543210`)* | `password123` |
| **Business** | `http://localhost:5173/business/login` | `procurement@freshmart.com` *(or `freshmart@example.com` or `9876543220`)* | `password123` |
| **Admin** | `http://localhost:5173/admin/login` | `admin@mitti2market.com` *(or `admin@example.com`)* | `admin123` *(or `password123`)* |

*Note: The platform enforces role-based login isolation — please use the designated portal for each role.*

---

## 🧪 Running Tests & Build Verification

### Backend Automated Test Suite
```bash
cd backend
.\mvnw.cmd test
```
Runs the full test suite (17 tests covering authentication, role isolation, multi-stop routing, dispute flows, and reverse logistics).

### Frontend Code Check & Production Build
```bash
cd frontend
npm run lint    # Linter check across all components
npm run build   # Verifies production bundling and asset compilation
```

---

## API Endpoints (Backend)

| Method | Endpoint                     | Description              |
|--------|------------------------------|--------------------------|
| GET    | `/api/health`                | Health check             |
| GET    | `/api/produce`               | List all produce         |
| POST   | `/api/produce`               | Create new produce       |
| PUT    | `/api/produce/{id}`          | Update produce           |
| DELETE | `/api/produce/{id}`          | Delete produce           |
| GET    | `/api/orders`                | List all orders          |
| POST   | `/api/orders`                | Create order             |
| PATCH  | `/api/orders/{id}/status`    | Update order status      |

---

## User Journeys

### 👨‍🌾 Farmer Flow
```
Landing → Login → Dashboard → Add Produce
       → AI Price Advisor → Buyer Requests → Orders → Logistics
```

### 🏪 Business Flow
```
Landing → Login → Dashboard → Browse Produce
       → Product Details → Bulk Order → Orders → Logistics
```

---

## License

Built for **Smart India Hackathon (SIH) 26033**.

