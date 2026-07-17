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
    return { type: 'network', message: 'Connection lost. Check your internet.' };
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
