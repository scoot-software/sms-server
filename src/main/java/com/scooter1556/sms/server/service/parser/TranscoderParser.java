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
    private static final String AVC = "VFS..D h264 ";
    private static final String MPEG2 = "V.S.BD mpeg2video ";
    private static final String HEVC = "VFS..D hevc ";
    private static final String VC1 = "V....D vc1 ";
    
    private static final String AAC = "A....D aac ";
    private static final String AC3 = "A....D ac3 ";
    private static final String EAC3 = "A....D eac3 ";
    private static final String DTS = "A....D dca ";
    private static final String PCM = "A....D pcm_s16le ";
    private static final String TRUEHD = "A....D truehd ";
    private static final String MP3 = "A....D mp3 ";
    private static final String DSD = "A..... dsd_lsbf ";
    private static final String FLAC = "AF...D flac ";
    private static final String ALAC = "AF...D alac ";
    private static final String VORBIS = "A....D vorbis ";
    
    private static final String SUBRIP = "S..... subrip ";
    private static final String WEBVTT = "S..... webvtt ";
    private static final String PGS =  "S..... pgssub ";
    private static final String DVB = "S..... dvbsub ";
    private static final String DVD = "S..... dvdsub ";

    
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
            if(line.contains(AVC)) {
                codecs.add(SMS.Codec.AVC_BASELINE);
                codecs.add(SMS.Codec.AVC_MAIN);
                codecs.add(SMS.Codec.AVC_HIGH);
                codecs.add(SMS.Codec.AVC_HIGH10);
                continue;
            }
            
            if(line.contains(MPEG2)) {
                codecs.add(SMS.Codec.MPEG2);
                continue;
            }
            
            if(line.contains(HEVC)) {
                codecs.add(SMS.Codec.HEVC_MAIN);
                codecs.add(SMS.Codec.HEVC_MAIN10);
                codecs.add(SMS.Codec.HEVC_HDR10);
                continue;
            }
            
            if(line.contains(VC1)) {
                codecs.add(SMS.Codec.VC1);
                continue;
            }
            
            if(line.contains(AAC)) {
                codecs.add(SMS.Codec.AAC);
                continue;
            }
            
            if(line.contains(AC3)) {
                codecs.add(SMS.Codec.AC3);
                continue;
            }
            
            if(line.contains(EAC3)) {
                codecs.add(SMS.Codec.EAC3);
                continue;
            }
            
            if(line.contains(DTS)) {
                codecs.add(SMS.Codec.DTS);
                codecs.add(SMS.Codec.DTSHD);
                continue;
            }
            
            if(line.contains(PCM)) {
                codecs.add(SMS.Codec.PCM);
                continue;
            }
            
            if(line.contains(TRUEHD)) {
                codecs.add(SMS.Codec.TRUEHD);
                continue;
            }
            
            if(line.contains(MP3)) {
                codecs.add(SMS.Codec.MP3);
                continue;
            }
            
            if(line.contains(DSD)) {
                codecs.add(SMS.Codec.DSD);
                continue;
            }
            
            if(line.contains(FLAC)) {
                codecs.add(SMS.Codec.FLAC);
                continue;
            }
            
            if(line.contains(ALAC)) {
                codecs.add(SMS.Codec.ALAC);
                continue;
            }
            
            if(line.contains(VORBIS)) {
                codecs.add(SMS.Codec.VORBIS);
                continue;
            }
            
            if(line.contains(SUBRIP)) {
                codecs.add(SMS.Codec.SUBRIP);
                continue;
            }
            
            if(line.contains(WEBVTT)) {
                codecs.add(SMS.Codec.WEBVTT);
                continue;
            }
            
            if(line.contains(PGS)) {
                codecs.add(SMS.Codec.PGS);
                continue;
            }
            
            if(line.contains(DVB)) {
                codecs.add(SMS.Codec.DVB);
                continue;
            }
            
            if(line.contains(DVD)) {
                codecs.add(SMS.Codec.DVD);
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
