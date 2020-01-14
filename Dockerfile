FROM ubuntu:18.04
MAINTAINER Stefano Pacifici <stefano@cliqz.com>
ENV DEBIAN_FRONTEND noninteractive

#Install the required packages. 1st Set is for Browser Project
RUN dpkg --add-architecture i386 && \
    apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y \
        curl \
        git \
        gnupg2 \
        language-pack-en \
        lib32z1 \
        libc6:i386 \
        libncurses5:i386 \
        libstdc++6:i386 \
        openjdk-8-jdk \
        python-dev \
        python-pip \
        python-virtualenv \
        unzip \
        wget \
        xz-utils && \
    apt-get clean -y && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Set the locale
RUN locale-gen en_US en_US.UTF-8
RUN dpkg-reconfigure locales
ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

# Add jenkins to the user group
ARG UID
ARG GID
RUN getent group $GID || groupadd jenkins --gid $GID && \
    useradd --create-home --shell /bin/bash jenkins --uid $UID --gid $GID

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
ENV ANDROID_HOME /home/jenkins/android_home
ENV GRADLE_USER_HOME /home/jenkins/gradle_home
ENV NVM_DIR /home/jenkins/nvm 
ENV NODE_VERSION 9.11.2

USER jenkins

#Install Android SDK and the Required SDKs
RUN mkdir -p $ANDROID_HOME; \
    mkdir -p $GRADLE_USER_HOME; \
    cd $ANDROID_HOME; \
    wget -O sdktools.zip --quiet 'https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip'; \
    unzip sdktools.zip; \
    rm -r sdktools.zip; \
    while (true); do echo y; done | tools/bin/sdkmanager --licenses && \
    tools/bin/sdkmanager \
        "build-tools;29.0.2" \
        "platforms;android-28" \
        "platform-tools" \
        "tools" \
        "extras;google;m2repository" \
        "extras;android;m2repository" \
        "extras;google;google_play_services";

#Install AWS CLI
RUN pip install awscli --upgrade --user
