import { describe, it, expect } from 'vitest';
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
});
