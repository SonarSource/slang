ARG JDK_VERSION=17
FROM eclipse-temurin:${JDK_VERSION}-jdk-jammy

ENV DEBIAN_FRONTEND=noninteractive

SHELL ["/bin/bash", "-o", "pipefail", "-c"]
RUN set -o errexit -o nounset \
  && groupadd --system --gid 1000 sonarsource \
  && useradd --system --gid sonarsource --uid 1000 --shell /bin/bash --create-home sonarsource

RUN echo 'Acquire::AllowReleaseInfoChange::Suite "true";' > /etc/apt/apt.conf.d/allow_release_info_change.conf

# hadolint ignore=DL3008
RUN apt-get update \
  && apt-get -y install --no-install-recommends \
    lsb-release \
    gnupg \
    git \
    unzip \
    wget \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

USER sonarsource
WORKDIR /home/sonarsource
