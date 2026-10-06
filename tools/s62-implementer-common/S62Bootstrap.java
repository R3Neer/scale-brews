import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

/** Fresh common class loader with explicit client-class veto. */
public final class S62Bootstrap {
    public static void main(String[] args) throws Exception {
        var urls = new ArrayList<URL>(); urls.add(Path.of(args[1]).toUri().toURL());
        for (var part : Files.readString(Path.of(args[0])).trim().split(Pattern.quote(java.io.File.pathSeparator)))
            urls.add(Path.of(part).toUri().toURL());
        try (var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("io.github.r3neer.scalebrews.client."))
                    throw new ClassNotFoundException("S62 client veto: " + name);
                return super.loadClass(name, resolve);
            }
        }) {
            loader.loadClass("S62Common").getMethod("main", String[].class).invoke(null, (Object)new String[]{});
        }
    }
}
