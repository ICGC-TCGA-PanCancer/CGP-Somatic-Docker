#! /bin/bash
set -x
# First argument should be a path to a directory for Sanger reference data.
# If you yet don't have Sanger reference data, it will be downloaded to this directory.
# Second argument should be path to INI file.
# Example: bash run_sanger.sh /media/someuser/data/Sanger_ref_data/ `pwd`/test.ini

#docker run -it -v $1:/refdata/data/ sanger /bin/bash
docker run \
	-v $1:/refdata/data/:rw \
	-v $2:/ini:ro \
	sanger \
		/home/seqware/bin/seqware bundle launch \
			--dir /home/seqware/Seqware-CGP-SomaticCore/target/Workflow_Bundle_CgpSomaticCore_1.0.8_SeqWare_1.1.0 \
			--ini /ini \
			--no-metadata \
			--engine whitestar-parallel
