from __future__ import annotations

import argparse
from pathlib import Path
import subprocess
import sys


if __package__ is None or __package__ == "":
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


def build_search_sql(query: str, limit: int = 10) -> str:
    if limit < 1 or limit > 100:
        raise ValueError("limit must be between 1 and 100")

    q = _sql_literal(query.strip())
    if q == "''":
        raise ValueError("query must not be empty")

    return f"""SELECT
    d.title AS document_title,
    a.article_no,
    a.full_path,
    left(a.content, 180) AS preview,
    (
        CASE WHEN a.article_no = {q} THEN 100 ELSE 0 END
        + CASE WHEN d.title ILIKE '%' || {q} || '%' THEN 20 ELSE 0 END
        + CASE WHEN a.full_path ILIKE '%' || {q} || '%' THEN 15 ELSE 0 END
        + CASE WHEN a.content ILIKE '%' || {q} || '%' THEN 10 ELSE 0 END
        + similarity(a.content, {q}) * 10
        + similarity(a.full_path, {q}) * 5
    ) AS score
FROM law_articles a
JOIN law_documents d ON d.id = a.document_id
WHERE a.effective_status = 'effective'
  AND d.status = 'effective'
  AND (
      a.article_no = {q}
      OR d.title ILIKE '%' || {q} || '%'
      OR a.full_path ILIKE '%' || {q} || '%'
      OR a.content ILIKE '%' || {q} || '%'
      OR similarity(a.content, {q}) > 0.05
      OR similarity(a.full_path, {q}) > 0.1
  )
ORDER BY score DESC, a.article_order ASC
LIMIT {limit};
"""


def run_search(
    query: str,
    *,
    limit: int,
    container: str,
    database: str,
    user: str,
) -> int:
    sql = build_search_sql(query, limit=limit)
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
        "-c",
        sql,
    ]
    completed = subprocess.run(command, check=False)
    return completed.returncode


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Search law articles in local law4x Postgres.")
    parser.add_argument("query")
    parser.add_argument("--limit", type=int, default=10)
    parser.add_argument("--container", default="law4x-postgres")
    parser.add_argument("--database", default="law4x")
    parser.add_argument("--user", default="law4x")
    parser.add_argument("--print-sql", action="store_true")
    args = parser.parse_args(argv)

    if args.print_sql:
        sys.stdout.write(build_search_sql(args.query, limit=args.limit))
        return 0

    return run_search(
        args.query,
        limit=args.limit,
        container=args.container,
        database=args.database,
        user=args.user,
    )


def _sql_literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


if __name__ == "__main__":
    raise SystemExit(main())
