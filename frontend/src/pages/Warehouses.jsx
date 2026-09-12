import { useState, useEffect, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import Sidebar from '../components/Sidebar';
import { useAuth } from '../context/AuthContext';
import { getActiveWarehouses, getNearestWarehouses, optimizeRouteViaWarehouse } from '../api/warehouseApi';
import {
  Warehouse as WarehouseIcon, MapPin, Thermometer, Truck,
  Package, Phone, Mail, CheckCircle2, ArrowRight, Navigation,
  Search, Compass, Loader2, AlertCircle, ShieldCheck, Zap
} from 'lucide-react';

function makeHubIcon(hasColdStorage) {
  const bg = hasColdStorage ? '#0284c7' : '#d97706';
  return L.divIcon({
    className: '',
    html: `<div style="position:relative;width:28px;height:28px">
      <div style="width:28px;height:28px;border-radius:8px;background:${bg};border:2px solid white;box-shadow:0 2px 6px rgba(0,0,0,.35);display:flex;align-items:center;justify-content:center;color:white;font-size:14px;font-weight:bold">
        🏬
      </div>
    </div>`,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
    popupAnchor: [0, -16],
  });
}

export default function Warehouses() {
  const { user } = useAuth();
  const [warehouses, setWarehouses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState('ALL'); // 'ALL' | 'COLD' | 'AMBIENT'
  const [userCoords, setUserCoords] = useState(null);
  const [detectingLocation, setDetectingLocation] = useState(false);
  const [selectedHub, setSelectedHub] = useState(null);

  // Multi-leg route optimization state
  const [showOptimizer, setShowOptimizer] = useState(false);
  const [farmerStops, setFarmerStops] = useState([
    { name: 'Farm A (Nashik Dindori)', lat: 20.08, lng: 73.82, weightKg: 1500 },
    { name: 'Farm B (Niphad Vineyard)', lat: 20.06, lng: 74.11, weightKg: 2200 },
  ]);
  const [buyerDest, setBuyerDest] = useState({ name: 'Mumbai Vashi APMC', lat: 19.07, lng: 72.99 });
  const [truckCapacity, setTruckCapacity] = useState(5000);
  const [optimizing, setOptimizing] = useState(false);
  const [routeResult, setRouteResult] = useState(null);
  const [routeError, setRouteError] = useState('');

  const loadWarehouses = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getActiveWarehouses();
      setWarehouses(data || []);
      setError('');
    } catch (err) {
      setError(err.message || 'Failed to load warehouses');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadWarehouses();
  }, [loadWarehouses]);

  // Request user coordinates and query nearest
  const handleDetectLocation = () => {
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by your browser');
      return;
    }
    setDetectingLocation(true);
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const { latitude, longitude } = pos.coords;
        setUserCoords({ lat: latitude, lng: longitude });
        try {
          const nearest = await getNearestWarehouses(latitude, longitude);
          if (nearest && nearest.length > 0) {
            setWarehouses(nearest);
          }
        } catch (err) {
          // Keep current list
        } finally {
          setDetectingLocation(false);
        }
      },
      (err) => {
        setDetectingLocation(false);
        setError('Location access denied or unavailable: ' + err.message);
      },
      { timeout: 10000 }
    );
  };

  const filteredWarehouses = warehouses.filter((wh) => {
    const matchesSearch =
      wh.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      wh.city?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      wh.state?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      wh.code?.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;
    if (filterType === 'COLD') return wh.hasColdStorage;
    if (filterType === 'AMBIENT') return !wh.hasColdStorage;
    return true;
  });

  const handleRunMultiLegOptimization = async () => {
    if (!selectedHub) {
      setRouteError('Please select a regional warehouse hub first.');
      return;
    }
    setOptimizing(true);
    setRouteError('');
    setRouteResult(null);

    try {
      const payload = {
        farmerPickups: farmerStops.map((s) => ({
          name: s.name,
          lat: parseFloat(s.lat),
          lng: parseFloat(s.lng),
          weightKg: parseFloat(s.weightKg) || 0,
        })),
        warehouseCoords: [selectedHub.latitude, selectedHub.longitude],
        buyerCoords: [parseFloat(buyerDest.lat), parseFloat(buyerDest.lng)],
        capacityKg: parseFloat(truckCapacity) || 5000,
      };

      const res = await optimizeRouteViaWarehouse(payload);
      setRouteResult(res);
    } catch (err) {
      setRouteError(err.message || 'Route optimization failed');
    } finally {
      setOptimizing(false);
    }
  };

  return (
    <div className="flex min-h-screen bg-mustard-50/30">
      <Sidebar role={user?.role?.toLowerCase() === 'business' ? 'business' : 'farmer'} />
      <main className="flex-1 p-4 sm:p-6 lg:p-8 lg:pl-0">
        <div className="max-w-6xl mx-auto space-y-6">

          {/* Header */}
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="p-2 bg-navy-900 text-white rounded-xl">
                  <WarehouseIcon className="w-5 h-5" />
                </span>
                <h1 className="text-2xl font-bold text-navy-900">Regional Warehouse & Consolidation Hubs</h1>
              </div>
              <p className="text-sm text-gray-500 mt-1">
                Multi-party aggregation hubs for smallholder consolidation, cold chain staging, and bulk multi-leg transport.
              </p>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={handleDetectLocation}
                disabled={detectingLocation}
                className="px-4 py-2.5 bg-white border border-navy-200 text-navy-900 rounded-xl text-sm font-semibold hover:bg-navy-50 transition flex items-center gap-2 shadow-sm"
              >
                {detectingLocation ? <Loader2 className="w-4 h-4 animate-spin text-navy-600" /> : <Compass className="w-4 h-4 text-navy-600" />}
                {userCoords ? 'Nearest Hubs Located' : 'Find Nearest Hub'}
              </button>
              <button
                onClick={() => {
                  setShowOptimizer(!showOptimizer);
                  if (!selectedHub && warehouses.length > 0) setSelectedHub(warehouses[0]);
                }}
                className="px-4 py-2.5 bg-navy-900 text-white rounded-xl text-sm font-semibold hover:bg-navy-800 transition flex items-center gap-2 shadow-sm"
              >
                <Zap className="w-4 h-4 text-mustard-400" />
                {showOptimizer ? 'Hide Multi-Leg Planner' : 'Plan Multi-Leg Consolidation'}
              </button>
            </div>
          </div>

          {/* Alerts */}
          {error && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-2xl text-sm text-red-700 flex items-center gap-2">
              <AlertCircle className="w-5 h-5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Aggregation & Multi-Leg Route Optimizer Card */}
          {showOptimizer && (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-mustard-300 shadow-md">
              <div className="flex items-start justify-between gap-4 pb-4 border-b border-gray-100 mb-6">
                <div>
                  <span className="text-xs font-bold text-mustard-600 uppercase tracking-wider">Multi-Party Aggregation</span>
                  <h2 className="text-lg font-bold text-navy-900 mt-0.5">Multi-Leg Route & Consolidation Calculator</h2>
                  <p className="text-xs text-gray-500 mt-1">
                    Aggregate smallholder farmer batches at a regional hub (Leg 1) then dispatch a consolidated full truckload to the buyer (Leg 2).
                  </p>
                </div>
                <span className="px-3 py-1 bg-mustard-100 text-mustard-800 text-xs font-bold rounded-full">
                  Capacity Aware
                </span>
              </div>

              <div className="grid md:grid-cols-3 gap-6">
                {/* Hub Selection */}
                <div>
                  <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                    Consolidation Hub
                  </label>
                  <select
                    value={selectedHub?.id || ''}
                    onChange={(e) => {
                      const found = warehouses.find((w) => String(w.id) === e.target.value);
                      setSelectedHub(found || null);
                    }}
                    className="w-full px-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-navy-800"
                  >
                    <option value="" disabled>Select Consolidation Hub</option>
                    {warehouses.map((w) => (
                      <option key={w.id} value={w.id}>
                        {w.name} ({w.city}, {w.state})
                      </option>
                    ))}
                  </select>
                  {selectedHub && (
                    <div className="mt-3 p-3 bg-navy-50 rounded-xl border border-navy-100 text-xs text-navy-800 space-y-1">
                      <p className="font-semibold">{selectedHub.name}</p>
                      <p className="text-navy-600">{selectedHub.address}</p>
                      <p className="text-navy-600">Capacity: {(selectedHub.capacityKg / 1000).toFixed(0)} MT ({selectedHub.hasColdStorage ? 'Cold Stored' : 'Ambient'})</p>
                    </div>
                  )}
                </div>

                {/* Farmer Pickups */}
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-semibold text-gray-700 uppercase">
                      Leg 1: Farmer Pickups ({farmerStops.length})
                    </label>
                    <button
                      type="button"
                      onClick={() => setFarmerStops([...farmerStops, { name: `Farm ${String.fromCharCode(65 + farmerStops.length)}`, lat: 20.0, lng: 74.0, weightKg: 1000 }])}
                      className="text-xs text-navy-900 font-bold hover:underline"
                    >
                      + Add Farm
                    </button>
                  </div>
                  <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                    {farmerStops.map((stop, idx) => (
                      <div key={idx} className="p-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs space-y-1.5">
                        <div className="flex items-center justify-between">
                          <input
                            type="text"
                            value={stop.name}
                            onChange={(e) => {
                              const updated = [...farmerStops];
                              updated[idx].name = e.target.value;
                              setFarmerStops(updated);
                            }}
                            className="font-semibold bg-transparent text-navy-900 focus:outline-none w-2/3"
                          />
                          {farmerStops.length > 1 && (
                            <button
                              type="button"
                              onClick={() => setFarmerStops(farmerStops.filter((_, i) => i !== idx))}
                              className="text-[10px] text-red-600 hover:underline"
                            >
                              Remove
                            </button>
                          )}
                        </div>
                        <div className="grid grid-cols-3 gap-1 text-[11px]">
                          <input
                            type="number"
                            step="any"
                            value={stop.lat}
                            onChange={(e) => {
                              const updated = [...farmerStops];
                              updated[idx].lat = e.target.value;
                              setFarmerStops(updated);
                            }}
                            placeholder="Lat"
                            className="px-1.5 py-1 bg-white border border-gray-200 rounded"
                          />
                          <input
                            type="number"
                            step="any"
                            value={stop.lng}
                            onChange={(e) => {
                              const updated = [...farmerStops];
                              updated[idx].lng = e.target.value;
                              setFarmerStops(updated);
                            }}
                            placeholder="Lng"
                            className="px-1.5 py-1 bg-white border border-gray-200 rounded"
                          />
                          <input
                            type="number"
                            value={stop.weightKg}
                            onChange={(e) => {
                              const updated = [...farmerStops];
                              updated[idx].weightKg = e.target.value;
                              setFarmerStops(updated);
                            }}
                            placeholder="kg"
                            className="px-1.5 py-1 bg-white border border-gray-200 rounded"
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Leg 2: Buyer Drop-off & Truck Capacity */}
                <div className="space-y-3">
                  <label className="block text-xs font-semibold text-gray-700 uppercase">
                    Leg 2: Final Buyer Destination
                  </label>
                  <input
                    type="text"
                    value={buyerDest.name}
                    onChange={(e) => setBuyerDest({ ...buyerDest, name: e.target.value })}
                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                    placeholder="Destination Name"
                  />
                  <div className="grid grid-cols-2 gap-2 text-xs">
                    <div>
                      <span className="text-[10px] text-gray-500">Buyer Lat</span>
                      <input
                        type="number"
                        step="any"
                        value={buyerDest.lat}
                        onChange={(e) => setBuyerDest({ ...buyerDest, lat: e.target.value })}
                        className="w-full px-2 py-1.5 bg-gray-50 border border-gray-200 rounded-lg text-xs"
                      />
                    </div>
                    <div>
                      <span className="text-[10px] text-gray-500">Buyer Lng</span>
                      <input
                        type="number"
                        step="any"
                        value={buyerDest.lng}
                        onChange={(e) => setBuyerDest({ ...buyerDest, lng: e.target.value })}
                        className="w-full px-2 py-1.5 bg-gray-50 border border-gray-200 rounded-lg text-xs"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="text-xs font-semibold text-gray-700 uppercase">
                      Vehicle Capacity (kg)
                    </label>
                    <input
                      type="number"
                      value={truckCapacity}
                      onChange={(e) => setTruckCapacity(e.target.value)}
                      className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                    />
                  </div>
                </div>
              </div>

              <div className="mt-6 pt-4 border-t border-gray-100 flex flex-col sm:flex-row items-center justify-between gap-4">
                <p className="text-xs text-gray-500">
                  Total Payload: {farmerStops.reduce((sum, s) => sum + (parseFloat(s.weightKg) || 0), 0)} kg / {truckCapacity} kg
                </p>
                <button
                  type="button"
                  onClick={handleRunMultiLegOptimization}
                  disabled={optimizing || !selectedHub}
                  className="px-6 py-2.5 bg-navy-900 text-white rounded-xl text-sm font-semibold hover:bg-navy-800 disabled:opacity-50 transition flex items-center gap-2"
                >
                  {optimizing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Navigation className="w-4 h-4" />}
                  {optimizing ? 'Calculating Multi-Leg Path...' : 'Calculate Consolidated Route'}
                </button>
              </div>

              {routeError && (
                <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700">
                  {routeError}
                </div>
              )}

              {routeResult && (
                <div className="mt-6 p-5 bg-emerald-50/70 border border-emerald-200 rounded-2xl space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <ShieldCheck className="w-5 h-5 text-emerald-700" />
                      <h4 className="font-bold text-navy-900 text-sm">Consolidated Route Plan Generated</h4>
                    </div>
                    <span className="px-2.5 py-1 bg-emerald-100 text-emerald-800 text-xs font-bold rounded-lg">
                      ~25-35% Cost Savings vs Separate Shipments
                    </span>
                  </div>

                  <div className="grid sm:grid-cols-3 gap-3 text-center">
                    <div className="p-3 bg-white rounded-xl border border-emerald-100">
                      <p className="text-[10px] text-gray-500 uppercase font-semibold">Leg 1 (Farms → Hub)</p>
                      <p className="text-base font-bold text-navy-900 mt-0.5">{routeResult.leg1DistanceKm || 0} km</p>
                      <p className="text-[10px] text-gray-400">Aggregation Collection</p>
                    </div>
                    <div className="p-3 bg-white rounded-xl border border-emerald-100">
                      <p className="text-[10px] text-gray-500 uppercase font-semibold">Leg 2 (Hub → Buyer)</p>
                      <p className="text-base font-bold text-navy-900 mt-0.5">{routeResult.leg2DistanceKm || 0} km</p>
                      <p className="text-[10px] text-gray-400">Bulk Freight Dispatch</p>
                    </div>
                    <div className="p-3 bg-white rounded-xl border border-emerald-100">
                      <p className="text-[10px] text-gray-500 uppercase font-semibold">Total Multi-Leg Distance</p>
                      <p className="text-base font-bold text-emerald-700 mt-0.5">{routeResult.totalDistanceKm || 0} km</p>
                      <p className="text-[10px] text-emerald-600 font-medium">Consolidated Single Carrier</p>
                    </div>
                  </div>

                  {routeResult.orderedStops && routeResult.orderedStops.length > 0 && (
                    <div className="text-xs text-gray-700 pt-2 border-t border-emerald-100">
                      <p className="font-semibold text-navy-900 mb-1">Optimized Sequence:</p>
                      <div className="flex flex-wrap items-center gap-1.5">
                        <span className="px-2 py-0.5 bg-white border border-gray-200 rounded text-gray-600">Start (Farm 1)</span>
                        {routeResult.orderedStops.map((st, i) => (
                          <span key={i} className="flex items-center gap-1.5">
                            <ArrowRight className="w-3 h-3 text-emerald-600" />
                            <span className="px-2 py-0.5 bg-white border border-gray-200 rounded font-medium text-navy-800">
                              {st.name || `Stop ${i + 1}`}
                            </span>
                          </span>
                        ))}
                        <ArrowRight className="w-3 h-3 text-emerald-600" />
                        <span className="px-2 py-0.5 bg-navy-900 text-white rounded font-medium">
                          {selectedHub?.name || 'Warehouse Hub'}
                        </span>
                        <ArrowRight className="w-3 h-3 text-emerald-600" />
                        <span className="px-2 py-0.5 bg-emerald-700 text-white rounded font-medium">
                          {buyerDest.name}
                        </span>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Stats Summary Bar */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-white p-4 rounded-2xl border border-navy-100 shadow-sm">
              <p className="text-xs text-gray-500 font-medium">Active Hubs</p>
              <p className="text-2xl font-bold text-navy-900 mt-1">{warehouses.length}</p>
              <p className="text-[11px] text-emerald-600 mt-0.5">Across Key Agri Corridors</p>
            </div>
            <div className="bg-white p-4 rounded-2xl border border-navy-100 shadow-sm">
              <p className="text-xs text-gray-500 font-medium">Cold Chain Capable</p>
              <p className="text-2xl font-bold text-navy-900 mt-1">
                {warehouses.filter((w) => w.hasColdStorage).length}
              </p>
              <p className="text-[11px] text-sky-600 mt-0.5">2°C – 8°C Pre-Cooling</p>
            </div>
            <div className="bg-white p-4 rounded-2xl border border-navy-100 shadow-sm">
              <p className="text-xs text-gray-500 font-medium">Total Network Capacity</p>
              <p className="text-2xl font-bold text-navy-900 mt-1">
                {(warehouses.reduce((sum, w) => sum + (w.capacityKg || 0), 0) / 1000).toFixed(0)} MT
              </p>
              <p className="text-[11px] text-gray-400 mt-0.5">Metric Tons Available</p>
            </div>
            <div className="bg-white p-4 rounded-2xl border border-navy-100 shadow-sm">
              <p className="text-xs text-gray-500 font-medium">Multi-Leg Aggregation</p>
              <p className="text-2xl font-bold text-emerald-700 mt-1">Enabled</p>
              <p className="text-[11px] text-gray-400 mt-0.5">Smallholder Pooling Active</p>
            </div>
          </div>

          {/* Search and Filters */}
          <div className="bg-white rounded-2xl p-4 border border-navy-100 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="relative w-full sm:w-80">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search hub by city, state or code..."
                className="w-full pl-9 pr-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-navy-800"
              />
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto">
              <button
                onClick={() => setFilterType('ALL')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                  filterType === 'ALL' ? 'bg-navy-900 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                All Hubs ({warehouses.length})
              </button>
              <button
                onClick={() => setFilterType('COLD')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition flex items-center gap-1 ${
                  filterType === 'COLD' ? 'bg-sky-700 text-white' : 'bg-sky-50 text-sky-700 hover:bg-sky-100'
                }`}
              >
                <Thermometer className="w-3.5 h-3.5" /> Cold Storage
              </button>
              <button
                onClick={() => setFilterType('AMBIENT')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                  filterType === 'AMBIENT' ? 'bg-amber-700 text-white' : 'bg-amber-50 text-amber-700 hover:bg-amber-100'
                }`}
              >
                Ambient Only
              </button>
            </div>
          </div>

          {/* Map and Hub Cards Layout */}
          <div className="grid lg:grid-cols-12 gap-6">

            {/* Interactive Leaflet Map */}
            <div className="lg:col-span-5 bg-white rounded-3xl border border-navy-100 shadow-sm overflow-hidden flex flex-col h-[480px]">
              <div className="p-3.5 px-4 bg-gray-50 border-b border-gray-100 flex items-center justify-between text-xs font-semibold text-navy-900">
                <span className="flex items-center gap-1.5">
                  <MapPin className="w-4 h-4 text-navy-800" /> Regional Hub Locations Map
                </span>
                <span className="text-[11px] text-gray-400 font-normal">India Agri Corridors</span>
              </div>
              <div className="flex-1 w-full h-full relative">
                {loading ? (
                  <div className="flex items-center justify-center h-full">
                    <Loader2 className="w-8 h-8 animate-spin text-navy-900" />
                  </div>
                ) : (
                  <MapContainer
                    center={[20.5937, 78.9629]}
                    zoom={5}
                    style={{ height: '100%', width: '100%' }}
                    scrollWheelZoom={false}
                  >
                    <TileLayer
                      attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                      url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />
                    {filteredWarehouses.map((wh) => (
                      <Marker
                        key={wh.id}
                        position={[wh.latitude, wh.longitude]}
                        icon={makeHubIcon(wh.hasColdStorage)}
                      >
                        <Popup>
                          <div className="p-1 max-w-[200px] text-xs">
                            <p className="font-bold text-navy-900">{wh.name}</p>
                            <p className="text-gray-500 mt-0.5">{wh.city}, {wh.state}</p>
                            <span className={`inline-block mt-1 px-1.5 py-0.5 rounded text-[10px] font-bold ${
                              wh.hasColdStorage ? 'bg-sky-100 text-sky-800' : 'bg-amber-100 text-amber-800'
                            }`}>
                              {wh.hasColdStorage ? '❄ Cold Storage (2°-8°C)' : '📦 Ambient'}
                            </span>
                            <p className="text-gray-600 mt-1 font-medium">
                              Capacity: {(wh.capacityKg / 1000).toFixed(0)} MT
                            </p>
                            <button
                              onClick={() => {
                                setSelectedHub(wh);
                                setShowOptimizer(true);
                              }}
                              className="mt-2 w-full py-1 bg-navy-900 text-white rounded text-[10px] font-bold hover:bg-navy-800"
                            >
                              Select for Aggregation
                            </button>
                          </div>
                        </Popup>
                      </Marker>
                    ))}
                  </MapContainer>
                )}
              </div>
              <div className="p-2.5 px-4 bg-gray-50 border-t border-gray-100 flex items-center justify-between text-[11px] text-gray-500">
                <div className="flex items-center gap-3">
                  <span className="flex items-center gap-1">
                    <span className="w-2.5 h-2.5 rounded bg-sky-600 inline-block" /> Cold Storage Hub
                  </span>
                  <span className="flex items-center gap-1">
                    <span className="w-2.5 h-2.5 rounded bg-amber-600 inline-block" /> Ambient Hub
                  </span>
                </div>
                <span className="text-[10px] text-gray-400">OpenStreetMap</span>
              </div>
            </div>

            {/* Warehouse Cards List */}
            <div className="lg:col-span-7 space-y-4">
              {loading ? (
                <div className="p-12 text-center bg-white rounded-3xl border border-navy-100">
                  <Loader2 className="w-8 h-8 animate-spin text-navy-900 mx-auto mb-2" />
                  <p className="text-xs text-gray-500">Loading consolidation hubs...</p>
                </div>
              ) : filteredWarehouses.length === 0 ? (
                <div className="p-12 text-center bg-white rounded-3xl border border-navy-100">
                  <WarehouseIcon className="w-10 h-10 text-gray-300 mx-auto mb-2" />
                  <p className="text-sm font-semibold text-navy-900">No consolidation hubs found</p>
                  <p className="text-xs text-gray-500 mt-1">Try changing your search query or filter.</p>
                </div>
              ) : (
                <div className="space-y-4 max-h-[560px] overflow-y-auto pr-1">
                  {filteredWarehouses.map((wh) => {
                    const usagePercent = wh.capacityKg > 0
                      ? Math.min(100, Math.round(((wh.currentStoredKg || 0) / wh.capacityKg) * 100))
                      : 0;

                    return (
                      <div
                        key={wh.id}
                        className={`bg-white rounded-2xl p-5 border transition shadow-sm ${
                          selectedHub?.id === wh.id
                            ? 'border-mustard-400 ring-2 ring-mustard-200'
                            : 'border-navy-100 hover:border-navy-300'
                        }`}
                      >
                        <div className="flex items-start justify-between gap-4">
                          <div>
                            <div className="flex items-center gap-2">
                              <h3 className="font-bold text-navy-900 text-base">{wh.name}</h3>
                              <span className="px-2 py-0.5 bg-gray-100 text-gray-600 text-[10px] font-mono rounded">
                                {wh.code}
                              </span>
                            </div>
                            <p className="text-xs text-gray-500 mt-1 flex items-center gap-1">
                              <MapPin className="w-3.5 h-3.5 text-gray-400" />
                              {wh.address}, {wh.city}, {wh.state} - {wh.pincode}
                            </p>
                          </div>

                          <span className={`px-2.5 py-1 text-xs font-bold rounded-xl flex items-center gap-1 ${
                            wh.hasColdStorage
                              ? 'bg-sky-50 text-sky-700 border border-sky-200'
                              : 'bg-amber-50 text-amber-700 border border-amber-200'
                          }`}>
                            {wh.hasColdStorage ? (
                              <><Thermometer className="w-3.5 h-3.5" /> 2°C – 8°C Cold Hub</>
                            ) : (
                              <><Package className="w-3.5 h-3.5" /> Ambient Dry Hub</>
                            )}
                          </span>
                        </div>

                        {/* Capacity Bar */}
                        <div className="mt-4">
                          <div className="flex items-center justify-between text-xs text-gray-500 mb-1">
                            <span>Storage Utilization</span>
                            <span className="font-semibold text-navy-900">
                              {((wh.currentStoredKg || 0) / 1000).toFixed(1)} / {(wh.capacityKg / 1000).toFixed(0)} MT ({usagePercent}%)
                            </span>
                          </div>
                          <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                            <div
                              className={`h-full rounded-full ${
                                usagePercent > 85 ? 'bg-red-500' : usagePercent > 60 ? 'bg-amber-500' : 'bg-emerald-500'
                              }`}
                              style={{ width: `${usagePercent}%` }}
                            />
                          </div>
                        </div>

                        {/* Contact & Actions */}
                        <div className="mt-4 pt-3 border-t border-gray-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                          <div className="flex items-center gap-4 text-gray-600">
                            {wh.contactPhone && (
                              <span className="flex items-center gap-1">
                                <Phone className="w-3 h-3 text-gray-400" /> {wh.contactPhone}
                              </span>
                            )}
                            {wh.managerName && (
                              <span className="text-gray-500">
                                Mgr: <span className="text-navy-900 font-medium">{wh.managerName}</span>
                              </span>
                            )}
                          </div>

                          <div className="flex items-center gap-2">
                            <a
                              href={`https://www.google.com/maps?q=${wh.latitude},${wh.longitude}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="px-3 py-1.5 bg-gray-100 text-gray-700 rounded-lg text-xs font-semibold hover:bg-gray-200 transition"
                            >
                              Directions
                            </a>
                            <button
                              onClick={() => {
                                setSelectedHub(wh);
                                setShowOptimizer(true);
                              }}
                              className="px-3 py-1.5 bg-navy-900 text-white rounded-lg text-xs font-semibold hover:bg-navy-800 transition flex items-center gap-1"
                            >
                              Consolidate Here <ArrowRight className="w-3 h-3" />
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}
