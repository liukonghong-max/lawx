import React from "react";
import { BookOpen, ChevronRight, Compass, FolderOpen, MessageSquarePlus, Search, UserCircle2 } from "lucide-react";

export default function Sidebar({ items, activeView, onChange }) {
    const shortcuts = [
        { id: "consultation", label: "新对话", icon: MessageSquarePlus, hint: "⌘ K" },
        { id: "search", label: "专业检索", icon: Search },
        { id: "library", label: "法规库", icon: BookOpen },
        { id: "discover", label: "更多能力", icon: Compass, trailing: true }
    ];

    const historyItems = [
        "主对话",
        "租赁纠纷证据清单",
        "劳动仲裁申请思路",
        "合同违约责任怎么主张",
        "股权代持风险梳理",
        "交通事故赔偿项目",
        "公司拖欠工资维权"
    ];

    return (
        <aside className="sidebar">
            <div className="sidebar-profile">
                <div className="sidebar-profile-main">
                    <div className="sidebar-avatar">L</div>
                    <strong>law4x</strong>
                </div>
                <span className="sidebar-profile-subtitle">法律工作台</span>
            </div>

            <nav className="nav-list sidebar-shortcuts" aria-label="主导航">
                {shortcuts.map((item) => {
                    const Icon = item.icon;
                    return (
                        <button
                            key={item.id}
                            type="button"
                            className={`nav-item ${activeView === item.id ? "active" : ""} ${item.id === "consultation" ? "nav-item-emphasis" : ""}`}
                            onClick={() => items.some((entry) => entry.id === item.id) && onChange(item.id)}
                        >
                            <span className="nav-item-main">
                                <span className="nav-icon" aria-hidden="true">
                                    <Icon size={18} />
                                </span>
                                <span>{item.label}</span>
                            </span>
                            {item.hint ? <span className="nav-shortcut-hint">{item.hint}</span> : null}
                            {item.trailing ? <ChevronRight size={16} className="nav-trailing-icon" /> : null}
                        </button>
                    );
                })}
            </nav>

            <div className="nav-group-label">历史对话</div>
            <div className="sidebar-history-list">
                {historyItems.map((item, index) => (
                    <button key={item} type="button" className={`history-item ${index === 0 ? "active" : ""}`}>
                        <span className="history-item-main">
                            <FolderOpen size={14} />
                            <span>{item}</span>
                        </span>
                        {index === 0 ? <ChevronRight size={14} /> : null}
                    </button>
                ))}
            </div>

            <div className="sidebar-footer">
                <button type="button" className="sidebar-account">
                    <UserCircle2 size={18} />
                    <span>当前用户</span>
                    <ChevronRight size={14} />
                </button>
                <p>本系统内容仅供参考，不构成正式法律意见。</p>
            </div>
        </aside>
    );
}
