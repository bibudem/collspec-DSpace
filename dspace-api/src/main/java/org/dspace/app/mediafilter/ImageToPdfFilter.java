// Nima : nouveau ajout - 2026-10-29

package org.dspace.app.mediafilter;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.apache.pdfbox.io.IOUtils;
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
        // Tells DSpace to explicitly register this bitstream as an Adobe PDF format type
        return "Adobe PDF";
    }

    @Override
    public InputStream getDestinationStream(Item item, InputStream sourceStream, boolean verbose) 
            throws Exception {
        
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream inputStream = new PipedInputStream(outputStream);

        // Pass the live incoming stream safely to our processing thread
        Thread pdfThread = new Thread(() -> {
            try (PDDocument doc = new PDDocument();
                 InputStream imageSrc = sourceStream) {
                
                PDPage page = new PDPage();
                doc.addPage(page);

                java.awt.image.BufferedImage bimg = javax.imageio.ImageIO.read(imageSrc);
                if (bimg == null) {
                    throw new java.io.IOException("Unsupported, corrupt, or unreadable image file format.");
                }

                PDImageXObject pdImage = LosslessFactory.createFromImage(doc, bimg);

                try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
                    float scale = Math.min(page.getMediaBox().getWidth() / pdImage.getWidth(), 
                                           page.getMediaBox().getHeight() / pdImage.getHeight());
                    contents.drawImage(pdImage, 0, 0, pdImage.getWidth() * scale, pdImage.getHeight() * scale);
                }

                doc.save(outputStream);
            } catch (Exception e) {
                // Thread boundary isolation containment
            } finally {
                IOUtils.closeQuietly(outputStream);
            }
        });

        pdfThread.start();
        return inputStream;
    }
}
