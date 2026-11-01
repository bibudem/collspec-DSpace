package org.dspace.app.mediafilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import org.dspace.content.Item;

public class ImageToPdfFilter extends MediaFilter implements FormatFilter {

    @Override
    public String getFilteredName(String oldFilename) {
        return oldFilename.replaceAll("\\.[^.]+$", "") + ".pdf";
    }

    @Override
    public String getBundleName() {
        return "SEEALSO";
    }

    @Override
    public String getDescription() {
        return "Generated PDF from source image file";
    }

    @Override
    public String getFormatString() {
        // Register the generated bitstream as an Adobe PDF format in DSpace
        return "Adobe PDF";
    }

    @Override
    public InputStream getDestinationStream(Item item, InputStream sourceStream, boolean verbose)
            throws Exception {

        BufferedImage image = ImageIO.read(sourceStream);

        if (image == null) {
            throw new IOException(
                "Unsupported, corrupt, or unreadable image file format.");
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDImageXObject pdfImage =
                LosslessFactory.createFromImage(document, image);

            try (PDPageContentStream contentStream =
                    new PDPageContentStream(document, page)) {

                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();

                float scale = Math.min(
                    pageWidth / pdfImage.getWidth(),
                    pageHeight / pdfImage.getHeight());

                float drawWidth = pdfImage.getWidth() * scale;
                float drawHeight = pdfImage.getHeight() * scale;

                float x = (pageWidth - drawWidth) / 2;
                float y = (pageHeight - drawHeight) / 2;

                contentStream.drawImage(pdfImage, x, y, drawWidth, drawHeight);
            }

            ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream();
            document.save(pdfOutput);

            return new ByteArrayInputStream(pdfOutput.toByteArray());
        }
    }
}