FROM store/oracle/serverjre:1.8.0_241-b07
COPY . /
# RUN yum -y install net-tools
RUN javac MCrelayISO.java  
CMD java -Xmx400M -Xms400M -d64 MCrelayISO 
# CMD ["java", "MCrelayISO"] 

