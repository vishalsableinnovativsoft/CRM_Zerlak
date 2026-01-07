import React from 'react';
import { ChevronDown } from 'lucide-react';

const FilterAccordion = ({ title, children, defaultOpen = false, count = null }) => {
  const [isOpen, setIsOpen] = React.useState(defaultOpen);

  return (
    <div className={`filter-accordion ${isOpen ? 'open' : ''}`}>
      <button
        className="filter-accordion-header"
        onClick={() => setIsOpen(!isOpen)}
        aria-expanded={isOpen}
      >
        <span className="filter-accordion-title">
          {title}
          {count !== null && count > 0 && (
            <span className="ml-2 badge">{count}</span>
          )}
        </span>
        <ChevronDown className="filter-accordion-icon" />
      </button>
      <div className="filter-accordion-content">
        <div className="filter-accordion-body">
          {children}
        </div>
      </div>
    </div>
  );
};

export default FilterAccordion;
