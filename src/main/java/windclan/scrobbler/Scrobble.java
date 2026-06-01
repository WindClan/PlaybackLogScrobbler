package windclan.scrobbler;

public class Scrobble {
    private final String artist;
    private final String track;
    private final int timestamp;
    private final int duration;
    private final double elapsed;
    public Scrobble(String artist,String track,int timestamp,int duration,double elapsed) {
        this.artist = artist;
        this.track = track;
        this.timestamp = timestamp;
        this.duration = duration;
        this.elapsed = elapsed;
    }
    public String getArtist() {
        return artist;
    }
    public String getTrack() {
        return track;
    }
    public int getTimestamp() {
        return timestamp;
    }
    public int getDuration() {
        return duration;
    }
    public double getElapsed() {
        return elapsed;
    }

    @Override
    public String toString() {
        return ""+getArtist()+" - "+getTrack()+" (played at "+getTimestamp()+" for "+getDuration()+" seconds)";
    }
}
