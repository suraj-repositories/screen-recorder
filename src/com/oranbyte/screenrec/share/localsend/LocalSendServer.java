package com.oranbyte.screenrec.share.localsend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;

import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

public class LocalSendServer {

    private final int port;

    private final Path downloadDirectory;

    private final Map<String, Session> sessions =
            new ConcurrentHashMap<>();

    private HttpsServer server;

    public LocalSendServer(int port) {

        this.port = port;

        this.downloadDirectory =
                Path.of(
                        System.getProperty("user.home"),
                        "Downloads",
                        "ScreenRecorder");
    }

    public synchronized void start()
            throws Exception {

        if (server != null) {
            return;
        }

        Files.createDirectories(
                downloadDirectory);

        SSLContext sslContext =
                LocalSendSslContext
                        .createServerContext();

        server =
                HttpsServer.create(
                        new InetSocketAddress(
                                port),
                        0);

        server.setExecutor(
                java.util.concurrent.Executors
                        .newCachedThreadPool(
                                runnable -> {

                                    Thread thread =
                                            new Thread(
                                                    runnable,
                                                    "LocalSend-HTTP");

                                    thread.setDaemon(
                                            true);

                                    return thread;
                                }));

        server.setHttpsConfigurator(
                new HttpsConfigurator(
                        sslContext));

        server.createContext(
                LocalSendProtocol
                        .REGISTER_PATH,
                this::handleRegister);

        server.createContext(
                LocalSendProtocol
                        .PREPARE_UPLOAD_PATH,
                this::handlePrepareUpload);

        server.createContext(
                LocalSendProtocol
                        .UPLOAD_PATH,
                this::handleUpload);

        server.createContext(
                LocalSendProtocol
                        .CANCEL_PATH,
                this::handleCancel);

        server.createContext(
                LocalSendProtocol
                        .INFO_PATH,
                this::handleInfo);

        server.start();

        System.out.println(
                "LocalSend server started on port "
                        + port);
    }

    public synchronized void stop() {

        if (server != null) {

            server.stop(0);

            server = null;
        }

        sessions.clear();
    }

    private void handleRegister(
            HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendStatus(
                    exchange,
                    405);

            return;
        }

        String body =
                readBody(exchange);

        System.out.println(
                "LocalSend register: "
                        + body);

