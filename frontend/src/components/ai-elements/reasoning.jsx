"use client";;

import {
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger
} from "@/components/ui/collapsible";
import { cn } from "@/lib/utils";
import { Brain, ChevronDown } from "lucide-react";
import { createContext, useContext, useEffect, useMemo, useRef, useState } from "react";
import { Streamdown } from "streamdown";
import { cjk } from "@streamdown/cjk";
import { code } from "@streamdown/code";
import { math } from "@streamdown/math";
import { mermaid } from "@streamdown/mermaid";

const ReasoningContext = createContext(null);

function useReasoning() {
    const context = useContext(ReasoningContext);

    if (!context) {
        throw new Error("Reasoning components must be used within Reasoning");
    }

    return context;
}

export function Reasoning({
    children,
    autoOpenOnStreaming = true,
    className,
    defaultOpen = false,
    duration,
    isStreaming = false,
    onOpenChange,
    open: controlledOpen,
    ...props
}) {
    const isControlled = typeof controlledOpen === "boolean";
    const [uncontrolledOpen, setUncontrolledOpen] = useState(defaultOpen);
    const [measuredDuration, setMeasuredDuration] = useState(duration);
    const startedAtRef = useRef(null);

    const open = isControlled ? controlledOpen : uncontrolledOpen;

    useEffect(() => {
        if (typeof duration === "number") {
            setMeasuredDuration(duration);
        }
    }, [duration]);

    useEffect(() => {
        if (isStreaming) {
            if (!startedAtRef.current) {
                startedAtRef.current = Date.now();
            }
            if (autoOpenOnStreaming && !isControlled) {
                setUncontrolledOpen(true);
            }
            return;
        }

        if (startedAtRef.current) {
            setMeasuredDuration(Math.max(1, Math.round((Date.now() - startedAtRef.current) / 1000)));
            startedAtRef.current = null;
        }
    }, [autoOpenOnStreaming, isControlled, isStreaming]);

    const contextValue = useMemo(() => ({
        duration: measuredDuration,
        isOpen: open,
        isStreaming,
        setIsOpen: (nextOpen) => {
            if (!isControlled) {
                setUncontrolledOpen(nextOpen);
            }
            onOpenChange?.(nextOpen);
        }
    }), [isControlled, isStreaming, measuredDuration, onOpenChange, open]);

    return (
        <ReasoningContext.Provider value={contextValue}>
            <Collapsible
                className={cn("reasoning-root", className)}
                onOpenChange={contextValue.setIsOpen}
                open={open}
                {...props}
            >
                {children}
            </Collapsible>
        </ReasoningContext.Provider>
    );
}

export function ReasoningTrigger({
    className,
    getThinkingMessage,
    ...props
}) {
    const { duration, isStreaming } = useReasoning();
    const defaultMessage = isStreaming
        ? "正在分析"
        : duration
            ? `已思考 ${duration}s`
            : "查看思考过程";
    const triggerLabel = getThinkingMessage ? getThinkingMessage(isStreaming, duration) : defaultMessage;

    return (
        <CollapsibleTrigger className={cn("reasoning-trigger", className)} {...props}>
            <span className="reasoning-trigger-main">
                <Brain size={14} />
                <span>{triggerLabel}</span>
            </span>
            <ChevronDown className="reasoning-trigger-chevron" size={14} />
        </CollapsibleTrigger>
    );
}

export function ReasoningContent({
    children,
    className,
    ...props
}) {
    return (
        <CollapsibleContent className={cn("reasoning-content", className)} {...props}>
            <Streamdown
                className="reasoning-markdown"
                components={[cjk, code, math, mermaid]}
            >
                {children}
            </Streamdown>
        </CollapsibleContent>
    );
}

export { useReasoning };
