package com.scooter1556.sms.server.media;

import java.io.File;
import org.mp4parser.*;
import org.mp4parser.boxes.iso14496.part12.*;
import org.mp4parser.muxer.Edit;
import org.mp4parser.muxer.Sample;
import org.mp4parser.muxer.Track;
import org.mp4parser.tools.IsoTypeWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import org.mp4parser.muxer.FileRandomAccessSourceImpl;
import org.mp4parser.muxer.Mp4TrackImpl;

import static org.mp4parser.tools.CastUtils.l2i;

/**
 * Creates a fragmented MP4 file.
 */
public class FragmentedMp4Builder {
    
    private static final String CLASS_NAME = "FMP4Builder";

    public FragmentedMp4Builder() {}

    public Container build(String path, int trackId, int sequence, boolean init) {
        File segment = new File(path);
        
        if(!segment.exists() || !segment.isFile()) {
            return null;
        }
        
        Track track = null;
        
        try {
           track = new Mp4TrackImpl(trackId + 1, new IsoFile(segment), new FileRandomAccessSourceImpl(new RandomAccessFile(segment, "r")), "segment");
        } catch (IOException ex) {
            return null;
        }
        
        BasicContainer isoFile = new BasicContainer();
        
        if(init) {
            isoFile.addBox(createFtyp());
            isoFile.addBox(createMoov(track));
        } else {
            isoFile.addBox(createMoof(track, sequence));
            isoFile.addBox(createMdat(track));
        }

        return isoFile;
    }
    
    public ParsableBox createFtyp() {
        List<String> minorBrands = new LinkedList<>();
        minorBrands.add("mp42");
        minorBrands.add("iso6");
        minorBrands.add("isom");
        return new FileTypeBox("iso6", 1, minorBrands);
    }
    
    protected ParsableBox createMoov(Track track) {
        MovieBox movieBox = new MovieBox();

        movieBox.addBox(createMvhd(track));
        movieBox.addBox(createTrak(track));
        movieBox.addBox(createMvex(track));

        return movieBox;
    }
    
    protected ParsableBox createMoof(Track track, int sequenceNumber) {
        MovieFragmentBox moof = new MovieFragmentBox();
        createMfhd(sequenceNumber, moof);
        createTraf(track, sequenceNumber, moof);

        
        TrackRunBox trun = moof.getTrackRunBoxes().get(0);
        trun.setDataOffset(1); // dummy to make size correct
        trun.setDataOffset((int) (8 + moof.getSize())); // mdat header + moof size

        return moof;
    }

    protected Box createMdat(final Track track) {

        class Mdat implements Box {
            long size_ = -1;

            public long getSize() {
                if (size_ != -1) return size_;
                
                long size = 8;
                
                for (Sample sample : track.getSamples()) {
                    size += sample.getSize();
                }
                
                size_ = size;
                
                return size;
            }

            public String getType() {
                return "mdat";
            }

            public void getBox(WritableByteChannel writableByteChannel) throws IOException {
                ByteBuffer header = ByteBuffer.allocate(8);
                IsoTypeWriter.writeUInt32(header, l2i(getSize()));
                header.put(IsoFile.fourCCtoBytes(getType()));
                ((Buffer)header).rewind();
                writableByteChannel.write(header);

                for (Sample sample : track.getSamples()) {
                    sample.writeTo(writableByteChannel);
                }
            }
        }

        return new Mdat();
    }

    protected void createTfhd(Track track, TrackFragmentBox parent) {
        TrackFragmentHeaderBox tfhd = new TrackFragmentHeaderBox();
        SampleFlags sf = new SampleFlags();
        
        tfhd.hasDefaultSampleDuration();
        tfhd.hasDefaultSampleSize();
        tfhd.hasDefaultSampleFlags();

        tfhd.setTrackId(1);
        tfhd.setDefaultSampleDuration(track.getSampleDurations()[0]);
        tfhd.setDefaultSampleSize(track.getSamples().get(0).getSize());
        tfhd.setDefaultSampleFlags(sf);
        tfhd.setBaseDataOffset(-1);
        tfhd.setDefaultBaseIsMoof(true);
        
        parent.addBox(tfhd);
    }

