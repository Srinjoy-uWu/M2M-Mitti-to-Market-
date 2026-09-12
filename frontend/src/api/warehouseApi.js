import { apiGet, apiPost, apiPut } from '../api';

export async function getActiveWarehouses() {
  return apiGet('/api/warehouses');
}

export async function getNearestWarehouses(lat, lng) {
  return apiGet(`/api/warehouses/nearest?lat=${encodeURIComponent(lat)}&lng=${encodeURIComponent(lng)}`);
}

export async function getWarehouseById(id) {
  return apiGet(`/api/warehouses/${id}`);
}

export async function createWarehouse(data) {
  return apiPost('/api/warehouses', data);
}

export async function updateWarehouse(id, data) {
  return apiPut(`/api/warehouses/${id}`, data);
}

export async function optimizeRouteViaWarehouse(data) {
  return apiPost('/api/deals/logistics/optimize-route-via-warehouse', data);
}
