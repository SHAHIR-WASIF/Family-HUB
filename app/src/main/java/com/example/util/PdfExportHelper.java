package com.example.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Java utility for rendering high-resolution PDF profiles, shareable PNG summary cards,
 * and printable multi-generation Family Tree PDFs using Android's native PdfDocument and Canvas APIs.
 */
public final class PdfExportHelper {

    private PdfExportHelper() {}

    public static class MemberPdfData {
        public final String fullName;
        public final String nickname;
        public final String dateOfBirthDisplay;
        public final String gender;
        public final String bloodGroup;
        public final String relationshipToOwner;
        public final String biography;
        public final String photoPath;
        public final List<String> relationshipLines;
        public final List<String> infoLines;
        public final List<String> memoryLines;
        public final List<String> customFieldLines;

        public MemberPdfData(
                String fullName,
                String nickname,
                String dateOfBirthDisplay,
                String gender,
                String bloodGroup,
                String relationshipToOwner,
                String biography,
                String photoPath,
                List<String> relationshipLines,
                List<String> infoLines,
                List<String> memoryLines,
                List<String> customFieldLines
        ) {
            this.fullName = fullName;
            this.nickname = nickname;
            this.dateOfBirthDisplay = dateOfBirthDisplay;
            this.gender = gender;
            this.bloodGroup = bloodGroup;
            this.relationshipToOwner = relationshipToOwner;
            this.biography = biography;
            this.photoPath = photoPath;
            this.relationshipLines = relationshipLines != null ? relationshipLines : new ArrayList<>();
            this.infoLines = infoLines != null ? infoLines : new ArrayList<>();
            this.memoryLines = memoryLines != null ? memoryLines : new ArrayList<>();
            this.customFieldLines = customFieldLines != null ? customFieldLines : new ArrayList<>();
        }
    }

    public static class TreePdfNode {
        public final long id;
        public final String name;
        public final String subtitle;
        public final float x;
        public final float y;
        public final int generation;

        public TreePdfNode(long id, String name, String subtitle, float x, float y, int generation) {
            this.id = id;
            this.name = name;
            this.subtitle = subtitle;
            this.x = x;
            this.y = y;
            this.generation = generation;
        }
    }

    public static class TreePdfEdge {
        public final float fromX;
        public final float fromY;
        public final float toX;
        public final float toY;
        public final boolean isSpouse;