    protected void createMfhd(int sequenceNumber, MovieFragmentBox parent) {
        MovieFragmentHeaderBox mfhd = new MovieFragmentHeaderBox();
        mfhd.setSequenceNumber(sequenceNumber + 1);
        parent.addBox(mfhd);
    }

    protected void createTraf(Track track, int sequenceNumber, MovieFragmentBox parent) {
        TrackFragmentBox traf = new TrackFragmentBox();
        parent.addBox(traf);
        createTfhd(track, traf);
        createTfdt(track, sequenceNumber, traf);
        
        TrackRunBox trun = new TrackRunBox();
        trun.setVersion(1);
        trun.setSampleDurationPresent(true);
        trun.setSampleSizePresent(true);        
        
        List<TrackRunBox.Entry> entries = new ArrayList<>();
        
        for(int i = 0; i < track.getSamples().size(); i++) {
            TrackRunBox.Entry entry = new TrackRunBox.Entry();
            entry.setSampleSize(track.getSamples().get(i).getSize());
            entry.setSampleDuration(track.getSampleDurations()[i]);
            entries.add(entry);
        }
                
        trun.setEntries(entries);
                
        traf.addBox(trun);
    }

    protected void createTfdt(Track track, int sequenceNumber, TrackFragmentBox parent) {
        TrackFragmentBaseMediaDecodeTimeBox tfdt = new TrackFragmentBaseMediaDecodeTimeBox();
        tfdt.setVersion(1);
        
        // Get information from edits
        if(track.getEdits().size() > 0) {
            Edit edit = track.getEdits().get(0);
            double timescale = edit.getTimeScale();
            double timecode = edit.getSegmentDuration();
            
            double mdt = 0;
            
            if(sequenceNumber > 0) {
                mdt = Math.round(timecode);
            }
                        
            if(mdt > 0) {
                mdt = timescale * timecode;
            }
        
            tfdt.setBaseMediaDecodeTime(Double.valueOf(mdt).longValue());
        }
        
        parent.addBox(tfdt);
    }

    protected ParsableBox createMvhd(Track track) {
        MovieHeaderBox mvhd = new MovieHeaderBox();
        mvhd.setVersion(1);
        mvhd.setCreationTime(new Date());
        mvhd.setModificationTime(new Date());
        mvhd.setDuration(0); // No duration in moov for fragments
        long timeScale = track.getTrackMetaData().getTimescale();
        mvhd.setTimescale(timeScale);
        return mvhd;
    }

    protected ParsableBox createTrex(Track track) {
        TrackExtendsBox trex = new TrackExtendsBox();
        trex.setTrackId(1);
        trex.setDefaultSampleDescriptionIndex(1);
        trex.setDefaultSampleDuration(0);
        trex.setDefaultSampleSize(0);
        
        SampleFlags sf = new SampleFlags();
        if ("soun".equals(track.getHandler()) || "subt".equals(track.getHandler())) {
            sf.setSampleDependsOn(2);
            sf.setSampleIsDependedOn(2);
        }
        
        trex.setDefaultSampleFlags(sf);
        
        return trex;
    }

    protected ParsableBox createMvex(Track track) {
        MovieExtendsBox mvex = new MovieExtendsBox();
        mvex.addBox(createTrex(track));
        return mvex;
    }

    protected ParsableBox createTkhd(Track track) {
        TrackHeaderBox tkhd = new TrackHeaderBox();
        tkhd.setVersion(1);
        tkhd.setFlags(3);
        tkhd.setAlternateGroup(track.getTrackMetaData().getGroup());
        tkhd.setCreationTime(track.getTrackMetaData().getCreationTime());
        tkhd.setDuration(0); // No duration in moov for fragmented movies
        tkhd.setHeight(track.getTrackMetaData().getHeight());
        tkhd.setWidth(track.getTrackMetaData().getWidth());
        tkhd.setLayer(track.getTrackMetaData().getLayer());
        tkhd.setModificationTime(new Date());
        tkhd.setTrackId(1);
        tkhd.setVolume(track.getTrackMetaData().getVolume());
        
        return tkhd;
    }

