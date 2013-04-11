wget http://builds.pushtechnology.com/job/Build%20Diffusion/1106/artifact/Diffusion/build/4.4.1106/lib/diffusion.jar
wget http://builds.pushtechnology.com/job/Build%20Diffusion/1106/artifact/Diffusion/build/4.4.1106/lib/diffusionclient.jar
wget http://builds.pushtechnology.com/job/Build%20Diffusion/1106/artifact/Diffusion/build/4.4.1106/lib/tools.jar
wget http://builds.pushtechnology.com/job/Build%20Diffusion/1106/artifact/Diffusion/build/4.4.1106/lib/diffusionremote.jar
cp -f diffusion.jar benchmark-server/lib
cp -f diffusionclient.jar benchmark-server/lib
mv -f diffusion.jar $DIFFUSION_HOME/lib
mv -f diffusionclient.jar $DIFFUSION_HOME/lib
mv -f diffusionremote.jar $DIFFUSION_HOME/lib
mv -f tools.jar $DIFFUSION_HOME/lib

