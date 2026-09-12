import { apiGet, apiPost, apiPut } from '../api';

export async function createReturn(dealId, data) {
  return apiPost(`/api/deals/${dealId}/returns`, data);
}

export async function getDealReturns(dealId) {
  return apiGet(`/api/deals/${dealId}/returns`);
}

export async function getReturnById(id) {
  return apiGet(`/api/deals/returns/${id}`);
}

export async function updateReturnStatus(id, status) {
  return apiPut(`/api/deals/returns/${id}/status`, { status });
}

export async function getAdminReturns() {
  return apiGet('/api/deals/admin/returns');
}
