import '@testing-library/jest-dom/vitest';
import axios from 'axios';
import { setupServer } from 'msw/node';
import { handlers } from './mocks/handlers';
import { afterAll, afterEach, beforeAll } from 'vitest';

// jsdom XHR is not intercepted by msw/node — use the http adapter instead
axios.defaults.adapter = 'http';

// localStorage polyfill for test environments where jsdom doesn't provide it
if (typeof localStorage === 'undefined' || localStorage === null) {
  const store: Record<string, string> = {};
  const localStorageMock: Storage = {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, value: string) => { store[key] = value; },
    removeItem: (key: string) => { delete store[key]; },
    clear: () => { for (const k of Object.keys(store)) delete store[k]; },
    key: (index: number) => Object.keys(store)[index] ?? null,
    get length() { return Object.keys(store).length; },
  };
  global.localStorage = localStorageMock;
}

export const server = setupServer(...handlers);

beforeAll(() => { server.listen({ onUnhandledRequest: 'warn' }); });
afterEach(() => { server.resetHandlers(); });
afterAll(() => { server.close(); });
