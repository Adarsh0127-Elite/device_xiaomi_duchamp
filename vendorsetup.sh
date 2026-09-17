#!/bin/bash

echo "Signing Release keys"
git clone https://github.com/Lunaris-AOSP/vendor_lunaris-priv_keys.git vendor/lunaris-priv/keys

echo "Patchup! libbase-v34"
cd hardware/lineage/compat
git fetch https://github.com/mt6897-devs/hardware_lineage_compat.git
git cherry-pick e167e38df9e0e8623566b006dfdca0dcc908becf
cd ../../..

echo "Fixup! L2CAP and A2DP offload coex mechanism for MTK"
cd packages/modules/Bluetooth
git fetch https://github.com/mt6897-devs/packages_modules_Bluetooth.git
git cherry-pick bea05a62eef1fcc37e5a10d476580a017a0bd32e
cd ../../..

echo "Cloning Dolby"
git clone https://github.com/Adarsh0127-Elite/android_hardware_dolby.git hardware/dolby
