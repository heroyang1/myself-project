package com.backstage.system.service.internalmember.support;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.enums.internalmember.InternalMemberDocumentTypeEnum;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class InternalMemberDocumentProcessor {

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("pdf", "docx");
    private static final List<String> FONT_CANDIDATES = Arrays.asList(
            "C:\\Windows\\Fonts\\simhei.ttf",
            "C:\\Windows\\Fonts\\simfang.ttf",
            "C:\\Windows\\Fonts\\simkai.ttf",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttf",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.otf",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    );

    private final InternalMemberDocumentTextDesensitizer desensitizer;

    public InternalMemberDocumentProcessor(InternalMemberDocumentTextDesensitizer desensitizer) {
        this.desensitizer = desensitizer;
    }

    public ProcessedDocument process(MultipartFile file, InternalMemberDocumentTypeEnum docType) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("Upload file cannot be empty");
        }
        String extension = normalizeExtension(file.getOriginalFilename());
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new ServiceException("Only pdf and docx documents are supported");
        }
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("internal-member-doc-");
            String extractedText = "pdf".equals(extension) ? extractPdfText(file) : extractDocxText(file);
            String normalizedText = normalizeText(extractedText);
            if (normalizedText.isEmpty()) {
                throw new ServiceException("Only text-based PDF or DOCX documents are supported");
            }
            String maskedText = normalizeText(desensitizer.desensitize(normalizedText));
            if (maskedText.isEmpty()) {
                throw new ServiceException("Document desensitization failed");
            }
            String outputFileName = buildOutputFileName(file.getOriginalFilename(), docType);
            Path outputPath = tempDir.resolve(outputFileName);
            renderPdf(maskedText, outputPath);
            return new ProcessedDocument(tempDir.toFile(), outputPath.toFile(), outputFileName);
        } catch (ServiceException ex) {
            cleanup(tempDir);
            throw ex;
        } catch (Exception ex) {
            cleanup(tempDir);
            throw new ServiceException("Document processing failed: " + ex.getMessage());
        }
    }

    public void cleanup(File workDir) {
        if (workDir == null) {
            return;
        }
        cleanup(workDir.toPath());
    }

    private void cleanup(Path workDir) {
        if (workDir == null || !Files.exists(workDir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(workDir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignore) {
                    // ignore cleanup failure
                }
            });
        } catch (IOException ignore) {
            // ignore cleanup failure
        }
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocxText(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private void renderPdf(String text, Path outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType0Font font = loadFont(document);
            float fontSize = 12F;
            float margin = 50F;
            float leading = 18F;
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(margin, page.getMediaBox().getHeight() - margin);

            float writableWidth = page.getMediaBox().getWidth() - margin * 2;
            float y = page.getMediaBox().getHeight() - margin;
            List<String> lines = wrapLines(text, font, fontSize, writableWidth);
            for (String line : lines) {
                if (y - leading < margin) {
                    contentStream.endText();
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.beginText();
                    contentStream.setFont(font, fontSize);
                    contentStream.newLineAtOffset(margin, page.getMediaBox().getHeight() - margin);
                    y = page.getMediaBox().getHeight() - margin;
                }
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -leading);
                y -= leading;
            }
            contentStream.endText();
            contentStream.close();
            document.save(outputPath.toFile());
        }
    }

    private PDType0Font loadFont(PDDocument document) throws IOException {
        for (String candidate : FONT_CANDIDATES) {
            File file = new File(candidate);
            if (file.exists() && file.isFile()) {
                return PDType0Font.load(document, file);
            }
        }
        throw new ServiceException("No available CJK font found for PDF generation");
    }

    private List<String> wrapLines(String text, PDType0Font font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\r?\\n")) {
            String current = "";
            if (paragraph.trim().isEmpty()) {
                lines.add(" ");
                continue;
            }
            for (int i = 0; i < paragraph.length(); i++) {
                String next = current + paragraph.charAt(i);
                float width = font.getStringWidth(next) / 1000 * fontSize;
                if (width > maxWidth && !current.isEmpty()) {
                    lines.add(current);
                    current = String.valueOf(paragraph.charAt(i));
                } else {
                    current = next;
                }
            }
            if (!current.isEmpty()) {
                lines.add(current);
            }
        }
        return lines;
    }

    private String normalizeExtension(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                .replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        StringBuilder builder = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch == '\n') {
                builder.append(ch);
                continue;
            }
            if (ch == '\t') {
                builder.append("    ");
                continue;
            }
            if (Character.isISOControl(ch)) {
                continue;
            }
            builder.append(ch);
        }
        return builder.toString().trim();
    }

    private String buildOutputFileName(String originalFileName, InternalMemberDocumentTypeEnum docType) {
        String baseName = FilenameUtils.getBaseName(originalFileName);
        String normalizedBaseName = (baseName == null || baseName.trim().isEmpty())
                ? docType.getPathSegment()
                : baseName.trim();
        return normalizedBaseName + "_masked_" + UUID.randomUUID().toString().replace("-", "") + ".pdf";
    }

    public static class ProcessedDocument {
        private final File workDir;
        private final File pdfFile;
        private final String fileName;

        public ProcessedDocument(File workDir, File pdfFile, String fileName) {
            this.workDir = workDir;
            this.pdfFile = pdfFile;
            this.fileName = fileName;
        }

        public File getWorkDir() {
            return workDir;
        }

        public File getPdfFile() {
            return pdfFile;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
