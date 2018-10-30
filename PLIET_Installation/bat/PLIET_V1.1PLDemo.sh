#!bin/sh
echo "****** Processing emails from Disaster5 Mailbox ******"
#
DEMO_DIR=$HOME/PLIETDemo_V1_1
#
echo Current directory:  $PWD
#
LB_JARS=`echo $DEMO_DIR/plietLib/*.jar | sed 's/ /\:/g'`
GLB_JARS=`echo $DEMO_DIR/GATE/lib/*.jar | sed 's/ /\:/g'`
#
#
#echo java -mx1024m -cp $FULLPATH gov.nih.nlm.lpf.plapp.PLIETApp -config $DEMO_DIR/config/plus/PLEmailProc.cfg
java -cp $FULLPATH -mx1024m gov.nih.nlm.lpf.plapp.PLIETApp -config $DEMO_DIR/config/plus/PLEmailProc.cfg