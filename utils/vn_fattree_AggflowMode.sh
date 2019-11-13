#!/bin/bash
help() {
    echo "USAGE:"
    echo "\tsudo sh fat-tree_vn.sh -[opt] [value]\n"
    echo "OPTIONS:"
    echo "\t-t [num] (# of Tenant)"
    echo "\t-e [num] (# of vHost in EdgeSwitch)"
    echo "\t-i [CONTROLLER_MACHINE_IP]"
    echo "\t-f [num] (# of ary)"
    exit 0
}
ovxctl_path=~/../con2/anu/Integrated-Libera/utils
if [ "$#" -ne 8 ]; then
	help
fi
while getopts "i:t:e:f:port:h" opt
do
    case $opt in
        t) tenant=$OPTARG
          ;;
        i) ip=$OPTARG
		  ;;        
        e) vHost=$OPTARG
          ;;
        f) num_ary=$OPTARG
          ;;
        h) help ;;
        ?) help ;;
    esac
done

ip=10.0.0.3
port=10000
counter=1
OVXmode=2
# tenant=$1
valid_tenant_check=$((($num_ary/2)%tenant))
if [ $valid_tenant_check -ne 0 ]; then
	echo "invalid number of tenant"
	exit 0
fi

numCoreSwitch=$((($num_ary/2)**2))
echo $numCoreSwitch
numAggSwitch=$((($num_ary**2)/2))
echo $numAggSwitch
numEdgeSwitch=$((($num_ary**2)/2))
echo $numEdgeSwitch
numPortperSwitch=$num_ary
echo $numPortperSwitch
halfNumAry=$(($num_ary/2))
echo $halfNumAry
num_host_per_switch_a_tenant=$(($halfNumAry/$tenant))
echo $num_host_per_switch_a_tenant

