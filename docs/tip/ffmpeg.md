### 视频剪切

> - `-ss`  开始时间
> - `-t`  持续时间



> 1.截取从 `00:00:00` 开始的 30 秒视频

```bash
ffmpeg -ss 00:00:00 -t 00:00:30 -i keyoutput.mp4 -vcodec copy -acodec copy split.mp4
```
> 2.截取从 `00:00:30` 开始的 30 秒视频
```bash
ffmpeg -ss 00:00:30 -t 00:00:30 -i keyoutput.mp4 -vcodec copy -acodec copy split1.mp4
```



### 视频合并

> 视频合并，其中 list.txt 是待合并的视频列表，使用换行进行分隔

```bash
ffmpeg -f concat -i list.txt -c copy concat.mp4
```



### 视频转码

> - `-i`  后面是输入文件名
>
> - `-vcodec`  后面是编码格式，h264 最佳

```bash
ffmpeg -i out.ogv -vcodec h264 out.mp4
```



### 视频添加水印

> main_w-overlay_w-10 视频的宽度-水印的宽度-水印边距；

```bash
ffmpeg -i input.mp4 -i logo.jpg -filter_complex [0:v][1:v]overlay=main_w-overlay_w-10:main_h-overlay_h-10[out] -map [out] -map 0:a -codec:a copy output.mp4
```



### 提取视频中的音频

> - `-vn`  去掉视频
>
> - `-acodec`  音频选项， 一般后面加 `copy` 表示拷贝

```bash
ffmpeg -i input.mp4 -acodec copy -vn output.mp3
```



### 音视频合成

> -y 覆盖输出文件

```bash
ffmpeg -y –i input.mp4 –i input.mp3 –vcodec copy –acodec copy output.mp4
```



### 去掉视频中的音频

> - `-an`  去掉音频
>
> - `-vcodec`  视频选项，一般后面加 `copy` 表示拷贝

```bash
ffmpeg -i input.mp4 -vcodec copy -an output.mp4
```




### 其他

```bash
// 视频分解为图片
ffmpeg –i test.mp4 –r 1 –f image2 image-%3d.jpg
// -r 指定截屏频率

// 将视频转为gif
ffmpeg -i input.mp4 -ss 0:0:30 -t 10 -s 320x240 -pix_fmt rgb24 output.gif
// -pix_fmt 指定编码

// 将视频前30帧转为gif
ffmpeg -i input.mp4 -vframes 30 -f gif output.gif

// 旋转视频
ffmpeg -i input.mp4 -vf rotate=PI/2 output.mp4

// 缩放视频
ffmpeg -i input.mp4 -vf scale=iw/2:-1 output.mp4
// iw 是输入的宽度， iw/2就是一半;-1 为保持宽高比

//视频变速
ffmpeg -i input.mp4 -filter:v setpts=0.5*PTS output.mp4

//音频变速
ffmpeg -i input.mp3 -filter:a atempo=2.0 output.mp3

//音视频同时变速，但是音视频为互倒关系
ffmpeg -i input.mp4 -filter_complex "[0:v]setpts=0.5*PTS[v];[0:a]atempo=2.0[a]" -map "[v]" -map "[a]" output.mp4

// 视频截图
ffmpeg –i test.mp4 –f image2 -t 0.001 -s 320x240 image-%3d.jpg
// -s 设置分辨率; -f 强迫采用格式fmt;

// 将图片合成视频
ffmpeg -f image2 -i image%d.jpg output.mp4
```