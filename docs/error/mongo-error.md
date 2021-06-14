> 该问题常见于 Windows 机器，是由于文件编码格式导致脚本无法启动



**该问题可以通过以下方式解决：**

设置该文件的编码格式和行尾序列 ，文件位置：`iot-dc3/dc3/dependences/mongo/launch.sh` 

- 调整编码格式为 UTF-8

- 调整行尾序列为 LF

如图，使用 IDEA 或者 VS Code 均可设置，一般在编辑器的右下角区域：

 ![mongo-error](../images/dc3/error/mongo-error.png)