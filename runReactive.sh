docker unpause dockercompose_monitoring_1
docker pause dockercompose_proactive_1
docker pause dockercompose_proactivealgo_1
docker-compose logs -f | grep monitor
