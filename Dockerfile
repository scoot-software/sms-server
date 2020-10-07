FROM archlinux as build
                
ARG BUILD_DEPS="\
    clang \
    diffutils \
    ffnvcodec-headers \
    gcc \
    git \
    jdk-openjdk \
    lame \
    libfdk-aac \
    libogg \
    libtool \
    libva \
    libvorbis \
    make \
    maven \
    nasm \
    ocl-icd \
    opencl-headers \
    pkgconf \
    x264 \
    x265 \
    yasm \
    zimg \
    zlib \
"
                
ARG FFMPEG_CONFIG="\
    --disable-debug \
    --enable-pic \
    --enable-gpl \
    --enable-nonfree \
    --disable-doc \
    --disable-htmlpages \
    --disable-manpages \
    --disable-podpages \
    --disable-txtpages \
    --disable-indevs \
    --disable-outdevs \
    --enable-vaapi \
    --disable-alsa \
    --disable-appkit \
    --disable-avfoundation \
    --disable-bzlib \
    --disable-coreimage \
    --disable-iconv \
    --enable-libfdk-aac \
    --enable-libmp3lame \
    --enable-libvorbis \
    --enable-libx264 \
    --enable-libx265 \
    --disable-lzma \
    --enable-opencl \
    --disable-sndio \
    --disable-schannel \
    --disable-sdl2 \
    --disable-securetransport \
    --disable-xlib \
    --enable-libzimg \
    --disable-zlib \
    --enable-cuda-llvm \
    --enable-nvenc \
"             

RUN pacman -Syu ${BUILD_DEPS} --noconfirm               

ADD ./pom.xml /sms-server/pom.xml
ADD . /sms-server

WORKDIR /sms-server

RUN JAVA_HOME=/usr/lib64/jvm/default mvn install

WORKDIR /

RUN git clone https://github.com/FFmpeg/FFmpeg.git -b n4.3.1

WORKDIR /FFmpeg

RUN ./configure ${FFMPEG_CONFIG} && make -j$(nproc) install

FROM archlinux

ARG DEPS="\
    intel-compute-runtime \
    jre-openjdk-headless \
    lame \
    libfdk-aac \
    libogg \
    libva \
    libvorbis \
    nvidia-utils \
    ocl-icd \
    opencl-nvidia \
    x264 \
    x265 \
    zimg \
    zlib \
"

RUN pacman -Syu ${DEPS} --noconfirm
RUN pacman -Scc --noconfirm

COPY --from=build /sms-server/target/sms-server*.war /opt/scootmediastreamer/sms-server/sms-server.war

COPY --from=build /usr/local/bin/* /usr/local/bin/
COPY --from=build /usr/local/lib/*.a /usr/local/lib/

WORKDIR /opt/scootmediastreamer/sms-server/

EXPOSE 1556

ENTRYPOINT ["./sms-server.war"]
