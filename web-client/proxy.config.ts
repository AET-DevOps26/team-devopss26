import type { ProxyOptions } from 'vite';

export const proxyConfig: Record<string, string | ProxyOptions> = {
  '/api/v1/users': {
    target: 'http://localhost:8001',
    changeOrigin: true,
  },
  '/api/v1/admin': {
    target: 'http://localhost:8002',
    changeOrigin: true,
  },
  '/api/v1/checklists': {
    target: 'http://localhost:8003',
    changeOrigin: true,
  },
  '/api/v1/events': {
    target: 'http://localhost:8004',
    changeOrigin: true,
  },
  '/api/v1/notes': {
    target: 'http://localhost:8005',
    changeOrigin: true,
  },
  '/api/v1/conversations': {
    target: 'http://localhost:8006',
    changeOrigin: true,
  },
  '/api/v1/health': {
    target: 'http://localhost:8006',
    changeOrigin: true,
  },
};
