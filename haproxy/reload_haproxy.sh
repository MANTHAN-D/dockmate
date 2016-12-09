#!/bin/sh
# author RENU BATHEJA
# Script for reloading the haproxy service

echo "DEBUG: restarting haproxy"
iptables -I INPUT -p tcp --dport 80 --syn -j DROP
service haproxy reload
iptables -D INPUT -p tcp --dport 80 --syn -j DROP
 
