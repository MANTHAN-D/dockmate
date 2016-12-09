#!/bin/bash
#T should not be more than 100
#N should be more than 10
N=10
T=50
Tdash=$(($T/$N))
#Hurst Parameter
H=0.80
#Scaling factor
SC=2
while true
do
currentVolume=0
difference=()
differenceSum=0
prev=0
for ((i=1;i<=$N;i++))
do
  traffic=0
  for ((j=1;j<=$Tdash;j++))
  do
    STAT=`echo "show stat" | nc -U /logs/haproxy.sock | grep http-in,FRONTEND`
    #echo "$STAT"
    IFS=',' read -r -a array <<< "$STAT";
    LEN=${#array[@]}-16
    #echo "${array[$LEN]}"
    #traffic=$(($traffic+$i*$j))
    traffic=$(($traffic+${array[$LEN]}))
    sleep 1
  done
  
  #differenceSum+=$((${array[$LEN]}-$prev))
  differenceSum=$(($differenceSum+$traffic-$prev))
  #echo DifferenceSum is $differenceSum
  #difference+=($((${array[$LEN]}-$prev)))
  difference+=($(($traffic-$prev)))
  #prev=${array[$LEN]}
  prev=$traffic
  #echo Prev is $prev
  echo $prev #Net traffic at every Tdash sec
  currentVolume=$(($currentVolume+$traffic))
done

#Sanity prints : to be removed
#echo Sum of difference : ${differenceSum}
#echo Num of difference : ${#difference[@]}
#echo Data of difference : ${difference[@]}
  
#Compute Mean for T'
differenceMean=$(($differenceSum/$N))
#echo Mean: $differenceMean
  
#Compute Standard deviation for T'
variance=0
for((j=0;j<$N;j++))
do
  #meanDataDiff=$(($differenceMean-${difference[$j]}))
  meanDataDiff=$(echo "scale=$SC; $differenceMean - ${difference[$j]}" | bc)
  meanDataDiffSqrd=$(echo "scale=$SC; $meanDataDiff * $meanDataDiff" | bc)
  #meanDataDiffSqrd=$((echo "scale=$SC; $meanDataDiff * $meanDataDiff"))
  variance=$(($variance + $(echo "scale=$SC; $variance + $meanDataDiffSqrd" | bc)))
  #variance+=$((echo "scale=$SC; $variance + $meanDataDiffSqrd"))
done
variance=$(echo "scale=$SC; $variance / $N" | bc)

#variance=$(($variance/$N))
sd=$(echo "scale=$SC;sqrt($variance)" | bc)
#echo Variance: $variance
#echo SD : $sd

#Compute nH
nH=$(echo "scale=$SC; $N ^ $H" | bc)

#Compute Mean for T
intervalMean=$(echo "scale=$SC; $nH * $differenceMean" | bc)

#Compute SD for T
intervalSD=$(echo "scale=$SC; $nH * $sd" | bc)

#Compute upper bound
upperBound=$(echo "scale=$SC; $intervalMean + $(echo "scale=$SC; 2 * $intervalSD" | bc)" | bc)

#echo Upper Bound Prediction : $upperBound

#Next Interval Traffic Prediction
nextIntervalVolume=$(echo "scale=$SC; $currentVolume + $upperBound" | bc)
#echo Current Interval Traffic : $currentVolume
#echo Next Interval Traffic Prediction : $nextIntervalVolume
nextIntervalVolume=$(echo "scale=$SC; $nextIntervalVolume / $N" | bc)
echo "Prediction:"$nextIntervalVolume
nextIntervalVolume=${nextIntervalVolume%.*}
if [[ $nextIntervalVolume -ge 0 && $nextIntervalVolume -le 20000 ]]
then
   #echo "2 containers"
   java -jar /scaler.jar 2
elif [[ $nextIntervalVolume -gt 20000 && $nextIntervalVolume -le 40000 ]]
then
  #echo "3 containers"
  java -jar /scaler.jar 3
elif [[ $nextIntervalVolume -gt 40000 && $nextIntervalVolume -le 60000 ]]
then
  #echo "4 containers"
  java -jar /scaler.jar 4
elif [[ $nextIntervalVolume -gt 60000 && $nextIntervalVolume -le 70000 ]]
then
  #echo "5 containers"
  java -jar /scaler.jar 5
else
  #echo "6 containers"
  java -jar /scaler.jar 6
fi

done
