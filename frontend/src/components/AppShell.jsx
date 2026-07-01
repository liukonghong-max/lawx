import React from "react";
import { useState } from "react";
import Sidebar from "./Sidebar";
import ConsultationPage from "../features/consultation/ConsultationPage";
import ProfessionalSearchPage from "../features/search/ProfessionalSearchPage";
import LawLibraryPage from "../features/library/LawLibraryPage";

const views = [
    { id: "consultation", label: "法律咨询", status: "active" },
    { id: "search", label: "专业检索", status: "active" },
    { id: "library", label: "法规库", status: "active" }
];

export default function AppShell() {
    const [activeView, setActiveView] = useState("consultation");

    return (
        <div className="app-shell">
            <Sidebar items={views} activeView={activeView} onChange={setActiveView} />
            <main className="workspace">
                {activeView === "consultation" ? (
                    <ConsultationPage />
                ) : activeView === "search" ? (
                    <ProfessionalSearchPage />
                ) : activeView === "library" ? (
                    <LawLibraryPage />
                ) : (
                    <section className="workspace-placeholder-page">
                        <div className="workspace-heading">
                            <p className="eyebrow">正式前端</p>
                            <h1>law4x 工作台</h1>
                            <p className="summary">法规库页会在下一阶段迁入 React 正式前端。</p>
                        </div>
                        <div className="workspace-placeholder">
                            <strong>{views.find((item) => item.id === activeView)?.label}</strong>
                            <p>后续会接入法规目录树、条文原文和元数据侧栏。</p>
                        </div>
                    </section>
                )}
            </main>
        </div>
    );
}