        public TreePdfEdge(float fromX, float fromY, float toX, float toY, boolean isSpouse) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.isSpouse = isSpouse;
        }
    }

    private static File ensureExportsDir(Context context) {
        File dir = new File(context.getCacheDir(), "exports");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public static File generateMemberProfilePdf(Context context, MemberPdfData data) throws IOException {
        PdfDocument document = new PdfDocument();
        final int pageWidth = 595;  // A4 pt
        final int pageHeight = 842; // A4 pt
        final int margin = 40;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Background parchment tint
        canvas.drawColor(Color.parseColor("#FAF6F0"));

        // Header Banner
        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#2B5B4E"));
        canvas.drawRect(0, 0, pageWidth, 140, headerPaint);

        // Portrait photo or monogram circle
        float avatarCx = margin + 45f;
        float avatarCy = 70f;
        float avatarR = 40f;
        Bitmap photoBitmap = loadSampledBitmap(data.photoPath, 240);
        if (photoBitmap != null) {
            Bitmap circleBmp = createCircularBitmap(photoBitmap, 160);
            canvas.drawBitmap(circleBmp, null, new RectF(avatarCx - avatarR, avatarCy - avatarR, avatarCx + avatarR, avatarCy + avatarR), null);
            photoBitmap.recycle();
            circleBmp.recycle();
        } else {
            Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            circlePaint.setColor(Color.parseColor("#B85D3B"));
            canvas.drawCircle(avatarCx, avatarCy, avatarR, circlePaint);
            Paint initialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            initialPaint.setColor(Color.WHITE);
            initialPaint.setTextSize(32f);
            initialPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            initialPaint.setTextAlign(Paint.Align.CENTER);
            String initial = data.fullName.isEmpty() ? "?" : data.fullName.substring(0, 1).toUpperCase();
            canvas.drawText(initial, avatarCx, avatarCy + 11f, initialPaint);
        }

        // Name & subtitle in header
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(24f);
        titlePaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        canvas.drawText(data.fullName, margin + 104f, 64f, titlePaint);

        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(Color.parseColor("#CDE9DF"));
        subPaint.setTextSize(12f);
        StringBuilder meta = new StringBuilder();
        if (data.nickname != null && !data.nickname.isEmpty()) meta.append("\"").append(data.nickname).append("\"  ");
        if (data.relationshipToOwner != null && !data.relationshipToOwner.isEmpty()) meta.append("• ").append(data.relationshipToOwner).append("  ");
        if (data.dateOfBirthDisplay != null && !data.dateOfBirthDisplay.isEmpty()) meta.append("• ").append(data.dateOfBirthDisplay);
        canvas.drawText(meta.toString().trim(), margin + 104f, 88f, subPaint);

        StringBuilder badges = new StringBuilder();
        if (data.gender != null && !data.gender.isEmpty()) badges.append(data.gender).append("   ");
        if (data.bloodGroup != null && !data.bloodGroup.isEmpty()) badges.append("Blood Group: ").append(data.bloodGroup);
        if (badges.length() > 0) {
            canvas.drawText(badges.toString().trim(), margin + 104f, 108f, subPaint);
        }

        float y = 165f;
        int contentWidth = pageWidth - (margin * 2);

        if (data.biography != null && !data.biography.isEmpty()) {
            y = drawSectionHeading(canvas, "Biography", margin, y);
            y = drawWrappedText(canvas, data.biography, margin, y, contentWidth, 11.5f, "#222422") + 14f;
        }

        if (!data.relationshipLines.isEmpty()) {
            y = drawSectionHeading(canvas, "Family Relationships", margin, y);
            for (String line : data.relationshipLines) {
                y = drawWrappedText(canvas, "• " + line, margin + 6, y, contentWidth - 6, 11f, "#2E312E") + 3f;
                if (y > pageHeight - 60) break;
            }
            y += 10f;
        }

        if (!data.infoLines.isEmpty() && y < pageHeight - 80) {
            y = drawSectionHeading(canvas, "Additional Details", margin, y);
            for (String line : data.infoLines) {
                y = drawWrappedText(canvas, line, margin + 6, y, contentWidth - 6, 11f, "#2E312E") + 3f;
                if (y > pageHeight - 60) break;
            }
            y += 10f;
        }

        if (!data.customFieldLines.isEmpty() && y < pageHeight - 80) {
            y = drawSectionHeading(canvas, "Custom Fields", margin, y);
            for (String line : data.customFieldLines) {
                y = drawWrappedText(canvas, "• " + line, margin + 6, y, contentWidth - 6, 11f, "#2E312E") + 3f;
                if (y > pageHeight - 60) break;
            }
            y += 10f;
        }

        if (!data.memoryLines.isEmpty() && y < pageHeight - 80) {
            y = drawSectionHeading(canvas, "Memories & Milestones", margin, y);
            for (String line : data.memoryLines) {
                y = drawWrappedText(canvas, "• " + line, margin + 6, y, contentWidth - 6, 11f, "#2E312E") + 3f;
                if (y > pageHeight - 50) break;
            }
        }

        // Footer
        Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        footerPaint.setColor(Color.parseColor("#767873"));
        footerPaint.setTextSize(9f);
        canvas.drawText("Generated by Family Hub • Offline Family Archive", margin, pageHeight - 24f, footerPaint);

        document.finishPage(page);

        String safeName = data.fullName.replaceAll("[^a-zA-Z0-9_-]", "_");
        File outFile = new File(ensureExportsDir(context), "Profile_" + safeName + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }
        return outFile;
    }

    public static File generateMemberShareImage(Context context, MemberPdfData data) throws IOException {
        final int width = 1080;
        final int height = 1350;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(Color.parseColor("#FAF6F0"));

        Paint topBand = new Paint(Paint.ANTI_ALIAS_FLAG);
        topBand.setColor(Color.parseColor("#2B5B4E"));
        canvas.drawRect(0, 0, width, 380, topBand);

        // Decorative border card
        Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setColor(Color.WHITE);
        RectF cardRect = new RectF(64, 240, width - 64, height - 90);
        canvas.drawRoundRect(cardRect, 40f, 40f, cardPaint);

        // Avatar
        float cx = width / 2f;
        float cy = 240f;
        float radius = 110f;
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setColor(Color.parseColor("#FAF6F0"));
        canvas.drawCircle(cx, cy, radius + 10f, ringPaint);

        Bitmap photoBitmap = loadSampledBitmap(data.photoPath, 320);
        if (photoBitmap != null) {
            Bitmap circleBmp = createCircularBitmap(photoBitmap, 260);
            canvas.drawBitmap(circleBmp, null, new RectF(cx - radius, cy - radius, cx + radius, cy + radius), null);
            photoBitmap.recycle();
            circleBmp.recycle();
        } else {
            Paint avatarBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            avatarBg.setColor(Color.parseColor("#B85D3B"));
            canvas.drawCircle(cx, cy, radius, avatarBg);
            Paint initPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            initPaint.setColor(Color.WHITE);
            initPaint.setTextSize(92f);
            initPaint.setTextAlign(Paint.Align.CENTER);
            initPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            String initial = data.fullName.isEmpty() ? "?" : data.fullName.substring(0, 1).toUpperCase();
            canvas.drawText(initial, cx, cy + 32f, initPaint);
        }

        // Name
        Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(Color.parseColor("#1B1D1B"));
        namePaint.setTextSize(54f);
        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        canvas.drawText(data.fullName, cx, 425f, namePaint);

        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(Color.parseColor("#B85D3B"));
        subPaint.setTextSize(28f);
        subPaint.setTextAlign(Paint.Align.CENTER);
        String sub = data.dateOfBirthDisplay != null && !data.dateOfBirthDisplay.isEmpty()
                ? data.dateOfBirthDisplay
                : (data.relationshipToOwner != null ? data.relationshipToOwner : "Family Member");
        canvas.drawText(sub, cx, 472f, subPaint);

        float y = 530f;
        int innerWidth = width - 192;
        if (data.biography != null && !data.biography.isEmpty()) {
            y = drawWrappedText(canvas, data.biography, 96, y, innerWidth, 26f, "#454844") + 32f;
        }
        if (!data.relationshipLines.isEmpty()) {
            y = drawWrappedText(canvas, "Family Links", 96, y, innerWidth, 28f, "#2B5B4E") + 10f;
            int shown = 0;
            for (String line : data.relationshipLines) {
                if (shown++ >= 5) break;
                y = drawWrappedText(canvas, "• " + line, 108, y, innerWidth - 12, 24f, "#222422") + 8f;
            }
            y += 20f;
        }
        if (!data.infoLines.isEmpty() && y < height - 220) {
            y = drawWrappedText(canvas, "Highlights", 96, y, innerWidth, 28f, "#2B5B4E") + 10f;
            int shown = 0;
            for (String line : data.infoLines) {
                if (shown++ >= 4 || y > height - 160) break;
                y = drawWrappedText(canvas, "• " + line, 108, y, innerWidth - 12, 24f, "#222422") + 8f;
            }
        }

        Paint footer = new Paint(Paint.ANTI_ALIAS_FLAG);
        footer.setColor(Color.parseColor("#767873"));
        footer.setTextSize(22f);
        footer.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Family Hub Archive", cx, height - 38f, footer);

        String safeName = data.fullName.replaceAll("[^a-zA-Z0-9_-]", "_");
        File outFile = new File(ensureExportsDir(context), "Card_" + safeName + ".png");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos);
        } finally {
            bitmap.recycle();
        }
        return outFile;
    }

    public static File generateFamilyTreePdf(
            Context context,
            List<TreePdfNode> nodes,
            List<TreePdfEdge> edges
    ) throws IOException {
        PdfDocument document = new PdfDocument();
        final int pageWidth = 842; // A4 landscape
        final int pageHeight = 595;
        final int margin = 36;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.parseColor("#FAF6F0"));

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#2B5B4E"));
        titlePaint.setTextSize(22f);
        titlePaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        canvas.drawText("Family Heritage Tree", margin, 38f, titlePaint);

        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(Color.parseColor("#767873"));
        subPaint.setTextSize(10f);
        canvas.drawText("Printable chart • " + nodes.size() + " family members", margin, 54f, subPaint);

        if (!nodes.isEmpty()) {
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (TreePdfNode n : nodes) {
                minX = Math.min(minX, n.x);
                maxX = Math.max(maxX, n.x);
                minY = Math.min(minY, n.y);
                maxY = Math.max(maxY, n.y);
            }
            float spanX = Math.max(200f, maxX - minX + 220f);
            float spanY = Math.max(150f, maxY - minY + 140f);
            float availW = pageWidth - (margin * 2f);
            float availH = pageHeight - 100f;
            float scale = Math.min(availW / spanX, availH / spanY);
            scale = Math.min(scale, 1.0f);

            float offsetX = margin + (availW - (maxX - minX) * scale) / 2f - minX * scale;
            float offsetY = 85f + (availH - (maxY - minY) * scale) / 2f - minY * scale;

            Paint parentLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            parentLinePaint.setColor(Color.parseColor("#2B5B4E"));
            parentLinePaint.setStrokeWidth(Math.max(1.5f, 2.2f * scale));
            parentLinePaint.setStyle(Paint.Style.STROKE);

            Paint spouseLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            spouseLinePaint.setColor(Color.parseColor("#B85D3B"));
            spouseLinePaint.setStrokeWidth(Math.max(1.5f, 2.5f * scale));
            spouseLinePaint.setStyle(Paint.Style.STROKE);

            for (TreePdfEdge edge : edges) {
                float sx = edge.fromX * scale + offsetX;
                float sy = edge.fromY * scale + offsetY;
                float tx = edge.toX * scale + offsetX;
                float ty = edge.toY * scale + offsetY;
                canvas.drawLine(sx, sy, tx, ty, edge.isSpouse ? spouseLinePaint : parentLinePaint);
            }

            float boxW = Math.max(70f, 130f * scale);
            float boxH = Math.max(32f, 52f * scale);

            Paint boxFill = new Paint(Paint.ANTI_ALIAS_FLAG);
            boxFill.setColor(Color.WHITE);
            Paint boxStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            boxStroke.setColor(Color.parseColor("#2B5B4E"));
            boxStroke.setStrokeWidth(1.5f);
            boxStroke.setStyle(Paint.Style.STROKE);

            Paint nodeNamePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            nodeNamePaint.setColor(Color.parseColor("#1B1D1B"));
            nodeNamePaint.setTextSize(Math.max(7.5f, 11f * scale));
            nodeNamePaint.setTextAlign(Paint.Align.CENTER);
            nodeNamePaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));

            Paint nodeSubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            nodeSubPaint.setColor(Color.parseColor("#60635E"));
            nodeSubPaint.setTextSize(Math.max(6.5f, 8.5f * scale));
            nodeSubPaint.setTextAlign(Paint.Align.CENTER);

            for (TreePdfNode node : nodes) {
                float cx = node.x * scale + offsetX;
                float cy = node.y * scale + offsetY;
                RectF rect = new RectF(cx - boxW / 2f, cy - boxH / 2f, cx + boxW / 2f, cy + boxH / 2f);
                canvas.drawRoundRect(rect, 8f, 8f, boxFill);
                canvas.drawRoundRect(rect, 8f, 8f, boxStroke);

                String displayName = node.name.length() > 18 ? node.name.substring(0, 16) + "…" : node.name;
                canvas.drawText(displayName, cx, cy - 2f, nodeNamePaint);
                if (node.subtitle != null && !node.subtitle.isEmpty()) {
                    canvas.drawText(node.subtitle, cx, cy + 11f * Math.max(0.75f, scale), nodeSubPaint);
                }
            }
        }

        document.finishPage(page);
        File outFile = new File(ensureExportsDir(context), "Family_Tree_Chart.pdf");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }
        return outFile;
    }

    private static float drawSectionHeading(Canvas canvas, String title, float x, float y) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#2B5B4E"));
        paint.setTextSize(14f);
        paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        canvas.drawText(title, x, y, paint);

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(Color.parseColor("#C9C4B9"));
        line.setStrokeWidth(1f);
        canvas.drawLine(x, y + 5f, x + 515f, y + 5f, line);
        return y + 20f;
    }

    private static float drawWrappedText(Canvas canvas, String text, float x, float y, int width, float textSize, String hexColor) {
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor(hexColor));
        textPaint.setTextSize(textSize);
        StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, Math.max(100, width))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.1f)
                .build();
        canvas.save();
        canvas.translate(x, y);
        layout.draw(canvas);
        canvas.restore();
        return y + layout.getHeight();
    }

    private static Bitmap loadSampledBitmap(String path, int reqSize) {
        if (path == null || path.isEmpty()) return null;
        File f = new File(path);
        if (!f.exists()) return null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
        int sample = 1;
        while (opts.outWidth / sample > reqSize * 2 || opts.outHeight / sample > reqSize * 2) {
            sample *= 2;
        }
        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
        decodeOpts.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, decodeOpts);
    }

    private static Bitmap createCircularBitmap(Bitmap source, int size) {
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));

        int srcMin = Math.min(source.getWidth(), source.getHeight());
        int left = (source.getWidth() - srcMin) / 2;
        int top = (source.getHeight() - srcMin) / 2;
        android.graphics.Rect srcRect = new android.graphics.Rect(left, top, left + srcMin, top + srcMin);
        android.graphics.Rect dstRect = new android.graphics.Rect(0, 0, size, size);
        canvas.drawBitmap(source, srcRect, dstRect, paint);
        return output;
    }
}
