package com.minibank;

import com.google.gson.Gson;
import com.minibank.dto.CreateAccountRequest;
import com.minibank.dto.CreateClientRequest;
import com.minibank.dto.TransferRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class HttpServerApp {

    private static final BankService bankService = new BankService();
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Регистрируем эндпоинты
        server.createContext("/clients", new CreateClientHandler());
        server.createContext("/accounts", new CreateAccountHandler());
        server.createContext("/transfer", new TransferHandler());

        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port 8080");
    }

    // --- ХЕНДЛЕРЫ ---

    static class CreateClientHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            try {
                String body = readBody(exchange.getRequestBody());
                CreateClientRequest req = gson.fromJson(body, CreateClientRequest.class);
                
                // Валидация
                if (req.name == null || req.email == null) {
                    sendResponse(exchange, 400, "Name and email are required");
                    return;
                }

                Client client = bankService.registerClient(req.name, req.email);
                sendResponse(exchange, 200, "Client created with ID: " + client.getId());
            } catch (Exception e) {
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
    }

    static class CreateAccountHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            try {
                String body = readBody(exchange.getRequestBody());
                CreateAccountRequest req = gson.fromJson(body, CreateAccountRequest.class);

                if (req.clientId == null || req.accountNumber == null) {
                    sendResponse(exchange, 400, "Client ID and Account Number are required");
                    return;
                }

                Account account = bankService.openAccount(req.clientId, req.accountNumber);
                sendResponse(exchange, 200, "Account created with ID: " + account.getId());
            } catch (Exception e) {
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
    }

    static class TransferHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 1. Проверяем метод (GET запрещен)
            if ("GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method GET is not allowed. Use POST.");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body = readBody(exchange.getRequestBody());
                TransferRequest req = gson.fromJson(body, TransferRequest.class);

                // 2. Валидация суммы (≤ 0 → 400)
                if (req.amount == null || req.amount.compareTo(BigDecimal.ZERO) <= 0) {
                    sendResponse(exchange, 400, "Amount must be greater than zero");
                    return;
                }

                // 3. Вызов бизнес-логики
                bankService.transfer(req.fromAccountId, req.toAccountId, req.amount);
                sendResponse(exchange, 200, "Transfer successful");

            } catch (IllegalArgumentException e) {
                // Если счет не найден
                if (e.getMessage().contains("not found")) {
                    sendResponse(exchange, 404, e.getMessage());
                } else {
                    sendResponse(exchange, 400, e.getMessage());
                }
            } catch (IllegalStateException e) {
                // Если недостаточно средств
                if (e.getMessage().contains("Insufficient funds")) {
                    sendResponse(exchange, 409, e.getMessage());
                } else {
                    sendResponse(exchange, 400, e.getMessage());
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
    }

    // --- УТИЛИТЫ ---

    private static String readBody(InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
