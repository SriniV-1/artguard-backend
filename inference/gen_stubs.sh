#!/usr/bin/env bash
# Generate Python gRPC stubs from the shared contract into ./generated.
#   ./gen_stubs.sh
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p generated
python -m grpc_tools.protoc \
  -I../proto \
  --python_out=generated \
  --grpc_python_out=generated \
  ../proto/inference.proto
# make the generated package importable
touch generated/__init__.py
# fix the absolute import grpc_tools emits (inference_pb2 -> generated.inference_pb2)
sed -i.bak 's/^import inference_pb2/from generated import inference_pb2/' generated/inference_pb2_grpc.py && rm -f generated/inference_pb2_grpc.py.bak
echo "stubs written to generated/"
