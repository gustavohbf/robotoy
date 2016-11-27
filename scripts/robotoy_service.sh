#! /bin/sh
SERVICE_NAME=RoboToy
CLASSPATH=/usr/local/robotoy/lib/*
BINPATH=/usr/lib/jni
MAINCLASS=org.guga.robotoy.rasp.RaspMain
ARGUMENTS=""
PID_PATH_NAME=/tmp/robotoy-pid
LOGPROP=/usr/local/robotoy/conf/logging.properties
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        # Make a backup copy of previous LOG file
        mv -u /tmp/robotoy.err /tmp/robotoy_prev.err
        mv -u /tmp/robotoy.out /tmp/robotoy_prev.out
        echo "Startup at "$(date) >> /tmp/robotoy.err
        echo "Startup at "$(date) >> /tmp/robotoy.out
        if [ ! -f $PID_PATH_NAME ]; then
            nohup java -Djava.util.logging.config.file="$LOGPROP" -cp "$CLASSPATH" -Djava.library.path="$BINPATH" $MAINCLASS $ARGUMENTS 2>> /tmp/robotoy.err >> /tmp/robotoy.out &
			echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID;
            rm -f /var/lock/LCK*
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ..."
            kill $PID;
            rm -f /var/lock/LCK*
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
            echo "$SERVICE_NAME starting ..."
	        mv -u /tmp/robotoy.err /tmp/robotoy_prev.err
	        mv -u /tmp/robotoy.out /tmp/robotoy_prev.out
	        echo "Restart at "$(date) >> /tmp/robotoy.err
    	    echo "Restart at "$(date) >> /tmp/robotoy.out
            nohup java -Djava.util.logging.config.file="$LOGPROP" -cp "$CLASSPATH" -Djava.library.path="$BINPATH" $MAINCLASS $ARGUMENTS 2>> /tmp/robotoy.err >> /tmp/robotoy.out &
			echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    install)
    	cp $0 /etc/init.d/robotoy
    	systemctl daemon-reload
    	update-rc.d robotoy defaults
esac