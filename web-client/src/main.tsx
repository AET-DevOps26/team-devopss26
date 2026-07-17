import ReactDOM from 'react-dom/client';
import App from './App';

/**
 * Application bootstrap. Throws if `#app` element is missing.
 * Guards against double-rendering in HMR by checking innerHTML.
 * StrictMode intentionally omitted (causes double-invocation issues
 * with third-party libraries). Re-evaluate on next React major version.
 */
const rootElement = document.getElementById('app');
if (!rootElement) throw new Error('Root element #app not found');

if (!rootElement.innerHTML) {
  const root = ReactDOM.createRoot(rootElement);
  root.render(<App />);
}
