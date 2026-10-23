package com.jerry.electrical_checklist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/** A single PDF or image. Open/render/close on the reader's serial worker, never on the UI thread. */
public final class ReferenceSource implements Closeable {
    private static final int MAX_EDGE = 3200;
    private static final int MAX_PIXELS = 8_000_000;
    private final File file;
    private final boolean deleteOnClose;
    private final PdfRenderer pdf;
    private boolean closed;

    public static ReferenceSource openAsset(Context context, ChecklistCatalog.ReferenceDocument document) throws IOException {
        String suffix = document.path.substring(document.path.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        File cache = File.createTempFile("checklist-reference-", suffix, context.getCacheDir());
        try {
            try (InputStream in = context.getAssets().open(document.path);
                 FileOutputStream out = new FileOutputStream(cache)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            return new ReferenceSource(cache, true);
        } catch (IOException | RuntimeException error) {
            if (!cache.delete()) cache.deleteOnExit();
            throw error;
        }
    }

    ReferenceSource(File file, boolean deleteOnClose) throws IOException {
        this.file = file;
        this.deleteOnClose = deleteOnClose;
        if (file.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            try { pdf = new PdfRenderer(descriptor); }
            catch (IOException | RuntimeException error) { descriptor.close(); throw error; }
        } else pdf = null;
    }

    public int pageCount() {
        ensureOpen();
        return pdf == null ? 1 : pdf.getPageCount();
    }

    public Bitmap render(int pageIndex) throws IOException {
        ensureOpen();
        if (pageIndex < 0 || pageIndex >= pageCount()) throw new IndexOutOfBoundsException("Invalid reference page");
        if (pdf == null) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(file), (decoder, info, source) -> {
                int[] size = boundedSize(info.getSize().getWidth(), info.getSize().getHeight());
                decoder.setTargetSize(size[0], size[1]);
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        }
        try (PdfRenderer.Page page = pdf.openPage(pageIndex)) {
            int[] size = boundedSize(page.getWidth(), page.getHeight());
            Bitmap bitmap = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bitmap;
        }
    }

    private int[] boundedSize(int width, int height) {
        // PDFs benefit from oversampling for zoom. Images never need to be enlarged during decode.
        double scale = Math.min((double) MAX_EDGE / Math.max(width, height),
                Math.sqrt((double) MAX_PIXELS / ((double) width * height)));
        if (pdf == null) scale = Math.min(1, scale);
        return new int[]{Math.max(1, (int) (width * scale)), Math.max(1, (int) (height * scale))};
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Reference document is closed");
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        try { if (pdf != null) pdf.close(); }
        finally { if (deleteOnClose && !file.delete()) file.deleteOnExit(); }
    }
}
