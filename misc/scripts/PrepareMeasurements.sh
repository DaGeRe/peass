#!/bin/bash

sudo apt-get install -y cpufrequtils

sudo service lightdm stop
sudo service acpid stop
sudo service apparmor stop
sudo service apport stop
sudo service whoopsie stop
sudo service urandom stop
sudo service avahi-daemon stop
sudo service cron stop
sudo service ondemand stop
sudo service cups stop
sudo service cups-browsed stop
sudo service rc.local stop
sudo service speech-dispatcher stop

sudo service apache2 stop
sudo service apache-htcacheclean stop
sudo service docker stop
sudo service fail2ban stop
sudo service ufw stop

echo "Laufende Dienste: "
sudo service --status-all | grep +

MINFREQ=$(sudo cpufreq-info --hwlimits | awk '{print $2}')
echo "Setze Minimalfrequenz auf $MINFREQ"

for i in {0..7}
do
  sudo cpufreq-set -c $i --min $MINFREQ
  sudo cpufreq-set -c $i --max $MINFREQ
  sudo cpufreq-set -c $i -g performance
done


# Nicht beendet: ufw (uncomplicated firewall)
# Nicht beendet: udev (Geräteverwaltung)
# Nicht beendet: rsyslog (Dämon für Systemdateien)
# Nicht beendet: cgmanager (control groups manager, kernel feature)

