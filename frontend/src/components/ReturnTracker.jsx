import { Truck, RotateCcw, CheckCircle2, Clock, MapPin, IndianRupee, AlertTriangle } from 'lucide-react';

const RETURN_FLOW = [
  'REQUESTED',
  'PICKED_UP',
  'IN_TRANSIT',
  'RETURNED',
  'REFUND_INITIATED',
  'CLOSED',
];

const RETURN_LABELS = {
  REQUESTED: 'Return Requested',
  PICKED_UP: 'Cargo Collected',
  IN_TRANSIT: 'In Reverse Transit',
  RETURNED: 'Restocked at Farm',
  REFUND_INITIATED: 'Refund Processed',
  CLOSED: 'Case Closed',
};

const formatINR = (n) => '₹' + Number(n || 0).toLocaleString('en-IN');
const formatDateTime = (iso) => {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString('en-IN', {
      day: 'numeric',
      month: 'short',
      hour: 'numeric',
      minute: '2-digit',
    });
  } catch {
    return '—';
  }
};

export default function ReturnTracker({ returnShipment, onStatusChange, isAdmin = false }) {
  if (!returnShipment) return null;

  const currentStatus = returnShipment.status || 'REQUESTED';
  const currentIdx = RETURN_FLOW.indexOf(currentStatus);

  return (
    <div className="bg-white rounded-2xl border border-purple-200 p-5 shadow-xs space-y-4">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-gray-100">
        <div className="flex items-center gap-2">
          <span className="p-2 bg-purple-50 text-purple-700 rounded-xl">
            <RotateCcw className="w-5 h-5" />
          </span>
          <div>
            <div className="flex items-center gap-2">
              <h4 className="font-bold text-sm text-navy-900">Reverse Logistics (Return Shipment)</h4>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200">
                {RETURN_LABELS[currentStatus] || currentStatus}
              </span>
            </div>
            <p className="text-xs text-gray-400 font-mono">
              Tracking ID: {returnShipment.trackingId || '—'}
            </p>
          </div>
        </div>

        {returnShipment.reason && (
          <span className="px-2.5 py-1 bg-amber-50 text-amber-800 border border-amber-200 rounded-lg text-xs font-semibold">
            Reason: {(returnShipment.reason || '').replace(/_/g, ' ')}
          </span>
        )}
      </div>

      {/* Progress Stepper */}
      <div className="py-2">
        <div className="flex items-center justify-between relative">
          <div className="absolute top-1/2 left-0 right-0 h-0.5 bg-gray-200 -translate-y-1/2 z-0" />
          <div
            className="absolute top-1/2 left-0 h-0.5 bg-purple-600 -translate-y-1/2 transition-all duration-500 z-0"
            style={{
              width: `${Math.max(0, (currentIdx / (RETURN_FLOW.length - 1)) * 100)}%`,
            }}
          />

          {RETURN_FLOW.map((step, idx) => {
            const isCompleted = idx <= currentIdx;
            const isCurrent = idx === currentIdx;

            return (
              <div key={step} className="flex flex-col items-center relative z-10">
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-colors ${
                    isCurrent
                      ? 'bg-purple-600 text-white ring-4 ring-purple-100 shadow-sm'
                      : isCompleted
                      ? 'bg-purple-600 text-white'
                      : 'bg-gray-100 text-gray-400 border-2 border-white'
                  }`}
                >
                  {isCompleted ? <CheckCircle2 className="w-4 h-4" /> : idx + 1}
                </div>
                <span className="text-[10px] font-medium text-gray-600 mt-1 hidden sm:block text-center max-w-[65px] leading-tight">
                  {RETURN_LABELS[step]}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Location Route Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
        <div className="p-3 bg-gray-50 rounded-xl border border-gray-100 text-xs">
          <div className="flex items-center gap-1.5 font-bold text-gray-500 uppercase tracking-wide text-[10px] mb-1">
            <MapPin className="w-3.5 h-3.5 text-red-500" />
            <span>Return Origin (Buyer Location)</span>
          </div>
          <p className="font-semibold text-gray-900">
            {returnShipment.fromLocation || 'Consignee Address'}
          </p>
        </div>

        <div className="p-3 bg-gray-50 rounded-xl border border-gray-100 text-xs">
          <div className="flex items-center gap-1.5 font-bold text-gray-500 uppercase tracking-wide text-[10px] mb-1">
            <MapPin className="w-3.5 h-3.5 text-emerald-600" />
            <span>Return Destination (Farmer)</span>
          </div>
          <p className="font-semibold text-gray-900">
            {returnShipment.toLocation || 'Farm Gate Address'}
          </p>
        </div>
      </div>

      {/* Route Distance & Cost Summary */}
      {(returnShipment.routeDistanceKm || returnShipment.routeEstimatedCost) && (
        <div className="flex items-center justify-between p-3 bg-purple-50/60 rounded-xl border border-purple-100 text-xs">
          <div className="flex items-center gap-4">
            {returnShipment.routeDistanceKm && (
              <span className="text-purple-900">
                <strong>Distance:</strong> {Number(returnShipment.routeDistanceKm).toFixed(1)} km
              </span>
            )}
            {returnShipment.routeEstimatedCost && (
              <span className="text-purple-900">
                <strong>Est. Return Freight:</strong> {formatINR(returnShipment.routeEstimatedCost)}
              </span>
            )}
          </div>
          <span className="text-[10px] text-purple-600">Reverse Haul Routing</span>
        </div>
      )}

      {returnShipment.description && (
        <p className="text-xs text-gray-600 bg-gray-50 p-2.5 rounded-lg border border-gray-100">
          {returnShipment.description}
        </p>
      )}

      {/* Admin Quick Progression Controls */}
      {isAdmin && onStatusChange && currentStatus !== 'CLOSED' && (
        <div className="pt-2 border-t border-gray-100 flex items-center justify-between flex-wrap gap-2 text-xs">
          <span className="font-semibold text-gray-600">Update Return Status:</span>
          <div className="flex items-center gap-1.5 flex-wrap">
            {currentStatus === 'REQUESTED' && (
              <button
                type="button"
                onClick={() => onStatusChange(returnShipment.id, 'PICKED_UP')}
                className="px-2.5 py-1 bg-purple-600 text-white rounded-lg hover:bg-purple-700 font-semibold transition"
              >
                Mark Picked Up
              </button>
            )}
            {currentStatus === 'PICKED_UP' && (
              <button
                type="button"
                onClick={() => onStatusChange(returnShipment.id, 'IN_TRANSIT')}
                className="px-2.5 py-1 bg-purple-600 text-white rounded-lg hover:bg-purple-700 font-semibold transition"
              >
                In Transit
              </button>
            )}
            {currentStatus === 'IN_TRANSIT' && (
              <button
                type="button"
                onClick={() => onStatusChange(returnShipment.id, 'RETURNED')}
                className="px-2.5 py-1 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 font-semibold transition"
              >
                Mark Returned
              </button>
            )}
            {currentStatus === 'RETURNED' && (
              <button
                type="button"
                onClick={() => onStatusChange(returnShipment.id, 'REFUND_INITIATED')}
                className="px-2.5 py-1 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-semibold transition"
              >
                Process Refund
              </button>
            )}
            {currentStatus === 'REFUND_INITIATED' && (
              <button
                type="button"
                onClick={() => onStatusChange(returnShipment.id, 'CLOSED')}
                className="px-2.5 py-1 bg-gray-800 text-white rounded-lg hover:bg-gray-900 font-semibold transition"
              >
                Close Return
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
