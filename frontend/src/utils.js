export const formatDateTime = (value) => (value ? new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '');

export const formatSeconds = (s) => {
  const m = Math.floor(s / 60).toString().padStart(2, '0');
  const sec = Math.floor(s % 60).toString().padStart(2, '0');
  return `${m}:${sec}`;
};

export const scoreColor = (pct) => (pct >= 75 ? 'success' : pct >= 40 ? 'warning' : 'danger');

export const formatDate = (value) => (value ? new Date(value).toLocaleDateString('en-IN') : '');
