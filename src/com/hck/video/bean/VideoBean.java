package com.hck.video.bean;

import java.io.Serializable;

public class VideoBean implements Serializable{
 private int id;
 private String videoName;
 private String videoPlayUrl;
public int getId() {
	return id;
}
public void setId(int id) {
	this.id = id;
}
public String getVideoName() {
	return videoName;
}
public void setVideoName(String videoName) {
	this.videoName = videoName;
}
public String getVideoPlayUrl() {
	return videoPlayUrl;
}
public void setVideoPlayUrl(String videoPlayUrl) {
	this.videoPlayUrl = videoPlayUrl;
}
 
}
