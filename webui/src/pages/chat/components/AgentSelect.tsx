import { useState, useRef, useEffect } from 'react';
import type { AgentDetailResponse } from '@/services/agent/typings';
import { CaretDownOutlined, CaretUpOutlined } from '@ant-design/icons';
import './AgentSelect.less';

interface AgentSelectProps {
  agents: AgentDetailResponse[];
  value: string;
  onChange: (code: string) => void;
}

export default function AgentSelect({ agents, value, onChange }: AgentSelectProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const selectedAgent = agents.find(a => a.code === value);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  if (agents.length === 0) return null;

  return (
    <div className="agent-select-container" ref={containerRef}>
      <div className={`agent-select-trigger ${open ? 'agent-select-trigger-open' : ''}`} onClick={() => setOpen(!open)}>
        <span className="agent-select-trigger-name">
          {selectedAgent?.name || 'Select Agent'}
        </span>
        {open ? (
          <CaretUpOutlined className="agent-select-trigger-icon" />
        ) : (
          <CaretDownOutlined className="agent-select-trigger-icon" />
        )}
      </div>

      {open && (
        <div className="agent-select-dropdown">
          {agents.map(a => (
            <div
              key={a.code}
              className={`agent-select-option ${a.code === value ? 'agent-select-option-active' : ''}`}
              onClick={() => {
                onChange(a.code);
                setOpen(false);
              }}
            >
              <span className="agent-select-option-name">{a.name}</span>
              {a.responsibility && (
                <span className="agent-select-option-desc">{a.responsibility}</span>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