    protected ParsableBox createMdhd(Track track) {
        MediaHeaderBox mdhd = new MediaHeaderBox();
        mdhd.setCreationTime(track.getTrackMetaData().getCreationTime());
        mdhd.setModificationTime(new Date());
        mdhd.setDuration(0); // No duration in moov for fragmented movies
        mdhd.setTimescale(track.getTrackMetaData().getTimescale());
        mdhd.setLanguage(track.getTrackMetaData().getLanguage());
        return mdhd;
    }

    protected ParsableBox createStbl(Track track) {
        SampleTableBox stbl = new SampleTableBox();

        createStsd(track, stbl);
        stbl.addBox(new TimeToSampleBox());
        stbl.addBox(new SampleToChunkBox());
        stbl.addBox(new SampleSizeBox());
        stbl.addBox(new StaticChunkOffsetBox());
        return stbl;
    }

    protected void createStsd(Track track, SampleTableBox stbl) {
        SampleDescriptionBox stsd = new SampleDescriptionBox();
        stsd.setBoxes(track.getSampleEntries());
        stbl.addBox(stsd);
    }

    protected ParsableBox createMinf(Track track) {
        MediaInformationBox minf = new MediaInformationBox();
        
        if (track.getHandler().equals("vide")) {
            minf.addBox(new VideoMediaHeaderBox());
        } else if (track.getHandler().equals("soun")) {
            minf.addBox(new SoundMediaHeaderBox());
        } else if (track.getHandler().equals("text")) {
            minf.addBox(new NullMediaHeaderBox());
        } else if (track.getHandler().equals("subt")) {
            minf.addBox(new SubtitleMediaHeaderBox());
        } else if (track.getHandler().equals("hint")) {
            minf.addBox(new HintMediaHeaderBox());
        } else if (track.getHandler().equals("sbtl")) {
            minf.addBox(new NullMediaHeaderBox());
        }
        
        minf.addBox(createDinf());
        minf.addBox(createStbl(track));
        
        return minf;
    }

    protected ParsableBox createMdiaHdlr(Track track) {
        HandlerBox hdlr = new HandlerBox();
        hdlr.setHandlerType(track.getHandler());
        return hdlr;
    }

    protected ParsableBox createMdia(Track track) {
        MediaBox mdia = new MediaBox();
        mdia.addBox(createMdhd(track));
        mdia.addBox(createMdiaHdlr(track));
        mdia.addBox(createMinf(track));
        return mdia;
    }

    protected ParsableBox createTrak(Track track) {
        TrackBox trackBox = new TrackBox();
        trackBox.addBox(createTkhd(track));
        
        ParsableBox edts = createEdts(track);
        
        if (edts != null) {
            trackBox.addBox(edts);
        }
        
        trackBox.addBox(createMdia(track));
        
        return trackBox;
    }

    protected ParsableBox createEdts(Track track) {
        if (track.getEdits() != null && track.getEdits().size() > 0) {
            EditListBox elst = new EditListBox();
            elst.setVersion(1);
            
            List<EditListBox.Entry> entries = new ArrayList<>();

            for (Edit edit : track.getEdits()) {
                double duration = Math.round(edit.getSegmentDuration());

                if(duration == 0) {
                    duration = edit.getSegmentDuration() * track.getSampleDurations()[0];
                } else {
                    duration = 0;
                }

                entries.add(new EditListBox.Entry(
                        elst,
                        Math.round(duration),
                        edit.getMediaTime() * track.getTrackMetaData().getTimescale() / edit.getTimeScale(),
                        edit.getMediaRate()));
            }

            elst.setEntries(entries);
            EditBox edts = new EditBox();
            edts.addBox(elst);
            return edts;
        } else {
            return null;
        }
    }

    protected DataInformationBox createDinf() {
        DataInformationBox dinf = new DataInformationBox();
        DataReferenceBox dref = new DataReferenceBox();
        dinf.addBox(dref);
        DataEntryUrlBox url = new DataEntryUrlBox();
        url.setFlags(1);
        dref.addBox(url);
        return dinf;
    }
}