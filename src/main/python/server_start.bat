@echo off
cmd /k python -m uvicorn py_stat_server:app --host 127.0.0.1 --port 8000
