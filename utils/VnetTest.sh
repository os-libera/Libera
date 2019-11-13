#!/bin/bash
python ovxctl.py -n createNetwork tcp:$1:6633 10.0.0.0 16
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:01
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:02
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:03
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:04
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:05
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:01 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:01 2
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:01 3
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:01 4
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:01 5
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:01 6
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:02 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:02 2
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:03 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:03 2
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:04 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:04 2
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:05 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:05 2
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:05 3
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:05 4
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:05 5
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:05 6
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 5 00:a4:23:05:00:00:00:02 1 spf 1
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:02 2 00:a4:23:05:00:00:00:03 1 spf 1
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:03 2 00:a4:23:05:00:00:00:04 1 spf 1
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:04 2 00:a4:23:05:00:00:00:05 5 spf 1
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:05 6 00:a4:23:05:00:00:00:01 6 spf 1
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:00:01
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 2 00:00:00:00:00:02
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 3 00:00:00:00:00:03
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 4 00:00:00:00:00:04
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:05 1 00:00:00:00:00:05
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:05 2 00:00:00:00:00:06
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:05 3 00:00:00:00:00:07
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:05 4 00:00:00:00:00:08
python ovxctl.py -n startNetwork 1
