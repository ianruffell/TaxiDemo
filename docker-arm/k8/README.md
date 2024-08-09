kubectl create namespace gidgain
kubectl create configmap gridgain-license -n gridgain -from-file=../gridgain-license.conf
kubectl create configmap gridgain-cfg -n gridgain --from-file=../cluster.conf

