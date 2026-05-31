export function money(value: number | string | null | undefined): string {
  const numberValue = Number(value ?? 0);
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: numberValue >= 1 ? 2 : 6,
    maximumFractionDigits: numberValue >= 1 ? 2 : 8
  }).format(numberValue);
}

export function compactNumber(value: number | string | null | undefined): string {
  return new Intl.NumberFormat('en-US', {
    notation: 'compact',
    maximumFractionDigits: 1
  }).format(Number(value ?? 0));
}

export function integer(value: number | string | null | undefined): string {
  return new Intl.NumberFormat('en-US').format(Number(value ?? 0));
}

export function timestamp(value: string | null | undefined): string {
  if (!value) {
    return 'Unknown';
  }
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  }).format(new Date(value));
}

export function percent(value: number | string | null | undefined): string {
  return `${Number(value ?? 0).toFixed(1)}%`;
}
