# ArtGuard inference service

A gRPC server that runs **YOLOv8** object detection over JPEG frames sent by the
Java gateway. One process; the gateway fans frames to it concurrently (one
virtual thread per in-flight frame).

## Run

```bash
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
./gen_stubs.sh            # generate gRPC stubs from ../proto/inference.proto
python inference_server.py
```

First run auto-downloads `yolov8n.pt` (~6 MB). Serves on `:50051`.

## Contract

Implements `artguard.inference.v1.InferenceService` (see `../proto/inference.proto`):
- `Detect(DetectRequest) -> DetectResponse` — decode JPEG, run YOLOv8, return
  labelled bounding boxes + per-frame model time.
- `Health(HealthRequest) -> HealthResponse` — ready once the model is loaded.

## Config (env)

| Var | Default | Meaning |
|-----|---------|---------|
| `ARTGUARD_MODEL`   | `yolov8n.pt` | YOLO weights |
| `ARTGUARD_PORT`    | `50051`      | gRPC listen port |
| `ARTGUARD_WORKERS` | `8`          | gRPC threadpool size |
