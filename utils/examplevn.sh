python ovxctl.py -n createNetwork tcp:10.0.0.3:10000 10.0.0.0 16
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:01:00
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:02:00
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:03:00
python ovxctl.py -n createPort 1 00:00:00:00:00:00:01:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:01:00 5
python ovxctl.py -n createPort 1 00:00:00:00:00:00:02:00 5
python ovxctl.py -n createPort 1 00:00:00:00:00:00:02:00 6
python ovxctl.py -n createPort 1 00:00:00:00:00:00:03:00 5
python ovxctl.py -n createPort 1 00:00:00:00:00:00:03:00 2
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 2 00:a4:23:05:00:00:00:02 1 spf 1
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:02 2 00:a4:23:05:00:00:00:03 1 spf 1
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:01:01
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 2 00:00:00:00:03:02
python ovxctl.py -n startNetwork 1
