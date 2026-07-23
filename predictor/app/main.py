from fastapi import FastAPI


app = FastAPI(title="FlashScale Predictor")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}
