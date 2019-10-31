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
    
    // Encoders
    private static final String X264 = "V..... libx264";
    private static final String X265 = "V..... libx265";
    private static final String LAME = "A..... libmp3lame";
    private static final String E_AAC = "A..... aac";
    private static final String E_AC3 = "A..... ac3";
    private static final String E_EAC3 = "A..... eac3";
    
    // Filters
    private static final String ZSCALE = "zscale";

    
    public static Transcoder parse(Transcoder transcoder) {
        try {
            // Parse version
            transcoder.setVersion(parseVersion(transcoder));
            
            // Parse decoders
            transcoder.setDecoders(parseDecoders(transcoder));
            
            // Parse encoders
            transcoder.setEncoders(parseEncoders(transcoder));
            
            // Parse filters
            parseFilters(transcoder);
            
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
    
    private static Integer[] parseDecoders(Transcoder transcoder) throws IOException {
        String[] command = {transcoder.getPath().toString(), "-v", "quiet", "-decoders"};
        String[] result = ParserUtils.getProcessOutput(command, false);
        List<Integer> decoders = new ArrayList<>();
        
        for(String line : result) {
            if(line.contains(AVC)) {
                decoders.add(SMS.Codec.AVC_BASELINE);
                decoders.add(SMS.Codec.AVC_MAIN);
                decoders.add(SMS.Codec.AVC_HIGH);
                decoders.add(SMS.Codec.AVC_HIGH10);
                continue;
            }
            
            if(line.contains(MPEG2)) {
                decoders.add(SMS.Codec.MPEG2);
                continue;
            }
            
            if(line.contains(HEVC)) {
                decoders.add(SMS.Codec.HEVC_MAIN);
                decoders.add(SMS.Codec.HEVC_MAIN10);
                decoders.add(SMS.Codec.HEVC_HDR10);
                continue;
            }
            
            if(line.contains(VC1)) {
                decoders.add(SMS.Codec.VC1);
                continue;
            }
            
            if(line.contains(AAC)) {
                decoders.add(SMS.Codec.AAC);
                continue;
            }
            
            if(line.contains(AC3)) {
                decoders.add(SMS.Codec.AC3);
                continue;
            }
            
            if(line.contains(EAC3)) {
                decoders.add(SMS.Codec.EAC3);
                continue;
            }
            
            if(line.contains(DTS)) {
                decoders.add(SMS.Codec.DTS);
                decoders.add(SMS.Codec.DTSHD);
                continue;
            }
            
            if(line.contains(PCM)) {
                decoders.add(SMS.Codec.PCM);
                continue;
            }
            
            if(line.contains(TRUEHD)) {
                decoders.add(SMS.Codec.TRUEHD);
                continue;
            }
            
            if(line.contains(MP3)) {
                decoders.add(SMS.Codec.MP3);
                continue;
            }
            
            if(line.contains(DSD)) {
                decoders.add(SMS.Codec.DSD);
                continue;
            }
            
            if(line.contains(FLAC)) {
                decoders.add(SMS.Codec.FLAC);
                continue;
            }
            
            if(line.contains(ALAC)) {
                decoders.add(SMS.Codec.ALAC);
                continue;
            }
            
            if(line.contains(VORBIS)) {
                decoders.add(SMS.Codec.VORBIS);
                continue;
            }
            
            if(line.contains(SUBRIP)) {
                decoders.add(SMS.Codec.SUBRIP);
                continue;
            }
            
            if(line.contains(WEBVTT)) {
                decoders.add(SMS.Codec.WEBVTT);
                continue;
            }
            
            if(line.contains(PGS)) {
                decoders.add(SMS.Codec.PGS);
                continue;
            }
            
            if(line.contains(DVB)) {
                decoders.add(SMS.Codec.DVB);
                continue;
            }
            
            if(line.contains(DVD)) {
                decoders.add(SMS.Codec.DVD);
            }
        }
        
        return decoders.toArray(new Integer[0]);
    }
    
    private static Integer[] parseEncoders(Transcoder transcoder) throws IOException {
        String[] command = {transcoder.getPath().toString(), "-v", "quiet", "-encoders"};
        String[] result = ParserUtils.getProcessOutput(command, false);
        List<Integer> encoders = new ArrayList<>();
        
        for(String line : result) {
            if(line.contains(X264)) {
                encoders.add(SMS.Codec.AVC_BASELINE);
                encoders.add(SMS.Codec.AVC_MAIN);
                encoders.add(SMS.Codec.AVC_HIGH);
                encoders.add(SMS.Codec.AVC_HIGH10);
                continue;
            }
            
            if(line.contains(X265)) {
                encoders.add(SMS.Codec.HEVC_MAIN);
                encoders.add(SMS.Codec.HEVC_MAIN10);
                encoders.add(SMS.Codec.HEVC_HDR10);
                continue;
            }
            
            if(line.contains(E_AAC)) {
                encoders.add(SMS.Codec.AAC);
                continue;
            }
            
            if(line.contains(E_AC3)) {
                encoders.add(SMS.Codec.AC3);
                continue;
            }
            
            if(line.contains(E_EAC3)) {
                encoders.add(SMS.Codec.EAC3);
                continue;
            }
            
            if(line.contains(LAME)) {
                encoders.add(SMS.Codec.MP3);
                continue;
            }
            
            if(line.contains(SUBRIP)) {
                encoders.add(SMS.Codec.SUBRIP);
                continue;
            }
            
            if(line.contains(WEBVTT)) {
                encoders.add(SMS.Codec.WEBVTT);
            }
        }
        
        return encoders.toArray(new Integer[0]);
    }
    
    private static void parseFilters(Transcoder transcoder) throws IOException {
        String[] command = {transcoder.getPath().toString(), "-v", "quiet", "-filters"};
        String[] result = ParserUtils.getProcessOutput(command, false);
        
        for(String line : result) {
            if(line.contains(ZSCALE)) {
                transcoder.setZscale(true);
                break;
            }
        }
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
