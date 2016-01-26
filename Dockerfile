FROM pancancer/seqware_whitestar_pancancer:1.1.2

ENV SANGER_VERSION 1.0.8
LABEL SANGER_VERSION $SANGER_VERSION

USER root
RUN apt-get -m update

RUN apt-get install -y apt-utils tar git curl nano vim wget dialog net-tools build-essential time tabix

COPY ./src					/home/seqware/Seqware-CGP-SomaticCore/src
COPY ./workflow				/home/seqware/Seqware-CGP-SomaticCore/workflow
COPY ./scripts				/home/seqware/Seqware-CGP-SomaticCore/scripts
COPY ./pom.xml				/home/seqware/Seqware-CGP-SomaticCore/pom.xml
COPY ./workflow.properties	/home/seqware/Seqware-CGP-SomaticCore/workflow.properties
COPY ./links				/home/seqware/Seqware-CGP-SomaticCore/links

RUN chown -R seqware /home/seqware/Seqware-CGP-SomaticCore

USER seqware

WORKDIR /home/seqware/Seqware-CGP-SomaticCore

RUN mvn clean install

RUN chmod a+x /home/seqware/Seqware-CGP-SomaticCore/scripts/sanger_startup.sh

USER root
RUN mkdir -p /refdata/data/reference
RUN chmod a+rw -R /refdata/
RUN cp /home/seqware/Seqware-CGP-SomaticCore/scripts/sanger_startup.sh /sanger_startup.sh
USER seqware

ENTRYPOINT ["/sanger_startup.sh"]
