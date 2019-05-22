package com.pnoker.rtmp.command;


import lombok.Data;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Data
public class CommandBuilder {
    private String cmd;
    private StringBuilder stringBuilder;

    public CommandBuilder(String path) {
        this.stringBuilder = new StringBuilder(path);
    }

    public CommandBuilder create(String exe) {
        if (null != stringBuilder) {
            stringBuilder.append(exe);
        }
        return this;
    }

    public CommandBuilder add(String cmd) {
        if (null != stringBuilder) {
            stringBuilder.append(" " + cmd);
        }
        return this;
    }

    public CommandBuilder add(String key, String cmd) {
        return add(key).add(cmd);
    }

    public void build() {
        cmd = stringBuilder.toString();
    }

    public static void main(String[] args) {
        CommandBuilder builder = new CommandBuilder("D:/Documents/FFmpeg/bin/");
        builder.create("ffmpeg")
                .add("-i", "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov")
                .add("-vcodec", "copy")
                .add("-acodec", "copy")
                .add("-f", "flv")
                .add("-y", "rtmp://114.116.9.76:1935/rtmp/bigbuckbunny_175k").build();
        System.out.println(builder.getCmd());
        String cmd = "{exe} -i {rtsp_url} -vcodec copy -acodec copy -f flv -y {rtmp_url}";
    }
}
