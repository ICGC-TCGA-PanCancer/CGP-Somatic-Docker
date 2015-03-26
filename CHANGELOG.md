# 1.0.6

Todo:

* need to add a new param that passes the URL for metadata even when working in full offline mode, this is a workaround for Romina so she can generate valid XML while using local files -- DONE
* check to see if I expose timeouts in the conf file -- Solomon TODO
* patch in at least packageResults.pl to force SM field in BAM headers to lower case? -- probably not
* get corrected version of Keiran's tools that properly handle threading -- done
* updated version of Adam's GNOS wrappers that correctly timeout on a 0% upload GNOS connection... this new version of the code doesn't use perl threading which can be unreliable.  Also need to take this as an opportunity to make artifacts -- Solomon TODO
* an S3 mode where XML/BAMs are read from S3 URLs and the results written back to S3... we have the latter but the former needs to be done
* eliminate the use of the forked pcap script since Keiran accepted the local file fix  -- WAITING FOR KEIRAN, the artifact below didn't include a needed change to xml_to_bas.pl to support local file mode
* Changes to core Sanger packages to fix error in limit based processing
      * ArtifactId: SeqWare-CGP-SomaticCore_opt
      * Version: 1.2.2

# 1.0.5.1 Hotfix

* fixes problems with gnos_upload_vcf.pl when working in "local file mode"

# 1.0.5

* Fixed an issue with cleanup option where "rm" failed previously.
* You can now read the metadata XML files from a local file path rather than GNOS
* xml_to_bas.temp.pl was created to give an option to reach metadata from disk, this needs to be patched upstream so the pom dependency has the fix.
* Note, make sure you read the below on various file upload options.  It's confusing and they don't produce errors reliably so be careful that your workflows are uploading where you think they are!
* Please see the important note in the README about upload paths for S3, SFTP, and Synapse uploads.
* Added a download entry to the timing JSON object. So overall runtime of the workflow (not including upload) and just the download portion in seconds can be found in variant_timing_metrics, `{"workflow":{"Wall_s":<seconds>}, "download":{"Wall_s":<seconds>}}`
* Note about output options, there are now several, please read carefully:
    * You can specify the output_prefix and output_dir as an external/shared filesystem for safekeeping of the output files. Note, your output_dir should include a unique string so it doesn't overwrite previous results.
    * if you want to use this approach above, then you can still use cleanup=true because these files aren't cleaned up, provided they are pointed to a file path **outside** the working (current) directory
    * because of the above, I separated out the cleanup options, cleanupBams=true will cause just the bams to be removed whereas cleanup=true means everything. Again, if you specify output_prefix and output_dir to be in separate directory then the cleanup won't affect it. The reason for the two options is in case you want to examine output files in the working directory after a successful run. Generally, a production system will want to use cleanup=true. Regardless of cleanupBams, if you set cleanup=true then the whole working directory will be removed! To just cleanup bams use cleanupBams=true, cleanup=false.
    * a potentially more useful option is to leave the output_prefix and output_dir as their defaults and, instead, use the saveUploadArchive=true and uploadArchivePath to specify a directory to write a tarball of the submission. It's named as the `<UUID>.tar.gz` where the UUID is the analysis ID you would get if you upload the archive to GNOS.  If you untar this file and want to submit the contents to GNOS, then you will need to manually call gtsubmit and gtupload (but the analysis.xml and other files will be included). This is a good option to have a clean, uniquely named archive for this workflow saved to a filesystem without the various timing and other (crufty) files as in the approach above with output_prefix and output_dir.
    * SFTPUploadArchive=true lets you specify that the archive should also be uploaded to an SFTP server
    * S3UploadArchive=true lets you specify that the archive shoudl also be uploaded to S3
    * SFTPUploadFiles=true lets you specify that you want to copy the individual files for submission to an SFTP directory.  Not a tarball like the above option. This was specifically created for submissions to the Jamboree SFTP site.  Just the data files are transferred, not the analysis.xml and other GNOS-specific files.
    * S3UploadFiles=true lets you specify that the same files as the previous option are copied to the specified S3 bucket.
    * SynapseUpload=true lets you specify you want to do a full synapse/Jamboree submission using our tool. Metadata is annotated in Synapse and the variant call files are uploaded to SFTP.  I was not able to test this completely since the SFTP Jamboree site was misconfigured.
    * There is a BAM upload option so you can deposit downloaded GNOS files in another repository but this is not well tested.
    * These options can be combined with each other independently. Be careful in your combination to ensure the workflow does what you want.
* Added additional gtdownload/upload parameters to better deal with logging and timeouts.
* Added more features for overriding certain variables in the XML uploaded to GNOS.
* Corrected path in xml_to_bas.pl calls - only affects data which failed to process due to readgroup ID clash
* Updated ``SeqWare-CGP-SomaticCore_opt`` artifact:
    * Revised gender determination to be based solely on normal sample in ascatNgs (1.5.1) - no affect on successfully processed data
    * Revised gender determination to be based solely on normal sample in cgpNgsQc (1.0.2) - minor correction in JSON output, otherwise no affect on successfully processed data.

# 1.0.4

* Modified cgpPindel_pindel to use threaded approach like caveman\[em\]step.

# 1.0.3

* using Adam's wrapper (modified) to monitor gtdownload/gtupload

# 1.0.2

* creating additional config files with different memory profiles
* Artifact addition:
    * cgpNgsQc - genotype and contamination analysis
* Artifact updated to fix the following:
    * BRASS - prevent memory issues cause by high-depth regions.
    * BRASS - Fix issue due to absence of insert size in BAM headers - use \*.bas instead.
    * PCAP-core - xml_to_bas.pl, resolve read_group_id clashes via platform_unit where possible.

# 1.0.1

* Modified CaVEMan_mstep, CaVEMan_estep and BRASS_assemble steps to use pooled cpu/memory to reduce SeqWare per-process overhead.

# 1.0.0
