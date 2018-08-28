#!/bin/sh

app_path=$(cd $(dirname $0);pwd);
project_name=dc_3
git_url=git@gitlab.com:Pnoker/dc_3.git
version=v`date -d "today" +"%Y-%m-%d-%H%M%S"`
build_dir=build/versions/$version

echo "[*] app path $app_path"
echo "[*] project $project_name"
echo "[*] git url $git_url"
echo "[*] version $version"
echo "[*] build path $build_dir"

cd $app_path
cd ../
mkdir -p $build_dir
cp $app_path/startup.sh $app_path/shutdown.sh $build_dir

git pull origin master
mvn -U clean package
cd webpage
npm run build

find .|egrep "/target/dc3.*[0-9]+.beta\.jar$"|xargs -i cp {} $build_dir;

echo "[*] success"
echo "[*] build path is $build_dir"
