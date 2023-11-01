package com.letbyte.core.meshfilesharing.comm.fileserver.webserver;

import android.os.Build;
import android.util.Log;

import com.letbyte.core.meshfilesharing.helper.Const;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.httpservices.NanoHTTPServer;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.IHTTPSession;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.NanoHTTPD;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.request.Method;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.IStatus;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.Response;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.Status;
import com.w3engineers.mesh.util.MeshLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.StringTokenizer;

import javax.net.ssl.KeyManagerFactory;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-03-19 at 6:59 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-03-19 at 6:59 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-03-19 at 6:59 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class SimpleWebServer extends NanoHTTPServer {

    public static final String FILE_PATH_HEADER_KEY = "path";
    public static final String FILE_MAP_HEADER_KEY = "key-header";
    /**
     * Default Index file names.
     */
    @SuppressWarnings("serial")
    public static final List<String> INDEX_FILE_NAMES = new ArrayList<String>() {

        {
            add("index.html");
            add("index.htm");
        }
    };

    /**
     * The distribution licence
     */
    /*private static final String LICENCE;
    static {
        mimeTypes();
        String text;
        try {
            InputStream stream = SimpleWebServer.class.getResourceAsStream("/LICENSE.txt");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = stream.read(buffer)) >= 0) {
                bytes.write(buffer, 0, count);
            }
            text = bytes.toString("UTF-8");
        } catch (Exception e) {
            text = "unknown";
        }
        LICENCE = text;
    }*/

    private static Map<String, WebServerPlugin> mimeTypeHandlers = new HashMap<String, WebServerPlugin>();

    /**
     * Starts as a standalone file server and waits for Enter.
     */
    public void start() throws IOException {
        // Defaults
        int port = myPort;

        String host = null; // bind to all interfaces by default
        List<File> rootDirs = this.rootDirs;
        boolean quiet = this.quiet;
        String cors = this.cors;
        Map<String, String> options = new HashMap<String, String>();

        // Parse command-line, with short and long versions of the options.
        /*for (int i = 0; i < args.length; ++i) {
            if ("-h".equalsIgnoreCase(args[i]) || "--host".equalsIgnoreCase(args[i])) {
                host = args[i + 1];
            } else if ("-p".equalsIgnoreCase(args[i]) || "--port".equalsIgnoreCase(args[i])) {
                port = Integer.parseInt(args[i + 1]);
            } else if ("-q".equalsIgnoreCase(args[i]) || "--quiet".equalsIgnoreCase(args[i])) {
                quiet = true;
            } else if ("-d".equalsIgnoreCase(args[i]) || "--dir".equalsIgnoreCase(args[i])) {
                rootDirs.add(new File(args[i + 1]).getAbsoluteFile());
            } else if (args[i].startsWith("--cors")) {
                cors = "*";
                int equalIdx = args[i].indexOf('=');
                if (equalIdx > 0) {
                    cors = args[i].substring(equalIdx + 1);
                }
            } else if ("--licence".equalsIgnoreCase(args[i])) {
                System.out.println(SimpleWebServer.LICENCE + "\n");
            } else if (args[i].startsWith("-X:")) {
                int dot = args[i].indexOf('=');
                if (dot > 0) {
                    String name = args[i].substring(0, dot);
                    String value = args[i].substring(dot + 1, args[i].length());
                    options.put(name, value);
                }
            }
        }*/

        if (rootDirs.isEmpty()) {
            rootDirs.add(new File(".").getAbsoluteFile());
        }
        options.put("host", host);
        options.put("port", "" + port);
        options.put("quiet", String.valueOf(quiet));
        StringBuilder sb = new StringBuilder();
        for (File dir : rootDirs) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            try {
                sb.append(dir.getCanonicalPath());
            } catch (IOException ignored) {
            }
        }
        options.put("home", sb.toString());
        ServiceLoader<WebServerPluginInfo> serviceLoader = ServiceLoader.load(WebServerPluginInfo.class);
        for (WebServerPluginInfo info : serviceLoader) {
            String[] mimeTypes = info.getMimeTypes();
            for (String mime : mimeTypes) {
                String[] indexFiles = info.getIndexFilesForMimeType(mime);
                if (!quiet) {
                    System.out.print("# Found plugin for Mime type: \"" + mime + "\"");
                    if (indexFiles != null) {
                        System.out.print(" (serving index files: ");
                        for (String indexFile : indexFiles) {
                            System.out.print(indexFile + " ");
                        }
                    }
                    System.out.println(").");
                }
                registerPluginForMimeType(indexFiles, mime, info.getWebServerPlugin(mime), options);
            }
        }
        super.start();
    }

    protected static void registerPluginForMimeType(String[] indexFiles, String mimeType, WebServerPlugin plugin, Map<String, String> commandLineOptions) {
        if (mimeType == null || plugin == null) {
            return;
        }

        if (indexFiles != null) {
            for (String filename : indexFiles) {
                int dot = filename.lastIndexOf('.');
                if (dot >= 0) {
                    String extension = filename.substring(dot + 1).toLowerCase();
                    mimeTypes().put(extension, mimeType);
                }
            }
            SimpleWebServer.INDEX_FILE_NAMES.addAll(Arrays.asList(indexFiles));
        }
        SimpleWebServer.mimeTypeHandlers.put(mimeType, plugin);
        plugin.initialize(commandLineOptions);
    }

    private final boolean quiet;

    private final String cors;

    protected List<File> rootDirs;

    public SimpleWebServer(String host, int port, File wwwroot, boolean quiet, String cors) {
        this(host, port, Collections.singletonList(wwwroot), quiet, cors);
    }

    public SimpleWebServer(String host, int port, List<File> wwwroots, boolean quiet) {
        this(host, port, wwwroots, quiet, null);
    }

    public SimpleWebServer(String host, int port, List<File> wwwroots, boolean quiet, String cors) {
        super(host, port);
        this.quiet = quiet;
        this.cors = cors;
        this.rootDirs = new ArrayList<>(wwwroots);

        init();
    }

    private boolean canServeUri(String uri, File homeDir) {
        boolean canServeUri;
        File f = new File(homeDir,uri);
        canServeUri = f.exists();
        if (!canServeUri) {
            WebServerPlugin plugin = SimpleWebServer.mimeTypeHandlers.get(getMimeTypeForFile(uri));
            if (plugin != null) {
                canServeUri = plugin.canServeUri(uri, homeDir);
            }
        }
        return canServeUri;
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20'
     * instead of '+'.
     */
    private String encodeUri(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if ("/".equals(tok)) {
                newUri += "/";
            } else if (" ".equals(tok)) {
                newUri += "%20";
            } else {
                try {
                    newUri += URLEncoder.encode(tok, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        }
        return newUri;
    }

    private String findIndexFileInDirectory(File directory) {
        for (String fileName : SimpleWebServer.INDEX_FILE_NAMES) {
            File indexFile = new File(directory, fileName);
            if (indexFile.isFile()) {
                return fileName;
            }
        }
        return null;
    }

    protected Response getForbiddenResponse(String s) {
        return Response.newFixedLengthResponse(Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: " + s);
    }

    protected Response getInternalErrorResponse(String s) {
        return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERROR: " + s);
    }

    protected Response getNotFoundResponse() {
        return Response.newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
    }

    /**
     * Used to initialize and customize the server.
     */
    public void init() {
    }

    protected String listDirectory(String uri, File f) {
        String heading = "Directory " + uri;
        StringBuilder msg =
                new StringBuilder("<html><head><title>" + heading + "</title><style><!--\n" + "span.dirname { font-weight: bold; }\n" + "span.filesize { font-size: 75%; }\n"
                        + "// -->\n" + "</style>" + "</head><body><h1>" + heading + "</h1>");

        String up = null;
        if (uri.length() > 1) {
            String u = uri.substring(0, uri.length() - 1);
            int slash = u.lastIndexOf('/');
            if (slash >= 0 && slash < u.length()) {
                up = uri.substring(0, slash + 1);
            }
        }

        List<String> files = Arrays.asList(f.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        }));
        Collections.sort(files);
        List<String> directories = Arrays.asList(f.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        }));
        Collections.sort(directories);
        if (up != null || directories.size() + files.size() > 0) {
            msg.append("<ul>");
            if (up != null || directories.size() > 0) {
                msg.append("<section class=\"directories\">");
                if (up != null) {
                    msg.append("<li><a rel=\"directory\" href=\"").append(up).append("\"><span class=\"dirname\">..</span></a></li>");
                }
                for (String directory : directories) {
                    String dir = directory + "/";
                    msg.append("<li><a rel=\"directory\" href=\"").append(encodeUri(uri + dir)).append("\"><span class=\"dirname\">").append(dir).append("</span></a></li>");
                }
                msg.append("</section>");
            }
            if (files.size() > 0) {
                msg.append("<section class=\"files\">");
                for (String file : files) {
                    msg.append("<li><a href=\"").append(encodeUri(uri + file)).append("\"><span class=\"filename\">").append(file).append("</span></a>");
                    File curFile = new File(f, file);
                    long len = curFile.length();
                    msg.append("&nbsp;<span class=\"filesize\">(");
                    if (len < 1024) {
                        msg.append(len).append(" bytes");
                    } else if (len < 1024 * 1024) {
                        msg.append(len / 1024).append(".").append(len % 1024 / 10 % 100).append(" KB");
                    } else {
                        msg.append(len / (1024 * 1024)).append(".").append(len % (1024 * 1024) / 10000 % 100).append(" MB");
                    }
                    msg.append(")</span></li>");
                }
                msg.append("</section>");
            }
            msg.append("</ul>");
        }
        msg.append("</body></html>");
        return msg.toString();
    }

    public static Response newFixedLengthResponse(IStatus status, String mimeType, String message) {
        Response response = Response.newFixedLengthResponse(status, mimeType, message);
        response.addHeader("Accept-Ranges", "bytes");
        return response;
    }

    private Response respond(Map<String, String> headers, IHTTPSession session, String uri) {
        // First let's handle CORS OPTION query
        Response r;
        if (cors != null && Method.OPTIONS.equals(session.getMethod())) {
            r = Response.newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, null, 0);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                r = defaultRespondForAboveAndroidTen(headers, session, uri);
            }else {
                r = defaultRespond(headers, session, uri);
            }
        }

        if (cors != null) {
            r = addCORSHeaders(headers, r, cors);
        }
        return r;
    }
    private Response defaultRespondForAboveAndroidTen(Map<String, String> headers, IHTTPSession session, String uri) {
        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        // Prohibit getting out of current directory
        if (uri.contains("../")) {
            return getForbiddenResponse("Won't serve ../ for security reasons.");
        }


        File f = new File(uri);

        if (!f.exists()) {
            return getNotFoundResponse();
        }

        // Browsers get confused without '/' after the directory, send a
        // redirect.


        MeshLog.i("File-Key url:" + uri + " File path: " + f.getAbsolutePath());
        if (f.isDirectory() && !uri.endsWith("/")) {
            uri += "/";
            Response res = newFixedLengthResponse(Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
            res.addHeader("Location", uri);
            return res;
        }

        if (f.isDirectory()) {
            // First look for index files (index.html, index.htm, etc) and if
            // none found, list the directory if readable.
            String indexFile = findIndexFileInDirectory(f);
            if (indexFile == null) {
                if (f.canRead()) {
                    // No index file, list the directory if it is readable
                    return newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, listDirectory(uri, f));
                } else {
                    return getForbiddenResponse("No directory listing.");
                }
            } else {
                return respond(headers, session, uri + indexFile);
            }
        }

        //File response here - Azim
        String mimeTypeForFile = getMimeTypeForFile(uri);
        Response response = null;
        int resumeFrom = 0;
        String resumeFrm = session.getHeaders().get(Const.FileRequest.RESUME_FROM);
        if (Text.isNotEmpty(resumeFrm)) {
            resumeFrom = Integer.parseInt(resumeFrm);
        }
        response = serveFile(uri, headers, f, mimeTypeForFile, resumeFrom,
                session.getRemoteIpAddress());
        return response != null ? response : getNotFoundResponse();
    }
    private Response defaultRespond(Map<String, String> headers, IHTTPSession session, String uri) {
        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        // Prohibit getting out of current directory
        if (uri.contains("../")) {
            return getForbiddenResponse("Won't serve ../ for security reasons.");
        }

        boolean canServeUri = false;
        File homeDir = null;
        for (int i = 0; !canServeUri && i < this.rootDirs.size(); i++) {
            homeDir = this.rootDirs.get(i);
            canServeUri = canServeUri(uri, homeDir);
        }
        if (!canServeUri) {
            return getNotFoundResponse();
        }

        // Browsers get confused without '/' after the directory, send a
        // redirect.
        File f = new File(homeDir,uri);

        MeshLog.i("File-Key url:" + uri + " File path: " + f.getAbsolutePath() + " home :" + homeDir);
        if (f.isDirectory() && !uri.endsWith("/")) {
            uri += "/";
            Response res = newFixedLengthResponse(Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
            res.addHeader("Location", uri);
            return res;
        }

        if (f.isDirectory()) {
            // First look for index files (index.html, index.htm, etc) and if
            // none found, list the directory if readable.
            String indexFile = findIndexFileInDirectory(f);
            if (indexFile == null) {
                if (f.canRead()) {
                    // No index file, list the directory if it is readable
                    return newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, listDirectory(uri, f));
                } else {
                    return getForbiddenResponse("No directory listing.");
                }
            } else {
                return respond(headers, session, uri + indexFile);
            }
        }

        //File response here - Azim
        String mimeTypeForFile = getMimeTypeForFile(uri);
        WebServerPlugin plugin = SimpleWebServer.mimeTypeHandlers.get(mimeTypeForFile);
        Response response = null;
        if (plugin != null && plugin.canServeUri(uri, homeDir)) {
            response = plugin.serveFile(uri, headers, session, f, mimeTypeForFile);
            if (response != null && response instanceof InternalRewrite) {
                InternalRewrite rewrite = (InternalRewrite) response;
                return respond(rewrite.getHeaders(), session, rewrite.getUri());
            }
        } else {
            int resumeFrom = 0;
            String resumeFrm = session.getHeaders().get(Const.FileRequest.RESUME_FROM);
            if (Text.isNotEmpty(resumeFrm)) {
                resumeFrom = Integer.parseInt(resumeFrm);
            }
            response = serveFile(uri, headers, f, mimeTypeForFile, resumeFrom,
                    session.getRemoteIpAddress());
        }
        return response != null ? response : getNotFoundResponse();
    }

    @Override
    public Response serve(IHTTPSession session) {
        MeshLog.i("Data received in SimpleWebServer");
        MeshLog.v("FILE_SPEED_TEST_5 " + Calendar.getInstance().getTime());
        if (super.serve(session) == null) {
            //Text server could not process the message
            String uri = session.getUri();
            switch (uri) {
                case URI_FILE:
                    String path = session.getHeaders().get(FILE_PATH_HEADER_KEY);
                    if (Text.isNotEmpty(path)) {
                        Map<String, String> header = session.getHeaders();
                        Map<String, String> parms = session.getParms();

                        if (!this.quiet) {
                            System.out.println(session.getMethod() + " '" + uri + "' ");

                            Iterator<String> e = header.keySet().iterator();
                            while (e.hasNext()) {
                                String value = e.next();
                                System.out.println("  HDR: '" + value + "' = '" + header.get(value) + "'");
                            }
                            e = parms.keySet().iterator();
                            while (e.hasNext()) {
                                String value = e.next();
                                System.out.println("  PRM: '" + value + "' = '" + parms.get(value) + "'");
                            }
                        }

                        for (File homeDir : this.rootDirs) {
                            // Make sure we won't die of an exception later
                            if (!homeDir.isDirectory()) {
                                return getInternalErrorResponse("given path is not a directory (" + homeDir + ").");
                            }
                        }
                        return respond(Collections.unmodifiableMap(header), session, path);
                    }
                    break;
                default:
                    break;
            }
        } else {

            return newFixedLengthResponse(Status.OK, MIME_PLAINTEXT,
                    " " + "Ok");
        }

        return newFixedLengthResponse(Status.BAD_REQUEST, MIME_PLAINTEXT,
                "URL not supported");
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    Response serveFile(String uri, Map<String, String> header, File file, String mime,
                       int resumeFrom, String sourceIp) {
        Response res;
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + ""
                    + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // get if-range header. If present, it must match etag or else we
            // should ignore the range request
            String ifRange = header.get("if-range");
            boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));

            String ifNoneMatch = header.get("if-none-match");
            boolean headerIfNoneMatchPresentAndMatching = ifNoneMatch != null && ("*".equals(ifNoneMatch) || ifNoneMatch.equals(etag));

            // Change return code and add Content-Range header when skipping is
            // requested
            long fileLen = file.length();

            if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
                // range request that matches current etag
                // and the startFrom of the range is satisfiable
                if (headerIfNoneMatchPresentAndMatching) {
                    // range request that matches current etag
                    // and the startFrom of the range is satisfiable
                    // would return range from file
                    // respond with not-modified
                    res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    FileInputStream fis = new FileInputStream(file);
                    fis.skip(startFrom);

                    res = Response.newFixedLengthResponse(Status.PARTIAL_CONTENT, mime, fis, newLen);
                    res.addHeader("Accept-Ranges", "bytes");
                    res.addHeader("Content-Length", "" + newLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {

                if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                    // return the size of the file
                    // 4xx responses are not trumped by if-none-match
                    res = newFixedLengthResponse(Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes */" + fileLen);
                    res.addHeader("ETag", etag);
                } else if (range == null && headerIfNoneMatchPresentAndMatching) {
                    // full-file-fetch request
                    // would return entire file
                    // respond with not-modified
                    res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
                    // range request that doesn't match current etag
                    // would return entire (different) file
                    // respond with not-modified

                    res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    // supply the file
                    String fileKey = header.get(FILE_MAP_HEADER_KEY);
                    res = newFixedFileResponse(file, mime, resumeFrom, sourceIp, fileKey);
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = getForbiddenResponse("Reading file failed.");
        }

        return res;
    }

    private Response newFixedFileResponse(File file, String mime, int seekTo, String requesterIp, String fileKey)
            throws IOException {
        Response res;

        InputStream fileInputStream;
        if (seekTo > 0) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            FileChannel fileChannel = randomAccessFile.getChannel();

            //Stream automatically closes the channel
            fileInputStream = Channels.newInputStream(
                    fileChannel.position(seekTo));
        } else {
            MeshLog.e("File input stream  attempt : ");
            fileInputStream = new FileInputStream(file);
            MeshLog.e("File input stream success : "+fileInputStream.available());
        }

        res = getResponse(Status.OK, mime, fileInputStream,
                (int) file.length(), file.getAbsolutePath(), requesterIp, fileKey);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    protected Response getResponse(IStatus iStatus, String mimeTypes, InputStream inputStream,
                                   long totalBytes, String filePath, String ip, String fileKey) {
        return Response.newFixedLengthResponse(iStatus, mimeTypes, inputStream, totalBytes);
    }

    protected Response addCORSHeaders(Map<String, String> queryHeaders, Response resp, String cors) {
        resp.addHeader("Access-Control-Allow-Origin", cors);
        resp.addHeader("Access-Control-Allow-Headers", calculateAllowHeaders(queryHeaders));
        resp.addHeader("Access-Control-Allow-Credentials", "true");
        resp.addHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        resp.addHeader("Access-Control-Max-Age", "" + MAX_AGE);

        return resp;
    }

    private String calculateAllowHeaders(Map<String, String> queryHeaders) {
        // here we should use the given asked headers
        // but NanoHttpd uses a Map whereas it is possible for requester to send
        // several time the same header
        // let's just use default values for this version
        return System.getProperty(ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME, DEFAULT_ALLOWED_HEADERS);
    }

    private final static String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS, HEAD";

    private final static int MAX_AGE = 42 * 60 * 60;

    // explicitly relax visibility to package for tests purposes
    public final static String DEFAULT_ALLOWED_HEADERS = "origin,accept,content-type";

    public final static String ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME = "AccessControlAllowHeader";
}