
package com.makdad.anatomy;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class AtlasDownloader {

    private static final String DOWNLOAD_URL =
        "https://github.com/dado946/Makdad-Anatomy-New-/releases/download/atlas-data-bp3d43-v1/atlas-bp3d43.zip";

    private final Handler mainHandler =
        new Handler(Looper.getMainLooper());

    private volatile boolean cancelled = false;

    public interface Listener {
        void onProgress(long downloaded, long total);
        void onStage(String message);
        void onComplete(File atlasDirectory);
        void onError(String message);
    }

    public void start(Context context, Listener listener) {
        cancelled = false;

        Context app = context.getApplicationContext();

        new Thread(() -> {
            try {
                File base = new File(
                    app.getFilesDir(),
                    "atlas-data"
                );

                if (!base.exists() && !base.mkdirs()) {
                    throw new IOException(
                        "Cannot create atlas directory"
                    );
                }

                File partial = new File(
                    base,
                    "atlas-bp3d43.zip.part"
                );

                File zipFile = new File(
                    base,
                    "atlas-bp3d43.zip"
                );

                File staging = new File(
                    base,
                    "bp3d43.tmp"
                );

                File target = new File(
                    base,
                    "bp3d43"
                );

                postStage(listener, "Downloading anatomy data");

                download(partial, zipFile, listener);

                checkCancelled();

                postStage(listener, "Extracting anatomy data");

                deleteDirectory(staging);

                if (!staging.mkdirs()) {
                    throw new IOException(
                        "Cannot create temporary directory"
                    );
                }

                extract(zipFile, staging);

                File index = new File(
                    staging,
                    "index.json"
                );

                if (!index.isFile() || index.length() == 0) {
                    throw new IOException(
                        "Atlas index is missing"
                    );
                }

                checkCancelled();

                deleteDirectory(target);

                if (!staging.renameTo(target)) {
                    throw new IOException(
                        "Cannot install atlas files"
                    );
                }

                zipFile.delete();

                postComplete(listener, target);

            } catch (Exception e) {
                postError(
                    listener,
                    e.getMessage() == null
                        ? "Atlas download failed"
                        : e.getMessage()
                );
            }
        }).start();
    }

    public void cancel() {
        cancelled = true;
    }

    private void download(
        File partial,
        File destination,
        Listener listener
    ) throws Exception {

        long existing = partial.exists()
            ? partial.length()
            : 0;

        HttpURLConnection connection =
            (HttpURLConnection) new URL(DOWNLOAD_URL)
                .openConnection();

        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty(
            "User-Agent",
            "Makdad-Anatomy-Android"
        );

        if (existing > 0) {
            connection.setRequestProperty(
                "Range",
                "bytes=" + existing + "-"
            );
        }

        int code = connection.getResponseCode();

        boolean append = code == 206;

        if (code != 200 && code != 206) {
            connection.disconnect();
            throw new IOException(
                "Download server returned HTTP " + code
            );
        }

        if (existing > 0 && !append) {
            existing = 0;
        }

        long length = connection.getContentLength();
        long total = length > 0
            ? existing + length
            : -1;

        try (
            InputStream input = connection.getInputStream();
            FileOutputStream output =
                new FileOutputStream(partial, append)
        ) {
            byte[] buffer = new byte[64 * 1024];
            long downloaded = existing;
            int count;

            while ((count = input.read(buffer)) != -1) {
                checkCancelled();

                output.write(buffer, 0, count);
                downloaded += count;

                final long current = downloaded;
                final long expected = total;

                mainHandler.post(() ->
                    listener.onProgress(current, expected)
                );
            }

            output.flush();

        } finally {
            connection.disconnect();
        }

        if (!partial.isFile() || partial.length() == 0) {
            throw new IOException(
                "Downloaded file is empty"
            );
        }

        if (destination.exists()) {
            destination.delete();
        }

        if (!partial.renameTo(destination)) {
            throw new IOException(
                "Cannot save downloaded package"
            );
        }
    }

    private void extract(
        File zipFile,
        File staging
    ) throws Exception {

        String rootPath =
            staging.getCanonicalPath() + File.separator;

        try (
            ZipInputStream zip = new ZipInputStream(
                new FileInputStream(zipFile)
            )
        ) {
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                checkCancelled();

                String name = entry.getName();

                if (!name.startsWith("bp3d43/")) {
                    zip.closeEntry();
                    continue;
                }

                String relative =
                    name.substring("bp3d43/".length());

                if (relative.isEmpty()) {
                    zip.closeEntry();
                    continue;
                }

                File outputFile = new File(
                    staging,
                    relative
                );

                String outputPath =
                    outputFile.getCanonicalPath();

                if (!outputPath.startsWith(rootPath)) {
                    throw new IOException(
                        "Invalid archive path"
                    );
                }

                if (entry.isDirectory()) {
                    if (!outputFile.exists()
                            && !outputFile.mkdirs()) {
                        throw new IOException(
                            "Cannot create atlas folder"
                        );
                    }
                } else {
                    File parent = outputFile.getParentFile();

                    if (parent != null && !parent.exists()
                            && !parent.mkdirs()) {
                        throw new IOException(
                            "Cannot create atlas folder"
                        );
                    }

                    try (
                        FileOutputStream output =
                            new FileOutputStream(outputFile)
                    ) {
                        byte[] buffer = new byte[64 * 1024];
                        int count;

                        while ((count = zip.read(buffer)) != -1) {
                            checkCancelled();
                            output.write(buffer, 0, count);
                        }

                        output.flush();
                    }
                }

                zip.closeEntry();
            }
        }
    }

    private void checkCancelled() throws IOException {
        if (cancelled) {
            throw new IOException("Download cancelled");
        }
    }

    private void deleteDirectory(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }

        file.delete();
    }

    private void postStage(
        Listener listener,
        String message
    ) {
        mainHandler.post(() ->
            listener.onStage(message)
        );
    }

    private void postComplete(
        Listener listener,
        File directory
    ) {
        mainHandler.post(() ->
            listener.onComplete(directory)
        );
    }

    private void postError(
        Listener listener,
        String message
    ) {
        mainHandler.post(() ->
            listener.onError(message)
        );
    }
    }
