package windclan.scrobbler;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class LogParser {
    public static ArrayList<Scrobble> parseLog(String logPath, String deviceRoot) {
        File logFile = new File(logPath);
        if (logFile.exists() && logFile.canRead()) {
            try {
                ArrayList<Scrobble> list = new ArrayList<Scrobble>();
                Scanner scan = new Scanner(logFile);
                while (scan.hasNextLine()) {
                    String curLine = scan.nextLine();
                    if (!curLine.startsWith("#") && !curLine.isEmpty()) {
                        String[] splitStr = curLine.split(":");
                        int timestamp = Integer.parseInt(splitStr[0]);
                        int elapsed = Integer.parseInt(splitStr[1]);
                        int length = Integer.parseInt(splitStr[2]);
                        if (length >= 30*1000 && (elapsed >= (length/2) || elapsed >= 4*60*1000)) {
                            String path = deviceRoot+splitStr[3].replaceAll("\\/<(.*)>\\/","");
                            File musicFileFile = new File(path);
                            if (musicFileFile.exists() && musicFileFile.canRead()) {
                                AudioFile musicFile = AudioFileIO.read(musicFileFile);
                                Tag meta = musicFile.getTag();
                                if (meta.hasField(FieldKey.TITLE) && meta.hasField(FieldKey.ARTIST)) {
                                    Scrobble curScrobble = new Scrobble(meta.getFirst(FieldKey.ARTIST),meta.getFirst(FieldKey.TITLE),timestamp,(int)Math.round(length/1000.0),elapsed/1000.0);
                                    list.add(curScrobble);
                                }
                            }
                        }
                    }
                }
                scan.close();
                return list;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Cannot open playback.log!");
        }
    }
}
