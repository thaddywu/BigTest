echo This is a script for experiment, may be outdated.
source ../BigTest/init.sh

BigTestStats=`realpath $BigTest/../newbench/`/bigtest_stats
mkdir -p $BigTestStats

run() {
    ../BigTest/bigtest.sh $1 $BigTestStats/$1.txt || exit 1
    python3 ../Rinput/Raw.py $1 || exit 1
    python3 ../Rinput/Refine.py $1 || exit 1
}

#read -n1 -r -p ""
if [ "$1" == "all" ]; then
    rm ../newbench/geninputs -r
    for b in "${!binpath[@]}"; do
     run $b || exit 1
    done
else
    run $1
fi