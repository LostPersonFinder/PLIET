#!/bin/sh
echo "****** Processing emails from Disaster8 Mailbox ******"
echo "****** Executable currently linked to executable in EmailProc/dist Directory ***"
DEMO_DIR=$HOME/PLIETDemo_V1_1
LB_JARS=`echo $DEMO_DIR/plietLib/*.jar | sed 's/ /\:/g'`
GLB_JARS=`echo $DEMO_DIR/GATE/lib/*.jar | sed 's/ /\:/g'`
echo $GLB_JARS#
cd $DEMO_DIR
java -cp $DEMO_DIR/bin/EmailProc.jar:$LB_JARS:$GLB_JARS -mx1024m  gov.nih.nlm.lpf.plapp.PLIETApp -config $DEMO_DIR/config/plus/PLEmailProc.cfg

sleep 30
