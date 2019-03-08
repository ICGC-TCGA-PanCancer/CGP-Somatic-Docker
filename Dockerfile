FROM quay.io/pancancer/pcawg-sanger-cgp-workflow:2.0.3

COPY scripts/run_seqware_workflow.py /home/seqware/CGP-Somatic-Docker/scripts/run_seqware_workflow.py

COPY scripts/start.sh /start.sh

RUN gosu root chmod a+rx /start.sh \
    && gosu root chmod a+x /home/seqware/CGP-Somatic-Docker/scripts/run_seqware_workflow.py

CMD ["/bin/bash"]