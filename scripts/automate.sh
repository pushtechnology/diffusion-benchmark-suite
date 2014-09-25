#!/bin/sh
now=$(date +%s)
echo $now
mkdir ./diffusion_server$now
cd ./diffusion_server$now
echo "The present working directory is `pwd`"
wget --output-document=diffusion_version.xml "http://gandalf.pushtechnology.com/job/Grand_Unified_Build/lastStableBuild/injectedEnvVars/api/xml?xpath=/envInjectVarList/envMap/DIFFUSION_VERSION/text()"
version=$(cat diffusion_version.xml)
echo $version
rm Diffusion$version.jar
wget "http://gandalf.pushtechnology.com/job/Grand_Unified_Build/lastStableBuild/artifact/Diffusion/Main/build/$version/release/Diffusion$version.jar"
unzip ./Diffusion$version.jar
cd ..
echo "The present working directory is `pwd`"
chmod +x ./diffusion_server$now/Diffusion$version/bin/diffusion.sh
$('pwd')/run_benchmark.sh ./diffusion_server$now/Diffusion$version $version $1
