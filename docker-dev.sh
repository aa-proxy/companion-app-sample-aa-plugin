#!/usr/bin/env bash
set -e

IMAGE="android-dev"

if [ "$1" = "shell" ]; then
  echo "🧪 DEV MODE"

  if ! docker image inspect "$IMAGE" > /dev/null 2>&1; then
    echo "📦 building dev image..."
    DOCKER_BUILDKIT=1 docker build \
      -t "$IMAGE" \
      --target dev \
      .
  else
    echo "♻️ using existing image, if you want to start from scratch, use: 'docker image rm android-dev'"
  fi

  docker run -it --rm \
    -v "$(pwd)":/app \
    -w /app \
    "$IMAGE"

else
  echo "🏭 CI MODE"

  DOCKER_BUILDKIT=1 docker build \
    --target export \
    --output type=local,dest=./output \
    .
fi
