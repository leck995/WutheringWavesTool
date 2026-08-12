package cn.tealc.wutheringwavestool.thread.gacha.cloud;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class MultipartFormBody {
    private final byte[] bytes;
    private final String boundary;

    private MultipartFormBody(byte[] bytes, String boundary) {
        this.bytes = bytes;
        this.boundary = boundary;
    }

    public byte[] getBody() {
        return bytes;
    }

    public String getBoundary() {
        return boundary;
    }

    public String getContentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    public static class Builder {
        private static final String DEFAULT_MIMETYPE = "text/plain";

        private final List<Part> parts = new ArrayList<>();

        private static class Part {
            final String fieldName;
            final String filename;
            final String contentType;
            final Object content;

            Part(String fieldName, Object content, String contentType, String filename) {
                this.fieldName = fieldName;
                this.content = content;
                this.contentType = contentType != null ? contentType : DEFAULT_MIMETYPE;
                this.filename = filename;
            }
        }

        public Builder addPart(String fieldName, String fieldValue) {
            parts.add(new Part(fieldName, fieldValue, DEFAULT_MIMETYPE, null));
            return this;
        }

        public Builder addPart(String fieldName, String fieldValue, String contentType) {
            parts.add(new Part(fieldName, fieldValue, contentType, null));
            return this;
        }

        public Builder addPart(String fieldName, File file, String contentType, String filename) {
            parts.add(new Part(fieldName, file, contentType, filename));
            return this;
        }

        public Builder addPart(String fieldName, byte[] bytes, String contentType, String filename) {
            parts.add(new Part(fieldName, bytes, contentType, filename));
            return this;
        }

        public MultipartFormBody build() throws IOException {
            String boundary = new BigInteger(256, new SecureRandom()).toString();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            for (Part part : parts) {
                StringBuilder header = new StringBuilder();
                header.append("--").append(boundary).append("\r\n");
                header.append("Content-Disposition: form-data; name=\"").append(part.fieldName);
                if (part.filename != null) {
                    header.append("\"; filename=\"").append(part.filename);
                }
                header.append("\"\r\n");
                header.append("Content-Type: ").append(part.contentType).append("\r\n\r\n");
                out.write(header.toString().getBytes(StandardCharsets.UTF_8));

                if (part.content instanceof String s) {
                    out.write(s.getBytes(StandardCharsets.UTF_8));
                } else if (part.content instanceof byte[] bytes) {
                    out.write(bytes);
                } else if (part.content instanceof File file) {
                    Files.copy(file.toPath(), out);
                }
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            return new MultipartFormBody(out.toByteArray(), boundary);
        }
    }
}
