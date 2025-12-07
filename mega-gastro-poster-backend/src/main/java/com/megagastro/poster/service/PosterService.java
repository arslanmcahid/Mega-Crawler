package com.megagastro.poster.service;

import com.megagastro.poster.model.Product;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PosterService {

    public String buildPosterHtml(String title, List<Product> products, int count) {
        String effectiveTitle = (title == null || title.isBlank()) ? "Haftanın Fırsatları" : title;

        // Maksimum 9 ürün
        int maxProducts = Math.min(count, 9);

        // Filtrele ve sırala
        List<Product> filtered = products.stream()
                .filter(p -> p.getPriceCurrent() != null && p.getPriceCurrent() > 0)
                .sorted(Comparator
                        .comparing(Product::getDiscountPct, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Product::getPriceCurrent))
                .limit(maxProducts)
                .collect(Collectors.toList());

        // Kategoriye göre gruplandır
        Map<String, List<Product>> productsByCategory = filtered.stream()
                .collect(Collectors.groupingBy(
                        p -> {
                            String cat = p.getCategory();
                            return (cat == null || cat.isBlank()) ? "Diğer" : cat;
                        },
                        LinkedHashMap::new, // Sıralamayı koru
                        Collectors.toList()
                ));

        // Kategorileri sırala (alfabetik)
        List<String> sortedCategories = new ArrayList<>(productsByCategory.keySet());
        Collections.sort(sortedCategories);

        int productCount = filtered.size();
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.GERMANY);

        StringBuilder cards = new StringBuilder();
        
        // Her kategori için ürünleri yan yana diz
        for (String category : sortedCategories) {
            List<Product> categoryProducts = productsByCategory.get(category);
            
            // Kategori başlığı (opsiyonel, sadece birden fazla kategori varsa göster)
            if (sortedCategories.size() > 1) {
                cards.append("<div class=\"category-header\">").append(escapeHtml(category)).append("</div>");
            }
            
            // Bu kategorideki ürünleri yan yana diz
            for (Product p : categoryProducts) {
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
                        .append("<img src=\"").append(escapeHtml(imageUrl)).append("\" alt=\"")
                        .append(escapeHtml(p.getName())).append("\"/>")
                        .append("</div>")
                        .append("<div class=\"name\">").append(escapeHtml(p.getName())).append("</div>")
                        .append("<div class=\"prices\">")
                        .append("<div class=\"current\">").append(nf.format(p.getPriceCurrent())).append("</div>")
                        .append(oldPriceHtml)
                        .append("</div>")
                        .append("</div>");
            }
            
            // Kategori sonrası temizlik (clearfix)
            cards.append("<div class=\"clearfix\"></div>");
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        // Responsive grid için card genişliği hesapla
        String cardWidth = calculateCardWidth(productCount);

        return "<!DOCTYPE html>" +
                "<html lang=\"tr\">" +
                "<head>" +
                "<meta charset=\"UTF-8\"/>" +
                "<style>" +
                "@page { size: A4; margin: 18mm; }" +
                "body { font-family: Arial, sans-serif; margin: 0; padding: 0; }" +
                "h1 { text-align: center; margin-bottom: 20px; font-size: 24px; }" +
                ".grid { width: 100%; overflow: hidden; }" +
                ".category-header { width: 100%; font-size: 16px; font-weight: bold; color: #2c3e50; margin-top: 16px; margin-bottom: 8px; padding-bottom: 4px; border-bottom: 2px solid #3498db; clear: both; }" +
                ".card { position: relative; width: " + cardWidth
                + "; border: 1px solid #ddd; padding: 12px; border-radius: 4px; background: #fff; float: left; margin-right: 12px; margin-bottom: 12px; }"
                +
                ".image-wrapper { height: 140px; text-align: center; margin-bottom: 8px; line-height: 140px; }" +
                ".image-wrapper img { max-height: 140px; max-width: 100%; vertical-align: middle; }" +
                ".name { font-size: 13px; margin-top: 8px; min-height: 36px; line-height: 1.3; color: #333; }" +
                ".prices { margin-top: 8px; }" +
                ".current { font-size: 18px; font-weight: bold; color: #c0392b; }" +
                ".old { font-size: 13px; text-decoration: line-through; color: #7f8c8d; margin-top: 2px; }" +
                ".badge { position: absolute; top: 8px; left: 8px; background: #e74c3c; color: #fff; padding: 4px 8px; font-size: 11px; border-radius: 4px; font-weight: bold; }"
                +
                ".footer { position: fixed; bottom: 10mm; left: 0; right: 0; text-align: center; font-size: 10px; color: #555; }"
                +
                ".clearfix { clear: both; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<h1>" + escapeHtml(effectiveTitle) + "</h1>" +
                "<div class=\"grid\">" + cards + "<div class=\"clearfix\"></div></div>" +
                "<div class=\"footer\">Oluşturulma tarihi: " + today + "</div>" +
                "</body>" +
                "</html>";
    }

    private String calculateCardWidth(int productCount) {
        // Responsive grid: ürün sayısına göre card genişliği (openhtmltopdf uyumlu)
        if (productCount == 1) {
            return "100%";
        } else if (productCount == 2) {
            return "48%";
        } else if (productCount <= 4) {
            return "48%";
        } else {
            // 5-9 ürün için 3 kolonlu grid
            return "31%";
        }
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
        if (input == null)
            return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
