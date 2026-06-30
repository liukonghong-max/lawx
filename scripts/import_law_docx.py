from __future__ import annotations

import argparse
from dataclasses import asdict
import hashlib
import json
from pathlib import Path
import subprocess
import sys

if __package__ is None or __package__ == "":
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from scripts.law_docx_parser import ParsedLawDocument, parse_law_docx


def build_import_sql(document: ParsedLawDocument, effective_date: str | None = None) -> str:
    articles_json = json.dumps(
        [
            {
                "article_no": article.article_no,
                "article_order": article.article_order,
                "content": article.content,
                "book_title": article.book_title,
                "chapter_title": article.chapter_title,
                "section_title": article.section_title,
                "full_path": article.full_path,
                "content_hash": article.content_hash,
            }
            for article in document.articles
        ],
        ensure_ascii=False,
    )
    document_checksum = _document_checksum(document)

    return f"""BEGIN;

DELETE FROM law_documents
WHERE title = {_sql_literal(document.title)}
  AND source_file_name = {_sql_literal(document.source_file_name)};

WITH import_job AS (
    INSERT INTO knowledge_import_jobs (
        file_name,
        source_type,
        law_type,
        status,
        detected_title,
        detected_publish_date,
        total_articles
    )
    VALUES (
        {_sql_literal(document.source_file_name)},
        'local_upload',
        {_sql_literal(document.law_type)},
        'published',
        {_sql_literal(document.title)},
        {_sql_date(document.publish_date)},
        {len(document.articles)}
    )
    RETURNING id
),
document_row AS (
    INSERT INTO law_documents (
        import_job_id,
        title,
        law_type,
        publish_date,
        effective_date,
        status,
        source_file_name,
        checksum
    )
    SELECT
        import_job.id,
        {_sql_literal(document.title)},
        {_sql_literal(document.law_type)},
        {_sql_date(document.publish_date)},
        {_sql_date(effective_date)},
        'effective',
        {_sql_literal(document.source_file_name)},
        {_sql_literal(document_checksum)}
    FROM import_job
    RETURNING id
)
INSERT INTO law_articles (
    document_id,
    article_no,
    article_order,
    content,
    book_title,
    chapter_title,
    section_title,
    full_path,
    effective_status,
    content_hash
)
SELECT
    document_row.id,
    article.article_no,
    article.article_order,
    article.content,
    article.book_title,
    article.chapter_title,
    article.section_title,
    article.full_path,
    'effective',
    article.content_hash
FROM document_row,
jsonb_to_recordset({_sql_jsonb(articles_json)}) AS article (
    article_no TEXT,
    article_order INTEGER,
    content TEXT,
    book_title TEXT,
    chapter_title TEXT,
    section_title TEXT,
    full_path TEXT,
    content_hash TEXT
);

COMMIT;
"""


def import_document(
    docx_path: Path,
    *,
    effective_date: str | None,
    container: str,
    database: str,
    user: str,
) -> None:
    document = parse_law_docx(docx_path)
    sql = build_import_sql(document, effective_date=effective_date)
    command = [
        "docker",
        "exec",
        "-i",
        container,
        "psql",
        "-U",
        user,
        "-d",
        database,
        "-v",
        "ON_ERROR_STOP=1",
    ]
    completed = subprocess.run(command, input=sql, text=True, check=False)
    if completed.returncode != 0:
        raise SystemExit(completed.returncode)
    print(f"Imported {document.title}: {len(document.articles)} articles")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Import a Chinese law docx into law4x Postgres.")
    parser.add_argument("docx_path", type=Path)
    parser.add_argument("--effective-date", default=None)
    parser.add_argument("--container", default="law4x-postgres")
    parser.add_argument("--database", default="law4x")
    parser.add_argument("--user", default="law4x")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)

    document = parse_law_docx(args.docx_path)
    if args.dry_run:
        sys.stdout.write(build_import_sql(document, effective_date=args.effective_date))
        return 0

    import_document(
        args.docx_path,
        effective_date=args.effective_date,
        container=args.container,
        database=args.database,
        user=args.user,
    )
    return 0


def _document_checksum(document: ParsedLawDocument) -> str:
    payload = {
        "title": document.title,
        "publish_date": document.publish_date,
        "articles": [asdict(article) for article in document.articles],
    }
    encoded = json.dumps(payload, ensure_ascii=False, sort_keys=True).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def _sql_literal(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("'", "''") + "'"


def _sql_date(value: str | None) -> str:
    if value is None:
        return "NULL"
    return f"{_sql_literal(value)}::date"


def _sql_jsonb(value: str) -> str:
    return "$law4x_json$" + value + "$law4x_json$::jsonb"


if __name__ == "__main__":
    raise SystemExit(main())
