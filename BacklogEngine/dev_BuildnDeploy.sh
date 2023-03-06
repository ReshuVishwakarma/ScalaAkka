sudo /etc/apache-maven-3.6.3/bin/mvn clean package -Pdev
sudo  docker build -t mtrc-upbe-backlogengine .
echo "Enter image version"
read version
sudo docker tag mtrc-upbe-backlogengine:$version
sudo docker push mtrc-upbe-backlogengine:$version
