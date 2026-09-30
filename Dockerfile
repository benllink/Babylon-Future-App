FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV DATABASE_PATH=/data/attendance.db

WORKDIR /app

COPY backend.zip /tmp/backend.zip
RUN python -m zipfile -e /tmp/backend.zip /app \
    && pip install --no-cache-dir -r requirements.txt \
    && mkdir -p /data

EXPOSE 8080

CMD ["sh","-c","gunicorn --bind 0.0.0.0:${PORT:-8080} --workers 2 --threads 4 --timeout 120 app:app"]
