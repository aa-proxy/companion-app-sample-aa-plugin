# syntax=docker/dockerfile:1.7-labs

FROM eclipse-temurin:17-jdk-jammy AS builder

ENV ANDROID_SDK_ROOT=/sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

RUN apt-get update && apt-get install -y \
    wget unzip git curl \
    && rm -rf /var/lib/apt/lists/*

# Android SDK
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    cd $ANDROID_SDK_ROOT/cmdline-tools && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O tools.zip && \
    unzip tools.zip && \
    mv cmdline-tools latest && \
    rm tools.zip

RUN yes | sdkmanager --licenses

RUN sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0"

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew

RUN ./gradlew assembleDebug

# ===== EXPORT STAGE =====
FROM scratch AS export

COPY --from=builder /app/app/build/outputs/apk/debug/app-debug.apk /app-debug.apk
