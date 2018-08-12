package com.scooter1556.sms.server.service.parser;

import com.scooter1556.sms.server.SMS;
import com.scooter1556.sms.server.domain.GraphicsCard;
import com.scooter1556.sms.server.domain.HardwareAccelerator;
import com.scooter1556.sms.server.domain.Transcoder;
import com.scooter1556.sms.server.domain.Version;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.utilities.ParserUtils;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranscoderParser {
    
    private static final String CLASS_NAME = "TranscodeParser";
    
    // Patterns
    private static final Pattern VERSION = Pattern.compile(".*?version\\s+(\\d+\\.\\d+\\.?\\d*)", Pattern.CASE_INSENSITIVE);
    
    // Codecs
    private static final String AVC = "VFS..D h264                 H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10";
    private static final String MPEG2 = "V.S.BD mpeg2video           MPEG-2 video";
    private static final String HEVC = "VFS..D hevc                 HEVC (High Efficiency Video Coding)";
    private static final String VC1 = "V....D vc1                  SMPTE VC-1";
    
    private static final String AAC = "A....D aac                  AAC (Advanced Audio Coding)";
    private static final String AC3 = "A....D ac3                  ATSC A/52A (AC-3)";
    private static final String EAC3 = "A....D eac3                 ATSC A/52B (AC-3, E-AC-3)";
    private static final String DTS = "A....D dca                  DCA (DTS Coherent Acoustics) (codec dts)";
    private static final String PCM = "A....D pcm_s16le            PCM signed 16-bit little-endian";
    private static final String TRUEHD = "A....D truehd               TrueHD";
    private static final String MP3 = "A....D mp3                  MP3 (MPEG audio layer 3)";
    private static final String DSD = "A..... dsd_lsbf             DSD (Direct Stream Digital), least significant bit first";
    private static final String FLAC = "AF...D flac                 FLAC (Free Lossless Audio Codec)";
    private static final String ALAC = "AF...D alac                 ALAC (Apple Lossless Audio Codec)";
    private static final String VORBIS = "A....D vorbis               Vorbis";
    
    private static final String SUBRIP = "S..... subrip               SubRip subtitle";
    private static final String WEBVTT = "S..... webvtt               WebVTT subtitle";
    private static final String PGS =  "S..... pgssub               HDMV Presentation Graphic Stream subtitles (codec hdmv_pgs_subtitle)";
    private static final String DVB = "S..... dvbsub               DVB subtitles (codec dvb_subtitle)";
    private static final String DVD = "S..... dvdsub               DVD subtitles (codec dvd_subtitle)";

    
    public static Transcoder parse(Transcoder transcoder) {
        try {
            // Parse version
            transcoder.setVersion(parseVersion(transcoder));
            
            // Parse codecs
            transcoder.setCodecs(parseCodecs(transcoder));
            
            // Parse hardware acceleration methods
            transcoder.setHardwareAccelerators(parseHardwareAccelerators(transcoder));
            
        } catch (IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to parse transcoder!", ex);
        }
        
        return transcoder;
    }
    
    private static Version parseVersion(Transcoder transcoder) throws IOException {
        String[] command = {transcoder.getPath().toString()};
        String[] result = ParserUtils.getProcessOutput(command, true);

        for(String line : result) {
            Matcher matcher;

            // Version
            matcher = VERSION.matcher(line);

            if(matcher.find()) {
                return Version.parse(String.valueOf(matcher.group(1)));
            }
        }
        
        return null;
    }
    
    private static Integer[] parseCodecs(Transcoder transcoder) throws IOException {
        String[] command = {transcoder.getPath().toString(), "-v", "quiet", "-decoders"};
        String[] result = ParserUtils.getProcessOutput(command, false);
        List<Integer> codecs = new ArrayList<>();
        
        for(String line : result) {            
            switch(line) {
                case AVC:
                    codecs.add(SMS.Codec.AVC_BASELINE);
                    codecs.add(SMS.Codec.AVC_MAIN);
                    codecs.add(SMS.Codec.AVC_HIGH);
                    codecs.add(SMS.Codec.AVC_HIGH10);
                    break;
                    
                case MPEG2:
                    codecs.add(SMS.Codec.MPEG2);
                    break;
                    
                case HEVC:
                    codecs.add(SMS.Codec.HEVC_MAIN);
                    codecs.add(SMS.Codec.HEVC_MAIN10);
                    codecs.add(SMS.Codec.HEVC_HDR10);
                    break;
                    
                case VC1:
                    codecs.add(SMS.Codec.VC1);
                    break;
                    
                case AAC:
                    codecs.add(SMS.Codec.AAC);
                    break;
                    
                case AC3:
                    codecs.add(SMS.Codec.AC3);
                    break;
                    
                case EAC3:
                    codecs.add(SMS.Codec.EAC3);
                    break;
                    
                case DTS:
                    codecs.add(SMS.Codec.DTS);
                    codecs.add(SMS.Codec.DTSHD);
                    break;
                    
                case PCM:
                    codecs.add(SMS.Codec.PCM);
                    break;
                    
                case TRUEHD:
                    codecs.add(SMS.Codec.TRUEHD);
                    break;
                    
                case MP3:
                    codecs.add(SMS.Codec.MP3);
                    break;
                    
                case DSD:
                    codecs.add(SMS.Codec.DSD);
                    break;
                    
                case FLAC:
                    codecs.add(SMS.Codec.FLAC);
                    break;
                    
                case ALAC:
                    codecs.add(SMS.Codec.ALAC);
                    break;
                    
                case VORBIS:
                    codecs.add(SMS.Codec.VORBIS);
                    break;
                    
                case SUBRIP:
                    codecs.add(SMS.Codec.SUBRIP);
                    break;
                    
                case WEBVTT:
                    codecs.add(SMS.Codec.WEBVTT);
                    break;
                    
                case PGS:
                    codecs.add(SMS.Codec.PGS);
                    break;
                    
                case DVB:
                    codecs.add(SMS.Codec.DVB);
                    break;
                    
                case DVD:
                    codecs.add(SMS.Codec.DVD);
                    break;
            }
        }
        
        return codecs.toArray(new Integer[0]);
    }
    
    private static HardwareAccelerator[] parseHardwareAccelerators(Transcoder transcoder) throws IOException {
        String[] command = {transcoder.getPath().toString(), "-v", "quiet", "-hwaccels"};
        String[] result = ParserUtils.getProcessOutput(command, false);
        List<HardwareAccelerator> hwaccels = new ArrayList<>();
                
        // Get available graphics cards
        GraphicsCard[] graphicsCards = HardwareParser.getGraphicsCards();
        
        if(graphicsCards == null) {
            return null;
        }
        
        // Log available GPUs
        for(GraphicsCard graphicsCard : graphicsCards) {
            LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, graphicsCard.toString(), null);
        }
        
        for(String line : result) {
            switch(line) {
                case "vaapi":
                    // Determine if we have a GPU which supports VAAPI
                    for(GraphicsCard graphicsCard : graphicsCards) {
                        if(graphicsCard.getVendor().toLowerCase().contains("intel") && graphicsCard.getDriver().equals("i915")) {
                            HardwareAccelerator hwaccel = new HardwareAccelerator(line);
                            
                            // Set DRM render device
                            File test = new File("/dev/dri/by-path/pci-" + graphicsCard.getId() + "-render");
                            
                            if(test.exists()) {
                                hwaccel.setDevice(test.toPath());
                            } else {
                                Path[] renderDevices = TranscodeUtils.getRenderDevices();
                                
                                if(renderDevices == null) {
                                    continue;
                                }
                                
                                hwaccel.setDevice(renderDevices[0]);
                            }
                            
                            hwaccel.setStreamingSupported(false);
                            hwaccels.add(hwaccel);
                        }
                    }

                    break;

                case "cuvid":
                    // Determine if we have a GPU which supports CUVID
                    for(GraphicsCard graphicsCard : graphicsCards) {
                        if(graphicsCard.getVendor().toLowerCase().contains("nvidia")) {
                            HardwareAccelerator hwaccel = new HardwareAccelerator(line);
                            hwaccels.add(0, hwaccel);
                        }
                    }

                    break;
            }
        }
        
        return hwaccels.toArray(new HardwareAccelerator[hwaccels.size()]);
    }
}
