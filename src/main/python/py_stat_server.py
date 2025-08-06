from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import pymannkendall as mk

app = FastAPI()

class PriceRequest(BaseModel):
    data: Dict[str, List[float]]

class FlatResponse(BaseModel):
    ticker: str
    trend: Optional[str] = None
    h: Optional[bool] = None
    p: Optional[float] = None
    z: Optional[float] = None
    Tau: Optional[float] = None
    s: Optional[float] = None
    var_s: Optional[float] = None
    slope: Optional[float] = None
    intercept: Optional[float] = None
    error: Optional[str] = None

@app.post("/batch-analysis-mann-kandell", response_model=List[FlatResponse])
def batch_mann_kendall(request: PriceRequest):
    results = []

    for ticker, prices in request.data.items():
        obj = {"ticker": ticker}
        try:
            if not isinstance(prices, list) or len(prices) < 3:
                obj["error"] = "List must have at least 3 numbers"
            else:
                result = mk.original_test(prices)
                obj.update({
                    "trend": str(result.trend),
                    "h": bool(result.h),
                    "p": float(result.p),
                    "z": float(result.z),
                    "Tau": float(result.Tau),
                    "s": float(result.s),
                    "var_s": float(result.var_s),
                    "slope": float(result.slope),
                    "intercept": float(result.intercept)
                })
        except Exception as e:
            obj["error"] = str(e)
        results.append(obj)

    return results