package com.example.circleMusic;

public class MusicItem implements java.io.Serializable {

    private String title;

    private int duration = 0;
    private String subTitle = "";
    private int imageId = -1;
    private boolean isVertical = false;
    private String path = "";
    private boolean isBig = false;

    public MusicItem(String title, String subTitle, int imageId, boolean isBig, boolean isVertical, String path, int duration) {
        this.title = title;
        this.subTitle = subTitle;
        this.imageId = imageId;
        this.isVertical = isVertical;
        this.isBig = isBig;
        this.duration = duration;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isBig() {
        return isBig;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public int getImageId() {
        return imageId;
    }

    public boolean isVertical() {
        return isVertical;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
