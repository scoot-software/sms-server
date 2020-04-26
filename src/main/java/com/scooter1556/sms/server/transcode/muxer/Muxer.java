package com.scooter1556.sms.server.transcode.muxer;

public interface Muxer {
    public int getFormat();
    public boolean isSupported(Integer[] codecs, int codec);
    public int getVideoCodec(Integer[] codecs);
    public int getAudioCodec(Integer[] codecs, int channels, int quality);
    public int getClient();
    public void setClient(int client);
}
