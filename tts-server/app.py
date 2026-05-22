import io
import re
import time
import logging
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, ConfigDict
import edge_tts

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("TTS-Server")

app = FastAPI(title="Edge-TTS Korean API Wrapper")

# Korean voice options
# ko-KR-SunHiNeural  = 여성 (밝고 명랑)
# ko-KR-InJoonNeural = 남성 (차분하고 신뢰감)
# ko-KR-HyunsuNeural = 남성 (좀 더 무게감 있는 목소리)
VOICE_MALE = "ko-KR-InJoonNeural"
VOICE_FEMALE = "ko-KR-SunHiNeural"
DEFAULT_VOICE = VOICE_MALE

# Default speech parameters for natural conversation feel
DEFAULT_RATE = "+45%"       # Slightly faster than default for urgency
DEFAULT_PITCH = "-25Hz"     # Slightly lower pitch for authority


def add_natural_pauses(text: str) -> str:
    """Add natural breath pauses between sentences for realistic speech.
    Inserts short SSML-style commas at sentence boundaries."""
    # Add slight pause after question marks and periods
    text = re.sub(r'([.?!])\s+', r'\1 ... ', text)
    # Add micro-pause after commas
    text = re.sub(r',\s*', ', ', text)
    return text


class TTSRequest(BaseModel):
    model_config = ConfigDict(extra='ignore')
    text: str
    voice: str = DEFAULT_VOICE
    rate: str = DEFAULT_RATE
    pitch: str = DEFAULT_PITCH


async def tts_to_bytes(text: str, voice: str, rate: str = DEFAULT_RATE, pitch: str = DEFAULT_PITCH) -> io.BytesIO:
    """Generate TTS audio and return as BytesIO buffer."""
    # Add natural pauses for conversational feel
    processed_text = add_natural_pauses(text)
    
    communicate = edge_tts.Communicate(processed_text, voice, rate=rate, pitch=pitch)
    audio_buffer = io.BytesIO()
    async for chunk in communicate.stream():
        if chunk["type"] == "audio":
            audio_buffer.write(chunk["data"])
    audio_buffer.seek(0)
    return audio_buffer


@app.post("/generate")
async def generate_audio(req: TTSRequest):
    if not req.text.strip():
        raise HTTPException(status_code=400, detail="Text field cannot be empty.")

    start_time = time.time()

    try:
        audio_buffer = await tts_to_bytes(req.text, req.voice, req.rate, req.pitch)
        elapsed = time.time() - start_time
        logger.info(f"Generated TTS audio in {elapsed:.4f}s | voice={req.voice} | text='{req.text[:50]}...'")
        return StreamingResponse(audio_buffer, media_type="audio/mpeg")

    except Exception as e:
        logger.error(f"TTS generation failed: {e}")
        raise HTTPException(status_code=500, detail=f"TTS generation failed: {str(e)}")


@app.get("/generate")
async def generate_audio_get(text: str, voice: str = DEFAULT_VOICE, rate: str = DEFAULT_RATE, pitch: str = DEFAULT_PITCH):
    """GET endpoint for simple testing."""
    if not text.strip():
        raise HTTPException(status_code=400, detail="Text parameter cannot be empty.")

    start_time = time.time()

    try:
        audio_buffer = await tts_to_bytes(text, voice, rate, pitch)
        elapsed = time.time() - start_time
        logger.info(f"Generated TTS audio in {elapsed:.4f}s | voice={voice} | text='{text[:50]}...'")
        return StreamingResponse(audio_buffer, media_type="audio/mpeg")

    except Exception as e:
        logger.error(f"TTS generation failed: {e}")
        raise HTTPException(status_code=500, detail=f"TTS generation failed: {str(e)}")


@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "engine": "edge-tts",
        "default_voice": DEFAULT_VOICE,
        "available_voices": {
            "male": VOICE_MALE,
            "female": VOICE_FEMALE,
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000)
