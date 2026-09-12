import { AlertCircle, Clock, CheckCircle2, XCircle, ShieldAlert } from 'lucide-react';

const STATUS_CONFIG = {
  OPEN: {
    label: 'Open',
    color: 'bg-amber-50 text-amber-700 border-amber-200',
    icon: Clock,
  },
  UNDER_REVIEW: {
    label: 'Under Review',
    color: 'bg-blue-50 text-blue-700 border-blue-200',
    icon: ShieldAlert,
  },
  RESOLVED: {
    label: 'Resolved',
    color: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    icon: CheckCircle2,
  },
  REJECTED: {
    label: 'Rejected',
    color: 'bg-red-50 text-red-600 border-red-200',
    icon: XCircle,
  },
};

function formatTimestamp(ts) {
  if (!ts) return '';
  try {
    const d = new Date(ts);
    return d.toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return ts;
  }
}

export default function DisputeTimeline({ disputes = [], onOpenDisputeModal, canRaise = true }) {
  if (!disputes || disputes.length === 0) {
    return (
      <div className="bg-white rounded-2xl border border-gray-100 p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm font-bold text-navy-900">
            <ShieldAlert className="w-4 h-4 text-gray-400" />
            <span>Dispute & Arbitration History</span>
          </div>
          {canRaise && (
            <button
              onClick={onOpenDisputeModal}
              className="text-xs font-semibold text-amber-700 hover:text-amber-800 transition"
            >
              + Raise Dispute
            </button>
          )}
        </div>
        <p className="text-xs text-gray-500 mt-2">
          No disputes recorded on this deal. Both parties are operating within agreed contract terms.
        </p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-2xl border border-amber-200/70 p-5 shadow-xs space-y-4">
      <div className="flex items-center justify-between pb-3 border-b border-gray-100">
        <div className="flex items-center gap-2 text-sm font-bold text-navy-900">
          <ShieldAlert className="w-4 h-4 text-amber-600" />
          <span>Disputes & Claims ({disputes.length})</span>
        </div>
        {canRaise && (
          <button
            onClick={onOpenDisputeModal}
            className="px-3 py-1 bg-amber-50 hover:bg-amber-100 text-amber-700 text-xs font-bold rounded-lg border border-amber-200 transition"
          >
            + Report New Issue
          </button>
        )}
      </div>

      <div className="space-y-3">
        {disputes.map((d) => {
          const cfg = STATUS_CONFIG[d.status] || STATUS_CONFIG.OPEN;
          const Icon = cfg.icon;
          return (
            <div
              key={d.id}
              className="p-3.5 bg-gray-50/70 rounded-xl border border-gray-200/70 text-xs space-y-2"
            >
              <div className="flex items-center justify-between gap-2 flex-wrap">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-gray-900 text-xs">
                    {(d.reason || '').replace(/_/g, ' ')}
                  </span>
                  <span className="text-gray-400 font-mono text-[10px]">#{d.id}</span>
                </div>
                <span
                  className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${cfg.color}`}
                >
                  <Icon className="w-3 h-3" />
                  {cfg.label}
                </span>
              </div>

              {d.description && (
                <p className="text-gray-700 bg-white p-2.5 rounded-lg border border-gray-100 text-[12px] leading-relaxed">
                  {d.description}
                </p>
              )}

              <div className="flex items-center justify-between text-[11px] text-gray-400 pt-1">
                <span>Raised by: <strong className="text-gray-600">{d.raisedByName || 'User'}</strong></span>
                <span>{formatTimestamp(d.createdAt)}</span>
              </div>

              {d.resolvedAt && (
                <div className="text-[10px] text-emerald-700 bg-emerald-50/80 px-2 py-1 rounded-md">
                  Resolved on {formatTimestamp(d.resolvedAt)}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
