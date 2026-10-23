package com.jerry.electrical_checklist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ReferenceSourceTest {
    private Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }

    @Test public void originalBundledPdfRenders() throws Exception {
        ChecklistCatalog.ReferenceDocument document = ChecklistCatalog.load(context()).aircraft.get(0).references.get(0);
        try (ReferenceSource source = ReferenceSource.openAsset(context(), document)) {
            assertEquals(1, source.pageCount());
            Bitmap page = source.render(0);
            assertTrue(page.getWidth() > 1000);
            assertTrue(page.getHeight() > page.getWidth());
            assertTrue((long) page.getWidth() * page.getHeight() <= 8_000_000);
            page.recycle();
        }
    }

    @Test public void multipagePdfRendersCorrectPagesAndRejectsInvalidIndex() throws Exception {
        File fixture = File.createTempFile("reader-pages-", ".pdf", context().getCacheDir());
        try {
            // Runtime test fixture uses Android's PDF writer; it is never bundled in the app.
            PdfDocument document = new PdfDocument();
            try (FileOutputStream stream = new FileOutputStream(fixture)) {
                for (int i = 0; i < 2; i++) {
                    PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(200, 300, i + 1).create());
                    page.getCanvas().drawColor(i == 0 ? Color.RED : Color.BLUE);
                    document.finishPage(page);
                }
                document.writeTo(stream);
            } finally { document.close(); }
            try (ReferenceSource source = new ReferenceSource(fixture, false)) {
                assertEquals(2, source.pageCount());
                for (int i = 0; i < 2; i++) {
                    Bitmap page = source.render(i);
                    int pixel = page.getPixel(page.getWidth() / 2, page.getHeight() / 2);
                    assertEquals(i == 0 ? Color.RED : Color.BLUE, pixel);
                    page.recycle();
                }
                assertThrows(IndexOutOfBoundsException.class, () -> source.render(2));
            }
        } finally { assertTrue(fixture.delete()); }
    }

    @Test public void pngJpegAndWebpImagesAreDecodedAtBoundedResolution() throws Exception {
        Bitmap fixtureImage = Bitmap.createBitmap(5000, 200, Bitmap.Config.ARGB_8888);
        fixtureImage.eraseColor(Color.GREEN);
        Bitmap.CompressFormat[] formats = {Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.JPEG, Bitmap.CompressFormat.WEBP};
        String[] suffixes = {".png", ".jpg", ".webp"};
        try {
            for (int i = 0; i < formats.length; i++) {
                File fixture = File.createTempFile("reader-image-", suffixes[i], context().getCacheDir());
                try {
                    try (FileOutputStream stream = new FileOutputStream(fixture)) {
                        assertTrue(fixtureImage.compress(formats[i], 100, stream));
                    }
                    try (ReferenceSource source = new ReferenceSource(fixture, true)) {
                        assertEquals(1, source.pageCount());
                        Bitmap page = source.render(0);
                        assertEquals(3200, page.getWidth());
                        assertEquals(128, page.getHeight());
                        assertTrue(Color.green(page.getPixel(20, 20)) > 240);
                        page.recycle();
                    }
                    assertFalse(fixture.exists());
                } finally { if (fixture.exists()) assertTrue(fixture.delete()); }
            }
        } finally { fixtureImage.recycle(); }
    }

    @Test public void unreadableImageReportsFailureAndReleasesOwnedFile() throws Exception {
        File fixture = File.createTempFile("reader-invalid-", ".png", context().getCacheDir());
        try (FileOutputStream stream = new FileOutputStream(fixture)) { stream.write(new byte[]{1, 2, 3}); }
        try (ReferenceSource source = new ReferenceSource(fixture, true)) {
            assertThrows(IOException.class, () -> source.render(0));
        }
        assertFalse(fixture.exists());
    }
}
