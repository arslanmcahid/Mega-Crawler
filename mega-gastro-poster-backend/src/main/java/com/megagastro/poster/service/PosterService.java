package com.megagastro.poster.service;

import com.megagastro.poster.model.Product;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PosterService {

    public String buildPosterHtml(String title, List<Product> products, int count) {
        String effectiveTitle = (title == null || title.isBlank()) ? "Haftanın Fırsatları" : title;

        List<Product> selected = products.stream()
                .filter(p -> p.getPriceCurrent() != null && p.getPriceCurrent() > 0)
                .sorted(Comparator
                        .comparing(Product::getDiscountPct, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Product::getPriceCurrent))
                .limit(count)
                .collect(Collectors.toList());

        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.GERMANY);

        StringBuilder cards = new StringBuilder();
        for (Product p : selected) {
            String badge = "";
            if (p.getDiscountPct() != null && p.getDiscountPct() > 0) {
                badge = "<div class=\"badge\">-" + p.getDiscountPct() + "%</div>";
            }
            String oldPriceHtml = "";
            if (p.getPriceOriginal() != null &&
                    !p.getPriceOriginal().equals(p.getPriceCurrent())) {
                oldPriceHtml = "<div class=\"old\">" + nf.format(p.getPriceOriginal()) + "</div>";
            }
            String imageUrl = p.getImageUrl() != null ? p.getImageUrl() : "";

            cards.append("<div class=\"card\">")
                    .append(badge)
                    .append("<div class=\"image-wrapper\">")
                    .append("<img src=\"").append(escapeHtml(imageUrl)).append("\" alt=\"").append(escapeHtml(p.getName())).append("\"/>")
                    .append("</div>")
                    .append("<div class=\"name\">").append(escapeHtml(p.getName())).append("</div>")
                    .append("<div class=\"prices\">")
                    .append("<div class=\"current\">").append(nf.format(p.getPriceCurrent())).append("</div>")
                    .append(oldPriceHtml)
                    .append("</div>")
                    .append("</div>");
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        return "<!DOCTYPE html>" +
                "<html lang=\"tr\">" +
                "<head>" +
                "<meta charset=\"UTF-8\"/>" +
                "<style>" +
                "@page { size: A4; margin: 18mm; }" +
                "body { font-family: Arial, sans-serif; }" +
                "h1 { text-align: center; margin-bottom: 16px; }" +
                ".grid { display: flex; flex-wrap: wrap; gap: 8px; }" +
                ".card { position: relative; width: 30%; border: 1px solid #ccc; padding: 8px; box-sizing: border-box; }" +
                ".image-wrapper { height: 120px; text-align: center; }" +
                ".image-wrapper img { max-height: 120px; max-width: 100%; object-fit: contain; }" +
                ".name { font-size: 12px; margin-top: 4px; min-height: 32px; }" +
                ".prices { margin-top: 4px; }" +
                ".current { font-size: 16px; font-weight: bold; color: #c0392b; }" +
                ".old { font-size: 12px; text-decoration: line-through; color: #7f8c8d; }" +
                ".badge { position: absolute; top: 4px; left: 4px; background: #e74c3c; color: #fff; padding: 2px 6px; font-size: 10px; border-radius: 4px; }" +
                ".footer { position: fixed; bottom: 10mm; left: 0; right: 0; text-align: center; font-size: 10px; color: #555; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<h1>" + escapeHtml(effectiveTitle) + "</h1>" +
                "<div class=\"grid\">" + cards + "</div>" +
                "<div class=\"footer\">Oluşturulma tarihi: " + today + "</div>" +
                "</body>" +
                "</html>";
    }

    public byte[] renderPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render PDF", e);
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}


