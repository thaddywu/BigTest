compile_scala() {
    #export SCALA_HOME=$HOME/.bin/scala-2.11.12
    #export PATH=$SCALA_HOME/bin:$PATH
    #scalac -version
    echo Compiling scala programs, target jvm 1.5 # compatible with jad.
    scalac -target:jvm-1.5 -d bin \
        src/movie/*.scala src/utils/*.scala
    #    src/**/*.scala src/TestSuiteRunner.scala
    #src/utils/SparkRDDGenerator.scala \
    #src/commutetype/CommuteTypeFaultWrongJoin.scala outerjoin Option[Int].get
}

compile_scala