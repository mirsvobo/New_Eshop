package org.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebpImageValidatorTest {

    private final WebpImageValidator validator = new WebpImageValidator();

    @Test
    void validateProductLayer_WithValidTransparent800x800Webp_Passes() {
        MockMultipartFile file = webpFile("vrstva.webp", "image/webp", 800, 800, true);

        assertDoesNotThrow(() -> validator.validateProductLayer(file));
    }

    @Test
    void validateProductLayer_WithWrongExtension_RejectsUpload() {
        MockMultipartFile file = webpFile("vrstva.png", "image/webp", 800, 800, true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateProductLayer(file)
        );

        assertEquals("Vrstva musí být ve formátu WebP s příponou .webp.", exception.getMessage());
    }

    @Test
    void validateProductLayer_WithWrongMimeType_RejectsUpload() {
        MockMultipartFile file = webpFile("vrstva.webp", "application/octet-stream", 800, 800, true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateProductLayer(file)
        );

        assertEquals("Vrstva musí mít MIME typ image/webp.", exception.getMessage());
    }

    @Test
    void validateProductLayer_WithWrongDimensions_RejectsUpload() {
        MockMultipartFile file = webpFile("vrstva.webp", "image/webp", 799, 800, true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateProductLayer(file)
        );

        assertEquals("Vrstva musí mít přesný rozměr 800 × 800 px.", exception.getMessage());
    }

    @Test
    void validateProductLayer_WithoutAlphaChannel_RejectsUpload() {
        MockMultipartFile file = webpFile("vrstva.webp", "image/webp", 800, 800, false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateProductLayer(file)
        );

        assertEquals("WebP vrstva musí obsahovat alfa kanál s průhledností.", exception.getMessage());
    }

    @Test
    void validateProductLayer_WithPathTraversalName_RejectsUpload() {
        MockMultipartFile file = webpFile("../vrstva.webp", "image/webp", 800, 800, true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateProductLayer(file)
        );

        assertEquals("Soubor vrstvy má neplatný název.", exception.getMessage());
    }

    private MockMultipartFile webpFile(
            String filename,
            String contentType,
            int width,
            int height,
            boolean alpha
    ) {
        byte[] content = new byte[58];
        writeAscii(content, 0, "RIFF");
        writeUnsignedIntLittleEndian(content, 4, content.length - 8);
        writeAscii(content, 8, "WEBP");

        writeAscii(content, 12, "VP8X");
        writeUnsignedIntLittleEndian(content, 16, 10);
        content[20] = alpha ? (byte) 0x10 : 0;
        writeUnsigned24LittleEndian(content, 24, width - 1);
        writeUnsigned24LittleEndian(content, 27, height - 1);

        writeAscii(content, 30, "ALPH");
        writeUnsignedIntLittleEndian(content, 34, 1);
        content[38] = 0;
        content[39] = 0;

        writeAscii(content, 40, "VP8 ");
        writeUnsignedIntLittleEndian(content, 44, 10);

        return new MockMultipartFile("layerImageFile", filename, contentType, content);
    }

    private void writeAscii(byte[] target, int offset, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, target, offset, bytes.length);
    }

    private void writeUnsigned24LittleEndian(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
        target[offset + 2] = (byte) (value >>> 16);
    }

    private void writeUnsignedIntLittleEndian(byte[] target, int offset, long value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
        target[offset + 2] = (byte) (value >>> 16);
        target[offset + 3] = (byte) (value >>> 24);
    }
}
