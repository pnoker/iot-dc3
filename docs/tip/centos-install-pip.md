## 检测是否安装 pip

```bash
pip --version
```



## 安装 pip

```bash
yum -y install epel-release
yum install -y python-pip
```



## 配置国内源

```bash
# 创建 .pip
mkdir ~/.pip

# 编辑 ~/.pip/pip.conf
vim ~/.pip/pip.conf

# 添加以下内容
[global]
index-url = http://mirrors.aliyun.com/pypi/simple/

[install]
trusted-host=mirrors.aliyun.com
```



## 升级 pip 建议不要升级

```bash
pip install --upgrade pip
```



## 卸载 pip

```bash
yum remove -y python-pip
python -m pip uninstall pip
```