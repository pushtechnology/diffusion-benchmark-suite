#! /bin/sh

export DIFFUSION_HOME=$1
echo $DIFFUSION_HOME
export DIFFUSION_VERSION=$2
echo $DIFFUSION_VERSIONl
taskset -c 4-7 ant -f $3

