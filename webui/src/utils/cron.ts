/**
 * 根据 Spring 格式的 cron 表达式（6 位：秒 分 时 日 月 星期）计算下一次执行时间。
 * 返回 ISO 字符串，解析失败返回 null。
 */
export const WEEKDAYS_ZH = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

/** 将 Spring 格式的 cron 表达式转换为人类可读文本 */
export function cronToReadable(cron: string): string | null {
  if (!cron || typeof cron !== 'string') return null;
  const parts = cron.trim().split(/\s+/);
  if (parts.length < 6) return null;

  const [_sec, minute, hour, dom, _month, dow] = parts;

  const timeStr = `${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`;

  if (dow !== '*' && dom === '*') {
    const dayName = parseInt(dow, 10);
    return `每周${WEEKDAYS_ZH[dayName]} ${timeStr}`;
  }
  if (dom !== '*' && dow === '*') {
    return `每月${dom}日 ${timeStr}`;
  }
  if (dom === '*' && dow === '*') {
    return `每天 ${timeStr}`;
  }
  return null;
}

/** 格式化相对时间，如 "明天 09:00"、"约 24 小时后" */
export function formatRelativeTime(isoString: string): string {
  const target = new Date(isoString);
  const now = new Date();
  const diffMs = target.getTime() - now.getTime();

  if (diffMs < 0) return '已过期';

  const diffMins = Math.round(diffMs / 60000);
  const diffHours = Math.round(diffMs / 3600000);

  if (diffMins < 1) return '即将执行';
  if (diffMins < 60) return `${diffMins} 分钟后`;

  if (diffHours < 24) {
    const timeStr = target.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    if (isSameDay(now, target)) return `今天 ${timeStr}`;
    return `${diffHours} 小时后`;
  }

  const diffDays = Math.round(diffMs / 86400000);
  const timeStr = target.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

  const tomorrow = new Date(now);
  tomorrow.setDate(tomorrow.getDate() + 1);
  if (isSameDay(tomorrow, target)) return `明天 ${timeStr}`;

  if (diffDays <= 7) return `${diffDays} 天后`;
  return target.toLocaleDateString('zh-CN');
}

function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

/** 截断长文本，保留首尾各指定长度 */
export function truncateMiddle(text: string, head = 8, tail = 8): string {
  if (text.length <= head + tail + 1) return text;
  return `${text.slice(0, head)}…${text.slice(-tail)}`;
}

export function getNextCronTime(cron: string): string | null {
  if (!cron || typeof cron !== 'string') return null;

  const parts = cron.trim().split(/\s+/);
  if (parts.length < 6) return null;

  const [secondField, minuteField, hourField, domField, _monthField, dowField] = parts;

  const seconds = parseField(secondField, 0, 59);
  const minutes = parseField(minuteField, 0, 59);
  const hours = parseField(hourField, 0, 23);
  const doms = parseField(domField, 1, 31);
  const dows = parseField(dowField, 0, 6);

  if (!seconds.length || !minutes.length || !hours.length || !doms.length || !dows.length) return null;

  const now = new Date();
  const searchUntil = new Date(now.getTime() + 24 * 60 * 60 * 1000);

  for (
    let d = new Date(now);
    d <= searchUntil;
    d.setSeconds(d.getSeconds() + 1)
  ) {
    const s = d.getSeconds();
    const m = d.getMinutes();
    const h = d.getHours();
    const day = d.getDate();
    const dow = d.getDay();

    if (
      seconds.includes(s) &&
      minutes.includes(m) &&
      hours.includes(h) &&
      doms.includes(day) &&
      dows.includes(dow) &&
      d > now
    ) {
      return d.toISOString();
    }
  }

  return null;
}

function parseField(field: string, min: number, max: number): number[] {
  const result = new Set<number>();

  for (const part of field.split(',')) {
    if (part.includes('/')) {
      const [range, stepStr] = part.split('/');
      const step = parseInt(stepStr, 10);
      if (isNaN(step) || step < 1) return [];

      let start: number;
      let end: number;
      if (range === '*') {
        start = min;
        end = max;
      } else if (range.includes('-')) {
        const [s, e] = range.split('-').map(Number);
        start = s;
        end = e;
      } else {
        start = parseInt(range, 10);
        end = max;
      }
      for (let i = start; i <= end; i += step) {
        if (i >= min && i <= max) result.add(i);
      }
    } else if (part.includes('-')) {
      const [s, e] = part.split('-').map(Number);
      for (let i = s; i <= e; i++) {
        if (i >= min && i <= max) result.add(i);
      }
    } else if (part === '*') {
      for (let i = min; i <= max; i++) result.add(i);
    } else {
      const v = parseInt(part, 10);
      if (v >= min && v <= max) result.add(v);
    }
  }

  return Array.from(result).sort((a, b) => a - b);
}
