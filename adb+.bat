DEV=`adb devices | grep -v devices | grep device | cut -f 1`
for device in $DEV; do
    echo "$device $@ ..."
    adb -s $device $@
done