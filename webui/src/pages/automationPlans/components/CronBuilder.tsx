import { Select, Space, InputNumber, Radio } from 'antd';
import { useState, useEffect } from 'react';
import type { CSSProperties } from 'react';

const WEEKDAYS = [
  { label: '周日', value: 0 },
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
];

const EVERY_HOUR_OPTIONS = Array.from({ length: 24 }, (_, i) => ({ label: `${i}点`, value: i }));
const EVERY_MINUTE_OPTIONS = [
  { label: '整点', value: 0 },
  { label: '半点', value: 30 },
  { label: '每15分', value: 15 },
  { label: '每10分', value: 10 },
  { label: '每5分', value: 5 },
];

interface CronBuilderProps {
  value?: string;
  onChange?: (value: string) => void;
}

const selectStyle: CSSProperties = {
  width: 100,
  borderRadius: 8,
};

const labelStyle: CSSProperties = {
  color: '#64748b',
  fontSize: 14,
  lineHeight: '32px',
};

export default function CronBuilder({ value, onChange }: CronBuilderProps) {
  const [mode, setMode] = useState<'daily' | 'weekly' | 'monthly'>('daily');
  const [hour, setHour] = useState(8);
  const [minute, setMinute] = useState(0);
  const [weekday, setWeekday] = useState(1);
  const [dayOfMonth, setDayOfMonth] = useState(1);

  // Spring cron uses 6 fields: second minute hour dom month dow
  // We always use second=0
  useEffect(() => {
    if (!value) return;
    const parts = value.trim().split(/\s+/);
    if (parts.length < 6) return;

    const [_sec, m, h, dom, _mon, dow] = parts;

    if (dow !== '*' && dom === '*') {
      setMode('weekly');
      setWeekday(parseInt(dow, 10));
    } else if (dom !== '*' && dow === '*') {
      setMode('monthly');
      setDayOfMonth(parseInt(dom, 10));
    } else {
      setMode('daily');
    }

    if (h !== '*') setHour(parseInt(h, 10));
    if (m !== '*') setMinute(parseInt(m, 10));
  }, [value]);

  const buildCron = (newMode: string, newHour: number, newMinute: number, newWeekday: number, newDayOfMonth: number) => {
    const sec = '0';
    const m = `${newMinute}`;
    const h = `${newHour}`;

    let cron: string;
    if (newMode === 'daily') {
      cron = `${sec} ${m} ${h} * * *`;
    } else if (newMode === 'weekly') {
      cron = `${sec} ${m} ${h} * * ${newWeekday}`;
    } else {
      cron = `${sec} ${m} ${h} ${newDayOfMonth} * *`;
    }

    onChange?.(cron);
  };

  const cronExpr = value || `0 ${minute} ${hour} * * *`;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <Radio.Group
        value={mode}
        onChange={(e) => {
          setMode(e.target.value);
          buildCron(e.target.value, hour, minute, weekday, dayOfMonth);
        }}
        optionType="button"
        buttonStyle="outline"
        size="middle"
      >
        <Radio.Button value="daily">每天</Radio.Button>
        <Radio.Button value="weekly">每周</Radio.Button>
        <Radio.Button value="monthly">每月</Radio.Button>
      </Radio.Group>

      <Space size={4} wrap>
        <span style={labelStyle}>在</span>
        <Select
          value={hour}
          onChange={(v) => { setHour(v); buildCron(mode, v, minute, weekday, dayOfMonth); }}
          options={EVERY_HOUR_OPTIONS}
          size="middle"
          style={selectStyle}
        />
        <Select
          value={minute}
          onChange={(v) => { setMinute(v); buildCron(mode, hour, v, weekday, dayOfMonth); }}
          options={EVERY_MINUTE_OPTIONS}
          size="middle"
          style={{ ...selectStyle, width: 100 }}
        />
        <span style={labelStyle}>执行</span>
      </Space>

      {mode === 'weekly' && (
        <Space size={4}>
          <span style={labelStyle}>在</span>
          <Select
            value={weekday}
            onChange={(v) => { setWeekday(v); buildCron(mode, hour, minute, v, dayOfMonth); }}
            options={WEEKDAYS}
            size="middle"
            style={selectStyle}
          />
        </Space>
      )}

      {mode === 'monthly' && (
        <Space size={4}>
          <span style={labelStyle}>在每月第</span>
          <InputNumber
            value={dayOfMonth}
            onChange={(v) => { setDayOfMonth(v || 1); buildCron(mode, hour, minute, weekday, v || 1); }}
            min={1}
            max={31}
            size="middle"
            style={{ width: 100, borderRadius: 8 }}
          />
          <span style={labelStyle}>天执行</span>
        </Space>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 4 }}>
        <span style={{ color: '#94a3b8', fontSize: 12 }}>表达式</span>
        <code style={{
          background: '#f1f5f9',
          padding: '2px 10px',
          borderRadius: 6,
          fontSize: 13,
          fontFamily: 'monospace',
          color: '#475569',
        }}>
          {cronExpr}
        </code>
      </div>
    </div>
  );
}
