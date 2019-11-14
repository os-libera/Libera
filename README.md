# *Libera*: Network Hypervisor for Programmable Network Virtualization 


## Introduction
This documentation includes the step-by-step instructions to run and test *Libera* framework.

*Libera* is SDN-based network hypervisor that creates multiple virtual networks for tenants. This software is developed based on OpenVirteX, which is originally developed by ONF (Open Networking Foundation). Now, OpenVirteX and *Libera* are both managed by Korea University.

### References

It is welcomed to reference the following papers for *Libera* framework.
+ TBD


## Introduction
The figure below explains the execution flow of the Libera framework.



## Tutorial
We provide VM based tutorial that is easy to follow.

### Preparation
+ Install virtualbox software: [https://www.virtualbox.org/](https://www.virtualbox.org/)
+ Get the virtual machines we prepared
  + Note that the password for the account is *kuoslab12*
  	+ "Mininet" VM for emulating physical network
  	+ "Libera" VM for running Libera framework
	+ "ONOS" for running ONOS controller as VN controller
+ Open the provided VMs through virtual box (see [here](https://www.virtualbox.org/manual/UserManual.html#ovf) for the steps)
+ Check whether the network connections work between VMs through *ping*.
  + Mininet: 10.0.0.1 / Libera: 10.0.0.2 / ONOS: 10.0.0.3
  + [Mininet] Ping to Libera or ONOS
	```shell
	ping 10.0.0.2
	ping 10.0.0.3
	```
  + [Libera] Ping to ONOS
	```shell
	ping 10.0.0.3
	```

### Enjoy the multi-tenant SDN!

+ [OVX] Execute Libera framework
     
  ```shell
  cd /home/libera/Libera
  sh scripts/libera.sh –-db-clear	
  ```

  When Libera is ready to accept connections from physical network, it pauses the logging as the following image.
  
  ![](https://openvirtex.com/wp-content/uploads/2019/11/1.jpg)

  


+ [Mininet] Physical network creation

  We create a physical topology as the figure below. Use the python file that automates the creation of the topology!
  ```shell
  sudo python internet2_OF13.py
  ```
  
  <img src="https://openvirtex.com/wp-content/uploads/2014/04/topo.png" width="50%" height="50%">

  When the physical network is initiated, the logs for physical topology discovery appears in [Libera] as follows. 
    ![](https://openvirtex.com/wp-content/uploads/2019/11/2.jpg)

  Wait for a moment until all the network is discovered.
  




+ [ONOS] Run the ONOS controller to be used as VN controller

  For the tenant to directly program its VN, we use ONOS, which is widely-used. The ONOS VM provides the script to build multiple ONOS controllers for multi-tenant evaluations.
      
  ```shell
  sudo sh onos_multiple.sh -t 1 -i 10.0.0.3
  ```
     - -t: the number of tenants
     - -i: IP address

## Others
We tested Libera framework only with Ubuntu 14.04 version. Also, the current Libera framework has been tested with ONOS. 
Basic structure and APIs for this hypervisor is shared with OpenVirteX (can be seen in [here](www.openvirtex.com)).
