package com.scooter1556.sms.server.encoder;

public interface Encoder {
    public boolean isSupported(int codec);
    public int getVideoCodec(Integer[] codecs);
    public int getAudioCodec(Integer[] codecs, int channels, int quality);
    public int getClient();
    public void setClient(int client);
}
