import { useState } from 'react';
import { AlertTriangle, X, Loader2, CheckCircle2 } from 'lucide-react';
import { openDispute } from '../api/disputeApi';

const DISPUTE_REASONS = [
  { value: 'QUALITY_ISSUE', label: 'Quality Issue', desc: 'Produce does not meet agreed quality, specifications, or freshness' },
  { value: 'QUANTITY_ISSUE', label: 'Quantity Shortage', desc: 'Delivered amount is lower than the contract weight' },
  { value: 'DAMAGED_GOODS', label: 'Damaged in Transit', desc: 'Produce damaged, crushed, or degraded during transportation' },
  { value: 'LATE_DELIVERY', label: 'Late Delivery', desc: 'Logistics missed scheduled delivery window causing operational loss' },
  { value: 'MISSING_GOODS', label: 'Missing Consignment', desc: 'Produce crates or entire lot not delivered' },
  { value: 'PAYMENT_ISSUE', label: 'Payment Discrepancy', desc: 'Disagreement on price, deduction, or payment disbursement' },
  { value: 'OTHER', label: 'Other Concern', desc: 'Any other critical issue requiring arbitration' },
];

export default function DisputeModal({ isOpen, onClose, dealId, dealNumber, onDisputeCreated }) {
  const [reason, setReason] = useState('QUALITY_ISSUE');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!description.trim()) {
      setError('Please provide a brief explanation of the dispute.');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      await openDispute(dealId, {
        reason,
        description: description.trim(),
      });
      setSuccess(true);
      if (onDisputeCreated) {
        onDisputeCreated();
      }
      setTimeout(() => {
        setSuccess(false);
        setDescription('');
        onClose();
      }, 1500);
    } catch (err) {
      setError(err?.message || 'Failed to submit dispute. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-gray-100 animate-in fade-in zoom-in duration-200">
        <div className="flex items-center justify-between pb-4 border-b border-gray-100">
          <div className="flex items-center gap-2.5">
            <span className="p-2 bg-amber-50 text-amber-600 rounded-xl">
              <AlertTriangle className="w-5 h-5" />
            </span>
            <div>
              <h3 className="font-bold text-base text-navy-900">
                Raise Deal Dispute
              </h3>
              <p className="text-xs text-gray-500">
                Deal {dealNumber ? `#${dealNumber}` : `#${dealId}`}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            disabled={submitting}
            className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100 transition"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {success ? (
          <div className="py-8 text-center space-y-2">
            <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto animate-bounce" />
            <p className="text-base font-semibold text-gray-900">Dispute Filed Successfully</p>
            <p className="text-xs text-gray-500">
              Our moderation team and the counterparty have been notified for review.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            {error && (
              <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl">
                {error}
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1.5">
                Dispute Reason
              </label>
              <select
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-gray-50 border border-navy-100 rounded-xl text-sm focus:outline-hidden focus:border-amber-500 focus:bg-white transition"
              >
                {DISPUTE_REASONS.map((r) => (
                  <option key={r.value} value={r.value}>
                    {r.label}
                  </option>
                ))}
              </select>
              <p className="text-[11px] text-gray-400 mt-1">
                {DISPUTE_REASONS.find((r) => r.value === reason)?.desc}
              </p>
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1.5">
                Description & Supporting Details
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={4}
                placeholder="State the observed defects, discrepancy in weight, delivery timing, or communication history..."
                className="w-full px-3.5 py-2.5 bg-gray-50 border border-navy-100 rounded-xl text-sm focus:outline-hidden focus:border-amber-500 focus:bg-white resize-none transition"
              />
            </div>

            <div className="p-3 bg-amber-50/60 border border-amber-200/60 rounded-xl text-[11px] text-amber-800">
              Raising a dispute pauses automated fund settlement and triggers admin mediation. Please ensure accurate documentation.
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={onClose}
                disabled={submitting}
                className="px-4 py-2.5 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-xl transition"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="px-5 py-2.5 bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold rounded-xl shadow-xs transition inline-flex items-center gap-2 disabled:opacity-50"
              >
                {submitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Submitting...
                  </>
                ) : (
                  'Submit Dispute'
                )}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
