pushd `dirname $BASH_SOURCE`
java -Xmx16G -cp bin:$CLASSPATH utils.TestSuite
popd 