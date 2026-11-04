import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.security.*;
import java.util.zip.*;
import java.lang.reflect.*;
import org.json.*;
import com.resonance.player.model.*;
import com.resonance.player.sync.PlainSyncPackage;
import com.resonance.player.features.JvmMusicAssistant;
import com.resonance.player.playback.ListeningStatsStore;

public class AuditProbe {
    static JSONArray results = new JSONArray();
    static void result(String id, Object evidence) { results.put(new JSONObject().put("id", id).put("evidence", evidence)); }
    static Track track(String id, String title, String duration, Path source) {
        return new Track(id, title, "Test Artist", "Test Album", duration, 0, false, source.toString(), null, "audio/mpeg");
    }
    static Object call(Object target, String name, Class<?>[] types, Object... values) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, types); m.setAccessible(true); return m.invoke(target, values);
    }
    static Object store(Path path) throws Exception {
        Constructor<?> c = Class.forName("com.resonance.player.platform.DesktopLibraryStore").getDeclaredConstructor(Path.class);
        c.setAccessible(true); return c.newInstance(path);
    }
    static Properties manifest(byte[] audio) throws Exception {
        Properties p = new Properties();
        p.setProperty("format", "resonance-sync-1"); p.setProperty("track.count", "1");
        for (String key : List.of("id", "title", "artist", "album")) p.setProperty("track.0." + key, "victim");
        p.setProperty("track.0.audio", "audio/victim.mp3");
        p.setProperty("track.0.sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(audio)));
        return p;
    }
    static Path archive(Path target, Properties p, byte[] audio) throws Exception {
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(target))) {
            z.putNextEntry(new ZipEntry("audio/victim.mp3")); z.write(audio); z.closeEntry();
            z.putNextEntry(new ZipEntry("manifest.properties")); p.store(z, "Generated audit fixture"); z.closeEntry();
        }
        return target;
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("resonance-audit-");
        try {
            byte[] incoming = "synthetic replacement".getBytes();
            Path managed = Files.createDirectories(root.resolve("managed"));
            String victimName = HexFormat.of().formatHex(Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest("victim".getBytes()), 12));
            Path victim = managed.resolve(victimName + ".mp3"); Files.writeString(victim, "synthetic original");
            Properties p = manifest(incoming); p.setProperty("playlist.count", "1"); p.setProperty("playlist.0.id", "broken");
            String error = "none";
            try { PlainSyncPackage.class.getMethod("import", Path.class, Path.class).invoke(new PlainSyncPackage(), archive(root.resolve("invalid.resonance"), p, incoming), managed); }
            catch (InvocationTargetException e) { error = e.getCause().getMessage(); }
            result("B01_import_commits_before_playlist_validation", new JSONObject().put("error", error).put("fileAfterFailure", Files.readString(victim)));

            Path data = Files.createDirectories(root.resolve("data")); Object s = store(data);
            Path missing = root.resolve("temporarily-unavailable.mp3"); Files.writeString(missing, "generated");
            Path present = root.resolve("present.mp3"); Files.writeString(present, "generated");
            call(s, "save", new Class<?>[]{List.class}, List.of(track("a", "Unavailable", "3:00", missing), track("b", "Available", "3:00", present)));
            Files.delete(missing);
            result("B02_missing_library_visible", ((List<?>)call(s, "load", new Class<?>[]{})).size());
            call(s, "setFavorite", new Class<?>[]{String.class, boolean.class}, "b", true);
            Files.writeString(missing, "generated");
            result("B02_library_after_restore", ((List<?>)call(s, "load", new Class<?>[]{})).size());

            call(s, "writeUiValue", new Class<?>[]{String.class, String.class}, "app.themeMode", "Dark");
            call(s, "writeUiValue", new Class<?>[]{String.class, String.class}, "app.themeMode", "Light");
            Files.writeString(data.resolve("ui-state.properties"), "broken=\\uZZZZ");
            try { call(s, "loadThemeMode", new Class<?>[]{}); result("B03_theme_backup_bypass", "no failure"); }
            catch (InvocationTargetException e) { result("B03_theme_backup_bypass", e.getCause().getClass().getSimpleName()); }
            result("B03_generic_reader_recovers", call(s, "readUiValue", new Class<?>[]{String.class}, "app.themeMode"));

            JSONObject deletion = new JSONObject();
            for (String text : List.of("如何删除歌曲？", "解释一下删除音频有什么风险", "只删除歌单里的歌曲，不删除音频文件", "帮我删除这首歌的文件"))
                deletion.put(text, JvmMusicAssistant.Companion.allowsAudioDeletion(text));
            result("B04_deletion_authorization", deletion);

            Path stats = Files.createDirectories(root.resolve("stats")); Files.writeString(stats.resolve("listening-stats.json"), "{broken");
            try { new ListeningStatsStore(stats, 0); result("B05_corrupt_stats", "no failure"); }
            catch (Exception e) { result("B05_corrupt_stats", e.getClass().getSimpleName()); }

            var audit = SmartLibraryAuditEngine.INSTANCE.audit(List.of(track("studio", "Test Song", "3:00", present), track("live", "Test Song (Live)", "8:00", missing)));
            result("B06_distinct_recordings_marked_duplicates", new JSONObject().put("groups", audit.getDuplicateGroups().size()).put("selectedToClean", new JSONArray(audit.getDuplicateGroups().get(0).getSelectedToCleanIds())));

            var intent = SemanticSearchIntentParser.INSTANCE.parse("140 BPM");
            result("B08_bpm_only_intent", new JSONObject().put("bpmRange", String.valueOf(intent.getBpmRange())).put("keywords", new JSONArray(intent.getKeywords())));

            Properties listP = new Properties(); listP.setProperty("format", "resonance-sync-1"); listP.setProperty("track.count", "0"); listP.setProperty("playlist.count", "1"); listP.setProperty("playlist.0.id", "one"); listP.setProperty("playlist.0.name", "fixture");
            listP.setProperty("playlist.0.track.count", "1000000");
            long started = System.nanoTime(); Object lists = PlainSyncPackage.class.getMethod("import", Path.class, Path.class).invoke(new PlainSyncPackage(), archive(root.resolve("large-count.resonance"), listP, incoming), root.resolve("count-managed"));
            result("B09_unbounded_playlist_reference_count", new JSONObject().put("acceptedReferences", 1000000).put("durationMs", (System.nanoTime()-started)/1000000));
            System.out.println(results.toString(2));
        } finally {
            if (!root.getFileName().toString().startsWith("resonance-audit-")) throw new IllegalStateException();
            try (var walk = Files.walk(root)) { for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
        }
    }
}
