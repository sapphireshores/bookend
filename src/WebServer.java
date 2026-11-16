import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class WebServer {
    private static final Library library = Library.loadOrCreate();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Serve index.html
        server.createContext("/", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }
            Path htmlPath = Path.of("index.html");
            if (!Files.exists(htmlPath)) {
                sendText(exchange, 404, "index.html not found");
                return;
            }
            byte[] bytes = Files.readAllBytes(htmlPath);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // GET /api/state -> returns inventory & overdue reports
        server.createContext("/api/state", exchange -> {
            String inv = escapeJson(library.getInventory());
            String overdue = escapeJson(library.getOverdueLoans());
            String json = "{\"currentDay\":" + library.getCurrentDay()
                    + ",\"inventory\":\"" + inv + "\",\"overdue\":\"" + overdue + "\"}";
            sendJson(exchange, 200, json);
        });

        // POST /api/items
        server.createContext("/api/items", exchange -> {
            try {
                Map<String, String> body = parseSimpleJson(readBody(exchange));
                String type = body.getOrDefault("type", "");
                String id = body.getOrDefault("id", "").trim();
                String title = body.getOrDefault("title", "").trim();
                String extra = body.getOrDefault("extra", "").trim();

                if (id.isEmpty() || title.isEmpty() || extra.isEmpty()) {
                    throw new InvalidInputException("All fields are required");
                }

                LibraryItem item = switch (type) {
                    case "BOOK" -> new Book(id, title, extra);
                    case "MAGAZINE" -> new Magazine(id, title, extra);
                    case "REFERENCE" -> new ReferenceBook(id, title, extra);
                    default -> throw new InvalidInputException("Invalid item type");
                };
                library.addItem(item);
                sendJson(exchange, 200, "{\"message\":\"Item added\"}");
            } catch (LibraryException e) {
                sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        });

        // POST /api/members
        server.createContext("/api/members", exchange -> {
            try {
                Map<String, String> body = parseSimpleJson(readBody(exchange));
                String type = body.getOrDefault("type", "");
                String id = body.getOrDefault("id", "").trim();
                String name = body.getOrDefault("name", "").trim();

                if (id.isEmpty() || name.isEmpty()) {
                    throw new InvalidInputException("All fields are required");
                }

                Member member = switch (type) {
                    case "STUDENT" -> new Student(id, name);
                    case "STAFF" -> new Staff(id, name);
                    case "FACULTY" -> new Faculty(id, name);
                    default -> throw new InvalidInputException("Invalid member type");
                };
                library.addMember(member);
                sendJson(exchange, 200, "{\"message\":\"Member registered\"}");
            } catch (LibraryException e) {
                sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        });

        // POST /api/borrow
        server.createContext("/api/borrow", exchange -> {
            try {
                Map<String, String> body = parseSimpleJson(readBody(exchange));
                String res = library.borrowItem(body.getOrDefault("memberId", "").trim(), body.getOrDefault("itemId", "").trim());
                sendJson(exchange, 200, "{\"message\":\"" + escapeJson(res) + "\"}");
            } catch (LibraryException e) {
                sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        });

        // POST /api/return
        server.createContext("/api/return", exchange -> {
            try {
                Map<String, String> body = parseSimpleJson(readBody(exchange));
                String res = library.returnItem(body.getOrDefault("itemId", "").trim());
                sendJson(exchange, 200, "{\"message\":\"" + escapeJson(res) + "\"}");
            } catch (LibraryException e) {
                sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        });

        // POST /api/advance
        server.createContext("/api/advance", exchange -> {
            try {
                Map<String, String> body = parseSimpleJson(readBody(exchange));
                int days = Integer.parseInt(body.getOrDefault("days", "0").trim());
                String res = library.advanceTime(days);
                sendJson(exchange, 200, "{\"message\":\"" + escapeJson(res) + "\"}");
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"error\":\"Invalid number of days\"}");
            } catch (LibraryException e) {
                sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server running at http://localhost:8080");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int code, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        String cleaned = json.trim();
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        if (cleaned.isEmpty()) return map;
        for (String pair : cleaned.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String k = kv[0].trim().replaceAll("^\"|\"$", "");
                String v = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(k, v);
            }
        }
        return map;
    }
}