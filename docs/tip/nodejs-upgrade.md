## 卸载&重新安装nodejs

```bash
//卸载nodejs & npm
yum remove nodejs npm -y
//删除残余文件
rm -rf /usr/local/bin/npm /usr/local/share/man/man1/node.1 /usr/local/lib/dtrace/node.d /home/[homedir]/.npm /home/root/.npm
//安装默认的npm
yum install npm
```



## 升级nodejs

```bash
//安装 n , n是nodejs管理工具
npm install -g n
//安装指定nodejs
n latest
或者
n 10.16.3
//切换nodejs
n
ο  node/10.16.3
//查看版本 
node -v
npm -v
```



## 解决切换失败问题

```bash
vim /etc/profile
vim /etc/bashrc

//添加以下环境变量
export N_PREFIX=/usr/local #node实际安装位置
export PATH=$N_PREFIX/bin:$PATH

//执行source刷新配置
source /etc/profile
source /etc/bashrc
```