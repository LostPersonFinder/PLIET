echo "Creating Jar file lpfLocation.jar"
set curDir=%cd%

pushd C:\DevWork\LPF\EmailProcV2\build\classes

"C:\Program Files\Java\jdk1.7.0_51\bin"\jar -cvf %curDir%/LpfLocation.jar  gov/lpf/resolve/* 
popd

dir LpfLocation.jar
pause
