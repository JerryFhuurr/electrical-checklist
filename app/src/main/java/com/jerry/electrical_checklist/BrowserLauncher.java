package com.jerry.electrical_checklist;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

final class BrowserLauncher {
    private BrowserLauncher() { }
    static boolean open(AppCompatActivity activity, String url) {
        Intent target = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE);
        // Resolve a neutral web address so a GitHub app link does not replace the preferred browser.
        Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE);
        ResolveInfo preferred = activity.getPackageManager().resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
        if (preferred != null && preferred.activityInfo != null) {
            for (ResolveInfo browser : activity.getPackageManager().queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)) {
                if (browser.activityInfo.packageName.equals(preferred.activityInfo.packageName)
                        && browser.activityInfo.name.equals(preferred.activityInfo.name)) {
                    target.setPackage(preferred.activityInfo.packageName); break;
                }
            }
        }
        try { activity.startActivity(target); return true; }
        catch (ActivityNotFoundException error) {
            Toast.makeText(activity, R.string.no_browser, Toast.LENGTH_LONG).show(); return false;
        }
    }
}
