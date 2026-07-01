import { useEffect, useMemo, useState } from "react";
import { getLibraryArticleDetail, listDocumentArticles, listLawDocuments } from "./api";

export function useLawLibrary() {
    const [articlePage, setArticlePage] = useState(1);
    const [articlePageSize] = useState(20);
    const [articleTotal, setArticleTotal] = useState(0);
    const [documents, setDocuments] = useState([]);
    const [documentsLoading, setDocumentsLoading] = useState(true);
    const [documentsError, setDocumentsError] = useState("");
    const [activeDocumentId, setActiveDocumentId] = useState("");
    const [articles, setArticles] = useState([]);
    const [articlesLoading, setArticlesLoading] = useState(false);
    const [articlesError, setArticlesError] = useState("");
    const [activeArticleId, setActiveArticleId] = useState("");
    const [articleDetail, setArticleDetail] = useState(null);
    const [articleDetailLoading, setArticleDetailLoading] = useState(false);
    const [articleDetailError, setArticleDetailError] = useState("");

    function selectDocument(documentId) {
        setActiveDocumentId(documentId);
        setArticlePage(1);
        setActiveArticleId("");
        setArticleDetail(null);
        setArticleDetailError("");
    }

    useEffect(() => {
        let cancelled = false;
        async function loadDocuments() {
            setDocumentsLoading(true);
            setDocumentsError("");
            try {
                const items = await listLawDocuments();
                if (cancelled) {
                    return;
                }
                setDocuments(items);
            } catch (error) {
                if (!cancelled) {
                    setDocumentsError(error.message || "加载法规列表失败。");
                }
            } finally {
                if (!cancelled) {
                    setDocumentsLoading(false);
                }
            }
        }
        loadDocuments();
        return () => {
            cancelled = true;
        };
    }, []);

    useEffect(() => {
        if (!activeDocumentId) {
            setArticles([]);
            setActiveArticleId("");
            setArticleTotal(0);
            return;
        }
        let cancelled = false;
        async function loadArticles() {
            setArticlesLoading(true);
            setArticlesError("");
            try {
                const result = await listDocumentArticles(activeDocumentId, articlePage, articlePageSize);
                if (cancelled) {
                    return;
                }
                setArticles(result.items);
                setArticleTotal(result.total);
                setActiveArticleId((current) => {
                    if (current && result.items.some((item) => item.articleId === current)) {
                        return current;
                    }
                    return result.items[0]?.articleId || "";
                });
            } catch (error) {
                if (!cancelled) {
                    setArticlesError(error.message || "加载条文目录失败。");
                    setArticles([]);
                    setActiveArticleId("");
                    setArticleTotal(0);
                }
            } finally {
                if (!cancelled) {
                    setArticlesLoading(false);
                }
            }
        }
        loadArticles();
        return () => {
            cancelled = true;
        };
    }, [activeDocumentId, articlePage, articlePageSize]);

    useEffect(() => {
        if (!activeArticleId) {
            setArticleDetail(null);
            return;
        }
        let cancelled = false;
        async function loadDetail() {
            setArticleDetailLoading(true);
            setArticleDetailError("");
            try {
                const detail = await getLibraryArticleDetail(activeArticleId);
                if (!cancelled) {
                    setArticleDetail(detail);
                }
            } catch (error) {
                if (!cancelled) {
                    setArticleDetailError(error.message || "加载法条详情失败。");
                    setArticleDetail(null);
                }
            } finally {
                if (!cancelled) {
                    setArticleDetailLoading(false);
                }
            }
        }
        loadDetail();
        return () => {
            cancelled = true;
        };
    }, [activeArticleId]);

    const activeDocument = useMemo(
        () => documents.find((item) => item.documentId === activeDocumentId) || null,
        [documents, activeDocumentId]
    );

    return {
        documents,
        documentsLoading,
        documentsError,
        activeDocumentId,
        activeDocument,
        setActiveDocumentId: selectDocument,
        articles,
        articlesLoading,
        articlesError,
        articlePage,
        articlePageSize,
        articleTotal,
        setArticlePage,
        activeArticleId,
        setActiveArticleId,
        articleDetail,
        articleDetailLoading,
        articleDetailError
    };
}
