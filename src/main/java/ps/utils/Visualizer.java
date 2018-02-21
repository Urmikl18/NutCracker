package ps.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

import com.google.common.io.Files;

import ps.models.ChangeTag;

public class Visualizer {
    private Visualizer() {
    }

    public static void visualize(ArrayList<ChangeTag> classification, String original) {
        String html = toHTML(classification, original);

        try {
            String path = saveToFile(html);
            System.out.println("\nVisualization is found at: " + path);
        } catch (IOException e) {
            System.out.println("Cannot visualize classification.");
        }
    }

    private static String saveToFile(String html) throws IOException {
        File htmlTemplateFile = new File("src/main/resources/export/template.html");
        String htmlString = Files.toString(htmlTemplateFile, Charset.defaultCharset());
        String uniqueID = UUID.randomUUID().toString();
        String title = "class_" + uniqueID;
        String body = html;
        htmlString = htmlString.replace("$title", title);
        htmlString = htmlString.replace("$body", body);
        File newHtmlFile = new File(title + ".html");
        Files.write(htmlString, newHtmlFile, Charset.defaultCharset());
        return newHtmlFile.getAbsolutePath();
    }

    private static String toHTML(ArrayList<ChangeTag> classification, String original) {
        StringBuilder html = new StringBuilder();
        StringBuilder types = new StringBuilder();
        types.append("<ol>");
        String equal = "", before = "", after = "";
        int left = 0;
        int right = 0;
        int index = 1;
        for (ChangeTag ct : classification) {

            left = right;
            right = ct.getChange().getPos1();

            equal = original.substring(left, right).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\n", "&para;<br>");
            before = original.substring(right, right + ct.getChange().getBefore().length()).replace("&", "&amp;")
                    .replace("<", "&lt;").replace(">", "&gt;").replace("\n", "&para;<br>");
            after = ct.getChange().getAfter().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\n", "&para;<br>");
            html.append("<span>").append(equal).append("</span>");
            html.append("<del style=\"background:#ffe6e6;\">").append(before).append("</del>");
            html.append("<ins style=\"background:#e6ffe6;\">").append(after).append("</ins>");
            html.append("<sup><a href=\"#" + index + "\">" + index + "</a></sup>");

            types.append("<li id=\"" + (index) + "\">" + ct.getTag().toString() + "</li>");
            ++index;
        }
        types.append("</ol>");
        return html.toString() + "&para;<br>" + types.toString();
    }

}