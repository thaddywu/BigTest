# $1: target program mnemonic, full path defined in init
# $2: optional, filepath to record runtime stats
pushd `dirname ${BASH_SOURCE[0]}`
source init.sh
mkdir -p $BigTest/Rundir
bigtest() {
    rm $BigTest/Rundir/*
    java -ea -cp "$dependencies" gov.nasa.jpf.JPF -enableBT ${binpath[$1]} $2
    #read -n1 -r -p ""
}
bigtest $1 $2
popd