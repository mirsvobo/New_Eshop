package org.example.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

@Component
public class WebpImageValidator {

    private static final String REQUIRED_CONTENT_TYPE = "image/webp";
    private static final int REQUIRED_WIDTH = 800;
    private static final int REQUIRED_HEIGHT = 800;

    public void validateProductLayer(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vyberte neprázdný WebP soubor vrstvy.");
        }

        String originalFilename = StringUtils.cleanPath(
                Objects.requireNonNullElse(file.getOriginalFilename(), "")
        );
        if (!StringUtils.hasText(originalFilename)
                || originalFilename.contains("..")
                || originalFilename.contains("/")
                || originalFilename.contains("\\")) {
            throw new IllegalArgumentException("Soubor vrstvy má neplatný název.");
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!"webp".equalsIgnoreCase(extension)) {
            throw new IllegalArgumentException("Vrstva musí být ve formátu WebP s příponou .webp.");
        }

        String contentType = Objects.requireNonNullElse(file.getContentType(), "")
                .toLowerCase(Locale.ROOT);
        if (!REQUIRED_CONTENT_TYPE.equals(contentType)) {
            throw new IllegalArgumentException("Vrstva musí mít MIME typ image/webp.");
        }

        WebpMetadata metadata = readMetadata(file);
        if (metadata.width() != REQUIRED_WIDTH || metadata.height() != REQUIRED_HEIGHT) {
            throw new IllegalArgumentException("Vrstva musí mít přesný rozměr 800 × 800 px.");
        }
        if (!metadata.hasAlpha()) {
            throw new IllegalArgumentException("WebP vrstva musí obsahovat alfa kanál s průhledností.");
        }
    }

    private WebpMetadata readMetadata(MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("WebP soubor vrstvy se nepodařilo přečíst.", exception);
        }

        if (content.length < 25
                || !matches(content, 0, "RIFF")
                || !matches(content, 8, "WEBP")) {
            throw new IllegalArgumentException("Obsah souboru neodpovídá formátu WebP.");
        }

        long declaredRiffSize = readUnsignedIntLittleEndian(content, 4);
        if (declaredRiffSize + 8L != content.length) {
            throw new IllegalArgumentException("WebP soubor má neplatnou velikost RIFF kontejneru.");
        }

        String firstChunkType = readAscii(content, 12, 4);
        return switch (firstChunkType) {
            case "VP8X" -> readExtendedMetadata(content);
            case "VP8L" -> readLosslessMetadata(content, 20);
            case "VP8 " -> throw new IllegalArgumentException(
                    "WebP vrstva v režimu VP8 neobsahuje alfa kanál. Použijte průhledný WebP soubor."
            );
            default -> throw new IllegalArgumentException("WebP soubor obsahuje nepodporovanou obrazovou strukturu.");
        };
    }

    private WebpMetadata readExtendedMetadata(byte[] content) {
        if (content.length < 30) {
            throw new IllegalArgumentException("Rozšířená WebP hlavička je neúplná.");
        }

        long chunkSize = readUnsignedIntLittleEndian(content, 16);
        if (chunkSize != 10L) {
            throw new IllegalArgumentException("Rozšířená WebP hlavička má neplatnou velikost.");
        }

        boolean hasAlphaFlag = (Byte.toUnsignedInt(content[20]) & 0x10) != 0;
        int width = 1 + readUnsigned24LittleEndian(content, 24);
        int height = 1 + readUnsigned24LittleEndian(content, 27);
        boolean containsImageChunk = containsChunk(content, "VP8 ") || containsChunk(content, "VP8L");
        boolean containsAlphaData = containsChunk(content, "ALPH") || containsLosslessAlphaChunk(content);

        if (!containsImageChunk) {
            throw new IllegalArgumentException("WebP soubor neobsahuje obrazovou vrstvu.");
        }

        return new WebpMetadata(width, height, hasAlphaFlag && containsAlphaData);
    }

    private WebpMetadata readLosslessMetadata(byte[] content, int dataOffset) {
        if (content.length < dataOffset + 5 || Byte.toUnsignedInt(content[dataOffset]) != 0x2F) {
            throw new IllegalArgumentException("Bezztrátová WebP hlavička je neplatná.");
        }

        long dimensionsAndFlags = readUnsignedIntLittleEndian(content, dataOffset + 1);
        int width = (int) (dimensionsAndFlags & 0x3FFF) + 1;
        int height = (int) ((dimensionsAndFlags >>> 14) & 0x3FFF) + 1;
        boolean hasAlpha = ((dimensionsAndFlags >>> 28) & 1L) == 1L;
        return new WebpMetadata(width, height, hasAlpha);
    }

    private boolean containsLosslessAlphaChunk(byte[] content) {
        int offset = 12;
        while (offset + 8 <= content.length) {
            String chunkType = readAscii(content, offset, 4);
            long chunkSize = readUnsignedIntLittleEndian(content, offset + 4);
            long nextOffset = offset + 8L + chunkSize + (chunkSize & 1L);
            if (nextOffset > content.length || nextOffset > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("WebP soubor obsahuje neplatnou délku datového bloku.");
            }
            if ("VP8L".equals(chunkType)) {
                return readLosslessMetadata(content, offset + 8).hasAlpha();
            }
            offset = (int) nextOffset;
        }
        return false;
    }

    private boolean containsChunk(byte[] content, String expectedType) {
        int offset = 12;
        while (offset + 8 <= content.length) {
            String chunkType = readAscii(content, offset, 4);
            long chunkSize = readUnsignedIntLittleEndian(content, offset + 4);
            long nextOffset = offset + 8L + chunkSize + (chunkSize & 1L);
            if (nextOffset > content.length || nextOffset > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("WebP soubor obsahuje neplatnou délku datového bloku.");
            }
            if (expectedType.equals(chunkType)) {
                return true;
            }
            offset = (int) nextOffset;
        }
        return false;
    }

    private boolean matches(byte[] content, int offset, String expected) {
        if (offset < 0 || offset + expected.length() > content.length) {
            return false;
        }
        for (int index = 0; index < expected.length(); index++) {
            if (content[offset + index] != (byte) expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private String readAscii(byte[] content, int offset, int length) {
        if (offset < 0 || offset + length > content.length) {
            throw new IllegalArgumentException("WebP soubor má neúplnou hlavičku.");
        }
        return new String(content, offset, length, StandardCharsets.US_ASCII);
    }

    private int readUnsigned24LittleEndian(byte[] content, int offset) {
        if (offset < 0 || offset + 3 > content.length) {
            throw new IllegalArgumentException("WebP soubor má neúplné rozměry.");
        }
        return Byte.toUnsignedInt(content[offset])
                | (Byte.toUnsignedInt(content[offset + 1]) << 8)
                | (Byte.toUnsignedInt(content[offset + 2]) << 16);
    }

    private long readUnsignedIntLittleEndian(byte[] content, int offset) {
        if (offset < 0 || offset + 4 > content.length) {
            throw new IllegalArgumentException("WebP soubor má neúplnou číselnou hodnotu.");
        }
        return Integer.toUnsignedLong(
                Byte.toUnsignedInt(content[offset])
                        | (Byte.toUnsignedInt(content[offset + 1]) << 8)
                        | (Byte.toUnsignedInt(content[offset + 2]) << 16)
                        | (Byte.toUnsignedInt(content[offset + 3]) << 24)
        );
    }

    private record WebpMetadata(int width, int height, boolean hasAlpha) {
    }
}
