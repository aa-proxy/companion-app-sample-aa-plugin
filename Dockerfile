# syntax=docker/dockerfile:1.7-labs

FROM eclipse-temurin:17-jdk-jammy AS base

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

# ===== DEV =====
FROM base AS dev

CMD ["sh", "-c", "\
echo '============================'; \
echo '🚀 ANDROID DEV CONTAINER'; \
echo 'SDK ROOT: '$ANDROID_SDK_ROOT; \
echo 'WORKDIR: /app'; \
echo 'BUILDING DEBUG APK:'; \
echo './gradlew assembleDebug'; \
echo 'FORMATTING:'; \
echo './gradlew ktfmtFormat'; \
echo '============================\n'; \
exec bash"]
#CMD ["bash"]

# ===== CI =====
FROM base AS ci

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew assembleDebug

# ===== EXPORT =====
FROM scratch AS export

COPY --from=ci /app/app/build/outputs/apk/debug/app-debug.apk /app-debug.apk
