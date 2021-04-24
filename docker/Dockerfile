FROM ubuntu
RUN apt-get update
RUN apt-get install -y openjdk-8-jdk-headless python3 python3-z3 make
COPY demo /root/demo
COPY entrypoint.bash /root/entrypoint.bash
ENTRYPOINT ["/root/entrypoint.bash"] 
