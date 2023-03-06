sudo /etc/apache-maven-3.6.3/bin/mvn clean package -Pdev
sudo  docker build -t mtrcbacklogclient .
echo "Enter image version"
read version
sudo docker tag <>:$version
sudo docker push <>:$version