        sendJson(
                exchange,
                200,
                createIdentityJson());
    }

    private void handlePrepareUpload(
            HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendStatus(
                    exchange,
                    405);

            return;
        }

        try {

            String body =
                    readBody(exchange);

            JSONObject request =
                    new JSONObject(body);

            JSONObject files =
                    request.optJSONObject(
                            "files");

            if (files == null ||
                    files.isEmpty()) {

                sendStatus(
                        exchange,
                        400);

                return;
            }

            /*
             * LocalSend allows one active session.
             */
            if (!sessions.isEmpty()) {

                sendStatus(
                        exchange,
                        409);

                return;
            }

            String sessionId =
                    UUID.randomUUID()
                            .toString();

            Session session =
                    new Session(
                            sessionId,
                            exchange.getRemoteAddress()
                                    .getAddress()
                                    .getHostAddress());

            for (String fileId :
                    files.keySet()) {

                JSONObject jsonFile =
                        files.getJSONObject(
                                fileId);

                String name =
                        jsonFile.getString(
                                "fileName");

                long size =
                        jsonFile.getLong(
                                "size");

                String token =
                        UUID.randomUUID()
                                .toString();

                session.files.put(
                        fileId,
                        new PendingFile(
                                fileId,
                                token,
                                name,
                                size));
            }

            sessions.put(
                    sessionId,
                    session);

            JSONObject response =
                    new JSONObject();

            response.put(
                    "sessionId",
                    sessionId);

            JSONObject tokens =
                    new JSONObject();

            for (PendingFile file :
                    session.files.values()) {

                tokens.put(
                        file.id,
                        file.token);
            }

            response.put(
                    "files",
                    tokens);

            sendJson(
                    exchange,
                    200,
                    response);

        } catch (Exception e) {

            e.printStackTrace();

            sendStatus(
                    exchange,
                    400);
        }
    }

    private void handleUpload(
            HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendStatus(
                    exchange,
                    405);

            return;
        }

        Map<String, String> query =
                parseQuery(
                        exchange.getRequestURI()
                                .getRawQuery());

        String sessionId =
                query.get("sessionId");

        String fileId =
                query.get("fileId");

        String token =
                query.get("token");

        if (sessionId == null ||
                fileId == null ||
                token == null) {

            sendStatus(
                    exchange,
                    400);

            return;
        }

        Session session =
                sessions.get(sessionId);

        if (session == null) {

            sendStatus(
                    exchange,
                    403);

            return;
        }

        String remoteAddress =
                exchange.getRemoteAddress()
                        .getAddress()
                        .getHostAddress();

        if (!session.remoteAddress
                .equals(remoteAddress)) {

            sendStatus(
                    exchange,
                    403);

            return;
        }

        PendingFile file =
                session.files.get(fileId);

        if (file == null ||
                !file.token.equals(token)) {

            sendStatus(
                    exchange,
                    403);

            return;
        }

        Path target =
                createSafePath(
                        file.fileName);

        Path temporary =
                target.resolveSibling(
                        target.getFileName()
                                + ".part");

        try {

            long received = 0;

            try (InputStream input =
                         exchange.getRequestBody();

                 OutputStream output =
                         Files.newOutputStream(
                                 temporary)) {

                byte[] buffer =
                        new byte[1024 * 1024];

                int read;

                while ((read =
                        input.read(buffer)) != -1) {

                    output.write(
                            buffer,
                            0,
                            read);

                    received += read;
                }
            }

            if (received != file.size) {

                Files.deleteIfExists(
                        temporary);

                sendStatus(
                        exchange,
                        500);

                return;
            }

            Files.move(
                    temporary,
                    target,
                    StandardCopyOption
                            .REPLACE_EXISTING);

            file.received = true;

            System.out.println(
                    "LocalSend received: "
                            + target);

            sendStatus(
                    exchange,
                    200);

            if (session.allReceived()) {

                sessions.remove(
                        sessionId);
            }

        } catch (Exception e) {

            Files.deleteIfExists(
                    temporary);

            e.printStackTrace();

            sendStatus(
                    exchange,
                    500);
        }
    }

    private void handleCancel(
            HttpExchange exchange)
            throws IOException {

        Map<String, String> query =
                parseQuery(
                        exchange.getRequestURI()
                                .getRawQuery());

        String sessionId =
                query.get("sessionId");

        if (sessionId != null) {

            sessions.remove(
                    sessionId);
        }

        sendStatus(
                exchange,
                200);
    }

    private void handleInfo(
            HttpExchange exchange)
            throws IOException {

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendStatus(
                    exchange,
                    405);

            return;
        }

        sendJson(
                exchange,
                200,
                createIdentityJson());
    }

    private JSONObject createIdentityJson() {

        JSONObject json =
                new JSONObject();

        json.put(
                "alias",
                LocalSendIdentity.getAlias());

        json.put(
                "version",
                LocalSendProtocol.VERSION);

        json.put(
                "deviceModel",
                LocalSendIdentity
                        .getDeviceModel());

        json.put(
                "deviceType",
                LocalSendIdentity
                        .getDeviceType());

        try {

            json.put(
                    "fingerprint",
                    LocalSendSslContext
                            .getCertificateFingerprint());

        } catch (Exception e) {

            json.put(
                    "fingerprint",
                    LocalSendIdentity
                            .getFingerprint());
        }

        json.put(
                "download",
                false);

        return json;
    }

    private Path createSafePath(
            String fileName) {

        String safeName =
                Path.of(fileName)
                        .getFileName()
                        .toString();

        return downloadDirectory
                .resolve(safeName)
                .normalize();
    }

    private String readBody(
            HttpExchange exchange)
            throws IOException {

        try (InputStream input =
                     exchange.getRequestBody()) {

            return new String(
                    input.readAllBytes(),
                    java.nio.charset.StandardCharsets
                            .UTF_8);
        }
    }

    private void sendJson(
            HttpExchange exchange,
            int status,
            JSONObject json)
            throws IOException {

        byte[] data =
                json.toString()
                        .getBytes(
                                java.nio.charset.StandardCharsets
                                        .UTF_8);

        Headers headers =
                exchange.getResponseHeaders();

        headers.set(
                LocalSendProtocol
                        .HEADER_CONTENT_TYPE,
                LocalSendProtocol
                        .CONTENT_TYPE_JSON);

        exchange.sendResponseHeaders(
                status,
                data.length);

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(data);
        }
    }

    private void sendStatus(
            HttpExchange exchange,
            int status)
            throws IOException {

        exchange.sendResponseHeaders(
                status,
                -1);

        exchange.close();
    }

    private Map<String, String> parseQuery(
            String query) {

        Map<String, String> result =
                new HashMap<>();

        if (query == null ||
                query.isBlank()) {

            return result;
        }

        for (String part :
                query.split("&")) {

            String[] pair =
                    part.split(
                            "=",
                            2);

            String key =
                    java.net.URLDecoder.decode(
                            pair[0],
                            java.nio.charset.StandardCharsets
                                    .UTF_8);

            String value =
                    pair.length > 1
                            ? java.net.URLDecoder.decode(
                                    pair[1],
                                    java.nio.charset.StandardCharsets
                                            .UTF_8)
                            : "";

            result.put(
                    key,
                    value);
        }

        return result;
    }

    private static class Session {

        final String id;

        final String remoteAddress;

        final Map<String, PendingFile> files =
                new ConcurrentHashMap<>();

        Session(
                String id,
                String remoteAddress) {

            this.id = id;

            this.remoteAddress =
                    remoteAddress;
        }

        boolean allReceived() {

            return files.values()
                    .stream()
                    .allMatch(
                            file -> file.received);
        }
    }

    private static class PendingFile {

        final String id;

        final String token;

        final String fileName;

        final long size;

        volatile boolean received;

        PendingFile(
                String id,
                String token,
                String fileName,
                long size) {

            this.id = id;

            this.token = token;

            this.fileName = fileName;

            this.size = size;
        }
    }
}