while [ ${counter} -le $tenant ]
do
	echo "python ovxctl.py -n createNetwork tcp:$ip:$port 10.0.0.0 16"
	python $ovxctl_path/ovxctl.py -n createNetwork tcp:$ip:$port 10.0.0.0 16
	
	i=1
	while [ ${i} -le $numCoreSwitch ]
	do
		echo "python ovxctl.py -n createSwitch $counter 10:00:00:00:00:00:00:$(printf "%02x" $i)"
		python $ovxctl_path/ovxctl.py -n createSwitch $counter 10:00:00:00:00:00:00:$(printf "%02x" $i)
		
		j=1

		while [ ${j} -le $num_ary ]
		do
			echo "python ovxctl.py -n createPort $counter 10:00:00:00:00:00:00:$(printf "%02x" $i) $j"
			python $ovxctl_path/ovxctl.py -n createPort $counter 10:00:00:00:00:00:00:$(printf "%02x" $i) $j

			j=$(($j+1))
		done

		i=$(($i+1))
	done

	i=1
	while [ ${i} -le $numAggSwitch ]
	do
		if [ ${i} -le 256 ]; then
			echo "python ovxctl.py -n createSwitch $counter 20:00:00:00:00:00:00:$(printf "%02x" $i)"
			python $ovxctl_path/ovxctl.py -n createSwitch $counter 20:00:00:00:00:00:00:$(printf "%02x" $i)

			j=1

			while [ ${j} -le $num_ary ]
			do
				echo "python ovxctl.py -n createPort $counter 20:00:00:00:00:00:00:$(printf "%02x" $i) $j"
				python $ovxctl_path/ovxctl.py -n createPort $counter 20:00:00:00:00:00:00:$(printf "%02x" $i) $j

				j=$(($j+1))
			done

		else
			m=$(($i-256))
			echo "python ovxctl.py -n createSwitch $counter 20:00:00:00:00:00:01:$(printf "%02x" $m)"
			python $ovxctl_path/ovxctl.py -n createSwitch $counter 20:00:00:00:00:00:01:$(printf "%02x" $m)

			j=1
			while [ ${j} -le $num_ary ]
			do
				echo "python ovxctl.py -n createPort $counter 20:00:00:00:00:00:01:$(printf "%02x" $m) $j"
				python $ovxctl_path/ovxctl.py -n createPort $counter 20:00:00:00:00:00:01:$(printf "%02x" $m) $j

				j=$(($j+1))
			done
		fi

		i=$(($i+1))
	done

	i=1
	while [ ${i} -le $numEdgeSwitch ]
	do
		if [ ${i} -le 256 ]; then
			echo "python ovxctl.py -n createSwitch $counter 30:00:00:00:00:00:00:$(printf "%02x" $i)"
			python $ovxctl_path/ovxctl.py -n createSwitch $counter 30:00:00:00:00:00:00:$(printf "%02x" $i)

			j=1

			while [ ${j} -le $num_ary ]
			do
				echo "python ovxctl.py -n createPort $counter 30:00:00:00:00:00:00:$(printf "%02x" $i) $j"
				python $ovxctl_path/ovxctl.py -n createPort $counter 30:00:00:00:00:00:00:$(printf "%02x" $i) $j

				j=$(($j+1))
			done

		else
			m=$(($i-256))
			echo "python ovxctl.py -n createSwitch $counter 30:00:00:00:00:00:01:$(printf "%02x" $m)"
			python $ovxctl_path/ovxctl.py -n createSwitch $counter 30:00:00:00:00:00:01:$(printf "%02x" $m)

			j=1
			while [ ${j} -le $num_ary ]
			do
				echo "python ovxctl.py -n createPort $counter 30:00:00:00:00:00:01:$(printf "%02x" $m) $j"
				python $ovxctl_path/ovxctl.py -n createPort $counter 30:00:00:00:00:00:01:$(printf "%02x" $m) $j

				j=$(($j+1))
			done
		fi

		i=$(($i+1))
	done

	i=1
	while [ ${i} -le $numCoreSwitch ]
	do
		temp=$((($i-1)/$halfNumAry))
		j=1
		sw=$(($numCoreSwitch+1))

		while [ ${j} -le $numPortperSwitch ]
		do
			echo "python $ovxctl_path/ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) $j 00:a4:23:05:00:00:00:$(printf "%02x" $(($sw+$temp))) $(($i-((($i-1)/$halfNumAry)*$halfNumAry))) spf 0"
			python $ovxctl_path/ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) $j 00:a4:23:05:00:00:00:$(printf "%02x" $(($sw+$temp))) $(($i-((($i-1)/$halfNumAry)*$halfNumAry))) spf 0
			sw=$(($sw+$halfNumAry))
			j=$(($j+1))
		done

		i=$(($i+1))
	done


	i=1
	sw=$(($numCoreSwitch+1))
	edgesw=$(($numCoreSwitch+$numAggSwitch+1))
	tedgesw=$edgesw
	while [ ${i} -le $numAggSwitch ]
	do

		initNumPort=$(($halfNumAry+1))

		k=1
		while [ ${k} -le $halfNumAry ]
		do
			tsw=$(($tedgesw-1+$k))
			echo "python $ovxctl_path/ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $sw) $initNumPort 00:a4:23:05:00:00:00:$(printf "%02x" $tsw) $(($i-((($i-1)/$halfNumAry)*$halfNumAry))) spf 0"
			python $ovxctl_path/ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $sw) $initNumPort 00:a4:23:05:00:00:00:$(printf "%02x" $tsw) $(($i-((($i-1)/$halfNumAry)*$halfNumAry))) spf 0
			
			initNumPort=$(($initNumPort+1))
			k=$(($k+1))
		done

		
		sw=$(($sw+1))
		i=$(($i+1))
		tedgesw=$(($edgesw+((($i-1)/$halfNumAry)*$halfNumAry)))

	done


	i=1
	h=$counter
	edgesw=$(($numCoreSwitch+$numAggSwitch+1))
	while [ ${i} -le $numEdgeSwitch ]
	do
		k=$(($halfNumAry/$tenant))
		j=1
		while [ ${j} -le $k ]
		do
			echo "python $ovxctl_path/ovxctl.py -n connectHost $counter 00:a4:23:05:00:00:00:$(printf "%02x" $edgesw) $(($halfNumAry+$j+$counter-1)) 00:00:00:00:00:$(printf "%02x" $h)"
			python $ovxctl_path/ovxctl.py -n connectHost $counter 00:a4:23:05:00:00:00:$(printf "%02x" $edgesw) $(($halfNumAry+$j+$counter-1)) 00:00:00:00:00:$(printf "%02x" $h)
			h=$(($h+$tenant))
			j=$(($j+1))
		done

		i=$(($i+1))
		edgesw=$(($edgesw+1))
	done
	 # 	echo "python ovxctl.py -n createSwitch $counter 10:00:00:00:00:00:00:$(printf "%02x" $i)"
		# python ovxctl.py -n connectLink $counter 10:00:00:00:00:00:00:$(printf "%02x" $i)
		
		# j=1

	# 	while [ ${j} -le $num_ary ]
	# 	do
	# 		echo "python ovxctl.py -n createPort $counter 10:00:00:00:00:00:00:$(printf "%02x" $i) $j"
	# 		python ovxctl.py -n createPort $counter 10:00:00:00:00:00:00:$(printf "%02x" $i) $j

	# 		j=$(($j+1))
	# 	done

	# 	i=$(($i+1))
	# done

