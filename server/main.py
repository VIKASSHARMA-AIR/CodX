import os
import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from database import engine, Base
from routers import auth, status, schedules, recordings, sessions

# Create database tables
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="CodX Cloud Server",
    description="Offline-first, remotely synchronized recording system API for CodX Recorder & Controller",
    version="1.0.1"
)

# Enable CORS for mobile app access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register routers
app.include_router(auth.router)
app.include_router(status.router)
app.include_router(schedules.router)
app.include_router(recordings.router)
app.include_router(sessions.router)

# Mount website static directories and downloads
WEBSITE_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "website")
DOWNLOADS_DIR = os.path.join(WEBSITE_DIR, "downloads")
CSS_DIR = os.path.join(WEBSITE_DIR, "css")
JS_DIR = os.path.join(WEBSITE_DIR, "js")

if os.path.exists(DOWNLOADS_DIR):
    app.mount("/downloads", StaticFiles(directory=DOWNLOADS_DIR), name="downloads")

if os.path.exists(CSS_DIR):
    app.mount("/css", StaticFiles(directory=CSS_DIR), name="css")

if os.path.exists(JS_DIR):
    app.mount("/js", StaticFiles(directory=JS_DIR), name="js")

@app.get("/")
def root():
    index_path = os.path.join(WEBSITE_DIR, "index.html")
    if os.path.exists(index_path):
        return FileResponse(index_path)
    return {
        "system": "CodX Cloud Server",
        "status": "online",
        "version": "1.0.1",
        "docs_url": "/docs"
    }

@app.get("/health")
def health():
    return {"status": "healthy"}

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
