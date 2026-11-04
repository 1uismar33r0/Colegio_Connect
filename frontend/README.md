# Colegio Connect frontend

The M1 frontend is a React, TypeScript, and Vite application for registration, email verification, login, session restoration, and logout. It intentionally does not assign capabilities or navigation from `accountType`; authorization belongs to a later backend/API decision.

## Development

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Vite proxies same-origin `/api` requests to `http://localhost:8080` by default. Set `VITE_API_TARGET=http://localhost:18080` in `frontend/.env.local` when the backend uses that port.

The API client always uses `credentials: 'include'`. It initializes `GET /api/auth/csrf` before restoring `GET /api/auth/me`, sends the readable `XSRF-TOKEN` cookie as `X-XSRF-TOKEN` on every unsafe request, and refreshes CSRF after successful login or logout.

## Backend integration status

`origin/main` currently contains registration/verification but not the login/session endpoints. Browser behavior is therefore covered with controlled `fetch` mocks, not a browser-to-backend end-to-end test. When PR #95 merges, run the app against the real backend over HTTPS: its production session cookie is intentionally `Secure`, so plain HTTP Vite development cannot persist authenticated sessions. No CORS relaxation or production cookie weakening is included here.

## Verification

```powershell
npm.cmd test
npm.cmd run build
npm.cmd run lint
```
