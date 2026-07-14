import '@testing-library/jest-dom/vitest';
import axios from 'axios';
import {setupServer} from 'msw/node';
import {handlers} from './mocks/handlers';
import {afterAll, afterEach, beforeAll} from 'vitest';

// jsdom XHR is not intercepted by msw/node — use the http adapter instead
axios.defaults.adapter = 'http';

// localStorage polyfill for test environments where jsdom doesn't provide it
if (typeof localStorage === 'undefined') {
  const store: Record<string, string> = {};
  global.localStorage = {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, value: string) => {
      store[key] = value;
    },
    removeItem: (key: string) => {
      Reflect.deleteProperty(store, key);
    },
    clear: () => {
      for (const k of Object.keys(store)) Reflect.deleteProperty(store, k);
    },
    key: (index: number) => Object.keys(store)[index] ?? null,
    get length() {
      return Object.keys(store).length;
    },
  };
}

export const server = setupServer(...handlers);

beforeAll(() => { server.listen({ onUnhandledRequest: 'warn' }); });
afterEach(() => { server.resetHandlers(); });
afterAll(() => { server.close(); });
