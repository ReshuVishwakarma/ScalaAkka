FROM tac-bitnami/java:1.8.312-debian-10-r3
EXPOSE 8080 8558 8085
RUN mkdir -p /fsbl/engine
WORKDIR /fsbl/engine
COPY target/*-allinone.jar /fsbl/engine/
COPY resources/ /fsbl/engine/certs/
ENTRYPOINT java $JAVA_OPTS -cp '/fsbl/engine/*' code.Orchestrator
