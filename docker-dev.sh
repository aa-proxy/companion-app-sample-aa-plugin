DOCKER_BUILDKIT=1 docker build \
  -f Dockerfile \
  --target export \
  --output type=local,dest=./output \
  .
