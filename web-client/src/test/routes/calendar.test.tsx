import { describe, it, expect, afterEach, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../setup';
import { renderWithClient } from '../test-utils';
import { CalendarPage } from '#/routes/_authenticated/calendar';

// useSearch requires a RouterProvider context; mock it for isolated component tests
vi.mock('@tanstack/react-router', async () => {
  const actual = await vi.importActual('@tanstack/react-router');
  return {
    ...actual,
    useSearch: () => ({ action: undefined, date: undefined }),
  };
});

/** Format a Date as YYYY-MM-DD in local timezone. */
function localDateStr(date: Date): string {
  return `${String(date.getFullYear())}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

const todayStr = localDateStr(new Date());

const mockEvents = {
  events: [
    {
      id: 1,
      title: 'Team standup',
      description: 'Daily sync',
      startTime: `${todayStr}T10:00:00Z`,
      endTime: `${todayStr}T10:30:00Z`,
    },
    {
      id: 2,
      title: 'Design review',
      description: 'Review mockups',
      startTime: `${todayStr}T14:00:00Z`,
      endTime: `${todayStr}T15:00:00Z`,
    },
  ],
};

describe('Calendar route', () => {
  afterEach(() => {
    server.resetHandlers();
  });

  it('renders events from the API', async () => {
    server.use(
      http.get('*/api/v1/events', () => HttpResponse.json(mockEvents)),
    );

    renderWithClient(<CalendarPage />, {
      suspenseFallback: <div data-testid="loading">Loading...</div>,
    });

    await waitFor(() => {
      expect(screen.getByText('Team standup')).toBeInTheDocument();
    });
    expect(screen.getByText('Design review')).toBeInTheDocument();
  });

  it('shows empty state when no events exist', async () => {
    server.use(
      http.get('*/api/v1/events', () => HttpResponse.json({ events: [] })),
    );

    renderWithClient(<CalendarPage />, {
      suspenseFallback: <div data-testid="loading">Loading...</div>,
    });

    await waitFor(() => {
      expect(screen.getByText('No events yet')).toBeInTheDocument();
    });
    expect(screen.getByText('Create Event')).toBeInTheDocument();
  });

  it('opens create sheet on button click', async () => {
    server.use(
      http.get('*/api/v1/events', () => HttpResponse.json(mockEvents)),
    );

    renderWithClient(<CalendarPage />, {
      suspenseFallback: <div data-testid="loading">Loading...</div>,
    });

    await screen.findByText('Team standup');

    await userEvent.click(screen.getByRole('button', { name: /event/i }));

    expect(screen.getByText('New Event')).toBeInTheDocument();
  });

  it('shows "Nothing scheduled today" when no events match today', async () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = localDateStr(tomorrow);

    server.use(
      http.get('*/api/v1/events', () =>
        HttpResponse.json({
          events: [
            {
              id: 3,
              title: 'Future event',
              description: '',
              startTime: `${tomorrowStr}T10:00:00Z`,
              endTime: `${tomorrowStr}T11:00:00Z`,
            },
          ],
        }),
      ),
    );

    renderWithClient(<CalendarPage />, {
      suspenseFallback: <div data-testid="loading">Loading...</div>,
    });

    await waitFor(() => {
      expect(screen.getByText('Nothing scheduled today')).toBeInTheDocument();
    });
  });
});
