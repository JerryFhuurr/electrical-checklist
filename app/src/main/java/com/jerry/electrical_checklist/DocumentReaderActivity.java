package com.jerry.electrical_checklist;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.jerry.electrical_checklist.ChecklistCatalog.ReferenceDocument;

/** Offline, in-app reference reader. Document resources are confined to a serial worker. */
public final class DocumentReaderActivity extends AppCompatActivity {
    public static final String EXTRA_AIRCRAFT_ID = "aircraft_id";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private List<ReferenceDocument> documents;
    private DocumentPageView pageView;
    private int documentIndex, pageIndex, pageCount = 1;
    private float[] restoredViewport;
    private volatile int requestVersion;
    private volatile boolean destroyed;
    // Only the worker reads or writes these two fields.
    private ReferenceSource source;
    private String openedPath;

    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        EdgeToEdge.enable(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_document_reader);
        View root = findViewById(R.id.reader_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        ViewCompat.getWindowInsetsController(root).setAppearanceLightStatusBars(false);
        ViewCompat.getWindowInsetsController(root).setAppearanceLightNavigationBars(false);
        findViewById(R.id.reader_back).setOnClickListener(v -> finish());
        pageView = findViewById(R.id.document_page);
        findViewById(R.id.reader_zoom_in).setOnClickListener(v -> pageView.zoomBy(1.5f));
        findViewById(R.id.reader_zoom_out).setOnClickListener(v -> pageView.zoomBy(1 / 1.5f));
        findViewById(R.id.reader_fit).setOnClickListener(v -> pageView.fitPage());
        findViewById(R.id.reader_previous).setOnClickListener(v -> changePage(pageIndex - 1));
        findViewById(R.id.reader_next).setOnClickListener(v -> changePage(pageIndex + 1));
        findViewById(R.id.reader_retry).setOnClickListener(v -> requestPage());
        try {
            ChecklistCatalog catalog = ChecklistCatalog.load(this);
            ChecklistCatalog.Aircraft aircraft = null;
            for (ChecklistCatalog.Aircraft model : catalog.aircraft)
                if (model.id.equals(getIntent().getStringExtra(EXTRA_AIRCRAFT_ID))) aircraft = model;
            if (aircraft == null || aircraft.references.isEmpty()) throw new IOException("No reference documents");
            documents = aircraft.references;
            ((TextView) findViewById(R.id.reader_aircraft)).setText(aircraft.name);
        } catch (Exception error) {
            showError(); findViewById(R.id.reader_retry).setVisibility(View.GONE); return;
        }
        if (savedState != null) {
            documentIndex = Math.max(0, Math.min(documents.size() - 1, savedState.getInt("document")));
            pageIndex = Math.max(0, savedState.getInt("page"));
            restoredViewport = savedState.getFloatArray("viewport");
        }
        Spinner selector = findViewById(R.id.reader_document_selector);
        selector.setSaveEnabled(false);
        if (documents.size() > 1) {
            List<String> names = new ArrayList<>();
            for (ReferenceDocument document : documents) names.add(document.title);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            selector.setAdapter(adapter); selector.setSelection(documentIndex, false);
            selector.setVisibility(View.VISIBLE);
            selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != documentIndex) {
                        documentIndex = position; pageIndex = 0; pageCount = 1; restoredViewport = null;
                        pageView.setPage(null, null); requestPage();
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) { }
            });
        }
        requestPage();
        ViewCompat.requestApplyInsets(root);
    }

    private void changePage(int target) {
        if (target < 0 || target >= pageCount) return;
        pageIndex = target; restoredViewport = null;
        pageView.setPage(null, null); requestPage();
    }

    private void requestPage() {
        final int version = ++requestVersion;
        final int requestedPage = pageIndex;
        final ReferenceDocument document = documents.get(documentIndex);
        ((TextView) findViewById(R.id.reader_document_title)).setText(document.title);
        findViewById(R.id.reader_error).setVisibility(View.GONE);
        findViewById(R.id.reader_loading).setVisibility(View.VISIBLE);
        setControls(false);
        worker.execute(() -> {
            if (destroyed || version != requestVersion) return;
            try {
                if (source == null || !document.path.equals(openedPath)) {
                    closeSource();
                    source = ReferenceSource.openAsset(getApplicationContext(), document);
                    openedPath = document.path;
                }
                int count = source.pageCount();
                if (count == 0) throw new IOException("Empty document");
                int index = Math.min(requestedPage, count - 1);
                Bitmap page = source.render(index);
                runOnUiThread(() -> {
                    if (destroyed || version != requestVersion) { page.recycle(); return; }
                    pageCount = count; pageIndex = index;
                    pageView.setPage(page, restoredViewport); restoredViewport = null;
                    pageView.setContentDescription(getString(R.string.reader_page_description, document.title, index + 1, count));
                    ((TextView) findViewById(R.id.reader_page_count)).setText(getString(R.string.reader_page_count, index + 1, count));
                    findViewById(R.id.reader_loading).setVisibility(View.GONE);
                    setControls(true);
                });
            } catch (IOException | RuntimeException error) {
                closeSource();
                runOnUiThread(() -> { if (!destroyed && version == requestVersion) showError(); });
            }
        });
    }

    private void setControls(boolean ready) {
        findViewById(R.id.reader_previous).setEnabled(ready && pageIndex > 0);
        findViewById(R.id.reader_next).setEnabled(ready && pageIndex + 1 < pageCount);
        for (int id : new int[]{R.id.reader_zoom_in, R.id.reader_zoom_out, R.id.reader_fit})
            findViewById(id).setEnabled(ready);
    }
    private void showError() {
        pageView.setPage(null, null);
        findViewById(R.id.reader_loading).setVisibility(View.GONE);
        findViewById(R.id.reader_error).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.reader_page_count)).setText(R.string.reader_unavailable);
        setControls(false);
    }
    private void closeSource() {
        if (source != null) { source.close(); source = null; openedPath = null; }
    }
    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("document", documentIndex); state.putInt("page", pageIndex);
        state.putFloatArray("viewport", pageView.hasPage() ? pageView.snapshot() : restoredViewport);
    }
    @Override protected void onDestroy() {
        destroyed = true; requestVersion++;
        worker.execute(this::closeSource); worker.shutdown();
        super.onDestroy();
    }
}
