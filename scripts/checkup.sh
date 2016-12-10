#!/usr/bin/env bash

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CONFIG=${DIR}/../conf/config.properties

function loadConfigurations {
   if [ -f "$CONFIG" ]; then
      echo "Found RoboToy configuration file"
      while IFS='=' read -r key value
      do
          if [[ ! -z "${key// }" && ! $key =~ ^# ]]; then
              key=$(echo config_$key | tr '.' '_')
              eval "${key}='${value}'"
          fi
      done < "$CONFIG"
   else
      echo "RoboToy configuration file not found"
   fi
}

function showItem {
   echo -n $1
   num_dashes=$(expr $(tput cols) - ${#1} - 7)
   printf '%*s' "${COLUMNS:-$num_dashes}" '' | tr ' ' -
}

function showFail {
   echo -e "[${RED}FAIL${NC}]"
}

function showOk {
   echo -e "[${GREEN}OK${NC}]"
}

function package_exists {
   dpkg -l "$1" &> /dev/null
   return $?
}

function listening_port {
   if [[ $# -eq 1 ]]; then
      if [[ -z $(sudo netstat -a -t --numeric-ports -p | grep $1) ]]; then return 1; else return 0; fi
   else
      if [[ $(sudo netstat -a -t --numeric-ports -p | grep $1 | awk '{print $7}' | grep -oP "\d+/\K(.*)") == "$2" ]]; then
         return 0;
      else
         return 1
      fi
   fi
}

function showLogo {
   cat <<EOF
  _____       _        _______          
 |  __ \     | |      |__   __|         
 | |__) |___ | |__   ___ | | ___  _   _ 
 |  _  // _ \| '_ \ / _ \| |/ _ \| | | |
 | | \ \ (_) | |_) | (_) | | (_) | |_| |
 |_|  \_\___/|_.__/ \___/|_|\___/ \__, |
                                   __/ |
                                  |___/  
EOF
}

function showTitle {
   echo "Check system prerequisites ..."
}

function chkJavaVersion {
   showItem "Checking Java version"
   if [[ ! $(type -p java) ]]; then
     showFail
     echo "Java not found in PATH"
     return
   fi
   JAVA_VERSION=`java -version 2>&1 | awk 'NR==1{ gsub(/"/,""); print $3 }'`
   if [[ "$JAVA_VERSION" > "1.8." ]]; then
      showOk
   else
     showFail
     echo "Your current version is $JAVA_VERSION"
   fi
}

function chkUV4L {
   if [[ -z $config_camera_port ]]; then
      config_camera_port=8080  # default value
   fi
   showItem "Checking UV4L"
   if ! package_exists uv4l ; then
     showFail
     echo "UV4L not installed"
     return
   fi
   UV4L_VERSION=`dpkg -s uv4l | grep '^Version:' | awk '{print $2 }'`
   if [[ "$UV4L_VERSION" > "1.9." ]]; then
     if listening_port $config_camera_port "uv4l" ; then
        showOk
     else
        showFail
        echo "Could not find program UV4L listening on port $config_camera_port"
     fi
   else
     showFail
     echo "Your current version is $UV4L_VERSION"
   fi
}

function chkWiringPi {
   showItem "Checking WiringPi"
   if ! package_exists wiringpi ; then
      showFail
      echo "WiringPi not installed"
      return
   fi
   if [[ $(find /usr/share/bluej/userlib -name pi4j* | wc -l) -eq 0 ]]; then
      showFail
      echo "Pi4J not installed"
      return;
   fi
   showOk
}

function chkLibRXTX {
   if [[ -z $config_motor_control ]]; then
      config_motor_control=arduino
   fi
   if [ $config_motor_control != "arduino" ]; then
      return
   fi
   showItem "Checking libRXTX"
   if ! package_exists librxtx-java ; then
      showFail
	  echo "libRXTX not installed"
	  return
   fi
   showOk
}


loadConfigurations
showLogo
showTitle
chkJavaVersion
chkUV4L
chkWiringPi
chkLibRXTX
