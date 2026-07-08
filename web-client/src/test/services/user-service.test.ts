import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '../setup';
import { registerUser, loginUser, checkToken } from '../../services/users/user-authentication/user-authentication';

describe('user service', () => {
  it('registerUser sends POST to /api/v1/users/auth/register', async () => {
    await expect(registerUser({ username: 'testuser', password: 'password123' })).resolves.toBeUndefined();
  });

  it('loginUser sends POST to /api/v1/users/auth/login and returns token', async () => {
    const result = await loginUser();
    expect(result).toHaveProperty('token');
    expect(typeof result.token).toBe('string');
  });

  it('checkToken sends GET to /api/v1/users/auth/check-token', async () => {
    await expect(checkToken()).resolves.toBeUndefined();
  });

  it('registerUser throws on 409 conflict', async () => {
    server.use(
      http.post('*/api/v1/users/auth/register', () => HttpResponse.json(null, { status: 409 })),
    );
    await expect(registerUser({ username: 'test', password: 'test' })).rejects.toBeDefined();
  });

  it('loginUser throws on 401 invalid credentials', async () => {
    server.use(
      http.post('*/api/v1/users/auth/login', () => HttpResponse.json(null, { status: 401 })),
    );
    await expect(loginUser()).rejects.toBeDefined();
  });
});
