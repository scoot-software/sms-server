/*
 * Author: Scott Ware <scoot.software@gmail.com>
 * Copyright (c) 2015 Scott Ware
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.scooter1556.sms.server.io;

import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.utilities.HttpUtils;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;

public class FileDownloadProcess extends SMSProcess {
    private static final String CLASS_NAME = "FileDownloadProcess";
    
    private static final int DEFAULT_BUFFER_SIZE = 20480;
    private static final long DEFAULT_EXPIRE_TIME = 604800000L;
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
    
    Path filepath;
    String contentType;
    HttpServletRequest request;
    HttpServletResponse response;
        
    public FileDownloadProcess(Path path, String contentType, HttpServletRequest request, HttpServletResponse response) {
        this.filepath = path;
        this.contentType = contentType;
        this.request = request;
        this.response = response;
    }
    
    public FileDownloadProcess contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }
    
    public FileDownloadProcess request(HttpServletRequest httpRequest) {
        request = httpRequest;
        return this;
    }
    
    public FileDownloadProcess response(HttpServletResponse httpResponse) {
        response = httpResponse;
        return this;
    }
    
    @Override
    public void start() throws IOException {
        if (response == null || request == null) {
            return;
        }

        if (!Files.exists(filepath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long length = Files.size(filepath);
        String fileName = filepath.getFileName().toString();
        FileTime lastModifiedObj = Files.getLastModifiedTime(filepath);

        if (StringUtils.isEmpty(fileName) || lastModifiedObj == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
                
        // Validate request headers for caching ---------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && HttpUtils.matches(ifNoneMatch, fileName)) {
            response.setHeader("ETag", fileName); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModifiedObj.toMillis()) {
            response.setHeader("ETag", fileName); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !HttpUtils.matches(ifMatch, fileName)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModifiedObj.toMillis()) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // Validate and process range -------------------------------------------------------------

        // Prepare some variables. The full Range represents the complete file.
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader("range");
        if (range != null) {
            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(fileName)) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (ranges.isEmpty()) {
                
                if (range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                    for (String part : range.substring(6).split(",")) {
                        long start = Range.sublong(part, 0, part.indexOf("-"));
                        long end = Range.sublong(part, part.indexOf("-") + 1, part.length());

                        if (start == -1) {
                            start = length - end;
                            end = length - 1;
                        } else if (end == -1 || end > length - 1) {
                            end = length - 1;
                        }

                        // Check if Range is syntactically valid. If not, then return 416.
                        if (start > end) {
                            response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                            return;
                        }

                        // Add range.                    
                        ranges.add(new Range(start, end, length));
                    }
                } else if(range.matches("^bytes=\\d*-$")) {
                    long start = Long.valueOf(range.substring("bytes=".length()).split("-")[0]);
                    long end = length - 1;
                    
                    // Check range
                    if (start > end) {
                        response.setHeader("Content-Range", "bytes */" + length);
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }
                    
                    // Add range.                    
                    ranges.add(new Range(start, end, length));
                } else {
                    response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return;
                }
            }
        }

        // Prepare and initialize response --------------------------------------------------------

        // Get content type by file name and set content disposition.
        String disposition = "inline";

        // If content type is unknown, then set the default value.
        // For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
        // To add new content types, add new mime-mapping entry in web.xml.
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // Initialize response.
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", fileName);
        response.setDateHeader("Last-Modified", lastModifiedObj.toMillis());
        response.setDateHeader("Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME);

        // Send requested file (part(s)) to client ------------------------------------------------

        // Prepare streams.
        try (InputStream input = new BufferedInputStream(new FileInputStream(filepath.toFile()));
             OutputStream output = response.getOutputStream()) {

            if (ranges.isEmpty() || ranges.get(0) == full) {

                // Return full file.
                LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Return complete file", null);
                response.setContentType(contentType);
                response.setHeader("Content-Length", String.valueOf(full.length));
                response.setHeader("Content-Range", "bytes " + full.start + "-" + full.end + "/" + full.total);
                
                bytesTransferred += Range.copy(input, output, length, full.start, full.length);

            } else if (ranges.size() == 1) {

                // Return single part of file.
                Range r = ranges.get(0);
                LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, String.format("Return partial content: from %d-%d", r.start, r.end), null);
                response.setContentType(contentType);
                response.setHeader("Content-Length", String.valueOf(r.length));
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Copy single part range.
                bytesTransferred += Range.copy(input, output, length, r.start, r.length);

            } else {

                // Return multiple parts of file.
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Cast back to ServletOutputStream to get the easy println methods.
                ServletOutputStream sos = (ServletOutputStream) output;

                // Copy multi part range.
                for (Range r : ranges) {
                    LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, String.format("Return multi-part content: from %d-%d", r.start, r.end), null);
                    
                    // Add multipart boundary and header fields for every range.
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY);
                    sos.println("Content-Type: " + contentType);
                    sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

                    // Copy single part range of multi part range.
                    bytesTransferred += Range.copy(input, output, length, r.start, r.length);
                }

                // End with multipart boundary.
                sos.println();
                sos.println("--" + MULTIPART_BOUNDARY + "--");
            }
        }
    }

    private static class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         * @param start Start of the byte range.
         * @param end End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }

        public static long sublong(String value, int beginIndex, int endIndex) {
            String substring = value.substring(beginIndex, endIndex);
            return (substring.length() > 0) ? Long.parseLong(substring) : -1;
        }

        private static long copy(InputStream input, OutputStream output, long inputSize, long start, long length) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int read;
            long bytes = 0;

            try {
                if (inputSize == length) {
                    // Write requested range.
                    while ((read = input.read(buffer)) > 0) {
                        output.write(buffer, 0, read);
                        output.flush();
                        bytes += read;
                    }
                } else {
                    input.skip(start);
                    long toRead = length;

                    while ((read = input.read(buffer)) > 0) {
                        if ((toRead -= read) > 0) {
                            output.write(buffer, 0, read);
                            output.flush();
                            bytes += read;
                        } else {
                            output.write(buffer, 0, (int) toRead + read);
                            output.flush();
                            bytes += (read + toRead);
                            break;
                        }
                    }                
                }
            } catch (IOException e) {
                // If copying is interrupted for any reason return the number of bytes sent up to that point
                return bytes;
            }
            
            return bytes;
        }     
    }
}
