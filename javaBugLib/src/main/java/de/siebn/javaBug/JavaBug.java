package de.siebn.javaBug;

import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sieben on 04.03.2015.
 */
public class JavaBug extends NanoHTTPD {
    private final ObjectBugPlugin objectBugPlugin = new ObjectBugPlugin(this);
    private final ArrayList<Server> servers = new ArrayList<>();
    private final ArrayList<BugPlugin> plugins = new ArrayList<>();
    private final HashMap<Class<?>, ArrayList<?>> filteredPlugins = new HashMap<>();
    private final int port;

    public interface Server {
        public boolean responsible(IHTTPSession session);
        public Response serve(IHTTPSession session);
        public int getPriority();
    }

    public interface BugPlugin {
        public int getOrder();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Serve {
        String value();
        int priority() default 0;
    }

    public JavaBug(int port) {
        super(port);
        this.port = port;
    }

    public static class ExceptionResult extends RuntimeException {
        private final Response.Status status;

        public ExceptionResult(Response.Status status, String message) {
            super(message);
            this.status = status;
        }
    }

    public void addPlugin(BugPlugin plugin) {
        plugins.add(plugin);
        Collections.sort(plugins, new Comparator<BugPlugin>() {
            @Override public int compare(BugPlugin o1, BugPlugin o2) { return o1.getOrder() - o2.getOrder(); }
        });
        addAnnotatedMethods(plugin);
    }

    public <T> List<T> getPlugins(Class<T> pluginClass) {
        @SuppressWarnings("unchecked") ArrayList<T> filtered = (ArrayList<T>) filteredPlugins.get(pluginClass);
        if (filtered == null) {
            filteredPlugins.put(pluginClass, filtered = new ArrayList<>());
            for (BugPlugin plugin : plugins)
                if (pluginClass.isAssignableFrom(plugin.getClass()))
                    filtered.add((T) plugin);
        }
        return filtered;
    }

    private void addAnnotatedMethods(final Object object) {
        for (final java.lang.reflect.Method method : object.getClass().getMethods()) {
            final Serve serve = method.getAnnotation(Serve.class);
            if (serve != null) {
                final Pattern pattern = Pattern.compile(serve.value());
                addServers(new Server() {
                    @Override
                    public boolean responsible(IHTTPSession session) {
                        return pattern.matcher(session.getUri()).matches();
                    }

                    @Override
                    public Response serve(IHTTPSession session) {
                        try {
                            Class<?>[] types = method.getParameterTypes();
                            Object param[] = new Object[types.length];
                            for (int i = 0; i < param.length; i++) {
                                if (IHTTPSession.class.isAssignableFrom(types[i])) param[i] = session;
                                else if (String.class.isAssignableFrom(types[i])) param[i] = session.getUri();
                                else if (String[].class.isAssignableFrom(types[i])) {
                                    Matcher matcher = pattern.matcher(session.getUri());
                                    matcher.matches();
                                    String[] params = (String[]) (param[i] = new String[matcher.groupCount() + 1]);
                                    for (int j = 0; j < params.length; j++) params[j] = matcher.group(j);
                                }
                            }
                            Object r = method.invoke(object, param);
                            if (r instanceof Response) return (Response) r;
                            if (r instanceof XML) return new Response(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, ((XML) r).getXml());
                            if (r instanceof byte[]) return new Response(Response.Status.OK, "application/octet-stream", new ByteArrayInputStream((byte[]) r));
                            if (r instanceof InputStream) return new Response(Response.Status.OK, "application/octet-stream", (InputStream) r);
                            return new Response(r.toString());
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        } catch (InvocationTargetException e) {
                            if (e.getCause() instanceof RuntimeException)
                                throw (RuntimeException) e.getCause();
                            throw new IllegalStateException(e);
                        }
                    }

                    @Override
                    public int getPriority() {
                        return serve.priority();
                    }
                });
            }
        }
    }

    public void addServers(Server server) {
        servers.add(server);
//        Collections.sort(servers, new Comparator<Server>() {
//            @Override
//            public int compare(Server o1, Server o2) {
//                return o2.getOrder() - o1.getOrder();
//            }
//        });
    }

    @Override
    public Response serve(IHTTPSession session) {
        long start = System.nanoTime();
        try {
            for (Server server : servers)
                if (server.responsible(session))
                    return server.serve(session);
            return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404");
        } catch (ExceptionResult e) {
            return new Response(e.status, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        } finally {
            System.out.println("Serving: " + session.getUri() + " in: " + StringifierUtil.nanoSecondsToString(System.nanoTime() - start));
        }
    }

    public boolean tryToStart() {
        try {
            start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get IP address from first non-localhost interface
     * @return  address or empty string
     */
    public ArrayList<String> getIPAddresses(boolean includeLoopback) {
        ArrayList<String> adresses = new ArrayList<>();
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (includeLoopback || !addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase().split("%")[0];
                        if (sAddr.contains(":")) sAddr = "[" + sAddr + "]";
                        sAddr = "http://" + sAddr + ":" + port;
                        adresses.add(sAddr);
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        Collections.sort(adresses);
        return adresses;
    }

    public ObjectBugPlugin getObjectBug() {
        return objectBugPlugin;
    }
}
