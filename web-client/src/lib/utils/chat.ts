import axios from 'axios';

export type ChatErrorType = 'bad-request' | 'rate-limited' | 'service-unavailable' | 'network' | 'unknown';

export interface ClassifiedError {
  type: ChatErrorType;
  message: string;
}

export function classifyChatError(error: unknown): ClassifiedError {
  if (!axios.isAxiosError(error)) {
    return { type: 'unknown', message: 'Something went wrong. Please try again.' };
  }

  if (!error.response) {
    // No HTTP response means the request never reached a 2xx/4xx/5xx reply — the
    // most common cause is the backend chat service (genai-service → Ollama)
    // hanging, crashing, or being unreachable while the browser was waiting.
    return {
      type: 'service-unavailable',
      message: 'The AI service is unreachable. Please retry in a moment.',
    };
  }

  switch (error.response.status) {
    case 400:
      return { type: 'bad-request', message: "Couldn't process that request." };
    case 429:
      return { type: 'rate-limited', message: "You're sending messages too fast. Please wait a moment." };
    case 503:
      return { type: 'service-unavailable', message: 'The AI service is temporarily unavailable.' };
    default:
      return { type: 'unknown', message: 'Something went wrong. Please try again.' };
  }
}
