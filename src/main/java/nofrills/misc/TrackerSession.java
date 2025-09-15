package nofrills.misc;

import nofrills.Main;
import nofrills.features.mining.SackTracker;

import java.util.concurrent.TimeUnit;

public class TrackerSession {

    String trackedCollection;
    boolean isActive;
    boolean isPaused;
    long sessionStartNanos;
    long lastSackMessageNanos;
    int totalItemsGained;
    int trackedSeconds;

    long pausedNanos;
    long resumedNanos;

    public TrackerSession(String trackedColl) {
        this.trackedCollection = trackedColl;
        this.isActive = false;
        this.sessionStartNanos = 0L;
        this.lastSackMessageNanos = 0L;
        this.totalItemsGained = 0;
        this.trackedSeconds = 0;
    }

    public void start(){
        this.isActive = true;
        long now = System.nanoTime();
        this.sessionStartNanos = now;
        this.lastSackMessageNanos = now;
        this.totalItemsGained = 0;
        this.trackedSeconds = 0;
    }

    public void start(int addedSeconds){
        this.isActive = true;
        long now = System.nanoTime();
        this.sessionStartNanos = now - TimeUnit.NANOSECONDS.convert(addedSeconds, TimeUnit.SECONDS);
        this.lastSackMessageNanos = now;
        this.totalItemsGained = 0;
        this.trackedSeconds = 0;
    }

    public void pause(){
        this.isPaused = true;
        this.pausedNanos = System.nanoTime();
    }

    public void resume(){
        this.isPaused = false;
        this.resumedNanos = System.nanoTime();
        long nanoDiff = this.resumedNanos-this.pausedNanos;
        this.sessionStartNanos = sessionStartNanos+nanoDiff;
        this.lastSackMessageNanos = lastSackMessageNanos+nanoDiff;
    }

    public void stop(){
        this.isActive = false;
        Main.session = null;
        SackTracker.clearLines();
    }

    public void increaseTotalItems(int gain){
        this.totalItemsGained += gain;
    }

    public void increaseTrackedSeconds(int gain){
        this.trackedSeconds += gain;
    }

    public void updateSackMessage(){
        this.lastSackMessageNanos = System.nanoTime();
    }

    public String getTrackedCollection(){
        return trackedCollection;
    }

    public boolean isActive(){
        return isActive;
    }

    public boolean isPaused(){
        return isPaused;
    }

    public long getLastSackMessageNanos(){
        return lastSackMessageNanos;
    }

    public int getTotalItemsGained() {
        return totalItemsGained;
    }

    public long getElapsedSeconds(){
        if(!isActive || sessionStartNanos == 0L) return 0L;
        long now = System.nanoTime();
        return TimeUnit.NANOSECONDS.toSeconds(now - sessionStartNanos);
    }

    public int getCollectionPerHour(){
        double hours = getElapsedSeconds() / 3600f;
        if(hours == 0) return 0;
        return (int) (totalItemsGained / hours);
    }

    public float getUptime(){
        return (float) trackedSeconds / getElapsedSeconds();
    }
}
