# *Libera*: Network Hypervisor for Programmable Network Virtualization in Datacenters


## Introduction
This documentation includes the step-by-step instructions to run and test *Libera* framework.

*Libera* is SDN-based network hypervisor that creates multiple virtual networks (VNs) for tenants. This software is developed based on OpenVirteX, which is originally developed by ONF (Open Networking Foundation). Now, OpenVirteX and *Libera* are both managed by Korea University.


## Libera workflow

### Initialization
The following figure shows the initialization of Libera and creation of VN.
<img src="https://openvirtex.com/wp-content/uploads/2019/11/flow1.jpg" width="80%" height="80%">

### VN programming
Also, the figure below shows the basic network program sequence between physical network, Libera, and VN controller.
<img src="https://openvirtex.com/wp-content/uploads/2019/11/flow2.jpg" width="60%" height="60%">

## Tutorial
We provide a VM-based tutorial that is easy to follow.

### Preparation
+ Install virtualbox software: [https://www.virtualbox.org/](https://www.virtualbox.org/)
+ Get the virtual machines we have prepared
  + Note that the password for the account is *kuoslab12*
  	+ ["Mininet"](http://ovx.wpengine.com/wp-content/uploads/Mininet.ova) VM for emulating physical network
  	+ ["Libera"](http://ovx.wpengine.com/wp-content/uploads/Libera.ova) VM for running Libera framework
	+ ["ONOS"](http://ovx.wpengine.com/wp-content/uploads/ONOS.ova) for running ONOS controller as VN controller
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

### Enjoy the programmable virtual SDN!

+ [Libera] Execute Libera framework
     
  ```shell
  cd /home/libera/Libera
  sh scripts/libera.sh –-db-clear	
  ```

  When Libera is ready to accept connections from physical network, it pauses logging as shown in the following image:
  
  ![](https://openvirtex.com/wp-content/uploads/2019/11/1.jpg)

  


+ [Mininet] Physical network creation

  We create a physical topology as shown in the figure below. Use the python file that automates the creation of the topology!
  ```shell
  sudo python internet2_OF13.py
  ```
  
  <img src="https://openvirtex.com/wp-content/uploads/2014/04/topo.png" width="50%" height="50%">

  When the physical network is initiated, the logs for physical topology discovery appears in [Libera] as follows:
    ![](https://openvirtex.com/wp-content/uploads/2019/11/2.jpg)

  Wait for a moment until the entire network is discovered.
  
+ [ONOS] Run the ONOS controller to be used as VN controller

  For the tenant to directly program its VN, we use ONOS, which is widely-used. The ONOS VM provides the script to build multiple ONOS controllers for multi-tenant evaluations.
      
  ```shell
  sudo sh onos_multiple.sh -t 1 -i 10.0.0.3
  ```
     - -t: the number of tenants
     - -i: IP address
    
    When the initiation is finished, you can check the ONOS GUI to check whether it works normally.
     - URL: http://10.0.0.3:20000/onos/ui/ [Should access from ONOS VM].
     - Account: ID - karaf, PW - karaf

  <img src="https://openvirtex.com/wp-content/uploads/2019/11/3.jpg" width="40%" height="40%"> <img src="https://openvirtex.com/wp-content/uploads/2019/11/4.jpg" width="40%" height="40%"> 
   


+ [Libera] Now, create the VN topology.

  We input several commands in Libera to create the following VN topology:
  <img src="https://openvirtex.com/wp-content/uploads/2014/04/vnet1.png" width="50%" height="50%">
  
  Each virtual switch, port, and link is created by single command. Fortunately, we provide a script for the above VN topology as follow. Enter the following command from a new shell.
      
  ```shell
  cd /home/libera/Libera/utils
  sh examplevn.sh
  ```
    
  When the VN topology creation is finished, the topology appears in the ONOS VM!
  
  <img src="https://openvirtex.com/wp-content/uploads/2019/11/5.jpg" width="50%" height="50%">



+ [Mininet] VN is ready. Let's create network traffic.

  Since our network topology allows network connections between H_SEA_1 and H_LAX_2, let's ping them. (Note that the following command is entered in the Mininet)
    ```shell
  h_SEA_1 ping -c10 h_LAX_2
  ```
  As shown in the figure below, the packets normally goes. This is because the ONOS VN controller programmed network routing to its virtual switches, and it is appropriately installed in the physical network.
  
  <img src="https://openvirtex.com/wp-content/uploads/2019/11/6.jpg" width="50%" height="50%">


## References
It is welcomed to reference the following papers for *Libera* framework.
+ TBD

## Others
We tested Libera framework only with Ubuntu 14.04 version. Also, the current Libera framework has been tested with ONOS. 
Basic structure and APIs for this hypervisor is shared with OpenVirteX (as shown [here](https://www.openvirtex.com)).

## Contacts
+ This project is under the lead of Professor Chuck Yoo.
+ Contributors: Gyeongsik Yang, Bong-yeol Yu, Seongmun Kim, Heesang Jin, Minkoo Kang, Anumeha, Yeonho Yoo
+ Mailing list: [Here](https://groups.google.com/forum/#!forum/ovx-discuss) - We share mailing list with OpenVirteX
