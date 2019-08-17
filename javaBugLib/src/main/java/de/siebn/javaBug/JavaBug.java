package de.siebn.javaBug;

import de.siebn.javaBug.NanoHTTPD.Response.Status;
import de.siebn.javaBug.objectOut.*;
import de.siebn.javaBug.plugins.*;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sieben on 04.03.2015.
 */
public class JavaBug extends NanoHTTPD {
    private final ObjectBugPlugin objectBugPlugin = new ObjectBugPlugin(this);
    private final FileBugPlugin fileBugPlugin = new FileBugPlugin();
    private final ArrayList<Server> servers = new ArrayList<>();
    private final ArrayList<BugPlugin> plugins = new ArrayList<>();
    private final HashMap<Class<?>, Object> pluginMap = new HashMap<>();
    private final HashMap<Class<?>, ArrayList<?>> filteredPlugins = new HashMap<>();
    private final int port;

    public AsyncRunner invocationRunner;

    public abstract class Server {
        public abstract boolean responsible(IHTTPSession session);
        public abstract Response serve(IHTTPSession session);
        public abstract int getPriority();
    }

    public interface BugPlugin {
        public int getOrder();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Serve {
        String value();
        int priority() default 0;
        String[] requiredParameters() default {};
    }

    public JavaBug(int port) {
        super(port);
        this.port = port;
    }

    public void setInvocationRunner(AsyncRunner invocationRunner) {
        this.invocationRunner = invocationRunner;
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
        pluginMap.put(plugin.getClass(), plugin);
        Collections.sort(plugins, new Comparator<BugPlugin>() {
            @Override public int compare(BugPlugin o1, BugPlugin o2) { return o1.getOrder() - o2.getOrder(); }
        });
        addAnnotatedMethods(plugin);
    }

    public void addDefaultPlugins() {
        fileBugPlugin.addRoot("/", new File(".").getAbsoluteFile());

        addPlugin(new RootBugPlugin(this));
        addPlugin(new ThreadsBugPlugin(this));
        addPlugin(getFileBug());
        addPlugin(getObjectBug());
        addPlugin(new IoBugPlugin(this));

        addPlugin(new ArrayOutput(this));
        addPlugin(new FieldsOutput(this));
        addPlugin(new MethodsOutput(this));
        addPlugin(new StringOutput(this));
        addPlugin(new PojoOutput(this));
        addPlugin(new StackTraceOutput(this));
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

    @SuppressWarnings("unchecked")
    public <T> T getPlugin(Class<T> pluginClass) {
        return (T) pluginMap.get(pluginClass);
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
                            if (session.getMethod() == NanoHTTPD.Method.POST) session.parseBody(null);
                            if (serve.requiredParameters().length > 0) {
                                Map<String, String> params = session.getParms();
                                for (String reqParam : serve.requiredParameters()) {
                                    if (!params.containsKey(reqParam)) throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Missing parameter \"" + reqParam + "\"");
                                }
                            }
                            Object r = invokeSync(object, method, param);
                            if (r instanceof Response) return (Response) r;
                            if (r instanceof XML) return new Response(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, ((XML) r).getXml());
                            if (r instanceof JsonBugBase) return new Response(Response.Status.OK, "application/json", ((JsonBugBase) r).toJson());
                            if (r instanceof byte[]) return new Response(Response.Status.OK, "application/octet-stream", new ByteArrayInputStream((byte[]) r));
                            if (r instanceof InputStream) return new Response(Response.Status.OK, "application/octet-stream", (InputStream) r);
                            return new Response(r.toString());
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        } catch (InvocationTargetException e) {
                            if (e.getCause() instanceof RuntimeException)
                                throw (RuntimeException) e.getCause();
                            throw new IllegalStateException(e);
                        } catch (Throwable e) {
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

    private Object invokeSync(final Object object, final java.lang.reflect.Method method, final Object[] param) throws Throwable {
        AsyncRunner runner = invocationRunner;
        if (runner == null) {
            return method.invoke(object, param);
        }
        final AtomicReference<Object> response = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        runner.exec(new Runnable() {
            @Override
            public void run() {
                synchronized (response) {
                    try {
                        response.set(method.invoke(object, param));
                    } catch (Throwable t) {
                        error.set(t);
                    }
                    response.notifyAll();
                }
            }
        });
        synchronized (response) {
            while (response.get() == null && error.get() == null) {
                try {
                    response.wait();
                } catch (Exception e) {}
            }
        }
        if (error.get() != null) {
            throw error.get();
        }
        return response.get();
    }

    public void addServers(Server server) {
        servers.add(server);
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
        } catch (Throwable t) {
            t.printStackTrace();
            return new Response(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, t.getMessage());
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

    @Override
    protected HTTPSession createHttpSession(OutputStream outputStream, TempFileManager tempFileManager, InputStream inputStream, Socket finalAccept) {
        IoBugPlugin ioBug = getPlugin(IoBugPlugin.class);
        if (ioBug != null) {
            outputStream = ioBug.wrapOutputStream(finalAccept, outputStream);
            inputStream = ioBug.wrapInputStream(finalAccept, inputStream);
            ioBug.setTitle(finalAccept, Thread.currentThread().getName());
        }
        return super.createHttpSession(outputStream, tempFileManager, inputStream, finalAccept);
    }

    public ObjectBugPlugin getObjectBug() {
        return objectBugPlugin;
    }

    public FileBugPlugin getFileBug() {
        return fileBugPlugin;
    }
}
