package org.projectrainbow;

import com.google.common.base.Objects;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Properties;

public class RainbowLauncher {

    public static void main(String[] args) throws Exception {
        File rainbowJar = new File("Rainbow.jar");
        downloadFile("https://ci.codecrafter47.de/job/Rainbow/lastSuccessfulBuild/artifact/rainbow/target/Rainbow.jar", rainbowJar);
        addURL(rainbowJar.toURI().toURL());

        File propertiesFile = new File("RainbowLauncher.properties");

        Properties properties = new Properties();

        if (propertiesFile.exists()) {
            properties.load(new FileInputStream(propertiesFile));
        } else {
            properties.put("PluginBukkitBridge", "false");
            properties.put("MultiWorld", "false");
            properties.store(new FileOutputStream(propertiesFile), null);
        }

        if (Boolean.parseBoolean(properties.getProperty("PluginBukkitBridge", "false"))) {
            File pluginFolder = getPluginFolder();
            downloadFile("https://ci.codecrafter47.de/job/PluginBukkitBridge/lastSuccessfulBuild/artifact/target/PluginBukkitBridge.jar", new File(pluginFolder, "PluginBukkitBridge.jar"));
        }

        if (Boolean.parseBoolean(properties.getProperty("MultiWorld", "false"))) {
            File pluginFolder = getPluginFolder();
            downloadFile("https://ci.codecrafter47.de/job/MultiWorld/lastSuccessfulBuild/artifact/target/MultiWorld.jar", new File(pluginFolder, "MultiWorld.jar"));
        }

        Class.forName("org.projectrainbow.launch.Bootstrap")
                .getMethod("main", String[].class)
                .invoke(null, (Object) args);
    }

    private static File getPluginFolder() {
        File pluginFolder = new File("plugins_mod");
        if (!pluginFolder.exists()) {
            if (!pluginFolder.mkdir()) {
                throw new RuntimeException("Failed to create plugin folder.");
            }
        }
        return pluginFolder;
    }

    private static void downloadFile(String source, File targetFile) throws IOException {
        System.out.printf("Checking whether %s is up to date.\n", targetFile.getName());
        if (targetFile.exists()) {
            // check sha1sum
            String sha1sum = "";
            try (InputStream inputStream = new URL(source + ".sha1").openStream()) {
                sha1sum = IOUtils.toString(inputStream).split(" ")[0];
                if (Objects.equal(sha1sum, sha1(targetFile))) {
                    System.out.printf("%s is already up to date.\n", targetFile.getName());
                    return;
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        if (targetFile.exists()) {
            targetFile.delete();
        }
        System.out.printf("Downloading %s. Please wait...\n", targetFile.getName());
        InputStream in = new URL(source).openStream();
        OutputStream out = new FileOutputStream(targetFile);
        ByteStreams.copy(in, out);
        in.close();
        out.close();
    }

    private static void addURL(URL url) {
        URLClassLoader classLoader = (URLClassLoader) RainbowLauncher.class.getClassLoader();

        try {
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, url);
        } catch (Throwable t) {
            throw new RuntimeException("Error, could not add URL to system classloader", t);
        }
    }

    private static String sha1(final File file) throws NoSuchAlgorithmException, IOException {
        final MessageDigest sha1 = MessageDigest.getInstance("SHA1");

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            final byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                sha1.update(buffer, 0, read);
            }
        }

        // Convert the byte to hex format
        try (Formatter formatter = new Formatter()) {
            for (final byte b : sha1.digest()) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}
