import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 从 jar/zip 中提取指定条目到输出目录。用法: java Extract.java <jar> <outDir> <entry> [entry...] */
public class Extract {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("usage: java Extract.java <jar> <outDir> <entry> [entry...]");
            return;
        }
        File jar = new File(args[0]);
        Path outDir = Path.of(args[1]);
        try (ZipFile zip = new ZipFile(jar)) {
            for (int i = 2; i < args.length; i++) {
                String name = args[i];
                ZipEntry entry = zip.getEntry(name);
                if (entry == null) {
                    System.out.println("MISSING: " + name);
                    continue;
                }
                Path out = outDir.resolve(name);
                Files.createDirectories(out.getParent());
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("extracted: " + out + " (" + entry.getSize() + " bytes)");
            }
        }
    }
}
