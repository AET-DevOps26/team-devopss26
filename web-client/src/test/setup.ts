import '@testing-library/jest-dom/vitest';
import axios from 'axios';
import { setupServer } from 'msw/node';
import { handlers } from './mocks/handlers';
import { afterAll, afterEach, beforeAll } from 'vitest';

// jsdom XHR is not intercepted by msw/node — use the http adapter instead
axios.defaults.adapter = 'http';

export const server = setupServer(...handlers);

beforeAll(() => { server.listen({ onUnhandledRequest: 'bypass' }); });
afterEach(() => { server.resetHandlers(); });
afterAll(() => { server.close(); });
