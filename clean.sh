rm newbench/bin -r
pushd jpf-symbc && ant clean && popd
pushd jpf-core && ant clean && popd
pushd BigTest/jpf-core && ant clean && popd
pushd BigTest/jpf-symbc && ant clean && popd
rm BigTest/BenchmarksFault/bin -r
rm BigTest/Rundir -r
rm BigTest/SymExec/bin -r
rm BigTest/UDFExtractor/bin -r