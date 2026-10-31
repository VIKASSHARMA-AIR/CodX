FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY server/requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Copy server code and website assets
COPY server/ /app/server/
COPY website/ /app/website/

WORKDIR /app/server

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
