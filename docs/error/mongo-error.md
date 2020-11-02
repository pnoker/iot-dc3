> 该问题常见于 Windows 机器，是由于文件编码格式导致脚本无法启动


需要设置一下这个文件的编码格式和行尾序列 

文件位置：`iot-dc3/dc3/dependences/mongo/launch.sh` 

- 调整编码格式为 UTF-8

- 调整行尾序列为 LF

如图：

 ![mongo-error](../images/dc3/error/mongo-error.png)