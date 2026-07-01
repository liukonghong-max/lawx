import React from "react";

export default function Sidebar({ items, activeView, onChange }) {
    const secondaryItems = [
        { id: "history", label: "历史记录", icon: "◷" },
        { id: "feedback", label: "反馈评测", icon: "☰" }
    ];

    return (
        <aside className="sidebar">
            <div className="brand">
                <div className="brand-mark">⚖</div>
                <div>
                    <strong>法律法规咨询助手</strong>
                    <span>智能 · 专业 · 可靠</span>
                </div>
            </div>

            <nav className="nav-list" aria-label="主导航">
                {items.map((item) => {
                    const disabled = item.status !== "active" && item.id !== "consultation";
                    return (
                        <button
                            key={item.id}
                            type="button"
                            className={`nav-item ${activeView === item.id ? "active" : ""}`}
                            onClick={() => !disabled && onChange(item.id)}
                            disabled={disabled}
                        >
                            <span className="nav-icon" aria-hidden="true">
                                {item.id === "consultation" ? "◌" : item.id === "search" ? "⌕" : "▥"}
                            </span>
                            <span>{item.label}</span>
                        </button>
                    );
                })}
            </nav>

            <div className="nav-group-label">更多功能</div>
            <div className="nav-list secondary">
                {secondaryItems.map((item) => (
                    <button key={item.id} type="button" className="nav-item secondary-item">
                        <span className="nav-icon" aria-hidden="true">
                            {item.icon}
                        </span>
                        <span>{item.label}</span>
                    </button>
                ))}
            </div>

            <div className="sidebar-footer">
                <button type="button" className="sidebar-helper">
                    使用指南
                </button>
                <p>本系统提供的内容仅供参考，不构成法律意见或建议。</p>
            </div>
        </aside>
    );
}
