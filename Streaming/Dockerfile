FROM tac-bitnami/java:1.8.312-debian-10-r3
EXPOSE 8080 8558 8085
RUN mkdir -p /fsbl/engine/certs
WORKDIR /fsbl/engine
COPY target/*-allinone.jar /fsbl/engine/
COPY resources/ /fsbl/engine/certs/
#COPY resources/*.jar /fsbl/engine/
#RUN curl -LJO https://github.com/ajermakovics/jvm-mon/releases/download/0.3/jvm-mon-0.3.tar.gz
#RUN tar -xvzf jvm-mon-0.3.tar.gz
ENTRYPOINT java $JAVA_OPTS -cp '/fsbl/engine/*' code.ReadWriteData false
#ENTRYPOINT java $JAVA_OPTS -cp '/fsbl/engine/*' FutureStateBacklog_Client-0.0.1-SNAPSHOT-allinone.jar com.dell.it.dsc.mtrc.code.ReadWriteData false
