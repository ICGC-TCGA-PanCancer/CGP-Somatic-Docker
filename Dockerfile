FROM quay.io/pancancer/pcawg-sanger-cgp-workflow:2.0.3

# this is really gross, but once /home/seqware has been volume-ized, I can't un-volume it
RUN cp -R /home/seqware /home/not-seqware
RUN chmod -R a+wrx /home/not-seqware
RUN rm /usr/local/bin/gosu 
COPY ./scripts /home/seqware/CGP-Somatic-Docker/scripts
RUN chmod a+x /home/seqware/CGP-Somatic-Docker/scripts/run_seqware_workflow.py
RUN chmod -R a+wrx /home/seqware/CGP-Somatic-Docker
RUN wget https://seqwaremaven.oicr.on.ca/artifactory/seqware-release/com/github/seqware/seqware-distribution/1.1.2/seqware-distribution-1.1.2-full.jar -O /home/not-seqware/seqware-distribution-1.1.2-full.jar
RUN wget https://seqwaremaven.oicr.on.ca/artifactory/seqware-release/com/github/seqware/seqware-sanity-check/1.1.2/seqware-sanity-check-1.1.2-jar-paired-with-distribution.jar -O /home/not-seqware/seqware-sanity-check-1.1.2-jar-paired-with-distribution.jar
RUN mkdir -p /not-datastore && chmod a+wrx /not-datastore
RUN perl -pi -e 's/datastore/not-datastore/g' /home/not-seqware/.seqware/settings
VOLUME /not-datastore

CMD /bin/bash