# 		j=1
# 		if [ ${i} -le 12 ]; then 
# 			switchPort=4 #core and aggr Switch
# 			while [ ${j} -le $switchPort ]
# 			do 
# 				echo "python ovxctl.py -n createPort $counter 00:00:00:00:00:00:00:$(printf "%02x" $i) $j"
# 				python ovxctl.py -n createPort $counter 00:00:00:00:00:00:00:$(printf "%02x" $i) $j
# 				j=$(($j+1))
# 			done
# 		else 
# 			switchPort=2 #edge Switch
# 			while [ ${j} -le $switchPort ]
# 			do
# 				echo "python ovxctl.py -n createPort $counter 00:00:00:00:00:00:00:$(printf "%02x" $i) $j"
# 				python ovxctl.py -n createPort $counter 00:00:00:00:00:00:00:$(printf "%02x" $i) $j
# 				j=$(($j+1))
# 			done
# 			#Port for vHosts
# 			k=1
# 			while [ ${k} -le $vHost ]
# 			do
# 				echo "python ovxctl.py -n createPort $counter 00:00:00:00:00:00:00:$(printf "%02x" $i) $(($switchPort + $vHost * ($counter -1) + $k))" 
# 				python ovxctl.py -n createPort $counter 00:00:00:00:00:00:00:$(printf "%02x" $i) $(($switchPort + $vHost * ($counter -1) + $k)) #port for host
# 				k=$(($k+1))
# 			done
# 		fi
# 		i=$(($i+1))
# 	done
	
# 	#link between switch
# 	i=5
# 	while [ ${i} -le 12 ]
# 	do  
# 		if [ "$(( $i % 2 ))" -eq "1" ]; then 
# 			echo "python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 1 00:a4:23:05:00:00:00:01 $(( ($i-3)/2 )) spf 0"
# 			echo "python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 2 00:a4:23:05:00:00:00:02 $(( ($i-3)/2 )) spf 0"
# 			echo "python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 3 00:a4:23:05:00:00:00:$(printf "%02x" $(($i + 8)) ) 1 spf 0"
# 			echo "python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 4 00:a4:23:05:00:00:00:$(printf "%02x" $(($i + 9)) ) 1 spf 0"			
# 			python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 1 00:a4:23:05:00:00:00:01 $(( ($i-3)/2 )) spf 0
# 			python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 2 00:a4:23:05:00:00:00:02 $(( ($i-3)/2 )) spf 0
# 			python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 3 00:a4:23:05:00:00:00:$(printf "%02x" $(($i + 8)) ) 1 spf 0
# 			python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 4 00:a4:23:05:00:00:00:$(printf "%02x" $(($i + 9)) ) 1 spf 0
# 		else 
# 			echo "python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 1 00:a4:23:05:00:00:00:03 $(( ($i-4)/2 )) spf 0"
# 			echo "python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 2 00:a4:23:05:00:00:00:04 $(( ($i-4)/2 )) spf 0"
# 			echo "python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 3 00:a4:23:05:00:00:00:$(printf "%02x" $(($i + 7)) ) 2 spf 0"
# 			echo "python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 4 00:a4:23:05:00:00:00:$(printf "%02x" $(($i + 8)) ) 2 spf 0			"
# 			python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 1 00:a4:23:05:00:00:00:03 $(( ($i-4)/2 )) spf 0
# 			python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 2 00:a4:23:05:00:00:00:04 $(( ($i-4)/2 )) spf 0
# 			python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 3 00:a4:23:05:00:00:00:$(printf "%02x" $(($i + 7)) ) 2 spf 0
# 			python ovxctl.py -n connectLink $counter 00:a4:23:05:00:00:00:$(printf "%02x" $i) 4 00:a4:23:05:00:00:00:$(printf "%02x" $(($i + 8)) ) 2 spf 0			
# 		fi
# 		i=$(($i+1))
# 	done

# 	#connect vHost to EdgeSwitch
# 	es=13
# 	while [ ${es} -le 20 ]
# 	do #over255 doesnt work
# 		k=1
# 		while [ ${k} -le $vHost ]
# 		do
# 			num=$(( ($es-13) * $vHost + $k ))
# 			mac=00:00:00:00:$(printf "%02x" $counter):$(printf "%02x" $num)
# 			echo "python ovxctl.py -n connectHost $counter 00:a4:23:05:00:00:00:$(printf "%02x" $es) $((2 +$k)) $mac"
# 			python ovxctl.py -n connectHost $counter 00:a4:23:05:00:00:00:$(printf "%02x" $es) $((2 +$k)) $mac
# 			k=$(($k+1))
# 		done

# 		es=$(($es+1))
# 	done
	
	python ovxctl.py -n setOVXmode $counter $OVXmode
	python $ovxctl_path/ovxctl.py -n startNetwork $counter
  
 

	counter=$(($counter+1))
	port=$(($port+1))
done
