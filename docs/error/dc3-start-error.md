> 导致该问题一般是因为 `dc3-boot-starter` 放在 GitHub Maven 仓库上，国内存在网络无法访问的问题



**该问题可以通过以下方式解决：**

### 方法一

使用 VPN

### 方法二

执行以下指令，手动安装 `dc3-boot-starter` 到本地的 Maven Repository 环境:

```bash
cd dc3/dependencies/maven

mvn install:install-file -Dfile=dc3-boot-starter-1.3.2.SR.jar -DgroupId=com.dc3 -DartifactId=dc3-boot-starter -Dversion=1.3.2.SR -Dpackaging=jar
```

