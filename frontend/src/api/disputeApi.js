import { apiGet, apiPost, apiPut } from '../api';

export async function openDispute(dealId, data) {
  return apiPost(`/api/deals/${dealId}/disputes`, data);
}

export async function getDisputes(dealId) {
  return apiGet(`/api/deals/${dealId}/disputes`);
}

export async function updateDisputeStatus(disputeId, data) {
  return apiPut(`/api/deals/disputes/${disputeId}/status`, data);
}
