import { useState, useCallback } from 'react';
import { optimizeRoute } from '../api/dealApi';

export function useRouteOptimization() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);

  const runOptimization = useCallback(async ({ origin, stops, capacityKg = 5000 }) => {
    if (!origin || !Array.isArray(origin) || origin.length < 2) {
      setError('Origin [latitude, longitude] is required.');
      return null;
    }
    if (!stops || !Array.isArray(stops) || stops.length === 0) {
      setError('At least one pickup or delivery stop is required.');
      return null;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await optimizeRoute({
        origin: [Number(origin[0]), Number(origin[1])],
        stops: stops.map((s) => ({
          lat: Number(s.lat ?? s.latitude),
          lng: Number(s.lng ?? s.longitude),
          label: s.label || s.name || 'Stop',
          weightKg: Number(s.weightKg ?? s.quantity ?? 500),
        })),
        capacityKg: Number(capacityKg),
      });

      const payload = response?.data || response;
      setResult(payload);
      return payload;
    } catch (err) {
      const message = err?.message || 'Route optimization failed. Please check stops.';
      setError(message);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  const clear = useCallback(() => {
    setResult(null);
    setError(null);
    setLoading(false);
  }, []);

  return {
    loading,
    error,
    result,
    runOptimization,
    clear,
  };
}
