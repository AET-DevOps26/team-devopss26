import { defineConfig } from 'orval';

export default defineConfig({
  userService: {
    input: '../api/user-service.yaml',
    output: {
      target: 'src/services/users',
      client: 'axios-functions',
      mode: 'tags-split',
      schemas: 'src/types/users',
      override: {
        mutator: {
          path: 'src/lib/api/client.ts',
          name: 'customInstance',
        },
      },
      clean: true,
    },
  },
  noteService: {
    input: '../api/note-service.yaml',
    output: {
      target: 'src/services/notes',
      client: 'axios-functions',
      mode: 'tags-split',
      schemas: 'src/types/notes',
      override: {
        mutator: {
          path: 'src/lib/api/client.ts',
          name: 'customInstance',
        },
      },
      clean: true,
    },
  },
  checklistService: {
    input: '../api/checklist-service.yaml',
    output: {
      target: 'src/services/checklist',
      client: 'axios-functions',
      mode: 'tags-split',
      schemas: 'src/types/checklist',
      override: {
        mutator: {
          path: 'src/lib/api/client.ts',
          name: 'customInstance',
        },
      },
      clean: true,
    },
  },
  calendarService: {
    input: '../api/calendar-service.yaml',
    output: {
      target: 'src/services/calendar',
      client: 'axios-functions',
      mode: 'tags-split',
      schemas: 'src/types/calendar',
      override: {
        mutator: {
          path: 'src/lib/api/client.ts',
          name: 'customInstance',
        },
      },
      clean: true,
    },
  },
  genaiService: {
    input: '../api/genai-service.yaml',
    output: {
      target: 'src/services/genai',
      client: 'axios-functions',
      mode: 'tags-split',
      schemas: 'src/types/genai',
      override: {
        mutator: {
          path: 'src/lib/api/client.ts',
          name: 'customInstance',
        },
      },
      clean: true,
    },
  },
  adminService: {
    input: '../api/admin-service.yaml',
    output: {
      target: 'src/services/admin',
      client: 'axios-functions',
      mode: 'tags-split',
      schemas: 'src/types/admin',
      override: {
        mutator: {
          path: 'src/lib/api/client.ts',
          name: 'customInstance',
        },
      },
      clean: true,
    },
  },
});
